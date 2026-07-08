package com.gnzalobnites.dailywallpapers.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class FDroidUpdateInfo(val versionName: String, val versionCode: Long)

/**
 * Comprueba si hay una versión más reciente publicada en F-Droid usando su
 * API pública de solo lectura (no descarga ningún APK ni binario ejecutable).
 * https://f-droid.org/docs/All_our_APIs/
 */
class FDroidUpdateChecker {

    suspend fun checkForUpdate(context: Context): FDroidUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val packageId = context.packageName
            val currentVersionCode = getCurrentVersionCode(context)
            if (currentVersionCode < 0) return@withContext null

            val url = URL("https://f-droid.org/api/v1/packages/$packageId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val suggestedVersionCode = json.optLong("suggestedVersionCode", -1)

            if (suggestedVersionCode <= currentVersionCode) {
                return@withContext null
            }

            val packages = json.optJSONArray("packages") ?: return@withContext null
            var versionName: String? = null
            for (i in 0 until packages.length()) {
                val pkg = packages.getJSONObject(i)
                if (pkg.optLong("versionCode") == suggestedVersionCode) {
                    versionName = pkg.optString("versionName")
                    break
                }
            }

            versionName?.let { FDroidUpdateInfo(it, suggestedVersionCode) }
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            -1L
        }
    }

    /** Abre la ficha de la app en f-droid.org; el usuario instala desde ahí, nunca esta app. */
    fun buildFDroidPageIntent(context: Context): Intent {
        val url = "https://f-droid.org/packages/${context.packageName}"
        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }
}
