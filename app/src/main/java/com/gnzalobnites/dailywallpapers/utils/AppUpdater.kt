package com.gnzalobnites.dailywallpapers.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.gnzalobnites.dailywallpapers.R
import java.io.File

class AppUpdater(private val context: Context) {

    private var downloadId: Long = -1
    private var downloadCompleteReceiver: BroadcastReceiver? = null

    fun downloadAndInstall(url: String, fileName: String = "DailyWallpapers-update.apk") {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(context.getString(R.string.update_download_title))
            setDescription(context.getString(R.string.update_download_description))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        downloadCompleteReceiver = onDownloadComplete(fileName)
        context.registerReceiver(
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )
        
        Toast.makeText(context, R.string.update_download_started, Toast.LENGTH_SHORT).show()
    }

    private fun onDownloadComplete(fileName: String) = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                installApk(context, fileName)
                try {
                    context.unregisterReceiver(this)
                } catch (e: IllegalArgumentException) {
                    // El receiver ya no estaba registrado
                }
                downloadCompleteReceiver = null
            }
        }
    }

    private fun installApk(context: Context, fileName: String) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (!file.exists()) {
            Toast.makeText(context, R.string.update_file_not_found, Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        if (installIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(installIntent)
        } else {
            Toast.makeText(context, R.string.update_no_installer, Toast.LENGTH_SHORT).show()
        }
    }

    fun cleanup() {
        downloadCompleteReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Ignorar
            }
            downloadCompleteReceiver = null
        }
    }
}