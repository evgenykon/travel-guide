package com.evgenykon.travelguide.data.repo

import com.evgenykon.travelguide.network.NominatimApi
import java.util.Collections
import java.util.Locale

data class PlaceInfo(
    val address: String?,
    val city: String?,
    val country: String?
) {
    val location: String?
        get() = listOfNotNull(city, country)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
}

class AddressRepository(private val api: NominatimApi) {

    private val cache = Collections.synchronizedMap(HashMap<String, PlaceInfo?>())

    suspend fun place(lat: Double, lng: Double): PlaceInfo? {
        val key = String.format(Locale.US, "%.5f,%.5f", lat, lng)
        if (cache.containsKey(key)) return cache[key]

        val value = runCatching {
            val response = api.reverse(latitude = lat, longitude = lng)
            val address = response.address
            val road = address?.road?.trim()?.takeIf { it.isNotBlank() }
            val house = address?.houseNumber?.trim()?.takeIf { it.isNotBlank() }
            val addressText = when {
                road != null && house != null -> "$road, д. $house"
                road != null -> road
                else -> response.displayName
                    ?.split(",")
                    ?.take(3)
                    ?.joinToString(", ") { it.trim() }
                    ?.takeIf { it.isNotBlank() }
            }
            val city = listOfNotNull(address?.city, address?.town, address?.village)
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() }
            PlaceInfo(
                address = addressText,
                city = city,
                country = address?.country?.trim()?.takeIf { it.isNotBlank() }
            )
        }.getOrNull()

        cache[key] = value
        return value
    }
}
