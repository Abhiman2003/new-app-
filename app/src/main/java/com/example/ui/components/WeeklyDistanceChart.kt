package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.max

data class DayTravelStat(
    val dayLabel: String, // e.g. "Mon", "Tue"
    val dateString: String, // "YYYY-MM-DD"
    val distanceKm: Double,
    val isToday: Boolean = false
)

@Composable
fun WeeklyDistanceChart(
    days: List<DayTravelStat>,
    modifier: Modifier = Modifier,
    onDaySelected: (DayTravelStat) -> Unit = {}
) {
    var selectedDay by remember { mutableStateOf<DayTravelStat?>(days.find { it.isToday } ?: days.lastOrNull()) }
    val maxDistance = max(days.maxOfOrNull { it.distanceKm } ?: 10.0, 5.0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_distance_chart"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with Selected Day Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Travel Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedDay?.let { "${it.dayLabel} • ${it.dateString}" } ?: "Last 7 Days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                selectedDay?.let { day ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f km", day.distanceKm),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bars Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { dayStat ->
                    val isSelected = selectedDay?.dateString == dayStat.dateString
                    val heightRatio = (dayStat.distanceKm / maxDistance).toFloat().coerceIn(0.06f, 1f)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                selectedDay = dayStat
                                onDaySelected(dayStat)
                            }
                            .testTag("chart_bar_${dayStat.dayLabel}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Value label above bar
                        if (dayStat.distanceKm > 0.1) {
                            Text(
                                text = String.format(Locale.US, "%.0f", dayStat.distanceKm),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Bar
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 22.dp else 16.dp)
                                .fillMaxHeight(heightRatio)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    brush = if (isSelected) {
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                        )
                                    } else if (dayStat.isToday) {
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF10B981), Color(0xFF059669))
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF64748B), Color(0xFF475569))
                                        )
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Day label below bar
                        Text(
                            text = dayStat.dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected || dayStat.isToday) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else if (dayStat.isToday) {
                                Color(0xFF10B981)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
