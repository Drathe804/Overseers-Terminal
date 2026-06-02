package com.dravenmiller.overseersterminal.components

import kotlinx.coroutines.flow.MutableStateFlow

// The Global Memory Bank for Turn-by-Turn Data
object NavState {
    val currentDirection = MutableStateFlow<String?>(null) // e.g., "Turn left on Main St"
    val currentDistance = MutableStateFlow<String?>(null)  // e.g., "in 500 ft"
}
