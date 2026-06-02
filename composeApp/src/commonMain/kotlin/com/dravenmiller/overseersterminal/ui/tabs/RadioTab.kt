package com.dravenmiller.overseersterminal.ui.tabs

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dravenmiller.overseersterminal.components.*
import com.dravenmiller.overseersterminal.theme.ThemeController
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch

enum class RadioSubTab { LOCAL, MEDIA, UHF_RADIO }

@Composable
fun RadioTab(themeController: ThemeController) {
    val tabs = RadioSubTab.values()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // --- SHARED MEMORY BANKS ---
    var frequency by remember { mutableStateOf(462.550f) }
    val mediaController = rememberMediaController()
    val mediaApps = remember { mediaController.getInstalledMediaApps() }

    val rawBtDevices by observeBluetoothDevices(mediaController)
    val btDevices = rawBtDevices.sortedByDescending { it.isConnected }
    var connectedDevice by remember { mutableStateOf(btDevices.firstOrNull { it.isConnected }) }
    var activeApp by remember { mutableStateOf<PipMediaApp?>(null) }

    val trackTitle by MediaState.trackTitle.collectAsState()
    val trackArtist by MediaState.trackArtist.collectAsState()
    val currentPos by MediaState.currentPosition.collectAsState()
    val maxDuration by MediaState.trackDuration.collectAsState()
    val radioPresets = listOf(Pair("Galaxy News Radio", 462.550f), Pair("Diamond City Radio", 462.600f), Pair("Sedalia Local", 462.650f))

    Column(modifier = Modifier.fillMaxSize()) {

        // --- THE CAROUSEL ---
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
                        text = { PipText(if (subTab == RadioSubTab.MEDIA) "DIGITAL MEDIA" else subTab.name, themeController, fontSize = 18.sp, modifier = Modifier.alpha(textAlpha)) }
                    )
                }
            }
        }

        // --- THE ROUTER ---
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            when (tabs[page]) {
                RadioSubTab.LOCAL -> RadioLocalSubTab(themeController, btDevices, connectedDevice, mediaController, trackTitle, trackArtist, currentPos, maxDuration) { connectedDevice = it }
                RadioSubTab.MEDIA -> RadioMediaSubTab(themeController, mediaApps, activeApp, mediaController, trackTitle, trackArtist, currentPos, maxDuration) { activeApp = it }
                RadioSubTab.UHF_RADIO -> RadioUhfSubTab(themeController, frequency, radioPresets, trackTitle, trackArtist, currentPos, maxDuration) { frequency = it }
            }
        }
    }
}

// *** LEAVE YOUR HardwareMediaControls AND RadioVisualizer FUNCTIONS DOWN HERE! ***

// --- THE REUSABLE BUTTON BLUEPRINT ---
@Composable
fun HardwareMediaControls(
    themeController: ThemeController,
    mediaController: PipMediaController,
    activeApp: PipMediaApp?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Filled.FastRewind, contentDescription = "Rewind", tint = themeController.activeColor, modifier = Modifier.size(20.dp).clickable { mediaController.sendCommand(MediaCommand.REWIND_30, activeApp) })
        Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Prev", tint = themeController.activeColor, modifier = Modifier.size(24.dp).clickable { mediaController.sendCommand(MediaCommand.PREVIOUS, activeApp) })
        Box(modifier = Modifier.border(2.dp, themeController.activeColor).clickable { mediaController.sendCommand(MediaCommand.PLAY_PAUSE, activeApp) }.padding(8.dp), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Play/Pause", tint = themeController.activeColor, modifier = Modifier.size(28.dp))
        }
        Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next", tint = themeController.activeColor, modifier = Modifier.size(24.dp).clickable { mediaController.sendCommand(MediaCommand.NEXT, activeApp) })
        Icon(imageVector = Icons.Filled.FastForward, contentDescription = "Forward", tint = themeController.activeColor, modifier = Modifier.size(20.dp).clickable { mediaController.sendCommand(MediaCommand.FAST_FORWARD_30, activeApp) })
    }
}

// ==========================================
// THE WASTELAND OSCILLOSCOPE DRAWING ENGINE
// ==========================================
@Composable
fun RadioVisualizer(
    themeController: ThemeController,
    isTransceiver: Boolean,
    trackTitle: String? = null,
    trackArtist: String? = null,
    currentPos: Long = 0L,
    maxDuration: Long = 0L
) {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )

    Box(
        modifier = Modifier.fillMaxWidth().height(100.dp).border(2.dp, themeController.activeColor).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isTransceiver) {
            // THE SINE WAVES
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val centerY = height / 2

                drawLine(color = themeController.activeColor.copy(alpha = 0.3f), start = Offset(0f, centerY), end = Offset(width, centerY), strokeWidth = 2f)

                val path = Path()
                path.moveTo(0f, centerY)
                for (x in 0..width.toInt() step 2) {
                    val xFloat = x.toFloat()
                    val y = centerY + (sin((xFloat * 0.05) + phase) * (height / 2.5)).toFloat()
                    path.lineTo(xFloat, y)
                }
                drawPath(path = path, color = themeController.activeColor, style = Stroke(width = 4f))
            }
        } else {
            // THE LIVE MEDIA DASHBOARD
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PipText(text = trackTitle?.uppercase() ?: "NO SIGNAL", themeController = themeController, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
                PipText(text = trackArtist?.uppercase() ?: "", themeController = themeController, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(16.dp))

                // The Progress Bar Math
                val progressRatio = if (maxDuration > 0) (currentPos.toFloat() / maxDuration.toFloat()) else 0f

                // Formats the math into Minutes/Seconds (e.g. 1:45)
                val currentSec = (currentPos / 1000) % 60
                val currentMin = (currentPos / 1000) / 60
                val maxSec = (maxDuration / 1000) % 60
                val maxMin = (maxDuration / 1000) / 60

                val timeString = "${currentMin}:${currentSec.toString().padStart(2, '0')} / ${maxMin}:${maxSec.toString().padStart(2, '0')}"

                Box(modifier = Modifier.fillMaxWidth(0.8f).height(10.dp).border(1.dp, themeController.activeColor), contentAlignment = Alignment.CenterStart) {
                    Box(modifier = Modifier.fillMaxWidth(progressRatio).fillMaxHeight().background(themeController.activeColor))
                }
                Spacer(modifier = Modifier.height(8.dp))
                PipText(text = timeString, themeController = themeController, fontSize = 12.sp)
            }
        }
    }
}