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
    fun buildsStreetAndHouseNumberAndCaches() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"display_name":"Россия, Москва, Тверская улица, 1","address":{"road":"Тверская улица","house_number":"1","city":"Москва"}}"""
                )
        )

        assertEquals("Тверская улица, д. 1", repository.address(55.76, 37.61))
        assertEquals("Тверская улица, д. 1", repository.address(55.76, 37.61))

        assertEquals(1, server.requestCount)
        val request = server.takeRequest()
        assertTrue(request.path.orEmpty().startsWith("/reverse?"))
        assertTrue(request.path.orEmpty().contains("lat=55.76"))
        assertTrue(request.path.orEmpty().contains("lon=37.61"))
    }

    @Test
    fun fallsBackToDisplayName() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"display_name":"Россия, Москва, Красная площадь","address":{"city":"Москва"}}"""
                )
        )

        assertEquals("Россия, Москва, Красная площадь", repository.address(55.75, 37.62))
    }

    @Test
    fun returnsNullOnServerError() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertNull(repository.address(1.0, 2.0))
    }
}
