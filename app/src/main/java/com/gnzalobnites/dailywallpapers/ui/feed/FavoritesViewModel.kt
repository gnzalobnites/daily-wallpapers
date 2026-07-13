package com.gnzalobnites.dailywallpapers.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import com.gnzalobnites.dailywallpapers.data.repository.WallpaperRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WallpaperRepository(application)

    private val _wallpapers = MutableLiveData<List<BingImage>>(emptyList())
    val wallpapers: LiveData<List<BingImage>> = _wallpapers

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.favoriteWallpapers.collect { list ->
                _wallpapers.value = list
                _isLoading.value = false
            }
        }
    }
}
