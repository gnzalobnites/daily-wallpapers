package com.gnzalobnites.dailywallpapers.data.model

import com.google.gson.annotations.SerializedName

data class BingResponse(
    @SerializedName("images") val images: List<BingImage>
)