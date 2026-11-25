# PULSE Smart Gym — P3 Implementation Report

**Phase:** P3 (Room Database, Core Domain & Repository Architecture)  
**Date:** 2026-11-25  
**Status:** Complete — Ready for P4

---

## 1. Initial Audit & Modules Added

- Preserved `:app` (Navigation 3) and `:core:designsystem` (`PulseTheme`) without modification to P2 behavior.
- Added `:core:model` (pure Kotlin/JVM module, `org.jetbrains.kotlin.jvm`, zero Android/Room dependencies).
- Added `:core:data` (Android Library module with Room `2.8.5`, KSP `2.2.10-2.0.2`, Preferences DataStore `1.3.0`).
- Added `android.disallowKotlinSourceSets=false` to `gradle.properties` for KSP `2.2.10-2.0.2` generated-source registration under AGP `9.1.1` built-in Kotlin.

---

## 2. Dependencies Introduced

- `androidx.room:room-runtime:2.8.5`, `room-ktx:2.8.5`, `room-compiler:2.8.5` (KSP), `room-testing:2.8.5`, `androidx.room` plugin `2.8.5`
- `com.google.devtools.ksp:2.2.10-2.0.2`
- `androidx.datastore:datastore-preferences:1.3.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2`, `kotlinx-coroutines-test:1.10.2`

---

## 3. Entities, DAOs & Schema

- **Database:** `PulseDatabase` (`version = 1`, `exportSchema = true`, schema JSON exported to `core/data/schemas/com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase/1.json`, no `fallbackToDestructiveMigration()`).
- **Entities (9 tables):** `exercises`, `workout_plans`, `plan_exercises`, `planned_sets`, `workout_sessions`, `session_exercises`, `session_planned_sets`, `performed_sets`, `rep_records`.
- **DAOs (3 DAOs):** `ExerciseDao`, `WorkoutPlanDao`, `WorkoutSessionDao`.

---

## 4. Repositories & Composition Root

| Interface (`:core:model`) | Implementation (`:core:data`) |
|---|---|
| `ExerciseRepository` | `RoomExerciseRepository` (with idempotent `ExerciseCatalogSeed`) |
| `WorkoutPlanRepository` | `RoomWorkoutPlanRepository` |
| `WorkoutSessionRepository` | `RoomWorkoutSessionRepository` |
| `WorkoutHistoryRepository` | `RoomWorkoutHistoryRepository` |
| `UserPreferencesRepository` | `DataStoreUserPreferencesRepository` |
| `AuthRepository` | `FakeAuthRepository` (debug-only demo sign-in) |

- Composition root: `PulseDataContainer` in `:core:data` instantiated lazily by `PulseApplication` in `:app`.

---

## 5. Transactional Workflows, Duplicate Protection & Snapshots

- **Create/Update Plan:** Atomic `@Transaction` (`createPlanGraph`, `replacePlanGraph`) with domain validation.
- **Start Session:** Atomic `@Transaction` (`startSessionGraph`) snapshotting plan name, exercise metadata, and planned sets.
- **Duplicate Set Prevention:** Unique SQLite index on `(sessionId, sessionExerciseId, setIndex)` in `performed_sets` + `OnConflictStrategy.IGNORE` returning `-1L` → `RecordSetResult.AlreadyRecorded`. Explicit corrections use `correctPerformedSet`.
- **Immutable History:** `workout_sessions.sourcePlanId` uses `ForeignKey.SET_NULL`; `session_exercises.sourceExerciseId` uses `ForeignKey.SET_NULL`. Editing or deleting a source plan preserves all completed/partial session history.

---

## 6. Build & Test Results

- **Unit & Build:**
  ```powershell
  .\gradlew.bat :core:model:build :core:data:assembleDebug :core:data:testDebugUnitTest :core:designsystem:assembleDebug :app:assembleDebug :app:testDebugUnitTest --no-daemon
  ```
  - **Result:** `BUILD SUCCESSFUL in 3m 9s` (all unit tests in `:core:model`, `:core:data`, and `:app` passed).
- **Instrumentation Tests (`Medium_Phone_API_36.0(AVD) - 16`):**
  ```powershell
  .\gradlew.bat :core:data:connectedDebugAndroidTest :app:connectedDebugAndroidTest --no-daemon
  ```
  - **Result:** `BUILD SUCCESSFUL in 2m 16s`
  - `:app:connectedDebugAndroidTest`: **6/6 PASSED** (0 failed, 0 skipped)
  - `:core:data:connectedDebugAndroidTest` (`PulseRoomIntegrationTest`): **7/7 PASSED** (0 failed, 0 skipped)

---

## 7. Known Limitations & Remaining Work

- `android.disallowKotlinSourceSets=false` is an experimental AGP 9 flag required until KSP adopts AGP 9's new source-set API.
- `FakeAuthRepository` is strictly for local development/testing; real API auth belongs to P4+.
- BLE GATT transport and `FakeDeviceDataSource` belong to P10.
