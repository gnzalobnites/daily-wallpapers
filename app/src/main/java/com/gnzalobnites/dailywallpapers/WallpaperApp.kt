package com.gnzalobnites.dailywallpapers

import android.app.Application
import com.gnzalobnites.dailywallpapers.worker.WorkerScheduler

class WallpaperApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Inicializar el scheduler con las preferencias guardadas
        WorkerScheduler.scheduleFromPreferences(this)
    }
}