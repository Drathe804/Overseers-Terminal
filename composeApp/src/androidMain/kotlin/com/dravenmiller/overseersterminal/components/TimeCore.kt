package com.dravenmiller.overseersterminal.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Tell the Android OS to check its internal watch!
actual fun getPipBoyTime(): String {
    return SimpleDateFormat("HH:mm", Locale.US).format(Date()) // E.g., 14:30
}

actual fun getPipBoyDate(): String {
    return SimpleDateFormat("MM.dd.yyyy", Locale.US).format(Date()) // E.g., 10.23.2077
}