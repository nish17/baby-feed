package com.snimesh.baby_feed.backdate

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

sealed class TimeParseResult {
    data class Parsed(val timestampMillis: Long) : TimeParseResult()
    data object Unparseable : TimeParseResult()
    data object ModelUnavailable : TimeParseResult()
}

/**
 * Asks the model to classify the input into a structured shape -- NOT to compute a final
 * timestamp itself. Real on-device testing found the model reliably extracts numbers/words
 * already present in the input ("20", "minutes", "1", "30") but is unreliable at actually doing
 * subtraction arithmetic, even with a worked example and low decoding temperature (tried both,
 * same wrong answer every time). All arithmetic now happens in resolveStructuredTimeResponse()
 * -- deterministic Kotlin code, not model output. This also means the model no longer needs the
 * current time as context at all, since it's only extracting what's already in the sentence.
 */
fun buildTimeParsingPrompt(capturedText: String): String =
    """
    Classify this description of when a baby was fed into exactly one of these formats. Reply
    with ONLY one line, nothing else.

    - A relative time like "20 minutes ago" or "about an hour ago": reply RELATIVE <number> <MINUTES or HOURS>
    - An explicit clock time like "1:30" or "1:30pm": reply ABSOLUTE <hour 1-12> <minute 0-59> <AM, PM, or UNSPECIFIED if not stated>
    - Right now / just fed: reply NOW
    - If you cannot determine a time: reply UNKNOWN

    Examples:
    Input: "she fed 20 minutes ago" -> RELATIVE 20 MINUTES
    Input: "about an hour ago" -> RELATIVE 1 HOURS
    Input: "fed at 1:30" -> ABSOLUTE 1 30 UNSPECIFIED
    Input: "fed at 1:30pm" -> ABSOLUTE 1 30 PM
    Input: "just now" -> NOW

    Now the real input:
    Input: "$capturedText"
    """.trimIndent()

/**
 * All arithmetic lives here, not in the model (see buildTimeParsingPrompt's doc comment).
 * Pulled out of MediaPipeTimeParsingLlm so it's a plain JVM unit test target.
 */
fun resolveStructuredTimeResponse(response: String, nowMillis: Long): TimeParseResult {
    val tokens = response.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return TimeParseResult.Unparseable

    val resolvedMillis = when (tokens[0].uppercase()) {
        "NOW" -> nowMillis
        "UNKNOWN" -> return TimeParseResult.Unparseable
        "RELATIVE" -> resolveRelative(tokens, nowMillis)
        "ABSOLUTE" -> resolveAbsolute(tokens, nowMillis)
        else -> null
    } ?: return TimeParseResult.Unparseable

    return if (resolvedMillis > nowMillis) TimeParseResult.Unparseable else TimeParseResult.Parsed(resolvedMillis)
}

/** Expects tokens: RELATIVE <amount> <MINUTES|HOURS>. */
private fun resolveRelative(tokens: List<String>, nowMillis: Long): Long? {
    if (tokens.size < 3) return null
    val amount = tokens[1].toLongOrNull() ?: return null
    val unitMillis = when (tokens[2].uppercase().removeSuffix("S")) {
        "MINUTE" -> 60_000L
        "HOUR" -> 3_600_000L
        else -> return null
    }
    return nowMillis - (amount * unitMillis)
}

/** Expects tokens: ABSOLUTE <hour 1-12> <minute 0-59> <AM|PM|UNSPECIFIED>. */
private fun resolveAbsolute(tokens: List<String>, nowMillis: Long): Long? {
    if (tokens.size < 4) return null
    val hour12 = tokens[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val minute = tokens[2].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
    val meridiem = tokens[3].uppercase()

    val now = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())

    fun candidate(isPm: Boolean, daysAgo: Long): ZonedDateTime {
        val hour24 = when {
            isPm && hour12 != 12 -> hour12 + 12
            !isPm && hour12 == 12 -> 0
            else -> hour12
        }
        return now.minusDays(daysAgo).withHour(hour24).withMinute(minute).withSecond(0).withNano(0)
    }

    // "1:30" with no AM/PM is ambiguous -- resolve it the way a person would: whichever
    // interpretation is the most recent one that's actually already happened.
    val candidates = when (meridiem) {
        "AM" -> listOf(candidate(isPm = false, daysAgo = 0), candidate(isPm = false, daysAgo = 1))
        "PM" -> listOf(candidate(isPm = true, daysAgo = 0), candidate(isPm = true, daysAgo = 1))
        "UNSPECIFIED" -> listOf(
            candidate(isPm = false, daysAgo = 0),
            candidate(isPm = true, daysAgo = 0),
            candidate(isPm = false, daysAgo = 1),
            candidate(isPm = true, daysAgo = 1),
        )
        else -> return null
    }

    return candidates
        .map { it.toInstant().toEpochMilli() }
        .filter { it <= nowMillis }
        .maxOrNull()
}
