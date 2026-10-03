package com.evgenykon.travelguide.data.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPlannerTest {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private fun point(
        name: String,
        lat: Double = 55.0,
        lng: Double = 37.0,
        description: String = ""
    ) = BackupPoint(name = name, lat = lat, lng = lng, description = description)

    @Test
    fun plansNewRouteAndPoints() {
        val backup = BackupFile(
            routes = listOf(BackupRoute(name = "Центр", points = listOf(point("Кремль")))),
            points = listOf(point("Скамейка", lat = 55.1, lng = 37.1))
        )

        val plan = BackupPlanner.plan(backup, emptySet(), emptySet())

        assertEquals(listOf("Центр"), plan.routesToCreate)
        assertEquals(2, plan.pointsToCreate.size)
        assertEquals("Центр", plan.pointsToCreate[0].routeName)
        assertEquals(null, plan.pointsToCreate[1].routeName)
    }

    @Test
    fun reusesExistingRouteCaseInsensitive() {
        val backup = BackupFile(
            routes = listOf(BackupRoute(name = " центр ", points = listOf(point("Кремль"))))
        )

        val plan = BackupPlanner.plan(backup, setOf("Центр"), emptySet())

        assertTrue(plan.routesToCreate.isEmpty())
        assertEquals(1, plan.pointsToCreate.size)
        assertEquals("центр", plan.pointsToCreate[0].routeName)
    }

    @Test
    fun skipsDuplicatePoints() {
        val backup = BackupFile(points = listOf(point("Кремль")))
        val existingKeys = setOf(BackupPlanner.pointKey("кремль", 55.0, 37.0))

        val plan = BackupPlanner.plan(backup, emptySet(), existingKeys)

        assertTrue(plan.pointsToCreate.isEmpty())
    }

    @Test
    fun serializationRoundTrip() {
        val backup = BackupFile(
            routes = listOf(BackupRoute(name = "Маршрут", points = listOf(point("Точка")))),
            points = listOf(point("Одиночная", lat = 1.0, lng = 2.0))
        )

        val text = json.encodeToString(BackupFile.serializer(), backup)
        val restored = json.decodeFromString(BackupFile.serializer(), text)

        assertEquals(backup.copy(exportedAt = restored.exportedAt), restored)
    }
}
