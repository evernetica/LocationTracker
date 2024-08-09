package com.example.trackingtest.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.trackingtest.notification.NotificationsHelper
import me.bvn13.sdk.android.gpx.GpxType
import me.bvn13.sdk.android.gpx.GpxWriter
import me.bvn13.sdk.android.gpx.MetadataType
import me.bvn13.sdk.android.gpx.WptType
import me.bvn13.sdk.android.gpx.toXmlString


class LocationTrackingService() : Service() {
    private val binder = LocalBinder()

    private val locationManager by lazy {
        ContextCompat.getSystemService(
            this.baseContext,
            LocationManager::class.java
        ) as LocationManager
    }
    private var listener = MyLocationListener(mutableListOf(), "", 0)

    private var selectedAccuracy = "default"
    private var interval = 1000

    private val locationHistory = mutableListOf<Location>()

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    class MyLocationListener(
        private val locationHistory: MutableList<Location>,
        private val selectedAccuracy: String,
        private val interval: Int,
    ) : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d(
                "STILL RUNNING HERE",
                "accuracy: $selectedAccuracy, interval: $interval\nlocation: ${location.latitude} / ${location.longitude}"
            )

            locationHistory.add(location)
        }
    }

    @SuppressLint("MissingPermission") // TODO: fix
    private fun startTracking() {
        listener = MyLocationListener(locationHistory, selectedAccuracy, interval)
        locationManager.requestLocationUpdates(
            if (locationManager.allProviders.contains(selectedAccuracy)) {
                selectedAccuracy
            } else {
                locationManager.allProviders.first()
            },
            interval.toLong(),
            0F,
            listener,
        )
    }

    private fun stopTracking() {
        locationManager.removeUpdates(listener)
    }

    @SuppressLint("MissingPermission") // TODO: fix
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("START COMMAND HERE", "")

        intent?.action?.let {
            when {
                it == "start" -> {
                    startAsForegroundService()

                    startTracking()

//                    mainHandler.post(object : Runnable {
//                        @SuppressLint("MissingPermission") // TODO: fix
//                        override fun run() {
//                            if (!isServiceRunning) return
////
////                            var text = "Location data [$selectedAccuracy]:"
////
////                            for (a in locationManager.allProviders) {
////                                val location = locationManager.getLastKnownLocation(a)
////                                text += "\n${a}: ${location?.latitude} : ${location?.longitude}"
////                            }
//
//                            val location = locationManager.getLastKnownLocation(
//                                if (locationManager.allProviders.contains(selectedAccuracy)) {
//                                    selectedAccuracy
//                                } else {
//                                    locationManager.allProviders.first()
//                                }
//                            )
//                            Log.d(
//                                "STILL RUNNING HERE",
//                                "accuracy: $selectedAccuracy, interval: $interval\nlocation: ${location?.latitude} / ${location?.longitude}"
//                            )
//                            if (location != null) locationHistory.add(location)
//
//                            mainHandler.postDelayed(this, interval.toLong())
//                        }
//                    })
                }

                it == "stop" -> {
                    stopForegroundService()
                }

                it == "stop_and_save" -> {

                    var text = "Location history:"
                    for (i in 0..<locationHistory.size) {
                        text += "\n$i [${locationHistory[i].time}]: ${locationHistory[i].latitude} / ${locationHistory[i].longitude}"
                    }
                    Log.d("HISTORY TO BE SAVED HERE", text)


                    val gpxType = GpxType(
                        metadata = MetadataType(
                            name = "test file name",
                            description = "test file description",
                            authorName = "test file author name",
                        ),
                        creator = "test file creator name",
                        wpt = locationHistory.map { loc ->
                            WptType(
                                lat = loc.latitude,
                                lon = loc.longitude,
                            )
                        }
                    ).toXmlString()


                    Log.d("GPX FILE HERE", "$gpxType")

                    stopForegroundService()
                }

                it.startsWith("accuracy") -> {
                    stopTracking()
                    selectedAccuracy = it.split("/").last()
                    startTracking()
                }

                it.startsWith("interval") -> {
                    stopTracking()
                    interval = it.split("/").last().toIntOrNull() ?: 1000
                    startTracking()
                }

                else -> Unit
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startAsForegroundService() {
        // create the notification channel
        NotificationsHelper.createNotificationChannel(this)

        // promote service to foreground service
        ServiceCompat.startForeground(
            this,
            1,
            NotificationsHelper.buildNotification(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
        )
    }

    private fun stopForegroundService() {
        stopTracking()
        stopSelf()
    }
}