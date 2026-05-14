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
import com.gnzalobnites.dailywallpapers.data.repository.WallpaperRepository
import com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DailyWallpaperWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

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

            val result = repository.getTodayImage()
            if (result.isFailure) {
                showErrorNotification("No se pudo conectar con el servidor para obtener el wallpaper diario.")
                reprogramarParaManana(prefs)
                return@withContext Result.retry()
            }
            
            val latestBingImage = result.getOrNull() ?: run {
                showErrorNotification("Error al procesar los datos de la imagen de Bing.")
                reprogramarParaManana(prefs)
                return@withContext Result.retry()
            }

            // Aplicar el wallpaper SIEMPRE si el autoApply está activado (sin importar si ya estaba aplicado)
            if (autoApplyEnabled) {
                val resolutionPref = prefs.wallpaperResolution.first()
                val imageUrl = if (resolutionPref == "hd") 
                    latestBingImage.getFullHdUrl() 
                else 
                    latestBingImage.getMobileUrl()
                
                val bitmapResult = repository.downloadBitmap(imageUrl)
                val bitmap = bitmapResult.getOrNull()
                
                if (bitmap != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, 
                            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }

                    prefs.saveLastAppliedDate(latestBingImage.startDate)
                } else {
                    showErrorNotification("Falló la descarga de la imagen al intentar aplicar el wallpaper.")
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

        } catch (e: Exception) {
            Log.e("DailyWallpaperWorker", "Error inesperado: ${e.message}", e)
            showErrorNotification("Ocurrió un error crítico: ${e.localizedMessage}")
            reprogramarParaManana(prefs)
            return@withContext Result.retry()
        }
    }

    private suspend fun reprogramarParaManana(prefs: PreferencesManager) {
        val hour = prefs.updateHour.first()
        val minute = prefs.updateMinute.first()
        WorkerScheduler.scheduleWallpaperWork(appContext, hour, minute)
    }

    private fun showErrorNotification(message: String) {
        try {
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "wallpaper_error_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Errores de Wallpaper",
                    NotificationManager.IMPORTANCE_HIGH // Subimos a HIGH para que haga ruido/aparezca en pantalla
                ).apply {
                    description = "Notificaciones cuando falla la actualización automática del wallpaper"
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
                .setContentTitle("Daily Wallpapers")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioridad alta
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e("DailyWallpaperWorker", "Error al mostrar notificación: ${e.message}", e)
        }
    }
}
