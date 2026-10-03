---
title: feat: Baby Feeding Tracker — Android Widget with On-Device AI Backdating
type: feat
status: active
date: 2026-10-02
origin: docs/brainstorms/baby-feeding-tracker-widget-requirements.md
deepened: 2026-10-02
---

# feat: Baby Feeding Tracker — Android Widget with On-Device AI Backdating

## Overview

A native Android (Kotlin) app and home-screen widget that lets a sleep-deprived new father log his baby's feedings with a single tap, see at a glance when the next feeding is due, and — the AI-core piece — log a *forgotten past* feed by speaking or typing naturally ("she fed at 1:30"), parsed into a timestamp by a small open-weight LLM running entirely on-device. No feeding data, voice audio, or text ever leaves the phone; there is no backend, no sync, and no network call anywhere in the logging path.

This is a weekend build for Hacktoberfest 2026 ("Build for a Friend" / "open-source AI at its core") by someone with zero prior Android development experience. The plan is explicitly sequenced so scope can be cut safely if the weekend runs short (see Priority Order, carried from the origin doc).

## Problem Frame

The father currently tracks feedings in an Obsidian note: opening the app, finding the last entry, and doing mental math to know when to feed next. This plan builds a replacement that surfaces "last fed" / "next feeding" / "time remaining" directly on the Android home screen, with one tap for the common case and natural-language/voice entry — backed by an on-device LLM — for the less common case of logging a feed he forgot to record earlier. See origin doc for full problem framing and Hacktoberfest context.

## Requirements Trace

- R1. Widget single-tap "Log feed now" action.
- R2. App-based natural-language text/voice backdating, parsed by an on-device LLM.
- R3. Confirmation (with undo) before saving a parsed backdated time.
- R4. Both speech-to-text and time parsing run entirely on-device; no network call, no cloud fallback for either step.
- R5. Widget displays last feed time, next feed time, time remaining.
- R6. Next feed time = last feed + configurable interval.
- R7. In-app screen to edit/delete past entries.
- R8. Settings screen to configure the feeding interval.
- R9. Widget shows an explicit overdue indicator once next-feed time passes.
- R10. Widget shows an empty-state prompt before any feed has ever been logged.
- R11. Manual date/time-picker fallback when parsing fails or on-device STT is unavailable.

## Scope Boundaries

(Carried from origin doc — see `docs/brainstorms/baby-feeding-tracker-widget-requirements.md` for full rationale.)

