package com.dravenmiller.overseersterminal.ui.tabs

import LocalAuthBridge
import PipPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dravenmiller.overseersterminal.AuthHolder
import com.dravenmiller.overseersterminal.GoogleAuthBridge
import com.dravenmiller.overseersterminal.components.*
import com.dravenmiller.overseersterminal.theme.ThemeController
import kotlinx.coroutines.delay
import com.dravenmiller.overseersterminal.CalendarEvent
import com.dravenmiller.overseersterminal.CalendarProcessor

@Composable
fun QuestsSubTab(themeController: ThemeController) {
    var selectedQuestId by rememberSaveable { mutableStateOf<String?>(null) }

    // --- TERMINAL OVERLAY STATES ---
    var showCreateQuest by rememberSaveable { mutableStateOf(false) }
    var showAddObjectiveTo by rememberSaveable { mutableStateOf<String?>(null) }
    var showCompletionPromptFor by rememberSaveable { mutableStateOf<String?>(null) }

    val authBridge = LocalAuthBridge.current

    // Tracks which Quest ID and Objective Index to add funds to!
    var showAppendFundsFor by rememberSaveable { mutableStateOf<Pair<String, Int>?>(null) }

    // --- THE LIVE TICK ENGINE ---
    var liveTimeMs by remember { mutableStateOf(getSystemEpochMillis()) }
    LaunchedEffect(Unit) {
        while(true) {
            delay(1000)
            liveTimeMs = getSystemEpochMillis()
        }
    }

    if (selectedQuestId == null) {
        val ct = getSystemEpochMillis()
        selectedQuestId = QuestEngine.activeQuests.firstOrNull { !it.isComplete && it.spawnTimeMs <= ct }?.id
            ?: QuestEngine.activeQuests.firstOrNull { it.isComplete }?.id
    }

    val selectedQuest = QuestEngine.activeQuests.find { it.id == selectedQuestId }

    Box(modifier = Modifier.fillMaxSize()) {

        Row(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {

            // --- LEFT COLUMN ---
            Column(modifier = Modifier.weight(0.4f).fillMaxHeight().padding(end = 16.dp)) {

                PipText("[ + INITIALIZE NEW QUEST ]", themeController = themeController, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().clickable { showCreateQuest = true }.padding(bottom = 16.dp))

                Spacer(modifier     = Modifier.height(16.dp))
                LazyColumn {
                    val currentTime = getSystemEpochMillis()
                    val visibleActive = QuestEngine.activeQuests.filter { !it.isComplete && it.spawnTimeMs <= currentTime }
                    val completed = QuestEngine.activeQuests.filter { it.isComplete }

                    visibleActive.groupBy { it.category }.forEach { (category, quests) ->
                        item { PipText("--- ${category.name} ---", themeController, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) }

                        items(quests) { quest ->
                            val isSelected = quest.id == selectedQuestId
                            val activeIcon = if (quest.isActive) "■ " else "  "

                            Row(
                                modifier = Modifier.fillMaxWidth().background(if (isSelected) themeController.activeColor else Color.Transparent)
                                    .clickable { selectedQuestId = quest.id }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PipText("$activeIcon${quest.title}", themeController, textColorOverride = if (isSelected) Color.Black else null, fontSize = 16.sp)
                            }
                        }
                    }

                    if (completed.isNotEmpty()) {
                        item { PipText("--- ARCHIVED LOGS ---", themeController, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) }
                        items(completed) { quest ->
                            val isSelected = quest.id == selectedQuestId
                            Row(modifier = Modifier.fillMaxWidth().background(if (isSelected) themeController.activeColor else Color.Transparent).clickable { selectedQuestId = quest.id }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                PipText("  ${quest.title}", themeController, textColorOverride = if (isSelected) Color.Black else null, fontSize = 16.sp, modifier = Modifier.alpha(if (isSelected) 1.0f else 0.5f))
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(themeController.activeColor.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.width(16.dp))

            // --- RIGHT COLUMN ---
            Column(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                if (selectedQuest != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp).border(2.dp, themeController.activeColor), contentAlignment = Alignment.Center) {
                        PipText(if (selectedQuest.isComplete) "[ QUEST COMPLETED ]" else "[ VAULT BOY ANIMATION ]", themeController)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    PipText(selectedQuest.title.uppercase(), themeController, fontSize = 24.sp, modifier = Modifier.alpha(if (selectedQuest.isComplete) 0.5f else 1.0f))
                    if (selectedQuest.repeatInterval != null) {
                        PipText("RADIANT LOOP: ${selectedQuest.repeatInterval}", themeController, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // THE PHASE REVEALER
                    val visibleObjectives = mutableListOf<Pair<Int, Objective>>()
                    var hideRemaining = false
                    var currentGroupIncomplete = false

                    for (i in selectedQuest.objectives.indices) {
                        val obj = selectedQuest.objectives[i]

                        // If this item starts a new phase, check if the previous phase was finished!
                        if (obj.waitForPrevious) {
                            if (currentGroupIncomplete) hideRemaining = true
                            currentGroupIncomplete = false // Reset for the new phase
                        }

                        if (hideRemaining) continue // Skips rendering future locked phases

                        visibleObjectives.add(i to obj)

                        // If anything mandatory in this phase is incomplete, it flags the phase as unfinished
                        if (!obj.isComplete && !obj.isOptional) {
                            currentGroupIncomplete = true
                        }
                    }
                    visibleObjectives.reverse() // Keep the newest active step at the top


                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(visibleObjectives.size) { index ->
                            val (realIndex, objective) = visibleObjectives[index]
                            val bgModifier = if (!objective.isComplete) Modifier.background(themeController.activeColor.copy(alpha = 0.2f)) else Modifier

                            // MATH: Evaluate Timer state!
                            var isTimerLocked = false
                            var timerDisplay = ""

                            if (objective.type == ObjectiveType.TIMED_WAIT && !objective.isComplete) {
                                if (objective.waitStartTimeMs != null && objective.waitDurationMs != null) {
                                    val elapsed = liveTimeMs - objective.waitStartTimeMs!!
                                    val remaining = objective.waitDurationMs - elapsed

                                    if (remaining > 0) {
                                        isTimerLocked = true
                                        val secs = (remaining / 1000) % 60
                                        val mins = (remaining / 60000) % 60
                                        val hrs = (remaining / 3600000)
                                        fun padZero(num: Long): String = if (num < 10) "0$num" else "$num"
                                        timerDisplay = if (hrs > 0) "${padZero(hrs)}:${padZero(mins)}:${padZero(secs)}" else "${padZero(mins)}:${padZero(secs)}"
                                    }
                                } else {
                                    isTimerLocked = true
                                    timerDisplay = "AWAITING TRIGGER"
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).then(bgModifier).clickable {
                                // THE FIX: Instantly reject clicks if it's locked or already complete!
                                if (objective.isComplete) return@clickable
                                if (isTimerLocked) return@clickable

                                if (objective.type == ObjectiveType.COLLECTION) {
                                    showAppendFundsFor = Pair(selectedQuest.id, realIndex)
                                } else {
                                    val questComplete = QuestEngine.toggleObjective(selectedQuest.id, realIndex)
                                    if (questComplete) showCompletionPromptFor = selectedQuest.id
                                }
                            }.drawBehind {
                                if (objective.isComplete) {
                                    drawLine(color = themeController.activeColor.copy(alpha = 0.5f), start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2), strokeWidth = 2.dp.toPx())
                                }
                            }.padding(8.dp)) {

                                // THE DYNAMIC TEXT
                                val optTag = if (objective.isOptional) "[OPTIONAL] " else ""

                                if (isTimerLocked) {
                                    PipText("$optTag[ CHECK STATUS: $timerDisplay ]", themeController, fontSize = 16.sp, modifier = Modifier.alpha(0.8f))
                                } else if (objective.type == ObjectiveType.TIMED_WAIT && objective.postWaitText != null) {
                                    PipText("$optTag${objective.postWaitText}", themeController, fontSize = 16.sp, modifier = if (objective.isComplete) Modifier.alpha(0.5f) else Modifier)
                                } else {
                                    PipText("$optTag${objective.text}", themeController, fontSize = 16.sp, modifier = if (objective.isComplete) Modifier.alpha(0.5f) else Modifier)
                                }

                                // COLLECTION BAR
                                if (objective.type == ObjectiveType.COLLECTION) {
                                    val prefix = if (objective.isCurrency) "FUNDS:" else "COLLECTED:"
                                    val currentStr = formatCollectionAmount(objective.currentAmount, objective.isCurrency)
                                    val targetStr = formatCollectionAmount(objective.targetAmount ?: 0f, objective.isCurrency)

                                    PipText("$prefix [ $currentStr / $targetStr ]", themeController, fontSize = 14.sp)
                                }

                                // METADATA DISPLAY
                                if (!objective.isComplete) {
                                    if (objective.date != null || objective.time != null) {
                                        PipText("  TIME: ${objective.date ?: ""} ${objective.time ?: ""}", themeController, fontSize = 12.sp, modifier = Modifier.alpha(0.8f))
                                    }
                                    if (objective.location != null) {
                                        PipText("  LOC:  ${objective.location}", themeController, fontSize = 12.sp, modifier = Modifier.alpha(0.8f))
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        val trackText = if (selectedQuest.isActive) "[ UNTRACK ]" else "[ TRACK ]"
                        PipText(trackText, themeController, fontSize = 14.sp, modifier = Modifier.clickable {
                            val idx = QuestEngine.activeQuests.indexOfFirst { it.id == selectedQuest.id }
                            if (idx != -1) QuestEngine.activeQuests[idx] = selectedQuest.copy(isActive = !selectedQuest.isActive)
                        })
                        PipText("[ ADD OBJECTIVE ]", themeController, fontSize = 14.sp, modifier = Modifier.clickable { showAddObjectiveTo = selectedQuest.id })
                    }
                }
            }
        }

        // --- LAYER 2: THE TERMINAL OVERLAYS ---
        if (showCreateQuest) {
            CreateQuestTerminal(themeController, onCancel = { showCreateQuest = false },
                onSave = { title, category, repeatInt, saveTemp, isSeq ->
                    // 1. Create the empty quest and grab the ID
                    val newId = QuestEngine.createNewQuest(title, category, repeatInt, saveTemp, isSeq)
                    showCreateQuest = false

                    // 2. Select it in the background list
                    selectedQuestId = newId

                    // 3. THE WIZARD CHAIN: Instantly open the objective terminal!
                    showAddObjectiveTo = newId
                }
            )
        }

        if (showAddObjectiveTo != null) {
            AddObjectiveTerminal(
                questId = showAddObjectiveTo!!, // THE FIX: We pass the ID in so it can read the list!
                themeController = themeController,
                onCancel = { showAddObjectiveTo = null },
                onSave = { objectives ->
                    QuestEngine.addObjectives(showAddObjectiveTo!!, objectives)
                    showAddObjectiveTo = null
                }
            )
        }


        if (showAppendFundsFor != null) {
            AppendFundsTerminal(themeController, onCancel = { showAppendFundsFor = null },
                onSave = { amount ->
                    val questComplete = QuestEngine.addFundsToObjective(showAppendFundsFor!!.first, showAppendFundsFor!!.second, amount)
                    showAppendFundsFor = null
                    if (questComplete) showCompletionPromptFor = selectedQuestId
                }
            )
        }

        if (showCompletionPromptFor != null) {
            QuestUpdatePrompt(themeController,
                onCompleteQuest = {
                    QuestEngine.completeQuest(showCompletionPromptFor!!)
                    showCompletionPromptFor = null
                },
                onAddNextObjective = {
                    val questId = showCompletionPromptFor!!
                    showCompletionPromptFor = null
                    showAddObjectiveTo = questId
                },
                onContinue = { showCompletionPromptFor = null }
            )
        }
    }
}

// =========================================================================
// TERMINAL POPUPS
// =========================================================================

@Composable
fun CreateQuestTerminal(themeController: ThemeController, onCancel: () -> Unit, onSave: (String, QuestCategory, String?, Boolean, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(QuestCategory.MAIN) }
    var saveAsTemplate by remember { mutableStateOf(false) }
    var isSequential by remember { mutableStateOf(true) }

    var repeatNum by remember { mutableStateOf("1") }
    val units = listOf("DAYS", "WEEKS", "MONTHS", "YEARS")
    var unitIndex by remember { mutableStateOf(1) }
    // Holds the data from Google
    var importedEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    // Shows a loading state
    var isFetching by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { onCancel() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).imePadding(), contentAlignment = Alignment.Center) {
            val scrollState = androidx.compose.foundation.rememberScrollState()

            Column(modifier = Modifier.fillMaxSize().border(2.dp, themeController.activeColor).background(Color.Black).padding(16.dp).verticalScroll(scrollState)) {
                PipText(">>> ESTABLISH NEW DIRECTIVE <<<", themeController, fontSize = 20.sp)
                Spacer(Modifier.height(16.dp))

                BasicTextField(value = title, onValueChange = { title = it }, textStyle = TextStyle(color = themeController.activeColor, fontSize = 20.sp), cursorBrush = SolidColor(themeController.activeColor), modifier = Modifier.fillMaxWidth().background(themeController.activeColor.copy(alpha = 0.1f)).padding(8.dp), decorationBox = { inner -> if (title.isEmpty()) PipText("QUEST DESIGNATION...", themeController, modifier = Modifier.alpha(0.5f)) else inner() })

                Spacer(Modifier.height(24.dp))
                PipText("CATEGORY CLASSIFICATION:", themeController, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))

                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(QuestCategory.values().size) { index ->
                        val cat = QuestCategory.values()[index]
                        val isSel = selectedCategory == cat
                        val catIcon = when(cat) {
                            QuestCategory.MAIN -> androidx.compose.material.icons.Icons.Filled.Star
                            QuestCategory.SIDE -> androidx.compose.material.icons.Icons.Filled.List
                            QuestCategory.RADIANT -> androidx.compose.material.icons.Icons.Filled.Autorenew
                            QuestCategory.GOALS -> androidx.compose.material.icons.Icons.Filled.Flag
                        }

                        Column(modifier = Modifier.background(if (isSel) themeController.activeColor else Color.Transparent).border(1.dp, themeController.activeColor).clickable { selectedCategory = cat }.padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.Icon(imageVector = catIcon, contentDescription = cat.name, tint = if (isSel) Color.Black else themeController.activeColor, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(8.dp))
                            PipText(cat.name, themeController, textColorOverride = if (isSel) Color.Black else null, fontSize = 12.sp)
                        }
                    }
                }

                if (selectedCategory == QuestCategory.RADIANT) {
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PipText("EVERY:", themeController, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(value = repeatNum, onValueChange = { repeatNum = it.filter { char -> char.isDigit() } }, textStyle = TextStyle(color = themeController.activeColor, fontSize = 20.sp), cursorBrush = SolidColor(themeController.activeColor), modifier = Modifier.width(60.dp).background(themeController.activeColor.copy(alpha = 0.1f)).padding(8.dp))
                        Spacer(Modifier.width(16.dp))
                        PipPicker(title = "", options = units, selectedIndex = unitIndex, themeController = themeController, onOptionSelected = { unitIndex = it })
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 1. NEW PLACEMENT: Put the Transmissions List HERE, before the bottom buttons!
                if (importedEvents.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PipText(
                            "--- INCOMING TRANSMISSIONS ---",
                            themeController,
                            fontSize = 12.sp,
                            modifier = Modifier.alpha(0.8f).padding(bottom = 8.dp)
                        )

                        // Draw each event
                        importedEvents.forEach { event ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // The Event Title
                                PipText(
                                    ">> ${event.title}",
                                    themeController,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )

                                // The Action Button
                                PipText(
                                    text = "[ CONVERT ]",
                                    themeController = themeController,
                                    fontSize = 12.sp,
                                    modifier = Modifier.border(1.dp, themeController.activeColor)
                                        .clickable {
                                            // Auto-fill the Quest Title box!
                                            title = event.title
                                        }.padding(4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(themeController.activeColor.copy(alpha = 0.3f)))
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // 2. THE BOTTOM ACTION ROW (Keep this isolated!)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        PipText(if (saveAsTemplate) "[☑] SAVE TEMPLATE" else "[ ] SAVE TEMPLATE", themeController, fontSize = 12.sp, modifier = Modifier.clickable { saveAsTemplate = !saveAsTemplate })
                        Spacer(Modifier.height(8.dp))
                        SyncButton(themeController)
                        Spacer(Modifier.height(8.dp))
                        PipText(
                            text = if (isFetching) "[ ESTABLISHING UPLINK... ]" else "[ FETCH CALENDAR DATA ]",
                            themeController = themeController,
                            textColorOverride = Color.Black,
                            modifier = Modifier
                                .background(themeController.activeColor)
                                .clickable {
                                    if (isFetching) return@clickable
                                    isFetching = true

                                    AuthHolder.bridge.value?.fetchUpcomingEvents { events ->
                                        isFetching = false
                                        if (events != null) {
                                            importedEvents = events
                                            println("Fetched ${events.size} events!")
                                        } else {
                                            println("Uplink failed.")
                                        }
                                    }
                                }
                                .padding(8.dp)
                        )
                    }

                    Row {
                        PipText("[ CANCEL ]", themeController, modifier = Modifier.clickable { onCancel() }.padding(8.dp))
                        PipText("[ INITIALIZE ]", themeController, textColorOverride = Color.Black, modifier = Modifier.background(themeController.activeColor).clickable {
                            if (title.isNotBlank()) {
                                val finalInterval = if (selectedCategory == QuestCategory.RADIANT) "$repeatNum ${units[unitIndex]}" else null
                                onSave(title, selectedCategory, finalInterval, saveAsTemplate, isSequential)
                            }
                        }.padding(8.dp))
                    }
                }
            }
        }
    }
}


@Composable
fun AddObjectiveTerminal(questId: String, themeController: ThemeController, onCancel: () -> Unit, onSave: (List<Objective>) -> Unit) {
    val existingObjectives = QuestEngine.activeQuests.find { it.id == questId }?.objectives ?: emptyList()

    var text by remember { mutableStateOf("") }
    val objTypes = listOf("STANDARD", "TIMED WAIT", "COLLECTION")
    var selectedTypeIndex by remember { mutableStateOf(0) }
    var targetAmount by remember { mutableStateOf("") }
    var isCurrency by remember { mutableStateOf(true) }
    var waitTime by remember { mutableStateOf("") }
    val timeUnits = listOf("MINUTES", "HOURS", "DAYS")
    var timeUnitIndex by remember { mutableStateOf(0) }
    var postWaitText by remember { mutableStateOf("") }
    var waitForPrevious by remember { mutableStateOf(true) }
    var isOptional by remember { mutableStateOf(false) } // <-- NEW TOGGLE STATE
    val queued = remember { mutableStateListOf<Objective>() }

    // --- LIVE PHASE CALCULATOR ---
    val combinedList = existingObjectives + queued
    var latestPhase = 1
    val phaseMap = mutableMapOf<String, Int>() // Maps Objective IDs to their Phase #

    for (i in combinedList.indices) {
        val obj = combinedList[i]
        if (i > 0 && obj.waitForPrevious) latestPhase++
        phaseMap[obj.id] = latestPhase
    }
    val pendingPhaseNum = if (combinedList.isEmpty()) 1 else if (waitForPrevious) latestPhase + 1 else latestPhase

    // --- NISKA'S EMOJI HELPER ---
    fun getEmoji(type: ObjectiveType): String = when(type) {
        ObjectiveType.TIMED_WAIT -> " ⏳"
        ObjectiveType.COLLECTION -> " 💰"
        ObjectiveType.STANDARD -> ""
    }

    Dialog(onDismissRequest = { onCancel() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).imePadding(), contentAlignment = Alignment.Center) {
            val scrollState = androidx.compose.foundation.rememberScrollState()

            Column(modifier = Modifier.fillMaxSize().border(2.dp, themeController.activeColor).background(Color.Black).padding(16.dp).verticalScroll(scrollState)) {
                PipText(">>> APPEND OBJECTIVE LOG <<<", themeController, fontSize = 20.sp)
                Spacer(Modifier.height(16.dp))

                // PHASE-SEPARATED OBJECTIVE DISPLAY
                if (existingObjectives.isNotEmpty()) {
                    val pastObjs = mutableListOf<Objective>()
                    val currentObjs = mutableListOf<Objective>()
                    val futureObjs = mutableListOf<Objective>()
                    var hideRem = false
                    var grpInc = false

                    existingObjectives.forEach { obj ->
                        if (obj.waitForPrevious) {
                            if (grpInc) hideRem = true
                            grpInc = false
                        }
                        if (hideRem) futureObjs.add(obj) else if (obj.isComplete) pastObjs.add(obj) else { currentObjs.add(obj); if (!obj.isOptional) grpInc = true }
                    }

                    // DRAWS THE CATEGORIES USING THE NEW IMMERSIVE ICONS
                    if (pastObjs.isNotEmpty()) {
                        PipText("--- PAST OBJECTIVES ---", themeController, fontSize = 12.sp, modifier = Modifier.alpha(0.5f))
                        pastObjs.forEach { obj -> ObjectiveDisplayRow(obj, phaseMap[obj.id], isLocked = false, isPast = true, themeController) }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (currentObjs.isNotEmpty()) {
                        PipText("--- CURRENT PHASE ---", themeController, fontSize = 12.sp, textColorOverride = Color.Black, modifier = Modifier.background(themeController.activeColor).padding(horizontal = 4.dp))
                        currentObjs.forEach { obj ->
                            // THE FIX: This is the ONLY currentObjs block now, and it includes the delete command!
                            ObjectiveDisplayRow(obj, phaseMap[obj.id], isLocked = false, isPast = false, themeController) {
                                QuestEngine.removeObjective(questId, obj.id)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (futureObjs.isNotEmpty()) {
                        PipText("--- FUTURE OBJECTIVES ---", themeController, fontSize = 12.sp, modifier = Modifier.alpha(0.5f))
                        futureObjs.forEach { obj ->
                            // Optional: If you want to delete locked future objectives too, you can add the same { QuestEngine.removeObjective... } lambda to the end of this row!
                            ObjectiveDisplayRow(obj, phaseMap[obj.id], isLocked = true, isPast = false, themeController) {
                                QuestEngine.removeObjective(questId, obj.id)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(themeController.activeColor.copy(alpha = 0.5f)))
                    Spacer(Modifier.height(16.dp))
                }

                PipPicker(title = "OBJECTIVE CLASSIFICATION:", options = objTypes, selectedIndex = selectedTypeIndex, themeController = themeController, onOptionSelected = { selectedTypeIndex = it })
                Spacer(Modifier.height(16.dp))

                if (selectedTypeIndex != 1) {
                    BasicTextField(value = text, onValueChange = { text = it }, textStyle = TextStyle(color = themeController.activeColor, fontSize = 16.sp), cursorBrush = SolidColor(themeController.activeColor), modifier = Modifier.fillMaxWidth().background(themeController.activeColor.copy(alpha = 0.1f)).padding(8.dp), decorationBox = { inner -> if (text.isEmpty()) PipText("OBJECTIVE TEXT...", themeController, modifier = Modifier.alpha(0.5f)) else inner() })
                }

                if (selectedTypeIndex == 2) {
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(value = targetAmount, onValueChange = { targetAmount = it.filter { char -> char.isDigit() || char == '.' } }, textStyle = TextStyle(color = themeController.activeColor, fontSize = 16.sp), cursorBrush = SolidColor(themeController.activeColor), modifier = Modifier.fillMaxWidth().background(themeController.activeColor.copy(alpha = 0.1f)).padding(8.dp), decorationBox = { inner -> if (targetAmount.isEmpty()) PipText("TARGET AMOUNT (e.g. 5000)...", themeController, modifier = Modifier.alpha(0.5f)) else inner() })
                    Spacer(Modifier.height(8.dp))
                    PipText(if (isCurrency) "[☑] FORMAT AS CURRENCY" else "[ ] FORMAT AS CURRENCY", themeController, fontSize = 12.sp, modifier = Modifier.clickable { isCurrency = !isCurrency })
                }

                if (selectedTypeIndex == 1) {
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(value = waitTime, onValueChange = { waitTime = it.filter { char -> char.isDigit() } }, textStyle = TextStyle(color = themeController.activeColor, fontSize = 16.sp), cursorBrush = SolidColor(themeController.activeColor), modifier = Modifier.weight(0.4f).background(themeController.activeColor.copy(alpha = 0.1f)).padding(8.dp), decorationBox = { inner -> if (waitTime.isEmpty()) PipText("WAIT...", themeController, modifier = Modifier.alpha(0.5f)) else inner() })
                        Spacer(Modifier.width(8.dp))
                        PipPicker(title = "", options = timeUnits, selectedIndex = timeUnitIndex, themeController = themeController, onOptionSelected = { timeUnitIndex = it })
                    }
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(value = postWaitText, onValueChange = { postWaitText = it }, textStyle = TextStyle(color = themeController.activeColor, fontSize = 16.sp), cursorBrush = SolidColor(themeController.activeColor), modifier = Modifier.fillMaxWidth().background(themeController.activeColor.copy(alpha = 0.1f)).padding(8.dp), decorationBox = { inner -> if (postWaitText.isEmpty()) PipText("POST-WAIT TEXT (e.g. Dry Sheets)...", themeController, modifier = Modifier.alpha(0.5f)) else inner() })
                }

                Spacer(Modifier.height(16.dp))
                PipText("[ QUEUE OBJECTIVE ]", themeController, modifier = Modifier.align(Alignment.End).clickable {
                    val activeText = if (selectedTypeIndex == 1) postWaitText else text
                    if (activeText.isNotBlank()) {
                        val type = ObjectiveType.values()[selectedTypeIndex]
                        val msMultiplier = when(timeUnits[timeUnitIndex]) { "MINUTES" -> 60000L; "HOURS" -> 3600000L; "DAYS" -> 86400000L; else -> 0L }
                        val durationMs = (waitTime.toLongOrNull() ?: 0L) * msMultiplier
                        queued.add(Objective(text = activeText, type = type, targetAmount = targetAmount.toFloatOrNull(), isCurrency = isCurrency, waitDurationMs = if (durationMs > 0) durationMs else null, postWaitText = postWaitText.ifBlank { null }, waitForPrevious = waitForPrevious, isOptional = isOptional))
                        text = ""; targetAmount = ""; waitTime = ""; postWaitText = ""
                    }
                })

                // DRAWS THE QUEUE USING THE NEW IMMERSIVE ICONS
                if (queued.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    PipText("--- PENDING QUEUE ---", themeController, fontSize = 12.sp, modifier = Modifier.alpha(0.8f))
                    queued.forEach { obj ->
                        ObjectiveDisplayRow(obj, phaseMap[obj.id], isLocked = false, isPast = false, themeController) {
                            queued.remove(obj) // Instantly deletes from the draft queue!
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        PipText(if (isOptional) "[☑] OPTIONAL DIRECTIVE" else "[ ] OPTIONAL DIRECTIVE", themeController, fontSize = 12.sp, modifier = Modifier.clickable { isOptional = !isOptional })
                        Spacer(Modifier.height(8.dp))
                        PipText(text = if (combinedList.isEmpty()) "[☑] INITIAL PHASE (PHASE 1)" else if (waitForPrevious) "[☑] START NEW PHASE (PHASE $pendingPhaseNum)" else "[ ] ADD TO CURRENT PHASE (PHASE $pendingPhaseNum)", themeController = themeController, fontSize = 12.sp, modifier = Modifier.clickable { if (combinedList.isNotEmpty()) waitForPrevious = !waitForPrevious })
                    }

                    Row {
                        PipText("[ CANCEL ]", themeController, modifier = Modifier.clickable { onCancel() }.padding(8.dp))
                        Spacer(Modifier.width(16.dp))
                        PipText("[ APPEND ]", themeController, textColorOverride = Color.Black, modifier = Modifier.background(themeController.activeColor).clickable {
                            val finalQueued = queued.toMutableList()
                            val activeText = if (selectedTypeIndex == 1) postWaitText else text
                            if (activeText.isNotBlank()) {
                                val type = ObjectiveType.values()[selectedTypeIndex]
                                val msMultiplier = when(timeUnits[timeUnitIndex]) { "MINUTES" -> 60000L; "HOURS" -> 3600000L; "DAYS" -> 86400000L; else -> 0L }
                                val durationMs = (waitTime.toLongOrNull() ?: 0L) * msMultiplier
                                finalQueued.add(Objective(text = activeText, type = type, targetAmount = targetAmount.toFloatOrNull(), isCurrency = isCurrency, waitDurationMs = if (durationMs > 0) durationMs else null, postWaitText = postWaitText.ifBlank { null }, waitForPrevious = waitForPrevious, isOptional = isOptional))
                            }
                            if (finalQueued.isNotEmpty()) onSave(finalQueued)
                        }.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ObjectiveDisplayRow(obj: Objective, phaseNum: Int?, isLocked: Boolean, isPast: Boolean, themeController: ThemeController, onDelete: (() -> Unit)? = null) {
    val opt = if (obj.isOptional) "[OPTIONAL] " else ""

    // We split the alpha so the text fades, but the icons and [X] stay bright!
    val textAlpha = if (isLocked) 0.3f else if (isPast) 0.5f else 1.0f
    val iconAlpha = if (isLocked) 0.3f else 1.0f // Checkmarks and icons stay at 100%

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {

            // 1. Draws the Checkmark OR the Lock Icon!
            if (isLocked) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = themeController.activeColor,
                    modifier = Modifier.size(14.dp).alpha(iconAlpha)
                )
                Spacer(Modifier.width(6.dp)) // Adds a small gap after the lock
            } else {
                val checkText = if (isPast) "[X]" else "[ ]"
                PipText("$checkText ", themeController, fontSize = 12.sp, modifier = Modifier.alpha(iconAlpha))
            }

            // 2. Draws the Objective Text (Dimmed if past)
            PipText("[PHASE $phaseNum] $opt${obj.text}", themeController, fontSize = 12.sp, modifier = Modifier.alpha(textAlpha))

            // 3. Draws the Type Icons (Bright)
            if (obj.type == ObjectiveType.TIMED_WAIT) {
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Notifications, contentDescription = "Timer", tint = themeController.activeColor, modifier = Modifier.size(14.dp).alpha(iconAlpha))
            } else if (obj.type == ObjectiveType.COLLECTION) {
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.Icon(imageVector = androidx.compose.material.icons.Icons.Filled.ShoppingCart, contentDescription = "Collection", tint = themeController.activeColor, modifier = Modifier.size(14.dp).alpha(iconAlpha))
            }
        }

        // THE DELETE BUTTON (Now a gorgeous red trash can!)
        if (onDelete != null) {
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = Color.Red,
                modifier = Modifier.size(16.dp).clickable { onDelete() }
            )
        }
    }
}


// --- NEW COLLECTION TERMINAL ---
@Composable
fun AppendFundsTerminal(themeController: ThemeController, onCancel: () -> Unit, onSave: (Float) -> Unit) {
    var amount by remember { mutableStateOf("") }
    Dialog(onDismissRequest = { onCancel() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).imePadding(), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.fillMaxWidth(0.8f).border(2.dp, themeController.activeColor).background(Color.Black).padding(24.dp)) {
                PipText(">>> APPEND FUNDS <<<", themeController, fontSize = 20.sp)
                Spacer(Modifier.height(24.dp))
                BasicTextField(value = amount, onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } }, textStyle = TextStyle(color = themeController.activeColor, fontSize = 20.sp), cursorBrush = SolidColor(themeController.activeColor), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(themeController.activeColor.copy(alpha = 0.1f)).padding(8.dp), decorationBox = { inner -> if (amount.isEmpty()) PipText("AMOUNT...", themeController, modifier = Modifier.alpha(0.5f)) else inner() })
                Spacer(Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    PipText("[ CANCEL ]", themeController, modifier = Modifier.clickable { onCancel() }.padding(8.dp))
                    Spacer(Modifier.width(16.dp))
                    PipText("[ SUBMIT ]", themeController, textColorOverride = Color.Black, modifier = Modifier.background(themeController.activeColor).clickable {
                        val parsed = amount.toFloatOrNull()
                        if (parsed != null && parsed > 0f) onSave(parsed)
                    }.padding(8.dp))
                }
            }
        }
    }
}

@Composable
fun QuestUpdatePrompt(themeController: ThemeController, onCompleteQuest: () -> Unit, onAddNextObjective: () -> Unit, onContinue: () -> Unit) {
    Dialog(onDismissRequest = { onContinue() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).imePadding(), contentAlignment = Alignment.Center) {
            Column(modifier = Modifier.fillMaxWidth(0.8f).border(2.dp, themeController.activeColor).background(Color.Black).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PipText(">>> OBJECTIVE ACCOMPLISHED <<<", themeController, fontSize = 20.sp)
                Spacer(Modifier.height(24.dp))
                PipText("Does this conclude the current quest parameters?", themeController, fontSize = 16.sp)
                Spacer(Modifier.height(32.dp))
                PipText("[ YES - QUEST COMPLETE ]", themeController, textColorOverride = Color.Black, modifier = Modifier.fillMaxWidth().background(themeController.activeColor).clickable { onCompleteQuest() }.padding(12.dp))
                Spacer(Modifier.height(16.dp))
                PipText("[ NO - ADD NEXT OBJECTIVE ]", themeController, modifier = Modifier.fillMaxWidth().border(1.dp, themeController.activeColor).clickable { onAddNextObjective() }.padding(12.dp))
                Spacer(Modifier.height(16.dp))
                PipText("[ NO - RESUME LOG ]", themeController, modifier = Modifier.fillMaxWidth().border(1.dp, themeController.activeColor).clickable { onContinue() }.padding(12.dp))
            }
        }
    }
}

// =========================================================================
// PURE KOTLIN FORMATTER (KMP Safe!)
// =========================================================================
fun formatCollectionAmount(value: Float, isCurrency: Boolean): String {
    return if (isCurrency) {
        // Splits the float into whole dollars and exact 2-digit cents!
        val totalCents = (value * 100).toLong()
        val dollars = totalCents / 100
        val cents = (totalCents % 100).toString().padStart(2, '0')
        "$$dollars.$cents"
    } else {
        // If it's a perfectly clean number (like 5.0), drop the decimal entirely!
        if (value % 1.0f == 0f) value.toLong().toString() else value.toString()
    }
}

@Composable
fun SyncButton(themeController: ThemeController) {
    // We observe the state
    val bridge = AuthHolder.bridge.value

    PipText(
        text = if (bridge != null) "[ SYNC GOOGLE CALENDAR ]" else "[ LOADING... ]",
        themeController = themeController,
        modifier = Modifier.clickable {
            // Only call if not null
            bridge?.startSignIn { success ->
                println("Sync result: $success")
            }
        }
    )
}
