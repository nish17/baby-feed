package com.snimesh.baby_feed.backdate

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
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
class MediaPipeTimeParsingLlm(
    private val context: Context,
    private val modelPath: String = MODEL_PATH,
) : TimeParsingLlm {

    @Volatile
    private var llmInference: LlmInference? = null
    private val loadMutex = Mutex()

    override suspend fun parse(text: String, nowMillis: Long): TimeParseResult =
        withContext(Dispatchers.IO) {
            if (!File(modelPath).exists()) {
                return@withContext TimeParseResult.ModelUnavailable
            }
            try {
                val llm = getOrLoadModel()
                val prompt = buildTimeParsingPrompt(text)
                // A try/catch cannot protect against exceeding maxTokens (see loadModel's
                // comment) -- the only real defense is checking before the call, not after.
                if (llm.sizeInTokens(prompt) > MAX_PROMPT_TOKENS) {
                    return@withContext TimeParseResult.ModelUnavailable
                }
                // A fresh session per call, not the implicit session LlmInference.generateResponse()
                // would use -- the implicit session defaults to temperature 0.8 (tuned for varied
                // creative text), which is a worse fit for this now-pure classification task than
                // greedy/argmax-leaning low-temperature decoding.
                val response = LlmInferenceSession.createFromOptions(llm, sessionOptions()).use { session ->
                    session.addQueryChunk(prompt)
                    session.generateResponse()
                }
                resolveStructuredTimeResponse(response, nowMillis)
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

    private fun sessionOptions(): LlmInferenceSession.LlmInferenceSessionOptions =
        LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTemperature(0.1f)
            .setTopK(40)
            .build()

    private fun loadModel(): LlmInference {
        // maxTokens covers input + output combined. Found by real on-device testing: exceeding
        // it doesn't throw a catchable exception -- it's a native OUT_OF_RANGE error that
        // triggers a JNI contract violation ("NewByteArray called with pending exception"),
        // which ART detects and responds to by aborting the whole process (SIGABRT). No amount
        // of Kotlin try/catch -- even catch (Throwable) -- can intercept that. The few-shot
        // prompt in buildTimeParsingPrompt tokenizes to ~300 tokens on its own, so the previous
        // value of 256 was already too tight even before the examples were added.
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(512)
            .setMaxTopK(40)
            .build()
        return LlmInference.createFromOptions(context, options)
    }

    companion object {
        const val MODEL_PATH = "/data/local/tmp/llm/model.task"

        /** Reserves ~100 tokens of the 512 maxTokens budget for the model's output. */
        private const val MAX_PROMPT_TOKENS = 412

        @Volatile
        private var instance: TimeParsingLlm? = null

        fun getInstance(context: Context): TimeParsingLlm =
            instance ?: synchronized(this) {
                instance ?: MediaPipeTimeParsingLlm(context.applicationContext).also { instance = it }
            }
    }
}
