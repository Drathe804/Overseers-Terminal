package com.dravenmiller.overseersterminal.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PipMap(
    modifier: Modifier,
    targetLocation: Pair<Double, Double>?,
    markers: List<PipLocation>,
    onMapUpdate: (Float, String, Pair<Double, Double>) -> Unit
) {
    Box(modifier = modifier)
}