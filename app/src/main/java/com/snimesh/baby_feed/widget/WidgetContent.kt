package com.snimesh.baby_feed.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.Text
import com.snimesh.baby_feed.data.FeedingStatus
import java.util.concurrent.TimeUnit

/**
 * Pulled out of FeedWidget so it's unit-testable via runGlanceAppWidgetUnitTest (Glance's own
 * composition can't be unit-tested if it lives directly on the GlanceAppWidget subclass).
 */
@Composable
fun WidgetContent(
    state: WidgetUiState,
    onLogClick: Action,
    onUndoClick: Action,
) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        if (state.justLoggedUndo) {
            Text(
                text = "Logged just now",
                modifier = GlanceModifier.semantics { testTag = "just_logged_text" },
            )
            Button(
                text = "Undo",
                onClick = onUndoClick,
                modifier = GlanceModifier.semantics { testTag = "undo_button" },
            )
        } else {
            when (val status = state.status) {
                is FeedingStatus.NoData -> {
                    Text(
                        text = "No feeds logged yet — tap to log",
                        modifier = GlanceModifier.semantics { testTag = "empty_state_text" },
                    )
                }
                is FeedingStatus.CountdownRemaining -> {
                    Text(
                        text = "Next feed in ${formatDuration(status.remainingMillis)}",
                        modifier = GlanceModifier.semantics { testTag = "countdown_text" },
                    )
                }
                is FeedingStatus.OverdueBy -> {
                    Text(
                        text = "Overdue by ${formatDuration(status.overdueMillis)}",
                        modifier = GlanceModifier.semantics { testTag = "overdue_text" },
                    )
                }
            }
        }
        Button(
            text = "Log feed now",
            onClick = onLogClick,
            modifier = GlanceModifier.semantics { testTag = "log_now_button" },
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
