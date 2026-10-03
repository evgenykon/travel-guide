package com.evgenykon.travelguide.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PointDao {

    @Query("SELECT * FROM points ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PointEntity>>

    @Query("SELECT * FROM points WHERE enabled = 1")
    fun observeEnabled(): Flow<List<PointEntity>>

    @Query("SELECT * FROM points WHERE routeId = :routeId ORDER BY name COLLATE NOCASE")
    fun observeByRoute(routeId: Long): Flow<List<PointEntity>>

    @Query("SELECT * FROM points WHERE id = :id")
    suspend fun getById(id: Long): PointEntity?

    @Query("SELECT * FROM points")
    suspend fun getAll(): List<PointEntity>

    @Insert
    suspend fun insert(point: PointEntity): Long

    @Update
    suspend fun update(point: PointEntity)

    @Delete
    suspend fun delete(point: PointEntity)

    @Query("UPDATE points SET routeId = :routeId WHERE id = :pointId")
    suspend fun setRoute(pointId: Long, routeId: Long?)

    @Query("UPDATE points SET enabled = :enabled WHERE id = :pointId")
    suspend fun setEnabled(pointId: Long, enabled: Boolean)
}

@Dao
interface RouteDao {

    @Query("SELECT * FROM routes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<RouteEntity>>

    @Query(
        "SELECT id, name, createdAt, updatedAt, " +
            "(SELECT COUNT(*) FROM points WHERE points.routeId = routes.id) AS pointCount " +
            "FROM routes ORDER BY updatedAt DESC"
    )
    fun observeWithCount(): Flow<List<RouteWithCount>>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getById(id: Long): RouteEntity?

    @Query("SELECT * FROM routes")
    suspend fun getAll(): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE id = :id")
    fun observeById(id: Long): Flow<RouteEntity?>

    @Insert
    suspend fun insert(route: RouteEntity): Long

    @Update
    suspend fun update(route: RouteEntity)

    @Delete
    suspend fun delete(route: RouteEntity)

    @Query("UPDATE routes SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT DISTINCT pointId FROM history WHERE kind = 'ENTER' AND pointId IS NOT NULL")
    fun observeVisitedPointIds(): Flow<List<Long>>

    @Query("SELECT DISTINCT pointId FROM history WHERE kind = 'ENTER' AND pointId IS NOT NULL")
    suspend fun visitedPointIds(): List<Long>

    @Insert
    suspend fun insert(entry: HistoryEntity): Long

    @Delete
    suspend fun delete(entry: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clear()
}
