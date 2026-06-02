package com.dravenmiller.overseersterminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.* // Added this for remember and mutableStateOf!
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.dravenmiller.overseersterminal.components.CrtOverlay
import com.dravenmiller.overseersterminal.theme.CrtBlack
import com.dravenmiller.overseersterminal.theme.rememberThemeController
import com.dravenmiller.overseersterminal.ui.MainScreen
import com.dravenmiller.overseersterminal.ui.BootScreen // Import the new screen!
import com.dravenmiller.overseersterminal.health.BiometricBridge

@Composable
fun App(biometricBridge: BiometricBridge? = null) {
    val themeController = rememberThemeController()

    // You will need to click on 'rememberSaveable' and hit Alt+Enter to import it!
    var isBooting by rememberSaveable { mutableStateOf(true) }


    // The Bottom Layer: CRT Black Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrtBlack)
    ) {

        // THE ROUTER: Which screen gets the power?
        if (isBooting) {
            // Show the Boot Screen!
            // When it finishes its script, it triggers this bracket, flipping the switch to false.
            BootScreen(themeController = themeController) {
                isBooting = false
            }
        } else {
            // Once isBooting is false, the boot screen is vaporized and the Main Screen loads!
            MainScreen(themeController = themeController, biometricBridge = biometricBridge)
        }

        // The Top Layer: The Scanline Glass! (Always on top of both screens)
        CrtOverlay(themeController = themeController)

    }
}
