package com.gnzalobnites.dailywallpapers.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gnzalobnites.dailywallpapers.MainActivity
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.RegionManager
import com.gnzalobnites.dailywallpapers.data.repository.WallpaperRepository
import com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DailyWallpaperWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "DailyWallpaperWorker"
        private const val CACHE_FILE_NAME = "daily_wallpaper_cache.jpg"
        private const val SUCCESS_NOTIFICATION_ID = 1001
        private const val JPEG_QUALITY = 100
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = PreferencesManager(appContext)
        
        try {
            val repository = WallpaperRepository(appContext)
            val wallpaperManager = WallpaperManager.getInstance(appContext)

            val autoUpdateEnabled = prefs.autoUpdate.first()
            val autoApplyEnabled = prefs.autoApply.first()

            if (!autoUpdateEnabled) {
                return@withContext Result.success()
            }

            val market = RegionManager.findMarketByTag(prefs.region.first())
            val result = repository.getTodayImage(market)
            if (result.isFailure) {
                showErrorNotification(appContext.getString(R.string.error_connection_failed))
                reprogramarParaManana(prefs)
                return@withContext Result.retry()
            }
            
            val latestBingImage = result.getOrNull() ?: run {
                showErrorNotification(appContext.getString(R.string.error_processing_bing_data))
                reprogramarParaManana(prefs)
                return@withContext Result.retry()
            }
            
            val lastApplied = prefs.lastAppliedDate.first()
            val alreadyAppliedToday = latestBingImage.startDate == lastApplied

            // Aplicar el wallpaper solo si el autoApply está activado y todavía
            // no se aplicó hoy. Esta guarda evita que la alarma exacta y el
            // respaldo periódico de WorkManager dupliquen la aplicación del
            // wallpaper y la notificación de éxito cuando ambos se disparan
            // dentro de la misma ventana de 24 horas.
            if (autoApplyEnabled && !alreadyAppliedToday) {
                // Definimos un archivo temporal en la caché segura de la app
                val cacheFile = File(appContext.cacheDir, CACHE_FILE_NAME)

                // Descargamos la imagen de Bing.
                val resolutionPref = prefs.wallpaperResolution.first()
                val imageUrl = if (resolutionPref == "hd") latestBingImage.getFullHdUrl() else latestBingImage.getMobileUrl()

                val bitmap = repository.downloadBitmap(imageUrl).getOrNull()

                // Guardamos en caché por resiliencia (p. ej. si el proceso muere
                // justo después de descargar pero antes de terminar de aplicar).
                bitmap?.let { bmp ->
                    try {
                        FileOutputStream(cacheFile).use { out ->
                            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                        }
                        Log.d(TAG, appContext.getString(R.string.log_image_saved_to_cache))
                    } catch (e: Exception) {
                        Log.e(TAG, appContext.getString(R.string.log_cache_write_error, e.message), e)
                    }
                }

                // APLICACIÓN SEGURA
                if (bitmap != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, 
                            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }

                    prefs.saveLastAppliedDate(latestBingImage.startDate)
                    showSuccessNotification(appContext)
                } else {
                    showErrorNotification(appContext.getString(R.string.error_image_retrieval_failed))
                    reprogramarParaManana(prefs)
                    return@withContext Result.retry()
                }
            }

            // Guardar en el historial de manera independiente
            val saveToHistoryPref = prefs.saveToHistory.first()
            if (saveToHistoryPref) {
                repository.saveToHistory(latestBingImage)
            }

            // Reprogramar exitosamente para el día siguiente
            reprogramarParaManana(prefs)
            return@withContext Result.success()

        } catch (e: CancellationException) {
            // Nunca atrapar cancelaciones: hay que dejar que se propaguen para
            // no romper la cancelación cooperativa de la corrutina. Esto ocurre
            // cuando WorkManager detiene el trabajo legítimamente (p. ej. se
            // perdieron las constraints de red), no es un error real.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, appContext.getString(R.string.log_unexpected_error, e.message), e)
            showErrorNotification(appContext.getString(R.string.error_critical, e.localizedMessage ?: ""))
            reprogramarParaManana(prefs)
            return@withContext Result.retry()
        }
    }

    private suspend fun reprogramarParaManana(prefs: PreferencesManager) {
        val hour = prefs.updateHour.first()
        val minute = prefs.updateMinute.first()
        WorkerScheduler.scheduleWallpaperWork(appContext, hour, minute)
    }

    private fun showSuccessNotification(context: Context) {
        try {
            val channelId = context.getString(R.string.notification_channel_updates_id)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    context.getString(R.string.notification_channel_updates_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notification_channel_updates_description)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.notification_success_message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(SUCCESS_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, context.getString(R.string.log_notification_error, e.message), e)
        }
    }

    private fun showErrorNotification(message: String) {
        try {
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = appContext.getString(R.string.notification_channel_error_id)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    appContext.getString(R.string.notification_channel_error_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = appContext.getString(R.string.notification_channel_error_description)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            val pendingIntent = PendingIntent.getActivity(appContext, 0, intent, pendingIntentFlags)

            val notification = NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(appContext.getString(R.string.app_name))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e(TAG, appContext.getString(R.string.log_notification_error, e.message), e)
        }
    }
}