package com.snimesh.baby_feed.widget

import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasClickAction
import androidx.glance.testing.unit.assertHasText
import androidx.glance.testing.unit.hasTestTag
import com.snimesh.baby_feed.data.FeedingStatus
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WidgetContentTest {

    @Test
    fun countdownRemaining_rendersCountdownTextAndClickableLogButton() = runGlanceAppWidgetUnitTest {
        provideComposable {
            WidgetContent(
                state = WidgetUiState(
                    status = FeedingStatus.CountdownRemaining(
                        remainingMillis = 2 * 60 * 60 * 1000L,
                        lastFeedMillis = 0L,
                        nextFeedMillis = 0L,
                    ),
                ),
                onLogClick = actionRunCallback<LogNowAction>(),
                onUndoClick = actionRunCallback<UndoLogAction>(),
            )
        }

        onNode(hasTestTag("countdown_text")).assertHasText("Next feed in 2h 0m")
        onNode(hasTestTag("log_now_button")).assertHasClickAction()
    }

    @Test
    fun countdownRemaining_rendersLastFedTimeInHeader() = runGlanceAppWidgetUnitTest {
        val lastFeedMillis = 1_700_000_000_000L
        provideComposable {
            WidgetContent(
                state = WidgetUiState(
                    status = FeedingStatus.CountdownRemaining(
                        remainingMillis = 2 * 60 * 60 * 1000L,
                        lastFeedMillis = lastFeedMillis,
                        nextFeedMillis = 0L,
                    ),
                ),
                onLogClick = actionRunCallback<LogNowAction>(),
                onUndoClick = actionRunCallback<UndoLogAction>(),
            )
        }

        val expectedTime = Instant.ofEpochMilli(lastFeedMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("h:mm a"))
        onNode(hasTestTag("last_fed_text")).assertHasText("Last fed $expectedTime")
    }

    @Test
    fun noData_rendersEmptyStatePrompt() = runGlanceAppWidgetUnitTest {
        provideComposable {
            WidgetContent(
                state = WidgetUiState(status = FeedingStatus.NoData),
                onLogClick = actionRunCallback<LogNowAction>(),
                onUndoClick = actionRunCallback<UndoLogAction>(),
            )
        }

        onNode(hasTestTag("empty_state_text")).assertHasText("No feeds logged yet — tap to log")
    }

    @Test
    fun overdue_rendersOverdueIndicator() = runGlanceAppWidgetUnitTest {
        provideComposable {
            WidgetContent(
                state = WidgetUiState(
                    status = FeedingStatus.OverdueBy(
                        overdueMillis = 18 * 60 * 1000L,
                        lastFeedMillis = 0L,
                        nextFeedMillis = 0L,
                    ),
                ),
                onLogClick = actionRunCallback<LogNowAction>(),
                onUndoClick = actionRunCallback<UndoLogAction>(),
            )
        }

        onNode(hasTestTag("overdue_text")).assertHasText("Overdue by 18m")
    }

    @Test
    fun justLoggedUndo_rendersUndoAffordanceInsteadOfStatus() = runGlanceAppWidgetUnitTest {
        provideComposable {
            WidgetContent(
                state = WidgetUiState(status = FeedingStatus.NoData, justLoggedUndo = true),
                onLogClick = actionRunCallback<LogNowAction>(),
                onUndoClick = actionRunCallback<UndoLogAction>(),
            )
        }

        onNode(hasTestTag("just_logged_text")).assertHasText("Logged just now")
        onNode(hasTestTag("undo_button")).assertHasClickAction()
    }
}
