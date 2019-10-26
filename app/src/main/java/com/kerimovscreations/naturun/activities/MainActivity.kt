package com.kerimovscreations.naturun.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kerimovscreations.naturun.R
import com.kerimovscreations.naturun.services.DriverService
import com.kerimovscreations.naturun.tools.DataSource
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit




class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    /**
     * UI Views
     */

    private lateinit var actionBtn: Button

    /**
     * Variables
     */

    private var user: FirebaseUser? = null
    private var googleMap: GoogleMap? = null
    private var isDriverAnimating = false

    private var driverCoordinateSubscription: Disposable? = null
    private var timerSubscription: Disposable? = null

    private var driverMarker: Marker? = null

    /**
     * Activity methods
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bind()

        FirebaseAuth.getInstance().currentUser?.let {
            this.user = it
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()

        if (this.user == null) {
            toAuth()
        }

        startDriverAnimation()

        driverCoordinateSubscription =
            DriverService.driverCoordinateBSubject.subscribe({ coordinate ->
                runOnUiThread {
                    moveMapTo(coordinate)
                }
            }, { error ->
                error.printStackTrace()
            })
    }

    override fun onPause() {
        super.onPause()

        stopDriverAnimation()

        driverCoordinateSubscription?.dispose()
        timerSubscription?.dispose()
    }

    /**
     * UI binding
     */

    private fun bind() {
        actionBtn = findViewById(R.id.animation_action_btn)

        actionBtn.setOnClickListener { onAction() }
    }

    /**
     * Navigation
     */

    private fun toAuth() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
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

        timerSubscription = io.reactivex.Observable.interval(3, TimeUnit.SECONDS).subscribe {
            currentTimeStep++
            if (currentTimeStep < driverRouteCoordinates.size) {
                DriverService.driverCoordinateBSubject.onNext(driverRouteCoordinates[currentTimeStep])
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

            if (driverMarker == null) {
                val carIc = BitmapDescriptorFactory.fromResource(R.mipmap.car_ic)

                val markerOptions =  MarkerOptions().position(coordinate)
                    .title("Current Location")
                    .snippet("Simulated driving action")
                    .icon(carIc)

                driverMarker = googleMap.addMarker(markerOptions)
            } else {
                driverMarker?.position = coordinate
            }

            val cameraPosition = CameraPosition.Builder()
                .target(coordinate)
                .zoom(17f)
                .bearing(0f)
                .tilt(80f)
                .build()
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    /**
     * Click handlers
     */

    private fun onAction() {
        if (isDriverAnimating) {
            stopDriverAnimation()
            actionBtn.text = "Start"
        } else {
            startDriverAnimation()
            actionBtn.text = "Stop"
        }
    }

    /**
     * Activity results
     */

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                FirebaseAuth.getInstance().currentUser?.let {
                    this.user = it
                }
            } else {
                response?.error?.errorCode?.let { errorCode ->
                    Log.e("APP", "Error code $errorCode")
                }
            }
        }
    }

    /**
     * Map callback
     */

    override fun onMapReady(g0: GoogleMap?) {
        googleMap = g0
        googleMap?.mapType = MAP_TYPE_SATELLITE
        googleMap?.setPadding(24, 0, 0, 0)
    }

    /**
     * Constants
     */

    companion object {
        const val RC_SIGN_IN = 1
    }
}
