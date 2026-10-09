package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.data.LatLngPoint
import com.example.data.TravelMode
import com.example.location.LiveTrackingState
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager

    private var locationCallback: LocationCallback? = null
    private var timerJob: Job? = null
    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private var lastRecordedLocation: Location? = null
    private var isTracking = false
    private var isPaused = false
    private var isSimulation = false
    private var selectedMode = TravelMode.WALK
    private var currentDistanceMeters = 0.0
    private var currentDurationSeconds = 0L
    private var currentSpeedKmh = 0.0
    private var maxSpeedKmh = 0.0
    private var startTimestamp = 0L
    private val routePoints = mutableListOf<LatLngPoint>()

    // Simulation reference waypoints
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

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val modeStr = intent.getStringExtra(EXTRA_MODE) ?: TravelMode.WALK.name
                val sim = intent.getBooleanExtra(EXTRA_SIMULATION, false)
                startTrackingService(TravelMode.fromString(modeStr), sim)
            }
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTrackingService()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Everyday Travel Tracker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous everyday travel tracking and real-time distance updates"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isPausedNow: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val distKm = currentDistanceMeters / 1000.0
        val mins = currentDurationSeconds / 60
        val secs = currentDurationSeconds % 60
        val timeFormatted = String.format(Locale.US, "%02d:%02d", mins, secs)

        val contentText = String.format(
            Locale.US,
            "Distance: %.2f km • Time: %s • Speed: %.1f km/h",
            distKm,
            timeFormatted,
            currentSpeedKmh
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPausedNow) "Tracking Paused • ${selectedMode.title}" else "Tracking Active • ${selectedMode.title}")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        // Action: Pause or Resume
        if (isPausedNow) {
            val resumeIntent = Intent(this, LocationTrackingService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePending = PendingIntent.getService(
                this,
                1,
                resumeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
        } else {
            val pauseIntent = Intent(this, LocationTrackingService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePending = PendingIntent.getService(
                this,
                1,
                pauseIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
        }

        // Action: Stop
        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)

        return builder.build()
    }

    private fun updateNotification() {
        if (!isTracking) return
        val notification = buildNotification(isPaused)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun startTrackingService(mode: TravelMode, sim: Boolean) {
        if (isTracking) return

        isTracking = true
        isPaused = false
        isSimulation = sim
        selectedMode = mode
        currentDistanceMeters = 0.0
        currentDurationSeconds = 0L
        currentSpeedKmh = 0.0
        maxSpeedKmh = 0.0
        startTimestamp = System.currentTimeMillis()
        lastRecordedLocation = null
        routePoints.clear()

        // Start Foreground with proper service type
        val notification = buildNotification(isPausedNow = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        publishCurrentState()
        startDurationTimer()

        if (isSimulation) {
            startSimulation()
        } else {
            startRealGps()
        }
    }

    private fun pauseTracking() {
        if (!isTracking) return
        isPaused = true
        currentSpeedKmh = 0.0
        publishCurrentState()
        updateNotification()
    }

    private fun resumeTracking() {
        if (!isTracking) return
        isPaused = false
        publishCurrentState()
        updateNotification()
    }

    private fun stopTrackingService() {
        if (!isTracking) {
            stopSelf()
            return
        }

        val finalState = getCurrentState()
        isTracking = false
        isPaused = false

        timerJob?.cancel()
        simulationJob?.cancel()
        stopRealGps()

        TrackingStateHolder.emitFinishedTrip(finalState)
        TrackingStateHolder.reset()

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startDurationTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && isTracking) {
                delay(1000L)
                if (!isPaused) {
                    currentDurationSeconds++
                    publishCurrentState()
                    updateNotification()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRealGps() {
        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(1.0f)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    onNewLocation(loc)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper()
            )

            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && routePoints.isEmpty()) {
                    onNewLocation(loc)
                }
            }
        } catch (_: SecurityException) {
            // Permission missing
        }
    }

    private fun stopRealGps() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun onNewLocation(loc: Location) {
        if (!isTracking || isPaused) return

        val speed = if (loc.hasSpeed() && loc.speed >= 0f) {
            loc.speed * 3.6
        } else {
            currentSpeedKmh
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
            // Filter minor GPS noise
            if (results[0] >= 1.0f) {
                addedDistance = results[0].toDouble()
                lastRecordedLocation = loc
            }
        } else {
            lastRecordedLocation = loc
        }

        currentDistanceMeters += addedDistance
        currentSpeedKmh = speed
        maxSpeedKmh = max(maxSpeedKmh, speed)

        val pt = LatLngPoint(
            latitude = loc.latitude,
            longitude = loc.longitude,
            altitude = loc.altitude,
            timestamp = System.currentTimeMillis(),
            speedKmh = speed
        )
        routePoints.add(pt)

        publishCurrentState()
        updateNotification()
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            var stepIndex = 0
            val totalSteps = simulationWaypoints.size
            val baseSpeed = when (selectedMode) {
                TravelMode.WALK -> 4.8
                TravelMode.RUN -> 10.2
                TravelMode.BICYCLE -> 18.5
                TravelMode.CAR -> 42.0
                TravelMode.TRANSIT -> 32.0
            }

            while (isActive && isTracking) {
                delay(2000L)
                if (!isPaused) {
                    val wp = simulationWaypoints[stepIndex % totalSteps]
                    val jitterLat = (Math.random() - 0.5) * 0.0001
                    val jitterLng = (Math.random() - 0.5) * 0.0001
                    val speed = baseSpeed + (Math.random() - 0.5) * 2.5

                    val pt = LatLngPoint(
                        latitude = wp.latitude + (stepIndex / totalSteps) * 0.002 + jitterLat,
                        longitude = wp.longitude + (stepIndex / totalSteps) * 0.002 + jitterLng,
                        altitude = 20.0 + (stepIndex % 5) * 2.0,
                        timestamp = System.currentTimeMillis(),
                        speedKmh = speed
                    )

                    val addedDist = (baseSpeed * 1000.0 / 3600.0) * 2.0
                    currentDistanceMeters += addedDist
                    currentSpeedKmh = speed
                    maxSpeedKmh = max(maxSpeedKmh, speed)
                    routePoints.add(pt)

                    stepIndex++
                    publishCurrentState()
                    updateNotification()
                }
            }
        }
    }

    private fun getCurrentState(): LiveTrackingState {
        val avgSpeed = if (currentDurationSeconds > 0) {
            (currentDistanceMeters / 1000.0) / (currentDurationSeconds / 3600.0)
        } else 0.0

        return LiveTrackingState(
            isTracking = isTracking,
            isPaused = isPaused,
            isSimulationMode = isSimulation,
            selectedMode = selectedMode,
            distanceMeters = currentDistanceMeters,
            durationSeconds = currentDurationSeconds,
            currentSpeedKmh = currentSpeedKmh,
            maxSpeedKmh = maxSpeedKmh,
            avgSpeedKmh = avgSpeed,
            currentLatLng = routePoints.lastOrNull(),
            routePoints = routePoints.toList(),
            startTimestamp = startTimestamp
        )
    }

    private fun publishCurrentState() {
        TrackingStateHolder.updateState(getCurrentState())
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        simulationJob?.cancel()
        stopRealGps()
    }

    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        const val EXTRA_MODE = "EXTRA_MODE"
        const val EXTRA_SIMULATION = "EXTRA_SIMULATION"

        fun start(context: Context, mode: TravelMode, isSimulation: Boolean = false) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MODE, mode.name)
                putExtra(EXTRA_SIMULATION, isSimulation)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
