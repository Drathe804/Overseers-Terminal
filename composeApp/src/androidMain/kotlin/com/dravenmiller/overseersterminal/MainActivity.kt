package com.dravenmiller.overseersterminal

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import com.dravenmiller.overseersterminal.health.AndroidBiometricBridge

class MainActivity : ComponentActivity() {

    // THE NEW HOOK
    companion object {
        lateinit var appContext: android.content.Context
    }
    // 1. The specific security clearances for Biometrics
    val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    // 2. The Official Google Health Prompt
    val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(permissions)) {
            Toast.makeText(this, "ACCESS GRANTED!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "ACCESS DENIED BY OS!", Toast.LENGTH_LONG).show()
        }
    }

    // 3. THE HEALTH BYPASS: A public function to fire the prompt manually!
    fun promptHealthPermissions() {
        Toast.makeText(this, "FIRING LAUNCHER...", Toast.LENGTH_SHORT).show()
        try {
            permissionLauncher.launch(permissions)
        } catch (e: Exception) {
            Toast.makeText(this, "CRASH: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- THE OVERSEER OVERRIDE (STORAGE CLEARANCE) ---
        // Checks if we are allowed to read the Downloads folder!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // If not, force the Android Settings screen to open!
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + packageName)
                startActivity(intent)
            }
        }

        // Boot up the Bridge and pass 'this' (MainActivity) as the context
        val hardwareBridge = AndroidBiometricBridge(this)

        // GRABS THE CONTEXT ON BOOT
        appContext = this.applicationContext

        setContent {
            App(biometricBridge = hardwareBridge)
        }
    }
}
