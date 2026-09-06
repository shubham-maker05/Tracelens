package com.geotagcamera.geotagginglocationonphoto.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Menu > Privacy Policy. Placeholder copy — replace with your real, reviewed
 * policy before publishing (it must accurately describe what TraceLens
 * actually collects: device location for the stamp, gallery photos the user
 * explicitly picks, and nothing sent to a server today). App name + author
 * credit sits at the bottom, as asked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Last updated: replace with your publish date.", style = MaterialTheme.typography.labelMedium)

            Section("What we collect") {
                "TraceLens uses your device's location only to stamp it onto photos you take or upload, and reads a photo from your gallery only when you pick one to upload or verify. None of this is uploaded to our servers — everything happens on your device."
            }
            Section("Location data") {
                "Location is fetched fresh each time you capture or upload a photo and is embedded into that photo's stamp and metadata. We do not track your location in the background."
            }
            Section("Photos & storage") {
                "Stamped photos are saved to your device's Pictures/TraceLens folder using Android's standard MediaStore APIs, exactly like any other camera app."
            }
            Section("Ads") {
                "Some editing features are unlocked by watching a short rewarded video ad. The ad network may collect data per its own privacy policy — see the ad network's terms for details."
            }
            Section("Contact") {
                "For questions about this policy, contact the developer via the app listing page."
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 24.dp))
            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("TraceLens", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Made by Shubham", style = MaterialTheme.typography.bodySmall)
                    Text("Built on the open-source GeoTag Camera project (GPLv3).", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, body: () -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(body(), style = MaterialTheme.typography.bodyMedium)
    }
}
