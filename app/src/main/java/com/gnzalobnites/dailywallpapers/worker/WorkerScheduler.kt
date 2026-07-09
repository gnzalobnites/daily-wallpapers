package com.gnzalobnites.dailywallpapers.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.gnzalobnites.dailywallpapers.AlarmScheduler
import com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object WorkerScheduler {

    // Nombre único del trabajo periódico de respaldo. Usar enqueueUniquePeriodicWork
    // con este nombre evita duplicar el trabajo si se reprograma varias veces.
    private const val BACKUP_WORK_NAME = "daily_wallpaper_backup_work"

    fun scheduleWallpaperWork(context: Context, targetHour: Int, targetMinute: Int) {
        // Mecanismo principal: alarma exacta.
        AlarmScheduler.scheduleExactAlarm(context, targetHour, targetMinute)
        // Respaldo real: si AlarmManager falla en silencio (Doze, OEM agresivo
        // con la batería, etc.), este trabajo periódico independiente de
        // WorkManager sigue intentando aplicar el wallpaper una vez al día.
        scheduleBackupWork(context)
    }

    private fun scheduleBackupWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val backupWorkRequest = PeriodicWorkRequestBuilder<DailyWallpaperWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            backupWorkRequest
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
        AlarmScheduler.cancelAlarm(context)
        WorkManager.getInstance(context).cancelUniqueWork(BACKUP_WORK_NAME)
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
