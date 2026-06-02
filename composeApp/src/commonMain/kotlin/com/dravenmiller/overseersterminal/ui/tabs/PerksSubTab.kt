package com.dravenmiller.overseersterminal.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import kotlin.math.round

data class PerkDef(val name: String, val level: Int, val maxLevel: Int, val description: String)

@Composable
fun PerksSubTab(themeController: ThemeController) {

    // Dynamically build the Perk List!
    val perks = remember(StatEngine.exercisePRs) {
        val list = mutableListOf<PerkDef>()

        // 1. THE CORE LIFTS (Dynamically catches the exact name of your best variation!)
        val bestBench = StatEngine.exercisePRs.filterKeys { it.contains("Bench", true) }.maxByOrNull { it.value.maxWeight }
        if (bestBench != null) {
            list.add(PerkDef("Barbarian (${bestBench.key})", ((bestBench.value.maxWeight - 45) / 90).toInt().coerceIn(0, 8), 8, "Max PR: ${bestBench.value.maxWeight} lbs\nWorld Record equivalent: 8 plates."))
        }

        val bestSquat = StatEngine.exercisePRs.filterKeys { it.contains("Squat", true) }.maxByOrNull { it.value.maxWeight }
        if (bestSquat != null) {
            list.add(PerkDef("Pack Mule (${bestSquat.key})", ((bestSquat.value.maxWeight - 45) / 90).toInt().coerceIn(0, 11), 11, "Max PR: ${bestSquat.value.maxWeight} lbs\nWorld Record equivalent: 11 plates."))
        }

        val bestDL = StatEngine.exercisePRs.filterKeys { it.contains("Deadlift", true) }.maxByOrNull { it.value.maxWeight }
        if (bestDL != null) {
            list.add(PerkDef("Strong Back (${bestDL.key})", ((bestDL.value.maxWeight - 45) / 90).toInt().coerceIn(0, 11), 11, "Max PR: ${bestDL.value.maxWeight} lbs\nWorld Record equivalent: 11 plates."))
        }

        // 2. THE CARDIO MACHINES (Only allows Bike, Treadmill, and Stairs)
        StatEngine.exercisePRs.forEach { (exerciseName, record) ->
            val nameLower = exerciseName.lowercase()

            // Only trigger if it matches our specific cardio whitelist!
            if (nameLower.contains("bike") || nameLower.contains("treadmill") || nameLower.contains("stair")) {

                if (record.maxTimeSec > 0f || record.maxDistance > 0f) {
                    val timeMins = (record.maxTimeSec / 60).toInt()
                    val timeSecs = (record.maxTimeSec % 60).toInt()
                    val timeStr = "${timeMins}m ${timeSecs}s"

                    // If you logged "weight", it displays as the Machine Level!
                    val resistanceStr = if (record.maxWeight > 0f) "\nMachine Resistance: Level ${record.maxWeight.toInt()}" else ""

                    // Ensures you get at least 1 star even if top speed is 0
                    val calculatedStars = ((record.topSpeedMph / 12f) * 5).toInt().coerceIn(1, 5)

                    // Custom descriptions based on the machine type!
                    val description = if (nameLower.contains("stair")) {
                        "Max Duration: $timeStr$resistanceStr" // No MPH for stairs!
                    } else {
                        val speedRounded = round(record.topSpeedMph * 10) / 10f
                        "Top Speed: $speedRounded MPH\nMax Distance: ${record.maxDistance} Miles\nMax Duration: $timeStr$resistanceStr"
                    }

                    list.add(PerkDef(
                        name = "Action Boy: $exerciseName",
                        level = calculatedStars,
                        maxLevel = 5,
                        description = description
                    ))
                }
            }
        }

        // 3. THE GITHUB HACKER PERKS
        StatEngine.codingLanguages.forEach { (language, sizeKb) ->
            val rank = ((sizeKb / 10000f) * 5).toInt().coerceIn(1, 5)
            list.add(PerkDef(
                name = "Hacker: $language",
                level = rank,
                maxLevel = 5,
                description = "Volume Written: $sizeKb KB\nIncreases overall S.P.E.C.I.A.L. Intelligence."
            ))
        }

        list
    }

    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val selectedPerk = if (perks.isNotEmpty()) perks[selectedIndex.coerceIn(0, perks.lastIndex)] else PerkDef("AWAITING DATA", 0, 0, "NO HOLOTAPE DETECTED")

    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // --- LEFT COLUMN: PERK LIST ---
        Column(modifier = Modifier.weight(0.4f).fillMaxHeight().padding(end = 16.dp)) {
            LazyColumn {
                itemsIndexed(perks) { index, perk ->
                    val isSelected = selectedIndex == index
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(if (isSelected) themeController.activeColor else Color.Transparent)
                            .clickable { selectedIndex = index }
                            .padding(8.dp)
                    ) {
                        PipText(
                            text = perk.name.uppercase(),
                            themeController = themeController,
                            textColorOverride = if (isSelected) Color.Black else null, // Makes selected text black!
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(themeController.activeColor.copy(alpha = 0.3f)))
        Spacer(modifier = Modifier.width(16.dp))

        // --- RIGHT COLUMN: PERK DETAILS ---
        Column(modifier = Modifier.weight(0.6f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp).border(2.dp, themeController.activeColor), contentAlignment = Alignment.Center) {
                PipText("[ VAULT BOY ANIMATION ]", themeController)
            }
            Spacer(modifier = Modifier.height(24.dp))
            PipText(selectedPerk.name.uppercase(), themeController, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val stars = "★".repeat(selectedPerk.level) + "☆".repeat(selectedPerk.maxLevel - selectedPerk.level)
            PipText("RANK: $stars", themeController, fontSize = 18.sp)

            Spacer(modifier = Modifier.height(16.dp))
            PipText(selectedPerk.description, themeController, fontSize = 14.sp)
        }
    }
}
