package com.dravenmiller.overseersterminal.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import kotlin.random.Random
import kotlin.math.abs

enum class QuestCategory { MAIN, SIDE, RADIANT, GOALS }

enum class ObjectiveType { STANDARD, TIMED_WAIT, COLLECTION }

expect @Composable fun RequestRuntimePermissions()

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

    val waitForPrevious: Boolean = true,
    val isOptional: Boolean = false
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

    // THE FIX: Removed the objective list requirement, and made it return the String ID!
    fun createNewQuest(title: String, category: QuestCategory, repeatInterval: String?, saveAsTemplate: Boolean, isSequential: Boolean): String {
        val newQuestId = "Q_${kotlin.random.Random.nextInt(100000, 999999)}"

        val newQuest = Quest(
            id = newQuestId,
            title = title,
            category = category,
            objectives = mutableListOf(), // Starts completely empty!
            isComplete = false,
            isActive = true,
            repeatInterval = repeatInterval,
            spawnTimeMs = getSystemEpochMillis()
        )

        activeQuests.add(0, newQuest)
        if (saveAsTemplate) savedTemplates.add(newQuest.copy(id = "T_${kotlin.random.Random.nextInt(100000, 999999)}"))

        return newQuestId // Passes the ID back to the UI!
    }


    fun toggleObjective(questId: String, objIndex: Int): Boolean {
        val qIdx = activeQuests.indexOfFirst { it.id == questId }
        if (qIdx == -1) return false
        val quest = activeQuests[qIdx]
        val updatedObjs = quest.objectives.toMutableList()

        updatedObjs[objIndex] = updatedObjs[objIndex].copy(isComplete = !updatedObjs[objIndex].isComplete)

        updatePhasesAndTimers(updatedObjs) // Automatically handles the next phases!
        activeQuests[qIdx] = quest.copy(objectives = updatedObjs)
        return updatedObjs.filter { !it.isOptional }.all { it.isComplete }
    }

    fun addFundsToObjective(questId: String, objIndex: Int, amount: Float): Boolean {
        val qIdx = activeQuests.indexOfFirst { it.id == questId }
        if (qIdx == -1) return false
        val quest = activeQuests[qIdx]
        val updatedObjs = quest.objectives.toMutableList()
        val obj = updatedObjs[objIndex]

        val newAmount = obj.currentAmount + amount
        var isNowComplete = obj.isComplete
        if (obj.targetAmount != null && newAmount >= obj.targetAmount) isNowComplete = true

        updatedObjs[objIndex] = obj.copy(currentAmount = newAmount, isComplete = isNowComplete)

        updatePhasesAndTimers(updatedObjs) // Automatically handles the next phases!
        activeQuests[qIdx] = quest.copy(objectives = updatedObjs)
        return updatedObjs.all { it.isComplete }
    }

    fun addObjectives(questId: String, newObjectives: List<Objective>) {
        val index = activeQuests.indexOfFirst { it.id == questId }
        if (index != -1) {
            val updated = activeQuests[index].objectives.toMutableList()
            updated.addAll(newObjectives)

            updatePhasesAndTimers(updated) // Check if the new items need timers started!
            activeQuests[index] = activeQuests[index].copy(objectives = updated)
        }
    }

    fun removeObjective(questId: String, objectiveId: String) {
        val qIdx = activeQuests.indexOfFirst { it.id == questId }
        if (qIdx != -1) {
            val quest = activeQuests[qIdx]
            // Filters out the deleted objective
            val updatedObjs = quest.objectives.filter { it.id != objectiveId }.toMutableList()

            updatePhasesAndTimers(updatedObjs) // Recalculate phases in case a blocker was deleted!
            activeQuests[qIdx] = quest.copy(objectives = updatedObjs)
        }
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

    // --- THE NEW PHASE SCANNER ---
    // This evaluates the entire quest to find active phases and trigger unlocked timers!
    private fun updatePhasesAndTimers(objectives: MutableList<Objective>) {
        var hideRemaining = false
        var groupIncomplete = false

        for (i in objectives.indices) {
            val o = objectives[i]

            // If this item is told to wait, and the previous group isn't done yet, trip the wire!
            if (o.waitForPrevious) {
                if (groupIncomplete) hideRemaining = true
                groupIncomplete = false // Reset for the new phase
            }


            // If we are in the active phase, evaluate it!
            if (!hideRemaining) {
                if (!o.isComplete && !o.isOptional) {
                    groupIncomplete = true

                    // If it's a timer that just got unlocked, START IT!
                    if (o.type == ObjectiveType.TIMED_WAIT && o.waitStartTimeMs == null) {
                        objectives[i] = o.copy(waitStartTimeMs = getSystemEpochMillis())
                        if (o.waitDurationMs != null && o.waitDurationMs > 0) {
                            val safeId = kotlin.random.Random.nextInt(1, 9999)
                            scheduleObjectiveNotification(safeId, "PIP-BOY DIRECTIVE UPDATE", o.postWaitText ?: "Timer Complete!", o.waitDurationMs)
                        }
                    }
                }
            }
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
