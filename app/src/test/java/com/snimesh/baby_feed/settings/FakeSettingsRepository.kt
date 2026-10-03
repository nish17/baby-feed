package com.snimesh.baby_feed.settings

class FakeSettingsRepository(
    private var intervalMillis: Long = DEFAULT_FEEDING_INTERVAL_MILLIS,
) : SettingsRepository {
    override suspend fun getFeedingIntervalMillis(): Long = intervalMillis

    override suspend fun setFeedingIntervalMillis(intervalMillis: Long) {
        this.intervalMillis = intervalMillis
    }
}
