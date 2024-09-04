@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.example.trackingtest

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.SideEffect
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
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
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
import com.example.trackingtest.ui.screens.SavedRoutesScreen
import com.example.trackingtest.ui.screens.TrackerScreen
import com.example.trackingtest.ui.theme.TrackingTestTheme
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

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

	private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
		val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
		for (service in manager.getRunningServices(Int.MAX_VALUE)) {
			if (serviceClass.name == service.service.className) {
				return true
			}
		}
		return false
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

			var serviceStarted by remember {
				mutableStateOf(isMyServiceRunning(LocationTrackingService::class.java)/*LocationTrackingService().LocalBinder().getService().isRunning()*/)
			}
			Log.d("INTENT HERE", "${intent.flags}\n${intent.action}\n$intent")

			val requestPermissionLauncher = rememberLauncherForActivityResult(
				ActivityResultContracts.RequestPermission()
			) { _ -> }

			SideEffect {
				requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
			}

			val navController = rememberNavController()

			NavHost(navController = navController, startDestination = Routes.TRACKER) {
				composable(route = Routes.TRACKER) {
					TrackerScreen(
						navController,
						liveRouteData,
						serviceStartedValue = serviceStarted,
						serviceStartedSet = { newValue -> serviceStarted = newValue },
					)
				}
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

			val requestPermissionLauncher = rememberLauncherForActivityResult(
				ActivityResultContracts.RequestPermission()
			) { isGranted ->

				if (isGranted) {
					val intent = Intent(context, LocationTrackingService::class.java)
					intent.setAction("start/${dropdownValue.getAccuracyValue()}")
					startForegroundService(context, intent)

					serviceStartedSet(true)
				} else {
					Toast.makeText(
						context,
						"Please, enable notifications in app settings",
						Toast.LENGTH_LONG
					).show()
				}
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

						requestPermissionLauncher.launch(
							android.Manifest.permission.POST_NOTIFICATIONS
						)

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

				val underlineColor = MaterialTheme.colorScheme.primary
				Button(
					enabled = !serviceStartedValue,
					onClick = {
						dropdownState = !dropdownState
					},
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.tertiaryContainer,
						contentColor = MaterialTheme.colorScheme.onSurface,
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
								underlineColor,
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
								color = MaterialTheme.colorScheme.primary,
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
						text = {
							Text(
								DropdownValues.Low.getDisplayName(),
								color = MaterialTheme.colorScheme.inverseSurface,
							)
						},
						onClick = {
							dropdownValue = DropdownValues.Low
							dropdownState = false
						})
					DropdownMenuItem(
						text = {
							Text(
								DropdownValues.Mid.getDisplayName(),
								color = MaterialTheme.colorScheme.inverseSurface,
							)
						},
						onClick = {
							dropdownValue = DropdownValues.Mid
							dropdownState = false
						})
					DropdownMenuItem(
						text = {
							Text(
								DropdownValues.High.getDisplayName(),
								color = MaterialTheme.colorScheme.inverseSurface,
							)
						},
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
