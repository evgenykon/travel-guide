package com.evgenykon.travelguide.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object Pkce {

    private val random = SecureRandom()

    fun generateVerifier(): String {
        val bytes = ByteArray(64)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun challenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
