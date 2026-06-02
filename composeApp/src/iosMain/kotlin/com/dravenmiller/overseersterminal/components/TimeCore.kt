package com.dravenmiller.overseersterminal.components

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

// Tell the Apple OS to check its internal watch!
actual fun getPipBoyTime(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "HH:mm" // 24-hour military time
    return formatter.stringFromDate(NSDate())
}

actual fun getPipBoyDate(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "MM.dd.yyyy" // Vault-Tec standard date
    return formatter.stringFromDate(NSDate())
}
