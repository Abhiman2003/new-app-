package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.LatLngPoint
import com.example.data.TravelMode
import com.example.data.TripEntity
import com.example.location.LiveTrackingState
import com.example.service.LocationTrackingService
import com.example.service.TrackingStateHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Everyday Tracker", appName)
  }

  @Test
  fun `verify LatLngPoint distance calculation`() {
    val p1 = LatLngPoint(37.7749, -122.4194)
    val p2 = LatLngPoint(37.7849, -122.4094)
    val dist = p1.distanceTo(p2)
    assertTrue("Distance should be around 1400m", dist in 1000.0..2000.0)
  }

  @Test
  fun `verify TripEntity encoding and decoding`() {
    val points = listOf(
      LatLngPoint(37.7749, -122.4194, 10.0, 1000L, 5.0),
      LatLngPoint(37.7759, -122.4184, 12.0, 2000L, 5.5)
    )
    val encoded = TripEntity.encodePoints(points)
    val trip = TripEntity(
      title = "Morning Stroll",
      mode = TravelMode.WALK.name,
      startTime = 1000L,
      endTime = 2000L,
      distanceMeters = 1500.0,
      durationSeconds = 600,
      avgSpeedKmh = 5.0,
      maxSpeedKmh = 6.0,
      calories = 80,
      dateString = "2026-10-09",
      pointsData = encoded
    )

    assertEquals(1.5, trip.distanceKm, 0.001)
    val decoded = trip.getPoints()
    assertEquals(2, decoded.size)
    assertEquals(37.7749, decoded[0].latitude, 0.0001)
  }

  @Test
  fun `verify TrackingStateHolder updates and resets`() {
    val testState = LiveTrackingState(
      isTracking = true,
      distanceMeters = 2450.0,
      durationSeconds = 1200L,
      currentSpeedKmh = 5.2
    )
    TrackingStateHolder.updateState(testState)
    assertEquals(2450.0, TrackingStateHolder.trackingState.value.distanceMeters, 0.01)
    assertTrue(TrackingStateHolder.trackingState.value.isTracking)

    TrackingStateHolder.reset()
    assertEquals(0.0, TrackingStateHolder.trackingState.value.distanceMeters, 0.01)
    assertEquals(false, TrackingStateHolder.trackingState.value.isTracking)
  }
}
