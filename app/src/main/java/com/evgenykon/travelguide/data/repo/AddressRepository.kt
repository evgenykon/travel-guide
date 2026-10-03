package com.evgenykon.travelguide.data.repo

import com.evgenykon.travelguide.network.NominatimApi
import java.util.Collections
import java.util.Locale

class AddressRepository(private val api: NominatimApi) {

    private val cache = Collections.synchronizedMap(HashMap<String, String?>())

    suspend fun address(lat: Double, lng: Double): String? {
        val key = String.format(Locale.US, "%.5f,%.5f", lat, lng)
        if (cache.containsKey(key)) return cache[key]

        val value = runCatching {
            val response = api.reverse(latitude = lat, longitude = lng)
            val road = response.address?.road?.trim()?.takeIf { it.isNotBlank() }
            val house = response.address?.houseNumber?.trim()?.takeIf { it.isNotBlank() }
            when {
                road != null && house != null -> "$road, д. $house"
                road != null -> road
                else -> response.displayName
                    ?.split(",")
                    ?.take(3)
                    ?.joinToString(", ") { it.trim() }
                    ?.takeIf { it.isNotBlank() }
            }
        }.getOrNull()

        cache[key] = value
        return value
    }
}
