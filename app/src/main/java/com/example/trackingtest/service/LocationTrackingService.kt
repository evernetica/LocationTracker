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
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.trackingtest.LocationServiceUpdate
import com.example.trackingtest.notification.NotificationsHelper
import me.bvn13.sdk.android.gpx.GpxType
import me.bvn13.sdk.android.gpx.MetadataType
import me.bvn13.sdk.android.gpx.WptType
import me.bvn13.sdk.android.gpx.toXmlString
import org.greenrobot.eventbus.EventBus
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.time.Instant
import java.util.Timer
import kotlin.concurrent.timer
import kotlin.io.path.Path
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class LocationTrackingService() : Service() {
    private val binder = LocalBinder()

    private var durationTimer: Timer? = null


    private val locationManager by lazy {
        ContextCompat.getSystemService(
            this.baseContext,
            LocationManager::class.java
        ) as LocationManager
    }
    private var listener = MyLocationListener(
        mutableListOf(),
        "",
        0,
        mutableFloatStateOf(0F),
        mutableStateOf(null),
        isServiceRunning = { isRunning() },
    )

    private var selectedAccuracy = "default"
    private var interval = 1000

    private val locationHistory = mutableListOf<Location>()
    private var totalDistance = mutableFloatStateOf(0F)
    private var lastLocation = mutableStateOf<Location?>(null)

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    private fun isRunning(): Boolean {
        return durationTimer != null
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    class MyLocationListener(
        private val locationHistory: MutableList<Location>,
        private val selectedAccuracy: String,
        private val interval: Int,
        private var mutableDistance: MutableFloatState,
        private var mutableLastLocation: MutableState<Location?>,
        private val isServiceRunning: () -> Boolean,
    ) : LocationListener {
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
                        newDistance = "${"%.2f".format(mutableDistance.floatValue / 1000)} km",
                        newTime = null,
                    )
                )
            }
            mutableLastLocation.value = location

            locationHistory.add(location)
        }
    }

    @SuppressLint("MissingPermission") // TODO: fix
    private fun startTracking() {
        listener = MyLocationListener(
            locationHistory,
            selectedAccuracy,
            interval,
            totalDistance,
            lastLocation,
            isServiceRunning = { isRunning() }
        )
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

        var timePassed = 0
        durationTimer = timer(
            initialDelay = 0,
            period = interval.toLong(),
        ) {
            timePassed += interval

            EventBus.getDefault()
                .post(
                    LocationServiceUpdate(
                        newDistance = null,
                        newTime = "${timePassed.toDuration(DurationUnit.MILLISECONDS)}",
                    ),
                )
        }
    }

    private fun stopTracking() {
        locationManager.removeUpdates(listener)
        durationTimer?.cancel()
    }

    @SuppressLint("MissingPermission") // TODO: fix
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("START COMMAND HERE", "")

        intent?.action?.let {
            when {
                it.startsWith("start") -> {
                    when (it.split("/").last()) {
                        "low" -> selectedAccuracy = "passive"
                        "mid" -> selectedAccuracy = "network"
                        "high" -> selectedAccuracy =
                            if (locationManager.isProviderEnabled("gps")) "gps" else "fused"
                    }

                    startAsForegroundService()

                    startTracking()
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


                    if (!File("${baseContext.filesDir.absolutePath}/saved_routes").exists()) {
                        Files.createDirectory(Path("${baseContext.filesDir.absolutePath}/saved_routes"))
                    }

                    val file = File(
                        "${baseContext.filesDir.absolutePath}/saved_routes",
                        "tracking_data_${Instant.now()}.gpx"
                    )
                    val fw = FileWriter(file.absoluteFile)
                    val bw = BufferedWriter(fw)
                    bw.write(gpxType)
                    bw.close()

                    Log.d(
                        "HERE NEW FILE SAVED",
                        "current file list:${
                            File("${baseContext.filesDir.absolutePath}/saved_routes").listFiles()
                                ?.map { "\n" + it.name }
                        }"
                    )

                    stopForegroundService()
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