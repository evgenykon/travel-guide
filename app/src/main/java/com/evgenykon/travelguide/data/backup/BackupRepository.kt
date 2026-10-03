package com.evgenykon.travelguide.data.backup

import com.evgenykon.travelguide.data.db.AppDatabase
import com.evgenykon.travelguide.data.db.PointEntity
import com.evgenykon.travelguide.data.db.RouteEntity
import kotlinx.serialization.json.Json

data class ImportResult(
    val routesAdded: Int,
    val pointsAdded: Int,
    val pointsSkipped: Int
)

class BackupRepository(private val db: AppDatabase) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private val routeDao = db.routeDao()
    private val pointDao = db.pointDao()

    suspend fun exportJson(): String {
        val routes = routeDao.getAll()
        val points = pointDao.getAll()
        val backup = BackupFile(
            routes = routes.map { route ->
                BackupRoute(
                    name = route.name,
                    createdAt = route.createdAt,
                    points = points.filter { it.routeId == route.id }.map { it.toBackup() }
                )
            },
            points = points.filter { it.routeId == null }.map { it.toBackup() }
        )
        return json.encodeToString(BackupFile.serializer(), backup)
    }

    suspend fun importJson(text: String): ImportResult {
        val backup = json.decodeFromString(BackupFile.serializer(), text)
        val existingRoutes = routeDao.getAll()
        val existingPoints = pointDao.getAll()

        val plan = BackupPlanner.plan(
            backup = backup,
            existingRouteNames = existingRoutes.map { it.name }.toSet(),
            existingPointKeys = existingPoints
                .map { BackupPlanner.pointKey(it.name, it.lat, it.lng) }
                .toSet()
        )

        val routeIds = existingRoutes
            .associate { it.name.trim().lowercase() to it.id }
            .toMutableMap()

        for (name in plan.routesToCreate) {
            val id = routeDao.insert(RouteEntity(name = name))
            routeIds[name.lowercase()] = id
        }

        var pointsAdded = 0
        for (planned in plan.pointsToCreate) {
            val routeId = planned.routeName?.let { routeIds[it.trim().lowercase()] }
            pointDao.insert(
                PointEntity(
                    routeId = routeId,
                    name = planned.point.name.trim().ifBlank { "Точка" },
                    lat = planned.point.lat,
                    lng = planned.point.lng,
                    description = planned.point.description,
                    enabled = planned.point.enabled,
                    radiusMeters = planned.point.radiusMeters.coerceIn(5.0, 100.0),
                    createdAt = planned.point.createdAt.takeIf { it > 0 }
                        ?: System.currentTimeMillis()
                )
            )
            pointsAdded++
        }

        val totalPoints = backup.routes.sumOf { it.points.size } + backup.points.size
        return ImportResult(
            routesAdded = plan.routesToCreate.size,
            pointsAdded = pointsAdded,
            pointsSkipped = totalPoints - pointsAdded
        )
    }

    private fun PointEntity.toBackup() = BackupPoint(
        name = name,
        lat = lat,
        lng = lng,
        description = description,
        enabled = enabled,
        radiusMeters = radiusMeters,
        createdAt = createdAt
    )
}
