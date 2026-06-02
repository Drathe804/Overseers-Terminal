package com.dravenmiller.overseersterminal.components


actual fun readLatestSwoleBackup(): String? {
    // Apple's file system is locked down in a sandbox, so we just return null!
    return null
}

actual suspend fun fetchGitHubData(username: String): Map<String, Int> = emptyMap()
