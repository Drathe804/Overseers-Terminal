package com.dravenmiller.overseersterminal.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dravenmiller.overseersterminal.components.PipText
import com.dravenmiller.overseersterminal.theme.ThemeController
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.ui.unit.sp

enum class DataSubTab { QUESTS, WORKSHOPS, STATS }

@Composable
fun DataTab(themeController: ThemeController) {
    val tabs = DataSubTab.values()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {

        // --- THE TRUE ROBCO CAROUSEL ---
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {

            // Dynamically measures your specific screen to force tabs perfectly into the center!
            // We subtract roughly half the width of the text so the word perfectly aligns.
            val centerPadding = maxWidth / 2 - 60.dp

            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = themeController.activeColor,
                edgePadding = centerPadding.coerceAtLeast(0.dp),
                indicator = {},
                divider = {},
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                tabs.forEachIndexed { index, subTab ->

                    // Math: How many slots away is this tab from the center?
                    val distance = kotlin.math.abs(pagerState.currentPage - index)

                    // The Optics: Center is 100%, Adjacent is 50%, Everything else is invisible!
                    val textAlpha = when (distance) {
                        0 -> 1.0f
                        1 -> 0.5f
                        else -> 0.0f
                    }

                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = {
                            PipText(
                                text = subTab.name.replace("_", " "),
                                themeController = themeController,
                                fontSize = 18.sp,
                                modifier = Modifier.alpha(textAlpha)
                            )
                        }
                    )
                }
            }
        }

        // --- THE GESTURAL SWIPE CONTENT ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            // Reads which page the user swiped to and loads the correct UI!
            when (tabs[page]) {
                DataSubTab.QUESTS -> QuestsSubTab(themeController)
                DataSubTab.WORKSHOPS -> PipText("WORKSHOP SETTLEMENTS OFFLINE", themeController, modifier = Modifier.padding(16.dp))
                DataSubTab.STATS -> DataStatsSubTab(themeController)
            }
        }
    }
}
