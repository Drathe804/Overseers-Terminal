package com.dravenmiller.overseersterminal.components

import org.jetbrains.compose.resources.DrawableResource
import overseersterminal.composeapp.generated.resources.*

// The RobCo Master Icon Dictionary
val PipIconRegistry: Map<String, DrawableResource> = mapOf(
    "Quest" to Res.drawable.map_icon_1,
    "Camp" to Res.drawable.map_icon_2,
    "Combat" to Res.drawable.map_icon_3,
    "Settlement" to Res.drawable.map_icon_4,
    "Danger" to Res.drawable.map_icon_5,
    // ... Paste the other 70 here!
)

// A fallback icon just in case a string doesn't match
val DefaultPipIcon = Res.drawable.map_icon_6
