package com.geotagcamera.geotagginglocationonphoto.ads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * "Modify date/time/day/location" is locked (design ask: monetize the one
 * editing action people would otherwise use to falsify a stamp — everything
 * else in Settings stays free). Tapping the locked row calls [onRequestUnlock];
 * this composable is the dialog that runs while the ad plays and calls
 * [onUnlocked] once [adManager] rewards.
 */
@Composable
fun RewardedUnlockDialog(
    adManager: RewardedAdManager,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isPlaying) onDismiss() },
        title = { Text(if (isPlaying) "Playing ad…" else "Unlock editing") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (isPlaying) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 12.dp))
                    Text("Please wait for the ad to finish to unlock date, time, day & location editing.")
                } else {
                    Text("Watch a short ad to unlock editing the date, time, day and location on this stamp.")
                }
            }
        },
        confirmButton = {
            if (!isPlaying) {
                Button(onClick = {
                    isPlaying = true
                    scope.launch {
                        val rewarded = adManager.showRewardedAd()
                        isPlaying = false
                        if (rewarded) onUnlocked() else onDismiss()
                    }
                }) { Text("Watch ad") }
            }
        },
        dismissButton = {
            if (!isPlaying) Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
