package com.evgenykon.travelguide.data.backup

import com.evgenykon.travelguide.data.prefs.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class SettingsBackupRepository(private val settingsStore: SettingsStore) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun exportJson(): String {
        val settings = settingsStore.settings.first()
        return json.encodeToString(
            SettingsBackupFile.serializer(),
            SettingsBackupFile(
                promptTemplate = settings.promptTemplate,
                model = settings.model,
                voice = settings.voice,
                speed = settings.speed,
                autoPlay = settings.autoPlay,
                radiusMeters = settings.radiusMeters,
                styleUrl = settings.styleUrl
            )
        )
    }

    suspend fun importJson(text: String) {
        val file = json.decodeFromString(SettingsBackupFile.serializer(), text)
        settingsStore.setPromptTemplate(file.promptTemplate)
        settingsStore.setModel(file.model)
        settingsStore.setVoice(file.voice)
        settingsStore.setSpeed(file.speed)
        settingsStore.setAutoPlay(file.autoPlay)
        settingsStore.setRadiusMeters(file.radiusMeters)
        settingsStore.setStyleUrl(file.styleUrl)
    }
}
