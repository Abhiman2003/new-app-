package com.example.viewmodel

import com.example.data.TravelMode
import com.example.data.TripEntity
import com.example.location.LiveTrackingState
import com.example.ui.components.DayTravelStat

enum class NavigationTab(val title: String) {
    TRACK("Live Track"),
    DAILY("Everyday"),
    STATS("Trends"),
    SETTINGS("Settings")
}

data class TrackerUiState(
    val currentTab: NavigationTab = NavigationTab.TRACK,
    val selectedDateString: String = "",
    val selectedDateTrips: List<TripEntity> = emptyList(),
    val allTrips: List<TripEntity> = emptyList(),
    val todayDistanceKm: Double = 0.0,
    val todayDurationSeconds: Long = 0L,
    val todayCalories: Int = 0,
    val todayTripsCount: Int = 0,
    val dailyGoalKm: Double = 8.0,
    val weeklyStats: List<DayTravelStat> = emptyList(),
    val liveTracking: LiveTrackingState = LiveTrackingState(),
    val activeTripDetail: TripEntity? = null,
    val showSaveTripDialog: Boolean = false,
    val pendingFinishedState: LiveTrackingState? = null,
    val useMiles: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val simulationModeEnabled: Boolean = false
)
