package com.snimesh.baby_feed.backdate

class FakeTimeParsingLlm(private val result: TimeParseResult) : TimeParsingLlm {
    override suspend fun parse(text: String, nowMillis: Long): TimeParseResult = result
}
