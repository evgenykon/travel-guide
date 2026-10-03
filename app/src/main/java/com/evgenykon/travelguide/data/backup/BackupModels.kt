package com.evgenykon.travelguide.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupFile(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val routes: List<BackupRoute> = emptyList(),
    val points: List<BackupPoint> = emptyList()
)

@Serializable
data class BackupRoute(
    val name: String,
    val createdAt: Long = 0L,
    val points: List<BackupPoint> = emptyList()
)

@Serializable
data class BackupPoint(
    val name: String,
    val lat: Double,
    val lng: Double,
    val description: String = "",
    val enabled: Boolean = true,
    val radiusMeters: Double = 10.0,
    val createdAt: Long = 0L
)

data class PlannedPoint(
    val routeName: String?,
    val point: BackupPoint
)

data class ImportPlan(
    val routesToCreate: List<String>,
    val pointsToCreate: List<PlannedPoint>
)
