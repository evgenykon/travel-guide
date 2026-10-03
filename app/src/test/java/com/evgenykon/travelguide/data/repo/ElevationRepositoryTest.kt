package com.evgenykon.travelguide.data.repo

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.evgenykon.travelguide.network.OpenMeteoApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class ElevationRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ElevationRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoApi::class.java)
        repository = ElevationRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun parsesElevationAndCachesResult() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"elevation":[156.0]}""")
        )

        assertEquals(156.0, repository.elevation(55.75, 37.61)!!, 0.001)
        assertEquals(156.0, repository.elevation(55.75, 37.61)!!, 0.001)

        assertEquals(1, server.requestCount)
        val request = server.takeRequest()
        assertEquals("/v1/elevation?latitude=55.75&longitude=37.61", request.path)
    }

    @Test
    fun returnsNullOnServerError() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertNull(repository.elevation(1.0, 2.0))
    }
}
