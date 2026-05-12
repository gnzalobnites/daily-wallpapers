package com.gnzalobnites.dailywallpapers.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.app.WallpaperManager
import android.os.Build
import android.provider.MediaStore
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.data.api.RetrofitClient
import com.gnzalobnites.dailywallpapers.data.database.WallpaperDatabase
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import java.util.Date

class WallpaperRepository(private val context: Context) {
    
    private val api = RetrofitClient.apiService
    private val database = WallpaperDatabase.getInstance(context)
    private val wallpaperDao = database.wallpaperDao()
    
    val allWallpapers: Flow<List<BingImage>> = wallpaperDao.getAllWallpapers()
    val favoriteWallpapers: Flow<List<BingImage>> = wallpaperDao.getFavoriteWallpapers()
    
    suspend fun getTodayImage(): Result<BingImage> {
        return try {
            val response = api.getDailyImage()
            val image = response.images.firstOrNull()
            
            image?.let {
                Result.success(it)
            } ?: Result.failure(Exception("No se encontró imagen"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getLastImages(count: Int = 8): Result<List<BingImage>> {
        return try {
            val response = api.getLastImages(index = 0, count = count)
            Result.success(response.images)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun downloadBitmap(imageUrl: String): Result<Bitmap> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.connect()
                
                val inputStream = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                Result.success(bitmap)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun setWallpaper(bitmap: Bitmap, setOnLockScreen: Boolean = false): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                
                if (setOnLockScreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                } else {
                    wallpaperManager.setBitmap(bitmap)
                }
                
                Result.success(true)
            } catch (e: IOException) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun saveToGallery(bitmap: Bitmap, title: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val filename = context.getString(R.string.file_name_format, System.currentTimeMillis())
                val resolver = context.contentResolver
                
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/DailyWallpapers")
                }
                
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    }
                    Result.success(context.getString(R.string.success_saved, filename))
                } ?: Result.failure(Exception("No se pudo crear el archivo"))
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Guarda en historial evitando duplicados usando startDate como clave única
     */
    suspend fun saveToHistory(image: BingImage, localPath: String? = null) {
        // Verificar si ya existe
        val exists = image.startDate.let { wallpaperDao.exists(it) }
        
        if (!exists) {
            val imageToSave = image.copy(
                appliedDate = Date(),
                localPath = localPath
            )
            wallpaperDao.insertWallpaper(imageToSave)
        } else {
            // Si ya existe, podríamos actualizar solo el localPath si es necesario
            localPath?.let { path ->
                val existing = wallpaperDao.getWallpaperByDate(image.startDate)
                existing?.let {
                    if (it.localPath == null) {
                        wallpaperDao.updateWallpaper(it.copy(localPath = path))
                    }
                }
            }
        }
    }
    
    /**
     * Toggle favorito con UPSERT: inserta si no existe, actualiza si existe
     */
    suspend fun toggleFavorite(image: BingImage) {
        // Buscar si ya existe en la base de datos
        val existing = image.startDate.let { wallpaperDao.getWallpaperByDate(it) }
        
        if (existing == null) {
            // No existe: insertar nueva con isFavorite = true
            val newImage = image.copy(
                isFavorite = true,
                appliedDate = Date()
            )
            wallpaperDao.insertWallpaper(newImage)
        } else {
            // Existe: actualizar solo el estado de favorito
            wallpaperDao.updateWallpaper(
                existing.copy(isFavorite = !existing.isFavorite)
            )
        }
    }
    
    /**
 * Obtiene un wallpaper por su fecha (startDate)
 */
suspend fun getWallpaperByDate(startDate: String): BingImage? {
    return wallpaperDao.getByDate(startDate)
}
    
    private fun getString(id: Int): String = context.getString(id)
    private fun getString(id: Int, vararg args: Any): String = context.getString(id, *args)
}