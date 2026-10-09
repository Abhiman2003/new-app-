package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TravelMode
import com.example.data.TripEntity
import com.example.ui.components.GoalProgressRing
import com.example.ui.components.TripCard
import com.example.viewmodel.TrackerUiState
import com.example.viewmodel.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DailyTravelScreen(
    state: TrackerUiState,
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate = state.selectedDateString
    val todayDate = remember { TripEntity.formatDate(System.currentTimeMillis()) }
    val isToday = selectedDate == todayDate

    val tripsForDay = state.selectedDateTrips
    val dayTotalDistanceKm = tripsForDay.sumOf { it.distanceKm }
    val dayTotalDurationSecs = tripsForDay.sumOf { it.durationSeconds }
    val dayTotalCalories = tripsForDay.sumOf { it.calories }

    var showManualAddDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // 1. Date Selector Strip
                DateSelectorHeader(
                    selectedDate = selectedDate,
                    onSelectDate = { viewModel.selectDate(it) }
                )
            }

            // 2. Everyday Travel Summary Card
            item {
                EverydaySummaryCard(
                    distanceKm = dayTotalDistanceKm,
                    goalKm = state.dailyGoalKm,
                    durationSeconds = dayTotalDurationSecs,
                    calories = dayTotalCalories,
                    tripsCount = tripsForDay.size
                )
            }

            // 3. Section Header: Trips on Selected Date
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isToday) "Today's Journeys" else "Journeys on this Day",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${tripsForDay.size} trips logged",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { showManualAddDialog = true },
                        modifier = Modifier.testTag("manual_log_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Past Trip")
                    }
                }
            }

            // 4. Trips List or Empty State
            if (tripsForDay.isEmpty()) {
                item {
                    EmptyDayState(
                        isToday = isToday,
                        onStartTracking = { viewModel.selectTab(com.example.viewmodel.NavigationTab.TRACK) },
                        onLogManual = { showManualAddDialog = true }
                    )
                }
            } else {
                items(tripsForDay, key = { it.id }) { trip ->
                    TripCard(
                        trip = trip,
                        onClick = { viewModel.openTripDetails(trip) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showManualAddDialog) {
        ManualAddTripDialog(
            dateString = selectedDate,
            onDismiss = { showManualAddDialog = false },
            onAdd = { mode, distKm, durationMins, title ->
                viewModel.addQuickManualTrip(mode, distKm, durationMins, title, selectedDate)
                showManualAddDialog = false
            }
        )
    }
}

@Composable
private fun DateSelectorHeader(
    selectedDate: String,
    onSelectDate: (String) -> Unit
) {
    // Generate dates for current week or recent 10 days
    val dates = remember {
        val list = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance()
        val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val dateNumFmt = SimpleDateFormat("d MMM", Locale.getDefault())

        for (i in 0 downTo -9) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, i)
            val dStr = TripEntity.formatDate(c.timeInMillis)
            val label = when (i) {
                0 -> "Today"
                -1 -> "Yesterday"
                else -> "${dayFmt.format(c.time)}, ${dateNumFmt.format(c.time)}"
            }
            list.add(Pair(dStr, label))
        }
        list
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Everyday Travel Date",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal date chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dates.forEach { (dateStr, label) ->
                    val isSelected = selectedDate == dateStr
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier
                            .clickable { onSelectDate(dateStr) }
                            .testTag("date_chip_$dateStr")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EverydaySummaryCard(
    distanceKm: Double,
    goalKm: Double,
    durationSeconds: Long,
    calories: Int,
    tripsCount: Int
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("everyday_summary_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Progress Goal Gauge
                GoalProgressRing(
                    currentDistanceKm = distanceKm,
                    goalDistanceKm = goalKm,
                    size = 135.dp
                )

                Spacer(modifier = Modifier.width(20.dp))

                // Everyday Key Metrics Grid
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EverydayMiniStat(
                        icon = Icons.Default.Route,
                        iconTint = MaterialTheme.colorScheme.primary,
                        label = "Total Travel",
                        value = String.format(Locale.US, "%.2f km", distanceKm)
                    )

                    val hours = durationSeconds / 3600
                    val mins = (durationSeconds % 3600) / 60
                    val durationText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                    EverydayMiniStat(
                        icon = Icons.Default.Schedule,
                        iconTint = Color(0xFF10B981),
                        label = "Travel Time",
                        value = durationText
                    )

                    EverydayMiniStat(
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = Color(0xFFF59E0B),
                        label = "Calories",
                        value = "$calories kcal"
                    )

                    EverydayMiniStat(
                        icon = Icons.Default.Explore,
                        iconTint = Color(0xFF8B5CF6),
                        label = "Trips Taken",
                        value = "$tripsCount journeys"
                    )
                }
            }
        }
    }
}

@Composable
private fun EverydayMiniStat(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = iconTint.copy(alpha = 0.15f),
            modifier = Modifier.size(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun EmptyDayState(
    isToday: Boolean,
    onStartTracking: () -> Unit,
    onLogManual: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isToday) "No travel recorded today yet" else "No travel logged for this day",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isToday) "Head outdoors and start tracking your journey or log a trip manually!" else "You can add a past trip or switch dates to view your travel history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isToday) {
                    Button(
                        onClick = onStartTracking,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("empty_start_tracking_btn")
                    ) {
                        Text("Start Live Track")
                    }
                }
                OutlinedButton(
                    onClick = onLogManual,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Log Trip Manually")
                }
            }
        }
    }
}

@Composable
private fun ManualAddTripDialog(
    dateString: String,
    onDismiss: () -> Unit,
    onAdd: (mode: TravelMode, distanceKm: Double, durationMins: Int, title: String) -> Unit
) {
    var title by remember { mutableStateOf("Everyday Commute") }
    var selectedMode by remember { mutableStateOf(TravelMode.WALK) }
    var distanceInput by remember { mutableStateOf("3.5") }
    var durationInput by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Past Everyday Journey") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Logging for date: $dateString",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Chips
                Text("Travel Mode", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TravelMode.entries.forEach { mode ->
                        val isSelected = selectedMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) mode.color else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedMode = mode }
                        ) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = distanceInput,
                        onValueChange = { distanceInput = it },
                        label = { Text("Distance (km)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = { durationInput = it },
                        label = { Text("Time (mins)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dist = distanceInput.toDoubleOrNull() ?: 2.0
                    val dur = durationInput.toIntOrNull() ?: 20
                    onAdd(selectedMode, dist, dur, title)
                }
            ) {
                Text("Add Journey")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
