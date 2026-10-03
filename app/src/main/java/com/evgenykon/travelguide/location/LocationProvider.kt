package com.evgenykon.travelguide.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task

class LocationProvider(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun lastLocation(): Task<Location> = client.lastLocation

    @SuppressLint("MissingPermission")
    fun requestUpdates(
        intervalMs: Long,
        minDistanceMeters: Float,
        callback: LocationCallback
    ): Task<Void> {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .build()
        return client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    fun removeUpdates(callback: LocationCallback) {
        client.removeLocationUpdates(callback)
    }
}
