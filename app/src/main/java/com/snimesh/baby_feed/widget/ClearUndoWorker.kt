package com.snimesh.baby_feed.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Best-effort nudge that clears the "just logged — undo?" flag and forces a redraw a few
 * seconds after logging, so the widget doesn't look stuck until some other event triggers a
 * redraw. FeedWidget.provideGlance() also self-heals once UNDO_WINDOW_MILLIS elapses even if
 * this worker never runs (e.g. killed by Doze), so this is a UX nicety, not the only mechanism.
 */
class ClearUndoWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val manager = GlanceAppWidgetManager(applicationContext)
            val glanceIds = manager.getGlanceIds(FeedWidget::class.java)
            for (glanceId: GlanceId in glanceIds) {
                updateAppWidgetState(applicationContext, glanceId) { prefs ->
                    prefs.remove(WidgetActionHandler.LAST_LOGGED_ENTRY_ID)
                    prefs.remove(WidgetActionHandler.LAST_LOGGED_AT_MILLIS)
                }
            }
            FeedWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
