package com.takehome.drama.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.takehome.drama.data.local.dao.SavedShowDao
import com.takehome.drama.data.local.dao.WatchProgressDao
import com.takehome.drama.data.local.entity.SavedShowEntity
import com.takehome.drama.data.local.entity.WatchProgressEntity

/** Bump version thì viết Migration — không dùng destructive migration (mất lịch sử xem). */
@Database(
    entities = [WatchProgressEntity::class, SavedShowEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchProgressDao(): WatchProgressDao

    abstract fun savedShowDao(): SavedShowDao

    companion object {
        const val NAME = "flowdrama.db"
    }
}
