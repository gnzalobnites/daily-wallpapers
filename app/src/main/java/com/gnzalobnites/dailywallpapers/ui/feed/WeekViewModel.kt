package com.gnzalobnites.dailywallpapers.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import com.gnzalobnites.dailywallpapers.data.repository.WallpaperRepository
import com.gnzalobnites.dailywallpapers.utils.SingleLiveEvent
import kotlinx.coroutines.launch

class WeekViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WallpaperRepository(application)

    private val _wallpapers = MutableLiveData<List<BingImage>>(emptyList())
    val wallpapers: LiveData<List<BingImage>> = _wallpapers

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = SingleLiveEvent<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadWeekWallpapers() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            repository.getLastImages(7).onSuccess { images ->
                _wallpapers.value = images
            }.onFailure { exception ->
                _errorMessage.value = "Error al cargar: ${exception.message}"
            }

            _isLoading.value = false
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
    }
}
