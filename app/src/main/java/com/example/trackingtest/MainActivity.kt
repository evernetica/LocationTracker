package com.example.trackingtest

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startForegroundService
import com.example.trackingtest.service.LocationTrackingService
import com.example.trackingtest.ui.theme.TrackingTestTheme
import java.util.stream.Stream

//TODO: ask for location permission (x2)
//TODO: ask for foreground permission

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var text by remember { mutableStateOf("location here") }

            TrackingTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LocationFragment(
                        text = text,
                        setText = { newValue ->
                            text = newValue
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun LocationFragment(
    modifier: Modifier = Modifier,
    text: String,
    setText: (newValue: String) -> (Unit)
) {
    val context = LocalContext.current

    Box {
        Column(modifier = modifier.verticalScroll(rememberScrollState())) {
            Text(text = text)
            Row {


                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Green,
                    ),
                    onClick = {
                        Log.d("CLICK HERE", "")

                        val intent = Intent(context, LocationTrackingService::class.java)
                        intent.setAction("start")
                        startForegroundService(context, intent)
                    }) {
                    Text("start service")
                }
            }

            Row {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                    ),
                    onClick = {
                        Log.d("CLICK HERE 2", "")

                        val intent = Intent(context, LocationTrackingService::class.java)
                        intent.setAction("stop")
                        startForegroundService(context, intent)
                    }) {
                    Text("stop service")
                }

                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Yellow,
                    ),
                    onClick = {
                        Log.d("CLICK HERE 3", "")

                        val intent = Intent(context, LocationTrackingService::class.java)
                        intent.setAction("stop_and_save")
                        startForegroundService(context, intent)
                    }) {
                    Text("stop and save")
                }
            }

            Button(
                onClick = {
                    val intent = Intent(context, LocationTrackingService::class.java)
                    intent.setAction("accuracy/passive")
                    startForegroundService(context, intent)
                }) {
                Text("accuracy/passive")
            }
            Button(
                onClick = {
                    val intent = Intent(context, LocationTrackingService::class.java)
                    intent.setAction("accuracy/network")
                    startForegroundService(context, intent)
                }) {
                Text("accuracy/network")
            }
            Button(
                onClick = {
                    val intent = Intent(context, LocationTrackingService::class.java)
                    intent.setAction("accuracy/fused")
                    startForegroundService(context, intent)
                }) {
                Text("accuracy/fused")
            }
            Button(
                onClick = {
                    val intent = Intent(context, LocationTrackingService::class.java)
                    intent.setAction("accuracy/gps")
                    startForegroundService(context, intent)
                }) {
                Text("accuracy/gps")
            }

            Button(
                modifier = Modifier.padding(top = 32.dp),
                onClick = {
                    val intent = Intent(context, LocationTrackingService::class.java)
                    intent.setAction("interval/1000")
                    startForegroundService(context, intent)
                }) {
                Text("interval/1000")
            }
            Button(
                onClick = {
                    val intent = Intent(context, LocationTrackingService::class.java)
                    intent.setAction("interval/3000")
                    startForegroundService(context, intent)
                }) {
                Text("interval/3000")
            }
            Button(
                onClick = {
                    val intent = Intent(context, LocationTrackingService::class.java)
                    intent.setAction("interval/5000")
                    startForegroundService(context, intent)
                }) {
                Text("interval/5000")
            }
            Button(
                onClick = {
                    val intent = Intent(context, LocationTrackingService::class.java)
                    intent.setAction("interval/10000")
                    startForegroundService(context, intent)
                }) {
                Text("interval/10000")
            }
        }
    }
}

// TYPE : location

