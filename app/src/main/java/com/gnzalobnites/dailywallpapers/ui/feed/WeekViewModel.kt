package com.gnzalobnites.dailywallpapers.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gnzalobnites.dailywallpapers.R  // ← AGREGA ESTE IMPORT
import com.gnzalobnites.dailywallpapers.RegionManager
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager
import com.gnzalobnites.dailywallpapers.data.repository.WallpaperRepository
import com.gnzalobnites.dailywallpapers.utils.SingleLiveEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WeekViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WallpaperRepository(application)
    private val preferences = PreferencesManager(application)

    private val _wallpapers = MutableLiveData<List<BingImage>>(emptyList())
    val wallpapers: LiveData<List<BingImage>> = _wallpapers

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = SingleLiveEvent<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private fun getString(id: Int): String {
        return getApplication<Application>().getString(id)
    }

    private fun getString(id: Int, vararg args: Any): String {
        return getApplication<Application>().getString(id, *args)
    }

    fun loadWeekWallpapers() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val market = RegionManager.findMarketByTag(preferences.region.first())
            repository.getLastImages(7, market = market).onSuccess { images ->
                _wallpapers.value = images
            }.onFailure { exception ->
                // Usa el operador Elvis para manejar el nullable
                _errorMessage.value = getString(R.string.error_loading_week, exception.message ?: "Unknown error")
            }

            _isLoading.value = false
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
    }
}