package com.evgenykon.travelguide.auth

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.SignedJWT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.util.Base64

class YandexJwtTest {

    @Test
    fun buildsVerifiableToken() {
        val generator = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        val keyPair = generator.generateKeyPair()

        val pem = "-----BEGIN PRIVATE KEY-----\n" +
            Base64.getMimeEncoder(64, "\n".toByteArray())
                .encodeToString(keyPair.private.encoded) +
            "\n-----END PRIVATE KEY-----"

        val key = YandexSaKeyJson(
            id = "key-id-1",
            serviceAccountId = "sa-id-1",
            privateKey = pem
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
}
