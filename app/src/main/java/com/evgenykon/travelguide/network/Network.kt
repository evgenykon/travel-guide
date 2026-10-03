package com.evgenykon.travelguide.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class Network {

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "EffTravelGuide/1.0 (Android; OSM data)")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val converterFactory = json.asConverterFactory("application/json".toMediaType())

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(converterFactory)
        .build()

    val openRouterApi: OpenRouterApi =
        retrofit("https://openrouter.ai/").create(OpenRouterApi::class.java)

    val yandexIamApi: YandexIamApi =
        retrofit("https://iam.api.cloud.yandex.net/").create(YandexIamApi::class.java)

    val yandexTtsApi: YandexTtsApi =
        retrofit("https://tts.api.cloud.yandex.net/").create(YandexTtsApi::class.java)

    val openMeteoApi: OpenMeteoApi =
        retrofit("https://api.open-meteo.com/").create(OpenMeteoApi::class.java)

    val nominatimApi: NominatimApi =
        retrofit("https://nominatim.openstreetmap.org/").create(NominatimApi::class.java)
}
