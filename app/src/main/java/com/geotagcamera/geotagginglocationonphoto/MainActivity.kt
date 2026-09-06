package com.geotagcamera.geotagginglocationonphoto

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geotagcamera.geotagginglocationonphoto.ui.nav.GeoTagCameraApp
import com.geotagcamera.geotagginglocationonphoto.ui.theme.AppThemeMode
import com.geotagcamera.geotagginglocationonphoto.ui.theme.GeoTagCameraTheme
import com.geotagcamera.geotagginglocationonphoto.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {

    // Held above the nav graph so a "verify this photo" share can be consumed exactly once.
    private var shareUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shareUri = extractSharedImageUri(intent)
        val themeMode = ThemePreferences(applicationContext).themeMode
            .stateIn(lifecycleScope, SharingStarted.Eagerly, AppThemeMode.SYSTEM)
        setContent {
            val mode by themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (mode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
            }
            GeoTagCameraTheme(darkTheme = darkTheme) {
                GeoTagCameraApp(
                    shareUri = shareUri,
                    onShareConsumed = { shareUri = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shareUri = extractSharedImageUri(intent)
    }

    private fun extractSharedImageUri(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type?.startsWith("image/") != true) return null
        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        return uri?.toString()
    }
}
