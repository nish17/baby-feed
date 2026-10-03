package com.snimesh.baby_feed.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll

class UndoLogAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetActionHandler.getInstance(context).undoLastLog()
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetActionHandler.JUST_LOGGED_UNDO] = false
        }
        FeedWidget().updateAll(context)
    }
}
