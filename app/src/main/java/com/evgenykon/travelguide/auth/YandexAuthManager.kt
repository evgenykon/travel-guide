package com.evgenykon.travelguide.auth

import com.evgenykon.travelguide.data.prefs.SecureStore
import com.evgenykon.travelguide.network.IamTokenRequest
import com.evgenykon.travelguide.network.YandexIamApi
import com.evgenykon.travelguide.network.responseDetails
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.time.Instant

class YandexAuthManager(
    private val secureStore: SecureStore,
    private val iamApi: YandexIamApi
) {

    private val iamMutex = Mutex()

    val isConnected: Boolean
        get() = !secureStore.yandexSaKeyJson.isNullOrBlank()

    fun currentKey(): YandexSaKeyJson? =
        secureStore.yandexSaKeyJson?.let { runCatching { YandexJwt.parseKey(it) }.getOrNull() }

    fun importKeyJson(keyJson: String): Result<YandexSaKeyJson> = runCatching {
        val key = YandexJwt.parseKey(keyJson)
        YandexJwt.parsePrivateKey(key.privateKey)
        secureStore.yandexSaKeyJson = keyJson.trim()
        secureStore.iamToken = null
        secureStore.iamExpiresAt = 0L
        key
    }

    fun disconnect() {
        secureStore.clearYandex()
    }

    suspend fun getIamToken(forceRefresh: Boolean = false): String = iamMutex.withLock {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val cached = secureStore.iamToken
            val expiresAt = secureStore.iamExpiresAt
            if (!cached.isNullOrBlank() && expiresAt - now > IAM_REFRESH_MARGIN_MS) {
                return@withLock cached
            }
        }
        val key = currentKey() ?: error("Yandex SpeechKit не подключён")
        val jwt = YandexJwt.build(key)
        val response = try {
            iamApi.createToken(IamTokenRequest(jwt = jwt))
        } catch (e: HttpException) {
            throw IllegalStateException(
                "Yandex IAM отклонил ключ (HTTP ${e.code()})${e.responseDetails()}",
                e
            )
        }
        val expiresAt = runCatching { Instant.parse(response.expiresAt).toEpochMilli() }
            .getOrDefault(now + FALLBACK_TTL_MS)
        secureStore.iamToken = response.iamToken
        secureStore.iamExpiresAt = expiresAt
        response.iamToken
    }

    private companion object {
        const val IAM_REFRESH_MARGIN_MS = 5 * 60 * 1000L
        const val FALLBACK_TTL_MS = 11 * 60 * 60 * 1000L
    }
}
