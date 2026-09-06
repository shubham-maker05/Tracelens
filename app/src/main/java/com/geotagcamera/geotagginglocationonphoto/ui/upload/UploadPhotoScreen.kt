package com.geotagcamera.geotagginglocationonphoto.ui.upload

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geotagcamera.geotagginglocationonphoto.ui.common.rememberPhotoPicker
import com.geotagcamera.geotagginglocationonphoto.ui.theme.GlassPanel
import com.geotagcamera.geotagginglocationonphoto.ui.theme.PrismBackdrop

/**
 * Menu > "Upload Photo": pick any existing image, TraceLens fetches the
 * phone's current location automatically and burns a geotag stamp onto it —
 * same stamp style, same signing, as a fresh capture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadPhotoScreen(onBack: () -> Unit) {
    val viewModel: UploadPhotoViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val pickImage = rememberPhotoPicker { uri -> viewModel.processPickedImage(uri) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Photo") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrismBackdrop(dark = false)
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                GlassPanel(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (val s = state) {
                            is UploadState.Idle -> {
                                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.padding(8.dp))
                                Text("Pick a photo from your gallery — TraceLens will stamp it with your current location automatically.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Button(onClick = pickImage) { Text("Choose photo") }
                            }
                            is UploadState.LocatingAndStamping -> {
                                CircularProgressIndicator()
                                Text("Fetching location & stamping…")
                            }
                            is UploadState.Done -> {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                Text("Saved to Pictures/TraceLens.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Button(onClick = { viewModel.reset() }) { Text("Upload another") }
                            }
                            is UploadState.Error -> {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(s.message, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Button(onClick = { viewModel.reset() }) { Text("Try again") }
                            }
                        }
                    }
                }
            }
        }
    }
}
