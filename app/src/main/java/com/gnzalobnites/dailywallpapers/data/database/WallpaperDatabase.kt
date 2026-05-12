package com.gnzalobnites.dailywallpapers.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.gnzalobnites.dailywallpapers.data.model.BingImage

@Database(
    entities = [BingImage::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WallpaperDatabase : RoomDatabase() {
    
    abstract fun wallpaperDao(): WallpaperDao
    
    companion object {
        @Volatile
        private var INSTANCE: WallpaperDatabase? = null
        
        fun getInstance(context: Context): WallpaperDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    WallpaperDatabase::class.java,
                    "wallpaper_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}