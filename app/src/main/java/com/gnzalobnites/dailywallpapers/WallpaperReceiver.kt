package com.gnzalobnites.dailywallpapers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gnzalobnites.dailywallpapers.worker.DailyWallpaperWorker

class WallpaperReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Ya no usamos goAsync() ni Coroutines aquí.
        // El Receiver solo actúa como un "gatillo" súper rápido.

        // 1. Le decimos a WorkManager que este trabajo requiere internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 2. Creamos una petición de trabajo de un solo uso apuntando a tu Worker actual
        val workRequest = OneTimeWorkRequestBuilder<DailyWallpaperWorker>()
            .setConstraints(constraints)
            .build()

        // 3. Lo encolamos de forma única (KEEP evita que se duplique si la alarma suena dos veces por error)
        WorkManager.getInstance(context).enqueueUniqueWork(
            "ExactAlarmTriggeredWork",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
        
        // ¡Listo! El BroadcastReceiver termina su ejecución en 2 milisegundos.
        // No hay descargas bloqueantes ni riesgo de que el sistema operativo lance un TimeoutException.
    }
}
