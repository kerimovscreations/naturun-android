package com.kerimovscreations.naturun.activities

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.kerimovscreations.naturun.R
import com.kerimovscreations.naturun.models.Animal
import com.kerimovscreations.naturun.services.DriverService
import com.kerimovscreations.naturun.services.NotificationService
import com.kerimovscreations.naturun.tools.DataSource
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    /**
     * UI Views
     */

    private lateinit var actionBtn: Button
    private lateinit var cameraBtn: Button
    private lateinit var warningCard: View
    private lateinit var warningCount: TextView

    /**
     * Variables
     */

    private var googleMap: GoogleMap? = null
    private var isDriverAnimating = false
    private var isAutoCameraMoveEnabled = true

    private var driverCoordinateSubscription: Disposable? = null
    private var nearbyAnimalCoordinatesSubscription: Disposable? = null
    private var timerSubscription: Disposable? = null

    private var driverMarker: Marker? = null

    private var localUserId: String? = null

    private var animalMarkers = arrayListOf<Marker>()

    private var animalIc: BitmapDescriptor? = null

    /**
     * Activity methods
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bind()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        val pref = applicationContext.getSharedPreferences("LocalPref", Context.MODE_PRIVATE)

        localUserId = pref.getString(getString(R.string.used_id_key), null)

        if (localUserId == null) {
            val uuid = UUID.randomUUID()
            localUserId = uuid.toString()
            pref.edit().putString(getString(R.string.used_id_key), localUserId).apply()
        }

        animalIc = BitmapDescriptorFactory.fromResource(R.mipmap.ic_animal)
    }

    override fun onResume() {
        super.onResume()

        driverCoordinateSubscription =
            DriverService.instance.driverCoordinateBSubject.subscribe({ coordinate ->
                runOnUiThread {
                    moveMapTo(coordinate)
                }
            }, { error ->
                error.printStackTrace()
            })

        nearbyAnimalCoordinatesSubscription =
            DriverService.instance.nearbyAnimalsCoordinatesBSubject.subscribe({ animals ->
                runOnUiThread {
                    drawAnimals(animals)
                }
            }, { error ->
                error.printStackTrace()
            })
    }

    override fun onPause() {
        super.onPause()

        stopDriverAnimation()

        driverCoordinateSubscription?.dispose()
        nearbyAnimalCoordinatesSubscription?.dispose()
        timerSubscription?.dispose()
    }

    /**
     * UI binding
     */

    private fun bind() {
        actionBtn = findViewById(R.id.animation_action_btn)
        cameraBtn = findViewById(R.id.follow_action_btn)
        warningCard = findViewById(R.id.warning_card)
        warningCount = findViewById(R.id.info_count)

        actionBtn.setOnClickListener { onAction() }
        cameraBtn.setOnClickListener { onFollowCamera() }
    }

    /**
     * Methods
     */

    private fun startDriverAnimation() {
        if (isDriverAnimating || googleMap == null) {
            return
        }

        isDriverAnimating = true

        var currentTimeStep = 0
        val driverRouteCoordinates = DataSource.getMovementLocations()

        DriverService.instance.driverCoordinateBSubject.onNext(driverRouteCoordinates[currentTimeStep])

        timerSubscription = io.reactivex.Observable.interval(2, TimeUnit.SECONDS).subscribe {
            currentTimeStep++
            if (currentTimeStep < driverRouteCoordinates.size) {
                DriverService.instance.driverCoordinateBSubject.onNext(driverRouteCoordinates[currentTimeStep])
            } else {
                stopDriverAnimation()
            }
        }
    }

    private fun stopDriverAnimation() {
        if (!isDriverAnimating) {
            return
        }

        isDriverAnimating = false
        timerSubscription?.dispose()
    }

    private fun moveMapTo(coordinate: LatLng) {
        googleMap?.let { googleMap ->

            val finalCoordinate = LatLng(coordinate.latitude + 0.003, coordinate.longitude)
            DriverService.instance.postLocation(this.localUserId!!, coordinate) { _, _ ->
            }
            DriverService.instance.getNearbyAnimals(
                coordinate,
                1000
            ) { _, _ -> }

            if (driverMarker == null) {
                val carIc = BitmapDescriptorFactory.fromResource(R.mipmap.car_ic)

                val markerOptions = MarkerOptions().position(coordinate)
                    .title("Current Location")
                    .snippet("Simulated driving action")
                    .icon(carIc)

                driverMarker = googleMap.addMarker(markerOptions)
            } else {
                driverMarker?.position = coordinate
            }

            if (isAutoCameraMoveEnabled){
                val cameraPosition = CameraPosition.Builder()
                    .target(finalCoordinate)
                    .zoom(17f)
                    .bearing(0f)
                    .tilt(80f)
                    .build()
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }
    }

    private fun drawAnimals(animals: List<Animal>) {
        googleMap?.let { googleMap ->

            if (animalMarkers.isEmpty() && animals.isNotEmpty()) {
                NotificationService.instance.setupNotification(this)
            }

            animalMarkers.forEach { marker ->
                marker.remove()
            }

            animalMarkers.clear()

            animals.forEach { animal ->
                val animalMarkerOptions = MarkerOptions().position(
                    LatLng(
                        animal.coordinates.latitude,
                        animal.coordinates.longitude
                    )
                )
                    .title("Animal id: ${animal.deviceId}")
                    .snippet("Time: ${animal.updatedAt}")
                    .icon(animalIc)
                animalMarkers.add(googleMap.addMarker(animalMarkerOptions))
            }

            if (animalMarkers.isEmpty()) {
                warningCard.visibility = View.GONE
            } else {
                warningCard.visibility = View.VISIBLE
                warningCount.text = animalMarkers.size.toString()
            }
        }
    }

    /**
     * Click handlers
     */

    private fun onAction() {
        if (isDriverAnimating) {
            stopDriverAnimation()
            actionBtn.text = "Start"
            actionBtn.background =
                ResourcesCompat.getDrawable(resources, R.drawable.bg_action_btn_green, null)
        } else {
            startDriverAnimation()
            actionBtn.text = "Stop"
            actionBtn.background =
                ResourcesCompat.getDrawable(resources, R.drawable.bg_action_btn_pink, null)
        }
    }

    private fun onFollowCamera() {
        cameraBtn.alpha = 1.0f
        isAutoCameraMoveEnabled = true
    }


    /**
     * Map callback
     */

    override fun onMapReady(g0: GoogleMap?) {
        googleMap = g0
        googleMap?.mapType = MAP_TYPE_SATELLITE
        googleMap?.setPadding(24, 0, 0, 0)
        googleMap?.setOnCameraMoveStartedListener {reason ->
            if (reason == REASON_GESTURE) {
                isAutoCameraMoveEnabled = false
                cameraBtn.alpha = 0.5f
            }
        }
    }
}
