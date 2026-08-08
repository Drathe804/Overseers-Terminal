package com.dravenmiller.overseersterminal

class IosGoogleAuthBridge : GoogleAuthBridge {
    override fun startSignIn(onResult: (Boolean) -> Unit) {
        println("IOS SIGN-IN NOT YET IMPLEMENTED")
        onResult(false)
    }

    override fun getSignedInAccountEmail(): String? {
        return null
    }

    // ADD THIS TO SATISFY THE COMPILER
    override fun fetchUpcomingEvents(onResult: (List<CalendarEvent>?) -> Unit) {
        println("IOS CALENDAR FETCH NOT YET IMPLEMENTED")
        onResult(null)
    }
}

// 1. Update this to match the 'expect' signature exactly
actual fun createGoogleAuthBridge(context: Any): GoogleAuthBridge {
    // 2. We don't need the context on iOS yet, so we just ignore it
    return IosGoogleAuthBridge()
}
