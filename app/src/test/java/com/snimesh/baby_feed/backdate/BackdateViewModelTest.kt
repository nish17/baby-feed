package com.snimesh.baby_feed.backdate

import com.snimesh.baby_feed.MainDispatcherRule
import com.snimesh.baby_feed.data.FakeFeedRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BackdateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun speak_whenTranscribedAndParsed_entersConfirming() = runTest {
        val now = 10_000_000L
        val viewModel = BackdateViewModel(
            repository = FakeFeedRepository(),
            speechCaptureController = FakeSpeechCaptureController(SpeechCaptureResult.Transcribed("she fed at 1:30")),
            timeParsingLlm = FakeTimeParsingLlm(TimeParseResult.Parsed(now)),
        )

        viewModel.onSpeakClicked()

        assertEquals(BackdateUiState.Confirming(now), viewModel.uiState.value)
    }

    @Test
    fun speak_whenOnDeviceSttUnavailable_opensManualPicker() = runTest {
        val viewModel = BackdateViewModel(
            repository = FakeFeedRepository(),
            speechCaptureController = FakeSpeechCaptureController(SpeechCaptureResult.Unavailable),
            timeParsingLlm = FakeTimeParsingLlm(TimeParseResult.Unparseable),
        )

        viewModel.onSpeakClicked()

        assertTrue(viewModel.uiState.value is BackdateUiState.ManualPicker)
    }

    @Test
    fun speak_whenPermissionDenied_opensManualPicker() = runTest {
        val viewModel = BackdateViewModel(
            repository = FakeFeedRepository(),
            speechCaptureController = FakeSpeechCaptureController(SpeechCaptureResult.PermissionDenied),
            timeParsingLlm = FakeTimeParsingLlm(TimeParseResult.Unparseable),
        )

        viewModel.onSpeakClicked()

        assertTrue(viewModel.uiState.value is BackdateUiState.ManualPicker)
    }

    @Test
    fun speak_whenRecognizerErrors_opensManualPicker() = runTest {
        val viewModel = BackdateViewModel(
            repository = FakeFeedRepository(),
            speechCaptureController = FakeSpeechCaptureController(SpeechCaptureResult.RecognitionError),
            timeParsingLlm = FakeTimeParsingLlm(TimeParseResult.Unparseable),
        )

        viewModel.onSpeakClicked()

        assertTrue(viewModel.uiState.value is BackdateUiState.ManualPicker)
    }

    @Test
    fun typedText_whenUnparseable_opensManualPicker() = runTest {
        val viewModel = BackdateViewModel(
            repository = FakeFeedRepository(),
            speechCaptureController = FakeSpeechCaptureController(SpeechCaptureResult.RecognitionError),
            timeParsingLlm = FakeTimeParsingLlm(TimeParseResult.Unparseable),
        )

        viewModel.onTypedTextSubmitted("gibberish")

        assertTrue(viewModel.uiState.value is BackdateUiState.ManualPicker)
    }

    @Test
    fun typedText_whenModelFileMissing_opensManualPicker() = runTest {
        val viewModel = BackdateViewModel(
            repository = FakeFeedRepository(),
            speechCaptureController = FakeSpeechCaptureController(SpeechCaptureResult.RecognitionError),
            timeParsingLlm = FakeTimeParsingLlm(TimeParseResult.ModelUnavailable),
        )

        viewModel.onTypedTextSubmitted("she fed at 1:30")

        assertTrue(viewModel.uiState.value is BackdateUiState.ManualPicker)
    }

    @Test
    fun manualEntry_selectingATime_entersConfirming() = runTest {
        val viewModel = BackdateViewModel(
            repository = FakeFeedRepository(),
            speechCaptureController = FakeSpeechCaptureController(SpeechCaptureResult.Unavailable),
            timeParsingLlm = FakeTimeParsingLlm(TimeParseResult.Unparseable),
        )
        viewModel.onManualEntryClicked()
        val pickerState = viewModel.uiState.value as BackdateUiState.ManualPicker
        val selected = pickerState.nowMillis - 60_000L

        viewModel.onManualTimeSelected(selected)

        assertEquals(BackdateUiState.Confirming(selected), viewModel.uiState.value)
    }

    @Test
    fun confirm_savesEntryAndTriggersWidgetRedraw() = runTest {
        val repository = FakeFeedRepository()
        var redrawTriggered = false
        val viewModel = BackdateViewModel(
            repository = repository,
            speechCaptureController = FakeSpeechCaptureController(SpeechCaptureResult.Unavailable),
            timeParsingLlm = FakeTimeParsingLlm(TimeParseResult.Unparseable),
            onEntrySaved = { redrawTriggered = true },
        )
        val timestamp = System.currentTimeMillis() - 60_000L

        viewModel.onConfirm(timestamp)

        assertEquals(timestamp, repository.mostRecent()?.timestampMillis)
        assertTrue(redrawTriggered)
        assertEquals(BackdateUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun undo_discardsWithoutSaving() = runTest {
        val repository = FakeFeedRepository()
        val viewModel = BackdateViewModel(
            repository = repository,
            speechCaptureController = FakeSpeechCaptureController(SpeechCaptureResult.Unavailable),
            timeParsingLlm = FakeTimeParsingLlm(TimeParseResult.Unparseable),
        )

        viewModel.onUndo()

        assertNull(repository.mostRecent())
        assertEquals(BackdateUiState.Idle, viewModel.uiState.value)
    }
}
