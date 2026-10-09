package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.data.LatLngPoint
import com.example.data.TravelMode
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

data class LiveTrackingState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val isSimulationMode: Boolean = false,
    val selectedMode: TravelMode = TravelMode.WALK,
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0L,
    val currentSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val currentLatLng: LatLngPoint? = null,
    val routePoints: List<LatLngPoint> = emptyList(),
    val startTimestamp: Long = 0L
)

class LocationTracker(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _trackingState = MutableStateFlow(LiveTrackingState())
    val trackingState: StateFlow<LiveTrackingState> = _trackingState.asStateFlow()

    private var locationCallback: LocationCallback? = null
    private var timerJob: Job? = null
    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private var lastRecordedLocation: Location? = null

    // Simulation reference path
    private val simulationWaypoints = listOf(
        LatLngPoint(37.7749, -122.4194),
        LatLngPoint(37.7758, -122.4180),
        LatLngPoint(37.7770, -122.4162),
        LatLngPoint(37.7785, -122.4140),
        LatLngPoint(37.7802, -122.4121),
        LatLngPoint(37.7818, -122.4095),
        LatLngPoint(37.7835, -122.4072),
        LatLngPoint(37.7850, -122.4055),
        LatLngPoint(37.7862, -122.4038),
        LatLngPoint(37.7875, -122.4015),
        LatLngPoint(37.7890, -122.3995)
    )

    fun setTravelMode(mode: TravelMode) {
        _trackingState.value = _trackingState.value.copy(selectedMode = mode)
    }

    @SuppressLint("MissingPermission")
    fun startTracking(isSimulation: Boolean = false) {
        val now = System.currentTimeMillis()
        lastRecordedLocation = null

        _trackingState.value = LiveTrackingState(
            isTracking = true,
            isPaused = false,
            isSimulationMode = isSimulation,
            selectedMode = _trackingState.value.selectedMode,
            distanceMeters = 0.0,
            durationSeconds = 0L,
            currentSpeedKmh = 0.0,
            maxSpeedKmh = 0.0,
            avgSpeedKmh = 0.0,
            currentLatLng = null,
            routePoints = emptyList(),
            startTimestamp = now
        )

        startDurationTimer()

        if (isSimulation) {
            startSimulationTracking()
        } else {
            startRealGpsTracking()
        }
    }

    fun pauseTracking() {
        if (!_trackingState.value.isTracking) return
        _trackingState.value = _trackingState.value.copy(isPaused = true, currentSpeedKmh = 0.0)
    }

    fun resumeTracking() {
        if (!_trackingState.value.isTracking) return
        _trackingState.value = _trackingState.value.copy(isPaused = false)
    }

    fun stopTracking(): LiveTrackingState {
        val finalState = _trackingState.value
        timerJob?.cancel()
        simulationJob?.cancel()
        stopRealGpsTracking()

        _trackingState.value = LiveTrackingState(
            selectedMode = finalState.selectedMode
        )
        return finalState
    }

    private fun startDurationTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val cur = _trackingState.value
                if (cur.isTracking && !cur.isPaused) {
                    val newDuration = cur.durationSeconds + 1
                    val newAvgSpeed = if (newDuration > 0) {
                        (cur.distanceMeters / 1000.0) / (newDuration / 3600.0)
                    } else 0.0

                    _trackingState.value = cur.copy(
                        durationSeconds = newDuration,
                        avgSpeedKmh = newAvgSpeed
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRealGpsTracking() {
        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(1.5f)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    onNewLocationReceived(loc)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper()
            )

            // Try to immediately get last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && _trackingState.value.currentLatLng == null) {
                    onNewLocationReceived(loc)
                }
            }
        } catch (_: SecurityException) {
            // Permission wasn't granted yet
        }
    }

    private fun stopRealGpsTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun onNewLocationReceived(loc: Location) {
        val curState = _trackingState.value
        if (!curState.isTracking || curState.isPaused) return

        val speedKmh = if (loc.hasSpeed() && loc.speed >= 0f) {
            loc.speed * 3.6
        } else {
            curState.currentSpeedKmh
        }

        var addedDistance = 0.0
        val lastLoc = lastRecordedLocation
        if (lastLoc != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                lastLoc.latitude, lastLoc.longitude,
                loc.latitude, loc.longitude,
                results
            )
            // Filter jitter
            if (results[0] >= 1.0f) {
                addedDistance = results[0].toDouble()
                lastRecordedLocation = loc
            }
        } else {
            lastRecordedLocation = loc
        }

        val point = LatLngPoint(
            latitude = loc.latitude,
            longitude = loc.longitude,
            altitude = loc.altitude,
            timestamp = System.currentTimeMillis(),
            speedKmh = speedKmh
        )

        val newDist = curState.distanceMeters + addedDistance
        val newMax = max(curState.maxSpeedKmh, speedKmh)
        val updatedPoints = curState.routePoints + point

        _trackingState.value = curState.copy(
            distanceMeters = newDist,
            currentSpeedKmh = speedKmh,
            maxSpeedKmh = newMax,
            currentLatLng = point,
            routePoints = updatedPoints
        )
    }

    private fun startSimulationTracking() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            var stepIndex = 0
            val totalSteps = simulationWaypoints.size
            val baseSpeed = when (_trackingState.value.selectedMode) {
                TravelMode.WALK -> 4.8
                TravelMode.RUN -> 10.2
                TravelMode.BICYCLE -> 18.5
                TravelMode.CAR -> 42.0
                TravelMode.TRANSIT -> 32.0
            }

            while (isActive) {
                delay(2000L)
                val curState = _trackingState.value
                if (curState.isTracking && !curState.isPaused) {
                    val wp = simulationWaypoints[stepIndex % totalSteps]
                    val jitterLat = (Math.random() - 0.5) * 0.0001
                    val jitterLng = (Math.random() - 0.5) * 0.0001
                    val simulatedPt = LatLngPoint(
                        latitude = wp.latitude + (stepIndex / totalSteps) * 0.002 + jitterLat,
                        longitude = wp.longitude + (stepIndex / totalSteps) * 0.002 + jitterLng,
                        altitude = 20.0 + (stepIndex % 5) * 2.0,
                        timestamp = System.currentTimeMillis(),
                        speedKmh = baseSpeed + (Math.random() - 0.5) * 3.0
                    )

                    val addedDist = (baseSpeed * 1000.0 / 3600.0) * 2.0
                    val newDist = curState.distanceMeters + addedDist
                    val newMax = max(curState.maxSpeedKmh, simulatedPt.speedKmh)
                    val updatedPoints = curState.routePoints + simulatedPt

                    _trackingState.value = curState.copy(
                        distanceMeters = newDist,
                        currentSpeedKmh = simulatedPt.speedKmh,
                        maxSpeedKmh = newMax,
                        currentLatLng = simulatedPt,
                        routePoints = updatedPoints
                    )

                    stepIndex++
                }
            }
        }
    }
}
