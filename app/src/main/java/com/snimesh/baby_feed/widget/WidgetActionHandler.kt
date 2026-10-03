package com.snimesh.baby_feed.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.snimesh.baby_feed.data.FeedRepository

/**
 * The actual "log an entry" / "undo" business logic, kept free of Context/GlanceId/Glance state
 * APIs so it's a plain JVM unit test target — LogNowAction/UndoLogAction (Glance ActionCallbacks)
 * handle the Glance-specific side effects (updateAppWidgetState, updateAll) themselves, since
 * those need a real Android/Glance runtime to exercise and aren't unit-tested here.
 */
class WidgetActionHandler(private val repository: FeedRepository) {

    suspend fun logFeedNow(nowMillis: Long = System.currentTimeMillis()): Long {
        return repository.logFeed(timestampMillis = nowMillis, nowMillis = nowMillis)
    }

    suspend fun undoLastLog() {
        repository.mostRecent()?.let { repository.deleteFeed(it) }
    }

    companion object {
        val JUST_LOGGED_UNDO = booleanPreferencesKey("just_logged_undo")

        @Volatile
        private var instance: WidgetActionHandler? = null

        fun getInstance(context: Context): WidgetActionHandler =
            instance ?: synchronized(this) {
                instance ?: WidgetActionHandler(FeedRepository.getInstance(context))
                    .also { instance = it }
            }
    }
}
