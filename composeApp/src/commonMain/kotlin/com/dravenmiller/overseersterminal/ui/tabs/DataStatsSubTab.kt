package com.dravenmiller.overseersterminal.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dravenmiller.overseersterminal.components.PipText
import com.dravenmiller.overseersterminal.components.QuestEngine
import com.dravenmiller.overseersterminal.components.StatEngine
import com.dravenmiller.overseersterminal.theme.ThemeController

// The classic Fallout 4 Data Logs
enum class StatCategory { GENERAL, QUESTS, COMBAT_AND_FITNESS, CRAFTING_AND_CODE, CRIME_AND_SALES }

@Composable
fun DataStatsSubTab(themeController: ThemeController) {
    var selectedCategory by rememberSaveable { mutableStateOf(StatCategory.GENERAL) }

    // Dynamically groups your life stats based on the selected category!
    val currentStats = when (selectedCategory) {
        StatCategory.GENERAL -> listOf(
            Pair("Locations Discovered", "2"),
            Pair("Days Survived", "14"),
            Pair("Caps Collected", "350")
        )
        StatCategory.QUESTS -> listOf(
            Pair("Quests Completed", "${QuestEngine.questsCompleted}"),
            Pair("Misc Objectives", "12"),
            Pair("Goals Accomplished", "${QuestEngine.goalsAccomplished}")
        )
        StatCategory.COMBAT_AND_FITNESS -> listOf(
            Pair("Workouts Logged", "${StatEngine.exercisePRs.size}"),
            Pair("Heaviest Bench", "${StatEngine.getHighestWeight("Bench")} lbs"),
            Pair("Top Running Speed", "${StatEngine.getHighestSpeed("Run")} MPH")
        )
        StatCategory.CRAFTING_AND_CODE -> listOf(
            Pair("Repositories Pushed", "${StatEngine.codingLanguages.size}"),
            Pair("Lines Written", "ERR: OVERFLOW"),
            Pair("Armor Mods Built", "4")
        )
        StatCategory.CRIME_AND_SALES -> listOf(
            Pair("Items Sold", "0"),
            Pair("Locks Picked", "0"),
            Pair("Terminals Hacked", "1")
        )
    }

    Row(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {

        // ==========================================
        // LEFT COLUMN: THE CATEGORIES
        // ==========================================
        Column(modifier = Modifier.weight(0.35f).fillMaxHeight().padding(end = 16.dp)) {
            StatCategory.values().forEach { category ->
                val isSelected = selectedCategory == category
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (isSelected) themeController.activeColor else Color.Transparent)
                        .clickable { selectedCategory = category }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PipText(
                        text = category.name.replace("_", " "),
                        themeController = themeController,
                        textColorOverride = if (isSelected) Color.Black else null,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(themeController.activeColor.copy(alpha = 0.3f)))
        Spacer(modifier = Modifier.width(16.dp))

        // ==========================================
        // RIGHT COLUMN: THE STATS TABLE
        // ==========================================
        Column(modifier = Modifier.weight(0.65f).fillMaxHeight()) {
            PipText(selectedCategory.name.replace("_", " "), themeController, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(currentStats) { stat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stat Name
                        PipText(stat.first, themeController, fontSize = 16.sp)
                        // Stat Value (Right Aligned)
                        PipText(stat.second, themeController, fontSize = 16.sp)
                    }
                    // The Faint Horizontal Row Divider!
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(themeController.activeColor.copy(alpha = 0.2f)))
                }
            }
        }
    }
}
