package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LatLngPoint
import com.example.data.TripEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripCard(
    trip: TripEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mode = trip.travelMode
    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val startFormatted = timeFormatter.format(Date(trip.startTime))
    val endFormatted = timeFormatter.format(Date(trip.endTime))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("trip_card_${trip.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top Row: Mode Badge + Title + Mini Sparkline
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = mode.color.copy(alpha = 0.2f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = mode.title,
                            tint = mode.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trip.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "$startFormatted - $endFormatted • ${mode.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Mini Path Canvas Preview
                val points = trip.getPoints()
                if (points.size >= 2) {
                    MiniRouteCanvas(
                        points = points,
                        routeColor = mode.color,
                        modifier = Modifier
                            .size(width = 54.dp, height = 36.dp)
                            .padding(end = 4.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Row: Distance | Duration | Avg Speed | Calories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricItem(
                    label = "Distance",
                    value = String.format(Locale.US, "%.2f km", trip.distanceKm),
                    highlightColor = MaterialTheme.colorScheme.primary
                )
                MetricItem(
                    label = "Time",
                    value = trip.formattedDuration
                )
                MetricItem(
                    label = "Avg Speed",
                    value = String.format(Locale.US, "%.1f km/h", trip.avgSpeedKmh)
                )
                MetricItem(
                    label = "Calories",
                    value = "${trip.calories} kcal"
                )
            }

            // Optional Route Addresses
            if (trip.startAddress.isNotBlank() || trip.endAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (trip.startAddress.isNotBlank()) trip.startAddress else "Start",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "to",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(12.dp)
                    )
                    Text(
                        text = if (trip.endAddress.isNotBlank()) trip.endAddress else "End",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    highlightColor: Color? = null
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MiniRouteCanvas(
    points: List<LatLngPoint>,
    routeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLng = points.minOf { it.longitude }
        val maxLng = points.maxOf { it.longitude }

        val latSpan = (maxLat - minLat).coerceAtLeast(0.0001)
        val lngSpan = (maxLng - minLng).coerceAtLeast(0.0001)

        val padding = 4f
        val w = size.width - padding * 2
        val h = size.height - padding * 2

        val path = Path()
        points.forEachIndexed { index, pt ->
            val x = padding + ((pt.longitude - minLng) / lngSpan * w).toFloat()
            val y = padding + (h - (pt.latitude - minLat) / latSpan * h).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = routeColor,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
