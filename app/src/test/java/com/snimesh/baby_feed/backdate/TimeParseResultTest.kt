package com.snimesh.baby_feed.backdate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class TimeParseResultTest {

    private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
    private val now = format.parse("2026-10-03T15:00")!!.time

    @Test
    fun validPastTimestamp_isParsed() {
        val result = parseModelResponse("2026-10-03T13:30", now)

        assertTrue(result is TimeParseResult.Parsed)
        assertEquals(format.parse("2026-10-03T13:30")!!.time, (result as TimeParseResult.Parsed).timestampMillis)
    }

    @Test
    fun unknownResponse_isUnparseable() {
        val result = parseModelResponse("UNKNOWN", now)

        assertEquals(TimeParseResult.Unparseable, result)
    }

    @Test
    fun nonsenseResponse_isUnparseable() {
        val result = parseModelResponse("I'm not sure what you mean", now)

        assertEquals(TimeParseResult.Unparseable, result)
    }

    @Test
    fun futureTimestamp_isRejectedAsUnparseable() {
        val result = parseModelResponse("2026-10-03T16:00", now)

        assertEquals(TimeParseResult.Unparseable, result)
    }

    @Test
    fun responseWithSurroundingText_stillExtractsTimestamp() {
        val result = parseModelResponse("The feed happened at 2026-10-03T13:30 based on what you said.", now)

        assertTrue(result is TimeParseResult.Parsed)
    }

    @Test
    fun formatPromptTimestamp_roundTripsThroughParseModelResponse() {
        val formatted = formatPromptTimestamp(now)

        val result = parseModelResponse(formatted, now)

        assertEquals(TimeParseResult.Parsed(now), result)
    }
}
