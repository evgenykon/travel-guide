package com.evgenykon.travelguide.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApi {

    @GET("api/v1/models")
    suspend fun models(@Header("Authorization") authorization: String? = null): ModelsResponse

    @POST("api/v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body body: ChatRequest
    ): ChatResponse

    @POST("api/v1/auth/keys")
    suspend fun exchangeCode(@Body body: ExchangeCodeRequest): ExchangeCodeResponse
}
