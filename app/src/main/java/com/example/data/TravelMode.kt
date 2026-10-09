package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.RouteCycle
import com.example.ui.theme.RouteDrive
import com.example.ui.theme.RouteRun
import com.example.ui.theme.RouteTransit
import com.example.ui.theme.RouteWalk

enum class TravelMode(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val metRate: Double // Metabolic equivalent for calorie estimation (kcal/kg/hr)
) {
    WALK("Walk", Icons.AutoMirrored.Filled.DirectionsWalk, RouteWalk, 3.5),
    RUN("Run", Icons.AutoMirrored.Filled.DirectionsRun, RouteRun, 8.0),
    BICYCLE("Cycle", Icons.AutoMirrored.Filled.DirectionsBike, RouteCycle, 6.0),
    CAR("Drive", Icons.Default.DirectionsCar, RouteDrive, 1.2),
    TRANSIT("Transit", Icons.Default.DirectionsTransit, RouteTransit, 1.5);

    companion object {
        fun fromString(value: String): TravelMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: WALK
        }
    }
}
