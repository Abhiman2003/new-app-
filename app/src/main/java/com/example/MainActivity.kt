package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DailyTravelScreen
import com.example.ui.screens.SaveTripDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TrackScreen
import com.example.ui.screens.TripDetailDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.NavigationTab
import com.example.viewmodel.TrackerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                EverydayTrackerApp()
            }
        }
    }
}

@Composable
fun EverydayTrackerApp(
    viewModel: TrackerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Handle back button on sub-screens
    if (state.activeTripDetail != null) {
        BackHandler { viewModel.closeTripDetails() }
    } else if (state.currentTab != NavigationTab.TRACK) {
        BackHandler { viewModel.selectTab(NavigationTab.TRACK) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = state.currentTab == NavigationTab.TRACK,
                    onClick = { viewModel.selectTab(NavigationTab.TRACK) },
                    icon = { Icon(Icons.Default.Explore, contentDescription = "Live Track") },
                    label = { Text("Track", fontWeight = if (state.currentTab == NavigationTab.TRACK) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_track"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = state.currentTab == NavigationTab.DAILY,
                    onClick = { viewModel.selectTab(NavigationTab.DAILY) },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Everyday") },
                    label = { Text("Everyday", fontWeight = if (state.currentTab == NavigationTab.DAILY) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_daily"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = state.currentTab == NavigationTab.STATS,
                    onClick = { viewModel.selectTab(NavigationTab.STATS) },
                    icon = { Icon(Icons.Default.Insights, contentDescription = "Trends") },
                    label = { Text("Trends", fontWeight = if (state.currentTab == NavigationTab.STATS) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_stats"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = state.currentTab == NavigationTab.SETTINGS,
                    onClick = { viewModel.selectTab(NavigationTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = if (state.currentTab == NavigationTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_settings"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.currentTab) {
                NavigationTab.TRACK -> TrackScreen(state = state, viewModel = viewModel)
                NavigationTab.DAILY -> DailyTravelScreen(state = state, viewModel = viewModel)
                NavigationTab.STATS -> StatsScreen(state = state, viewModel = viewModel)
                NavigationTab.SETTINGS -> SettingsScreen(state = state, viewModel = viewModel)
            }

            // Save Completed Trip Dialog
            if (state.showSaveTripDialog && state.pendingFinishedState != null) {
                SaveTripDialog(
                    state = state.pendingFinishedState!!,
                    onSave = { title, startAddr, endAddr ->
                        viewModel.saveFinishedTrip(title, startAddr, endAddr)
                    },
                    onDiscard = { viewModel.discardFinishedTrip() }
                )
            }

            // Trip Detailed View Dialog
            state.activeTripDetail?.let { trip ->
                TripDetailDialog(
                    trip = trip,
                    onClose = { viewModel.closeTripDetails() },
                    onDelete = { viewModel.deleteTrip(it) }
                )
            }
        }
    }
}
