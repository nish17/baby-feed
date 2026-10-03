package com.snimesh.baby_feed.backdate

class FakeSpeechCaptureController(private val result: SpeechCaptureResult) : SpeechCaptureController {
    override suspend fun startListening(): SpeechCaptureResult = result
}
