package com.dravenmiller.overseersterminal.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.dravenmiller.overseersterminal.theme.ThemeController

@Composable
fun CrtOverlay(themeController: ThemeController, modifier: Modifier = Modifier) {
    // 1. The Animation Engine
    val infiniteTransition = rememberInfiniteTransition()

    // 2. The Roll Tracker: Goes from 0.0 (top) to 1.0 (bottom) over 4 seconds, then restarts
    val rollOffset by infiniteTransition.animateFloat(
        initialValue = -0.2f, // Start slightly above the screen
        targetValue = 1.2f,   // End slightly below the screen
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // --- LAYER 1: The Static Scanlines (from before) ---
        val scanlineColor = Color.Black.copy(alpha = 0.25f)
        val strokeWidth = 3f
        val spacing = 8f
        var currentY = 0f

        while (currentY < size.height) {
            drawLine(
                color = scanlineColor,
                start = Offset(0f, currentY),
                end = Offset(size.width, currentY),
                strokeWidth = strokeWidth
            )
            currentY += spacing
        }

        // --- LAYER 2: The Glowing CRT Sweep ---
        val sweepHeight = size.height * 0.15f // Makes the bar take up 15% of the screen height
        val currentSweepY = size.height * rollOffset

        // We use a gradient so the bar fades smoothly at the top and bottom!
        val sweepBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                themeController.activeColor.copy(alpha = 0.08f), // Faint glowing core
                Color.Transparent
            ),
            startY = currentSweepY,
            endY = currentSweepY + sweepHeight
        )

        drawRect(
            brush = sweepBrush,
            topLeft = Offset(0f, currentSweepY),
            size = Size(size.width, sweepHeight)
        )
    }
}
