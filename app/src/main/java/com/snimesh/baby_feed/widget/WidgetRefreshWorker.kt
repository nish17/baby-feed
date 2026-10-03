package com.snimesh.baby_feed.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 15-minute periodic backstop so the overdue indicator (R9) still flips even with no user
 * interaction — AppWidgetManager's own updatePeriodMillis floor is 30 minutes, and Samsung's
 * Doze/battery management may make even this best-effort rather than exact (see Risks &
 * Dependencies in the plan).
 */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        FeedWidget().updateAll(applicationContext)
        return Result.success()
    }
}
