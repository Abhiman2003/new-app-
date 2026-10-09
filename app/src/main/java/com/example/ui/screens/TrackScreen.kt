package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.TravelMode
import com.example.ui.components.TravelMapCanvas
import com.example.viewmodel.TrackerUiState
import com.example.viewmodel.TrackerViewModel
import java.util.Locale

@Composable
fun TrackScreen(
    state: TrackerUiState,
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val live = state.liveTracking
    val isTracking = live.isTracking
    val isPaused = live.isPaused
    val currentMode = live.selectedMode

    // Check location permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val granted = fine || coarse
        hasLocationPermission = granted
        viewModel.setLocationPermissionGranted(granted)
        if (granted) {
            viewModel.startTracking()
        }
    }

    fun requestPermissionsAndStart() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (hasLocationPermission) {
            viewModel.startTracking()
        } else {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Full Screen Interactive Travel Map
        TravelMapCanvas(
            routePoints = live.routePoints,
            currentLocation = live.currentLatLng,
            routeColor = currentMode.color,
            autoFollowUser = isTracking
        )

        // 2. Top Bar: Mode Selectors & Simulation Mode Indicator
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Prominent Simulation Banner if Simulation Mode is enabled
            if (state.simulationModeEnabled) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.95f),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("simulation_mode_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Test Mode",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Safety Simulation Mode Active (Emulator/Preview Test)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                }
            }

            // Foreground Service Active Notification Pill
            if (isTracking) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.9f),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("foreground_service_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Foreground Service Active • Continuous tracking & notifications enabled",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFE0F2FE)
                        )
                    }
                }
            }

            // Transport Mode Selector Chips Row
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TravelMode.entries.forEach { mode ->
                        val isSelected = currentMode == mode
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) mode.color else Color.Transparent,
                            modifier = Modifier
                                .clickable { viewModel.setTravelMode(mode) }
                                .testTag("mode_chip_${mode.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = mode.title,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Permission Notice Card (if not tracking and permission missing)
        if (!hasLocationPermission && !isTracking) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.9f)
                    .testTag("permission_rationale_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Location Permission Required",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Everyday Tracker runs a foreground service to continuously track your coordinates and calculate distance in real-time, even when you lock your screen or switch apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { requestPermissionsAndStart() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("grant_permission_btn")
                    ) {
                        Text("Grant Permissions & Start")
                    }
                }
            }
        }

        // 4. Bottom HUD & Recording Controller Card
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Live Stats HUD Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("track_stats_hud")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Status Badge & Simulation Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tracking Status Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isTracking && !isPaused -> Color(0xFF10B981).copy(alpha = 0.15f)
                                isTracking && isPaused -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = when {
                                        isTracking && !isPaused -> Color(0xFF10B981)
                                        isTracking && isPaused -> Color(0xFFF59E0B)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        isTracking && !isPaused -> "FOREGROUND SERVICE ACTIVE"
                                        isTracking && isPaused -> "SERVICE PAUSED"
                                        else -> "READY TO TRAVEL"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = when {
                                        isTracking && !isPaused -> Color(0xFF10B981)
                                        isTracking && isPaused -> Color(0xFFF59E0B)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        // Simulation Mode Toggle for Testing
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                if (!isTracking) {
                                    viewModel.toggleSimulationMode(!state.simulationModeEnabled)
                                }
                            }
                        ) {
                            Text(
                                text = "Test Route",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = state.simulationModeEnabled,
                                onCheckedChange = { if (!isTracking) viewModel.toggleSimulationMode(it) },
                                enabled = !isTracking,
                                modifier = Modifier.testTag("simulation_toggle"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFF59E0B),
                                    checkedTrackColor = Color(0xFF78350F)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Live Metrics (Speedometer, Real-time Distance, Duration)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Big Speedometer
                        Column {
                            Text(
                                text = "Live Speed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", live.currentSpeedKmh),
                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                    color = currentMode.color
                                )
                                Text(
                                    text = " km/h",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        // Real-time Distance
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Distance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val distKm = live.distanceMeters / 1000.0
                            Text(
                                text = String.format(Locale.US, "%.2f km", distKm),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Duration
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val mins = live.durationSeconds / 60
                            val secs = live.durationSeconds % 60
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", mins, secs),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Main Tracking Action Buttons
                    if (!isTracking) {
                        Button(
                            onClick = { requestPermissionsAndStart() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_tracking_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentMode.color
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "START TRACKING (FOREGROUND SERVICE)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                color = Color.White
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Pause / Resume Button
                            FilledTonalButton(
                                onClick = {
                                    if (isPaused) viewModel.resumeTracking() else viewModel.pauseTracking()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("pause_resume_btn"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPaused) "Resume" else "Pause"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPaused) "Resume" else "Pause")
                            }

                            // Finish / Stop Button
                            Button(
                                onClick = { viewModel.stopTracking() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("stop_tracking_btn"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Finish", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Finish", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
