package com.kerimovscreations.naturun.services

import com.google.android.gms.maps.model.LatLng
import io.reactivex.subjects.BehaviorSubject

object DriverService {
    var driverCoordinateBSubject = BehaviorSubject.createDefault(LatLng(0.0, 0.0))
}