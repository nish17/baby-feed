package com.snimesh.baby_feed.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Single hub for every write path (widget tap, backdate save, history edit/delete) and the
 * sole source of "last feed" / full history reads. See docs/plans/2026-10-02-001-feat-baby-feeding-tracker-plan.md
 * (High-Level Technical Design) for why this is intentionally the only write path in the app.
 */
class FeedRepository(private val dao: FeedDao) {

    fun observeAll(): Flow<List<FeedEntry>> = dao.observeAll()

    /** "Last feed" = max(timestamp) across all entries, not the most-recently-edited row. */
    suspend fun mostRecent(): FeedEntry? = dao.getMostRecent()

    suspend fun logFeed(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
        require(timestampMillis <= nowMillis) { "Cannot log a feed in the future" }
        return dao.insert(FeedEntry(timestampMillis = timestampMillis))
    }

    suspend fun updateFeed(entry: FeedEntry, nowMillis: Long = System.currentTimeMillis()) {
        require(entry.timestampMillis <= nowMillis) { "Cannot log a feed in the future" }
        dao.update(entry)
    }

    suspend fun deleteFeed(entry: FeedEntry) = dao.delete(entry)

    companion object {
        @Volatile
        private var instance: FeedRepository? = null

        /** Lazy singleton keyed on applicationContext, for non-ViewModel call sites (LogNowAction, WidgetRefreshWorker). */
        fun getInstance(context: Context): FeedRepository =
            instance ?: synchronized(this) {
                instance ?: FeedRepository(FeedDatabase.getInstance(context).feedDao())
                    .also { instance = it }
            }
    }
}
