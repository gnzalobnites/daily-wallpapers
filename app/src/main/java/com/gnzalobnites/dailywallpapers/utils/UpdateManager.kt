package com.gnzalobnites.dailywallpapers.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val versionName: String, val downloadUrl: String)

class UpdateManager {

    suspend fun checkForUpdates(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/gnzalobnites/daily-wallpapers/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val tagName = json.getString("tag_name")
                
                val cleanTagName = tagName.removePrefix("v")
                val cleanCurrent = currentVersion.removePrefix("v")

                if (isNewerVersion(cleanCurrent, cleanTagName)) {
                    val assets = json.getJSONArray("assets")
                    if (assets.length() > 0) {
                        val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                        return@withContext UpdateInfo(tagName, downloadUrl)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun isNewerVersion(current: String, fetched: String): Boolean {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val fetchedParts = fetched.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLength = maxOf(currentParts.size, fetchedParts.size)
        for (i in 0 until maxLength) {
            val c = currentParts.getOrElse(i) { 0 }
            val f = fetchedParts.getOrElse(i) { 0 }
            if (f > c) return true
            if (f < c) return false
        }
        return false
    }
}