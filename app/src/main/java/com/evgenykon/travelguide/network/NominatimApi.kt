package com.evgenykon.travelguide.network

import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {

    @GET("reverse")
    suspend fun reverse(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("format") format: String = "jsonv2",
        @Query("accept-language") language: String = "ru",
        @Query("zoom") zoom: Int = 18
    ): NominatimResponse
}
