package com.dravenmiller.overseersterminal.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.dravenmiller.overseersterminal.components.PipText
import com.dravenmiller.overseersterminal.components.VaultBoyAnimator
import com.dravenmiller.overseersterminal.theme.ThemeController
import kotlinx.coroutines.delay
import overseersterminal.composeapp.generated.resources.*

// The 7 Phases of the Cinematic Boot Sequence
enum class BootState { BLINK_IN, TYPING, BLINK_OUT, SCROLL_UP, FADE_IN, ANIMATING, SUCCESS }

@Composable
fun BootScreen(themeController: ThemeController, onBootComplete: () -> Unit) {
    val bootLines = listOf(
        "WELCOME TO ROBCO INDUSTRIES UNIFIED OPERATING SYSTEM",
        "COPYRIGHT 2075-2077 ROBCO INDUSTRIES",
        "-Server 1-",
        "Compiled...",
        "Initializing Pip-Boy OS...",
        "Connecting to Biometric Scanners...",
        "Establishing Satellite Uplink...",
        "SYSTEM READY."
    )

    // --- YOUR BOOT ANIMATION ASSETS ---
    val bootFrames = listOf(
        Res.drawable.boot_1,
        Res.drawable.boot_2,
        Res.drawable.boot_3,
        Res.drawable.boot_4,
        Res.drawable.boot_5,
        Res.drawable.boot_6,
        Res.drawable.boot_7,
        Res.drawable.boot_8
    )

    // The Memory Banks
    var state by remember { mutableStateOf(BootState.BLINK_IN) }
    var completedLines by remember { mutableStateOf(listOf<String>()) }
    var currentTypingLine by remember { mutableStateOf("") }
    var showCursor by remember { mutableStateOf(true) }

    // --- THE HOLLYWOOD CAMERAS ---
    // 1. The Scroll Camera (Triggers when state is SCROLL_UP or later)
    val scrollOffset by animateDpAsState(
        targetValue = if (state.ordinal >= BootState.SCROLL_UP.ordinal) (-1000).dp else 0.dp,
        animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing)
    )

    // 2. The Alpha Fade (Triggers when state is FADE_IN or later)
    val imageAlpha by animateFloatAsState(
        targetValue = if (state.ordinal >= BootState.FADE_IN.ordinal) 1f else 0f,
        animationSpec = tween(durationMillis = 2000) // 1 second smooth fade
    )

    // --- THE DIRECTOR'S SCRIPT ---
    LaunchedEffect(Unit) {
        // PHASE 1: Initial Blinking
        for (i in 0..5) {
            showCursor = !showCursor
            delay(350)
        }
        showCursor = true
        state = BootState.TYPING

        // PHASE 2: Slower Typing
        for (line in bootLines) {
            currentTypingLine = ""
            for (char in line) {
                currentTypingLine += char
                delay(45L)
            }
            completedLines = completedLines + currentTypingLine
            currentTypingLine = ""
            delay(150L)
        }

        // PHASE 3: Final Blinking
        state = BootState.BLINK_OUT
        for (i in 0..5) {
            showCursor = !showCursor
            delay(350)
        }
        showCursor = false

        // PHASE 4: Scroll the screen up to black
        state = BootState.SCROLL_UP
        delay(1500L) // Wait exactly as long as the scroll animation takes

        // PHASE 5: Fade in the Vault Boy (Standing perfectly still!)
        state = BootState.FADE_IN
        delay(1500L) // Wait 1s for fade, plus an extra 0.5s dramatic pause

        // PHASE 6: Action! Play the thumbs up!
        state = BootState.ANIMATING
        delay(1300L) // Wait for the frames to finish flipping (approx 1.3 secs)

        // PHASE 7: Stamp the Success text!
        state = BootState.SUCCESS
        delay(2500L) // Let the user bask in their success for 2.5 seconds

        // CUT! Launch the Main OS.
        onBootComplete()
    }

    // --- THE STAGE (Screen Layout) ---
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {

        // LAYER 1: The Scrolling Text
        Column(
            modifier = Modifier.fillMaxSize().offset(y = scrollOffset),
            horizontalAlignment = Alignment.Start
        ) {
            completedLines.forEach { line ->
                PipText(text = line, themeController = themeController)
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (state == BootState.TYPING || state == BootState.BLINK_IN || state == BootState.BLINK_OUT) {
                val cursorChar = if (showCursor) "█" else ""
                PipText(text = currentTypingLine + cursorChar, themeController = themeController)
            }
        }

        // LAYER 2: The Vault Boy & Success Text
        if (state.ordinal >= BootState.FADE_IN.ordinal) {
            Column(
                modifier = Modifier.alpha(imageAlpha), // Tied to the smooth fader
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VaultBoyAnimator(
                    frames = bootFrames,
                    themeController = themeController,
                    modifier = Modifier.size(200.dp),
                    loop = false,
                    // THE MAGIC TRICK: Don't let him move until the Director hits Phase 6!
                    play = (state.ordinal >= BootState.ANIMATING.ordinal)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Only draw the SUCCESS text if we've reached Phase 7
                if (state == BootState.SUCCESS) {
                    PipText(text = "SUCCESS", themeController = themeController)
                }
            }
        }
    }
}
