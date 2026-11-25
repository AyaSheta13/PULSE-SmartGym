# PULSE Smart Gym — P3 Data & Domain Architecture

---

## 1. Module Dependency Graph

```text
:app
 ├── :core:designsystem
 ├── :core:model
 └── :core:data

:core:data
 └── :core:model

:core:designsystem
 └── (independent — zero dependency on :app, :core:model, or :core:data)
```

- `:core:model` is a pure Kotlin/JVM module (`org.jetbrains.kotlin.jvm`) with only `kotlinx-coroutines-core` (no Android or Room dependencies).
- `:core:data` is an Android Library module housing Room, KSP, DataStore, repository implementations, mappers, and `PulseDataContainer`.

---

## 2. Domain vs. Entity Separation

- Domain models (`ExerciseDefinition`, `WorkoutPlan`, `WorkoutSession`, `PerformedSet`, `SessionSummary`, `UserPreferences`, `AuthUser`) live in `:core:model` without Room annotations.
- Room entities (`*Entity`) and relation classes (`*WithSets`, `*WithExercises`) live strictly in `:core:data/local/database/` and are mapped to domain models in `DataMappers.kt`.
- Repository interfaces live in `:core:model/repository/Repositories.kt`; UI/ViewModels in `:app` will consume only `:core:model` interfaces and domain models (`Flow` for reactive observation, `suspend` functions for mutations).

---

## 3. Persistence & Preferences Ownership

- **Room (`PulseDatabase`):** Relational workout data (exercise catalog, workout plans, session snapshots, performed sets, rep records).
- **Preferences DataStore (`PulsePreferencesDataSource`):** Simple non-sensitive settings only (`preferredWeightUnit`, `onboardingCompleted`, `defaultRestSeconds`). IO exceptions recover safely with `emptyPreferences()`.
- **Composition Root (`PulseDataContainer` & `PulseApplication`):** Provides a single `PulseDatabase` instance, single `DataStore<Preferences>`, and lazy repository singletons per process without Hilt overhead.

---

## 4. Authentication & BLE Compatibility Boundaries

- **`AuthRepository` / `FakeAuthRepository`:** Exposes `observeAuthState(): Flow<AuthState>`, `signInDemo(...)`, and `signOut()`. `allowDemoSignIn` is bound to `isDebuggable` (`false` in Release builds). No passwords or tokens are stored; a future Retrofit/Ktor `ApiAuthRepository` can swap in behind `AuthRepository` in P4+ without changing UI or domain contracts.
- **Hardware / BLE (`DeviceContracts.kt`):** Defines `DeviceConnectionState`, `TrackingMode`, `MeasurementOrigin`, `RepClassification`, and `RepMeasurement` without GATT UUIDs, packet schemas, or BLE permissions. P10 will introduce `:core:device` implementing `FakeDeviceDataSource` and later `RealBleDeviceDataSource`.

---

## 5. Navigation 3 Integration Note (for P7/P8)

- P2 routes `ReadyStartSet(sessionId, exerciseId, setIndex)`, `ManualRepCounter(...)`, and `DeviceAssistedRepCounter(...)` carry `exerciseId: String`. When P7/P8 wires live session screens, `exerciseId` in those routes will pass `SessionExercise.id` (the unique session exercise occurrence ID) so repeated exercises within a plan resolve unambiguously.
