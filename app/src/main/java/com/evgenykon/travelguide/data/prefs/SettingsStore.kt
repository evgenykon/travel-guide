package com.evgenykon.travelguide.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val model: String = DEFAULT_MODEL,
    val voice: String = DEFAULT_VOICE,
    val speed: Float = 1.0f,
    val autoPlay: Boolean = true,
    val radiusMeters: Float = 10f,
    val styleUrl: String = "",
    val trackingEnabled: Boolean = false,
    val routeFilterId: Long? = null
) {
    companion object {
        const val DEFAULT_MODEL = "openai/gpt-4o-mini"
        const val DEFAULT_VOICE = "alena"
        const val NO_ROUTE_FILTER = -1L
    }
}

class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val routeFilter = prefs[KEY_ROUTE_FILTER] ?: AppSettings.NO_ROUTE_FILTER
        AppSettings(
            model = prefs[KEY_MODEL] ?: AppSettings.DEFAULT_MODEL,
            voice = prefs[KEY_VOICE] ?: AppSettings.DEFAULT_VOICE,
            speed = prefs[KEY_SPEED] ?: 1.0f,
            autoPlay = prefs[KEY_AUTOPLAY] ?: true,
            radiusMeters = prefs[KEY_RADIUS] ?: 10f,
            styleUrl = prefs[KEY_STYLE_URL] ?: "",
            trackingEnabled = prefs[KEY_TRACKING] ?: false,
            routeFilterId = routeFilter.takeIf { it != AppSettings.NO_ROUTE_FILTER }
        )
    }

    suspend fun setModel(model: String) = edit { it[KEY_MODEL] = model }
    suspend fun setVoice(voice: String) = edit { it[KEY_VOICE] = voice }
    suspend fun setSpeed(speed: Float) = edit { it[KEY_SPEED] = speed }
    suspend fun setAutoPlay(enabled: Boolean) = edit { it[KEY_AUTOPLAY] = enabled }
    suspend fun setRadiusMeters(radius: Float) = edit { it[KEY_RADIUS] = radius }
    suspend fun setStyleUrl(url: String) = edit { it[KEY_STYLE_URL] = url }
    suspend fun setTrackingEnabled(enabled: Boolean) = edit { it[KEY_TRACKING] = enabled }
    suspend fun setRouteFilterId(id: Long?) = edit {
        it[KEY_ROUTE_FILTER] = id ?: AppSettings.NO_ROUTE_FILTER
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private companion object {
        val KEY_MODEL = stringPreferencesKey("model")
        val KEY_VOICE = stringPreferencesKey("voice")
        val KEY_SPEED = floatPreferencesKey("speed")
        val KEY_AUTOPLAY = booleanPreferencesKey("autoplay")
        val KEY_RADIUS = floatPreferencesKey("radius")
        val KEY_STYLE_URL = stringPreferencesKey("style_url")
        val KEY_TRACKING = booleanPreferencesKey("tracking")
        val KEY_ROUTE_FILTER = longPreferencesKey("route_filter")
    }
}
