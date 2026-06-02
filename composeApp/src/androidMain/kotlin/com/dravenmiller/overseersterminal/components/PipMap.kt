package com.dravenmiller.overseersterminal.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.maps.android.compose.Polyline
import androidx.compose.ui.graphics.Color



@Composable
actual fun PipMap(
    modifier: Modifier,
    targetLocation: Pair<Double, Double>?,
    markers: List<PipLocation>,
    onMapUpdate: (Float, String, Pair<Double, Double>) -> Unit
) {

    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }
    val cameraState = rememberCameraPositionState()

    var routeLine by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- THE UPGRADED FAST TRAVEL & AUTO-CENTER ENGINE ---
    @SuppressLint("MissingPermission")
    LaunchedEffect(hasLocationPermission, targetLocation) {
        if (targetLocation != null) {
            // 1. Fly the camera to the destination
            val destination = LatLng(targetLocation.first, targetLocation.second)
            cameraState.animate(CameraUpdateFactory.newLatLngZoom(destination, 16f))

            // 2. SATELLITE PING: Find where we are, and ask Google for the roads!
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val origin = "${loc.latitude},${loc.longitude}"
                            val dest = "${targetLocation.first},${targetLocation.second}"

                            // ⚠️ IMPORTANT: Paste your actual Google API Key here!
                            val apiKey = "YOUR_API_KEY_HERE"

                            val urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=$dest&key=$apiKey"
                            val connection = URL(urlString).openConnection() as HttpURLConnection
                            val response = connection.inputStream.bufferedReader().use { it.readText() }

                            // Decrypt the JSON
                            val jsonObject = JSONObject(response)
                            val routes = jsonObject.getJSONArray("routes")
                            if (routes.length() > 0) {
                                val overviewPolyline = routes.getJSONObject(0).getJSONObject("overview_polyline").getString("points")
                                val decodedPath = decodePolyline(overviewPolyline)

                                // Send the path to the UI!
                                withContext(Dispatchers.Main) { routeLine = decodedPath }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } else if (hasLocationPermission) {
            // 3. AUTO-CENTER: The target is null, so find the player's physical location!
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    coroutineScope.launch {
                        val playerPos = LatLng(location.latitude, location.longitude)
                        cameraState.animate(CameraUpdateFactory.newLatLngZoom(playerPos, 16f))
                    }
                }
            }
        }
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            hasLocationPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // --- THE GEOCODER SENSOR ---
    // This watches the map. Whenever the user stops dragging, it runs!
    LaunchedEffect(cameraState.isMoving) {
        val zoom = cameraState.position.zoom

        if (!cameraState.isMoving) {
            val target = cameraState.position.target

            // We jump to a background thread so the map doesn't freeze while calculating
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(target.latitude, target.longitude, 1)

                    val newLabel = if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        when {
                            zoom < 4f -> "PLANET EARTH"
                            zoom < 6f -> addr.countryName?.uppercase()
                                ?: "UNKNOWN REGION" // Country
                            zoom < 9f -> addr.adminArea?.uppercase()
                                ?: "WASTELAND TERRITORY" // State
                            else -> addr.locality?.uppercase() ?: addr.subAdminArea?.uppercase()
                            ?: "UNMAPPED SECTOR" // City/County
                        }
                    } else {
                        "UNMAPPED SECTOR"
                    }

                    // Inside the try block:
                    withContext(Dispatchers.Main) {
                        onMapUpdate(zoom, newLabel, Pair(target.latitude, target.longitude))
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onMapUpdate(
                            zoom,
                            "SATELLITE INTERFERENCE",
                            Pair(target.latitude, target.longitude)
                        )
                    }
                }
            }
        } else {
            // While dragging:
            onMapUpdate(zoom, "CALIBRATING SENSORS...", Pair(0.0, 0.0))
        }

    }

    val retroMapStyle = """
        [
          { "elementType": "labels", "stylers": [ { "visibility": "off" } ] },
          { "featureType": "water", "stylers": [ { "color": "#020502" } ] },
          { "featureType": "landscape.natural", "stylers": [ { "color": "#404040" } ] },
          { "featureType": "landscape.man_made", "stylers": [ { "color": "#202020" } ] },
          { "featureType": "road", "elementType": "geometry", "stylers": [ { "color": "#FFFFFF" }, { "weight": 1.5 } ] },
          { "featureType": "poi", "stylers": [ { "visibility": "off" } ] },
          { "featureType": "transit", "stylers": [ { "visibility": "off" } ] }
        ]
    """.trimIndent()

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraState,
        properties = MapProperties(
            mapType = MapType.NORMAL,
            mapStyleOptions = MapStyleOptions(retroMapStyle),
            isMyLocationEnabled = hasLocationPermission,
            isBuildingEnabled = true
        ),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
    ) {
        // --- NEW: THE LASER ROUTER ---
        if (routeLine.isNotEmpty()) {
            Polyline(
                points = routeLine,
                color = Color(0xFF14FF00), // Glowing Pip-Boy Green!
                width = 14f,
                zIndex = 1f
            )
        }
        markers.forEach { location ->
            MarkerComposable(
                state = MarkerState(
                    position = LatLng(
                        location.coordinates.first,
                        location.coordinates.second
                    )
                ),
                title = location.name
            ) {
                // 1. Ask the Registry for the icon! (If it can't find the name, use the Default)
                val iconRes = PipIconRegistry[location.iconType] ?: DefaultPipIcon

                // 2. Draw it!
                androidx.compose.foundation.Image(
                    painter = org.jetbrains.compose.resources.painterResource(iconRes),
                    contentDescription = location.name,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// --- ROBCO ROUTE DECRYPTION ALGORITHM ---
fun decodePolyline(encoded: String): List<com.google.android.gms.maps.model.LatLng> {
    val poly = ArrayList<com.google.android.gms.maps.model.LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        poly.add(com.google.android.gms.maps.model.LatLng(lat / 100000.0, lng / 100000.0))
    }
    return poly
}
