package com.snimesh.baby_feed.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises FeedRepository against a real, in-memory Room database. Named FeedDaoTest because
 * the plan's test scenarios here are about real Room/SQLite round-tripping, not pure logic —
 * the validation and "last feed" selection behavior lives in FeedRepository, backed by FeedDao.
 */
@RunWith(AndroidJUnit4::class)
class FeedDaoTest {

    private lateinit var db: FeedDatabase
    private lateinit var repository: FeedRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FeedDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomFeedRepository(db.feedDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndReadBack_mostRecentMatches() = runTest {
        val now = 10_000_000L
        repository.logFeed(timestampMillis = now, nowMillis = now)

        val mostRecent = repository.mostRecent()

        assertEquals(now, mostRecent?.timestampMillis)
    }

    @Test
    fun logFeed_withFutureTimestamp_isRejected() = runTest {
        val now = 10_000_000L
        val oneMinuteInFuture = now + 60_000L

        try {
            repository.logFeed(timestampMillis = oneMinuteInFuture, nowMillis = now)
            error("Expected logFeed to reject a future timestamp")
        } catch (expected: IllegalArgumentException) {
            // expected
        }

        assertNull(repository.mostRecent())
    }

    @Test
    fun deletingOnlyEntry_returnsToNoData() = runTest {
        val now = 10_000_000L
        repository.logFeed(timestampMillis = now, nowMillis = now)

        val entry = repository.mostRecent()!!
        repository.deleteFeed(entry)

        assertNull(repository.mostRecent())
    }

    @Test
    fun deletingNewestOfSeveral_fallsBackToNextMostRecent() = runTest {
        val now = 10_000_000L
        val earlier = now - 60_000L
        repository.logFeed(timestampMillis = earlier, nowMillis = now)
        repository.logFeed(timestampMillis = now, nowMillis = now)

        val newest = repository.mostRecent()!!
        assertEquals(now, newest.timestampMillis)
        repository.deleteFeed(newest)

        val newMostRecent = repository.mostRecent()
        assertEquals(earlier, newMostRecent?.timestampMillis)
    }

    @Test
    fun updateFeed_withValidPastTimestamp_persists() = runTest {
        val now = 10_000_000L
        repository.logFeed(timestampMillis = now - 60_000L, nowMillis = now)
        val entry = repository.mostRecent()!!

        repository.updateFeed(entry.copy(timestampMillis = now - 30_000L), nowMillis = now)

        assertEquals(now - 30_000L, repository.mostRecent()?.timestampMillis)
    }

    @Test
    fun updateFeed_withFutureTimestamp_isRejectedAndLeavesOriginalUnchanged() = runTest {
        val now = 10_000_000L
        repository.logFeed(timestampMillis = now - 60_000L, nowMillis = now)
        val entry = repository.mostRecent()!!

        try {
            repository.updateFeed(entry.copy(timestampMillis = now + 60_000L), nowMillis = now)
            error("Expected updateFeed to reject a future timestamp")
        } catch (expected: IllegalArgumentException) {
            // expected
        }

        assertEquals(now - 60_000L, repository.mostRecent()?.timestampMillis)
    }

    @Test
    fun getById_returnsTheMatchingEntry() = runTest {
        val now = 10_000_000L
        val entryId = repository.logFeed(timestampMillis = now, nowMillis = now)

        val found = repository.getById(entryId)

        assertEquals(now, found?.timestampMillis)
    }

    @Test
    fun getById_withUnknownId_returnsNull() = runTest {
        assertNull(repository.getById(999L))
    }
}
