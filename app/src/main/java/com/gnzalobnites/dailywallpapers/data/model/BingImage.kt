package com.gnzalobnites.dailywallpapers.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.Date
import java.io.Serializable

@Entity(tableName = "wallpaper_history")
data class BingImage(
    @PrimaryKey
    @SerializedName("startdate")
    val startDate: String, // Usamos startDate como clave primaria única
    
    @SerializedName("url")
    val url: String,
    
    @SerializedName("urlbase")
    val urlbase: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("copyright")
    val copyright: String,
    
    val appliedDate: Date = Date(),
    var isFavorite: Boolean = false,
    var localPath: String? = null
) : Serializable {
    
    // URL para versión paisaje/horizontal (1920x1080)
    fun getFullHdUrl(): String = "https://www.bing.com${urlbase}_1920x1080.jpg"
    
    // URL para versión retrato/vertical/móvil (1080x1920)
    fun getMobileUrl(): String = "https://www.bing.com${urlbase}_1080x1920.jpg"
    
    fun getFormattedDate(): String? {
        return try {
            "${startDate.substring(6,8)}/${startDate.substring(4,6)}/${startDate.substring(0,4)}"
        } catch (e: Exception) { null }
    }
} 