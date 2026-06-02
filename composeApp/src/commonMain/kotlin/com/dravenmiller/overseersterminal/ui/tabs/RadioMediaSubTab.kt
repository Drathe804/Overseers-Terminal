package com.dravenmiller.overseersterminal.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dravenmiller.overseersterminal.components.PipMediaApp
import com.dravenmiller.overseersterminal.components.PipMediaController
import com.dravenmiller.overseersterminal.components.PipText
import com.dravenmiller.overseersterminal.theme.ThemeController

@Composable
fun RadioMediaSubTab(
    themeController: ThemeController, mediaApps: List<PipMediaApp>, activeApp: PipMediaApp?,
    mediaController: PipMediaController, trackTitle: String?, trackArtist: String?, currentPos: Long, maxDuration: Long,
    onAppSelect: (PipMediaApp?) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(0.4f).fillMaxHeight().padding(end = 16.dp)) {
            LazyColumn {
                item {
                    PipText(text = "GLOBAL SYSTEM", themeController = themeController, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().background(if (activeApp == null) themeController.activeColor.copy(alpha = 0.3f) else Color.Transparent).clickable { onAppSelect(null) }.padding(8.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(themeController.activeColor.copy(alpha = 0.3f)))
                    Spacer(modifier = Modifier.height(4.dp))
                }
                items(mediaApps) { app ->
                    val isSelected = activeApp == app
                    PipText(text = app.name.uppercase(), themeController = themeController, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().background(if (isSelected) themeController.activeColor.copy(alpha = 0.3f) else Color.Transparent).clickable { onAppSelect(if (activeApp == app) null else app) }.padding(8.dp))
                }
            }
        }
        Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(themeController.activeColor.copy(alpha = 0.3f)))
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(0.6f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            RadioVisualizer(themeController, false, trackTitle, trackArtist, currentPos, maxDuration)
            Spacer(modifier = Modifier.height(32.dp))
            PipText("TARGET: ${activeApp?.name?.uppercase() ?: "GLOBAL SYSTEM"}", themeController, fontSize = 16.sp)
            Spacer(Modifier.height(24.dp))
            HardwareMediaControls(themeController, mediaController, activeApp)
        }
    }
}
