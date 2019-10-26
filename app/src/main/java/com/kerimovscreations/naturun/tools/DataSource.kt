package com.kerimovscreations.naturun.tools

import com.google.android.gms.maps.model.LatLng

object DataSource {

    fun getMovementLocations(): List<LatLng> {
        val list = arrayListOf<LatLng>()

        val startX = -32.349657
        val endX = -32.321607
        val startY = 121.759095
        val endY = 121.758989
        val stepX = (endX - startX) / 100.0
        val stepY = (endY - startY) / 100.0

        for (i in 0 until 100) {
            list.add(LatLng(startX + stepX * i, startY + stepY * i))
        }

        return list
    }
}