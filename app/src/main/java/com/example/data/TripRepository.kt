package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class TripRepository(private val tripDao: TripDao) {

    val allTrips: Flow<List<TripEntity>> = tripDao.getAllTrips()

    fun getTripsForDate(dateString: String): Flow<List<TripEntity>> {
        return tripDao.getTripsByDate(dateString)
    }

    fun getTripsInRange(startDate: String, endDate: String): Flow<List<TripEntity>> {
        return tripDao.getTripsInRange(startDate, endDate)
    }

    fun getTripById(id: Long): Flow<TripEntity?> {
        return tripDao.getTripById(id)
    }

    suspend fun insertTrip(trip: TripEntity): Long = withContext(Dispatchers.IO) {
        tripDao.insertTrip(trip)
    }

    suspend fun deleteTrip(trip: TripEntity) = withContext(Dispatchers.IO) {
        tripDao.deleteTrip(trip)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        tripDao.deleteAllTrips()
    }

    suspend fun seedSampleTripsIfEmpty() = withContext(Dispatchers.IO) {
        if (tripDao.getTripsCount() > 0) return@withContext

        val now = Calendar.getInstance()
        val sampleList = mutableListOf<TripEntity>()

        // Helper to format date offset in days
        fun getDateString(dayOffset: Int): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, dayOffset)
            return TripEntity.formatDate(cal.timeInMillis)
        }

        // 1. Today Morning Walk
        val todayStr = getDateString(0)
        val todayWalkPoints = generateRoutePoints(
            startLat = 37.7749,
            startLng = -122.4194,
            latDeltas = listOf(0.002, 0.003, 0.001, -0.001, -0.003, -0.002),
            lngDeltas = listOf(0.001, 0.004, 0.006, 0.005, 0.002, 0.000),
            baseSpeed = 5.2
        )
        sampleList.add(
            TripEntity(
                title = "Morning Coffee Stroll",
                mode = TravelMode.WALK.name,
                startTime = System.currentTimeMillis() - 4 * 3600 * 1000,
                endTime = System.currentTimeMillis() - 3 * 3600 * 1000 + 15 * 60 * 1000,
                distanceMeters = 2450.0,
                durationSeconds = 1890,
                avgSpeedKmh = 4.8,
                maxSpeedKmh = 6.1,
                calories = 142,
                startAddress = "Home • 5th & Market",
                endAddress = "Blue Bottle Cafe",
                dateString = todayStr,
                pointsData = TripEntity.encodePoints(todayWalkPoints)
            )
        )

        // 2. Today Afternoon Bike Commute
        val todayBikePoints = generateRoutePoints(
            startLat = 37.7780,
            startLng = -122.4100,
            latDeltas = listOf(0.006, 0.012, 0.019, 0.024, 0.030),
            lngDeltas = listOf(-0.003, -0.008, -0.014, -0.018, -0.022),
            baseSpeed = 19.5
        )
        sampleList.add(
            TripEntity(
                title = "Afternoon City Cycle",
                mode = TravelMode.BICYCLE.name,
                startTime = System.currentTimeMillis() - 2 * 3600 * 1000,
                endTime = System.currentTimeMillis() - 3600 * 1000 - 35 * 60 * 1000,
                distanceMeters = 5820.0,
                durationSeconds = 1500,
                avgSpeedKmh = 16.4,
                maxSpeedKmh = 24.8,
                calories = 265,
                startAddress = "Work Campus",
                endAddress = "Mission Dolores Park",
                dateString = todayStr,
                pointsData = TripEntity.encodePoints(todayBikePoints)
            )
        )

        // 3. Yesterday Commute & Evening Run
        val yesterdayStr = getDateString(-1)
        val yestRunPoints = generateRoutePoints(
            startLat = 37.7690,
            startLng = -122.4467,
            latDeltas = listOf(0.003, 0.008, 0.012, 0.008, 0.002, 0.000),
            lngDeltas = listOf(0.005, 0.009, 0.006, -0.002, -0.004, 0.000),
            baseSpeed = 10.2
        )
        sampleList.add(
            TripEntity(
                title = "Golden Gate Park Run",
                mode = TravelMode.RUN.name,
                startTime = System.currentTimeMillis() - 26 * 3600 * 1000,
                endTime = System.currentTimeMillis() - 25 * 3600 * 1000 - 20 * 60 * 1000,
                distanceMeters = 4920.0,
                durationSeconds = 2400,
                avgSpeedKmh = 9.8,
                maxSpeedKmh = 12.4,
                calories = 380,
                startAddress = "Panhandle Entrance",
                endAddress = "Conservatory of Flowers",
                dateString = yesterdayStr,
                pointsData = TripEntity.encodePoints(yestRunPoints)
            )
        )

        val yestDrivePoints = generateRoutePoints(
            startLat = 37.7700,
            startLng = -122.4200,
            latDeltas = listOf(0.015, 0.035, 0.060, 0.090),
            lngDeltas = listOf(0.020, 0.040, 0.070, 0.110),
            baseSpeed = 45.0
        )
        sampleList.add(
            TripEntity(
                title = "Highway Commute to Bay",
                mode = TravelMode.CAR.name,
                startTime = System.currentTimeMillis() - 32 * 3600 * 1000,
                endTime = System.currentTimeMillis() - 31 * 3600 * 1000 - 30 * 60 * 1000,
                distanceMeters = 14300.0,
                durationSeconds = 1800,
                avgSpeedKmh = 42.1,
                maxSpeedKmh = 68.0,
                calories = 95,
                startAddress = "Downtown",
                endAddress = "East Bay Bridge",
                dateString = yesterdayStr,
                pointsData = TripEntity.encodePoints(yestDrivePoints)
            )
        )

        // 4. Past few days for rich weekly analytics
        for (i in 2..6) {
            val pastDateStr = getDateString(-i)
            val dist = 3200.0 + (i * 1150.0)
            val mode = when (i % 3) {
                0 -> TravelMode.WALK
                1 -> TravelMode.BICYCLE
                else -> TravelMode.RUN
            }
            sampleList.add(
                TripEntity(
                    title = "Daily Routine ${mode.title}",
                    mode = mode.name,
                    startTime = System.currentTimeMillis() - (i * 24 + 10) * 3600 * 1000L,
                    endTime = System.currentTimeMillis() - (i * 24 + 9) * 3600 * 1000L,
                    distanceMeters = dist,
                    durationSeconds = (dist / (mode.metRate * 1.5)).toLong().coerceIn(900L, 3600L),
                    avgSpeedKmh = when (mode) {
                        TravelMode.WALK -> 4.9
                        TravelMode.RUN -> 9.5
                        TravelMode.BICYCLE -> 17.2
                        else -> 30.0
                    },
                    maxSpeedKmh = 20.0,
                    calories = (dist * 0.06).toInt() + 100,
                    startAddress = "District $i",
                    endAddress = "Terminal $i",
                    dateString = pastDateStr,
                    pointsData = TripEntity.encodePoints(
                        generateRoutePoints(37.77 + i * 0.005, -122.42 - i * 0.005, listOf(0.002, 0.006, 0.010), listOf(0.003, 0.007, 0.012), 12.0)
                    )
                )
            )
        }

        tripDao.insertAll(sampleList)
    }

    private fun generateRoutePoints(
        startLat: Double,
        startLng: Double,
        latDeltas: List<Double>,
        lngDeltas: List<Double>,
        baseSpeed: Double
    ): List<LatLngPoint> {
        val points = mutableListOf<LatLngPoint>()
        var curLat = startLat
        var curLng = startLng
        var curTime = System.currentTimeMillis() - 3600 * 1000

        points.add(LatLngPoint(curLat, curLng, 15.0, curTime, baseSpeed * 0.8))

        for (i in latDeltas.indices) {
            curLat = startLat + latDeltas[i]
            curLng = startLng + lngDeltas[i]
            curTime += 180 * 1000
            val speed = baseSpeed + (i % 3 - 1) * 1.5
            points.add(LatLngPoint(curLat, curLng, 18.0 + i * 2, curTime, speed.coerceAtLeast(1.0)))
        }
        return points
    }
}
