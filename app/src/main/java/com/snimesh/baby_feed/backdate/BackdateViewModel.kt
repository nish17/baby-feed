package com.snimesh.baby_feed.backdate

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import com.snimesh.baby_feed.data.FeedRepository
import com.snimesh.baby_feed.widget.FeedWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The sole orchestrator of the backdate flow: it alone calls SpeechCaptureController,
 * TimeParsingLlm, and FeedRepository on this path (see Key Technical Decisions). BackdateActivity
 * only renders state.
 *
 * onEntrySaved is injected (rather than calling FeedWidget().updateAll() directly) so this class
 * stays a plain JVM unit test target -- the widget redraw is a thin Android-specific side effect,
 * same split as WidgetActionHandler / LogNowAction.
 */
class BackdateViewModel(
    private val repository: FeedRepository,
    private val speechCaptureController: SpeechCaptureController,
    private val timeParsingLlm: TimeParsingLlm,
    private val onEntrySaved: suspend () -> Unit = {},
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackdateUiState>(BackdateUiState.Idle)
    val uiState: StateFlow<BackdateUiState> = _uiState.asStateFlow()

    fun onSpeakClicked() {
        _uiState.value = BackdateUiState.Listening
        viewModelScope.launch {
            when (val result = speechCaptureController.startListening()) {
                is SpeechCaptureResult.Transcribed -> parseAndTransition(result.text)
                SpeechCaptureResult.Unavailable,
                SpeechCaptureResult.PermissionDenied,
                SpeechCaptureResult.RecognitionError,
                -> openManualPicker()
            }
        }
    }

    fun onTypedTextSubmitted(text: String) {
        viewModelScope.launch { parseAndTransition(text) }
    }

    fun onManualEntryClicked() {
        openManualPicker()
    }

    fun onManualTimeSelected(timestampMillis: Long) {
        _uiState.value = BackdateUiState.Confirming(timestampMillis)
    }

    fun onConfirm(timestampMillis: Long) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                repository.logFeed(timestampMillis = timestampMillis, nowMillis = now)
                onEntrySaved()
            } catch (rejected: IllegalArgumentException) {
                // A backward clock step between picker-open and confirm time could flip the
                // "not in the future" check; same silent-reject convention as HistoryViewModel
                // rather than letting it crash (found by code review).
            }
            _uiState.value = BackdateUiState.Idle
        }
    }

    fun onUndo() {
        _uiState.value = BackdateUiState.Idle
    }

    private suspend fun parseAndTransition(text: String) {
        _uiState.value = BackdateUiState.Parsing
        when (val result = timeParsingLlm.parse(text, System.currentTimeMillis())) {
            is TimeParseResult.Parsed -> _uiState.value = BackdateUiState.Confirming(result.timestampMillis)
            TimeParseResult.Unparseable, TimeParseResult.ModelUnavailable -> openManualPicker()
        }
    }

    private fun openManualPicker() {
        _uiState.value = BackdateUiState.ManualPicker(nowMillis = System.currentTimeMillis())
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                return BackdateViewModel(
                    repository = FeedRepository.getInstance(appContext),
                    speechCaptureController = AndroidSpeechCaptureController(appContext),
                    timeParsingLlm = MediaPipeTimeParsingLlm.getInstance(appContext),
                    onEntrySaved = { FeedWidget().updateAll(appContext) },
                ) as T
            }
        }
    }
}
