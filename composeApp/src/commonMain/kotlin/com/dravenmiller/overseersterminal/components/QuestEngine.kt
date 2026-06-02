package com.dravenmiller.overseersterminal.components

import androidx.compose.runtime.mutableStateListOf
import kotlin.random.Random
import kotlin.math.abs

enum class QuestCategory { MAIN, SIDE, RADIANT, GOALS }

enum class ObjectiveType { STANDARD, TIMED_WAIT, COLLECTION }

data class Objective(
    val id: String = "OBJ_${kotlin.random.Random.nextInt(100000, 999999)}",
    var text: String,
    var isComplete: Boolean = false,
    val type: ObjectiveType = ObjectiveType.STANDARD,

    val time: String? = null,
    val date: String? = null,
    val location: String? = null,

    val targetAmount: Float? = null,
    var currentAmount: Float = 0f,
    val isCurrency: Boolean = false,

    val waitDurationMs: Long? = null,
    var waitStartTimeMs: Long? = null,
    val postWaitText: String? = null,

    val waitForPrevious: Boolean = true
)

data class Quest(
    val id: String,
    val title: String,
    val category: QuestCategory,
    val objectives: MutableList<Objective>,
    var isComplete: Boolean = false,
    var isActive: Boolean = false,
    var repeatInterval: String? = null,
    var spawnTimeMs: Long = 0L
)

object QuestEngine {
    var questsCompleted = 0
    var workoutsCompleted = 0
    var goalsAccomplished = 0

    val activeQuests = mutableStateListOf<Quest>()
    val savedTemplates = mutableStateListOf<Quest>()
    val knownLocations = listOf("ANY", "Vault 111 (Fenway)", "Sedalia Outpost", "Supermarket", "Gym", "Dentist")

    fun createNewQuest(title: String, category: QuestCategory, objectives: List<Objective>, repeatInterval: String?, saveAsTemplate: Boolean) {
        val processedObjectives = objectives.toMutableList()
        // If the very first objective is a Timer, start it instantly!
        if (processedObjectives.isNotEmpty() && processedObjectives[0].type == ObjectiveType.TIMED_WAIT) {
            processedObjectives[0] = processedObjectives[0].copy(waitStartTimeMs = getSystemEpochMillis())
        }

        val newQuest = Quest("Q_${Random.nextInt(100000, 999999)}", title, category, processedObjectives, false, true, repeatInterval)
        activeQuests.add(0, newQuest)
        if (saveAsTemplate) savedTemplates.add(newQuest.copy(id = "T_${Random.nextInt(100000, 999999)}"))
    }

    fun addObjectives(questId: String, newObjectives: List<Objective>) {
        val index = activeQuests.indexOfFirst { it.id == questId }
        if (index != -1) {
            val updated = activeQuests[index].objectives.toMutableList()

            // THE FIX: Check if we are currently completely caught up on this quest!
            val allPreviousComplete = updated.all { it.isComplete }
            val processedNew = newObjectives.toMutableList()

            // If we are caught up, and the very first thing we append is a timer, start it instantly!
            if (allPreviousComplete && processedNew.isNotEmpty() && processedNew[0].type == ObjectiveType.TIMED_WAIT) {
                processedNew[0] = processedNew[0].copy(waitStartTimeMs = getSystemEpochMillis())
            }

            updated.addAll(processedNew)
            activeQuests[index] = activeQuests[index].copy(objectives = updated)
        }
    }

    // --- NEW: THE SEQUENTIAL CHAIN LOGIC ---
    fun toggleObjective(questId: String, objIndex: Int): Boolean {
        val qIdx = activeQuests.indexOfFirst { it.id == questId }
        if (qIdx == -1) return false
        val quest = activeQuests[qIdx]
        val updatedObjs = quest.objectives.toMutableList()

        val isNowComplete = !updatedObjs[objIndex].isComplete
        updatedObjs[objIndex] = updatedObjs[objIndex].copy(isComplete = isNowComplete)

        // If we just completed this step, unlock the next one!
        if (isNowComplete && objIndex + 1 < updatedObjs.size) {
            val nextObj = updatedObjs[objIndex + 1]
            if (nextObj.type == ObjectiveType.TIMED_WAIT && nextObj.waitStartTimeMs == null) {
                updatedObjs[objIndex + 1] = nextObj.copy(waitStartTimeMs = getSystemEpochMillis())

                // --- THE NEW NOTIFICATION TRIGGER ---
                if (nextObj.waitDurationMs != null && nextObj.waitDurationMs > 0) {
                    val safeId = kotlin.random.Random.nextInt(1, 9999)
                    scheduleObjectiveNotification(
                        id = safeId,
                        title = "PIP-BOY DIRECTIVE UPDATE",
                        message = nextObj.postWaitText ?: "Timer Complete!",
                        delayMs = nextObj.waitDurationMs
                    )
                }
            }
        }

        activeQuests[qIdx] = quest.copy(objectives = updatedObjs)
        return isNowComplete && updatedObjs.all { it.isComplete } // Returns true if Quest is finished!
    }

