---
date: 2026-10-02
topic: baby-feeding-tracker-widget
---

# Baby Feeding Tracker Widget

## Problem Frame
A close friend (the father) is tracking his newborn's feeding times in an Obsidian note. Figuring out "when was she last fed, and when should I feed her next" means opening the app, finding the last entry, and doing the math — friction that's real during sleep-deprived newborn care. He needs to log a feed and see the next feeding time (and time remaining) directly from his Android home screen, without opening an app.

This is being built for Hacktoberfest 2026's "Build for a Friend" weekend, themed "Build something with open-source AI at its core." The AI has to be load-bearing, not decorative: the app uses an on-device, open-weight model to parse natural-language time expressions when logging a *past* feed, so sensitive baby-care input never leaves the father's phone, works with no internet connection (useful at 3am), and costs nothing to run.

## Requirements

**Logging**
- R1. Widget shows a single-tap "Log feed now" action that records the current time as a feeding event. This is the widget's only action — backdated logging is an app feature, not a widget feature (see Key Decisions).
- R2. The app supports logging a feed in the past via natural-language text or voice input (e.g., "she fed at 1:30", "about 20 minutes ago"), parsed by an on-device open-weight LLM into a specific timestamp.
- R3. Before saving a backdated entry, show a brief confirmation of the parsed time (e.g., "Logged: fed at 1:30 PM — Undo?") so a misparse can be caught and corrected.
- R4. Both steps of voice-based backdating — speech-to-text and time parsing — run entirely on-device, with no cloud fallback for either: no network call is made to log a feed.
- R11. If parsing fails, is ambiguous, or on-device speech recognition isn't available, the app falls back to a manual date/time picker so a past feed can always be logged.

**Widget Display**
- R5. Home screen widget displays: time of last feed, calculated next feeding time, and time remaining until then.
- R6. Next feeding time = last feed time + a configurable interval (not hardcoded — must be adjustable as the baby's schedule changes with age).
- R9. When the current time passes the calculated next feeding time, the widget switches from a countdown to an overdue indicator (e.g., "Overdue by 18 min") rather than showing a stale, zero, or negative countdown.
- R10. Before any feed has ever been logged, the widget shows a prompt (e.g., "No feeds logged yet — tap to log") instead of attempting to display a last-feed/next-feed countdown with no data to compute from.

**App**
- R7. A simple in-app screen lists past feeding entries and allows editing or deleting a mistaken entry (the widget only shows current state, so corrections need a home).
- R8. Settings screen to configure the feeding interval used for the next-feeding calculation.

## Success Criteria
- The father installs and actually uses the app/widget for real feeding tracking, not just as a demo.
- Logging a feed (including a backdated one) is faster and less error-prone than his current Obsidian-note workflow.
- No feeding data or voice/text input ever leaves the device — satisfied by R4 covering both speech-to-text and time parsing, not just the parsing model.
- The Hacktoberfest submission can honestly explain why the open-weight, on-device model mattered here (privacy of baby-care data, offline reliability at any hour, zero running cost) — not just that an LLM was present.

## Scope Boundaries
- Feeding only. Diaper changes, medicine, and massage tracking are explicitly out of scope, even though they were part of the original motivating context.
- Android only (father's Samsung S23+). Mom's iPhone is explicitly out of scope for V1 — not a requirement, not a tracked stretch goal.
- No cross-device sync or shared backend. The app is single-device, local-only; this is itself part of the "no server you don't control" pitch, not a gap to fill later.
- No push notifications when feeding is due/overdue — the widget's at-a-glance countdown is the only due-time signal.
- Feed entries capture time only — no amount, type (breast/bottle), duration, or side tracking.
- Single baby/profile — no multi-child support.

## Priority Order (Weekend Cut-Line)
Given this is a native Android build with zero prior Android experience, on top of an on-device LLM integration that's new territory either way, requirements are explicitly ordered so scope can be cut safely if time runs short — later groups are droppable without undermining the earlier ones:
1. **Core logging + widget** (R1, R5, R6, R9, R10): one-tap "log now," and the widget reliably displaying last feed / next feed / countdown / overdue / empty states. This alone is a usable, honest improvement over the Obsidian note.
2. **AI-backed backdating** (R2, R3, R4, R11): the on-device voice/text parsing feature that makes this a Hacktoberfest "open-source AI at its core" submission, not just a logging app.
3. **History and settings** (R7, R8): editing/deleting past entries and configuring the interval. Nice to have; a hardcoded default interval and no edit screen is a survivable cut if the weekend runs out.

## Key Decisions
- **Native Android (Kotlin), not Flutter or a PWA**: chosen over a cross-platform or web-based alternative despite zero prior Android experience, to get a true native home-screen widget and the most traditional Android build. Accepts higher tooling/learning risk for a more faithful native result; mitigated by the explicit priority order above.
- **AI's role is voice/text-to-time parsing for backdated entries, not primary logging or a conversational assistant**: the common case ("just fed her") is a one-tap button; the model only does work where natural language genuinely beats a form — interpreting relative/absolute time expressions when logging a forgotten past feed.
- **Backdated logging is app-only, not a widget action**: the widget stays a single button plus a display (R1, R5, R6, R9, R10) — simplest possible widget to build and debug given zero prior Android/widget experience. Logging a past feed means opening the app, which is an acceptable tradeoff since it's the less common case.
- **Fixed interval after last feed, not a learned/predicted interval**: simplest to trust at 3am; avoids an opaque "smart" prediction the father can't reason about.
- **Confirm before saving a parsed backdated time**: trades a small amount of friction for protection against a misheard/misparsed time silently corrupting the countdown.
- **On-device-only speech-to-text and parsing, with a manual-picker fallback**: keeps the "data never leaves the device" claim actually true (not just true of the parsing step), while R11 ensures a past feed can still be logged even if the on-device voice/AI path fails or isn't available on this hardware.

## Dependencies / Assumptions
- Assumes the father's phone (Samsung S23+, Android) can run a small on-device LLM at acceptable speed/battery cost; exact model (e.g., Gemma 3 1B/2B or similar) and runtime (e.g., MediaPipe LLM Inference, llama.cpp) to be finalized during planning based on what runs acceptably on that hardware.
- Assumes sideloading/installing a debug or personally-signed APK is acceptable (this is a personal gift, not a Play Store release).

## Outstanding Questions

### Deferred to Planning
- [Affects R2, R4][Needs research] Which specific open-weight model and on-device runtime gives acceptable parse latency/accuracy on a Samsung S23+ for short time-expression parsing?
- [Affects R2, R4][Needs research] Which on-device speech-recognition API/engine satisfies the on-device-only requirement (e.g., Android's offline SpeechRecognizer), and whether it's reliably available on a Samsung S23+ — if not, R11's manual-picker fallback is the expected path.
- [Affects R6][User decision] Default feeding interval value and how/where it's first set (e.g., prompted on first launch vs. a sensible default like 3 hours).
- [Affects R5, R9][Technical] Android's AppWidgetManager has a minimum periodic-update granularity (~30 minutes via updatePeriodic; finer updates need WorkManager/AlarmManager). Confirm the countdown's intended precision is achievable within that constraint.

## Next Steps
-> /ce:plan for structured implementation planning
