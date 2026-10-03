package com.evgenykon.travelguide.data.repo

import com.evgenykon.travelguide.network.OpenMeteoApi
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class ElevationRepository(private val api: OpenMeteoApi) {

    private val cache = ConcurrentHashMap<String, Double>()

    suspend fun elevation(lat: Double, lng: Double): Double? {
        val key = cacheKey(lat, lng)
        cache[key]?.let { return it }
        return runCatching {
            api.elevation(lat, lng).elevation.firstOrNull()
        }.getOrNull()?.also { cache[key] = it }
    }

    private fun cacheKey(lat: Double, lng: Double): String =
        String.format(Locale.US, "%.4f,%.4f", lat, lng)
}
