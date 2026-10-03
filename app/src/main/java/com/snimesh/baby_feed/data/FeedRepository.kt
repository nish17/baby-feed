package com.snimesh.baby_feed.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Single hub for every write path (widget tap, backdate save, history edit/delete) and the
 * sole source of "last feed" / full history reads. See docs/plans/2026-10-02-001-feat-baby-feeding-tracker-plan.md
 * (High-Level Technical Design) for why this is intentionally the only write path in the app.
 *
 * Declared as an interface (real impl below, plus a hand-written fake in tests) so ViewModels
 * and action handlers that depend on it can be unit-tested without a real Room database.
 */
interface FeedRepository {
    fun observeAll(): Flow<List<FeedEntry>>

    /** "Last feed" = max(timestamp) across all entries, not the most-recently-edited row. */
    suspend fun mostRecent(): FeedEntry?

    suspend fun logFeed(timestampMillis: Long, nowMillis: Long): Long

    suspend fun updateFeed(entry: FeedEntry, nowMillis: Long)

    suspend fun deleteFeed(entry: FeedEntry)

    companion object {
        @Volatile
        private var instance: FeedRepository? = null

        /** Lazy singleton keyed on applicationContext, for non-ViewModel call sites (LogNowAction, WidgetRefreshWorker). */
        fun getInstance(context: Context): FeedRepository =
            instance ?: synchronized(this) {
                instance ?: RoomFeedRepository(FeedDatabase.getInstance(context).feedDao())
                    .also { instance = it }
            }
    }
}

class RoomFeedRepository(private val dao: FeedDao) : FeedRepository {

    override fun observeAll(): Flow<List<FeedEntry>> = dao.observeAll()

    override suspend fun mostRecent(): FeedEntry? = dao.getMostRecent()

    override suspend fun logFeed(timestampMillis: Long, nowMillis: Long): Long {
        require(timestampMillis <= nowMillis) { "Cannot log a feed in the future" }
        return dao.insert(FeedEntry(timestampMillis = timestampMillis))
    }

    override suspend fun updateFeed(entry: FeedEntry, nowMillis: Long) {
        require(entry.timestampMillis <= nowMillis) { "Cannot log a feed in the future" }
        dao.update(entry)
    }

    override suspend fun deleteFeed(entry: FeedEntry) = dao.delete(entry)
}
