package com.gnzalobnites.dailywallpapers.ui.main

import android.app.Application
import android.app.WallpaperManager
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import com.gnzalobnites.dailywallpapers.data.preferences.PreferencesManager
import com.gnzalobnites.dailywallpapers.utils.SingleLiveEvent
import com.gnzalobnites.dailywallpapers.data.repository.WallpaperRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WallpaperRepository(application)
    private val preferences = PreferencesManager(application)
    
    private val _currentImage = MutableLiveData<BingImage?>()
    val currentImage: LiveData<BingImage?> = _currentImage
    
    private val _currentBitmap = MutableLiveData<Bitmap?>()
    val currentBitmap: LiveData<Bitmap?> = _currentBitmap
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _successMessage = SingleLiveEvent<String?>()
    private val _errorMessage = SingleLiveEvent<String?>()
    val successMessage: LiveData<String?> = _successMessage
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _autoUpdate = MutableLiveData<Boolean>()
    val autoUpdate: LiveData<Boolean> = _autoUpdate
    
    init {
        loadPreferences()
    }
    
    private fun loadPreferences() {
        viewModelScope.launch {
            preferences.autoUpdate.collect { enabled ->
                _autoUpdate.postValue(enabled)
            }
        }
    }
    
    fun loadTodayImage() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _errorMessage.postValue(null)
            repository.getTodayImage().onSuccess { image ->
                _currentImage.postValue(image)
                loadImageBitmap(image)
            }.onFailure { exception ->
                _errorMessage.postValue(getString(R.string.error_loading, exception.message ?: ""))
                _isLoading.postValue(false)
            }
        }
    }
    
    private fun loadImageBitmap(image: BingImage) {
        viewModelScope.launch {
            val resolutionPref = preferences.wallpaperResolution.first()
            val url = if (resolutionPref == "hd") image.getFullHdUrl() else image.getMobileUrl()
            repository.downloadBitmap(url).onSuccess { bitmap ->
                _currentBitmap.postValue(bitmap)
                // ELIMINADO: checkAutoApply(bitmap, image) - Ya no se aplica automáticamente al abrir la app
            }.onFailure { exception ->
                _errorMessage.postValue(getString(R.string.error_download, exception.message ?: ""))
            }
            _isLoading.postValue(false)
        }
    }
    
    fun applyWallpaper(image: BingImage, location: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.postValue(true)
                val resolutionPref = preferences.wallpaperResolution.first()
                val imageUrl = when (resolutionPref) {
                    "hd" -> image.getFullHdUrl()
                    else -> image.getMobileUrl()
                }
                val futureTarget = Glide.with(getApplication<Application>())
                    .asBitmap()
                    .load(imageUrl)
                    .submit()
                val bitmap = futureTarget.get()
                
                val flags = when (location) {
                    1 -> WallpaperManager.FLAG_SYSTEM
                    2 -> WallpaperManager.FLAG_LOCK
                    3 -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                    else -> WallpaperManager.FLAG_SYSTEM
                }
                
                applyWallpaperOptimized(bitmap, flags)
                
                preferences.saveLastAppliedDate(image.startDate)
                _successMessage.postValue(getString(R.string.success_applied))
            } catch (e: Exception) {
                _errorMessage.postValue(getString(R.string.error_applying, e.message ?: ""))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
    
    private fun applyWallpaperOptimized(bitmap: Bitmap, flags: Int) {
        val wallpaperManager = WallpaperManager.getInstance(getApplication())
        wallpaperManager.setBitmap(bitmap, null, true, flags)
    }
    
    fun saveToGallery() {
        viewModelScope.launch {
            val image = _currentImage.value
            val bitmap = _currentBitmap.value
            if (image != null && bitmap != null) {
                _isLoading.postValue(true)
                repository.saveToGallery(bitmap, image.title).onSuccess { message ->
                    _successMessage.postValue(message)
                    repository.saveToHistory(image, message)
                }.onFailure { exception ->
                    _errorMessage.postValue(getString(R.string.error_saving, exception.message ?: ""))
                }
                _isLoading.postValue(false)
            }
        }
    }
    
    fun toggleFavorite() {
        val image = _currentImage.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.toggleFavorite(image)
                val updatedImage = repository.getWallpaperByDate(image.startDate)
                
                if (updatedImage != null) {
                    _currentImage.postValue(updatedImage)
                } else {
                    val updated = image.copy(
                        isFavorite = !image.isFavorite,
                        appliedDate = image.appliedDate
                    )
                    _currentImage.postValue(updated)
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error al actualizar favorito: ${e.message}")
            }
        }
    }
    
    fun clearMessages() {
        _errorMessage.postValue(null)
        _successMessage.postValue(null)
    }
    
    fun checkForUpdatesSilently() {
        viewModelScope.launch {
            try {
                val isAutoUpdateEnabled = preferences.autoUpdate.first()
                if (!isAutoUpdateEnabled) return@launch

                val lastCheckDate = preferences.lastUpdateCheck.first()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                
                if (lastCheckDate == today) return@launch

                val result = repository.getTodayImage()
                val latestImage = result.getOrNull() ?: return@launch

                val lastAppliedDate = preferences.lastAppliedDate.first()
                
                if (latestImage.startDate != lastAppliedDate) {
                    downloadNewWallpaperSilently(latestImage)
                }

                preferences.saveLastUpdateCheck(today)

            } catch (e: Exception) {
                // Falla silenciosamente si no hay internet
            }
        }
    }
    
    private fun downloadNewWallpaperSilently(image: BingImage) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolutionPref = preferences.wallpaperResolution.first()
                val url = if (resolutionPref == "hd") image.getFullHdUrl() else image.getMobileUrl()
                
                val futureTarget = Glide.with(getApplication<Application>())
                    .asBitmap()
                    .load(url)
                    .submit()
                val bitmap = futureTarget.get()
                
                // ELIMINADO: Auto-aplicado aquí también - Solo se aplica en el Worker
                
                val saveToHistoryPref = preferences.saveToHistory.first()
                if (saveToHistoryPref) {
                    repository.saveToHistory(image)
                }
            } catch (e: Exception) {
                // Error silencioso
            }
        }
    }
    
    private fun getString(id: Int): String {
        return getApplication<Application>().getString(id)
    }
    
    private fun getString(id: Int, vararg args: Any): String {
        return getApplication<Application>().getString(id, *args)
    }
}