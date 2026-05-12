package com.gnzalobnites.dailywallpapers.ui.about

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class AboutViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _appVersion = MutableLiveData<String>()
    val appVersion: LiveData<String> = _appVersion
    
    private val _appName = MutableLiveData<String>()
    val appName: LiveData<String> = _appName
    
    init {
        loadAppInfo()
    }
    
    private fun loadAppInfo() {
        try {
            val packageInfo = getApplication<Application>().packageManager
                .getPackageInfo(getApplication<Application>().packageName, 0)
            
            _appVersion.value = "Versión ${packageInfo.versionName}"
            _appName.value = getApplication<Application>()
                .getString(com.gnzalobnites.dailywallpapers.R.string.app_name)
        } catch (e: PackageManager.NameNotFoundException) {
            _appVersion.value = "Versión desconocida"
        }
    }
}
