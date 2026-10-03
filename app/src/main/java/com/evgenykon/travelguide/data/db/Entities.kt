package com.evgenykon.travelguide.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routes"
)
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "points",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("routeId")]
)
data class PointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long? = null,
    val name: String,
    val lat: Double,
    val lng: Double,
    val description: String = "",
    val enabled: Boolean = true,
    val radiusMeters: Double = 10.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pointId: Long?,
    val pointName: String,
    val routeName: String?,
    val lat: Double,
    val lng: Double,
    val kind: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class RouteWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pointCount: Int
)
