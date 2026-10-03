package com.snimesh.baby_feed.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FeedEntry::class], version = 1, exportSchema = false)
abstract class FeedDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao

    companion object {
        @Volatile
        private var instance: FeedDatabase? = null

        fun getInstance(context: Context): FeedDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FeedDatabase::class.java,
                    "feed.db",
                ).build().also { instance = it }
            }
    }
}
