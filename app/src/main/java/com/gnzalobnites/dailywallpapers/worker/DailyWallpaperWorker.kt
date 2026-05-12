package com.gnzalobnites.dailywallpapers.worker

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gnzalobnites.dailywallpapers.data.repository.WallpaperRepository
import com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DailyWallpaperWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var shouldScheduleNextDay = true

        try {
            val prefs = PreferencesManager(appContext)
            val repository = WallpaperRepository(appContext)
            val wallpaperManager = WallpaperManager.getInstance(appContext)

            val autoApplyEnabled = prefs.autoApply.first()
            
            if (!autoApplyEnabled) {
                return@withContext Result.success()
            }

            val result = repository.getTodayImage()
            if (result.isFailure) {
                shouldScheduleNextDay = false
                return@withContext Result.retry()
            }
            
            val latestBingImage = result.getOrNull() ?: run {
                shouldScheduleNextDay = false
                return@withContext Result.retry()
            }

            val lastAppliedDate = prefs.lastAppliedDate.first() ?: ""

            val isNewDailyImage = latestBingImage.startDate != lastAppliedDate

            if (isNewDailyImage) {
                val resolutionPref = prefs.wallpaperResolution.first()
                // CAMBIADO: Usar getMobileUrl() como predeterminado, getFullHdUrl() solo si se selecciona "hd"
                val imageUrl = if (resolutionPref == "hd") 
                    latestBingImage.getFullHdUrl() 
                else 
                    latestBingImage.getMobileUrl()
                
                val bitmapResult = repository.downloadBitmap(imageUrl)
                val bitmap = bitmapResult.getOrNull()
                
                if (bitmap != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, 
                            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }

                    prefs.saveLastAppliedDate(latestBingImage.startDate)
                    
                    // Guardar en historial si está activado
                    val saveToHistoryPref = prefs.saveToHistory.first()
                    if (saveToHistoryPref) {
                        repository.saveToHistory(latestBingImage)
                    }
                } else {
                    shouldScheduleNextDay = false
                    return@withContext Result.retry()
                }
            }

            Result.success()

        } catch (e: Exception) {
            Log.e("DailyWallpaperWorker", "Error: ${e.message}", e)
            shouldScheduleNextDay = false
            Result.retry()
        } finally {
            if (shouldScheduleNextDay) {
                WorkerScheduler.scheduleNextMidnightWorker(appContext)
            }
        }
    }
}