package com.dravenmiller.overseersterminal.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.dravenmiller.overseersterminal.theme.ThemeController

@Composable
fun PipText(
    text: String,
    themeController: ThemeController,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    textColorOverride: Color? = null // 1. THE OVERRIDE SWITCH
) {
    Text(
        text = text.uppercase(),
        // 2. THE ROUTER: Use the override if it exists, otherwise use the theme color
        color = textColorOverride ?: themeController.activeColor,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        modifier = modifier
    )
}
