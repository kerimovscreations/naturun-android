package com.kerimovscreations.naturun.services

import android.content.Context
import android.util.Log
import com.android.volley.NetworkResponse
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.LatLng
import io.reactivex.subjects.BehaviorSubject
import org.json.JSONObject

class DriverService private constructor() {

    companion object {
        val instance = DriverService()
    }

    var driverCoordinateBSubject = BehaviorSubject.createDefault(LatLng(0.0, 0.0))

    private var queue: RequestQueue? = null

    /**
     * Methods
     */

    fun setContext(context: Context) {
        queue = Volley.newRequestQueue(context)
    }

    /**
     * HTTP
     */
    fun postLocation(
        id: String,
        coordinate: LatLng,
        onResult: (result: Boolean, message: String) -> Unit
    ) {

        val params = HashMap<String, String>()
        params["id"] = id
        params["type"] = "driver"
        params["lat"] = coordinate.latitude.toString()
        params["long"] = coordinate.longitude.toString()
        val requestJSONObject = JSONObject(params as Map<*, *>)

        val request = object : JsonObjectRequest(
            Method.POST,
            "http://naturun-94aa4.appspot.com/api/updatelocation",
            requestJSONObject,
            Response.Listener {
                Log.e("APP", it.toString())
            },

            Response.ErrorListener {
                Log.e("APP", it.toString())
            }
        ) {

            override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
                val statusCode = response?.statusCode
                Log.e("APP", response?.statusCode.toString())
                onResult(statusCode == 200, "")
                return super.parseNetworkResponse(response)
            }
        }

        queue?.add(request)
    }

}