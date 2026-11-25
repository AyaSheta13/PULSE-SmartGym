# PULSE Smart Gym — P5 Implementation Report

**Phase:** P5 — Home, Main Navigation & Profile  
**Date:** 2026-11-25  
**Baseline:** `2262f38cc37d42f9cc0006789328cf63ed7d3901`  
**Status:** Complete — Ready for Review

---

## 1. Architecture & Packages

- **Domain & Analytics (`:core:model`):**
  - `HistoryModels.kt`: Added `WeeklyProgressSummary`, `WorkoutAnalytics.calculateWeeklySummary(...)`, `WorkoutAnalytics.selectFeaturedPlan(...)`, and `WorkoutAnalytics.selectLastFinishedSession(...)`.
  - `ProfileAndAuthModels.kt`: Extended `AuthUser` with `createdAtEpochMs: Long? = null`.
- **Data (`:core:data`):**
  - `LocalAuthRepository.kt`: Populated `AuthUser.createdAtEpochMs` from persisted `LocalAccountEntity.createdAtEpochMs` (never fabricated for guests).
- **Home Feature (`app/.../feature/home/`):**
  - `HomeUiState.kt` (`Loading`, `SignedOut`, `Error`, `Loaded` with `isNoPlansEmptyState` and `isPopulatedState`)
  - `HomeViewModel.kt` (combines `AuthRepository`, `UserProfileRepository`, `WorkoutPlanRepository`, `WorkoutSessionRepository`, `WorkoutHistoryRepository` via `flatMapLatest` on `observeAuthState()`)
  - `HomeScreen.kt` (First Workout, Derived No-Plans, Populated, Weekly Progress, Plans Preview, Last Session, and honest Start Workout unavailable sheet)
- **Profile & Settings Feature (`app/.../feature/profile/`):**
  - `ProfileUiState.kt`, `ProfileViewModel.kt`, `ProfileScreen.kt`, `ProfileSheets.kt`
  - `edit/EditProfileUiState.kt`, `edit/EditProfileViewModel.kt`, `edit/EditProfileScreen.kt`
- **Navigation 3 (`app/.../navigation/`):**
  - `PulseNavigationEntryProvider.kt`: Replaced `HomeTab`, `ProfileTab`, and `EditProfile` placeholders with real screens while preserving `WorkoutsTab`, `ProgressTab`, `HistoryTab`, `WorkoutDetails`, `SessionDetails`, and `MyDevice` contracts and P4 `PerStackViewModelStoreHolder` + `MainAuthorizationGate`.

---

## 2. Business Rules & Weekly Metrics Policy

- **Week Boundary:** Monday (`DayOfWeek.MONDAY`) through Sunday (`DayOfWeek.SUNDAY`) in the device's local `ZoneId`. UTC `startedAtEpochMs` timestamps are converted via `Instant.ofEpochMilli(...).atZone(zoneId).toLocalDate()` so local-calendar and DST boundaries are respected.
- **Completed Workouts:** `completedWorkoutsCount` counts `SessionStatus.COMPLETED` only. `SessionStatus.PARTIALLY_COMPLETED` is tracked separately in `partialWorkoutsCount` and does not increment the goal counter. `ACTIVE` and `ABANDONED` sessions are excluded.
- **Weekly Target:** Read from `UserProfile.weeklyWorkouts` (no hardcoded `4`).
- **Weighted Volume:** Sum of `WorkoutAnalytics.calculateVolumeKg` (`fullReps × actualLoadKg`) across finished sessions (`COMPLETED` + `PARTIALLY_COMPLETED`) in the current local week; partial reps and unknown/null loads are excluded.
- **Featured Plan Selection (Deterministic):** Active session's `sourcePlanId` if present; otherwise most recently updated plan (`updatedAtEpochMs`); otherwise `null` (No Plans empty state).
- **Last Session:** Most recent `COMPLETED` or `PARTIALLY_COMPLETED` session snapshot; partial sessions are explicitly labeled `"Ended early"`.

---

## 3. Profile, Edit Profile, Settings & Logout

- **Profile Header:** Shows real `UserProfile.fullName`, derived initials, account email or `"Offline Guest Profile"` for guests, and real `Member since MMM yyyy` from `LocalAccountEntity.createdAtEpochMs` (or `"Guest Session • On-Device Only"`).
- **Edit Profile:** Supports editing `fullName`, `weightKg`, `heightCm`, `gender`, `fitnessGoal`, and `experienceLevel` with validation (`fullName` non-blank, `weightKg` finite in `(0, 500]`, `heightCm` in `[80, 260]`), read-only email, honest `"Change Avatar"` notice, `Cancel / Discard Changes`, and atomic Room persistence via `UserProfileRepository.saveCompletedProfile` preserving `age` and `weeklyWorkouts`.
- **Settings Sheets:** `Account Preferences` (`528:328`), `Units & Measurements` (`316:530` — Metric persisted via `UserPreferencesRepository`, Imperial disabled `"Coming in future release"`), `Privacy & Local Storage` (`316:515`), and honest `"Workout Reminders"` (`Coming Soon`).
- **Logout:** Calls `AuthRepository.signOut()`, resets navigation to `AuthEntry`, clears all Main tab `ViewModelStore`s and saveable state, and preserves Room workout/profile data.

---

## 4. Deferred Work (P6 / P7 / P9 / P10)

- **P6:** Workout Plan Creation/Editing UI, Exercise Picker, Workout Details UI, full My Workouts tab.
- **P7:** Live Workout Session runner, Rep Counter, Rest Timer, Workout Completion UI.
- **P9:** Full Workout History and Progress/Analytics tabs.
- **P10:** BLE scanning, device pairing, and real-time telemetry for `My PULSE Device`.
