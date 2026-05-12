package com.gnzalobnites.dailywallpapers.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = PreferencesManager(application)
    
    private val _autoUpdate = MutableLiveData<Boolean>()
    val autoUpdate: LiveData<Boolean> = _autoUpdate
    
    // CAMBIADO: Inicializar con "mobile" en lugar de "hd"
    private val _wallpaperResolution = MutableLiveData<String>("mobile")
    val wallpaperResolution: LiveData<String> = _wallpaperResolution
    
    private val _saveToHistory = MutableLiveData<Boolean>()
    val saveToHistory: LiveData<Boolean> = _saveToHistory
    
    private val _autoApply = MutableLiveData<Boolean>()
    val autoApply: LiveData<Boolean> = _autoApply
    
    private val _darkMode = MutableLiveData<String>()
    val darkMode: LiveData<String> = _darkMode
    
    private val _language = MutableLiveData<String>()
    val language: LiveData<String> = _language

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            launch { preferences.autoUpdate.collect { _autoUpdate.value = it } }
            launch { preferences.wallpaperResolution.collect { _wallpaperResolution.value = it } }
            launch { preferences.saveToHistory.collect { _saveToHistory.value = it } }
            launch { preferences.autoApply.collect { _autoApply.value = it } }
            launch { preferences.darkMode.collect { _darkMode.value = it } }
            launch { preferences.language.collect { _language.value = it } }
        }
    }

    fun saveAutoUpdate(enabled: Boolean) {
        viewModelScope.launch {
            preferences.saveAutoUpdate(enabled)
            _autoUpdate.value = enabled
        }
    }

    fun saveWallpaperResolution(resolution: String) {
        viewModelScope.launch {
            preferences.saveWallpaperResolution(resolution)
            _wallpaperResolution.value = resolution
        }
    }

    fun saveSaveToHistory(enabled: Boolean) {
        viewModelScope.launch {
            preferences.saveSaveToHistory(enabled)
            _saveToHistory.value = enabled
        }
    }

    fun saveAutoApply(enabled: Boolean) {
        viewModelScope.launch {
            preferences.saveAutoApply(enabled)
            _autoApply.value = enabled
        }
    }

    fun saveDarkMode(mode: String) {
        viewModelScope.launch {
            preferences.saveDarkMode(mode)
            _darkMode.value = mode
        }
    }

    fun saveLanguage(lang: String) {
        viewModelScope.launch {
            preferences.saveLanguage(lang)
            _language.value = lang
        }
    }
}