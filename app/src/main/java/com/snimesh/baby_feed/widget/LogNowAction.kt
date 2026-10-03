package com.snimesh.baby_feed.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll

/** Thin shim: a Glance ActionCallback must have a public zero-arg constructor, so the testable
 * logic lives in WidgetActionHandler — this class only wires it to the Glance-specific effects. */
class LogNowAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetActionHandler.getInstance(context).logFeedNow()
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetActionHandler.JUST_LOGGED_UNDO] = true
        }
        FeedWidget().updateAll(context)
    }
}
