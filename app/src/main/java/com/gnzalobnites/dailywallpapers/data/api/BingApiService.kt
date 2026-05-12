package com.gnzalobnites.dailywallpapers.data.api

import com.gnzalobnites.dailywallpapers.data.model.BingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BingApiService {
    @GET("HPImageArchive.aspx")
    suspend fun getDailyImage(
        @Query("format") format: String = "js",
        @Query("idx") index: Int = 0,
        @Query("n") count: Int = 1,
        @Query("mkt") market: String = "es-ES"
    ): BingResponse
    
    @GET("HPImageArchive.aspx")
    suspend fun getLastImages(
        @Query("format") format: String = "js",
        @Query("idx") index: Int = 0,
        @Query("n") count: Int = 8,
        @Query("mkt") market: String = "es-ES"
    ): BingResponse
}