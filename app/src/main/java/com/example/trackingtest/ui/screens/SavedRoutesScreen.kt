package com.example.trackingtest.ui.screens

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.foundation.SwipeToReveal
import androidx.wear.compose.foundation.rememberRevealState
import com.example.trackingtest.R
import com.example.trackingtest.ui.theme.TrackingTestTheme
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalWearFoundationApi::class, ExperimentalMaterial3Api::class)
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
								tint = MaterialTheme.colorScheme.inverseSurface,
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
											tint = MaterialTheme.colorScheme.surface,
										)
									}

									val requestStoragePermissionLauncher =
										rememberLauncherForActivityResult(
											ActivityResultContracts.RequestPermission()
										) { isGranted ->

											if (isGranted) {
												val downloadsDir =
													Environment.getExternalStoragePublicDirectory(
														Environment.DIRECTORY_DOWNLOADS
													)
												val targetDir =
													File(downloadsDir.absolutePath + "/saved_routes/")
												if (!targetDir.exists()) targetDir.mkdirs()

												val targetFile =
													File(targetDir.absolutePath + "/" + file.name)
												targetFile.createNewFile()
												targetFile.writeBytes(file.readBytes())

												Toast.makeText(
													context,
													"File saved to \"Downloads/saved_routes/\"",
													Toast.LENGTH_LONG,
												).show()
											} else {
												Toast.makeText(
													context,
													"Please, grant storage permission in app settings",
													Toast.LENGTH_LONG
												).show()
											}
										}

									Button(
										onClick = {
											if (Build.VERSION.SDK_INT <= 28) {
												requestStoragePermissionLauncher.launch(
													android.Manifest.permission.WRITE_EXTERNAL_STORAGE
												)
											} else {
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
											}
										},
										shape = RectangleShape,
										modifier = Modifier
											.fillMaxSize()
											.weight(1f),
									) {
										Icon(
											bitmap = ImageBitmap.imageResource(R.drawable.share),
											"",
											tint = MaterialTheme.colorScheme.surface,
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