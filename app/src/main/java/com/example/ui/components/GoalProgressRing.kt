package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun GoalProgressRing(
    currentDistanceKm: Double,
    goalDistanceKm: Double,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    strokeWidth: Dp = 12.dp
) {
    val progressFraction = if (goalDistanceKm > 0) {
        (currentDistanceKm / goalDistanceKm).toFloat().coerceIn(0f, 1f)
    } else 0f

    val isGoalAchieved = currentDistanceKm >= goalDistanceKm && goalDistanceKm > 0
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 900),
        label = "goalProgress"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("goal_progress_ring"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            val canvasSize = this.size
            val strokePx = strokeWidth.toPx()

            // Background circle track
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Foreground progress arc with vibrant gradient
            val gradientBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF38BDF8),
                    Color(0xFF10B981),
                    Color(0xFF38BDF8)
                )
            )

            val sweepAngle = animatedProgress * 360f
            if (sweepAngle > 0f) {
                drawArc(
                    brush = gradientBrush,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Center Content: Total km & Goal
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.US, "%.1f", currentDistanceKm),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "of ${String.format(Locale.US, "%.1f", goalDistanceKm)} km",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isGoalAchieved) {
                Text(
                    text = "🎯 GOAL MET!",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF10B981)
                )
            } else {
                val percent = (progressFraction * 100).toInt()
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF38BDF8)
                )
            }
        }
    }
}
