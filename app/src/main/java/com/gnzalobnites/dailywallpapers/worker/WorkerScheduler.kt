package com.gnzalobnites.dailywallpapers.worker

import android.content.Context
import androidx.work.*
import com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkerScheduler {

    private const val WORK_NAME = "daily_wallpaper_update"

    fun scheduleWallpaperWork(context: Context, targetHour: Int, targetMinute: Int) {
        val initialDelay = calculateInitialDelay(targetHour, targetMinute)
        
        // Relajamos las restricciones para que sea más probable que se ejecute a la hora exacta
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Usamos OneTimeWorkRequest. El worker se encargará de programar el siguiente
        val workRequest = OneTimeWorkRequestBuilder<DailyWallpaperWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES) // Si falla, reintenta rápido
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    fun scheduleFromPreferences(context: Context) {
        runBlocking {
            val prefs = PreferencesManager(context)
            val autoUpdate = prefs.autoUpdate.first()
            val hour = prefs.updateHour.first()
            val minute = prefs.updateMinute.first()
            
            if (autoUpdate) {
                scheduleWallpaperWork(context, hour, minute)
            } else {
                cancelScheduledWork(context)
            }
        }
    }
    
    fun cancelScheduledWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
    
    private fun calculateInitialDelay(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val scheduledTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Si la hora ya pasó hoy, programar para mañana a esa hora
        if (scheduledTime.before(now)) {
            scheduledTime.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return scheduledTime.timeInMillis - now.timeInMillis
    }
    
    fun getFormattedScheduledTime(context: Context): String {
        return runBlocking {
            val prefs = PreferencesManager(context)
            val hour = prefs.updateHour.first()
            val minute = prefs.updateMinute.first()
            String.format("%02d:%02d", hour, minute)
        }
    }
} 
