// app/src/main/java/com/gnzalobnites/dailywallpapers/ui/history/HistoryViewModel.kt
package com.gnzalobnites.dailywallpapers.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import com.gnzalobnites.dailywallpapers.data.repository.WallpaperRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = WallpaperRepository(application)
    
    private val _wallpapers = MutableLiveData<List<BingImage>>(emptyList())
    val wallpapers: LiveData<List<BingImage>> = _wallpapers
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _selectedImage = MutableLiveData<BingImage?>()
    val selectedImage: LiveData<BingImage?> = _selectedImage
    
    // LiveData para mensajes tipo Toast
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message
    
    init {
        loadHistory()
    }
    
    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.allWallpapers.collect { list ->
                _wallpapers.value = list
                _isLoading.value = false
            }
        }
    }
    
    fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.favoriteWallpapers.collect { list ->
                _wallpapers.value = list
                _isLoading.value = false
            }
        }
    }
    
    fun selectImage(image: BingImage) {
        _selectedImage.value = image
    }
    
    fun clearSelection() {
        _selectedImage.value = null
    }
    
    fun toggleFavorite(image: BingImage) {
        viewModelScope.launch {
            repository.toggleFavorite(image)
            // Recargar la lista actual para reflejar cambios
            if (wallpapers.value?.firstOrNull { it.startDate == image.startDate }?.isFavorite != image.isFavorite) {
                // Si estamos en favoritos y quitamos favorito, recargar favoritos
                if (image.isFavorite) {
                    loadFavorites()
                } else {
                    loadHistory()
                }
            }
        }
    }
    
    fun saveToInternalStorage(image: BingImage) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Descargar el bitmap en alta calidad
                val url = image.getFullHdUrl()
                repository.downloadBitmap(url).onSuccess { bitmap ->
                    
                    val cleanTitle = image.title.replace(Regex("[^a-zA-Z0-9áéíóúñÑüÜ ]"), "").take(50).trim()
                    val fileName = if (cleanTitle.isNotEmpty()) {
                        "$cleanTitle-${System.currentTimeMillis()}.jpg"
                    } else {
                        "wallpaper-${System.currentTimeMillis()}.jpg"
                    }
                    
                    // Guardarlo localmente
                    repository.saveToInternalStorage(bitmap, fileName).onSuccess { path ->
                        repository.saveToHistory(image, path) // Actualiza el registro con la nueva ruta
                        _message.value = getApplication<Application>().getString(R.string.saved_internal_success)
                        
                        // Recargar la lista para que la UI se actualice
                        if (image.isFavorite) loadFavorites() else loadHistory()
                    }.onFailure {
                        _message.value = getApplication<Application>().getString(R.string.error_saving_internal)
                    }
                    
                }.onFailure {
                    _message.value = getApplication<Application>().getString(R.string.error_downloading_image)
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearMessage() {
        _message.value = null
    }
}