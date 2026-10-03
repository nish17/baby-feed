package com.snimesh.baby_feed.widget

import android.content.Context
import android.util.Log
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
        return try {
            FeedWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Periodic widget refresh failed, will retry", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WidgetRefreshWorker"
    }
}
