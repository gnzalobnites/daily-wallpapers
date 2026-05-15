package com.gnzalobnites.dailywallpapers

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.gnzalobnites.dailywallpapers.worker.WorkerScheduler

class WallpaperApp : Application() {
    
    companion object {
        private const val PREF_NAME = "app_settings"
        private const val KEY_LANGUAGE = "language"
        
        lateinit var prefs: SharedPreferences
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar SharedPreferences
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        // Aplicar idioma guardado ANTES de cualquier actividad
        applySavedLanguage()
        
        // Inicializar el scheduler con las preferencias guardadas
        WorkerScheduler.scheduleFromPreferences(this)
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