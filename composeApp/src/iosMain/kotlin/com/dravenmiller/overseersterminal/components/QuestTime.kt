package com.dravenmiller.overseersterminal.components

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

// Safely asks Apple for the time!
actual fun getSystemEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
