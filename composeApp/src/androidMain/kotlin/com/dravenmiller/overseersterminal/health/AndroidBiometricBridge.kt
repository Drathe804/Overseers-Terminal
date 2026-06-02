package com.dravenmiller.overseersterminal.health

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit
import android.content.Intent
import com.dravenmiller.overseersterminal.MainActivity


class AndroidBiometricBridge(private val context: Context) : BiometricBridge {


    override fun requestPermissions() {
        try {
            // Because we passed MainActivity in as the "context" earlier,
            // we can safely cast it back to MainActivity and pull the trigger!
            val activity = context as MainActivity
            activity.promptHealthPermissions()
        } catch (e: Exception) {
            println("Terminal desync: Could not launch Health Connect.")
        }
    }



    // 1. Boot up the connection to Samsung Health / Google Fit
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    // 2. The HP Calculation (Sleep)
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getSleepHpScore(): Int {
        return try {
            // Look at the last 24 hours
            val endTime = Instant.now()
            val startTime = endTime.minus(24, ChronoUnit.HOURS)

            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = healthConnectClient.readRecords(request)

            // Add up all the sleep you got
            var totalSleepHours = 0.0
            response.records.forEach { session ->
                val durationMs = session.endTime.toEpochMilli() - session.startTime.toEpochMilli()
                totalSleepHours += durationMs / (1000.0 * 60 * 60)
            }

            // RobCo Math: 8 hours = 100 HP. 4 hours = 50 HP.
            val hpScore = ((totalSleepHours / 8.0) * 100).toInt()

            // Don't let it go over 100 if you sleep 10 hours!
            hpScore.coerceIn(0, 100)

        } catch (e: Exception) {
            0 // If API fails, return 0 HP (You are dead)
        }
    }

    // 3. The AP Calculation (Heart Rate)
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getHeartRateApScore(): Int {
        return try {
            // Look at the last 1 hour to find your most recent pulse
            val endTime = Instant.now()
            val startTime = endTime.minus(1, ChronoUnit.HOURS)

            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = healthConnectClient.readRecords(request)
            val latestRecord = response.records.lastOrNull()

            // Get the last recorded BPM
            val currentBpm = latestRecord?.samples?.lastOrNull()?.beatsPerMinute ?: 70L

            // RobCo Math: If resting (60 BPM), AP is 100.
            // For every beat over 60, you lose 1 AP. At 160 BPM, AP is 0!
            val apScore = 100 - (currentBpm.toInt() - 60)

            apScore.coerceIn(0, 100)

        } catch (e: Exception) {
            100 // If API fails, default to full AP
        }
    }
}
