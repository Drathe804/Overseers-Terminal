package com.dravenmiller.overseersterminal.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import com.dravenmiller.overseersterminal.theme.ThemeController

@Composable
fun VaultBoyAnimator(
    frames: List<DrawableResource>,
    themeController: ThemeController,
    modifier: Modifier = Modifier,
    frameDelay: Long = 150L,
    loop: Boolean = true,
    play: Boolean = true // <-- THE NEW PAUSE/PLAY SWITCH
) {
    var currentFrameIndex by remember { mutableStateOf(0) }

    // It watches the 'play' variable. If 'play' changes, this triggers!
    LaunchedEffect(frames, loop, play) {
        if (!play) return@LaunchedEffect // If play is false, do absolutely nothing!

        if (loop) {
            // Normal behavior: Loop forever (for walking)
            while (true) {
                delay(frameDelay)
                currentFrameIndex = (currentFrameIndex + 1) % frames.size
            }
        } else {
            // Boot Screen behavior: Play once, then stop
            for (i in 0 until frames.size - 1) {
                delay(frameDelay)
                currentFrameIndex++
            }
            // It will naturally freeze on the very last frame!
        }
    }

    Image(
        painter = painterResource(frames[currentFrameIndex]),
        contentDescription = "Vault Dweller Animation",
        modifier = modifier,
        colorFilter = ColorFilter.tint(themeController.activeColor)
    )
}
