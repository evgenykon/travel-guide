package com.evgenykon.travelguide.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTest {

    @Test
    fun distanceBetweenMoscowAndSpb() {
        val distance = Geo.distanceMeters(55.7558, 37.6173, 59.9343, 30.3351)
        assertTrue("distance=$distance", distance in 630_000.0..640_000.0)
    }

    @Test
    fun distanceIsZeroForSamePoint() {
        assertEquals(0.0, Geo.distanceMeters(55.0, 37.0, 55.0, 37.0), 0.001)
    }

    @Test
    fun circleRingHasAllPointsAtRadius() {
        val ring = Geo.circleRing(55.0, 37.0, 10.0, steps = 16)
        assertEquals(16, ring.size)
        ring.forEach { (lat, lng) ->
            assertEquals(10.0, Geo.distanceMeters(55.0, 37.0, lat, lng), 0.5)
        }
    }

    @Test
    fun destinationMovesNorth() {
        val (lat, lng) = Geo.destination(55.0, 37.0, 0.0, 1000.0)
        assertTrue(lat > 55.0)
        assertEquals(37.0, lng, 0.001)
    }
}
