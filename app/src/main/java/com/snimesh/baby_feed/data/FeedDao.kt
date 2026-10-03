package com.snimesh.baby_feed.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Insert
    suspend fun insert(entry: FeedEntry): Long

    @Update
    suspend fun update(entry: FeedEntry)

    @Delete
    suspend fun delete(entry: FeedEntry)

    @Query("SELECT * FROM feed_entries ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<FeedEntry>>

    @Query("SELECT * FROM feed_entries WHERE id = :id")
    suspend fun getById(id: Long): FeedEntry?

    /** "Last feed" = max(timestamp) across all entries, not the most-recently-edited row. */
    @Query("SELECT * FROM feed_entries ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getMostRecent(): FeedEntry?
}
