package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LatLngPoint
import com.example.data.TravelMode
import com.example.data.TripEntity
import com.example.data.TripRepository
import com.example.service.LocationTrackingService
import com.example.service.TrackingStateHolder
import com.example.ui.components.DayTravelStat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TripRepository

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TripRepository(database.tripDao())

        val today = TripEntity.formatDate(System.currentTimeMillis())
        _uiState.update { it.copy(selectedDateString = today) }

        // Seed sample trips if DB is fresh
        viewModelScope.launch {
            repository.seedSampleTripsIfEmpty()
        }

        // Collect all trips and aggregate daily statistics
        viewModelScope.launch {
            repository.allTrips.collectLatest { trips ->
                updateTripAggregates(trips)
            }
        }

        // Collect live tracking state continuously from the Foreground Service
        viewModelScope.launch {
            TrackingStateHolder.trackingState.collectLatest { liveState ->
                _uiState.update { it.copy(liveTracking = liveState) }
            }
        }

        // Listen for finished trip events from Foreground Service
        viewModelScope.launch {
            TrackingStateHolder.finishedTripEvent.collectLatest { finishedState ->
                if (finishedState.distanceMeters > 5.0 || finishedState.durationSeconds > 5L) {
                    _uiState.update {
                        it.copy(
                            pendingFinishedState = finishedState,
                            showSaveTripDialog = true
                        )
                    }
                }
            }
        }
    }

    private fun updateTripAggregates(trips: List<TripEntity>) {
        val todayStr = TripEntity.formatDate(System.currentTimeMillis())
        val selectedStr = _uiState.value.selectedDateString.ifEmpty { todayStr }

        val todayTrips = trips.filter { it.dateString == todayStr }
        val selectedTrips = trips.filter { it.dateString == selectedStr }

        val todayDistance = todayTrips.sumOf { it.distanceKm }
        val todayDuration = todayTrips.sumOf { it.durationSeconds }
        val todayCalories = todayTrips.sumOf { it.calories }

        // Compute 7-day stats
        val weekly = compute7DayStats(trips)

        _uiState.update {
            it.copy(
                allTrips = trips,
                selectedDateTrips = selectedTrips,
                todayDistanceKm = todayDistance,
                todayDurationSeconds = todayDuration,
                todayCalories = todayCalories,
                todayTripsCount = todayTrips.size,
                weeklyStats = weekly
            )
        }
    }

    private fun compute7DayStats(trips: List<TripEntity>): List<DayTravelStat> {
        val result = mutableListOf<DayTravelStat>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        // Days from 6 days ago up to today
        for (offset in -6..0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, offset)
            val dateStr = TripEntity.formatDate(c.timeInMillis)
            val dayLabel = dayFormat.format(c.time)
            val isToday = offset == 0

            val dayTrips = trips.filter { it.dateString == dateStr }
            val dist = dayTrips.sumOf { it.distanceKm }

            result.add(
                DayTravelStat(
                    dayLabel = dayLabel,
                    dateString = dateStr,
                    distanceKm = dist,
                    isToday = isToday
                )
            )
        }
        return result
    }

    fun selectTab(tab: NavigationTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun selectDate(dateString: String) {
        val selectedTrips = _uiState.value.allTrips.filter { it.dateString == dateString }
        _uiState.update {
            it.copy(
                selectedDateString = dateString,
                selectedDateTrips = selectedTrips
            )
        }
    }

    fun setTravelMode(mode: TravelMode) {
        val cur = _uiState.value.liveTracking
        val updated = cur.copy(selectedMode = mode)
        TrackingStateHolder.updateState(updated)
        _uiState.update { it.copy(liveTracking = updated) }
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
    }

    fun toggleSimulationMode(enabled: Boolean) {
        _uiState.update { it.copy(simulationModeEnabled = enabled) }
    }

    fun startTracking() {
        val isSimulation = _uiState.value.simulationModeEnabled
        val mode = _uiState.value.liveTracking.selectedMode
        LocationTrackingService.start(getApplication(), mode, isSimulation)
    }

    fun pauseTracking() {
        LocationTrackingService.pause(getApplication())
    }

    fun resumeTracking() {
        LocationTrackingService.resume(getApplication())
    }

    fun stopTracking() {
        LocationTrackingService.stop(getApplication())
    }

    fun saveFinishedTrip(title: String, startAddress: String, endAddress: String) {
        val pending = _uiState.value.pendingFinishedState ?: return
        viewModelScope.launch {
            val mode = pending.selectedMode
            val distKm = pending.distanceMeters / 1000.0
            val durationHours = pending.durationSeconds / 3600.0
            // Estimate calories based on MET
            val calories = (mode.metRate * 70.0 * durationHours).toInt().coerceAtLeast((distKm * 50).toInt())
            val dateStr = TripEntity.formatDate(pending.startTimestamp.takeIf { it > 0 } ?: System.currentTimeMillis())

            val defaultTitle = title.ifBlank {
                "${mode.title} Journey"
            }

            val entity = TripEntity(
                title = defaultTitle,
                mode = mode.name,
                startTime = pending.startTimestamp,
                endTime = System.currentTimeMillis(),
                distanceMeters = pending.distanceMeters,
                durationSeconds = pending.durationSeconds,
                avgSpeedKmh = pending.avgSpeedKmh,
                maxSpeedKmh = pending.maxSpeedKmh,
                calories = calories,
                startAddress = startAddress,
                endAddress = endAddress,
                dateString = dateStr,
                pointsData = TripEntity.encodePoints(pending.routePoints)
            )

            repository.insertTrip(entity)

            _uiState.update {
                it.copy(
                    showSaveTripDialog = false,
                    pendingFinishedState = null
                )
            }
        }
    }

    fun discardFinishedTrip() {
        _uiState.update {
            it.copy(
                showSaveTripDialog = false,
                pendingFinishedState = null
            )
        }
    }

    fun updateDailyGoal(goalKm: Double) {
        _uiState.update { it.copy(dailyGoalKm = goalKm.coerceIn(1.0, 50.0)) }
    }

    fun toggleUnit() {
        _uiState.update { it.copy(useMiles = !it.useMiles) }
    }

    fun openTripDetails(trip: TripEntity) {
        _uiState.update { it.copy(activeTripDetail = trip) }
    }

    fun closeTripDetails() {
        _uiState.update { it.copy(activeTripDetail = null) }
    }

    fun deleteTrip(trip: TripEntity) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
            if (_uiState.value.activeTripDetail?.id == trip.id) {
                _uiState.update { it.copy(activeTripDetail = null) }
            }
        }
    }

    fun addQuickManualTrip(
        mode: TravelMode,
        distanceKm: Double,
        durationMinutes: Int,
        title: String,
        dateString: String = _uiState.value.selectedDateString
    ) {
        viewModelScope.launch {
            val distMeters = distanceKm * 1000.0
            val durationSecs = durationMinutes * 60L
            val hours = durationMinutes / 60.0
            val speed = if (hours > 0) distanceKm / hours else 0.0
            val calories = (mode.metRate * 70.0 * hours).toInt().coerceAtLeast(40)

            val baseLat = 37.7749
            val baseLng = -122.4194
            val mockPoints = listOf(
                LatLngPoint(baseLat, baseLng, 15.0, System.currentTimeMillis() - durationSecs * 1000, speed),
                LatLngPoint(baseLat + 0.005 * distanceKm, baseLng + 0.005 * distanceKm, 20.0, System.currentTimeMillis(), speed)
            )

            val entity = TripEntity(
                title = title.ifBlank { "Quick ${mode.title}" },
                mode = mode.name,
                startTime = System.currentTimeMillis() - durationSecs * 1000,
                endTime = System.currentTimeMillis(),
                distanceMeters = distMeters,
                durationSeconds = durationSecs,
                avgSpeedKmh = speed,
                maxSpeedKmh = speed * 1.25,
                calories = calories,
                startAddress = "Manual Log",
                endAddress = "Destination",
                dateString = dateString,
                pointsData = TripEntity.encodePoints(mockPoints)
            )

            repository.insertTrip(entity)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}
