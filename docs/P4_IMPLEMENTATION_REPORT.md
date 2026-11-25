# PULSE Smart Gym — P4 Implementation Report

**Phase:** P4 — Pixel-Accurate Splash, Onboarding, Authentication & Profile Setup  
**Date:** 2026-11-25  
**Status:** Complete — Ready for P5 Review

---

## 1. Screens Implemented & Figma Mapping

All 12 Figma reference frames (designed at `390 × 884`) were inspected individually and implemented in Compose:

1. **Splash / Launch (`57:20479`)** — `SplashScreen.kt`
2. **Onboarding 1 — Welcome (`57:20531`)** — `OnboardingScreen.kt` (Page 0)
3. **Onboarding 2 — Track Your Way (`57:20254`)** — `OnboardingScreen.kt` (Page 1)
4. **Onboarding 3 — Save & Grow (`57:20391`)** — `OnboardingScreen.kt` (Page 2)
5. **Welcome / Auth Entry (`281:2`)** — `AuthEntryScreen.kt`
6. **Log In (`281:25`)** — `LoginScreen.kt`
7. **Log In — Validation Errors (`533:328`)** — `LoginScreen.kt` (error state)
8. **Create Account / Sign Up (`281:64`)** — `SignUpScreen.kt`
9. **Sign Up — Validation Errors (`533:356`)** — `SignUpScreen.kt` (error state)
10. **Forgot Password (`281:95`)** — `ForgotPasswordScreen.kt`
11. **Profile Setup — Personal Info (`282:2`)** — `PersonalInfoScreen.kt`
12. **Profile Setup — Fitness Goals (`282:55`)** — `FitnessGoalsScreen.kt`

---

## 2. Authentication, Guest Mode & Security Boundaries

- **Offline Guest Identity (`LocalAuthRepository.continueAsGuest`):**
  - Lazily generates and persists a unique `guest_<uuid>` owner ID in DataStore (`PulsePreferencesDataSource.getOrCreateGuestOwnerId`).
  - Persists active session state so guest users remain signed in across app restarts and keep all workout/profile data isolated under their guest `ownerId`.
  - Usable in both Debug and Release builds.
- **Local Account Registration & Login (`LocalAuthRepository`):**
  - Stores local accounts in Room (`local_accounts`) with unique lowercase email index, 16-byte `SecureRandom` salt, and `PBKDF2WithHmacSHA256` (100,000 iterations, 256-bit key) via `PasswordHasher`.
  - Never stores or logs plaintext passwords.
  - Supports `Remember me` persistence; when `rememberMe = false`, local account sessions are cleared on restart.
- **Password Reset (`ForgotPasswordScreen`):**
  - Unauthenticated password reset is explicitly blocked (`DataError.Unauthorized`) with honest UI feedback (`Password Reset Unavailable`) so an unauthenticated user cannot overwrite an existing local account.
- **Debug Helpers:**
  - Debug validation-error preview buttons (`⚠ Test Invalid Login & Error Feedback`, `⚠ Test Sign Up Validation Errors (4 Scenarios)`) are strictly gated behind `isDebugEntryEnabled`.
- **Authentication Initialization, Main Authorization Gate & Process-Death Restoration:**
  - `AuthState.Initializing` prevents `PulseApp` from treating a not-yet-restored session as signed-out while `AppEntryFlow.MAIN` is restored after process death.
  - `PulseApp` runs `restoreSession()` immediately and gates `PulseAppShell` (`HomeTab`) behind an explicit `authorizedOwnerId` check that waits for both `AuthState.Authenticated` and `isProfileSetupCompleted(user.id)` to resolve to `true` before composing Main, preventing any flash of Main even when `isProfileSetupCompleted` is delayed.
- **Navigation 3 ViewModel Lifecycle & Owner-Isolated Drafts:**
  - Uses the official `androidx.lifecycle:lifecycle-viewmodel-navigation3` (`rememberViewModelStoreNavEntryDecorator()`) paired with per-tab `rememberSaveableStateHolder()` + `rememberSaveableStateHolderNavEntryDecorator()` in `PulseNavDisplay.kt`.
  - Each stack (`AUTH` and each `PulseTab`) is scoped to its own `StackScopedViewModelStoreOwner` retained in `PerStackViewModelStoreHolder`, keeping inactive tab entries and saveable state alive across tab switches while clearing popped destination `ViewModel`s automatically.
  - `PerStackViewModelStoreHolder.syncAuthState` + `sessionEpoch` invalidates all tab `ViewModelStore`s and saveable state on logout or account switch without wiping state during `Initializing -> Authenticated` or `ActivityScenario.recreate()` for the same user.
  - Non-sensitive form fields (`OnboardingViewModel.currentPage`, `LoginViewModel.email`/`rememberMe`, `SignUpViewModel.fullName`/`email`, `ProfileSetupDraftHolder`) use `SavedStateHandle`; passwords are never persisted in `SavedStateHandle`, DataStore, or Room.
  - `ProfileSetupDraftHolder` keys drafts strictly by `ownerId` and resets immediately on owner/account switch.
