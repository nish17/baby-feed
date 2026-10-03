package com.snimesh.baby_feed.widget

import com.snimesh.baby_feed.data.FeedingStatus

/**
 * What the widget renders: the three display states from NextFeedingCalculator, plus a
 * short-lived "just logged — undo?" overlay (see Key Technical Decisions in the plan for why
 * this lives here rather than a Toast/Snackbar, which a Glance ActionCallback can't host).
 */
data class WidgetUiState(
    val status: FeedingStatus,
    val justLoggedUndo: Boolean = false,
)
