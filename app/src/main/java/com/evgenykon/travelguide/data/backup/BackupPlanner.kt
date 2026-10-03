package com.evgenykon.travelguide.data.backup

import java.util.Locale

object BackupPlanner {

    fun pointKey(name: String, lat: Double, lng: Double): String =
        "${name.trim().lowercase()}|${String.format(Locale.US, "%.6f", lat)}|" +
            String.format(Locale.US, "%.6f", lng)

    fun plan(
        backup: BackupFile,
        existingRouteNames: Set<String>,
        existingPointKeys: Set<String>
    ): ImportPlan {
        val routesToCreate = mutableListOf<String>()
        val pointsToCreate = mutableListOf<PlannedPoint>()
        val knownRoutes = existingRouteNames.map { it.trim().lowercase() }.toMutableSet()
        val knownPoints = existingPointKeys.toMutableSet()

        for (route in backup.routes) {
            val name = route.name.trim().ifBlank { "Маршрут" }
            if (name.lowercase() !in knownRoutes) {
                knownRoutes += name.lowercase()
                routesToCreate += name
            }
            for (point in route.points) {
                val key = pointKey(point.name, point.lat, point.lng)
                if (key in knownPoints) continue
                knownPoints += key
                pointsToCreate += PlannedPoint(name, point)
            }
        }

        for (point in backup.points) {
            val key = pointKey(point.name, point.lat, point.lng)
            if (key in knownPoints) continue
            knownPoints += key
            pointsToCreate += PlannedPoint(null, point)
        }

        return ImportPlan(routesToCreate, pointsToCreate)
    }
}
