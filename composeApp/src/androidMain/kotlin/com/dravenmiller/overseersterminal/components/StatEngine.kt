package com.dravenmiller.overseersterminal.components

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

actual fun readLatestSwoleBackup(): String? {
    try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir.exists() && downloadsDir.isDirectory) {
            // Find all files matching the Swole Scroll backup naming convention
            val backupFiles = downloadsDir.listFiles { file ->
                file.name.startsWith("swole_backup_") && file.name.endsWith(".json")
            }

            if (backupFiles != null && backupFiles.isNotEmpty()) {
                // Sort them by the exact date/time they were modified, and grab the newest!
                val latestFile = backupFiles.maxByOrNull { it.lastModified() }
                return latestFile?.readText()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

actual suspend fun fetchGitHubData(username: String): Map<String, Int> = withContext(Dispatchers.IO) {
    val languageMap = mutableMapOf<String, Int>()

    try {
        val url = URL("https://api.github.com/users/$username/repos")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "RobCo-PipBoy-Terminal")

        if (connection.responseCode == 200) {
            val jsonResponse = connection.inputStream.bufferedReader().readText()

            // 1. Slice the massive JSON into individual repositories
            val repos = jsonResponse.split("\"full_name\":")

            for (repo in repos) {
                // 2. Find the primary language and the size in KB
                val langMatch = "\"language\":\"([^\"]+)\"".toRegex().find(repo)
                val sizeMatch = "\"size\":(\\d+)".toRegex().find(repo)

                if (langMatch != null && sizeMatch != null) {
                    val lang = langMatch.groupValues[1]
                    val size = sizeMatch.groupValues[1].toIntOrNull() ?: 0

                    // 3. Add the bytes to the running total for that language!
                    languageMap[lang] = (languageMap[lang] ?: 0) + size
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext languageMap
}
