package com.gnzalobnites.dailywallpapers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // goAsync() + CoroutineScope(Dispatchers.IO): mismo patrón que
            // WallpaperReceiver.kt. Evita bloquear el hilo principal del
            // receiver durante el arranque, cuando hay más riesgo de
            // contención de E/S y de que el sistema mate al receiver.
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = PreferencesManager(context)
                    val autoUpdate = prefs.autoUpdate.first()

                    if (autoUpdate) {
                        val hour = prefs.updateHour.first()
                        val minute = prefs.updateMinute.first()
                        AlarmScheduler.scheduleExactAlarm(context, hour, minute)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
