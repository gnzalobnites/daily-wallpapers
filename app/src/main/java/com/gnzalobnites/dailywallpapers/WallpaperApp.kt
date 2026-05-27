package com.gnzalobnites.dailywallpapers

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

class WallpaperApp : Application() {
    
    companion object {
        private const val PREF_NAME = "app_settings"
        private const val KEY_LANGUAGE = "language"
        
        lateinit var prefs: SharedPreferences
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        applySavedLanguage()
        
        // Programar alarma solo si el permiso está concedido
        runBlocking {
            val preferencesManager = com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager(this@WallpaperApp)
            val autoUpdate = preferencesManager.autoUpdate.first()
            
            if (autoUpdate && hasExactAlarmPermission()) {
                val hour = preferencesManager.updateHour.first()
                val minute = preferencesManager.updateMinute.first()
                AlarmScheduler.scheduleExactAlarm(this@WallpaperApp, hour, minute)
            }
        }
    }
    
    private fun hasExactAlarmPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }
    
    private fun applySavedLanguage() {
        val savedLanguage = prefs.getString(KEY_LANGUAGE, "es") ?: "es"
        val localeList = LocaleListCompat.forLanguageTags(savedLanguage)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
    
    fun saveLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        applySavedLanguage()
    }
}