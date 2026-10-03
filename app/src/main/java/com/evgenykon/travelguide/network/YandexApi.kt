package com.evgenykon.travelguide.network

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface YandexIamApi {

    @POST("iam/v1/tokens")
    suspend fun createToken(@Body body: IamTokenRequest): IamTokenResponse
}

interface YandexTtsApi {

    @FormUrlEncoded
    @POST("speech/v1/tts:synthesize")
    suspend fun synthesize(
        @Header("Authorization") authorization: String,
        @Field("text") text: String,
        @Field("voice") voice: String,
        @Field("speed") speed: String = "1.0",
        @Field("lang") lang: String = "ru-RU",
        @Field("format") format: String = "mp3"
    ): ResponseBody

    @GET("tts/v3/voices")
    suspend fun voices(@Header("Authorization") authorization: String): VoicesResponse
}
