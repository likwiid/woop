// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GoalEntity::class, ObstacleEntity::class, ReviewEntity::class, PromptEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WoopDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun promptDao(): PromptDao

    companion object {
        fun build(context: Context): WoopDatabase =
            Room.databaseBuilder(context, WoopDatabase::class.java, "woop.db").build()
    }
}
