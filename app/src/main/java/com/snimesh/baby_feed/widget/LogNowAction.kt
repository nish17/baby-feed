package com.snimesh.baby_feed.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Thin shim: a Glance ActionCallback must have a public zero-arg constructor, so the testable
 * logic lives in WidgetActionHandler — this class only wires it to the Glance-specific effects. */
class LogNowAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val now = System.currentTimeMillis()
        val entryId = WidgetActionHandler.getInstance(context).logFeedNow(now)
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetActionHandler.LAST_LOGGED_ENTRY_ID] = entryId
            prefs[WidgetActionHandler.LAST_LOGGED_AT_MILLIS] = now
        }
        FeedWidget().updateAll(context)

        val clearUndoWork = OneTimeWorkRequestBuilder<ClearUndoWorker>()
            .setInitialDelay(WidgetActionHandler.UNDO_WINDOW_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(clearUndoWork)
    }
}
