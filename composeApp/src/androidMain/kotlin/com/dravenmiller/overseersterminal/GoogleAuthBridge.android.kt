package com.dravenmiller.overseersterminal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dravenmiller.overseersterminal.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import kotlinx.coroutines.*

class AndroidGoogleAuthBridge(private val context: Context) : GoogleAuthBridge {

    // Configure sign-in to request the user's email and calendar read-only scope
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        // Now it's dynamic!
        .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
        .requestScopes(Scope(CalendarScopes.CALENDAR_READONLY))
        .build()

    private val client = GoogleSignIn.getClient(context, gso)

    // Inside AndroidGoogleAuthBridge.kt
    override fun startSignIn(onResult: (Boolean) -> Unit) {
        // 1. Check if we already have an account
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            Log.d("OVERSEER_TERMINAL", "User already signed in, skipping UI.")
            onResult(true) // Immediately tell the UI we are logged in
            return
        }

        // 2. If no account, THEN launch the intent
        val activity = context as? Activity
        activity?.startActivityForResult(client.signInIntent, 9001)
    }

    override fun getSignedInAccountEmail(): String? {
        return GoogleSignIn.getLastSignedInAccount(context)?.email
    }

    override fun fetchUpcomingEvents(onResult: (List<CalendarEvent>?) -> Unit) {
        // 1. Verify we are actually logged in
        val account = GoogleSignIn.getLastSignedInAccount(context)
        /*if (account == null) {
            onResult(null)
            return
        }*/

        // --- ADD THIS DEBUG LOG ---
        Log.d("OVERSEER_TERMINAL", "Fetch requested. Account: ${account?.email}")

        if (account == null) {
            Log.e("OVERSEER_TERMINAL", "Fetch aborted: No account logged in!")
            onResult(null)
            return
        }
        // 2. Launch a background thread so we don't freeze the Pip-Boy
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Construct the secure Google Credential
                val credential = GoogleAccountCredential.usingOAuth2(
                    context, listOf(CalendarScopes.CALENDAR_READONLY)
                ).apply {
                    selectedAccount = account.account
                }

                // Build the Calendar Engine
                val service = Calendar.Builder(
                    NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
                ).setApplicationName("Overseers Terminal").build()

                // Fetch the next 10 events from right now
                val now = DateTime(System.currentTimeMillis())
                // --- ROBCO DIAGNOSTIC: PRINT ALL CALENDARS ---
                val calendarList = service.calendarList().list().execute()
                println("--- DETECTED CALENDARS ---")
                calendarList.items?.forEach { cal ->
                    println("NAME: ${cal.summary} | ID: ${cal.id}")
                }
                println("--------------------------")
                val events = service.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute()

                // Map Google's raw data into our clean KMP Data Class
                val items = events.items
                val parsedEvents = items?.mapNotNull { event ->
                    val title = event.summary ?: "Unknown Anomaly"
                    // Get the start time (handles both specific times and all-day events)
                    val start = event.start.dateTime ?: event.start.date
                    CalendarEvent(title = title, startTimeMs = start.value, id = event.id)
                } ?: emptyList()

                // Switch back to the Main UI thread to return the data
                withContext(Dispatchers.Main) {
                    onResult(parsedEvents)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(null)
                }
            }
        }
    }
}

actual fun createGoogleAuthBridge(context: Any): GoogleAuthBridge {
    // Cast the 'Any' back to 'Context' safely
    return AndroidGoogleAuthBridge(context as Context)
}
