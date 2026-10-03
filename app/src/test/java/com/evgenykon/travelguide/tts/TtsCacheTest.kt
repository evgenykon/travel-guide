package com.evgenykon.travelguide.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TtsCacheTest {

    @Test
    fun keyIsDeterministic() {
        assertEquals(
            TtsCache.key("Привет", "alena", 1.0),
            TtsCache.key("Привет", "alena", 1.0)
        )
    }

    @Test
    fun keyDependsOnAllInputs() {
        val base = TtsCache.key("Привет", "alena", 1.0)
        assertNotEquals(base, TtsCache.key("Пока", "alena", 1.0))
        assertNotEquals(base, TtsCache.key("Привет", "filipp", 1.0))
        assertNotEquals(base, TtsCache.key("Привет", "alena", 1.2))
    }

    @Test
    fun fileForProducesMp3InCacheDir() {
        val cache = TtsCache(File("/tmp/tts-test"))
        val file = cache.fileFor("текст", "alena", 1.0)
        assertEquals("/tmp/tts-test", file.parentFile?.path)
        assertTrue(file.name.endsWith(".mp3"))
        assertEquals(64 + 4, file.name.length)
    }
}
