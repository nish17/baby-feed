package com.snimesh.baby_feed.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.snimesh.baby_feed.data.FeedingStatus
import com.snimesh.baby_feed.ui.theme.AccentColor
import com.snimesh.baby_feed.ui.theme.CardBackground
import com.snimesh.baby_feed.ui.theme.MutedText
import com.snimesh.baby_feed.ui.theme.OnAccentText
import com.snimesh.baby_feed.ui.theme.PrimaryText
import com.snimesh.baby_feed.ui.theme.SecondaryButtonBackground
import com.snimesh.baby_feed.ui.theme.WarningColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private val LastFedFormatter = DateTimeFormatter.ofPattern("h:mm a")

/**
 * Pulled out of FeedWidget so it's unit-testable via runGlanceAppWidgetUnitTest (Glance's own
 * composition can't be unit-tested if it lives directly on the GlanceAppWidget subclass).
 *
 * Deliberately draws its own dark card rather than relying on the system's default widget
 * background: the latter adapts to wallpaper/theme and left default-black text unreadable on a
 * dark wallpaper (the bug this redesign fixes).
 */
@Composable
fun WidgetContent(
    state: WidgetUiState,
    onLogClick: Action,
    onUndoClick: Action,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(CardBackground))
            .cornerRadius(24.dp)
            .padding(16.dp),
    ) {
        HeaderRow(lastFedLabel = (state.status as? FeedingStatus.CountdownRemaining)?.lastFeedMillis
            ?.let { formatLastFed(it) }
            ?: (state.status as? FeedingStatus.OverdueBy)?.lastFeedMillis?.let { formatLastFed(it) })

        Spacer(modifier = GlanceModifier.defaultWeight())

        if (state.justLoggedUndo) {
            Text(
                text = "Logged just now",
                style = TextStyle(color = ColorProvider(PrimaryText), fontSize = 18.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.semantics { testTag = "just_logged_text" },
            )
            Spacer(modifier = GlanceModifier.height(12.dp))
            Button(
                text = "Undo",
                onClick = onUndoClick,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = ColorProvider(SecondaryButtonBackground),
                    contentColor = ColorProvider(PrimaryText),
                ),
                modifier = GlanceModifier.fillMaxWidth().semantics { testTag = "undo_button" },
            )
        } else {
            when (val status = state.status) {
                is FeedingStatus.NoData -> {
                    Text(
                        text = "No feeds logged yet — tap to log",
                        style = TextStyle(color = ColorProvider(PrimaryText), fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.semantics { testTag = "empty_state_text" },
                    )
                }
                is FeedingStatus.CountdownRemaining -> {
                    Text(
                        text = "Next feed in ${formatDuration(status.remainingMillis)}",
                        style = TextStyle(color = ColorProvider(AccentColor), fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.semantics { testTag = "countdown_text" },
                    )
                }
                is FeedingStatus.OverdueBy -> {
                    Text(
                        text = "Overdue by ${formatDuration(status.overdueMillis)}",
                        style = TextStyle(color = ColorProvider(WarningColor), fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.semantics { testTag = "overdue_text" },
                    )
                }
            }
            Spacer(modifier = GlanceModifier.height(12.dp))
            Button(
                text = "Log feed now",
                onClick = onLogClick,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = ColorProvider(AccentColor),
                    contentColor = ColorProvider(OnAccentText),
                ),
                modifier = GlanceModifier.fillMaxWidth().semantics { testTag = "log_now_button" },
            )
        }
    }
}

@Composable
private fun HeaderRow(lastFedLabel: String?) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = "🍼 Baby Feed",
            style = TextStyle(color = ColorProvider(MutedText), fontSize = 12.sp, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.defaultWeight(),
        )
        if (lastFedLabel != null) {
            Text(
                text = "Last fed $lastFedLabel",
                style = TextStyle(color = ColorProvider(MutedText), fontSize = 12.sp),
                modifier = GlanceModifier.semantics { testTag = "last_fed_text" },
            )
        }
    }
}

private fun formatLastFed(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(LastFedFormatter)

private fun formatDuration(millis: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
