package com.snimesh.baby_feed.settings

import android.content.Context
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

val DEFAULT_FEEDING_INTERVAL_MILLIS: Long = TimeUnit.HOURS.toMillis(3)

/**
 * Declared as an interface (real impl below, plus a hand-written fake in tests) -- same pattern
 * as FeedRepository, SpeechCaptureController, and TimeParsingLlm.
 */
interface SettingsRepository {
    suspend fun getFeedingIntervalMillis(): Long
    suspend fun setFeedingIntervalMillis(intervalMillis: Long)

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SharedPrefsSettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}

/**
 * A single scalar value, not a collection -- plain SharedPreferences rather than DataStore/Room,
 * on first launch with no interval set yet, defaults to DEFAULT_FEEDING_INTERVAL_MILLIS rather
 * than blocking on a forced setup prompt.
 */
class SharedPrefsSettingsRepository(private val context: Context) : SettingsRepository {
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    override suspend fun getFeedingIntervalMillis(): Long =
        prefs.getLong(KEY_INTERVAL_MILLIS, DEFAULT_FEEDING_INTERVAL_MILLIS)

    override suspend fun setFeedingIntervalMillis(intervalMillis: Long) {
        prefs.edit { putLong(KEY_INTERVAL_MILLIS, intervalMillis) }
    }

    companion object {
        private const val PREFS_NAME = "baby_feed_settings"
        private const val KEY_INTERVAL_MILLIS = "feeding_interval_millis"
    }
}
