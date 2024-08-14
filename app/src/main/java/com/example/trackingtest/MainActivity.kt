@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.widget.ContentLoadingProgressBar
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.RevealState
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.foundation.SwipeToReveal
import androidx.wear.compose.foundation.rememberRevealState
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.SwipeToRevealCard
import com.example.trackingtest.service.LocationTrackingService
import com.example.trackingtest.ui.theme.TrackingTestTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File
import java.util.stream.Stream

//TODO: ask for location permission (x2)
//TODO: ask for foreground permission

class MainActivity : ComponentActivity() {

    abstract class Routes {
        companion object {
            const val tracker = "tracker"
            const val savedRoutes = "savedRoutes"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = Routes.tracker) {
                composable(route = Routes.tracker) { TrackerScreen(navController) }
                composable(
                    route = Routes.savedRoutes,
                    enterTransition = {
                        slideIn(
                            initialOffset = { offset -> IntOffset(offset.width, 0) },
                        )
                    },
                    exitTransition = {
                        slideOut(
                            targetOffset = { offset -> IntOffset(offset.width, 0) },
                        )
                    },
                ) { SavedRoutesScreen(navController) }
            }
        }
    }
}

//TODO: спросить за перекидывание navController

@Composable
fun TrackerScreen(navController: NavController) {
    var text by remember { mutableStateOf("location here") }

    TrackingTestTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {},
                    actions = {
                        Button(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Black,
                            ),
                            onClick = {
                                navController.navigate(MainActivity.Routes.savedRoutes)
                            }) {
                            Icon(
                                ImageBitmap.imageResource(id = R.drawable.history_edu_icon),
                                "",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            LocationFragment(
                text = text,
                setText = { newValue ->
                    text = newValue
                },
                modifier = Modifier.padding(innerPadding),
                navController = navController
            )
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class, ExperimentalWearFoundationApi::class)
@Composable
fun SavedRoutesScreen(navController: NavController) {
    data class TrackingRecord(val name: String)

    TrackingTestTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        Button(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Black,
                            ),
                            onClick = {
                                navController.popBackStack()
                            }) {
                            Icon(
                                ImageBitmap.imageResource(id = R.drawable.back),
                                "",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    },
                )
            },
        )
        { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                val context = LocalContext.current
                var fileList by remember {
                    mutableStateOf(
                        File("${context.filesDir.absolutePath}/saved_routes").listFiles()?.toList()
                    )
                }

                Column {
                    fileList?.map { file ->

                        val coroutineScope = rememberCoroutineScope()
                        val swipeState = rememberRevealState(
                            anchors = mapOf(
                                RevealValue.Covered to 0F,
                                RevealValue.Revealing to 0.4F,
                            )
                        )

                        SwipeToReveal(
                            state = swipeState,
                            primaryAction = {
                                Row {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                swipeState.snapTo(RevealValue.Covered)
                                                fileList = fileList?.minus(file)
                                            }
                                        },
                                        shape = RectangleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Red,
                                        ),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .weight(1f),
                                    ) {
                                        Icon(
                                            bitmap = ImageBitmap.imageResource(R.drawable.delete),
                                            "",
                                        )
                                    }

                                    Button(
                                        onClick = {},
                                        shape = RectangleShape,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .weight(1f),
                                    ) {
                                        Icon(
                                            bitmap = ImageBitmap.imageResource(R.drawable.share),
                                            "",
                                        )
                                    }
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Icon(
                                    bitmap = ImageBitmap.imageResource(id = R.drawable.history_edu_icon),
                                    "",
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(16.dp),
                                )
                                Text(
                                    text = file.name,
                                    modifier = Modifier.padding(8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Divider(
                            thickness = 1.dp,
                            color = Color.LightGray,
                        )
                    }
                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun LocationFragment(
    modifier: Modifier = Modifier,
    text: String,
    setText: (newValue: String) -> (Unit),
    navController: NavController,
) {
    val context = LocalContext.current

    Box {
        Column(
            modifier = modifier
                .padding(8.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Travel Duration:", style = MaterialTheme.typography.titleMedium)
                Text("-")
                Text("Travel Distance:", style = MaterialTheme.typography.titleMedium)
                Text("-")
            }

            var serviceStarted by remember {
                mutableStateOf(false)
            }
            var showDialog by remember {
                mutableStateOf(false)
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredSize(128.dp)
                    .aspectRatio(1f),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (serviceStarted) Color.Red else Color.Green
                ),
                border = BorderStroke(1.dp, Color.Gray),
                onClick = {
                    Log.d("CLICK HERE", "$serviceStarted")
                    if (serviceStarted) {
                        showDialog = true
                    } else {
                        serviceStarted = true
                    }
                }
            ) {
                Text(if (serviceStarted) "Stop" else "Start")
            }

            if (showDialog) AlertDialog(

                modifier = Modifier
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp),
                onDismissRequest = {
                    showDialog = false
                    serviceStarted = false
                },
                content = {
                    Column {
                        Text(
                            "Do you want to save the trip?",
                            modifier = Modifier.padding(16.dp),
                        )
                        Row {
                            Button(onClick = {
                                showDialog = false
                                serviceStarted = false
                                //TODO: save
                            }) {
                                Text("Save")
                            }
                            Box(Modifier.width(8.dp))
                            Button(onClick = {
                                showDialog = false
                                serviceStarted = false
                            }) {
                                Text("Just Stop")
                            }
                            Box(Modifier.width(8.dp))
                            Button(onClick = {
                                showDialog = false
                            }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            )

            Column {
                var dropdownState by remember {
                    mutableStateOf(false)
                }
                var dropdownValue by remember {
                    mutableStateOf("Low")
                }

                Button(
                    onClick = {
                        dropdownState = !dropdownState
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                    ),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    border = BorderStroke(1.dp, Color.Gray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            val strokeWidth = 2 * density
                            val y = size.height - strokeWidth / 2

                            drawContent()
                            drawLine(
                                Color.Blue,
                                Offset(0f, y),
                                Offset(size.width, y),
                                strokeWidth
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            "Accuracy",
                            style = MaterialTheme.typography.titleSmall.merge(
                                fontSize = 10.sp,
                                color = Color.Blue,
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(dropdownValue)
                    }
                }
                DropdownMenu(
                    expanded = dropdownState,
                    onDismissRequest = { dropdownState = !dropdownState }) {
                    DropdownMenuItem(text = { Text("Low") }, onClick = {
                        dropdownValue = "Low"
                        dropdownState = false
                    })
                    DropdownMenuItem(text = { Text("High") }, onClick = {
                        dropdownValue = "High"
                        dropdownState = false
                    })
                }
            }

            Box {}

//            Text(text = text)
//            Row {
//
//
//                Button(
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = Color.Green,
//                    ),
//                    onClick = {
//                        Log.d("CLICK HERE", "")
//
//                        val intent = Intent(context, LocationTrackingService::class.java)
//                        intent.setAction("start")
//                        startForegroundService(context, intent)
//                    }) {
//                    Text("start service")
//                }
//            }
//
//            Row {
//                Button(
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = Color.Red,
//                    ),
//                    onClick = {
//                        Log.d("CLICK HERE 2", "")
//
//                        val intent = Intent(context, LocationTrackingService::class.java)
//                        intent.setAction("stop")
//                        startForegroundService(context, intent)
//                    }) {
//                    Text("stop service")
//                }
//
//                Button(
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = Color.Yellow,
//                    ),
//                    onClick = {
//                        Log.d("CLICK HERE 3", "")
//
//                        val intent = Intent(context, LocationTrackingService::class.java)
//                        intent.setAction("stop_and_save")
//                        startForegroundService(context, intent)
//                    }) {
//                    Text("stop and save")
//                }
//            }
//
//            Button(
//                onClick = {
//                    val intent = Intent(context, LocationTrackingService::class.java)
//                    intent.setAction("accuracy/passive")
//                    startForegroundService(context, intent)
//                }) {
//                Text("accuracy/passive")
//            }
//            Button(
//                onClick = {
//                    val intent = Intent(context, LocationTrackingService::class.java)
//                    intent.setAction("accuracy/network")
//                    startForegroundService(context, intent)
//                }) {
//                Text("accuracy/network")
//            }
//            Button(
//                onClick = {
//                    val intent = Intent(context, LocationTrackingService::class.java)
//                    intent.setAction("accuracy/fused")
//                    startForegroundService(context, intent)
//                }) {
//                Text("accuracy/fused")
//            }
//            Button(
//                onClick = {
//                    val intent = Intent(context, LocationTrackingService::class.java)
//                    intent.setAction("accuracy/gps")
//                    startForegroundService(context, intent)
//                }) {
//                Text("accuracy/gps")
//            }
//
//            Button(
//                modifier = Modifier.padding(top = 32.dp),
//                onClick = {
//                    val intent = Intent(context, LocationTrackingService::class.java)
//                    intent.setAction("interval/1000")
//                    startForegroundService(context, intent)
//                }) {
//                Text("interval/1000")
//            }
//            Button(
//                onClick = {
//                    val intent = Intent(context, LocationTrackingService::class.java)
//                    intent.setAction("interval/3000")
//                    startForegroundService(context, intent)
//                }) {
//                Text("interval/3000")
//            }
//            Button(
//                onClick = {
//                    val intent = Intent(context, LocationTrackingService::class.java)
//                    intent.setAction("interval/5000")
//                    startForegroundService(context, intent)
//                }) {
//                Text("interval/5000")
//            }
//            Button(
//                onClick = {
//                    val intent = Intent(context, LocationTrackingService::class.java)
//                    intent.setAction("interval/10000")
//                    startForegroundService(context, intent)
//                }) {
//                Text("interval/10000")
//            }
        }
    }
}

// TYPE : location

