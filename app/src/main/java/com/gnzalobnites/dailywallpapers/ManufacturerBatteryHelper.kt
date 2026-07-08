package com.gnzalobnites.dailywallpapers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object ManufacturerBatteryHelper {

    private val knownIntents: Map<String, List<Intent>> = mapOf(
        "xiaomi" to listOf(
            Intent().setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        ),
        "zte" to listOf(
            Intent().setClassName(
                "com.zte.heartyservice",
                "com.zte.heartyservice.autorun.AppAutoRunManager"
            ),
            Intent().setClassName(
                "com.zte.heartyservice",
                "com.zte.heartyservice.setting.MainTabActivity"
            )
        ),
        "huawei" to listOf(
            Intent().setClassName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        ),
        "oppo" to listOf(
            Intent().setClassName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        ),
        "vivo" to listOf(
            Intent().setClassName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        )
    )

    /** True solo para fabricantes con capas conocidas por matar procesos en segundo plano. */
    fun isKnownRestrictiveManufacturer(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return knownIntents.keys.any { manufacturer.contains(it) }
    }

    fun openManufacturerBatterySettings(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val candidates = knownIntents.entries
            .firstOrNull { manufacturer.contains(it.key) }
            ?.value.orEmpty()

        for (intent in candidates) {
            try {
                context.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                // Probar el siguiente candidato o caer al ajuste generico
            }
        }
        openGenericAppDetails(context)
    }

    private fun openGenericAppDetails(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}
