package com.dravenmiller.overseersterminal.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// 1. The Terminal Modes
enum class ThemeMode {
    GREEN, BARBIE_PINK, CUSTOM
}

// 2. The Hardware Controller
class ThemeController {
    // The "by mutableStateOf" tells Compose to instantly redraw the screen if these ever change.
    var currentMode by mutableStateOf(ThemeMode.GREEN)

    var customRed by mutableStateOf(20f)
    var customGreen by mutableStateOf(255f)
    var customBlue by mutableStateOf(0f)

    // 3. The Color Router
    // This variable automatically calculates the correct color based on the states above.
    val activeColor: Color
        get() = when (currentMode) {
            ThemeMode.GREEN -> PipGreen
            ThemeMode.BARBIE_PINK -> BarbiePink
            ThemeMode.CUSTOM -> Color(
                red = customRed.toInt(),
                green = customGreen.toInt(),
                blue = customBlue.toInt()
            )
        }
}

// 4. The Memory Builder
// We call this function inside App.kt so the terminal doesn't forget your theme when you switch tabs!
@Composable
fun rememberThemeController(): ThemeController {
    return remember { ThemeController() }
}
