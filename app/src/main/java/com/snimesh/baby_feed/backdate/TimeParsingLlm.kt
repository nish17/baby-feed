package com.snimesh.baby_feed.backdate

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Declared as an interface (real impl below, plus a hand-written fake in tests) — same pattern
 * as SpeechCaptureController and FeedRepository.
 */
interface TimeParsingLlm {
    suspend fun parse(text: String, nowMillis: Long): TimeParseResult
}

/**
 * Application-scoped singleton (see getInstance): the ~529MB Gemma model is loaded once, lazily,
 * on first use and kept warm — reloading it per call would add several seconds of latency every
 * single time. The model is expected at MODEL_PATH, pushed there via `adb push` (see the plan's
 * Key Technical Decisions) rather than bundled in the APK.
 *
 * The whole point of this class is "never throws, fails closed to the manual picker" — both the
 * lazy model-load and generateResponse() are wrapped so a corrupt model, OOM, or native/JNI
 * failure degrades to ModelUnavailable instead of crashing the app (found by code review: the
 * original version only guarded the "file missing" case).
 */
class MediaPipeTimeParsingLlm(private val context: Context) : TimeParsingLlm {

    @Volatile
    private var llmInference: LlmInference? = null
    private val loadMutex = Mutex()

    override suspend fun parse(text: String, nowMillis: Long): TimeParseResult =
        withContext(Dispatchers.IO) {
            if (!File(MODEL_PATH).exists()) {
                return@withContext TimeParseResult.ModelUnavailable
            }
            try {
                val llm = getOrLoadModel()
                val prompt = buildPrompt(nowFormatted = formatPromptTimestamp(nowMillis), capturedText = text)
                val response = llm.generateResponse(prompt)
                parseModelResponse(response, nowMillis)
            } catch (e: Throwable) {
                // Throwable, not Exception: loading a ~529MB native model can throw
                // OutOfMemoryError, which extends Error, not Exception. The explicit contract
                // here is "never crash" -- a native/JNI failure degrading to the manual picker
                // is strictly better than letting any kind of failure propagate.
                TimeParseResult.ModelUnavailable
            }
        }

    /** Mutex-guarded so two concurrent parse() calls can't both load the ~529MB model at once. */
    private suspend fun getOrLoadModel(): LlmInference =
        llmInference ?: loadMutex.withLock {
            llmInference ?: loadModel().also { llmInference = it }
        }

    private fun loadModel(): LlmInference {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(MODEL_PATH)
            .setMaxTokens(256)
            .setMaxTopK(40)
            .build()
        return LlmInference.createFromOptions(context, options)
    }

    private fun buildPrompt(nowFormatted: String, capturedText: String): String =
        """
        The current date and time is $nowFormatted.
        The user said: "$capturedText"
        They are describing when their baby was fed. Reply with ONLY the resulting date and
        time in the exact format yyyy-MM-ddTHH:mm, and nothing else.
        If you cannot determine a specific time, reply with exactly: UNKNOWN
        """.trimIndent()

    companion object {
        const val MODEL_PATH = "/data/local/tmp/llm/model.task"

        @Volatile
        private var instance: TimeParsingLlm? = null

        fun getInstance(context: Context): TimeParsingLlm =
            instance ?: synchronized(this) {
                instance ?: MediaPipeTimeParsingLlm(context.applicationContext).also { instance = it }
            }
    }
}
