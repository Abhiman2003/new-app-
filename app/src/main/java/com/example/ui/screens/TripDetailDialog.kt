package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.TripEntity
import com.example.ui.components.TravelMapCanvas
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripDetailDialog(
    trip: TripEntity,
    onClose: () -> Unit,
    onDelete: (TripEntity) -> Unit
) {
    val mode = trip.travelMode
    val points = trip.getPoints()
    val timeFormatter = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
    val startStr = timeFormatter.format(Date(trip.startTime))

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("trip_detail_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = mode.color.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = mode.title,
                                    tint = mode.color,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = trip.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = startStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("trip_detail_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Route Map
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    TravelMapCanvas(
                        routePoints = points,
                        currentLocation = points.firstOrNull(),
                        routeColor = mode.color,
                        autoFollowUser = false
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detailed Metrics Grid
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailMetric("Distance", String.format(Locale.US, "%.2f km", trip.distanceKm), MaterialTheme.colorScheme.primary)
                            DetailMetric("Duration", trip.formattedDuration, MaterialTheme.colorScheme.onSurface)
                            DetailMetric("Avg Speed", String.format(Locale.US, "%.1f km/h", trip.avgSpeedKmh), mode.color)
                            DetailMetric("Calories", "${trip.calories} kcal", MaterialTheme.colorScheme.tertiary)
                        }

                        if (trip.startAddress.isNotBlank() || trip.endAddress.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (trip.startAddress.isNotBlank()) trip.startAddress else "Starting Point",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "to",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp).size(12.dp)
                                )
                                Text(
                                    text = if (trip.endAddress.isNotBlank()) trip.endAddress else "Destination",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { onDelete(trip) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("delete_trip_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Trip")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetric(
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
            color = tint
        )
    }
}
