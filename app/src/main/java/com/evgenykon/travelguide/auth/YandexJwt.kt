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
        val base64 = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\n", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        val der = Base64.getDecoder().decode(base64)
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(der))
    }
}
