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

/** Same scope/limitation as RefreshWorkerTest -- see its doc comment. */
@RunWith(AndroidJUnit4::class)
class ClearUndoWorkerTest {

    @Test
    fun doWork_succeeds() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<ClearUndoWorker>(context).build()

        val result = worker.doWork()

        assertTrue(result is Result.Success)
    }
}
