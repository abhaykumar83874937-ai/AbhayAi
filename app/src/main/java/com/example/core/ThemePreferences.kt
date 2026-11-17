package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studyDataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_study_prefs")

enum class ThemeMode(val label: String) {
    SYSTEM("Auto"),
    LIGHT("Light"),
    DARK("Dark")
}

class ThemePreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val guestKey = booleanPreferencesKey("guest_mode_enabled")

    val themeMode: Flow<ThemeMode> = context.studyDataStore.data.map { prefs ->
        when (prefs[themeKey]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val isGuestMode: Flow<Boolean> = context.studyDataStore.data.map { prefs ->
        prefs[guestKey] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.studyDataStore.edit { prefs ->
            prefs[themeKey] = mode.name
        }
    }

    suspend fun setGuestMode(enabled: Boolean) {
        context.studyDataStore.edit { prefs ->
            prefs[guestKey] = enabled
        }
    }
}
