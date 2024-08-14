@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.trackingtest

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.foundation.SwipeToReveal
import androidx.wear.compose.foundation.rememberRevealState
import com.example.trackingtest.service.LocationTrackingService
import com.example.trackingtest.ui.theme.TrackingTestTheme
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

//TODO: ask for location permission (x2)
//TODO: ask for foreground permission
//TODO: ask for storage permission

class LocationServiceUpdate(val newDistance: String?, val newTime: String?)

enum class DropdownValues {
    Low,
    Mid,
    High;

    fun getDisplayName(): String {
        return when (this) {
            Low -> "Low"
            Mid -> "Medium"
            High -> "High"
        }
    }

    fun getAccuracyValue(): String {
        return when (this) {
            Low -> "low"
            Mid -> "medium"
            High -> "high"
        }
    }
}


class MainActivity : ComponentActivity() {

    private val liveRouteData: MutableLiveData<LocationServiceUpdate> by lazy {
        MutableLiveData<LocationServiceUpdate>()
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: LocationServiceUpdate?) {
        liveRouteData.value = event
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    abstract class Routes {
        companion object {
            const val TRACKER = "tracker"
            const val SAVED_ROUTES = "savedRoutes"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = Routes.TRACKER) {
                composable(route = Routes.TRACKER) { TrackerScreen(navController, liveRouteData) }
                composable(
                    route = Routes.SAVED_ROUTES,
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

@Composable
fun TrackerScreen(
    navController: NavController,
    liveRouteData: MutableLiveData<LocationServiceUpdate>
) {
    var serviceStarted by remember {
        mutableStateOf(false)
    }

    TrackingTestTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Location Tracking") },
                    actions = {
                        Button(
                            enabled = !serviceStarted,
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Black,
                            ),
                            onClick = {
                                navController.navigate(MainActivity.Routes.SAVED_ROUTES)
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
                modifier = Modifier.padding(innerPadding),
                liveRouteData = liveRouteData,
                serviceStartedSet = { newValue -> serviceStarted = newValue },
                serviceStartedValue = serviceStarted,
            )
        }
    }
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun SavedRoutesScreen(navController: NavController) {
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

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
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
                                                file.delete()
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
                                        onClick = {
                                            val resolver = context.contentResolver
                                            val values = ContentValues()

                                            values.put(
                                                MediaStore.MediaColumns.DISPLAY_NAME,
                                                file.name
                                            )
                                            values.put(
                                                MediaStore.MediaColumns.MIME_TYPE,
                                                "application/my-custom-type"
                                            )
                                            values.put(
                                                MediaStore.MediaColumns.RELATIVE_PATH,
                                                Environment.DIRECTORY_DOWNLOADS + "/" + "saved_routes"
                                            )
                                            val uri = resolver.insert(
                                                MediaStore.Files.getContentUri("external"),
                                                values
                                            )

                                            val outputStream = resolver.openOutputStream(uri!!)

                                            outputStream?.write(file.readBytes())
                                            outputStream?.close()

                                            Toast.makeText(
                                                context,
                                                "File saved to \"Downloads/saved_routes/\"",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        },
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
    liveRouteData: MutableLiveData<LocationServiceUpdate>,
    serviceStartedSet: (Boolean) -> Unit,
    serviceStartedValue: Boolean,
) {
    val context = LocalContext.current

    var travelDuration by remember {
        mutableStateOf("-")
    }

    var travelDistance by remember {
        mutableStateOf("-")
    }

    liveRouteData.observe(LocalLifecycleOwner.current) {
        if (it.newTime != null) travelDuration = it.newTime
        if (it.newDistance != null) travelDistance = it.newDistance
    }

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
                Text(travelDuration)
                Text("Travel Distance:", style = MaterialTheme.typography.titleMedium)
                Text(travelDistance)
            }

            var showDialog by remember {
                mutableStateOf(false)
            }

            var dropdownValue by remember {
                mutableStateOf(DropdownValues.Low)
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredSize(128.dp)
                    .aspectRatio(1f),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (serviceStartedValue) Color.Red else Color.Green
                ),
                border = BorderStroke(1.dp, Color.Gray),
                onClick = {
                    Log.d("CLICK HERE", "$serviceStartedValue")
                    if (serviceStartedValue) {
                        showDialog = true
                    } else {
                        val intent = Intent(context, LocationTrackingService::class.java)
                        intent.setAction("start/${dropdownValue.getAccuracyValue()}")
                        startForegroundService(context, intent)

                        serviceStartedSet(true)
                    }
                }
            ) {
                Text(if (serviceStartedValue) "Stop" else "Start")
            }

            if (showDialog) AlertDialog(

                modifier = Modifier
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp),
                onDismissRequest = {
                    showDialog = false
                    serviceStartedSet(false)
                },
                content = {
                    Column {
                        Text(
                            "Do you want to save the trip?",
                            modifier = Modifier.padding(16.dp),
                        )
                        Row {
                            Button(onClick = {
                                val intent = Intent(context, LocationTrackingService::class.java)
                                intent.setAction("stop_and_save")
                                startForegroundService(context, intent)

                                showDialog = false
                                serviceStartedSet(false)
                                travelDuration = "-"
                                travelDistance = "-"
                            }) {
                                Text("Save")
                            }
                            Box(Modifier.width(8.dp))
                            Button(onClick = {
                                val intent = Intent(context, LocationTrackingService::class.java)
                                intent.setAction("stop")
                                startForegroundService(context, intent)

                                showDialog = false
                                serviceStartedSet(false)
                                travelDuration = "-"
                                travelDistance = "-"
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

                Button(
                    enabled = !serviceStartedValue,
                    onClick = {
                        dropdownState = !dropdownState
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Black,
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

                        Text(dropdownValue.getDisplayName())
                    }
                }

                DropdownMenu(
                    expanded = dropdownState,
                    onDismissRequest = { dropdownState = !dropdownState }) {
                    DropdownMenuItem(
                        text = { Text(DropdownValues.Low.getDisplayName()) },
                        onClick = {
                            dropdownValue = DropdownValues.Low
                            dropdownState = false
                        })
                    DropdownMenuItem(
                        text = { Text(DropdownValues.Mid.getDisplayName()) },
                        onClick = {
                            dropdownValue = DropdownValues.Mid
                            dropdownState = false
                        })
                    DropdownMenuItem(
                        text = { Text(DropdownValues.High.getDisplayName()) },
                        onClick = {
                            dropdownValue = DropdownValues.High
                            dropdownState = false
                        })
                }
            }

            Box {} // bottom "padding"
        }
    }
}
