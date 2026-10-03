package com.evgenykon.travelguide.location

import org.junit.Assert.assertEquals
import org.junit.Test

class GeofenceEngineTest {

    private val point = TrackPoint(id = 1L, lat = 55.0, lng = 37.0, radiusMeters = 10.0)

    @Test
    fun triggersOnceOnEnter() {
        val engine = GeofenceEngine(cooldownMs = 0L, clock = { 0L })
        assertEquals(1, engine.onLocation(listOf(point), 55.0, 37.0).size)
        assertEquals(0, engine.onLocation(listOf(point), 55.0, 37.0).size)
        assertEquals(0, engine.onLocation(listOf(point), 55.00001, 37.00001).size)
    }

    @Test
    fun triggersAgainAfterExit() {
        val engine = GeofenceEngine(cooldownMs = 0L, clock = { 0L })
        engine.onLocation(listOf(point), 55.0, 37.0)
        engine.onLocation(listOf(point), 55.001, 37.0)
        assertEquals(1, engine.onLocation(listOf(point), 55.0, 37.0).size)
    }

    @Test
    fun cooldownBlocksQuickRetrigger() {
        var now = 0L
        val engine = GeofenceEngine(cooldownMs = 60_000L, clock = { now })

        assertEquals(1, engine.onLocation(listOf(point), 55.0, 37.0).size)

        engine.onLocation(listOf(point), 55.001, 37.0)
        now = 30_000L
        assertEquals(0, engine.onLocation(listOf(point), 55.0, 37.0).size)

        engine.onLocation(listOf(point), 55.001, 37.0)
        now = 61_000L
        assertEquals(1, engine.onLocation(listOf(point), 55.0, 37.0).size)
    }

    @Test
    fun disabledPointsAreIgnored() {
        val engine = GeofenceEngine(cooldownMs = 0L, clock = { 0L })
        val disabled = point.copy(enabled = false)
        assertEquals(0, engine.onLocation(listOf(disabled), 55.0, 37.0).size)
    }

    @Test
    fun outsideRadiusDoesNotTrigger() {
        val engine = GeofenceEngine(cooldownMs = 0L, clock = { 0L })
        assertEquals(0, engine.onLocation(listOf(point), 55.001, 37.0).size)
    }
}
