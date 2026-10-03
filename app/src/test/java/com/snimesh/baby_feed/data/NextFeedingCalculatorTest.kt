package com.snimesh.baby_feed.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class NextFeedingCalculatorTest {

    private val threeHours = TimeUnit.HOURS.toMillis(3)

    @Test
    fun `no entries returns NoData`() {
        val status = NextFeedingCalculator.calculate(lastFeedMillis = null, intervalMillis = threeHours)
        assertEquals(FeedingStatus.NoData, status)
    }

    @Test
    fun `last feed one hour ago with three hour interval returns two hours remaining`() {
        val now = 10_000_000L
        val oneHourAgo = now - TimeUnit.HOURS.toMillis(1)

        val status = NextFeedingCalculator.calculate(
            lastFeedMillis = oneHourAgo,
            intervalMillis = threeHours,
            nowMillis = now,
        )

        assertTrue(status is FeedingStatus.CountdownRemaining)
        val remaining = (status as FeedingStatus.CountdownRemaining).remainingMillis
        assertEquals(TimeUnit.HOURS.toMillis(2), remaining)
    }

    @Test
    fun `current time past next feed time returns overdue`() {
        val now = 10_000_000L
        val fourHoursAgo = now - TimeUnit.HOURS.toMillis(4)

        val status = NextFeedingCalculator.calculate(
            lastFeedMillis = fourHoursAgo,
            intervalMillis = threeHours,
            nowMillis = now,
        )

        assertTrue(status is FeedingStatus.OverdueBy)
        val overdueBy = (status as FeedingStatus.OverdueBy).overdueMillis
        assertEquals(TimeUnit.HOURS.toMillis(1), overdueBy)
    }

    @Test
    fun `exactly at next feed time is not yet overdue`() {
        val now = 10_000_000L
        val threeHoursAgo = now - threeHours

        val status = NextFeedingCalculator.calculate(
            lastFeedMillis = threeHoursAgo,
            intervalMillis = threeHours,
            nowMillis = now,
        )

        assertTrue(status is FeedingStatus.CountdownRemaining)
        assertEquals(0L, (status as FeedingStatus.CountdownRemaining).remainingMillis)
    }
}
