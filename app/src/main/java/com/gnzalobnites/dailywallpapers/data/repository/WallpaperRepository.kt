// app/src/main/java/com/gnzalobnites/dailywallpapers/data/repository/WallpaperRepository.kt
package com.gnzalobnites.dailywallpapers.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.app.WallpaperManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gnzalobnites.dailywallpapers.R
import com.gnzalobnites.dailywallpapers.data.api.RetrofitClient
import com.gnzalobnites.dailywallpapers.data.database.WallpaperDatabase
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
    
    /**
     * Guarda la imagen en el almacenamiento compartido (Pictures/DailyWallpapers)
     * para que persista incluso después de desinstalar la app.
     */
    suspend fun saveToGallery(bitmap: Bitmap, title: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val folderName = "DailyWallpapers"
                val mimeType = "image/jpeg"
                // Limpiar el título para usarlo como nombre de archivo
                val cleanTitle = title
                    .replace(Regex("[^a-zA-Z0-9áéíóúñÑüÜ ]"), "")
                    .take(50)
                    .trim()
                val timestamp = System.currentTimeMillis()
                val fileName = if (cleanTitle.isNotEmpty()) {
                    "$cleanTitle-$timestamp.jpg"
                } else {
                    "wallpaper-$timestamp.jpg"
                }
                
                val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ (API 29+): Usar MediaStore
                    saveUsingMediaStore(bitmap, folderName, fileName, mimeType)
                } else {
                    // Android 9 y anteriores: Usar File API tradicional
                    saveUsingFileApi(bitmap, folderName, fileName, mimeType)
                }
                
                if (success) {
                    Result.success(context.getString(R.string.success_saved, fileName))
                } else {
                    Result.failure(Exception("No se pudo guardar la imagen"))
                }
                
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Guarda usando MediaStore (Android 10+)
     * Esta es la forma moderna que no requiere permisos de escritura
     */
    private fun saveUsingMediaStore(
        bitmap: Bitmap, 
        folderName: String, 
        fileName: String, 
        mimeType: String
    ): Boolean {
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$folderName")
                // Marcar como pendiente mientras se escribe
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            
            val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val imageUri = resolver.insert(imageCollection, contentValues) ?: return false
            
            // Escribir la imagen
            resolver.openOutputStream(imageUri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            
            // Liberar el archivo para que otras apps puedan verlo
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Guarda usando File API tradicional (Android 9 y anteriores)
     * Requiere el permiso WRITE_EXTERNAL_STORAGE
     */
    private fun saveUsingFileApi(
        bitmap: Bitmap, 
        folderName: String, 
        fileName: String, 
        mimeType: String
    ): Boolean {
        return try {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                folderName
            )
            
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val file = File(directory, fileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            
            // Notificar a la galería para que aparezca inmediatamente
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType),
                null
            )
            
            // Guardar la ruta local en la imagen para referencia futura
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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