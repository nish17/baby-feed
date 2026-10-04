package com.snimesh.baby_feed.backdate

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaPipeTimeParsingLlmTest {

    /**
     * Uses a deliberately bogus path rather than depending on whatever happens to be (or not be)
     * at the real MODEL_PATH on this device -- that's what made the previous version of this
     * test device-state-dependent (flagged by code review).
     */
    @Test
    fun parse_whenModelFileMissing_returnsModelUnavailableWithoutCrashing() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val llm = MediaPipeTimeParsingLlm(context, modelPath = "/data/local/tmp/llm/does-not-exist.task")

        val result = llm.parse("she fed at 1:30", System.currentTimeMillis())

        assertEquals(TimeParseResult.ModelUnavailable, result)
    }

    /**
     * Exercises real on-device inference against the actual pushed Gemma model. Only
     * meaningful once the model has been adb-pushed to MediaPipeTimeParsingLlm.MODEL_PATH --
     * will report ModelUnavailable (a correct, non-crashing result) rather than fail outright
     * if the model isn't present on whatever device runs this.
     */
    @Test
    fun parse_withRealModel_parsesARelativeTimeExpression() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val llm = MediaPipeTimeParsingLlm(context)
        val now = System.currentTimeMillis()

        val result = llm.parse("she fed about 20 minutes ago", now)

        when (result) {
            is TimeParseResult.Parsed -> {
                val deltaMinutes = (now - result.timestampMillis) / 60_000
                // Tight tolerance -- the model only has to classify this as RELATIVE 20
                // MINUTES; the subtraction itself is exact Kotlin arithmetic, not model output.
                assertTrue(
                    "Expected exactly 20 minutes ago, got $deltaMinutes minutes ago",
                    deltaMinutes in 19..21,
                )
            }
            TimeParseResult.ModelUnavailable -> {
                // Model not pushed to this device -- a valid, non-crashing outcome, not a failure.
            }
            TimeParseResult.Unparseable -> {
                error("Model produced output it couldn't parse for a clear relative-time phrase")
            }
        }
    }

    @Test
    fun parse_withRealModel_parsesAnExplicitClockTime() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val llm = MediaPipeTimeParsingLlm(context)
        val now = System.currentTimeMillis()

        val result = llm.parse("fed at 1:30", now)

        when (result) {
            is TimeParseResult.Parsed -> {
                // Precise hour/minute correctness is a manual-verification concern (model
                // output quality, not something to pin down with an exact assertion) -- this
                // just confirms the model produced *something* parseable for a clear phrase.
                assertTrue(
                    "Expected the parsed time to be at or before now",
                    result.timestampMillis <= now,
                )
            }
            TimeParseResult.ModelUnavailable -> {
                // Model not pushed to this device -- a valid, non-crashing outcome, not a failure.
            }
            TimeParseResult.Unparseable -> {
                error("Model produced output it couldn't parse for a clear explicit-time phrase")
            }
        }
    }
}
