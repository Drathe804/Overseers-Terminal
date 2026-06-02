package com.dravenmiller.overseersterminal.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dravenmiller.overseersterminal.components.PipIconRegistry
import com.dravenmiller.overseersterminal.components.PipLocation
import com.dravenmiller.overseersterminal.components.PipMap
import com.dravenmiller.overseersterminal.components.PipText
import com.dravenmiller.overseersterminal.components.getPipBoyDate
import com.dravenmiller.overseersterminal.components.getPipBoyTime
import com.dravenmiller.overseersterminal.theme.ThemeController
import kotlinx.coroutines.delay

enum class MapSubTab { SATELLITE, FAST_TRAVEL, ADD_MARKER } // Added ADD_MARKER state!

@Composable
fun MapTab(themeController: ThemeController) {
    val uriHandler = LocalUriHandler.current

    var currentSubTab by remember { mutableStateOf(MapSubTab.SATELLITE) }
    var locationLabel by remember { mutableStateOf("CALIBRATING SENSORS...") }
    var currentScanCoords by remember { mutableStateOf(Pair(0.0, 0.0)) }

    var timeText by remember { mutableStateOf(getPipBoyTime()) }
    var dateText by remember { mutableStateOf(getPipBoyDate()) }
    var activeTarget by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    // Tunes into the NavState radio broadcast!
    val navDirection by com.dravenmiller.overseersterminal.components.NavState.currentDirection.collectAsState()
    val navDistance by com.dravenmiller.overseersterminal.components.NavState.currentDistance.collectAsState()


    // Data Entry Memory Banks
    var customMarkerName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("quest") }

    // Pre-loaded known locations (Now with icon types!)
    val knownLocations = remember {
        mutableStateListOf(
            PipLocation("Vault 111 (Fenway)", Pair(42.3467, -71.0972), "quest"),
            PipLocation("Sedalia Outpost", Pair(38.7045, -93.2283), "camp")
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            timeText = getPipBoyTime()
            dateText = getPipBoyDate()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // --- THE UPGRADED MAP SUB-NAVIGATION BAR ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // [ LOCAL MAP ]
            val isLocal = currentSubTab == MapSubTab.SATELLITE
            PipText(
                text = "LOCAL MAP",
                themeController = themeController,
                textColorOverride = if (isLocal) Color.Black else null,
                modifier = Modifier
                    .clickable { currentSubTab = MapSubTab.SATELLITE }
                    .background(if (isLocal) themeController.activeColor else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // [ FAST TRAVEL]
            val isWorld = currentSubTab == MapSubTab.FAST_TRAVEL
            PipText(
                text = "FAST TRAVEL",
                themeController = themeController,
                textColorOverride = if (isWorld) Color.Black else null,
                modifier = Modifier
                    .clickable { currentSubTab = MapSubTab.FAST_TRAVEL }
                    .background(if (isWorld) themeController.activeColor else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }


        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentSubTab) {

                MapSubTab.SATELLITE -> {
                    PipMap(
                        modifier = Modifier.fillMaxSize(),
                        targetLocation = activeTarget,
                        markers = knownLocations // <-- Pushing the markers to the map!
                    ) { zoom, newLocation, coords ->
                        locationLabel = newLocation
                        currentScanCoords = coords
                    }

                    Box(modifier = Modifier.fillMaxSize().background(themeController.activeColor.copy(alpha = 0.4f)))
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PipText("+", themeController = themeController, fontSize = 24.sp)
                    }

                    // --- LAYER 3.5: THE TURN-BY-TURN HUD ---
                    if (navDirection != null) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .background(Color.Black.copy(alpha = 0.85f))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PipText(text = ">>> AUTOPILOT ENGAGED <<<", themeController = themeController, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            PipText(text = navDirection!!.uppercase(), themeController = themeController, fontSize = 20.sp)
                            if (navDistance != null) {
                                PipText(text = navDistance!!.uppercase(), themeController = themeController, fontSize = 16.sp)
                            }
                        }
                    }

                    Row(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).background(Color.Black.copy(alpha = 0.75f)).padding(8.dp)) {
                        PipText(text = dateText, themeController = themeController, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        PipText(text = timeText, themeController = themeController, fontSize = 14.sp)
                    }

                    Column(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), horizontalAlignment = Alignment.End) {
                        if (locationLabel != "CALIBRATING SENSORS..." && locationLabel != "SATELLITE INTERFERENCE") {
                            PipText(
                                text = "[ PLOT NEW WAYPOINT ]",
                                themeController = themeController,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .clickable {
                                        // 1. Open the Data Entry screen
                                        customMarkerName = locationLabel // Pre-fill with the geocoded name
                                        currentSubTab = MapSubTab.ADD_MARKER
                                    }
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        PipText(text = locationLabel, themeController = themeController, modifier = Modifier.background(Color.Black.copy(alpha = 0.75f)).padding(8.dp))
                    }
                }

                // THE NEW DATA ENTRY TERMINAL (Inside MapTab.kt)
                MapSubTab.ADD_MARKER -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PipText("ENTER WAYPOINT DESIGNATION:", themeController)
                        Spacer(Modifier.height(8.dp))

                        BasicTextField(
                            value = customMarkerName,
                            onValueChange = { customMarkerName = it },
                            textStyle = androidx.compose.ui.text.TextStyle(color = themeController.activeColor, fontSize = 24.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(themeController.activeColor),
                            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(16.dp)
                        )

                        Spacer(Modifier.height(24.dp))
                        PipText("SELECT SENSOR ICON:", themeController)
                        Spacer(Modifier.height(8.dp))

                        // --- THE 75-ICON SCROLLING GRID ---
                        val allIcons = PipIconRegistry.keys.toList() // Automatically grabs all 75 names!

                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3), // 3 columns wide
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allIcons.size) { index ->
                                val iconName = allIcons[index]
                                val isSelected = selectedIcon == iconName

                                Box(
                                    modifier = Modifier
                                        .background(if (isSelected) themeController.activeColor.copy(alpha = 0.3f) else Color.Transparent)
                                        .clickable { selectedIcon = iconName }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PipText(if (isSelected) "[ $iconName ]" else iconName, themeController)
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // The Save Button
                        PipText(
                            text = ">>> SAVE TO DATABANKS <<<",
                            themeController = themeController,
                            fontSize = 20.sp,
                            modifier = Modifier.clickable {
                                knownLocations.add(PipLocation(customMarkerName, currentScanCoords, selectedIcon))
                                currentSubTab = MapSubTab.SATELLITE
                            }.padding(16.dp)
                        )
                    }
                }


                // Fast Travel remains identical to before!
                MapSubTab.FAST_TRAVEL -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        items(knownLocations) { location ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PipText(text = "> ${location.name}", themeController = themeController, fontSize = 16.sp, modifier = Modifier.weight(1f).clickable {
                                    activeTarget = location.coordinates
                                    currentSubTab = MapSubTab.SATELLITE
                                })
                                PipText(text = "[ NAVIGATE ]", themeController = themeController, fontSize = 16.sp, modifier = Modifier.clickable {
                                    uriHandler.openUri("http://maps.google.com/maps?daddr=${location.coordinates.first},${location.coordinates.second}")
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
