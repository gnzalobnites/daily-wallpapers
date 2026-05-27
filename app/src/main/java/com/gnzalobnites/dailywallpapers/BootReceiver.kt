package com.gnzalobnites.dailywallpapers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            runBlocking {
                val prefs = PreferencesManager(context)
                val autoUpdate = prefs.autoUpdate.first()
                
                if (autoUpdate) {
                    val hour = prefs.updateHour.first()
                    val minute = prefs.updateMinute.first()
                    AlarmScheduler.scheduleExactAlarm(context, hour, minute)
                }
            }
        }
    }
}