- Feeding only — no diaper, medicine, or massage tracking.
- Android only, targeting one specific device (father's Samsung Galaxy S23+). No iOS/Flutter, no cross-device sync.
- No backend, no shared/multi-user state. Single device, single baby profile.
- No push notifications — the widget's at-a-glance display is the only due-time signal.
- Feed entries capture time only — no amount, type, duration, or side.
- Not published to Google Play — sideloaded (personally built/signed) APK, installed via USB debugging.

### Deferred to Separate Tasks

- Migrating from MediaPipe's `tasks-genai` to LiteRT-LM: explicitly deferred past this weekend (see Key Technical Decisions). Only revisit if the project continues past Hacktoberfest.
- Mom's iPhone — not in scope for this plan at all (per origin doc, not even a tracked stretch goal).

## Context & Research

### Relevant Code and Patterns

None — this repository is currently empty aside from `docs/brainstorms/` and `.claude/` (confirmed via repo research: no git history, no Gradle/Android project, no source files of any kind). Every technical decision below is a first decision for this codebase, not a convention match.

### External References

- MediaPipe LLM Inference API (Android/Kotlin): `com.google.mediapipe:tasks-genai` — officially in **maintenance-only mode**; Google's recommended successor is LiteRT-LM. Key classes: `LlmInference`, `LlmInferenceOptions` (builder: `setModelPath`, `setMaxTokens`, `setTopK`, `setTemperature`), `generateResponse(prompt)` / `generateResponseAsync(prompt, callback)`. `minSdkVersion 24`. Does not reliably run on emulators — requires a real device. Source: `developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android`, sample code at `github.com/google-ai-edge/mediapipe-samples/examples/llm_inference/android`.
- Model: **Gemma 3 1B, int4 quantization-aware-trained (QAT)**, ~529MB `.task` bundle, from Hugging Face (`litert-community/Gemma3-1B-IT` or `google/gemma-3-1b-it`). Gemma models require a one-time Hugging Face (or Kaggle) account license click-through before download. Licensed under Google's "Gemma Terms of Use" (not OSI-approved, but permissive for this use including redistribution-notice obligations if the model file itself is redistributed — not applicable here since it's sideloaded to one personal device).
- **Model is too large to bundle in the APK** per MediaPipe's own docs. Recommended dev path: `adb push` the `.task` file to the device (e.g., `/data/local/tmp/llm/model.task`) and point `setModelPath()` there — explicitly sanctioned by Google's docs as "a simpler workflow" for exactly this kind of use case.
- **Samsung Galaxy S23+ does not support Gemini Nano / AICore** (Samsung's supported list starts at Galaxy S24). This rules out the simpler ML Kit GenAI Prompt API shortcut — confirms the plan must bundle and run its own model via MediaPipe.
- Jetpack Glance (`androidx.glance:glance-appwidget`), current stable 1.2.0, default `minSdk 23`. `GlanceAppWidget` + `GlanceAppWidgetReceiver` for the widget; `actionRunCallback<MyAction>()` + `ActionCallback` for tap handling (must have a public zero-arg constructor); `GlanceAppWidgetManager` / `MyAppWidget().updateAll(context)` to force an out-of-cycle redraw (suspend function — call from a coroutine, e.g. from inside the `ActionCallback`). Source: `developer.android.com/develop/ui/compose/glance/*`.
- **`AppWidgetProviderInfo.updatePeriodMillis` has a hard floor of 30 minutes** — the system silently clamps anything lower. `PeriodicWorkRequest` (WorkManager) has its own floor of 15 minutes. Neither supports a truly live countdown on its own.
- `RemoteViews.setChronometer()` / `setChronometerCountDown()` ticks on-device via `SystemClock.elapsedRealtime()` with no further updates needed once the base time is pushed — the best mechanism for a live-ticking countdown, if reachable through Glance's current composable set (flagged as needing a quick spike in Unit 2 — Glance may not expose `Chronometer` directly, in which case fall back to event-driven `updateAll()` + a 15-minute WorkManager safety net).
- `SpeechRecognizer.createOnDeviceSpeechRecognizer(context)` + `isOnDeviceRecognitionAvailable(context)` + `checkRecognitionSupport(...)` — all API 31+ (Android 12), guarantee on-device-only recognition when available. `RecognizerIntent.EXTRA_PREFER_OFFLINE` (API 23+) is only an advisory hint on the general recognizer and may be silently ignored by the OEM. **Documented OEM inconsistency**: Samsung devices have been observed where the OS's own dictation works but the app-facing `isOnDeviceRecognitionAvailable()` call returns `false` anyway — this is a real risk to validate early (Unit 3), not just a theoretical edge case.
- Sideloaded APKs (installed via `adb install`, not Play) have **no Play-enforced size cap** — only device storage matters. Play's 150-500MB base-APK limits are irrelevant to this build.

## Key Technical Decisions

- **MediaPipe `tasks-genai`, not LiteRT-LM** (user decision): chosen for the weekend build despite being the deprecated/maintenance-only path, because it has far more tutorials, sample code, and community troubleshooting available for a first-time Android developer under a hard deadline. Documented as known migration debt — revisit LiteRT-LM only if the project continues past the hackathon.
- **Gemma 3 1B, int4 QAT (~529MB)**: the model Google specifically publishes for mobile use with MediaPipe; realistic latency/memory footprint on a Snapdragan 8 Gen 2 (S23+) for a short extraction prompt. A 2B+ model is not recommended given the device and time budget.
- **Model provisioning via `adb push`, not Gradle assets**: bundling the ~500MB model as an APK asset would make every Gradle build/install cycle multi-minute over USB, which is unacceptable for a one-weekend iteration loop. The model is pushed once to `/data/local/tmp/llm/model.task` and `TimeParsingLlm` loads it directly from that path for the life of this project — there is no separate "copy to app-private storage" step to build; that would be a second, unowned provisioning path with no corresponding unit, and this plan commits to the simpler one. Operational tradeoff: if the phone is factory-reset or storage is cleared, the model must be re-pushed before AI backdating works again — the app should detect this (model file missing) and route straight to R11's manual-picker fallback rather than crashing.
- **`minSdk = 31` (Android 12)**: this targets one specific personal device (the father's Samsung S23+), so there is no back-compat burden. API 31 is required for `createOnDeviceSpeechRecognizer`/`isOnDeviceRecognitionAvailable`, so setting the floor there (rather than the lower 24 MediaPipe technically allows) avoids writing and testing a pre-31 STT code path that will never run on the target device.
- **Jetpack Glance for the widget, not classic `AppWidgetProvider`/`RemoteViews`**: Glance's Compose-like DSL and `ActionCallback` model are substantially more approachable for a first-time Android developer than hand-writing `RemoteViews` + `PendingIntent` wiring, while still being Google's current recommended widget API.
- **Widget live-countdown approach**: attempt a `RemoteViews.Chronometer` interop for a truly ticking display (Unit 2 includes an early spike to confirm Glance can expose this). If that proves infeasible within Glance's current API, fall back to immediate `updateAll()` on every log event plus a 15-minute `WorkManager` periodic refresh as a backstop — accepting coarser between-tap staleness, which is acceptable since this isn't safety-critical.
- **No DI framework (no Hilt/Koin)**: manual constructor wiring / simple singleton accessors for the repository and LLM/STT controllers, to avoid adding another new concept on top of Kotlin + Android + Glance + MediaPipe + SpeechRecognizer for a first-time Android developer in one weekend. A lightweight `ViewModel` + `StateFlow` per screen is still used where natural, since it's the standard idiomatic pattern most current Android docs/samples assume.
- **Testing relies only on official, idiomatic libraries (Turbine, `glance-testing`, `work-testing`), not custom test infrastructure** — these carry real learning cost for a first-time Android developer same as any new library, but they're a different kind of cost than a DI framework: each is narrowly scoped to one unit's tests, optional to use (a plain `.value`/`toList()` assertion works without Turbine), and skippable in favor of manual verification without blocking a unit's feature code. **Automated test coverage is droppable under the same time pressure as feature scope**: if a unit's test setup stalls, fall back to manual on-device verification for that unit and keep moving — `NextFeedingCalculatorTest` and `BackdateViewModelTest` are the cheapest, highest-value tests (pure JVM, no Android/Glance/Robolectric dependency) and are worth protecting first if the weekend gets tight; `WidgetContentTest`, `RefreshWorkerTest`, and `FeedDaoTest` are the safest to cut.
- **`BackdateActivity` is the app's `LAUNCHER` activity**: it offers the backdating entry points plus simple navigation (e.g., a menu or buttons) to `HistoryActivity` and `SettingsActivity`. No separate `MainActivity`/hub screen is introduced — consistent with the "no new abstraction without a need" approach used elsewhere in this plan (see No DI framework, above).
- **The widget's post-tap undo affordance is rendered inline within `WidgetContent` itself, not via a system `Toast` or a `Snackbar`**: a plain `Toast` has no tappable action button, and a `Snackbar` needs a Compose/Activity host that a Glance `ActionCallback` doesn't have. Instead, `WidgetUiState` carries a short-lived "just logged — undo?" flag that `WidgetContent` renders as an inline clickable "Undo" element for a few seconds after a tap, cleared on the next redraw or a brief delayed re-render. This stays entirely within Glance's existing render path rather than introducing a new UI surface.
- **Room (SQLite) for persistence**: a single `FeedEntry` table (timestamp only, per R per origin doc's scope boundary). No backend, no sync — this is itself part of the privacy pitch, not a gap.
- **STT fails closed, not open**: if `isOnDeviceRecognitionAvailable()` returns false (or the on-device recognizer errors), the app does **not** fall back to network-based speech recognition — it routes directly to R11's manual date/time-picker. This keeps the "nothing leaves the device" claim actually true rather than silently true-only-in-the-common-case.
- **"Last feed" = max(timestamp) across stored entries, not the most-recently-edited row.** Entries (new or edited via R7) cannot be timestamped later than the current time — validated at save time. This resolves an ambiguity the flow analysis flagged: without this rule, editing an older entry could otherwise be misread as "the new last feed."
- **Changing the feeding interval (R8) recomputes next-feed-time immediately** against the existing last-feed timestamp — it applies live to whatever countdown/overdue state is currently showing, not just to feeds logged after the change. Simplest mental model for the father.
- **R1 gets a brief inline undo affordance** (rendered within `WidgetContent` itself — see the undo-mechanism decision above — mirroring R3's confirm-before-commit pattern for backdating) specifically to cover an accidental double-tap. This was not in the original R1 wording; it closes a gap the flow analysis raised — without it, R7 (history edit/delete) is the *only* way to fix a mis-tap. This covers the mis-tap case specifically; it does not cover noticing, after the fact, that a confirmed backdated entry was parsed or picked wrong (see the R7-droppability open question below — that gap is narrower than "R7 is safely droppable" implies).
- **Deleting all entries reverts the widget to R10's empty state**; deleting only the newest entry falls back to whatever the next most recent remaining entry is, per the max-timestamp rule above — this falls out naturally from the data model rather than needing special-case logic.
- **`BackdateViewModel` is the sole orchestrator of the backdate flow**: it alone calls `SpeechCaptureController`, `TimeParsingLlm`, and `FeedRepository` on this path; `BackdateActivity` only renders state. `LogNowAction` (a Glance `ActionCallback`, which requires a public zero-arg constructor and cannot bind to an Activity-scoped `ViewModel`) is an intentional, documented exception to this ViewModel-mediated shape — not an inconsistency to "fix" later.
- **The manual date/time-picker path also routes through R3's confirm/undo step before saving**, not directly to the repository. This keeps the undo affordance uniform across every save path (LLM-parsed or manually picked) rather than only protecting the AI-parsed case.
- **`TimeParsingLlm` (and the ~529MB model it wraps) is an application-scoped singleton, lazily loaded once on first backdate use and kept warm thereafter** — reloading the model per-screen would add several seconds of latency to every single backdating attempt, not just the first.
- **Non-ViewModel call sites (`LogNowAction`, `WidgetRefreshWorker`) access `FeedRepository` via a lazy singleton accessor keyed on `applicationContext`**, consistent with the "no DI framework" decision above — this is the concrete mechanism behind that decision, not a separate one.

## High-Level Technical Design

> *This illustrates the intended approach and is directional guidance for review, not implementation specification. The implementing agent should treat it as context, not code to reproduce.*

**Component boundaries.** A single `data` package (Room entities/DAO/repository/calculator) has zero outbound dependencies — every other package depends only on it, never on each other:

```
        widget/ ---\
     backdate/ -----> data/  (FeedRepository, FeedDatabase, NextFeedingCalculator)
      history/ -----> ^
     settings/ ------/
```

`FeedRepository` is the single hub: three inbound write paths (`LogNowAction` from the widget, the backdate save step, History edit/delete) and one outbound effect (`FeedWidget().updateAll()`) fan out from it. This is what keeps R5/R6/R9/R10 consistent regardless of which screen produced the change (see System-Wide Impact) — and it's checkable directly from this diagram rather than by cross-referencing four units' prose.

**Backdate flow (Units 3-4), sequence:**

```
BackdateActivity -> BackdateViewModel.captureSpeechOrText()
  BackdateViewModel -> Listening (speak) | TypedEntry (type) | ManualPicker (manual)
  Listening -> SpeechCaptureController -> BackdateViewModel: transcribed text | routes to ManualPicker on failure
  (transcribed text | typed text) -> BackdateViewModel enters Parsing
  Parsing -> TimeParsingLlm.parse(text, now) -> TimeParseResult
    Parsed(timestamp)     -> BackdateViewModel enters Confirming
    Unparseable           -> routes to ManualPicker
    model file missing    -> routes to ManualPicker
  ManualPicker (any entry route) -> user picks a time -> BackdateViewModel enters Confirming
  Confirming -> user confirms -> BackdateViewModel -> FeedRepository.insert() -> FeedWidget.updateAll()
  Confirming -> user undoes  -> discard, back to Idle
```

This is the single source of truth for the backdate flow's sequencing — Unit 4's own "Technical design" block only points back here rather than restating it, to avoid two descriptions of the same flow drifting out of sync.

`Listening` and `Parsing` are explicit, renderable states, not just transient calls: cold-loading the ~529MB model can take several seconds (see the `TimeParsingLlm` singleton decision), so `BackdateActivity` must show a visible "listening…" / "thinking…" state during each rather than appearing frozen — a generic progress indicator is sufficient, no bespoke UI needed.

**Fails-closed state machine.** Five distinct failure triggers (on-device recognition unavailable, `RECORD_AUDIO` denied, recognizer error, LLM output unparseable, model file missing) all converge on the same `ManualPicker` state rather than five near-duplicate fallback implementations — and `ManualPicker` itself always re-enters `Confirming` before a save, per the decision above, closing what would otherwise be an unresolved "does the manual picker skip confirmation?" gap.

**Ownership / lifecycle:**

| Component | Scope/Lifetime | Accessed by |
|---|---|---|
| `FeedRepository` / `FeedDatabase` | Application-scoped singleton, lazy accessor on `applicationContext` | `LogNowAction`, `WidgetRefreshWorker`, `BackdateViewModel`, `HistoryViewModel`, `SettingsViewModel` |
| `TimeParsingLlm` / `LlmInference` | Application-scoped singleton, lazily loaded on first backdate use, kept warm | `BackdateViewModel` only |

## Open Questions

### Resolved During Planning

- MediaPipe vs. LiteRT-LM: MediaPipe `tasks-genai`, per user decision (see Key Technical Decisions).
- `minSdk` target: 31, since this targets one known device.
- Widget update mechanism: Chronometer-first with WorkManager/event-driven fallback (see Key Technical Decisions); final choice confirmed during Unit 2's spike.
- Last-feed-after-edit and interval-change-mid-countdown semantics: resolved as explicit rules above (flagged by flow analysis as previously undefined).
- STT fail-open vs. fail-closed: fails closed to the manual picker, consistent with the on-device-only privacy requirement.
- R7 (History) droppability gap: accepted as a documented weekend-scope tradeoff (user decision) rather than making Unit 5 non-droppable or adding a cheaper substitute affordance (see Unit 5 Goal and Risks & Dependencies).

### Deferred to Implementation

- Whether Glance's current composable set can host a `RemoteViews.Chronometer` directly, or whether a lower-level RemoteViews interop (or the WorkManager-only fallback) is needed — resolve via the Unit 2 spike, not by guessing now.
- Exact prompt template and output-parsing strategy for the LLM time-extraction step (MediaPipe's `tasks-genai` doesn't support schema-constrained structured output, so the app must parse a free-text model response defensively) — exact prompt wording and validation thresholds are an implementation-time tuning exercise, not a planning decision.
- Whether on-device speech recognition (`isOnDeviceRecognitionAvailable()`) actually returns `true` on the specific test device, given documented Samsung OEM inconsistency — must be verified against real hardware early in Unit 3, with R11's manual fallback as the expected outcome if it does not.
- Minor edge-case polish not required for a working weekend demo: widget resize/minimum-size layout behavior, system clock-skew/DST handling for the countdown, interval min/max validation bounds, and distinguishing "mic permission denied" from "on-device engine unavailable" in the STT fallback UX (both currently route to the same R11 fallback, which is sufficient for now).
- Whether `runGlanceAppWidgetUnitTest` requires Robolectric configuration (shadowed Android framework classes, `testOptions.unitTests.isIncludeAndroidResources`, first-run SDK jar downloads) for this Glance/AGP version combination — if so, Unit 1 or Unit 2 needs that Gradle config added; if it stalls, fall back to manual verification for `WidgetContentTest` per the test-coverage-is-droppable decision (see Key Technical Decisions).

## Output Structure

    baby-feed/
    ├── app/
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── main/
    │       │   ├── AndroidManifest.xml
    │       │   ├── java/com/nish/babyfeed/
    │       │   │   ├── data/
    │       │   │   │   ├── FeedEntry.kt
    │       │   │   │   ├── FeedDao.kt
    │       │   │   │   ├── FeedDatabase.kt
    │       │   │   │   ├── FeedRepository.kt
    │       │   │   │   └── NextFeedingCalculator.kt
    │       │   │   ├── widget/
    │       │   │   │   ├── FeedWidget.kt
    │       │   │   │   ├── FeedWidgetReceiver.kt
    │       │   │   │   ├── WidgetContent.kt
    │       │   │   │   ├── LogNowAction.kt
    │       │   │   │   ├── WidgetActionHandler.kt
    │       │   │   │   └── WidgetRefreshWorker.kt
    │       │   │   ├── backdate/
    │       │   │   │   ├── BackdateActivity.kt
    │       │   │   │   ├── SpeechCaptureController.kt
    │       │   │   │   ├── TimeParsingLlm.kt
    │       │   │   │   ├── TimeParseResult.kt
    │       │   │   │   └── BackdateViewModel.kt
    │       │   │   ├── history/
    │       │   │   │   ├── HistoryActivity.kt
    │       │   │   │   └── HistoryViewModel.kt
    │       │   │   └── settings/
    │       │   │       ├── SettingsActivity.kt
    │       │   │       └── SettingsViewModel.kt
    │       │   └── res/
    │       │       └── xml/feed_widget_info.xml
    │       ├── test/java/com/nish/babyfeed/
    │       │   ├── data/NextFeedingCalculatorTest.kt
    │       │   ├── widget/WidgetContentTest.kt
    │       │   ├── widget/WidgetActionHandlerTest.kt
    │       │   ├── backdate/TimeParseResultTest.kt
    │       │   ├── backdate/BackdateViewModelTest.kt
    │       │   ├── history/HistoryViewModelTest.kt
    │       │   └── settings/SettingsViewModelTest.kt
    │       └── androidTest/java/com/nish/babyfeed/
    │           ├── data/FeedDaoTest.kt
    │           └── widget/RefreshWorkerTest.kt
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradle.properties
    └── README.md

This is directional scope, not a constraint — adjust package/file names as implementation reveals a better shape.

## Implementation Units

- [x] **Unit 1: Project Scaffolding & Local Data Layer**

**Goal:** Stand up the Android project (Gradle, Kotlin, `minSdk 31`, Room) and the persistence layer everything else depends on.

**Requirements:** R5, R6, R9, R10 (data foundation for the widget display logic)

**Dependencies:** None — this is the first unit.

**Files:**
- Create: `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- Create: `README.md` (documents the one-time Gemma model setup: Hugging Face/Kaggle license click-through, then `adb push model.task /data/local/tmp/llm/model.task` — the concrete mitigation for the model-provisioning risk in Risks & Dependencies)
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/nish/babyfeed/data/FeedEntry.kt`
- Create: `app/src/main/java/com/nish/babyfeed/data/FeedDao.kt`
- Create: `app/src/main/java/com/nish/babyfeed/data/FeedDatabase.kt`
- Create: `app/src/main/java/com/nish/babyfeed/data/FeedRepository.kt`
- Create: `app/src/main/java/com/nish/babyfeed/data/NextFeedingCalculator.kt`
- Test: `app/src/androidTest/java/com/nish/babyfeed/data/FeedDaoTest.kt`
- Test: `app/src/test/java/com/nish/babyfeed/data/NextFeedingCalculatorTest.kt`

**Approach:**
- `FeedEntry`: a single Room entity with `id`, `timestamp` (epoch millis), nothing else — matches the origin doc's "time only" scope boundary.
- `FeedRepository` wraps the DAO and exposes: insert, delete, list-all (as `Flow`), and "most recent entry" (= max timestamp — see Key Technical Decisions), rejecting any insert/update with a timestamp later than "now."
- `NextFeedingCalculator` is a small pure function: given a last-feed timestamp (nullable) and an interval, returns one of {no-data (R10), countdown-remaining (R5/R6), overdue-by (R9)}. Keeping this pure and separate from Android/Glance code makes it trivially unit-testable (plain JVM, no Room/Android dependency) and is the single source of truth the widget, and later the settings screen's live-apply behavior, both call into.

**Patterns to follow:**
- `Room.inMemoryDatabaseBuilder(...)` run with `AndroidJUnit4` for `FeedDaoTest` — real Room/SQLite behavior needs an instrumented test, not a plain JVM unit test, since `FeedRepositoryTest` as originally scoped was actually exercising Room, not pure logic.

**Test scenarios:**
- Happy path (instrumented, `FeedDaoTest`): insert a feed entry via an in-memory Room DB, read it back, "most recent" matches.
- Edge case (instrumented): inserting/updating an entry with a timestamp later than "now" is rejected.
- Edge case (instrumented): deleting the only entry returns to the no-data state; deleting the newest of several falls back to the next-most-recent.
- Happy path (unit, `NextFeedingCalculatorTest`): last-feed 1 hour ago and a 3-hour interval returns "2 hours remaining."
- Edge case (unit): no entries returns the no-data/empty state.
- Edge case (unit): current time past last-feed + interval returns overdue-by-X.

**Verification:**
- `NextFeedingCalculatorTest` (JVM) and `FeedDaoTest` (instrumented, physical device) both pass; app compiles and installs a blank Activity to a physical device via USB debugging.

---

- [ ] **Unit 2: Home-Screen Widget — Log & Display**

**Goal:** Ship the widget itself: one-tap logging with undo, and the last-feed/next-feed/countdown/overdue/empty display states.

**Requirements:** R1, R5, R6, R9, R10 (Priority Group 1 — the core, non-AI value)

**Dependencies:** Unit 1 (data layer)

**Files:**
- Create: `app/src/main/java/com/nish/babyfeed/widget/FeedWidget.kt`
- Create: `app/src/main/java/com/nish/babyfeed/widget/FeedWidgetReceiver.kt`
- Create: `app/src/main/java/com/nish/babyfeed/widget/WidgetContent.kt`
- Create: `app/src/main/java/com/nish/babyfeed/widget/LogNowAction.kt`
- Create: `app/src/main/java/com/nish/babyfeed/widget/WidgetActionHandler.kt`
- Create: `app/src/main/java/com/nish/babyfeed/widget/WidgetRefreshWorker.kt`
- Create: `app/src/main/res/xml/feed_widget_info.xml`
- Modify: `app/src/main/AndroidManifest.xml` (register the widget receiver)
- Test: `app/src/test/java/com/nish/babyfeed/widget/WidgetContentTest.kt`
- Test: `app/src/test/java/com/nish/babyfeed/widget/WidgetActionHandlerTest.kt`
- Test: `app/src/androidTest/java/com/nish/babyfeed/widget/RefreshWorkerTest.kt`

**Approach:**
- `FeedWidget` (`GlanceAppWidget`) stays thin — it delegates its content to a standalone `WidgetContent` composable (`WidgetContent(state: WidgetUiState, onLogClick: () -> Unit)`), which is what makes the widget's three display states (no-data/R10, countdown-remaining/R5-R6, overdue/R9) unit-testable at all (Glance's own content can't be unit-tested if it lives directly on the `GlanceAppWidget` subclass).
- `WidgetActionHandler` holds the actual "log an entry" business logic (insert via `FeedRepository`, set the short-lived "just logged — undo?" flag on `WidgetUiState`, then trigger `FeedWidget().updateAll(context)`) as a plain, DI-free class accessed via the lazy singleton accessor (see Key Technical Decisions) — kept separate from `LogNowAction` so the logic itself is a normal JVM unit test target, independent of Glance's `ActionCallback` machinery.
- `LogNowAction` (`ActionCallback`, public zero-arg constructor) is a thin shim that just calls into `WidgetActionHandler`; the undo affordance itself is rendered by `WidgetContent` reading that flag (see Key Technical Decisions), not by `LogNowAction` directly.
- `WidgetRefreshWorker`: a 15-minute `PeriodicWorkRequest` backstop so the overdue indicator (R9) still flips even with no user interaction, acknowledging Samsung's aggressive Doze/battery management may make this best-effort rather than exact.

**Execution note:** Start Unit 2 with a short, time-boxed spike (an hour, not a day) to confirm whether Glance can host a `RemoteViews.Chronometer` for a truly live countdown. If it can't be made to work quickly, fall back to the `updateAll()` + `WidgetRefreshWorker` approach described above rather than losing a day to it — this is exactly the kind of execution-time unknown the plan intentionally left open (see Open Questions).

**Patterns to follow:**
- `developer.android.com/develop/ui/compose/glance/create-app-widget` for `GlanceAppWidget`/`GlanceAppWidgetReceiver` structure.
- `developer.android.com/develop/ui/compose/glance/user-interaction` for `actionRunCallback`/`ActionCallback`.
- `androidx.glance:glance-testing` + `androidx.glance:glance-appwidget-testing`'s `runGlanceAppWidgetUnitTest` for unit-testing `WidgetContent` as a plain JVM test (no emulator) — query/assert API mirrors Compose UI testing (`onNode(hasTestTag(...))`, `.assertHasText(...)`, `.assertIsClickable()`). It does not simulate a real tap or run WorkManager, which is why `WidgetActionHandler` and `WidgetRefreshWorker` are tested separately.
- `androidx.work:work-testing`'s `TestListenableWorkerBuilder` for `RefreshWorkerTest` — this needs a real `Context`, so it's an instrumented test, not a JVM unit test.

**Test scenarios:**
- Happy path (unit, `WidgetContentTest`): given a countdown-remaining `WidgetUiState`, the composable renders the expected last-feed/next-feed/remaining text and a clickable "Log feed now" node.
- Happy path (unit, `WidgetContentTest`): given the no-data state, renders the R10 empty-state prompt instead.
- Edge case (unit, `WidgetContentTest`): given an overdue state, renders the R9 overdue indicator.
- Happy path (unit, `WidgetActionHandlerTest`): calling the handler inserts an entry via `FeedRepository` and requests a widget redraw.
- Edge case (unit, `WidgetActionHandlerTest`): double-invoking the handler in quick succession — the second call's undo flag supersedes the first (the first entry becomes final once the second affordance appears; it remains recoverable only via History, not via a stacked undo).
- Happy path (instrumented, `RefreshWorkerTest`): running the worker via `TestListenableWorkerBuilder` recomputes and applies the overdue state when the next-feed time has passed.
- Manual verification (not automated — Glance's unit-test API doesn't cover real rendering or taps): widget adds to the home screen, tap responsiveness, and the real 15-minute WorkManager cadence in production.
- Integration: deleting all entries (once Unit 5 exists) renders the R10 empty state via `WidgetContent`; this unit's display logic must already support rendering "zero entries" correctly so Unit 5 doesn't need widget-side changes.

**Verification:**
- `WidgetContentTest` and `WidgetActionHandlerTest` pass; `RefreshWorkerTest` passes on-device; widget is manually confirmed on the home screen to log a feed on tap and display all three states correctly.

---

- [ ] **Unit 3: Backdated Entry Capture — On-Device Speech-to-Text + Manual Fallback**

**Goal:** The app-side entry point for logging a past feed: capture either spoken or typed natural language, with a manual date/time-picker fallback when on-device STT isn't available or fails.

**Requirements:** R2 (entry point), R4 (on-device-only, fail closed), R11 (manual fallback)

**Dependencies:** Unit 1 (data layer, for eventually saving the parsed entry)

**Files:**
- Create: `app/src/main/java/com/nish/babyfeed/backdate/BackdateActivity.kt`
- Create: `app/src/main/java/com/nish/babyfeed/backdate/SpeechCaptureController.kt`
- Create: `app/src/main/java/com/nish/babyfeed/backdate/BackdateViewModel.kt`
- Modify: `app/src/main/AndroidManifest.xml` (`RECORD_AUDIO` permission, register `BackdateActivity`)
- Test: `app/src/test/java/com/nish/babyfeed/backdate/BackdateViewModelTest.kt`

**Approach:**
- `BackdateActivity` is the app's `LAUNCHER` activity (see Key Technical Decisions) and is also the hub for navigating to History and Settings — offers "speak" and "type" entry points plus a manual date/time-picker. `BackdateActivity` only renders state; `BackdateViewModel` is the sole orchestrator, calling `SpeechCaptureController` directly and holding the result on its way to the parsing step (Unit 4) — see High-Level Technical Design.
- `SpeechCaptureController` is declared as an interface (real implementation wrapping `SpeechRecognizer`, plus a hand-written fake for `BackdateViewModelTest`) — mirroring the `FeedRepository`/`SettingsRepository` fake pattern used elsewhere, so `BackdateViewModelTest` can exercise the routing logic (available/unavailable/error → `ManualPicker`) without touching the real Android `SpeechRecognizer`.
- The real implementation gates on `SpeechRecognizer.isOnDeviceRecognitionAvailable()` / `checkRecognitionSupport()` before attempting `createOnDeviceSpeechRecognizer()`. If unavailable, or if recognition errors out, it does **not** fall back to the network-backed recognizer — it surfaces the manual picker (R11) directly, consistent with R4's fail-closed decision.
- Request `RECORD_AUDIO` at the point of use (not at app launch); a denied permission routes to the same manual-picker fallback as an unavailable engine.

**Execution note:** Verify `isOnDeviceRecognitionAvailable()` against the actual target device as the very first thing in this unit, before writing the rest of the capture flow — this is a documented source of OEM inconsistency (see Context & Research) and determines whether voice input is usable at all versus manual-picker-only for this specific phone.

**Test scenarios:**
- Happy path: on-device recognition available → `SpeechCaptureController` returns transcribed text.
- Error path: on-device recognition unavailable → controller surfaces the manual-picker fallback, with no network call attempted.
- Error path: `RECORD_AUDIO` permission denied → same manual-picker fallback.
- Error path: recognizer errors mid-capture (e.g., `ERROR_SERVER_DISCONNECTED`-equivalent on-device failure) → same manual-picker fallback, no crash.
- Happy path: typed text entry bypasses speech capture entirely and flows to the same downstream parsing step.
- Edge case: a noisy or degraded audio transcription (fake returns low-confidence or garbled text) is passed through to Unit 4's parsing step as-is — the acceptable outcome is `TimeParseResult.Unparseable` degrading to the manual picker, not a wrong-but-confident transcription silently producing a wrong save.

**Verification:**
- On the target device: speaking a time (if on-device STT is available there) or typing one produces text that reaches the parsing step in Unit 4; if STT is unavailable on this device, the manual picker is reachable and usable as the primary backdating path.

---

- [ ] **Unit 4: On-Device LLM Time Parsing & Confirmation**

**Goal:** Parse the natural-language text from Unit 3 into a timestamp using the on-device Gemma model, with a confirm/undo step before saving.

**Requirements:** R2 (parsing), R3 (confirmation/undo), R4 (on-device-only), R11 (fallback when parsing fails)

**Dependencies:** Unit 3 (produces the text to parse), Unit 1 (saving the resulting entry). Precondition: the Gemma 3 1B `.task` model must already be `adb push`-ed to the device (see Key Technical Decisions) — this unit's verification cannot succeed without it, and `TimeParsingLlm` must detect a missing file and route to the manual picker rather than crash.

**Files:**
- Create: `app/src/main/java/com/nish/babyfeed/backdate/TimeParsingLlm.kt`
- Create: `app/src/main/java/com/nish/babyfeed/backdate/TimeParseResult.kt`
- Modify: `app/src/main/java/com/nish/babyfeed/backdate/BackdateViewModel.kt` (wire parsing + confirmation state; sole caller of both `SpeechCaptureController` and `TimeParsingLlm`, and the sole writer to `FeedRepository` on this path)
- Modify: `app/src/main/java/com/nish/babyfeed/backdate/BackdateActivity.kt` (confirmation UI, undo — rendering only, no direct repository or LLM calls)
- Test: `app/src/test/java/com/nish/babyfeed/backdate/TimeParseResultTest.kt`

**Approach:**
- `TimeParsingLlm` is declared as an interface (real implementation wrapping MediaPipe's `LlmInference`, plus a hand-written fake for `TimeParseResultTest`/`BackdateViewModelTest`) — same pattern as `SpeechCaptureController` (Unit 3) and `FeedRepository`/`SettingsRepository`.
- The real implementation wraps MediaPipe's `LlmInference` (`generateResponse`/`generateResponseAsync`, off the main thread), loading the Gemma 3 1B `.task` model from the path it was `adb push`-ed to (see Key Technical Decisions). It is an application-scoped singleton, lazily loaded on first use and kept warm for the rest of the session (see High-Level Technical Design) — reloading the ~529MB model per screen would add several seconds of latency to every single backdating attempt. Prompts the model with the current date/time as context plus the captured text, and asks for a specific, easily-parsed timestamp representation in its response.
- `TimeParseResult` defensively parses the model's free-text response (MediaPipe doesn't support schema-constrained output) into either a valid timestamp or a "couldn't parse" result — low-confidence or unparseable output routes to R11's manual picker rather than guessing.
- `BackdateViewModel` is the sole writer to `FeedRepository` on this path: on a successful parse it enters a `Confirming` state, `BackdateActivity` renders the R3 confirmation ("Logged: fed at 1:30 PM — Undo?"), and only a confirm (not a render) triggers the actual save — which calls `FeedRepository.insert()` followed by `FeedWidget().updateAll(context)` so the widget reflects the new entry immediately (same pattern as Unit 2's `WidgetActionHandler`). The manual-picker path also enters `Confirming` before saving — see Key Technical Decisions — so the undo affordance is uniform across every entry route, not just the LLM-parsed one.
- If the model file is missing from device storage (e.g., phone was reset and never re-provisioned — see Key Technical Decisions), detect this and route straight to the manual picker rather than crashing on load.

**Execution note:** Before wiring `TimeParseResult`/`BackdateViewModel`/confirmation UI around it, spike the raw path first — push the model, load `LlmInference`, and run `generateResponse` on a few real phrasings, timing it — mirroring the time-boxed validation Units 2 and 3 already do for their own unproven bets (Chronometer interop, STT availability). This is the plan's least-proven piece; discovering unacceptable latency or output quality *before* building the surrounding flow is far cheaper than discovering it after. Do the Hugging Face/Kaggle license click-through and the `adb push` itself as the first concrete step of this spike, not as a standalone risk-table line item.

**Technical design:** *(directional guidance, not implementation specification)* See the High-Level Technical Design section's sequence and state-machine diagrams for this unit's full flow — not repeated here to avoid two descriptions of the same flow drifting out of sync.

**Patterns to follow:**
- `developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android` for `LlmInference`/`LlmInferenceOptions` usage; `github.com/google-ai-edge/mediapipe-samples` for a working Kotlin sample to model this unit's integration on.

**Test scenarios:**
- Happy path: a clear natural-language time expression ("she fed at 1:30") parses to the expected timestamp.
- Happy path: a relative expression ("20 minutes ago") parses relative to the "now" passed into the prompt.
- Edge case: ambiguous or nonsensical input produces `TimeParseResult.Unparseable`, not a guessed timestamp.
- Error path: model file missing from device storage routes to the manual picker without crashing.
- Integration: a successful parse shows the R3 confirmation, and tapping "Undo" discards the entry rather than saving it.

**Verification:**
- On the target device, speaking or typing several realistic phrasings ("she fed at 1:30", "about 20 minutes ago", "just now") each either produces a correct confirmed timestamp or cleanly falls back to the manual picker — never a silently wrong save.

---

- [ ] **Unit 5: History List — Edit & Delete**

**Goal:** A simple screen to view, edit, and delete past entries — the correction mechanism for anything the widget/backdating flow got wrong, and the main item droppable under time pressure per the Priority Order. Unit 2's undo affordance covers the dominant mis-tap case, but **not** the case of noticing later that a confirmed backdated entry (R3) was parsed or picked wrong (e.g., AM/PM mixed up) — if this unit is cut, that specific gap is an accepted, documented weekend-scope tradeoff (see Risks & Dependencies), not an oversight.

**Requirements:** R7

**Dependencies:** Unit 1 (data layer)

**Files:**
- Create: `app/src/main/java/com/nish/babyfeed/history/HistoryActivity.kt`
- Create: `app/src/main/java/com/nish/babyfeed/history/HistoryViewModel.kt`
- Modify: `app/src/main/AndroidManifest.xml` (register `HistoryActivity`)
- Test: `app/src/test/java/com/nish/babyfeed/history/HistoryViewModelTest.kt`

**Approach:**
- List entries newest-first from `FeedRepository`'s `Flow`; edit reuses the same "no future timestamp" validation as Unit 1; delete removes the row and relies on the repository's existing max-timestamp logic to determine the new "last feed" (no special-case code needed here — see Key Technical Decisions). Both edit and delete call `FeedWidget().updateAll(context)` after writing, same as every other write path (see High-Level Technical Design).

**Patterns to follow:**
- Fake, not mock, `FeedRepository` in `HistoryViewModelTest` (a hand-written `FakeFeedRepository` implementing the same interface) — current Android testing convention for `Flow`-backed repositories; use `runTest` + Turbine (`viewModel.uiState.test { ... }`) to assert the ordered list/edit/delete state sequence.

**Test scenarios:**
- Happy path (unit, fake repository): editing an entry's timestamp (to a valid past time) updates what the ViewModel exposes as "last feed" if it's now the newest.
- Edge case (unit): attempting to edit an entry to a future timestamp is rejected (same validation as Unit 1).
- Edge case (unit): deleting the single remaining entry emits the zero-entries state.
- Edge case (unit): deleting the newest of several entries correctly surfaces the next-most-recent as the new "last feed."
- Integration (manual verification): deleting the single remaining entry on-device reverts the widget to the R10 empty state (covered by Unit 2's test for the rendering side; this unit only needs to emit the correct zero-entries state).

**Verification:**
- `HistoryViewModelTest` passes; entries can be listed, edited within valid bounds, and deleted on-device, and the widget's displayed state updates to match after any change.

---

- [ ] **Unit 6: Settings — Feeding Interval**

**Goal:** Let the father configure the interval used to compute "next feeding time," with changes applying live.

**Requirements:** R8

**Dependencies:** Unit 1 (data layer — interval is read by `NextFeedingCalculator`)

**Files:**
- Create: `app/src/main/java/com/nish/babyfeed/settings/SettingsActivity.kt`
- Create: `app/src/main/java/com/nish/babyfeed/settings/SettingsViewModel.kt`
- Modify: `app/src/main/AndroidManifest.xml` (register `SettingsActivity`)
- Test: `app/src/test/java/com/nish/babyfeed/settings/SettingsViewModelTest.kt`

**Approach:**
- Interval stored via a simple `DataStore`/`SharedPreferences` value (not Room — it's a single scalar, not a collection), wrapped behind a small `SettingsRepository` interface so `SettingsViewModelTest` can fake it. On first launch with no interval set yet, default to a sensible value (e.g., 3 hours) rather than blocking on a forced setup prompt. Any change immediately triggers `FeedWidget().updateAll(context)` so the countdown recomputes against the existing last-feed timestamp right away (see Key Technical Decisions).

**Test scenarios:**
- Happy path (unit, fake `SettingsRepository`): setting an interval persists and is read back correctly.
- Edge case (unit): changing the interval updates the ViewModel's exposed value immediately (the live-recompute effect on the widget itself is a manual/integration check, not a ViewModel-level unit test).
- Test expectation: first-launch default value — covered by `NextFeedingCalculatorTest` in Unit 1 (no-data/default-interval case), not re-tested here.

**Verification:**
- `SettingsViewModelTest` passes; interval can be changed from Settings on-device and the widget reflects the new calculation without requiring a new feed to be logged.

## System-Wide Impact

- **Interaction graph:** `LogNowAction`/`WidgetActionHandler` (Unit 2), the backdating save path (Unit 4), and History edits/deletes (Unit 5) all write through the same `FeedRepository` (Unit 1) and all trigger `FeedWidget().updateAll()` — there is exactly one write path and one redraw trigger, which keeps the four display states (R5/R6/R9/R10) consistent no matter which screen produced the change. See High-Level Technical Design for the component diagram making this checkable directly rather than asserted here.
- **Error propagation:** Failures in the AI path (STT unavailable, parse failure, missing model file) are designed to degrade to the manual picker (R11) rather than propagate as crashes or silent data corruption — this is the plan's central reliability property, since the AI piece is the least proven part of the stack.
- **State lifecycle risks:** The widget process can cold-render before the rest of the app has run (e.g., right after a reboot) — `FeedWidget`'s display logic must tolerate reading from the Room database directly without assuming any other component has initialized first.
- **Unchanged invariants:** No network access exists anywhere in this app; this plan does not introduce one. Any future unit that appears to need connectivity should be treated as a scope violation of the origin doc's privacy pitch, not implemented as a quiet addition.

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| MediaPipe `tasks-genai` is deprecated/maintenance-only | Accepted for weekend velocity (user decision); documented migration debt, not a surprise discovered mid-build |
| Gemma 3 1B model (~529MB) must be manually provisioned via `adb push`, not bundled | Document the one-time push step; detect a missing model file at runtime and fall back to the manual picker instead of crashing |
| On-device STT (`isOnDeviceRecognitionAvailable`) may report unavailable on this specific Samsung S23+ unit | R11's manual-picker fallback is mandatory, not optional; verify against the real device as the first step of Unit 3, not assumed |
| `AppWidgetManager`'s ~30-min update floor + Samsung's aggressive Doze/battery management could make the countdown/overdue display stale | Immediate `updateAll()` after every log/edit/delete event (not dependent on the periodic cycle) + request a battery-optimization exemption from the father once installed + treat the 15-min `WorkManager` backstop as best-effort |
| Zero prior Android experience across three unfamiliar technologies (Glance, MediaPipe, SpeechRecognizer) stacked into one weekend | Explicit Priority Order (origin doc): core widget (Units 1-2) first, AI backdating (Units 3-4) second, history/settings (Units 5-6) last and droppable |
| If Unit 5 (History) is cut, there is no way to fix a confirmed-but-wrong backdated entry discovered after the fact (R3's confirm/undo only catches mistakes at the moment of save) | Accepted as a documented weekend-scope tradeoff (user decision) — not a silent gap; the father would need to know a wrong-but-confirmed entry is uncorrectable until Unit 5 is built |
| Gemma's Hugging Face/Kaggle gated download requires an account + license click-through | Budget ~15 minutes for this as an explicit early setup step, not an afterthought |
| MediaPipe doesn't reliably run on emulators | Plan assumes development directly against a physical Samsung S23+ (or equivalent) device via USB debugging from Unit 1 onward |

## Sources & References

- **Origin document:** [docs/brainstorms/baby-feeding-tracker-widget-requirements.md](../brainstorms/baby-feeding-tracker-widget-requirements.md)
- MediaPipe LLM Inference API (Android): https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android
- MediaPipe sample code: https://github.com/google-ai-edge/mediapipe-samples/tree/main/examples/llm_inference/android
- Gemma 3 1B model card: https://huggingface.co/google/gemma-3-1b-it
- Jetpack Glance: https://developer.android.com/develop/ui/compose/glance/create-app-widget
- Glance user interaction: https://developer.android.com/develop/ui/compose/glance/user-interaction
- SpeechRecognizer: https://developer.android.com/reference/android/speech/SpeechRecognizer
- Samsung Gemini Nano device support (confirms S23+ excluded): https://www.sammobile.com/news/all-samsung-devices-that-support-gemini-nano/
