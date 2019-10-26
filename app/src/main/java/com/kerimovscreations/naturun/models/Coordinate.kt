package com.kerimovscreations.naturun.models

import com.google.gson.annotations.SerializedName

data class Coordinate(
    @SerializedName("_latitude")
    val latitude: Double,
    @SerializedName("_longitude")
    val longitude: Double
)