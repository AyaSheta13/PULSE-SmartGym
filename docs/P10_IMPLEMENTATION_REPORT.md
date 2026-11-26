# P10 Implementation Report — Device Connection, BLE-Ready Architecture & Simulator

## Branch & Scope
- **Branch:** `feat/p10-device-connection-and-simulator`
- **Scope:** P10 only (Device Center, Discovery/Connecting/Failed/Connected flows, Connection-Lost Recovery + Manual Fallback, Device-Assisted Rep Counter, BLE-ready `DeviceDataSource`/`DeviceRepository` boundary, `FakeDeviceDataSource` simulator, `RealBleDeviceDataSource` fail-honest stub, API-aware Bluetooth permissions, explicit `DemoEnvironmentController` isolation, and full repetition ledger restoration).

## Architecture & Reliability Policies

### 1. Complete Repetition Ledger Restoration
- `SetExecutionDraft` (`SetDraftStore.kt`) is scoped by `(ownerId, environment, sessionId, exerciseId, setIndex)` and includes `trackingWindowId`, `fullReps`, `partialReps`, `repRecords`, `acceptedEventIds`, `origin`, `actualLoad` (preserving explicit `null` via `hasInitializedLoad`), `setStartedAtElapsedMs`, `trackingMode`, `hadDeviceTracking`, and `isManualFallback`.
- `SetDraftSavedStateCodec` serializes a bounded draft (`MAX_SAVED_LEDGER_RECORDS = 100`, `MAX_SAVED_EVENT_IDS = 120`) into `SavedStateHandle` without placing unbounded data in navigation arguments and never silently truncates accepted repetitions or event IDs.
- Both `DeviceAssistedRepCounterViewModel` (auto and manual `+1`) and `ManualRepCounterViewModel` (in manual fallback mode) enforce the same `100`-repetition limit and display a clear user message when reached; ordinary manual sets (`hadDeviceTracking == false`) retain their existing `999`-rep behavior.
- On restoration, `DeviceAssistedRepCounterViewModel` and `ManualRepCounterViewModel` validate all fields, derive aggregate counts from the ledger for device-tracked sets, preserve `acceptedEventIds` to reject replayed events after restoration, and handle device reboots safely (`savedElapsed > currentElapsed` resets the monotonic baseline).
- Ordinary manual sets (`hadDeviceTracking == false`) retain their aggregate-only representation.

### 2. Explicit Demo Environment Isolation
- `DemoEnvironmentController` (hosted in `authFlowViewModelStoreOwner`, surviving Activity recreation) manages `ExecutionEnvironmentState`: `Genuine`, `DemoInitializing`, `DemoReady`, `DemoClosing`, and `DemoFailed`.
- Workout/summary routes carry `ExecutionEnvironmentKind` (`GENUINE` or `DEMO`), enforced by `DemoEnvironmentGate`: demo routes never fall back to genuine storage during initialization, closing, failure, or process restart.
- `DemoEnvironmentController` cancels active init jobs before closing `IsolatedTestContainer`, clears `SetDraftStore` for `DEMO` on exit or owner change, and hosts `SetDraftStore` so fallback drafts survive Activity recreation.
- `ReadyStartSetViewModel` and `DeviceEventNormalizer` ensure a `SIMULATED` provider never authorizes automatic tracking or events on a `GENUINE` session.

### 3. Event Acceptance Boundaries & Set Completion
- `DeviceEventNormalizer.normalizeAndValidate()` enforces owner authentication, active session, unrecorded planned set, active accepting state (stopped on submission/end/fallback/disposal), environment-provider match, connection generation, tracking window, bounded event count (`100`) and retained IDs (`120`), and strict origin provenance (`SIMULATED_TELEMETRY` for `SIMULATED`, `SENSOR_TELEMETRY` for `REAL_BLE`).
- Manual fallback navigation uses durable `pendingManualFallback` UI state in addition to the event flow.
- `SetFinishHelper` catches storage exceptions (rethrowing `CancellationException`), returns `AlreadyCompleted` for duplicate sets, isolates rest-timer failures from saved sets, and invalidates callbacks on owner change.

### 4. Measurement Correction Semantics
- `RecordSetRequest.repCorrectionPolicy` (`RepCorrectionPolicy`: `PRESERVE_IF_COUNTS_MATCH`, `REPLACE_WITH_LEDGER`, `INVALIDATE_LEDGER`) governs `RoomWorkoutSessionRepository.correctPerformedSet()`.
- Dashboard Edit Set (`WorkoutSessionViewModel.submitManualSet` + `ManualSetEntrySheet`) explicitly selects `PRESERVE_IF_COUNTS_MATCH` for weight-only edits and `INVALIDATE_LEDGER` when rep counts change, displaying an inline notice explaining provenance preservation or per-rep measurement invalidation.
- Weight-only corrections (`PRESERVE_IF_COUNTS_MATCH` with matching counts and empty `repRecords`) preserve existing `trackingMode`, `origin`, `durationMs`, and `repRecords`.
- `INVALIDATE_LEDGER` explicitly clears detailed records; `REPLACE_WITH_LEDGER` requires non-empty validated `repRecords`.

### 5. Permission & Provider Lifecycle
- `DeviceDiscovery` uses `ActivityCompat.shouldShowRequestPermissionRationale` to distinguish `PERMISSION_DENIED` from `PERMISSION_PERMANENTLY_DENIED`, and refreshes readiness on `Lifecycle.Event.ON_RESUME`.
- `DefaultDeviceRepository.onOwnerChanged()` and `selectProvider()` synchronously cancel active scan/connection jobs via `resetToDisconnected()`.

## Verification Results

| Suite | Total | Passed | Failed | Errors | Skipped | Not Run |
| --- | --- | --- | --- | --- | --- | --- |
| `:core:model:test` | 6 | 6 | 0 | 0 | 0 | 0 |
| `:core:data:testDebugUnitTest` | 4 | 4 | 0 | 0 | 0 | 0 |
| `:core:designsystem:testDebugUnitTest` | 4 | 4 | 0 | 0 | 0 | 0 |
| `:app:testDebugUnitTest` | 58 | 58 | 0 | 0 | 0 | 0 |
| `:core:data:connectedDebugAndroidTest` | 27 | 27 | 0 | 0 | 0 | 0 |
| `:app:connectedDebugAndroidTest` | 42 | 42 | 0 | 0 | 0 | 0 |
| **Total** | **141** | **141** | **0** | **0** | **0** | **0** |

- `:app:assembleDebug` — BUILD SUCCESSFUL
- `:app:assembleRelease` — BUILD SUCCESSFUL
