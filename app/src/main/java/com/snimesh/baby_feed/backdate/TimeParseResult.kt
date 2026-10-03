package com.snimesh.baby_feed.backdate

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class TimeParseResult {
    data class Parsed(val timestampMillis: Long) : TimeParseResult()
    data object Unparseable : TimeParseResult()
    data object ModelUnavailable : TimeParseResult()
}

private val PROMPT_TIMESTAMP_FORMAT = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
}
private val TIMESTAMP_PATTERN = Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}""")

fun formatPromptTimestamp(millis: Long): String =
    PROMPT_TIMESTAMP_FORMAT.get()!!.format(Date(millis))

/**
 * Defensively parses the model's free-text response (MediaPipe's tasks-genai doesn't support
 * schema-constrained output) into a timestamp. Pulled out of MediaPipeTimeParsingLlm so it's a
 * plain JVM unit test target, independent of the real model.
 */
fun parseModelResponse(response: String, nowMillis: Long): TimeParseResult {
    val trimmed = response.trim()
    if (trimmed.equals("UNKNOWN", ignoreCase = true)) return TimeParseResult.Unparseable
    val match = TIMESTAMP_PATTERN.find(trimmed) ?: return TimeParseResult.Unparseable
    val parsedMillis = try {
        PROMPT_TIMESTAMP_FORMAT.get()!!.parse(match.value)?.time
    } catch (e: java.text.ParseException) {
        null
    } ?: return TimeParseResult.Unparseable
    return if (parsedMillis > nowMillis) TimeParseResult.Unparseable else TimeParseResult.Parsed(parsedMillis)
}
