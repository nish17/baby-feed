package com.snimesh.baby_feed.history

import com.snimesh.baby_feed.MainDispatcherRule
import com.snimesh.baby_feed.data.FakeFeedRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun editingAnEntry_updatesWhatRepositoryExposesAsLastFeed() = runTest {
        val repository = FakeFeedRepository()
        val now = 10_000_000L
        repository.logFeed(timestampMillis = now - 60_000L, nowMillis = now)
        val entry = repository.mostRecent()!!
        val viewModel = HistoryViewModel(repository)

        viewModel.updateEntry(entry.copy(timestampMillis = now - 30_000L))

        assertEquals(now - 30_000L, repository.mostRecent()?.timestampMillis)
    }

    @Test
    fun editingAnEntryToAFutureTimestamp_isRejected() = runTest {
        val repository = FakeFeedRepository()
        val now = 10_000_000L
        repository.logFeed(timestampMillis = now - 60_000L, nowMillis = now)
        val entry = repository.mostRecent()!!
        val viewModel = HistoryViewModel(repository)

        viewModel.updateEntry(entry.copy(timestampMillis = System.currentTimeMillis() + 60_000L))

        assertEquals(entry.timestampMillis, repository.mostRecent()?.timestampMillis)
    }

    @Test
    fun deletingTheSingleRemainingEntry_emitsZeroEntriesState() = runTest {
        val repository = FakeFeedRepository()
        repository.logFeed(timestampMillis = 10_000_000L, nowMillis = 10_000_000L)
        val entry = repository.mostRecent()!!
        val viewModel = HistoryViewModel(repository)

        viewModel.deleteEntry(entry)

        assertTrue(viewModel.entries.value.isEmpty())
        assertNull(repository.mostRecent())
    }

    @Test
    fun deletingTheNewestOfSeveral_surfacesNextMostRecent() = runTest {
        val repository = FakeFeedRepository()
        val now = 10_000_000L
        repository.logFeed(timestampMillis = now - 60_000L, nowMillis = now)
        repository.logFeed(timestampMillis = now, nowMillis = now)
        val newest = repository.mostRecent()!!
        val viewModel = HistoryViewModel(repository)

        viewModel.deleteEntry(newest)

        assertEquals(now - 60_000L, repository.mostRecent()?.timestampMillis)
        assertEquals(1, viewModel.entries.value.size)
    }
}
