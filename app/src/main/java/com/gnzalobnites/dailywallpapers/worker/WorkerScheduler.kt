package com.gnzalobnites.dailywallpapers.worker

import android.content.Context
import com.gnzalobnites.dailywallpapers.AlarmScheduler
import com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

object WorkerScheduler {

    fun scheduleWallpaperWork(context: Context, targetHour: Int, targetMinute: Int) {
        AlarmScheduler.scheduleExactAlarm(context, targetHour, targetMinute)
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