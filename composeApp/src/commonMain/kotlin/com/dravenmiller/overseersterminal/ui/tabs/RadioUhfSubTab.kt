package com.dravenmiller.overseersterminal.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dravenmiller.overseersterminal.components.PipText
import com.dravenmiller.overseersterminal.components.formatRadioFreq
import com.dravenmiller.overseersterminal.theme.ThemeController

@Composable
fun RadioUhfSubTab(
    themeController: ThemeController, frequency: Float, radioPresets: List<Pair<String, Float>>,
    trackTitle: String?, trackArtist: String?, currentPos: Long, maxDuration: Long,
    onFreqChange: (Float) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(0.4f).fillMaxHeight().padding(end = 16.dp)) {
            LazyColumn {
                item { PipText("--- KNOWN FREQUENCIES ---", themeController, fontSize = 12.sp); Spacer(Modifier.height(8.dp)) }
                items(radioPresets) { preset ->
                    val isSelected = frequency == preset.second
                    PipText(text = preset.first.uppercase(), themeController = themeController, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().background(if (isSelected) themeController.activeColor.copy(alpha = 0.3f) else Color.Transparent).clickable { onFreqChange(preset.second) }.padding(8.dp))
                }
            }
        }
        Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(themeController.activeColor.copy(alpha = 0.3f)))
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(0.6f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            RadioVisualizer(themeController, true, trackTitle, trackArtist, currentPos, maxDuration)
            Spacer(modifier = Modifier.height(32.dp))
            PipText("${formatRadioFreq(frequency)} MHz", themeController, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Slider(value = frequency, onValueChange = onFreqChange, valueRange = 400.000f..480.000f, colors = SliderDefaults.colors(thumbColor = themeController.activeColor, activeTrackColor = themeController.activeColor, inactiveTrackColor = themeController.activeColor.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth())
        }
    }
}
