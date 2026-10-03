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
import java.util.concurrent.TimeUnit

class FeedWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = FeedRepository.getInstance(context)
        val mostRecent = repository.mostRecent()
        // TODO(Unit 6): read the configurable interval from Settings instead of this default.
        val status = NextFeedingCalculator.calculate(
            lastFeedMillis = mostRecent?.timestampMillis,
            intervalMillis = DEFAULT_FEEDING_INTERVAL_MILLIS,
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

    companion object {
        val DEFAULT_FEEDING_INTERVAL_MILLIS: Long = TimeUnit.HOURS.toMillis(3)
    }
}
