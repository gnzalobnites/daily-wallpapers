package com.gnzalobnites.dailywallpapers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gnzalobnites.dailywallpapers.worker.WorkerScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            WorkerScheduler.scheduleNextMidnightWorker(context)
        }
    }
}