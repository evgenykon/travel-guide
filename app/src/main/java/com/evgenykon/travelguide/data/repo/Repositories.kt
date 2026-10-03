package com.evgenykon.travelguide.data.repo

import com.evgenykon.travelguide.data.db.HistoryDao
import com.evgenykon.travelguide.data.db.HistoryEntity
import com.evgenykon.travelguide.data.db.PointDao
import com.evgenykon.travelguide.data.db.PointEntity
import com.evgenykon.travelguide.data.db.RouteDao
import com.evgenykon.travelguide.data.db.RouteEntity
import com.evgenykon.travelguide.data.db.RouteWithCount
import kotlinx.coroutines.flow.Flow

class PointRepository(private val dao: PointDao) {

    fun observeAll(): Flow<List<PointEntity>> = dao.observeAll()
    fun observeEnabled(): Flow<List<PointEntity>> = dao.observeEnabled()
    fun observeByRoute(routeId: Long): Flow<List<PointEntity>> = dao.observeByRoute(routeId)

    suspend fun get(id: Long): PointEntity? = dao.getById(id)

    suspend fun save(point: PointEntity): Long {
        return if (point.id == 0L) {
            dao.insert(point)
        } else {
            dao.update(point.copy(updatedAt = System.currentTimeMillis()))
            point.id
        }
    }

    suspend fun delete(point: PointEntity) = dao.delete(point)
    suspend fun setRoute(pointId: Long, routeId: Long?) = dao.setRoute(pointId, routeId)
    suspend fun setEnabled(pointId: Long, enabled: Boolean) = dao.setEnabled(pointId, enabled)
}

class RouteRepository(private val dao: RouteDao, private val pointDao: PointDao) {

    fun observeAll(): Flow<List<RouteEntity>> = dao.observeAll()
    fun observeWithCount(): Flow<List<RouteWithCount>> = dao.observeWithCount()
    fun observeById(id: Long): Flow<RouteEntity?> = dao.observeById(id)

    suspend fun get(id: Long): RouteEntity? = dao.getById(id)
    suspend fun create(name: String): Long = dao.insert(RouteEntity(name = name))
    suspend fun rename(id: Long, name: String) = dao.rename(id, name)
    suspend fun delete(route: RouteEntity) = dao.delete(route)
}

class HistoryRepository(private val dao: HistoryDao) {

    fun observeAll(): Flow<List<HistoryEntity>> = dao.observeAll()
    suspend fun add(entry: HistoryEntity): Long = dao.insert(entry)
    suspend fun delete(entry: HistoryEntity) = dao.delete(entry)
    suspend fun clear() = dao.clear()
}
