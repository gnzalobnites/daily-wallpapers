// app/src/main/java/com/gnzalobnites/dailywallpapers/WallpaperReceiver.kt
package com.gnzalobnites.dailywallpapers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager
import com.gnzalobnites.dailywallpapers.data.repository.WallpaperRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class WallpaperReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PreferencesManager(context)
                val autoUpdateEnabled = prefs.autoUpdate.first()
                
                if (!autoUpdateEnabled) {
                    pendingResult.finish()
                    return@launch
                }
                
                val repository = WallpaperRepository(context)
                val wallpaperManager = WallpaperManager.getInstance(context)
                val autoApplyEnabled = prefs.autoApply.first()
                
                // Obtener imagen del día
                val result = repository.getTodayImage()
                if (result.isFailure) {
                    showNotification(context, "Error al obtener el wallpaper diario", true)
                    rescheduleAlarm(context, prefs)
                    pendingResult.finish()
                    return@launch
                }
                
                val image = result.getOrNull() ?: run {
                    pendingResult.finish()
                    return@launch
                }
                
                // Aplicar wallpaper si está habilitado
                var bitmap: android.graphics.Bitmap? = null
                
                if (autoApplyEnabled) {
                    val resolutionPref = prefs.wallpaperResolution.first()
                    val imageUrl = if (resolutionPref == "hd") image.getFullHdUrl() else image.getMobileUrl()
                    
                    val bitmapResult = repository.downloadBitmap(imageUrl)
                    bitmap = bitmapResult.getOrNull()
                    
                    if (bitmap != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            wallpaperManager.setBitmap(bitmap, null, true,
                                WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                        } else {
                            wallpaperManager.setBitmap(bitmap)
                        }
                        prefs.saveLastAppliedDate(image.startDate)
                        showNotification(context, "Wallpaper actualizado correctamente", false)
                    } else {
                        showNotification(context, "Error al descargar el wallpaper", true)
                    }
                }
                
                // Guardar en historial con el bitmap descargado (si existe)
                val saveToHistoryPref = prefs.saveToHistory.first()
                if (saveToHistoryPref && bitmap != null) {
                    // 1. Limpiar el título para asegurar que sea un nombre de archivo válido
                    val cleanTitle = image.title
                        .replace(Regex("[^a-zA-Z0-9áéíóúñÑüÜ ]"), "")
                        .take(50)
                        .trim()
                        
                    val fileName = if (cleanTitle.isNotEmpty()) {
                        "$cleanTitle-${System.currentTimeMillis()}.jpg"
                    } else {
                        "wallpaper-${System.currentTimeMillis()}.jpg"
                    }

                    // 2. Guardar en el almacenamiento interno
                    val saveResult = repository.saveToInternalStorage(bitmap, fileName)
                    val localPath = saveResult.getOrNull()

                    // 3. Guardar el registro en la base de datos CON la ruta local
                    repository.saveToHistory(image, localPath)
                } else if (saveToHistoryPref) {
                    // Si no se pudo descargar el bitmap (autoApply desactivado), guardar sin ruta local
                    repository.saveToHistory(image)
                }
                
                // Reprogramar para el día siguiente
                rescheduleAlarm(context, prefs)
                
            } catch (e: Exception) {
                Log.e("WallpaperReceiver", "Error: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    private suspend fun rescheduleAlarm(context: Context, prefs: PreferencesManager) {
        val hour = prefs.updateHour.first()
        val minute = prefs.updateMinute.first()
        AlarmScheduler.scheduleExactAlarm(context, hour, minute)
    }
    
    private fun showNotification(context: Context, message: String, isError: Boolean) {
        val channelId = "wallpaper_updates"
        val notificationManager = NotificationManagerCompat.from(context)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Actualizaciones de Wallpaper",
                if (isError) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de actualización diaria de wallpaper"
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Daily Wallpapers")
            .setContentText(message)
            .setPriority(if (isError) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
} 