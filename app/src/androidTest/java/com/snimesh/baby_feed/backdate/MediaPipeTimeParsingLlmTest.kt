package com.snimesh.baby_feed.backdate

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real device check for the "model file missing" path (Unit 4's error-path test scenario) --
 * the Gemma model genuinely isn't pushed to this device yet, so this exercises the actual
 * fallback rather than a fake.
 */
@RunWith(AndroidJUnit4::class)
class MediaPipeTimeParsingLlmTest {

    @Test
    fun parse_whenModelFileMissing_returnsModelUnavailableWithoutCrashing() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val llm = MediaPipeTimeParsingLlm(context)

        val result = llm.parse("she fed at 1:30", System.currentTimeMillis())

        assertEquals(TimeParseResult.ModelUnavailable, result)
    }
}
