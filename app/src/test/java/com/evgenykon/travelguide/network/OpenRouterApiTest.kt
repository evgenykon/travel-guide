package com.evgenykon.travelguide.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class OpenRouterApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: OpenRouterApi

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenRouterApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun parsesModelsList() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"data":[{"id":"openai/gpt-4o-mini","name":"GPT-4o mini","context_length":128000}]}"""
                )
        )

        val models = api.models("Bearer test-key")

        assertEquals(1, models.data.size)
        assertEquals("openai/gpt-4o-mini", models.data[0].id)
        assertEquals(128000, models.data[0].contextLength)

        val request = server.takeRequest()
        assertEquals("/api/v1/models", request.path)
        assertEquals("Bearer test-key", request.getHeader("Authorization"))
    }

    @Test
    fun parsesChatCompletion() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"choices":[{"message":{"role":"assistant","content":"Привет, путешественник!"}}]}"""
                )
        )

        val response = api.chatCompletions(
            "Bearer test-key",
            ChatRequest(
                model = "openai/gpt-4o-mini",
                messages = listOf(ChatMessage("user", "hi"))
            )
        )

        assertEquals("Привет, путешественник!", response.choices.first().message?.content)

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"model\":\"openai/gpt-4o-mini\""))
        assertTrue(body.contains("\"role\":\"user\""))
    }

    @Test
    fun exchangesCodeForApiKey() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"key":"sk-or-v1-abc","user_id":"user_1"}""")
        )

        val response = api.exchangeCode(
            ExchangeCodeRequest(code = "code1", codeVerifier = "verifier1")
        )

        assertEquals("sk-or-v1-abc", response.key)

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"code\":\"code1\""))
        assertTrue(body.contains("\"code_verifier\":\"verifier1\""))
        assertTrue(body.contains("\"code_challenge_method\":\"S256\""))
    }
}
