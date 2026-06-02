package com.dravenmiller.overseersterminal.health

interface BiometricBridge {
    // 1. THIS IS THE LINE STATTAB IS LOOKING FOR:
    fun requestPermissions()

    // 2. The HP calculation
    suspend fun getSleepHpScore(): Int

    // 3. The AP calculation
    suspend fun getHeartRateApScore(): Int
}
