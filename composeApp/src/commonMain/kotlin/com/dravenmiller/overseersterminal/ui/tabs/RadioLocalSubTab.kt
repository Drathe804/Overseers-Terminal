package com.dravenmiller.overseersterminal.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dravenmiller.overseersterminal.components.PipBluetoothDevice
import com.dravenmiller.overseersterminal.components.PipMediaController
import com.dravenmiller.overseersterminal.components.PipText
import com.dravenmiller.overseersterminal.theme.ThemeController

@Composable
fun RadioLocalSubTab(
    themeController: ThemeController, btDevices: List<PipBluetoothDevice>, connectedDevice: PipBluetoothDevice?,
    mediaController: PipMediaController, trackTitle: String?, trackArtist: String?, currentPos: Long, maxDuration: Long,
    onDeviceSelect: (PipBluetoothDevice) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // LEFT COLUMN
        Column(modifier = Modifier.weight(0.4f).fillMaxHeight().padding(end = 16.dp)) {
            LazyColumn {
                if (btDevices.isEmpty()) item { PipText("NO AUDIO HARDWARE DETECTED", themeController) }
                items(btDevices) { device ->
                    val isSelected = connectedDevice == device
                    PipText(
                        text = device.name.uppercase(), themeController = themeController, fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().alpha(if (device.isConnected) 1.0f else 0.3f)
                            .background(if (isSelected) themeController.activeColor.copy(alpha = 0.3f) else Color.Transparent)
                            .then(if (device.isConnected) Modifier.clickable { onDeviceSelect(device) } else Modifier).padding(8.dp)
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(themeController.activeColor.copy(alpha = 0.3f)))
        Spacer(modifier = Modifier.width(16.dp))

        // RIGHT COLUMN
        Column(modifier = Modifier.weight(0.6f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            RadioVisualizer(themeController, false, trackTitle, trackArtist, currentPos, maxDuration)
            Spacer(modifier = Modifier.height(32.dp))
            if (connectedDevice != null) {
                PipText("STATUS: SECURE LINK ESTABLISHED", themeController, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                PipText("DEVICE: ${connectedDevice.name.uppercase()}", themeController, fontSize = 20.sp)
                Spacer(Modifier.height(24.dp))
                if (trackTitle != "NO ACTIVE SIGNAL" && trackTitle != "AWAITING SIGNAL...") {
                    HardwareMediaControls(themeController, mediaController, null)
                } else {
                    PipText("NO AUDIO DETECTED ON HARDWARE", themeController, fontSize = 14.sp)
                }
            } else {
                PipText("STATUS: NO ACTIVE LINK", themeController)
                Spacer(Modifier.height(16.dp))
                PipText("SELECT CONNECTED HARDWARE", themeController, fontSize = 16.sp)
            }
        }
    }
}
