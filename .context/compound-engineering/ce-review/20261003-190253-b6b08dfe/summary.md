# Code Review Summary — baby-feed (Units 1-6)

Run: see metadata.json in this directory.
Scope: base 0287d81 -> HEAD da27ce2 (full implementation of docs/plans/2026-10-02-001-feat-baby-feeding-tracker-plan.md)
Reviewers: correctness, testing, maintainability, project-standards, agent-native-reviewer, learnings-researcher, security, performance, reliability, adversarial

## Findings fixed in this session

- **P1** Undo mechanism deleted whatever was "most recent" instead of the specific entry the banner referred to -> redesigned to bind undo to the logged entry's id with an 8s expiry window (WidgetActionHandler, LogNowAction, UndoLogAction, FeedWidget, new ClearUndoWorker). Added FeedRepository.getById().
- **P1** TimeParsingLlm.parse() only guarded "model file missing" -- a corrupt model/OOM/native failure would crash -> wrapped loadModel()+generateResponse() in try/catch(Throwable), mapped to ModelUnavailable.
- **P1** BackdateViewModel.onConfirm() had no try/catch around logFeed() (clock-skew crash risk) -> added, matching HistoryViewModel's existing convention.
- **P2** MediaPipeTimeParsingLlm's lazy model load wasn't synchronized (race/leak risk) -> added a Mutex.
- **P2** AndroidManifest allowBackup="true" with no exclude rules (Auto Backup could leak feed.db off-device) -> set allowBackup="false", removed the now-unused backup_rules.xml/data_extraction_rules.xml.
- **P2** SpeechCaptureController: no timeout if the recognizer never calls back -> added withTimeoutOrNull (15s). createOnDeviceSpeechRecognizer() wasn't guarded -> wrapped in try/catch.
- **P2** WidgetRefreshWorker.doWork() had no try/catch/logging -> added, returns Result.retry() on failure.
- **P3** FeedWidget's dependency on settings/ contradicted the plan's "single hub" diagram -> updated the plan doc to document it as a sanctioned exception instead of adding indirection.
- **P3** OnDeviceSttAvailabilityTest was a committed no-op probe that already served its purpose -> deleted.
- Added test coverage testing-reviewer flagged as missing: FeedDao update/getById, AndroidSpeechCaptureController's permission-denied path, HistoryViewModel's onEntriesChanged redraw trigger, SettingsViewModel's startup hydration.

## Verified after fixes

- 39 unit tests pass (was 34).
- 12 instrumented tests pass on a real Samsung Galaxy S21 device (was 7).
- Full debug APK builds and installs; app launches without crashing.

## Deliberately not fixed (tracked as residual, not blocking)

- history/HistoryViewModel.kt: concurrent widget-undo + History edit on the same row can silently no-op (Room @Update on a missing row). Low-probability, non-corrupting (just a missed edit), acceptable for a personal single-device app.
- RefreshWorkerTest/ClearUndoWorkerTest can't assert on real rendered widget state -- FeedWidget.provideGlance() calls the production FeedRepository singleton directly rather than an injectable one, and no widget is pinned to a home screen in the instrumented-test environment. Making FeedWidget's data source injectable would be the real fix; deferred as out of scope for this pass.
- Agent-native-reviewer's two observations (no external/IPC entry to the widget action; no model-status query function) -- both genuinely non-blocking for a personal hackathon app.
