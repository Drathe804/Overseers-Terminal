package com.dravenmiller.overseersterminal.components

// Safely asks Android for the time!
actual fun getSystemEpochMillis(): Long = System.currentTimeMillis()
