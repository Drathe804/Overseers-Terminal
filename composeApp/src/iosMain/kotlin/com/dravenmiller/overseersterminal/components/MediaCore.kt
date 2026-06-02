package com.dravenmiller.overseersterminal.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

actual class PipMediaController {
    actual fun sendCommand(command: MediaCommand, targetApp: PipMediaApp?) { }
    actual fun getInstalledMediaApps(): List<PipMediaApp> = emptyList()
    actual fun getPairedBluetoothDevices(): List<PipBluetoothDevice> = emptyList()
}

// 2. The iOS Factory
@Composable
actual fun rememberMediaController(): PipMediaController {
    return remember { PipMediaController() }
}

// 3. The iOS Format Hack
actual fun formatRadioFreq(frequency: Float): String {
    // A quick math trick to get 3 decimal places without using Java!
    val mainNumber = frequency.toInt()
    val remainder = ((frequency - mainNumber) * 1000).toInt()

    // Pads the end with zeros (e.g., .5 becomes .500)
    val paddedRemainder = remainder.toString().padStart(3, '0')

    return "$mainNumber.$paddedRemainder"
}

@Composable
actual fun observeBluetoothDevices(controller: PipMediaController): State<List<PipBluetoothDevice>> {
    return remember { mutableStateOf(emptyList()) }
}
