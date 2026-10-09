package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LatLngPoint
import kotlin.math.cos
import kotlin.math.sin

enum class MapThemeMode {
    DARK_NAV,
    LIGHT_CITY,
    EMERALD_OUTDOORS
}

@Composable
fun TravelMapCanvas(
    routePoints: List<LatLngPoint>,
    currentLocation: LatLngPoint?,
    modifier: Modifier = Modifier,
    routeColor: Color = Color(0xFF38BDF8),
    autoFollowUser: Boolean = true,
    showMilestones: Boolean = true
) {
    // Zoom and pan state
    var zoomLevel by remember { mutableFloatStateOf(14.5f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var centerLat by remember { mutableDoubleStateOf(37.7749) }
    var centerLng by remember { mutableDoubleStateOf(-122.4194) }
    var mapTheme by remember { mutableStateOf(MapThemeMode.DARK_NAV) }

    // Pulsing radar animation for user's current location
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadiusFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Center on current location when it updates or when requested
    LaunchedEffect(currentLocation, autoFollowUser) {
        if (autoFollowUser && currentLocation != null) {
            centerLat = currentLocation.latitude
            centerLng = currentLocation.longitude
            panOffsetX = 0f
            panOffsetY = 0f
        } else if (centerLat == 37.7749 && routePoints.isNotEmpty()) {
            val avgLat = routePoints.map { it.latitude }.average()
            val avgLng = routePoints.map { it.longitude }.average()
            centerLat = avgLat
            centerLng = avgLng
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("travel_map_canvas")
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        panOffsetX += pan.x
                        panOffsetY += pan.y
                        zoomLevel = (zoomLevel * zoom).coerceIn(10f, 20f)
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW / 2f + panOffsetX
            val centerY = canvasH / 2f + panOffsetY

            // Sizing: pixels per degree at current zoom
            val scaleFactor = (1 shl zoomLevel.toInt()).toFloat() * 1.8f

            fun latLngToScreen(lat: Double, lng: Double): Offset {
                val dLat = lat - centerLat
                val dLng = lng - centerLng
                // Web Mercator-like projection conversion for screen
                val x = centerX + (dLng * scaleFactor).toFloat()
                val y = centerY - (dLat * scaleFactor).toFloat()
                return Offset(x, y)
            }

            // 1. Draw Map Base Ground
            drawMapGround(mapTheme, size)

            // 2. Draw Simulated City Vector Features (parks, water, arterial grid)
            drawCityVectorFeatures(mapTheme, centerX, centerY, scaleFactor, size)

            // 3. Draw Route Path if points available
            if (routePoints.size >= 2) {
                drawRoutePolyline(
                    points = routePoints,
                    routeColor = routeColor,
                    latLngToScreen = ::latLngToScreen,
                    showMilestones = showMilestones
                )
            }

            // 4. Draw Start and End Markers
            if (routePoints.isNotEmpty()) {
                val startScreen = latLngToScreen(routePoints.first().latitude, routePoints.first().longitude)
                drawStartPin(startScreen)

                if (routePoints.size > 2) {
                    val endScreen = latLngToScreen(routePoints.last().latitude, routePoints.last().longitude)
                    drawEndPin(endScreen)
                }
            }

            // 5. Draw Current Live Location with Pulsing Radar Halo
            val activeLoc = currentLocation ?: routePoints.lastOrNull()
            if (activeLoc != null) {
                val userScreen = latLngToScreen(activeLoc.latitude, activeLoc.longitude)
                drawLiveUserLocation(
                    center = userScreen,
                    pulseFraction = pulseRadiusFraction,
                    pulseAlpha = pulseAlpha,
                    headingSpeed = activeLoc.speedKmh
                )
            }
        }

        // Map HUD Controls (Zoom In, Zoom Out, Center, Theme Toggle)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SmallFloatingActionButton(
                onClick = {
                    mapTheme = when (mapTheme) {
                        MapThemeMode.DARK_NAV -> MapThemeMode.LIGHT_CITY
                        MapThemeMode.LIGHT_CITY -> MapThemeMode.EMERALD_OUTDOORS
                        MapThemeMode.EMERALD_OUTDOORS -> MapThemeMode.DARK_NAV
                    }
                },
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("map_theme_toggle"),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Map Style")
            }

            SmallFloatingActionButton(
                onClick = {
                    if (routePoints.isNotEmpty()) {
                        val minLat = routePoints.minOf { it.latitude }
                        val maxLat = routePoints.maxOf { it.latitude }
                        val minLng = routePoints.minOf { it.longitude }
                        val maxLng = routePoints.maxOf { it.longitude }
                        centerLat = (minLat + maxLat) / 2.0
                        centerLng = (minLng + maxLng) / 2.0
                        panOffsetX = 0f
                        panOffsetY = 0f
                        zoomLevel = 14.5f
                    }
                },
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("map_fit_route_btn"),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(Icons.Default.ZoomOutMap, contentDescription = "Fit Route")
            }

            SmallFloatingActionButton(
                onClick = {
                    if (currentLocation != null) {
                        centerLat = currentLocation.latitude
                        centerLng = currentLocation.longitude
                    } else if (routePoints.isNotEmpty()) {
                        centerLat = routePoints.last().latitude
                        centerLng = routePoints.last().longitude
                    }
                    panOffsetX = 0f
                    panOffsetY = 0f
                },
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("map_recenter_btn"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center on Location")
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { zoomLevel = (zoomLevel + 0.8f).coerceAtMost(20f) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("map_zoom_in_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(
                        onClick = { zoomLevel = (zoomLevel - 0.8f).coerceAtLeast(10f) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("map_zoom_out_btn")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Map Scale / Coordinates pill top-left
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shadowElevation = 3.dp
        ) {
            val scaleMeters = when {
                zoomLevel > 17f -> "100 m"
                zoomLevel > 15f -> "500 m"
                zoomLevel > 13f -> "1 km"
                else -> "5 km"
            }
            Text(
                text = "📍 Everyday Map • $scaleMeters",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

private fun DrawScope.drawMapGround(theme: MapThemeMode, canvasSize: Size) {
    val bgColor = when (theme) {
        MapThemeMode.DARK_NAV -> Color(0xFF0B132B)
        MapThemeMode.LIGHT_CITY -> Color(0xFFF1F5F9)
        MapThemeMode.EMERALD_OUTDOORS -> Color(0xFF0F2027)
    }
    drawRect(color = bgColor, size = canvasSize)
}

private fun DrawScope.drawCityVectorFeatures(
    theme: MapThemeMode,
    centerX: Float,
    centerY: Float,
    scaleFactor: Float,
    canvasSize: Size
) {
    val roadMajorColor = when (theme) {
        MapThemeMode.DARK_NAV -> Color(0xFF1E293B)
        MapThemeMode.LIGHT_CITY -> Color(0xFFE2E8F0)
        MapThemeMode.EMERALD_OUTDOORS -> Color(0xFF203A43)
    }
    val roadMinorColor = when (theme) {
        MapThemeMode.DARK_NAV -> Color(0xFF172033)
        MapThemeMode.LIGHT_CITY -> Color(0xFFCBD5E1)
        MapThemeMode.EMERALD_OUTDOORS -> Color(0xFF1B2A32)
    }
    val waterColor = when (theme) {
        MapThemeMode.DARK_NAV -> Color(0xFF0F3460)
        MapThemeMode.LIGHT_CITY -> Color(0xFFBAE6FD)
        MapThemeMode.EMERALD_OUTDOORS -> Color(0xFF0D324D)
    }
    val parkColor = when (theme) {
        MapThemeMode.DARK_NAV -> Color(0xFF064E3B).copy(alpha = 0.35f)
        MapThemeMode.LIGHT_CITY -> Color(0xFFDCFCE7)
        MapThemeMode.EMERALD_OUTDOORS -> Color(0xFF134E5E).copy(alpha = 0.4f)
    }

    // Water body on side (Bay/River feature)
    val waterPath = Path().apply {
        moveTo(centerX + 320f, -100f)
        cubicTo(
            centerX + 260f, centerY - 150f,
            centerX + 390f, centerY + 250f,
            centerX + 290f, canvasSize.height + 100f
        )
        lineTo(canvasSize.width + 100f, canvasSize.height + 100f)
        lineTo(canvasSize.width + 100f, -100f)
        close()
    }
    drawPath(path = waterPath, color = waterColor)

    // City Park block
    drawRoundRect(
        color = parkColor,
        topLeft = Offset(centerX - 380f, centerY - 240f),
        size = Size(260f, 180f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
    )

    // Minor Roads Grid
    val step = (scaleFactor * 0.003f).coerceIn(40f, 140f)
    var x = (centerX % step) - step * 2
    while (x < canvasSize.width + step * 2) {
        drawLine(
            color = roadMinorColor,
            start = Offset(x, 0f),
            end = Offset(x, canvasSize.height),
            strokeWidth = 2f
        )
        x += step
    }

    var y = (centerY % step) - step * 2
    while (y < canvasSize.height + step * 2) {
        drawLine(
            color = roadMinorColor,
            start = Offset(0f, y),
            end = Offset(canvasSize.width, y),
            strokeWidth = 2f
        )
        y += step
    }

    // Major Arterial Roads
    drawLine(
        color = roadMajorColor,
        start = Offset(0f, centerY),
        end = Offset(canvasSize.width, centerY),
        strokeWidth = 10f
    )
    drawLine(
        color = roadMajorColor,
        start = Offset(centerX, 0f),
        end = Offset(centerX, canvasSize.height),
        strokeWidth = 10f
    )
    // Diagonal Highway
    drawLine(
        color = roadMajorColor,
        start = Offset(centerX - 400f, centerY + 300f),
        end = Offset(centerX + 400f, centerY - 300f),
        strokeWidth = 12f
    )
}

private fun DrawScope.drawRoutePolyline(
    points: List<LatLngPoint>,
    routeColor: Color,
    latLngToScreen: (Double, Double) -> Offset,
    showMilestones: Boolean
) {
    val path = Path()
    var isFirst = true

    points.forEach { pt ->
        val screenPos = latLngToScreen(pt.latitude, pt.longitude)
        if (isFirst) {
            path.moveTo(screenPos.x, screenPos.y)
            isFirst = false
        } else {
            path.lineTo(screenPos.x, screenPos.y)
        }
    }

    // Outer glow / casing
    drawPath(
        path = path,
        color = routeColor.copy(alpha = 0.3f),
        style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Vibrant main route polyline
    drawPath(
        path = path,
        color = routeColor,
        style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Milestones markers along the route (e.g. at every 1000 meters)
    if (showMilestones && points.size > 5) {
        var accumulatedDist = 0.0
        var nextMilestoneKm = 1
        for (i in 1 until points.size) {
            val distStep = points[i - 1].distanceTo(points[i])
            accumulatedDist += distStep
            if (accumulatedDist >= nextMilestoneKm * 1000.0) {
                val msScreen = latLngToScreen(points[i].latitude, points[i].longitude)
                // Draw milestone badge
                drawCircle(color = Color(0xFF0F172A), radius = 10f, center = msScreen)
                drawCircle(color = Color(0xFFF59E0B), radius = 7f, center = msScreen)
                drawCircle(color = Color.White, radius = 3.5f, center = msScreen)
                nextMilestoneKm++
            }
        }
    }
}

private fun DrawScope.drawStartPin(center: Offset) {
    // Green Start beacon
    drawCircle(color = Color(0x6610B981), radius = 14f, center = center)
    drawCircle(color = Color(0xFF10B981), radius = 9f, center = center)
    drawCircle(color = Color.White, radius = 4f, center = center)
}

private fun DrawScope.drawEndPin(center: Offset) {
    // Amber / Red Finish Pin
    drawCircle(color = Color(0x66EF4444), radius = 14f, center = center)
    drawCircle(color = Color(0xFFEF4444), radius = 9f, center = center)
    drawCircle(color = Color.White, radius = 4f, center = center)
}

private fun DrawScope.drawLiveUserLocation(
    center: Offset,
    pulseFraction: Float,
    pulseAlpha: Float,
    headingSpeed: Double
) {
    // Expanding radar ripple ring
    val maxRadius = 36f
    val currentRadius = 12f + pulseFraction * maxRadius
    drawCircle(
        color = Color(0xFF38BDF8).copy(alpha = pulseAlpha),
        radius = currentRadius,
        center = center,
        style = Stroke(width = 2.5f)
    )

    // Solid Location Dot
    drawCircle(
        color = Color(0x440284C7),
        radius = 16f,
        center = center
    )
    drawCircle(
        color = Color(0xFF0284C7),
        radius = 9f,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 4f,
        center = center
    )
}
