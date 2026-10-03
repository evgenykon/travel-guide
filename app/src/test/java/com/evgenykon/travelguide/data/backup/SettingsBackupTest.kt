package com.evgenykon.travelguide.data.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupTest {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test
    fun serializationRoundTrip() {
        val file = SettingsBackupFile(
            promptTemplate = "Мой промпт {name}",
            model = "openai/gpt-4o-mini",
            voice = "filipp",
            speed = 1.2f,
            autoPlay = false,
            radiusMeters = 25f,
            styleUrl = "https://example.com/style.json"
        )

        val text = json.encodeToString(SettingsBackupFile.serializer(), file)
        val restored = json.decodeFromString(SettingsBackupFile.serializer(), text)

        assertEquals(file.copy(exportedAt = restored.exportedAt), restored)
        assertTrue(text.contains("\"promptTemplate\""))
        assertTrue(text.contains("\"model\""))
    }

    @Test
    fun ignoresUnknownFieldsFromNewerVersions() {
        val text = """
            {
              "version": 2,
              "promptTemplate": "p",
              "model": "m",
              "voice": "alena",
              "speed": 1.0,
              "autoPlay": true,
              "radiusMeters": 10.0,
              "styleUrl": "",
              "futureField": "ignored"
            }
        """.trimIndent()

        val restored = json.decodeFromString(SettingsBackupFile.serializer(), text)

        assertEquals("p", restored.promptTemplate)
        assertEquals("m", restored.model)
    }
}
