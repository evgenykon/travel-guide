package com.evgenykon.travelguide.tts

import java.io.File
import java.security.MessageDigest

class TtsCache(private val dir: File) {

    fun fileFor(text: String, voice: String, speed: Double): File =
        File(dir, "${key(text, voice, speed)}.mp3")

    fun sizeBytes(): Long =
        dir.listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L

    fun clear(): Int {
        val files = dir.listFiles() ?: return 0
        var deleted = 0
        files.forEach { if (it.isFile && it.delete()) deleted++ }
        return deleted
    }

    companion object {
        fun key(text: String, voice: String, speed: Double): String =
            sha256("$voice|$speed|mp3|$text")

        private fun sha256(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