- **Restored Login Figma Elements (`281:52`, `281:56`):**
  - Added `OR CONTINUE WITH` separator and `Google` / `Apple` buttons on `LoginScreen`, producing honest unavailable feedback until OAuth is integrated.
- **Deferred Backend Work:**
  - Remote email recovery, OAuth providers (Google / Apple), and cloud account sync remain deferred to future backend integration.

---

## 3. Profile Persistence & Room Migration (v1 → v2)

- **Domain (`:core:model`):** Added `GenderIdentity`, `FitnessGoal`, `ExperienceLevel`, `UserProfile`, `PersonalInfoDraft`, `FitnessGoalsDraft`, `ProfileAndAuthValidators`, and `UserProfileRepository`.
- **Database (`:core:data`):**
  - Bumped `PulseDatabase` to `version = 2` (`exportSchema = true`, exported `2.json`).
  - Added `LocalAccountEntity` (`local_accounts`) and `UserProfileEntity` (`user_profiles`), plus `LocalAccountDao` and `UserProfileDao`.
  - Added explicit `PulseDatabase.MIGRATION_1_2` and verified with `MigrationTestHelper` that existing v1 `workout_plans` and `workout_sessions` survive intact.
- **Draft Preservation:** `ProfileSetupDraftHolder` preserves Personal Info across Back navigation from Fitness Goals (2/2 → 1/2), and `RoomUserProfileRepository.saveCompletedProfile` saves both steps atomically in a Room transaction.

---

## 4. Navigation 3 Integration

- Replaced P2 placeholders for `Splash`, `Onboarding`, `AuthEntry`, `Login`, `SignUp`, `ForgotPassword`, `ProfileSetupPersonalInfo`, and `ProfileSetupFitnessGoals` in `PulseNavigationEntryProvider.kt`.
- Added `PulseNavigator.replaceAuthRoot` so `Splash` -> `Onboarding` / `AuthEntry` / `ProfileSetupPersonalInfo` replaces the transient splash root.
- Added session guard in `PulseApp.kt` (`enforceAuthGuard`) so signing out or having an `Unauthenticated` session prevents `MAIN` flow access and resets to `AuthEntry`.

---

## 5. Verification Results

- **Gradle Build & Test Command:**
  ```powershell
  .\gradlew.bat :core:model:build :core:data:testDebugUnitTest :core:designsystem:testDebugUnitTest :app:testDebugUnitTest :core:data:connectedDebugAndroidTest :app:connectedDebugAndroidTest --no-daemon
  ```
- **Result:** `BUILD SUCCESSFUL in 1m 52s`
  - `:core:model:build` — **PASSED**
  - `:core:data:testDebugUnitTest` (`FakeAuthRepositoryTest`, `P4ValidatorsUnitTest`) — **PASSED**
  - `:core:designsystem:testDebugUnitTest` (`PulseDesignSystemContractTest`) — **PASSED**
  - `:app:testDebugUnitTest` (`ExampleUnitTest`, `PulseNavigatorTest`, `P4LifecycleAndDraftUnitTest` — 14 tests) — **PASSED**
  - `:core:data:connectedDebugAndroidTest` (`PulseRoomIntegrationTest` + `Migration1To2Test`, 14/14 tests on `Medium_Phone_API_36.0`) — **PASSED**
  - `:app:connectedDebugAndroidTest` (`PulseNavigationUiTest`, `PulseActivityRecreationUiTest`, `P4AuthAndProfileUiTest`, `P4ScreenshotCaptureTest`, `ExampleInstrumentedTest`, 16/16 tests on `Medium_Phone_API_36.0`) — **PASSED**
- **Visual QA:** All 12 screens were captured on `Medium_Phone_API_36.0` at `390 × 884` dp and compared against Figma references with normalized side-by-side + diff images saved in `docs/visual-qa/p4/`; see [P4_VISUAL_QA_REPORT.md](P4_VISUAL_QA_REPORT.md).
