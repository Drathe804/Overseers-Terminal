package com.dravenmiller.overseersterminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dravenmiller.overseersterminal.components.PipText
import com.dravenmiller.overseersterminal.health.BiometricBridge
import com.dravenmiller.overseersterminal.theme.ThemeController
import com.dravenmiller.overseersterminal.ui.tabs.DataTab
import com.dravenmiller.overseersterminal.ui.tabs.MapTab
import com.dravenmiller.overseersterminal.ui.tabs.RadioTab
import com.dravenmiller.overseersterminal.ui.tabs.StatSubTab
import com.dravenmiller.overseersterminal.ui.tabs.StatTab

enum class PipTab {
    STAT, INV, DATA, MAP, RADIO
}

@Composable
fun MainScreen(themeController: ThemeController, biometricBridge: BiometricBridge?) {
    var currentTab by rememberSaveable { mutableStateOf(PipTab.STAT) }// Now survives screen rotations!
    var currentSubTab by rememberSaveable { mutableStateOf(StatSubTab.STATUS) }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
    ) {
        // --- 1. THE BRACKETED HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // The continuous horizontal baseline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(themeController.activeColor)
            )

            // The Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                PipTab.values().forEach { tab ->
                    val isSelected = currentTab == tab

                    PipText(
                        text = tab.name,
                        themeController = themeController,
                        modifier = Modifier
                            .clickable { currentTab = tab }
                            // 1. The Eraser: Black background blocks the floor line
                            .background(if (isSelected) Color.Black else Color.Transparent)

                            // 2. The Hardware Brackets: Calibrated for half-height hooks
                            .drawBehind {
                                if (isSelected) {
                                    val stroke = 2.dp.toPx()
                                    val c = themeController.activeColor

                                    // THE CALIBRATION: Stop exactly halfway up the text box
                                    val midY = size.height * 0.5f

                                    // How far the hook pushes inward behind the letters
                                    val cornerLength = 10.dp.toPx()

                                    // Left Wall (Bottom up to the halfway mark)
                                    drawLine(c, Offset(0f, size.height), Offset(0f, midY), stroke)
                                    // Left Hook (Turns inward)
                                    drawLine(c, Offset(0f, midY), Offset(cornerLength, midY), stroke)

                                    // Right Wall (Bottom up to the halfway mark)
                                    drawLine(c, Offset(size.width, size.height), Offset(size.width, midY), stroke)
                                    // Right Hook (Turns inward)
                                    drawLine(c, Offset(size.width, midY), Offset(size.width - cornerLength, midY), stroke)
                                }
                            }

                            // 3. Internal Padding: Keeps the text from touching your newly drawn walls!
                            // (We removed the specific bottom padding so the box touches the floor perfectly)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // The Main Display Area
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (currentTab) {
                PipTab.STAT -> StatTab(themeController, biometricBridge)
                PipTab.INV -> PipText("INVENTORY OFFLINE", themeController)
                PipTab.DATA -> DataTab(themeController)

                // --- THE NEW SATELLITE UPLINK ---
                PipTab.MAP -> MapTab(themeController)

                PipTab.RADIO -> RadioTab(themeController)
            }
        }

    }
}
