package com.gnzalobnites.dailywallpapers.worker

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkerScheduler {

    fun scheduleNextMidnightWorker(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val nextMidnight = getNextMidnightTime()
        val delay = maxOf(0, nextMidnight - System.currentTimeMillis())

        val request = OneTimeWorkRequestBuilder<DailyWallpaperWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniqueWork(
            "daily_wallpaper_midnight",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun getNextMidnightTime(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}