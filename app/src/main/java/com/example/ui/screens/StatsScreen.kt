package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TravelMode
import com.example.ui.components.WeeklyDistanceChart
import com.example.viewmodel.TrackerUiState
import com.example.viewmodel.TrackerViewModel
import java.util.Locale

@Composable
fun StatsScreen(
    state: TrackerUiState,
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val allTrips = state.allTrips
    val totalKm = allTrips.sumOf { it.distanceKm }
    val totalSeconds = allTrips.sumOf { it.durationSeconds }
    val totalCalories = allTrips.sumOf { it.calories }
    val totalTripsCount = allTrips.size

    val distinctDatesCount = allTrips.map { it.dateString }.distinct().size.coerceAtLeast(1)
    val dailyAvgKm = totalKm / distinctDatesCount

    // Mode Breakdown
    val modeDistances = TravelMode.entries.associateWith { mode ->
        allTrips.filter { it.mode == mode.name }.sumOf { it.distanceKm }
    }
    val ecoKm = (modeDistances[TravelMode.WALK] ?: 0.0) +
            (modeDistances[TravelMode.RUN] ?: 0.0) +
            (modeDistances[TravelMode.BICYCLE] ?: 0.0)
    val co2SavedKg = ecoKm * 0.171 // approx 171g CO2 saved per km compared to driving

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("stats_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Travel Trends & Analytics",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 1. Weekly Travel Distance Chart
        item {
            WeeklyDistanceChart(
                days = state.weeklyStats,
                onDaySelected = { dayStat ->
                    viewModel.selectDate(dayStat.dateString)
                }
            )
        }

        // 2. Lifetime Travel Summary Grid
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Everyday Travel Records",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCardMini("Total Travel", String.format(Locale.US, "%.1f km", totalKm), MaterialTheme.colorScheme.primary)
                        StatCardMini("Daily Average", String.format(Locale.US, "%.1f km", dailyAvgKm), Color(0xFF10B981))
                        StatCardMini("Total Trips", "$totalTripsCount", Color(0xFFF59E0B))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val hours = totalSeconds / 3600
                        val mins = (totalSeconds % 3600) / 60
                        StatCardMini("Active Time", "${hours}h ${mins}m", Color(0xFF8B5CF6))
                        StatCardMini("Calories Burned", "$totalCalories kcal", Color(0xFFEC4899))
                        val maxDayKm = state.weeklyStats.maxOfOrNull { it.distanceKm } ?: 0.0
                        StatCardMini("Best Day", String.format(Locale.US, "%.1f km", maxDayKm), Color(0xFF06B6D4))
                    }
                }
            }
        }

        // 3. Travel Mode Breakdown
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Travel Mode Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    TravelMode.entries.forEach { mode ->
                        val dist = modeDistances[mode] ?: 0.0
                        val fraction = if (totalKm > 0) (dist / totalKm).toFloat() else 0f
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = mode.icon,
                                        contentDescription = mode.title,
                                        tint = mode.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%.1f km (%.0f%%)", dist, fraction * 100f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = mode.color,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
        }

        // 4. Eco Travel & Carbon Offset Badge
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Nature,
                                contentDescription = "Eco",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Eco Travel Impact",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = String.format(Locale.US, "You've traveled %.1f km on foot/bike, saving ~%.1f kg of CO2!", ecoKm, co2SavedKg),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun StatCardMini(
    label: String,
    value: String,
    accentColor: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = accentColor
        )
    }
}
