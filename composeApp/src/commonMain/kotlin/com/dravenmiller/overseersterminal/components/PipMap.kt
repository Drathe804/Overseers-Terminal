package com.dravenmiller.overseersterminal.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// 1. THE UPGRADED LOCATION DATA (Now it holds an icon type!)
data class PipLocation(val name: String, val coordinates: Pair<Double, Double>, val iconType: String)

// 2. THE UPGRADED BLUEPRINT
@Composable
expect fun PipMap(
    modifier: Modifier = Modifier,
    targetLocation: Pair<Double, Double>? = null,
    markers: List<PipLocation> = emptyList(), // <-- Accepts your list of custom markers!
    onMapUpdate: (Float, String, Pair<Double, Double>) -> Unit = { _, _, _ -> }
)
