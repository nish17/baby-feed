package com.snimesh.baby_feed.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Only verifies doWork() completes successfully. Deeper verification (seeding an overdue entry
 * and asserting the rendered widget state actually flips to "overdue") isn't currently possible
 * from here: FeedWidget.provideGlance() calls the production FeedRepository.getInstance(context)
 * singleton directly rather than an injectable one, so a test can't substitute isolated data
 * without writing into the real on-device feed.db -- which this device also uses for manual
 * verification. Making FeedWidget's data source injectable would be the real fix, deferred as
 * out of scope for this pass.
 */
@RunWith(AndroidJUnit4::class)
class RefreshWorkerTest {

    @Test
    fun doWork_succeeds() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<WidgetRefreshWorker>(context).build()

        val result = worker.doWork()

        assertTrue(result is Result.Success)
    }
}
