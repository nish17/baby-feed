package com.snimesh.baby_feed.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.state.GlanceStateDefinition
import com.snimesh.baby_feed.data.FeedRepository
import com.snimesh.baby_feed.data.NextFeedingCalculator
import com.snimesh.baby_feed.settings.SettingsRepository

class FeedWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = FeedRepository.getInstance(context)
        val settingsRepository = SettingsRepository.getInstance(context)
        val mostRecent = repository.mostRecent()
        val intervalMillis = settingsRepository.getFeedingIntervalMillis()
        val status = NextFeedingCalculator.calculate(
            lastFeedMillis = mostRecent?.timestampMillis,
            intervalMillis = intervalMillis,
        )

        provideContent {
            val prefs = currentState<Preferences>()
            val justLoggedUndo = prefs[WidgetActionHandler.JUST_LOGGED_UNDO] ?: false
            WidgetContent(
                state = WidgetUiState(status = status, justLoggedUndo = justLoggedUndo),
                onLogClick = actionRunCallback<LogNowAction>(),
                onUndoClick = actionRunCallback<UndoLogAction>(),
            )
        }
    }
}
