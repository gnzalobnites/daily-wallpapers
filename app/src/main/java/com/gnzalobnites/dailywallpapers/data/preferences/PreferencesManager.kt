package com.gnzalobnites.dailywallpapers.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        val AUTO_UPDATE = booleanPreferencesKey("auto_update")
        val WALLPAPER_RESOLUTION = stringPreferencesKey("wallpaper_resolution")
        val SAVE_TO_HISTORY = booleanPreferencesKey("save_to_history")
        val AUTO_APPLY = booleanPreferencesKey("auto_apply")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val LAST_APPLIED_DATE = stringPreferencesKey("last_applied_date")
        // NUEVA: llave interna del Worker para el guard anti-duplicados,
        // independiente de las aplicaciones manuales del usuario.
        val LAST_AUTO_APPLY_TRIGGER = stringPreferencesKey("last_auto_apply_trigger")
        val LAST_UPDATE_CHECK = stringPreferencesKey("last_update_check")
        
        // NUEVAS: Preferencias para la hora de actualización
        val UPDATE_HOUR = intPreferencesKey("update_hour")
        val UPDATE_MINUTE = intPreferencesKey("update_minute")
        
        // NUEVA: Preferencia para la región
        val REGION = stringPreferencesKey("region")
    }
    
    val autoUpdate: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AUTO_UPDATE] ?: true }
    
    val wallpaperResolution: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[WALLPAPER_RESOLUTION] ?: "mobile" }
    
    val saveToHistory: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SAVE_TO_HISTORY] ?: true }
    
    val autoApply: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AUTO_APPLY] ?: false }
    
    val darkMode: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[DARK_MODE] ?: "system" }
    
    val language: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[LANGUAGE] ?: "es" }
    
    val lastAppliedDate: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[LAST_APPLIED_DATE] }
    
    // NUEVO: usado únicamente por DailyWallpaperWorker para saber si ya
    // se ejecutó la aplicación automática para el horario programado actual.
    val lastAutoApplyTrigger: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[LAST_AUTO_APPLY_TRIGGER] }
    
    val lastUpdateCheck: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[LAST_UPDATE_CHECK] }
    
    // NUEVOS: Getters para la hora de actualización
    val updateHour: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[UPDATE_HOUR] ?: 0 }
    
    val updateMinute: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[UPDATE_MINUTE] ?: 0 }
    
    // NUEVA: Getter para la región
    val region: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[REGION] ?: "es-ES" }
    
    suspend fun saveAutoUpdate(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_UPDATE] = enabled
        }
    }
    
    suspend fun saveWallpaperResolution(resolution: String) {
        context.dataStore.edit { preferences ->
            preferences[WALLPAPER_RESOLUTION] = resolution
        }
    }
    
    suspend fun saveSaveToHistory(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SAVE_TO_HISTORY] = enabled
        }
    }
    
    suspend fun saveAutoApply(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_APPLY] = enabled
        }
    }
    
    suspend fun saveDarkMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = mode
        }
    }
    
    suspend fun saveLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE] = lang
        }
    }
    
    suspend fun saveLastAppliedDate(date: String?) {
        context.dataStore.edit { preferences ->
            if (date == null) {
                preferences.remove(LAST_APPLIED_DATE)
            } else {
                preferences[LAST_APPLIED_DATE] = date
            }
        }
    }
    
    suspend fun saveLastAutoApplyTrigger(trigger: String?) {
        context.dataStore.edit { preferences ->
            if (trigger == null) {
                preferences.remove(LAST_AUTO_APPLY_TRIGGER)
            } else {
                preferences[LAST_AUTO_APPLY_TRIGGER] = trigger
            }
        }
    }
    
    suspend fun saveLastUpdateCheck(date: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_UPDATE_CHECK] = date
        }
    }
    
    // NUEVOS: Setters para la hora de actualización
    suspend fun saveUpdateTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[UPDATE_HOUR] = hour
            preferences[UPDATE_MINUTE] = minute
        }
    }
    
    // NUEVA: Setter para la región
    suspend fun saveRegion(regionTag: String) {
        context.dataStore.edit { preferences ->
            preferences[REGION] = regionTag
        }
    }
}