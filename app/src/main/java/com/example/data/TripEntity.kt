package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val mode: String, // from TravelMode.name
    val startTime: Long,
    val endTime: Long,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val calories: Int,
    val startAddress: String = "",
    val endAddress: String = "",
    val dateString: String, // Format: YYYY-MM-DD
    val pointsData: String // Semicolon or comma-separated coordinates: "lat,lng,alt,spd,time;..."
) {
    val travelMode: TravelMode
        get() = TravelMode.fromString(mode)

    val distanceKm: Double
        get() = distanceMeters / 1000.0

    val distanceMiles: Double
        get() = distanceKm * 0.621371

    val formattedDuration: String
        get() {
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            return if (hours > 0) {
                String.format(Locale.US, "%dh %02dm", hours, minutes)
            } else {
                String.format(Locale.US, "%dm %02ds", minutes, seconds)
            }
        }

    fun getPoints(): List<LatLngPoint> {
        if (pointsData.isBlank()) return emptyList()
        return try {
            pointsData.split(";").mapNotNull { segment ->
                val parts = segment.split(",")
                if (parts.size >= 2) {
                    LatLngPoint(
                        latitude = parts[0].toDoubleOrNull() ?: return@mapNotNull null,
                        longitude = parts[1].toDoubleOrNull() ?: return@mapNotNull null,
                        altitude = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
                        speedKmh = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0,
                        timestamp = parts.getOrNull(4)?.toLongOrNull() ?: 0L
                    )
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        fun encodePoints(points: List<LatLngPoint>): String {
            return points.joinToString(";") {
                String.format(Locale.US, "%.6f,%.6f,%.1f,%.1f,%d",
                    it.latitude, it.longitude, it.altitude, it.speedKmh, it.timestamp)
            }
        }

        fun formatDate(timeMillis: Long): String {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return formatter.format(Date(timeMillis))
        }
    }
}
