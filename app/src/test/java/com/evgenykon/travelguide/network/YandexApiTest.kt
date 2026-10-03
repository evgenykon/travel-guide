package com.evgenykon.travelguide.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class YandexApiTest {

    private lateinit var server: MockWebServer
    private lateinit var iamApi: YandexIamApi
    private lateinit var ttsApi: YandexTtsApi

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        iamApi = retrofit.create(YandexIamApi::class.java)
        ttsApi = retrofit.create(YandexTtsApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun createsIamTokenFromJwt() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"iamToken":"t1.IAM","expiresAt":"2026-12-31T23:59:59Z"}""")
        )

        val response = iamApi.createToken(IamTokenRequest(jwt = "signed-jwt"))

        assertEquals("t1.IAM", response.iamToken)
        assertEquals("2026-12-31T23:59:59Z", response.expiresAt)

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"jwt\":\"signed-jwt\""))
    }

    @Test
    fun synthesizesAudioBytes() = runTest {
        val audio = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "audio/mpeg")
                .setBody(Buffer().write(audio))
        )

        val body = ttsApi.synthesize(
            authorization = "Bearer t1.IAM",
            text = "Привет",
            voice = "alena",
            speed = "1.00"
        )
        val bytes = body.use { it.bytes() }

        assertArrayEquals(audio, bytes)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/speech/v1/tts:synthesize", request.path)
        assertEquals("Bearer t1.IAM", request.getHeader("Authorization"))

        val form = request.body.readUtf8()
        assertTrue(form.contains("voice=alena"))
        assertTrue(form.contains("format=mp3"))
        assertTrue(form.contains("lang=ru-RU"))
        assertTrue(form.contains("speed=1.00"))
        assertTrue(form.contains("text="))
    }

    @Test
    fun listsVoices() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"voices":[{"name":"alena","gender":"FEMALE","languages":["ru-RU"]},{"name":"john","gender":"MALE","languages":["en-US"]}]}"""
                )
        )

        val response = ttsApi.voices("Bearer t1.IAM")

        assertEquals(2, response.voices.size)
        assertEquals("alena", response.voices[0].name)
        assertEquals("alena (жен.)", response.voices[0].displayName)

        val request = server.takeRequest()
        assertEquals("/tts/v3/voices", request.path)
        assertEquals("Bearer t1.IAM", request.getHeader("Authorization"))
    }
}
