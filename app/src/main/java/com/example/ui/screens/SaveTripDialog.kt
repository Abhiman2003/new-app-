package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.LiveTrackingState
import java.util.Locale

@Composable
fun SaveTripDialog(
    state: LiveTrackingState,
    onSave: (title: String, startAddr: String, endAddr: String) -> Unit,
    onDiscard: () -> Unit
) {
    val mode = state.selectedMode
    var title by remember { mutableStateOf("${mode.title} Journey") }
    var startAddress by remember { mutableStateOf("") }
    var endAddress by remember { mutableStateOf("") }

    val distKm = state.distanceMeters / 1000.0
    val durationMins = state.durationSeconds / 60
    val durationSecs = state.durationSeconds % 60

    AlertDialog(
        onDismissRequest = onDiscard,
        modifier = Modifier.testTag("save_trip_dialog"),
        title = {
            Text(
                text = "🎉 Journey Completed!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Quick Summary Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Distance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f km", distKm),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Duration",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${durationMins}m ${durationSecs}s",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Avg Speed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f km/h", state.avgSpeedKmh),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Trip Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_trip_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = startAddress,
                    onValueChange = { startAddress = it },
                    label = { Text("Start Location (optional)") },
                    placeholder = { Text("e.g. Home, Central Park") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = endAddress,
                    onValueChange = { endAddress = it },
                    label = { Text("Destination (optional)") },
                    placeholder = { Text("e.g. Office, Downtown") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, startAddress, endAddress) },
                modifier = Modifier.testTag("save_trip_confirm_btn")
            ) {
                Text("Save to History")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDiscard,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("save_trip_discard_btn")
            ) {
                Text("Discard")
            }
        }
    )
}
