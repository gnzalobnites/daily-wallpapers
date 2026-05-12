package com.gnzalobnites.dailywallpapers.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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
}