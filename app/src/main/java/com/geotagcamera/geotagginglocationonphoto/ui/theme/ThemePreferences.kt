package com.geotagcamera.geotagginglocationonphoto.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

/** User-chosen theme: SYSTEM follows the device setting, LIGHT/DARK force one. */
enum class AppThemeMode { SYSTEM, LIGHT, DARK }

/** Persists the Settings > "Dark theme" toggle across app restarts. */
class ThemePreferences(private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<AppThemeMode> = context.themeDataStore.data.map { prefs ->
        runCatching { AppThemeMode.valueOf(prefs[Keys.MODE] ?: AppThemeMode.SYSTEM.name) }
            .getOrDefault(AppThemeMode.SYSTEM)
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.themeDataStore.edit { prefs -> prefs[Keys.MODE] = mode.name }
    }
}
