package com.snimesh.baby_feed.widget

import com.snimesh.baby_feed.data.FakeFeedRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    fun undoLastLog_removesTheMostRecentEntry() = runTest {
        val repository = FakeFeedRepository()
        val handler = WidgetActionHandler(repository)
        handler.logFeedNow(nowMillis = 10_000_000L)

        handler.undoLastLog()

        assertNull(repository.mostRecent())
    }

    @Test
    fun undoLastLog_withNoEntries_doesNothing() = runTest {
        val repository = FakeFeedRepository()
        val handler = WidgetActionHandler(repository)

        handler.undoLastLog()

        assertNull(repository.mostRecent())
    }
}
