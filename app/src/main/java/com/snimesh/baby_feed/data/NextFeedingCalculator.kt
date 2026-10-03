package com.snimesh.baby_feed.data

/** The widget's three display states (R10, R5/R6, R9). */
sealed class FeedingStatus {
    data object NoData : FeedingStatus()

    data class CountdownRemaining(
        val remainingMillis: Long,
        val lastFeedMillis: Long,
        val nextFeedMillis: Long,
    ) : FeedingStatus()

    data class OverdueBy(
        val overdueMillis: Long,
        val lastFeedMillis: Long,
        val nextFeedMillis: Long,
    ) : FeedingStatus()
}

/**
 * Pure, plain-JVM function (no Room/Android dependency) — the single source of truth the widget,
 * and the settings screen's live-apply behavior, both call into.
 */
object NextFeedingCalculator {
    fun calculate(
        lastFeedMillis: Long?,
        intervalMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): FeedingStatus {
        if (lastFeedMillis == null) return FeedingStatus.NoData
        val nextFeedMillis = lastFeedMillis + intervalMillis
        val remainingMillis = nextFeedMillis - nowMillis
        return if (remainingMillis >= 0) {
            FeedingStatus.CountdownRemaining(remainingMillis, lastFeedMillis, nextFeedMillis)
        } else {
            FeedingStatus.OverdueBy(-remainingMillis, lastFeedMillis, nextFeedMillis)
        }
    }
}
