package com.dravenmiller.overseersterminal.components

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.State
import android.content.ComponentName // <-- Make sure to add this import!
import android.content.IntentFilter
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf

actual class PipMediaController(private val context: Context) {

    actual fun sendCommand(command: MediaCommand, targetApp: PipMediaApp?) {
        val keyCode = when (command) {
            MediaCommand.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            MediaCommand.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaCommand.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            MediaCommand.REWIND_30 -> KeyEvent.KEYCODE_MEDIA_REWIND
            MediaCommand.FAST_FORWARD_30 -> KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
        }

        if (targetApp != null) {
            // THE TARGETED STRIKE (Explicit Intent)
            // By specifying the exact component, Android allows us to wake dormant apps!
            val targetComponent = ComponentName(targetApp.packageName, targetApp.receiverName)

            val intentDown = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                component = targetComponent // <-- Bypasses Android security!
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            }
            val intentUp = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                component = targetComponent
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }

            context.sendOrderedBroadcast(intentDown, null)
            context.sendOrderedBroadcast(intentUp, null)

        } else {
            // THE GLOBAL STRIKE
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
    }

    // SCANS FOR MUSIC APPS
    actual fun getInstalledMediaApps(): List<PipMediaApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        val receivers = pm.queryBroadcastReceivers(intent, 0)

        return receivers.mapNotNull { resolveInfo ->
            val name = pm.getApplicationLabel(resolveInfo.activityInfo.applicationInfo).toString()
            val pkg = resolveInfo.activityInfo.packageName
            val receiver = resolveInfo.activityInfo.name // <-- We capture the secret antenna name!
            PipMediaApp(name, pkg, receiver)
        }.distinctBy { it.packageName }
    }
    // SCANS FOR BLUETOOTH AUDIO DEVICES ONLY
    @SuppressLint("MissingPermission")
    actual fun getPairedBluetoothDevices(): List<PipBluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return adapter.bondedDevices.filter { device ->
            device.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO
        }.map { device ->

            // THE REFLECTION HACK: Force Android to tell us if it's currently connected!
            val isConnected = try {
                device.javaClass.getMethod("isConnected").invoke(device) as Boolean
            } catch (e: Exception) { false }

            PipBluetoothDevice(device.name ?: "UNKNOWN AUDIO DEVICE", device.address, isConnected)
        }
    }

}

@Composable
actual fun rememberMediaController(): PipMediaController {
    val context = LocalContext.current
    return remember { PipMediaController(context) }
}

// ... (Keep your PipMediaController, rememberMediaController, and formatRadioFreq code) ...

// THE LIVE BLUETOOTH RADAR
@Composable
actual fun observeBluetoothDevices(controller: PipMediaController): State<List<PipBluetoothDevice>> {
    val context = LocalContext.current
    // Start by grabbing whatever is currently connected right now
    val devicesState = remember { mutableStateOf(controller.getPairedBluetoothDevices()) }

    DisposableEffect(context) {
        // 1. Build the Antenna
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Whenever the OS shouts, re-scan the hardware and update the UI!
                devicesState.value = controller.getPairedBluetoothDevices()
            }
        }

        // 2. Tell the Antenna what frequencies to listen for
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)    // A device paired up
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED) // A device powered off/walked away
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)   // Bluetooth was toggled on/off
        }

        // 3. Plug it in!
        context.registerReceiver(receiver, filter)

        // 4. Safely unplug it when the user closes the map
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    return devicesState
}


actual fun formatRadioFreq(frequency: Float): String = "%.3f".format(frequency)
