package com.evgenykon.travelguide.auth

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.SignedJWT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64

class YandexJwtTest {

    private fun newKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private fun pem(privateKey: ByteArray, header: String = "PRIVATE KEY"): String =
        "-----BEGIN $header-----\n" +
            Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(privateKey) +
            "\n-----END $header-----"

    @Test
    fun buildsVerifiableToken() {
        val keyPair = newKeyPair()
        val key = YandexSaKeyJson(
            id = "key-id-1",
            serviceAccountId = "sa-id-1",
            privateKey = pem(keyPair.private.encoded)
        )

        val jwt = YandexJwt.build(key, nowMs = 1_700_000_000_000L, ttlSeconds = 3600L)
        val parsed = SignedJWT.parse(jwt)

        assertTrue(parsed.verify(RSASSAVerifier(keyPair.public as RSAPublicKey)))
        assertEquals("key-id-1", parsed.header.keyID)
        assertEquals("sa-id-1", parsed.jwtClaimsSet.issuer)
        assertEquals(listOf(IAM_TOKEN_AUDIENCE), parsed.jwtClaimsSet.audience)
        assertEquals(1_700_000_000_000L, parsed.jwtClaimsSet.issueTime.time)
        assertEquals(1_700_003_600_000L, parsed.jwtClaimsSet.expirationTime.time)
    }

    @Test
    fun parsesPemWithSpacesAndBom() {
        val keyPair = newKeyPair()
        val dirtyPem = "\uFEFF" + pem(keyPair.private.encoded)
            .replace("\n", "\n   ")
            .replace("-----BEGIN PRIVATE KEY-----", "-----BEGIN PRIVATE KEY----- ")

        val parsed = YandexJwt.parsePrivateKey(dirtyPem)

        assertEquals((keyPair.private as RSAPrivateCrtKey).modulus, (parsed as RSAPrivateCrtKey).modulus)
    }

    @Test
    fun parsesEscapedNewlines() {
        val keyPair = newKeyPair()
        val escaped = pem(keyPair.private.encoded).replace("\n", "\\n")

        val parsed = YandexJwt.parsePrivateKey(escaped)

        assertEquals((keyPair.private as RSAPrivateCrtKey).modulus, (parsed as RSAPrivateCrtKey).modulus)
    }

    @Test
    fun parsesPkcs1Key() {
        val keyPair = newKeyPair()
        val pkcs1Pem = pem(pkcs1Der(keyPair.private as RSAPrivateCrtKey), header = "RSA PRIVATE KEY")

        val parsed = YandexJwt.parsePrivateKey(pkcs1Pem)

        assertEquals((keyPair.private as RSAPrivateCrtKey).modulus, (parsed as RSAPrivateCrtKey).modulus)
    }

    @Test
    fun reportsInvalidCharacter() {
        val error = runCatching {
            YandexJwt.parsePrivateKey("-----BEGIN PRIVATE KEY-----\nMII#BAD\n-----END PRIVATE KEY-----")
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error!!.message.orEmpty().contains("'#'"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsKeyWithoutRequiredFields() {
        YandexJwt.parseKey("{}")
    }

    @Test
    fun parsesValidKeyJson() {
        val key = YandexJwt.parseKey(
            """{"id":"k","service_account_id":"s","private_key":"-----BEGIN PRIVATE KEY-----\nAA==\n-----END PRIVATE KEY-----"}"""
        )
        assertEquals("k", key.id)
        assertEquals("s", key.serviceAccountId)
    }

    private fun pkcs1Der(key: RSAPrivateCrtKey): ByteArray = derWrap(
        0x30,
        derInt(BigInteger.ZERO) +
            derInt(key.modulus) +
            derInt(key.publicExponent) +
            derInt(key.privateExponent) +
            derInt(key.primeP) +
            derInt(key.primeQ) +
            derInt(key.primeExponentP) +
            derInt(key.primeExponentQ) +
            derInt(key.crtCoefficient)
    )

    private fun derInt(value: BigInteger): ByteArray = derWrap(0x02, value.toByteArray())

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
