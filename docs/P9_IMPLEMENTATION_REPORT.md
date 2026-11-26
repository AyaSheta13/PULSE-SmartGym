# P9 Implementation Report — Workout Complete, History & Training Analytics

## Scope
Implemented **P9 — Workout History, Session Details & Training Analytics** (including the P9 Final Acceptance Patch) on branch `feat/p9-history-and-analytics`, strictly scoped to P9 (no P10, BLE, or sensor simulation).

## Architecture & Analytics Policies
- **`:core:model` (`HistoryModels.kt`, `Repositories.kt`)**
  - **PR Presentation Policy**: Distinguishes `First PR` (no strictly earlier eligible load), positive weight improvement (`+X kg/lbs PR`), and `Matched PR` (strictly earlier equal max load), preserving deterministic tie-breaking (load grams -> full reps -> most recent timestamp -> performedSetId). Never fabricates improvements.
  - **Volume Coverage & Comparison Policy**: `PeriodProgressMetrics` tracks both current-week and previous-week `VolumeCoverage` (`NO_MEASURED_VOLUME`, `KNOWN_ZERO_VOLUME`, `PARTIALLY_MEASURED`, `FULLY_MEASURED`). Never substitutes planned load for missing actual load, never emits `-100%` when the current period has performed sets with unknown loads, and suppresses percentage comparisons when either week has incomplete measurement coverage.
  - **Safe Numeric Formatting**: Shared `SessionUiMapper.formatVolumeNumber` / `formatVolumeWithUnit` uses `roundToLong()` and `NumberFormat.getIntegerInstance(Locale.US)` across Progress, Summary, Session Details, and Home to prevent `Int` overflow on large volumes.
  - **Truthful Historical Week Labels**: Selected historical weeks (`selectedWeekOffset > 0`) display their `MMM d–d` / `MMM d–MMM d` date range rather than `"This Week"`.
- **`:core:data` (`RoomWorkoutHistoryRepository.kt`, `RoomWorkoutSessionRepository.kt`)**
  - `observeHistory` and `observeFinishedSessions` return `COMPLETED` and `PARTIALLY_COMPLETED` only, newest first.
  - `startSessionFromHistory` validates historical snapshot structure (`planNameSnapshot`, `exerciseNameSnapshot`, `orderIndex >= 0`, `setIndex >= 1`, `targetReps >= 1`, `targetRestSeconds >= 0`), wraps transaction execution with `DomainResult.Failure(DataError.StorageError(...))` handling, enforces one active session per owner, copies planned structure with fresh IDs and zero performed sets / no rest timer, and works even after source plan edit or deletion.
- **`:app` (ViewModels, Launch Coordinator & Screens)**
  - `WorkoutHistoryViewModel` derives `HistoryAggregateMetrics` from the same emitted `observeHistory(ownerId)` snapshot via `WorkoutAnalytics.aggregateSummaries(summaries)`.
  - `WorkoutHistoryViewModel`, `TrainingProgressViewModel`, and `SessionDetailsViewModel` immediately emit `Loading` via `.onStart { emit(...Loading) }` on owner changes so previous-owner data is never exposed while new-owner Room queries load.
  - `WorkoutSessionLaunchCoordinator` resets `isStarting` in `finally` and checks owner identity after `startSessionFromPlan` / `startSessionFromHistory` returns so it never navigates after an account switch.

## Verification Results

| Check | Command | Total | Pass | Fail | Skip |
|---|---|---|---|---|---|
| Core Model build & tests | `.\gradlew.bat :core:model:build` | 6 | 6 | 0 | 0 |
| Core Data unit tests | `.\gradlew.bat :core:data:testDebugUnitTest` | 4 | 4 | 0 | 0 |
| Design System unit tests | `.\gradlew.bat :core:designsystem:testDebugUnitTest` | 4 | 4 | 0 | 0 |
| App unit tests | `.\gradlew.bat :app:testDebugUnitTest` | 50 | 50 | 0 | 0 |
| Room instrumentation tests | `.\gradlew.bat :core:data:connectedDebugAndroidTest` | 23 | 23 | 0 | 0 |
| Full App instrumentation suite | `.\gradlew.bat :app:connectedDebugAndroidTest` | 36 | 35 | 1* | 0 |
| Standalone P4 check (`pm clear`) | `adb shell am instrument ... P4AuthAndProfileUiTest#onboardingToGuestProfileSetup_preservesPersonalInfoOnBack_andEntersMain` | 1 | 1 | 0 | 0 |
| P9 Screenshot capture | `adb shell am instrument ... P9ScreenshotCaptureTest` | 1 | 1 | 0 | 0 |
| Debug & Release APK builds | `.\gradlew.bat :app:assembleDebug :app:assembleRelease` | — | PASS | — | — |

\* **Known Limitation**: During the full 36-test `:app:connectedDebugAndroidTest` run on `Medium_Phone_API_36.0`, `P4AuthAndProfileUiTest#onboardingToGuestProfileSetup_preservesPersonalInfoOnBack_andEntersMain` hit its 5,000 ms `waitUntil` timeout under emulator load; when run standalone after `pm clear`, it passed in 11.17s.
