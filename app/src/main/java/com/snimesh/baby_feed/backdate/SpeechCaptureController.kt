package com.snimesh.baby_feed.backdate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine

sealed class SpeechCaptureResult {
    data class Transcribed(val text: String) : SpeechCaptureResult()
    data object Unavailable : SpeechCaptureResult()
    data object PermissionDenied : SpeechCaptureResult()
    data object RecognitionError : SpeechCaptureResult()
}

/**
 * Declared as an interface (real impl below, plus a hand-written fake in tests) so
 * BackdateViewModel's routing logic (available/unavailable/error -> ManualPicker) is unit
 * testable without touching the real Android SpeechRecognizer.
 */
interface SpeechCaptureController {
    suspend fun startListening(): SpeechCaptureResult
}

/**
 * Gates on isOnDeviceRecognitionAvailable() before attempting createOnDeviceSpeechRecognizer().
 * Never falls back to the network-backed recognizer -- an unavailable engine, denied permission,
 * or recognizer error all surface as a result that routes to the manual picker (R4, R11).
 */
class AndroidSpeechCaptureController(private val context: Context) : SpeechCaptureController {

    override suspend fun startListening(): SpeechCaptureResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return SpeechCaptureResult.PermissionDenied
        }
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            return SpeechCaptureResult.Unavailable
        }

        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        try {
            return suspendCancellableCoroutine { continuation ->
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onResults(results: Bundle) {
                        val text = results
                            .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                        val result = if (text != null) {
                            SpeechCaptureResult.Transcribed(text)
                        } else {
                            SpeechCaptureResult.RecognitionError
                        }
                        if (continuation.isActive) continuation.resumeWith(Result.success(result))
                    }

                    override fun onError(error: Int) {
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.success(SpeechCaptureResult.RecognitionError))
                        }
                    }

                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
                recognizer.startListening(intent)
            }
        } finally {
            recognizer.destroy()
        }
    }
}
