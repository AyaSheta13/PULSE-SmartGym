# PULSE Smart Gym — Pre-P11 Global UI, Motion & UX Refinement Report

- **Branch**: `feat/p10-device-connection-and-simulator`
- **Baseline Commit**: `efbf1e69d2b83cc765714c799fa774287d072ea2`
- **Status**: All 11 pre-P11 UI, motion, auth, asset, and device-discovery refinements implemented and verified.

---

## 1. Summary of Refinements

1. **Global Bottom Sheet Animation (`PulseBottomSheet.kt` & All Modals)**
   - Replaced the hardcoded `600f` offset in `PulseBottomSheet` with measured-height animation (`onSizeChanged` + `graphicsLayer { translationY = sheetHeightPx * slideFraction }`), `300ms` (`FastOutSlowInEasing`) enter, `240ms` (`FastOutLinearInEasing`) exit, simultaneous scrim alpha fade, and unmount only after exit completes.
   - Extracted `PulseAnimatedBottomSheetOverlay` and applied it across `ProfileSheets`, `ManualSetEntrySheet`, `EndWorkoutEarlyModal` (`WorkoutSessionScreen`), `DeletePlanModal` (`WorkoutPlanEditorScreen`), and `WorkoutSessionLaunchOverlays`. Updated `ConnectionLostRecoverySheet` `AnimatedVisibility` to `300ms` enter / `240ms` exit.

2. **Onboarding Animation Refinement (`OnboardingScreen.kt`, `OnboardingViewModel.kt`)**
   - Replaced static page switching with `HorizontalPager` synced two-way with `OnboardingViewModel` (`selectPage` + `animateScrollToPage` `320ms` `FastOutSlowInEasing`).
   - Added animated progress indicator width/color transitions (`animateDpAsState` / `animateColorAsState` `260ms`) and preserved double-tap completion guards.

3. **Android System Bar Consistency (`MainActivity.kt`, `PulseAppShell.kt`, `TopBar.kt`, Custom Top Bars)**
   - Configured transparent light `SystemBarStyle` edge-to-edge in `MainActivity.kt` with `isNavigationBarContrastEnforced = false` on API 29+.
   - Removed root `WindowInsets.statusBars` from `PulseAppShell` and moved status-bar inset + container background (`PulseTheme.color.container`) into `PulseTopAppBar` and non-`PulseTopAppBar` screen top bars (`OnboardingScreen`, `AuthEntryScreen`, `ReadyStartSetScreen` `SetExecutionTopBar`, `SessionDetailsTopBar`, `ProgressTopBar`, `HistoryTopBar`, `SessionTopBar`, `WorkoutResultScreen`), eliminating the top status-bar seam.

4. **Application Launcher Icon**
   - Replaced default Android Studio green-grid & robot launcher assets with PULSE coral (`#FF623E`) `ic_launcher_background.xml`, centered white PULSE lightning bolt `ic_launcher_foreground.xml` within the adaptive icon safe zone, monochrome support in `mipmap-anydpi-v26`, and regenerated legacy `ic_launcher.png` / `ic_launcher_round.png` across `mdpi`–`xxxhdpi`.

5. **Splash Screen Duration & Animation (`SplashViewModel.kt`, `SplashScreen.kt`)**
   - Increased minimum splash display duration from `450ms` to `2_200ms` (`DEFAULT_MIN_DISPLAY_DURATION_MS = 2_200L`, injectable for deterministic unit testing).
   - Added restrained one-shot entrance alpha/scale animation (`420ms`–`460ms`, `FastOutSlowInEasing`) guarded by `rememberSaveable`.

6. **Correct Home Navigation Icon (`home.kt`)**
   - Removed the upward roof protrusion above the apex in both `Home` and `HomeFilled` vector paths while preserving proportions, door geometry, and colors.

7. **Global Bottom Action Container Layout (`PulseAppShell.kt`, `WorkoutDetailsScreen.kt`)**
   - Removed `WindowInsets.navigationBars` from `PulseAppShell`'s `contentBottomInsetModifier` (keeping `.imePadding()`), eliminating double bottom-inset consumption and the exposed `#FAF8F7` strip below white bottom action containers.
   - Updated `WorkoutDetailsScreen` bottom action bar to use a top `1.dp` divider with seamless white background extending under `navigationBarsPadding()`.

