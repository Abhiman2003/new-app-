package com.example.service

import com.example.location.LiveTrackingState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object TrackingStateHolder {
    private val _trackingState = MutableStateFlow(LiveTrackingState())
    val trackingState: StateFlow<LiveTrackingState> = _trackingState.asStateFlow()

    private val _finishedTripEvent = MutableSharedFlow<LiveTrackingState>(extraBufferCapacity = 1)
    val finishedTripEvent: SharedFlow<LiveTrackingState> = _finishedTripEvent.asSharedFlow()

    fun updateState(newState: LiveTrackingState) {
        _trackingState.value = newState
    }

    fun emitFinishedTrip(finished: LiveTrackingState) {
        _finishedTripEvent.tryEmit(finished)
    }

    fun reset() {
        _trackingState.value = LiveTrackingState(
            selectedMode = _trackingState.value.selectedMode
        )
    }
}
