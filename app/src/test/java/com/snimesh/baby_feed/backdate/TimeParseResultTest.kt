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
    fun relativeMinutes_isSubtractedFromNow() {
        val result = resolveStructuredTimeResponse("RELATIVE 20 MINUTES", now)

        assertEquals(TimeParseResult.Parsed(now - 20 * 60_000L), result)
    }

    @Test
    fun relativeHours_isSubtractedFromNow() {
        val result = resolveStructuredTimeResponse("RELATIVE 1 HOURS", now)

        assertEquals(TimeParseResult.Parsed(now - 60 * 60_000L), result)
    }

    @Test
    fun relativeSingularUnit_isAlsoAccepted() {
        val result = resolveStructuredTimeResponse("RELATIVE 1 HOUR", now)

        assertEquals(TimeParseResult.Parsed(now - 60 * 60_000L), result)
    }

    @Test
    fun now_resolvesToCurrentTime() {
        val result = resolveStructuredTimeResponse("NOW", now)

        assertEquals(TimeParseResult.Parsed(now), result)
    }

    @Test
    fun unknown_isUnparseable() {
        val result = resolveStructuredTimeResponse("UNKNOWN", now)

        assertEquals(TimeParseResult.Unparseable, result)
    }

    @Test
    fun nonsenseResponse_isUnparseable() {
        val result = resolveStructuredTimeResponse("I'm not sure what you mean", now)

        assertEquals(TimeParseResult.Unparseable, result)
    }

    @Test
    fun blankResponse_isUnparseable() {
        val result = resolveStructuredTimeResponse("   ", now)

        assertEquals(TimeParseResult.Unparseable, result)
    }

    @Test
    fun relativeWithMissingUnit_isUnparseable() {
        val result = resolveStructuredTimeResponse("RELATIVE 20", now)

        assertEquals(TimeParseResult.Unparseable, result)
    }

    @Test
    fun relativeWithNonNumericAmount_isUnparseable() {
        val result = resolveStructuredTimeResponse("RELATIVE twenty MINUTES", now)

        assertEquals(TimeParseResult.Unparseable, result)
    }

    @Test
    fun relativeInTheFuture_isRejectedAsUnparseable() {
        // now minus a negative amount would be in the future -- the model shouldn't produce
        // this, but a malformed/hallucinated response must still fail closed.
        val result = resolveStructuredTimeResponse("RELATIVE -5 MINUTES", now)

        assertEquals(TimeParseResult.Unparseable, result)
    }

    @Test
    fun absoluteWithPm_resolvesToTodayWhenInThePast() {
        // now is 2026-10-03T15:00; 1:30 PM today is in the past.
        val result = resolveStructuredTimeResponse("ABSOLUTE 1 30 PM", now)

        assertEquals(TimeParseResult.Parsed(format.parse("2026-10-03T13:30")!!.time), result)
    }

    @Test
    fun absoluteWithAm_resolvesToTodayWhenAlreadyInThePast() {
        // now is 2026-10-03T15:00; 1:30 AM today has already passed, so this should resolve to
        // today at 01:30, not yesterday.
        val result = resolveStructuredTimeResponse("ABSOLUTE 1 30 AM", now)

        assertEquals(TimeParseResult.Parsed(format.parse("2026-10-03T01:30")!!.time), result)
    }

    @Test
    fun absoluteWithAm_rollsBackToYesterdayWhenNoTodayOccurrenceHasHappenedYet() {
        // An earlier "now" where even the AM occurrence hasn't happened yet today.
        val earlyNow = format.parse("2026-10-03T00:30")!!.time

        val result = resolveStructuredTimeResponse("ABSOLUTE 1 30 AM", earlyNow)

        assertEquals(TimeParseResult.Parsed(format.parse("2026-10-02T01:30")!!.time), result)
    }

    @Test
    fun absoluteWithUnspecifiedMeridiem_picksTheMostRecentPastOccurrence() {
        // now is 2026-10-03T15:00. Candidates for "1:30" are: today 01:30 (past), today 13:30
        // (past), yesterday 01:30, yesterday 13:30. The most recent is today 13:30.
        val result = resolveStructuredTimeResponse("ABSOLUTE 1 30 UNSPECIFIED", now)

        assertEquals(TimeParseResult.Parsed(format.parse("2026-10-03T13:30")!!.time), result)
    }

    @Test
    fun absoluteNoonAndMidnight_areHandledCorrectly() {
        val noonResult = resolveStructuredTimeResponse("ABSOLUTE 12 0 PM", now)
        assertEquals(TimeParseResult.Parsed(format.parse("2026-10-03T12:00")!!.time), noonResult)

        val midnightResult = resolveStructuredTimeResponse("ABSOLUTE 12 0 AM", now)
        assertEquals(TimeParseResult.Parsed(format.parse("2026-10-03T00:00")!!.time), midnightResult)
    }

    @Test
    fun absoluteWithOutOfRangeHour_isUnparseable() {
        val result = resolveStructuredTimeResponse("ABSOLUTE 13 30 PM", now)

        assertEquals(TimeParseResult.Unparseable, result)
    }

    @Test
    fun absoluteWithOutOfRangeMinute_isUnparseable() {
        val result = resolveStructuredTimeResponse("ABSOLUTE 1 60 PM", now)

        assertEquals(TimeParseResult.Unparseable, result)
    }

    @Test
    fun responseIsCaseInsensitiveAndTrimsWhitespace() {
        val result = resolveStructuredTimeResponse("  relative 20 minutes  ", now)

        assertEquals(TimeParseResult.Parsed(now - 20 * 60_000L), result)
    }

    @Test
    fun buildTimeParsingPrompt_describesTheStructuredOutputFormats() {
        val prompt = buildTimeParsingPrompt("fed at 1:30")

        assertTrue(prompt.contains("RELATIVE"))
        assertTrue(prompt.contains("ABSOLUTE"))
        assertTrue(prompt.contains("NOW"))
        assertTrue(prompt.contains("UNKNOWN"))
        assertTrue(prompt.contains("fed at 1:30"))
    }
}
