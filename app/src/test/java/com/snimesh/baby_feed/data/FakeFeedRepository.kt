package com.snimesh.baby_feed.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Hand-written fake, not a mock, per the plan's testing convention for FeedRepository. */
class FakeFeedRepository : FeedRepository {
    private val entriesFlow = MutableStateFlow<List<FeedEntry>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<FeedEntry>> = entriesFlow

    override suspend fun mostRecent(): FeedEntry? = entriesFlow.value.maxByOrNull { it.timestampMillis }

    override suspend fun getById(id: Long): FeedEntry? = entriesFlow.value.find { it.id == id }

    override suspend fun logFeed(timestampMillis: Long, nowMillis: Long): Long {
        require(timestampMillis <= nowMillis) { "Cannot log a feed in the future" }
        val entry = FeedEntry(id = nextId++, timestampMillis = timestampMillis)
        entriesFlow.value = entriesFlow.value + entry
        return entry.id
    }

    override suspend fun updateFeed(entry: FeedEntry, nowMillis: Long) {
        require(entry.timestampMillis <= nowMillis) { "Cannot log a feed in the future" }
        entriesFlow.value = entriesFlow.value.map { if (it.id == entry.id) entry else it }
    }

    override suspend fun deleteFeed(entry: FeedEntry) {
        entriesFlow.value = entriesFlow.value.filterNot { it.id == entry.id }
    }
}
