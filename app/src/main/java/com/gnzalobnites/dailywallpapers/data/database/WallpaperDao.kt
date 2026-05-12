package com.gnzalobnites.dailywallpapers.data.database

import androidx.room.*
import com.gnzalobnites.dailywallpapers.data.model.BingImage
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface WallpaperDao {
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWallpaper(wallpaper: BingImage)
    
    @Update
    suspend fun updateWallpaper(wallpaper: BingImage)
    
    @Delete
    suspend fun deleteWallpaper(wallpaper: BingImage)
    
    @Query("SELECT * FROM wallpaper_history ORDER BY appliedDate DESC")
    fun getAllWallpapers(): Flow<List<BingImage>>
    
    @Query("SELECT * FROM wallpaper_history WHERE isFavorite = 1 ORDER BY appliedDate DESC")
    fun getFavoriteWallpapers(): Flow<List<BingImage>>
    
    @Query("SELECT * FROM wallpaper_history WHERE startDate = :startDate")
    suspend fun getWallpaperByDate(startDate: String): BingImage?
    
    @Query("SELECT COUNT(*) > 0 FROM wallpaper_history WHERE startDate = :startDate")
    suspend fun exists(startDate: String): Boolean
    
    @Query("DELETE FROM wallpaper_history WHERE appliedDate < :cutoffDate")
    suspend fun deleteOldWallpapers(cutoffDate: Date)
    
    @Query("SELECT * FROM wallpaper_history WHERE startDate = :startDate")
    suspend fun getByDate(startDate: String): BingImage?
}