    // --- NEW: THE COLLECTION BANK LOGIC ---
    fun addFundsToObjective(questId: String, objIndex: Int, amount: Float): Boolean {
        val qIdx = activeQuests.indexOfFirst { it.id == questId }
        if (qIdx == -1) return false
        val quest = activeQuests[qIdx]
        val updatedObjs = quest.objectives.toMutableList()
        val obj = updatedObjs[objIndex]

        val newAmount = obj.currentAmount + amount
        var isNowComplete = obj.isComplete

        if (obj.targetAmount != null && newAmount >= obj.targetAmount) {
            isNowComplete = true
            // Unlock next step!
            if (objIndex + 1 < updatedObjs.size) {
                val nextObj = updatedObjs[objIndex + 1]
                if (nextObj.type == ObjectiveType.TIMED_WAIT && nextObj.waitStartTimeMs == null) {
                    updatedObjs[objIndex + 1] = nextObj.copy(waitStartTimeMs = getSystemEpochMillis())
                }
            }
        }

        updatedObjs[objIndex] = obj.copy(currentAmount = newAmount, isComplete = isNowComplete)
        activeQuests[qIdx] = quest.copy(objectives = updatedObjs)
        return isNowComplete && updatedObjs.all { it.isComplete }
    }

    fun completeQuest(questId: String) {
        val index = activeQuests.indexOfFirst { it.id == questId }
        if (index != -1) {
            val quest = activeQuests[index]
            activeQuests[index] = quest.copy(isComplete = true, isActive = false)
            questsCompleted++
            if (quest.category == QuestCategory.GOALS) goalsAccomplished++

            if (quest.category == QuestCategory.RADIANT) {
                val delayMs = parseIntervalToMillis(quest.repeatInterval)
                val resetObjectives = quest.objectives.map {
                    // Reset amounts, times, and completions for the clone!
                    it.copy(isComplete = false, currentAmount = 0f, waitStartTimeMs = null)
                }.toMutableList()

                activeQuests.add(quest.copy(
                    id = "Q_${Random.nextInt(100000, 999999)}",
                    isComplete = false, isActive = false, objectives = resetObjectives,
                    spawnTimeMs = getSystemEpochMillis() + delayMs
                ))
            }
        }
    }

    private fun parseIntervalToMillis(interval: String?): Long {
        if (interval == null) return 0L
        val parts = interval.split(" ")
        if (parts.size < 2) return 0L
        val amount = parts[0].toLongOrNull() ?: 1L
        return when (parts[1].uppercase()) {
            "DAYS" -> amount * 86400000L
            "WEEKS" -> amount * 604800000L
            "MONTHS" -> amount * 2592000000L
            "YEARS" -> amount * 31536000000L
            else -> 0L
        }
    }




// ==========================================
    // THE GEOFENCE & TIME-FENCE ENGINE
    // ==========================================
    fun evaluateTriggers(currentLocation: String, currentTime: String) {
        for (i in activeQuests.indices) {
            val quest = activeQuests[i]
            if (quest.isComplete) continue

            var questChanged = false
            val updatedObjectives = quest.objectives.toMutableList()

            for (j in updatedObjectives.indices) {
                val obj = updatedObjectives[j]
                if (!obj.isComplete && obj.location != null && obj.location != "ANY") {

                    // Did we arrive at the location?
                    if (currentLocation.contains(obj.location, ignoreCase = true)) {

                        // Is there a time limit?
                        if (obj.time != null) {
                            if (isWithin10Minutes(obj.time, currentTime)) {
                                updatedObjectives[j] = obj.copy(isComplete = true)
                                questChanged = true
                            }
                        } else {
                            // No time limit, just being here is enough!
                            updatedObjectives[j] = obj.copy(isComplete = true)
                            questChanged = true
                        }
                    }
                }
            }

            if (questChanged) {
                activeQuests[i] = quest.copy(objectives = updatedObjectives)
            }
        }
    }


    // Time Math: Converts "10:30 AM" into total minutes for easy 10-minute margin checks!
    private fun isWithin10Minutes(targetTime: String, currentTime: String): Boolean {
        try {
            fun toMins(t: String): Int {
                val parts = t.replace(" ", ":").split(":")
                if (parts.size < 2) return -1
                var h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                val ampm = if (parts.size > 2) parts[2].uppercase() else ""
                if (ampm == "PM" && h != 12) h += 12
                if (ampm == "AM" && h == 12) h = 0
                return h * 60 + m
            }
            val t1 = toMins(targetTime)
            val t2 = toMins(currentTime)
            if (t1 == -1 || t2 == -1) return false

            var diff = abs(t1 - t2)
            if (diff > 720) diff = 1440 - diff // Handles Midnight wraparound!
            return diff <= 10
        } catch (e: Exception) { return false }
    }
}

expect fun getSystemEpochMillis(): Long

expect fun scheduleObjectiveNotification(id: Int, title: String, message: String, delayMs: Long)
