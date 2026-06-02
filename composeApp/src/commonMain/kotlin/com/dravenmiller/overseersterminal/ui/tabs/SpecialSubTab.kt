package com.dravenmiller.overseersterminal.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dravenmiller.overseersterminal.components.PipText
import com.dravenmiller.overseersterminal.components.StatEngine
import com.dravenmiller.overseersterminal.theme.ThemeController

@Composable
fun SpecialSubTab(themeController: ThemeController) {
    val stats = listOf(
        Triple("STRENGTH", StatEngine.getStrength(), "Determines raw physical power. Governed by Swole Scroll Max Lifts."),
        Triple("PERCEPTION", StatEngine.getPerception(), "Environmental awareness and system sensors."),
        Triple("ENDURANCE", StatEngine.getEndurance(), "Overall physical fitness and cardiovascular health."),
        Triple("CHARISMA", StatEngine.getCharisma(), "Network size. Governed by terminal contact list."),
        Triple("INTELLIGENCE", StatEngine.getIntelligence(), "Mental acuity. Governed by GitHub programming languages."),
        Triple("AGILITY", StatEngine.getAgility(), "Finesse and speed."),
        Triple("LUCK", StatEngine.getLuck(), "General good fortune.")
    )

    // Saves the index through rotation
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val selectedStat = stats[selectedIndex]

    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // --- LEFT COLUMN: CLEAN LIST ---
        Column(modifier = Modifier.weight(0.4f).fillMaxHeight().padding(end = 16.dp)) {
            stats.forEachIndexed { index, stat ->
                val isSelected = selectedIndex == index
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (isSelected) themeController.activeColor.copy(alpha = 0.3f) else Color.Transparent)
                        .clickable { selectedIndex = index }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PipText(stat.first, themeController, fontSize = 20.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    // Number out of 10 aligned to the right!
                    PipText("${stat.second}", themeController, fontSize = 20.sp)
                }
            }
        }

        Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(themeController.activeColor.copy(alpha = 0.3f)))
        Spacer(modifier = Modifier.width(16.dp))

        // --- RIGHT COLUMN: DETAILS ---
        Column(modifier = Modifier.weight(0.6f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).border(2.dp, themeController.activeColor), contentAlignment = Alignment.Center) {
                PipText("[ ${selectedStat.first} ANIMATION ]", themeController)
            }
            Spacer(modifier = Modifier.height(24.dp))
            PipText(selectedStat.first, themeController, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val stars = "★".repeat(selectedStat.second) + "☆".repeat(10 - selectedStat.second)
            PipText("LEVEL: $stars", themeController, fontSize = 18.sp)

            Spacer(modifier = Modifier.height(16.dp))
            PipText(selectedStat.third, themeController, fontSize = 14.sp)
        }
    }
}
