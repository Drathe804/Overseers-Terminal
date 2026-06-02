package com.dravenmiller.overseersterminal.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// The Blueprint to find the file
expect fun readLatestSwoleBackup(): String?

// The Blueprint to connect to the global net!
expect suspend fun fetchGitHubData(username: String): Map<String, Int>
object StatEngine {

    // The new Data Chip that holds either Lifts OR Cardio!
    data class Record(val maxWeight: Float = 0f, val maxDistance: Float = 0f, val maxTimeSec: Float = 0f, val topSpeedMph: Float = 0f)

    // --- LIVE DATABASE OF EVERY PR! ---
    var exercisePRs by mutableStateOf<Map<String, Record>>(emptyMap())

    // A smart search tool for Weight!
    fun getHighestWeight(keyword: String): Float {
        return exercisePRs.filterKeys { it.contains(keyword, ignoreCase = true) }
            .values.maxOfOrNull { it.maxWeight } ?: 0f
    }

    // A smart search tool for Speed!
    fun getHighestSpeed(keyword: String): Float {
        return exercisePRs.filterKeys { it.contains(keyword, ignoreCase = true) }
            .values.maxOfOrNull { it.topSpeedMph } ?: 0f
    }

    // --- THE S.P.E.C.I.A.L. CALCULATORS ---
    fun getStrength(): Int {
        val benchPlates = ((getHighestWeight("Bench") - 45) / 90).coerceAtLeast(0f)
        val squatPlates = ((getHighestWeight("Squat") - 45) / 90).coerceAtLeast(0f)
        val dlPlates = ((getHighestWeight("Deadlift") - 45) / 90).coerceAtLeast(0f)
        return (benchPlates + squatPlates + dlPlates).toInt().coerceIn(1, 10)
    }

    // --- GITHUB INTELLIGENCE DATA ---
    var codingLanguages by mutableStateOf<Map<String, Int>>(emptyMap())

    // Update Intelligence to count your active languages!
    fun getIntelligence(): Int = codingLanguages.size.coerceIn(1, 10)

    // The Network Sync function
    suspend fun syncGitHub(username: String) {
        // OVERRIDE: Only ping the server if our memory banks are empty!
        // This prevents GitHub from banning your IP address for spamming requests.
        if (codingLanguages.isEmpty()) {
            codingLanguages = fetchGitHubData(username)
        }
    }



    fun getPerception(): Int = 5
    fun getEndurance(): Int = 6
    fun getCharisma(): Int = 7

    // Agility is now based on your actual Cardio Speed! (Maxes out at 12mph / 5 min mile)
    fun getAgility(): Int {
        val maxRunSpeed = getHighestSpeed("Run").coerceAtLeast(getHighestSpeed("Treadmill"))
        return ((maxRunSpeed / 12f) * 10).toInt().coerceIn(1, 10)
    }

    fun getLuck(): Int = 9

    // --- THE WASTELAND JSON SCRAPER ---
    fun parseSwoleHolotape(jsonString: String) {
        val prMap = mutableMapOf<String, Record>()
        val blocks = jsonString.split("\"exercise\":{")

        for (block in blocks) {
            if (!block.contains("\"name\":")) continue

            val nameMatch = "\"name\":\"([^\"]+)\"".toRegex().find(block)
            val exerciseName = nameMatch?.groups?.get(1)?.value ?: continue

            val isSingleSide = block.contains("\"isSingleSide\":true")
            val multiplier = if (isSingleSide) 2f else 1f

            var maxW = 0f
            var maxD = 0f
            var maxT = 0f
            var topS = 0f

            // Break the block down into individual Sets!
            val setRegex = "\\{([^}]+)\\}".toRegex()
            setRegex.findAll(block).forEach { match ->
                val setJson = match.value
                val w = "\"weight\":([0-9.]+)".toRegex().find(setJson)?.groups?.get(1)?.value?.toFloatOrNull() ?: 0f
                val d = "\"distance\":([0-9.]+)".toRegex().find(setJson)?.groups?.get(1)?.value?.toFloatOrNull() ?: 0f
                val t = "\"time\":([0-9.]+)".toRegex().find(setJson)?.groups?.get(1)?.value?.toFloatOrNull() ?: 0f

                if ((w * multiplier) > maxW) maxW = w * multiplier
                if (d > maxD) maxD = d
                if (t > maxT) maxT = t

                // If it logged both distance and time, calculate speed!
                // (Assuming distance is Miles and time is Seconds!)
                if (d > 0 && t > 0) {
                    val hours = t / 3600f
                    val speed = d / hours
                    if (speed > topS) topS = speed
                }
            }

            // Merge with existing records!
            val existing = prMap[exerciseName] ?: Record()
            prMap[exerciseName] = Record(
                maxWeight = maxOf(maxW, existing.maxWeight),
                maxDistance = maxOf(maxD, existing.maxDistance),
                maxTimeSec = maxOf(maxT, existing.maxTimeSec),
                topSpeedMph = maxOf(topS, existing.topSpeedMph)
            )
        }
        exercisePRs = prMap
    }

    fun loadLiveBiometrics() {
        val json = readLatestSwoleBackup()
        if (json != null) {
            parseSwoleHolotape(json)
        }
    }
}
