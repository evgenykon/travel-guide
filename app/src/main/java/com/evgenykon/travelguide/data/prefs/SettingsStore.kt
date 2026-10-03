package com.evgenykon.travelguide.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
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
    val routeFilterId: Long? = null,
    val promptTemplate: String = DEFAULT_PROMPT_TEMPLATE,
    val xiaomiAutostartConfirmed: Boolean = false,
    val xiaomiBatteryConfirmed: Boolean = false,
    val lastLat: Double? = null,
    val lastLng: Double? = null
) {
    companion object {
        const val DEFAULT_MODEL = "openai/gpt-4o-mini"
        const val DEFAULT_VOICE = "alena"
        const val NO_ROUTE_FILTER = -1L

        val DEFAULT_PROMPT_TEMPLATE = """
            Составь описание конкретной точки на карте для путешественника.
            Название: {name}
            Адрес: {address}
            Город, страна: {location}
            Объект на карте (OSM): {object}
            Заметка пользователя: {hint}

            Структура ответа — сплошным текстом, на русском, без заголовков, нумерации и списков:
            1) Основная часть: что это за место и его описание — 2 предложения.
            2) Историческая значимость — 1 предложение. Если есть достоверные исторические данные (история создания, связанные события) — добавь ещё 4–6 предложений.
            3) Если с местом документально связаны известные личности — добавь 2 предложения: имя, чем человек известен и что именно он делал в этом месте. Если подтверждённых сведений нет — личности не упоминай.

            Требования:
            — используй только достоверные, проверяемые факты;
            — строго запрещены домыслы и предположения: формулировки «возможно», «вероятно», «могли бы», «считается, что» недопустимы;
            — не выдумывай имена, даты, события и связи между людьми и местом;
            — если достоверных данных нет — пропусти соответствующий пункт, а не додумывай;
            — в ответе не указывай координаты, адрес, широту/долготу и другие технические данные: текст озвучивается вслух и должен звучать как рассказ экскурсовода;
            — учитывай, что объект находится в {city}, {country}: не путай его с одноимёнными местами в других городах и странах;
            — избегай общих фраз и описаний «в целом» — пиши именно об этой точке;
            — упоминай только подтверждённых авторов и создателей объекта.
        """.trimIndent()
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
            routeFilterId = routeFilter.takeIf { it != AppSettings.NO_ROUTE_FILTER },
            promptTemplate = prefs[KEY_PROMPT] ?: AppSettings.DEFAULT_PROMPT_TEMPLATE,
            xiaomiAutostartConfirmed = prefs[KEY_XIAOMI_AUTOSTART] ?: false,
            xiaomiBatteryConfirmed = prefs[KEY_XIAOMI_BATTERY] ?: false,
            lastLat = prefs[KEY_LAST_LAT],
            lastLng = prefs[KEY_LAST_LNG]
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
    suspend fun setPromptTemplate(template: String) = edit { it[KEY_PROMPT] = template }
    suspend fun resetPromptTemplate() = edit { it[KEY_PROMPT] = AppSettings.DEFAULT_PROMPT_TEMPLATE }
    suspend fun setXiaomiAutostartConfirmed(value: Boolean) = edit { it[KEY_XIAOMI_AUTOSTART] = value }
    suspend fun setXiaomiBatteryConfirmed(value: Boolean) = edit { it[KEY_XIAOMI_BATTERY] = value }
    suspend fun setLastLocation(lat: Double, lng: Double) = edit {
        it[KEY_LAST_LAT] = lat
        it[KEY_LAST_LNG] = lng
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
        val KEY_PROMPT = stringPreferencesKey("prompt_template")
        val KEY_XIAOMI_AUTOSTART = booleanPreferencesKey("xiaomi_autostart")
        val KEY_XIAOMI_BATTERY = booleanPreferencesKey("xiaomi_battery")
        val KEY_LAST_LAT = doublePreferencesKey("last_lat")
        val KEY_LAST_LNG = doublePreferencesKey("last_lng")
    }
}
