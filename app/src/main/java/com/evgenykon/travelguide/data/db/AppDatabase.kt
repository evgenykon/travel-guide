package com.evgenykon.travelguide.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RouteEntity::class, PointEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pointDao(): PointDao
    abstract fun routeDao(): RouteDao
    abstract fun historyDao(): HistoryDao
}
