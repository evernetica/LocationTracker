package com.example.trackingtest.service

import android.location.Location
import android.location.LocationListener
import android.util.Log
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import com.example.trackingtest.LocationServiceUpdate
import org.greenrobot.eventbus.EventBus

class MyLocationListener(
    private val locationHistory: MutableList<Location>,
    private val selectedAccuracy: String,
    private val interval: Int,
    private var mutableDistance: MutableFloatState,
    private var mutableLastLocation: MutableState<Location?>,
    private val isServiceRunning: () -> Boolean,
) : LocationListener {
    fun formatDistanceForDisplay(distance: Float) : String {
        return "%.2f".format(distance / 1000)
    }

    override fun onLocationChanged(location: Location) {
        if (!isServiceRunning()) return

        Log.d(
            "STILL RUNNING HERE",
            "accuracy: $selectedAccuracy, interval: $interval\nlocation: ${location.latitude} / ${location.longitude}"
        )

        if (mutableLastLocation.value != null) {
            mutableDistance.floatValue += location.distanceTo(mutableLastLocation.value!!)
            EventBus.getDefault().post(
                LocationServiceUpdate(
                    newDistance = "${formatDistanceForDisplay(mutableDistance.floatValue)} km [${locationHistory.size + 1} waypoint(s)]",
                    newTime = null,
                )
            )
        } else {
            EventBus.getDefault().post(
                LocationServiceUpdate(
                    newDistance = "0.00 km [1 waypoint(s)]",
                    newTime = null,
                )
            )
        }
        mutableLastLocation.value = location

        locationHistory.add(location)
    }
}