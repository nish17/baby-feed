package com.snimesh.baby_feed.widget

import com.snimesh.baby_feed.data.FakeFeedRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetActionHandlerTest {

    @Test
    fun logFeedNow_insertsAnEntryAtTheGivenTime() = runTest {
        val repository = FakeFeedRepository()
        val handler = WidgetActionHandler(repository)
        val now = 10_000_000L

        handler.logFeedNow(nowMillis = now)

        assertEquals(now, repository.mostRecent()?.timestampMillis)
    }

    @Test
    fun loggingTwiceInQuickSuccession_secondEntryBecomesMostRecent() = runTest {
        val repository = FakeFeedRepository()
        val handler = WidgetActionHandler(repository)

        handler.logFeedNow(nowMillis = 10_000_000L)
        handler.logFeedNow(nowMillis = 10_000_050L)

        assertEquals(10_000_050L, repository.mostRecent()?.timestampMillis)
    }

    @Test
    fun undoLog_removesTheSpecifiedEntry() = runTest {
        val repository = FakeFeedRepository()
        val handler = WidgetActionHandler(repository)
        val entryId = handler.logFeedNow(nowMillis = 10_000_000L)

        handler.undoLog(entryId)

        assertNull(repository.mostRecent())
    }

    @Test
    fun undoLog_doesNotDeleteALaterEntryLoggedAfterTheBannerAppeared() = runTest {
        // Regression test: tapping "Log now" twice (banner still showing from the first tap)
        // must undo only the entry the banner actually refers to, not whatever is newest.
        val repository = FakeFeedRepository()
        val handler = WidgetActionHandler(repository)
        val firstEntryId = handler.logFeedNow(nowMillis = 10_000_000L)
        handler.logFeedNow(nowMillis = 10_000_600_000L) // a real feed, ~10 minutes later

        handler.undoLog(firstEntryId)

        val remaining = repository.mostRecent()
        assertNotNull(remaining)
        assertEquals(10_000_600_000L, remaining?.timestampMillis)
    }

    @Test
    fun undoLog_withAlreadyDeletedEntry_doesNothing() = runTest {
        val repository = FakeFeedRepository()
        val handler = WidgetActionHandler(repository)

        handler.undoLog(entryId = 999L)

        assertNull(repository.mostRecent())
    }
}
