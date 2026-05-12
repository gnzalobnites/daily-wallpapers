package com.gnzalobnites.dailywallpapers

import android.app.Application
import androidx.work.*
import com.gnzalobnites.dailywallpapers.worker.DailyWallpaperWorker
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import com.gnzalobnites.dailywallpapers.worker.WorkerScheduler

class WallpaperApp : Application() {
    
    private val applicationScope = CoroutineScope(Dispatchers.Default)
    
    override fun onCreate() {
        super.onCreate()
        delayedInit()
    }
    
    private fun delayedInit() {
        applicationScope.launch {
            setupWorkManager()
        }
    }
    
    private fun setupWorkManager() {
        // Usar el planificador exacto de medianoche
        WorkerScheduler.scheduleNextMidnightWorker(this)
    }
}