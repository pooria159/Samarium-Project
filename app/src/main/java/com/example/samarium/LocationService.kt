package com.example.samarium

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager

class LocationService(context: Context) {
    private var locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun startListening(locationListener: LocationListener) {
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
    }
}
