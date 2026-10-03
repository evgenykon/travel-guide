package com.evgenykon.travelguide.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64

class PkceTest {

    @Test
    fun verifierIsUrlSafe() {
        val verifier = Pkce.generateVerifier()
        assertTrue(verifier.length >= 43)
        assertFalse(verifier.contains("="))
        assertFalse(verifier.contains("+"))
        assertFalse(verifier.contains("/"))
    }

    @Test
    fun challengeMatchesSha256Base64Url() {
        val verifier = "test-verifier-value"
        val expected = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        )
        assertEquals(expected, Pkce.challenge(verifier))
    }

    @Test
    fun verifiersAreUnique() {
        assertNotEquals(Pkce.generateVerifier(), Pkce.generateVerifier())
    }
}
