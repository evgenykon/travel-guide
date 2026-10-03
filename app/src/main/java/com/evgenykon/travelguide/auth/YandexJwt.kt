package com.evgenykon.travelguide.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date

const val IAM_TOKEN_AUDIENCE = "https://iam.api.cloud.yandex.net/iam/v1/tokens"

@Serializable
data class YandexSaKeyJson(
    val id: String,
    @SerialName("service_account_id") val serviceAccountId: String,
    @SerialName("private_key") val privateKey: String
)

object YandexJwt {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseKey(keyJson: String): YandexSaKeyJson {
        val key = json.decodeFromString<YandexSaKeyJson>(keyJson.trim())
        require(key.id.isNotBlank()) { "В ключе отсутствует поле id" }
        require(key.serviceAccountId.isNotBlank()) { "В ключе отсутствует поле service_account_id" }
        require(key.privateKey.contains("PRIVATE KEY")) { "В ключе отсутствует поле private_key" }
        return key
    }

    fun build(
        key: YandexSaKeyJson,
        nowMs: Long = System.currentTimeMillis(),
        ttlSeconds: Long = 3600
    ): String {
        val privateKey = parsePrivateKey(key.privateKey)
        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(key.id)
            .build()
        val claims = JWTClaimsSet.Builder()
            .audience(IAM_TOKEN_AUDIENCE)
            .issuer(key.serviceAccountId)
            .issueTime(Date(nowMs))
            .expirationTime(Date(nowMs + ttlSeconds * 1000))
            .build()
        val jwt = SignedJWT(header, claims)
        jwt.sign(RSASSASigner(privateKey))
        return jwt.serialize()
    }

    fun parsePrivateKey(pem: String): PrivateKey {
        val normalized = pem
            .replace("\\n", "\n")
            .replace("\\r", "\n")
            .replace("\r\n", "\n")

        val begin = normalized.indexOf("-----BEGIN")
        val end = normalized.indexOf("-----END")
        val body = if (begin >= 0 && end > begin) normalized.substring(begin, end) else normalized

        val base64 = body
            .replace(Regex("-----BEGIN[^-]*-----"), "")
            .replace(Regex("-----END[^-]*-----"), "")
            .filterNot { it.isWhitespace() || it == '\uFEFF' }

        val badChar = base64.firstOrNull { !isBase64Char(it) }
        if (badChar != null) {
            throw IllegalArgumentException(
                "Некорректный private_key: недопустимый символ '$badChar' (код ${badChar.code}). " +
                    "Нужен key.json сервисного аккаунта Yandex Cloud."
            )
        }
        if (base64.isEmpty()) {
            throw IllegalArgumentException("Некорректный private_key: пустое значение")
        }

        val der = try {
            Base64.getDecoder().decode(base64)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Некорректный private_key: ${e.message}", e)
        }

        val isPkcs1 = normalized.contains("BEGIN RSA PRIVATE KEY")
        val pkcs8 = if (isPkcs1) pkcs1ToPkcs8(der) else der

        return try {
            KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(pkcs8))
        } catch (e: Exception) {
            throw IllegalArgumentException("Не удалось прочитать приватный ключ: ${e.message}", e)
        }
    }

    private fun isBase64Char(c: Char): Boolean =
        c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '+' || c == '/' || c == '='

    private fun pkcs1ToPkcs8(pkcs1: ByteArray): ByteArray {
        val version = byteArrayOf(0x02, 0x01, 0x00)
        val algorithmIdentifier = byteArrayOf(
            0x30, 0x0d,
            0x06, 0x09,
            0x2a, 0x86.toByte(), 0x48, 0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01,
            0x05, 0x00
        )
        val privateKey = derWrap(0x04, pkcs1)
        return derWrap(0x30, version + algorithmIdentifier + privateKey)
    }

    private fun derWrap(tag: Int, content: ByteArray): ByteArray {
        val length = derLength(content.size)
        val result = ByteArray(1 + length.size + content.size)
        result[0] = tag.toByte()
        length.copyInto(result, 1)
        content.copyInto(result, 1 + length.size)
        return result
    }

    private fun derLength(length: Int): ByteArray = when {
        length < 0x80 -> byteArrayOf(length.toByte())
        length < 0x100 -> byteArrayOf(0x81.toByte(), length.toByte())
        length < 0x10000 -> byteArrayOf(
            0x82.toByte(),
            (length shr 8).toByte(),
            length.toByte()
        )
        else -> byteArrayOf(
            0x83.toByte(),
            (length shr 16).toByte(),
            (length shr 8).toByte(),
            length.toByte()
        )
    }
}
