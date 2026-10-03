package com.snimesh.baby_feed.widget

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import com.snimesh.baby_feed.data.FeedRepository

/**
 * The actual "log an entry" / "undo" business logic, kept free of Context/GlanceId/Glance state
 * APIs so it's a plain JVM unit test target — LogNowAction/UndoLogAction (Glance ActionCallbacks)
 * handle the Glance-specific side effects (updateAppWidgetState, updateAll) themselves, since
 * those need a real Android/Glance runtime to exercise and aren't unit-tested here.
 *
 * Undo is bound to the specific entry id that was logged, not "whatever is most recent" — fixed
 * after code review found that a real feed logged while the undo banner was still showing would
 * silently delete the wrong (newer, real) entry instead of the mis-tap.
 */
class WidgetActionHandler(private val repository: FeedRepository) {

    suspend fun logFeedNow(nowMillis: Long = System.currentTimeMillis()): Long {
        return repository.logFeed(timestampMillis = nowMillis, nowMillis = nowMillis)
    }

    /** No-ops if the entry was already edited/deleted by another path (e.g. History) in the meantime. */
    suspend fun undoLog(entryId: Long) {
        repository.getById(entryId)?.let { repository.deleteFeed(it) }
    }

    companion object {
        val LAST_LOGGED_ENTRY_ID = longPreferencesKey("last_logged_entry_id")
        val LAST_LOGGED_AT_MILLIS = longPreferencesKey("last_logged_at_millis")

        /** How long the inline "Logged just now — Undo?" affordance stays valid for. */
        const val UNDO_WINDOW_MILLIS = 8_000L

        @Volatile
        private var instance: WidgetActionHandler? = null

        fun getInstance(context: Context): WidgetActionHandler =
            instance ?: synchronized(this) {
                instance ?: WidgetActionHandler(FeedRepository.getInstance(context))
                    .also { instance = it }
            }
    }
}
