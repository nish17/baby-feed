package com.snimesh.baby_feed.backdate

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * RECORD_AUDIO is not granted by default in an instrumented test, so the permission-denied
 * path is the one deterministic, device-independent behavior of the real controller we can
 * verify without a human actually speaking into the device.
 */
@RunWith(AndroidJUnit4::class)
class AndroidSpeechCaptureControllerTest {

    @Test
    fun startListening_withoutRecordAudioPermission_returnsPermissionDenied() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = AndroidSpeechCaptureController(context)

        val result = controller.startListening()

        assertEquals(SpeechCaptureResult.PermissionDenied, result)
    }
}