8. **Login & Sign Up Validation States (`LoginScreen.kt`, `LoginViewModel.kt`, `SignUpScreen.kt`, `SignUpViewModel.kt`)**
   - Removed production/debug validation showcase buttons (`⚠ Test Invalid Login & Error Feedback`, `Try Valid Demo Credentials`, `⚠ Test Sign Up Validation Errors (4 Scenarios)`, `Use Valid Demo Registration`) and sample-trigger methods (`triggerValidationErrorSample`, `clearErrorsAndFillDebugCredentials`, `triggerValidationErrorsSample`, `clearErrorsAndFillDemoRegistration`, `isValidationErrorState`).
   - Preserved real inline validation errors, typed field inputs, and retryable primary CTAs (`Log In` / `Create Account`).

9. **Remove Google & Apple Login (`LoginScreen.kt`, `LoginViewModel.kt`)**
   - Removed `OR CONTINUE WITH` divider, `Google` and `Apple` buttons, `ExternalProviderButton`, and `onExternalProviderClick`.

10. **Counter Card Background Decorations (`RepCounterGauge.kt`)**
    - Restored the two Figma clipped decorative halos (`#16FF623E` top-right, `#1C39B8FD` bottom-left, `96.dp` radius) behind `PulseRepCounterGauge` content while preserving the `320ms` `FastOutSlowInEasing` progress arc animation.

11. **Device Discovery — Searching Animation (`PulsePodIllustration.kt`, `FakeDeviceDataSource.kt`)**
    - Upgraded `BluetoothRadarIndicator` to 3 staggered expanding coral pulse rings (`1_600ms` cycle) around the stationary Bluetooth icon while `isAnimating == true`, stopping cleanly when `isAnimating == false`.
    - Updated `FakeDeviceDataSource.DEFAULT_SCAN_DELAY_MS` to `3_500L` (injectable for tests) and `connectStepDelayMs` to `350L`.

---

## 2. Verification Results

| Suite / Task | Result | Exact Counts |
| :--- | :--- | :--- |
| `:app:assembleDebug` | PASSED | — |
| `:app:assembleRelease` | PASSED | — |
| `:app:testDebugUnitTest` | PASSED | 61 tests, 0 failures, 0 errors, 0 skipped |
| `:core:designsystem:testDebugUnitTest` | PASSED | 4 tests, 0 failures, 0 errors, 0 skipped |
| `:core:data:testDebugUnitTest` | PASSED | 5 tests, 0 failures, 0 errors, 0 skipped |
| `:core:model:test` | PASSED | 6 tests, 0 failures, 0 errors, 0 skipped |
| `:core:data:connectedDebugAndroidTest` | PASSED | 27 tests, 0 failures, 0 errors, 0 skipped |
| `:app:connectedDebugAndroidTest` | PASSED | 48 tests, 0 failures, 0 errors, 0 skipped |

- **Visual QA Artifacts**: `docs/visual-qa/pre-p11/before/` and `docs/visual-qa/pre-p11/after/`:
  - `01_launcher_icon.png`: Installed adaptive launcher icon (`#FF623E` coral + centered white PULSE bolt).
  - `03_onboarding.png` / `04_auth_entry.png` / `18_three_button_nav.png`: Edge-to-edge status bar & 3-button Android navigation bar matching adjacent surfaces with zero seam or translucent scrim.
  - `08_home_nav_selected.png` & `09_home_nav_unselected.png`: Selected (`HomeFilled`) and unselected (`Home`) navigation icons without roof protrusion.
  - `10_workout_details.png`: Corrected Workout Details bottom action container (`Start Workout` / `Edit Plan`) with single inset consumption and no excess whitespace.
  - `11_rep_counter_halos_manual.png` & `12_rep_counter_halos_device.png`: Both manual and device-assisted rep counter cards with clipped top-right coral and bottom-left blue halos.
  - `13_device_discovery_scanning.png`: Staggered 3-ring radar pulse animation during `SCANNING`.
  - `14_login_inline_validation.png` & `15_signup_inline_validation.png`: Inline field-specific validation on Login & Sign Up with preserved inputs and no social login or debug showcase buttons.
  - `16_bottom_sheet_open.png` & `17_bottom_sheet_mid_exit.png`: Overlay bottom sheet open state and mid-exit downward slide + simultaneous scrim fade (`240ms` exit).

## 3. Remaining Limitations
- `PreP11VisualQaCaptureTest` captures Compose-rendered surfaces for component/layout evidence alongside live `adb` captures (`01_launcher_icon.png`, `04_auth_entry.png`, `05_login.png`, `18_three_button_nav.png`).
- Real ESP32 BLE GATT characteristic/protocol integration remains deferred to P11/P12 by design (`FakeDeviceDataSource` provides simulated discovery and telemetry).
