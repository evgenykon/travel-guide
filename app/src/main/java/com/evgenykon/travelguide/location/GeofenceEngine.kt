package com.evgenykon.travelguide.location

import com.evgenykon.travelguide.util.Geo

data class TrackPoint(
    val id: Long,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Double,
    val enabled: Boolean = true
)

class GeofenceEngine(
    private val cooldownMs: Long = 60_000L,
    private val clock: () -> Long = System::currentTimeMillis
) {

    private data class State(
        var inside: Boolean = false,
        var lastTriggeredAt: Long? = null
    )

    private val states = mutableMapOf<Long, State>()

    fun onLocation(points: List<TrackPoint>, lat: Double, lng: Double): List<TrackPoint> {
        val triggered = mutableListOf<TrackPoint>()
        val now = clock()

        for (point in points) {
            if (!point.enabled) {
                states.remove(point.id)
                continue
            }
            val state = states.getOrPut(point.id) { State() }
            val inside = Geo.distanceMeters(lat, lng, point.lat, point.lng) <= point.radiusMeters
            when {
                inside && !state.inside -> {
                    state.inside = true
                    val lastTriggered = state.lastTriggeredAt
                    if (lastTriggered == null || now - lastTriggered >= cooldownMs) {
                        state.lastTriggeredAt = now
                        triggered += point
                    }
                }
                !inside && state.inside -> {
                    state.inside = false
                }
            }
        }

        val activeIds = points.map { it.id }.toSet()
        states.keys.retainAll(activeIds)
        return triggered
    }

    fun reset() {
        states.clear()
    }
}
