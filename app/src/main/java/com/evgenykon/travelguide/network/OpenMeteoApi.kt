package com.evgenykon.travelguide.network

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    @GET("v1/elevation")
    suspend fun elevation(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): ElevationResponse
}
