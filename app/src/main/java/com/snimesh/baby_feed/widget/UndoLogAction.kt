package com.snimesh.baby_feed.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition

class UndoLogAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs: Preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val entryId = prefs[WidgetActionHandler.LAST_LOGGED_ENTRY_ID]

        if (entryId != null) {
            WidgetActionHandler.getInstance(context).undoLog(entryId)
        }

        updateAppWidgetState(context, glanceId) { mutablePrefs ->
            mutablePrefs.remove(WidgetActionHandler.LAST_LOGGED_ENTRY_ID)
            mutablePrefs.remove(WidgetActionHandler.LAST_LOGGED_AT_MILLIS)
        }
        FeedWidget().updateAll(context)
    }
}
