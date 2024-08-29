package com.example.trackingtest.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import com.example.trackingtest.LocationFragment
import com.example.trackingtest.LocationServiceUpdate
import com.example.trackingtest.MainActivity
import com.example.trackingtest.R
import com.example.trackingtest.ui.theme.TrackingTestTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
	navController: NavController,
	liveRouteData: MutableLiveData<LocationServiceUpdate>,
	serviceStartedValue: Boolean,
	serviceStartedSet: (Boolean) -> Unit,
) {
	TrackingTestTheme {
		Scaffold(
			modifier = Modifier.fillMaxSize(),
			topBar = {
				TopAppBar(
					title = { Text("Location Tracking") },
					actions = {
						Button(
							enabled = !serviceStartedValue,
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
				serviceStartedSet = serviceStartedSet,
				serviceStartedValue = serviceStartedValue,
			)
		}
	}
}