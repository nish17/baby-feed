package com.snimesh.baby_feed.backdate

import android.content.Context
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Not a real test — a one-off probe to answer the plan's Unit 3 execution note ("verify
 * isOnDeviceRecognitionAvailable() against the actual target device as the very first thing")
 * before writing the rest of the backdating capture flow. Logs the result; does not assert,
 * since either answer is valid and determines which code path Unit 3 actually needs.
 */
@RunWith(AndroidJUnit4::class)
class OnDeviceSttAvailabilityTest {
    @Test
    fun logOnDeviceRecognitionAvailability() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val available = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        Log.i("OnDeviceSttProbe", "isOnDeviceRecognitionAvailable=$available")
    }
}
