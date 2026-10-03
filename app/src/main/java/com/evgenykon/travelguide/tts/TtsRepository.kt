package com.evgenykon.travelguide.tts

import android.content.Context
import com.evgenykon.travelguide.auth.YandexAuthManager
import com.evgenykon.travelguide.network.VoiceDto
import com.evgenykon.travelguide.network.YandexTtsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.File
import java.util.Locale

class TtsRepository(
    context: Context,
    private val auth: YandexAuthManager,
    private val api: YandexTtsApi,
    private val player: AudioPlayer
) {

    private val cache = TtsCache(File(context.filesDir, "tts"))

    fun cacheSizeBytes(): Long = cache.sizeBytes()

    fun clearCache(): Int = cache.clear()

    suspend fun listVoices(): List<VoiceDto> = runCatching {
        val token = auth.getIamToken()
        val voices = api.voices("Bearer $token").voices
        val russian = voices.filter { voice ->
            voice.languages.isEmpty() || voice.languages.any { it.startsWith("ru") }
        }
        russian.ifEmpty { FALLBACK_VOICES }
    }.getOrElse { FALLBACK_VOICES }

    suspend fun ensureAudio(text: String, voice: String, speed: Double): File =
        withContext(Dispatchers.IO) {
            val target = cache.fileFor(text, voice, speed)
            if (target.exists() && target.length() > 0) return@withContext target

            val body = synthesizeWithRetry(text, voice, speed)
            val bytes = body.use { it.bytes() }
            require(bytes.isNotEmpty()) { "Пустой аудиоответ от Yandex SpeechKit" }

            target.parentFile?.mkdirs()
            val tmp = File(target.parentFile, target.name + ".tmp")
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            target
        }

    suspend fun speak(text: String, voice: String, speed: Double): Result<Unit> = runCatching {
        require(text.isNotBlank()) { "Описание пустое" }
        val file = ensureAudio(text, voice, speed)
        withContext(Dispatchers.Main) { player.play(file) }
    }

    fun stopPlayback() = player.stop()

    private suspend fun synthesizeWithRetry(
        text: String,
        voice: String,
        speed: Double
    ): ResponseBody {
        val token = auth.getIamToken()
        return try {
            api.synthesize("Bearer $token", text = text, voice = voice, speed = formatSpeed(speed))
        } catch (e: HttpException) {
            if (e.code() == 401) {
                val fresh = auth.getIamToken(forceRefresh = true)
                api.synthesize("Bearer $fresh", text = text, voice = voice, speed = formatSpeed(speed))
            } else {
                throw e
            }
        }
    }

    private fun formatSpeed(speed: Double): String =
        String.format(Locale.US, "%.2f", speed)

    companion object {
        val FALLBACK_VOICES: List<VoiceDto> = listOf(
            VoiceDto("alena", "FEMALE"),
            VoiceDto("filipp", "MALE"),
            VoiceDto("ermil", "MALE"),
            VoiceDto("jane", "FEMALE"),
            VoiceDto("madirus", "MALE"),
            VoiceDto("omazh", "FEMALE"),
            VoiceDto("zahar", "MALE"),
            VoiceDto("dasha", "FEMALE"),
            VoiceDto("julia", "FEMALE"),
            VoiceDto("lera", "FEMALE"),
            VoiceDto("masha", "FEMALE"),
            VoiceDto("marina", "FEMALE"),
            VoiceDto("alexander", "MALE"),
            VoiceDto("kirill", "MALE"),
            VoiceDto("anton", "MALE")
        )
    }
}
