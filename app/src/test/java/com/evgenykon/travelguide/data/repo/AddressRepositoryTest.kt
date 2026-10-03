package com.evgenykon.travelguide.data.repo

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.evgenykon.travelguide.network.NominatimApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class AddressRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AddressRepository

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
            .create(NominatimApi::class.java)
        repository = AddressRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun buildsPlaceInfoAndCaches() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"display_name":"Армения, Ереван, улица Абовяна, 1","address":{"road":"улица Абовяна","house_number":"1","city":"Ереван","country":"Армения"}}"""
                )
        )

        val first = repository.place(40.18, 44.51)
        val second = repository.place(40.18, 44.51)

        assertEquals("улица Абовяна, д. 1", first?.address)
        assertEquals("Ереван", first?.city)
        assertEquals("Армения", first?.country)
        assertEquals("Ереван, Армения", first?.location)
        assertEquals(first, second)

        assertEquals(1, server.requestCount)
        val request = server.takeRequest()
        assertTrue(request.path.orEmpty().startsWith("/reverse?"))
        assertTrue(request.path.orEmpty().contains("lat=40.18"))
        assertTrue(request.path.orEmpty().contains("lon=44.51"))
    }

    @Test
    fun fallsBackToDisplayName() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"display_name":"Армения, Ереван, Мост Победы","address":{"city":"Ереван","country":"Армения"}}"""
                )
        )

        val place = repository.place(40.17, 44.51)

        assertEquals("Армения, Ереван, Мост Победы", place?.address)
        assertEquals("Ереван, Армения", place?.location)
    }

    @Test
    fun usesTownWhenCityIsMissing() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"display_name":"Россия, Тверь","address":{"road":"Советская улица","town":"Тверь","country":"Россия"}}"""
                )
        )

        val place = repository.place(56.86, 35.91)

        assertEquals("Тверь", place?.city)
        assertEquals("Тверь, Россия", place?.location)
    }

    @Test
    fun returnsNullOnServerError() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertNull(repository.place(1.0, 2.0))
    }
}
