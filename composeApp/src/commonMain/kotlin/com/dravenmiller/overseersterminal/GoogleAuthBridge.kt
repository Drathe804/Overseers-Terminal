package com.dravenmiller.overseersterminal

interface GoogleAuthBridge {
    fun startSignIn(onResult: (Boolean) -> Unit)
    fun getSignedInAccountEmail(): String?

    // ADD THIS NEW TRANSMISSION PROTOCOL
    fun fetchUpcomingEvents(onResult: (List<CalendarEvent>?) -> Unit)
}

expect fun createGoogleAuthBridge(context: Any): GoogleAuthBridge
