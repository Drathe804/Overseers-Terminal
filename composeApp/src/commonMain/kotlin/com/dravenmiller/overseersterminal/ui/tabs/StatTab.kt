package com.dravenmiller.overseersterminal.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dravenmiller.overseersterminal.components.PipText
import com.dravenmiller.overseersterminal.components.StatEngine
import com.dravenmiller.overseersterminal.theme.ThemeController
import com.dravenmiller.overseersterminal.health.BiometricBridge
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch


enum class StatSubTab { STATUS, SPECIAL, PERKS }

@Composable
fun StatTab(themeController: ThemeController, biometricBridge: BiometricBridge?) {
    // --- THE CAROUSEL ENGINE ---
    val tabs = StatSubTab.values()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // 1. The Biometric Memory Banks
    var hpScore by remember { mutableStateOf(100) }
    var apScore by remember { mutableStateOf(100) }
    var terminalLog by remember { mutableStateOf("SYSTEM READY") }

    // 2. The Hardware Scanner
    LaunchedEffect(Unit) {
        StatEngine.loadLiveBiometrics()
        StatEngine.syncGitHub("DravenMiller")
        if (biometricBridge != null) {
            hpScore = biometricBridge.getSleepHpScore()
            apScore = biometricBridge.getHeartRateApScore()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // --- THE SUB-NAVIGATION CAROUSEL ---
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val centerPadding = maxWidth / 2 - 60.dp
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage, containerColor = Color.Transparent, contentColor = themeController.activeColor,
                edgePadding = centerPadding.coerceAtLeast(0.dp), indicator = {}, divider = {}, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                tabs.forEachIndexed { index, subTab ->
                    val distance = kotlin.math.abs(pagerState.currentPage - index)
                    val textAlpha = when (distance) { 0 -> 1.0f; 1 -> 0.5f; else -> 0.0f }

                    Tab(selected = pagerState.currentPage == index, onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { PipText(subTab.name, themeController, fontSize = 18.sp, modifier = Modifier.alpha(textAlpha)) }
                    )
                }
            }
        }

        // --- THE SWIPEABLE CONTENT ---
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            when (tabs[page]) {
                StatSubTab.STATUS -> StatusSubTab(themeController, biometricBridge)
                StatSubTab.SPECIAL -> SpecialSubTab(themeController)
                StatSubTab.PERKS -> PerksSubTab(themeController)
            }
        }
    }
}
