package com.dravenmiller.overseersterminal

import androidx.compose.runtime.mutableStateOf

object AuthHolder {
    // This allows the UI to "listen" for when the bridge is ready
    var bridge = mutableStateOf<GoogleAuthBridge?>(null)
}