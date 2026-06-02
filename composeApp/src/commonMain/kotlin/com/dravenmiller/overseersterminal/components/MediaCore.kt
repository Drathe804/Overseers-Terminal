package com.dravenmiller.overseersterminal.components

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.State

// Add 'isConnected' to the data chip!
data class PipBluetoothDevice(val name: String, val macAddress: String, val isConnected: Boolean)

enum class MediaCommand { PLAY_PAUSE, NEXT, PREVIOUS, REWIND_30, FAST_FORWARD_30 }

// 1. Add the secret 'receiverName' to the data chip
data class PipMediaApp(val name: String, val packageName: String, val receiverName: String)

expect class PipMediaController {
    // 2. Change targetPackage to accept the full targetApp!
    fun sendCommand(command: MediaCommand, targetApp: PipMediaApp? = null)
    fun getInstalledMediaApps(): List<PipMediaApp>
    fun getPairedBluetoothDevices(): List<PipBluetoothDevice>
}

// The Global Memory Bank for Audio Intercepts
object MediaState {
    val trackTitle = MutableStateFlow<String?>("AWAITING SIGNAL...")
    val trackArtist = MutableStateFlow<String?>("")
    val currentPosition = MutableStateFlow(0L) // In milliseconds
    val trackDuration = MutableStateFlow(0L)   // In milliseconds
}


@Composable
expect fun rememberMediaController(): PipMediaController

expect fun formatRadioFreq(frequency: Float): String

// THE LIVE RADAR BLUEPRINT
@Composable
expect fun observeBluetoothDevices(controller: PipMediaController): State<List<PipBluetoothDevice>>