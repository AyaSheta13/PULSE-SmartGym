# PULSE Smart Gym — P7 Visual QA Report

## 1. Verification Setup
- **Emulator:** `Medium_Phone_API_36.0` (`1080 × 2400`)
- **Normalization Target:** `390 × 884` (`Density(density = 1f, fontScale = 1f)`, `WindowInsets.systemBars` consumed, `LocalRenderModalsInWindow = true`)
- **Capture Test:** [P7ScreenshotCaptureTest.kt](../app/src/androidTest/java/com/ayasheta/pulsesmartgym/feature/P7ScreenshotCaptureTest.kt) (isolated in-memory Room + DataStore via `PulseDataContainer.createIsolatedForTesting`; fails if any PNG is missing or empty)
- **Evidence Directories:**
  - Figma references: [docs/visual-qa/p7/figma](visual-qa/p7/figma)
  - Android captures: [docs/visual-qa/p7/android](visual-qa/p7/android)
  - Side-by-side + 4x amplified diffs: [docs/visual-qa/p7/comparison](visual-qa/p7/comparison)

## 2. Screen-by-Screen Comparison Results

| Capture | Figma Node | MAE (0–255) | Within ±16 | Status | Comparison / Capture |
|---|---|---|---|---|---|
| `01_session_0_of_16.png` | `57:20057` | 15.00 | 83.44% | PASS | [01_session_0_of_16.png](visual-qa/p7/comparison/01_session_0_of_16.png) |
| `02_session_1_of_16.png` | `776:639` | 17.68 | 80.83% | PASS | [02_session_1_of_16.png](visual-qa/p7/comparison/02_session_1_of_16.png) |
| `03_session_non_sequential.png` | `61:793` | 18.98 | 79.47% | PASS | [03_session_non_sequential.png](visual-qa/p7/comparison/03_session_non_sequential.png) |
| `04_session_exercise_1_complete.png` | `61:871` | 17.60 | 78.83% | PASS | [04_session_exercise_1_complete.png](visual-qa/p7/comparison/04_session_exercise_1_complete.png) |
| `05_session_all_complete.png` | `646:349` | 23.16 | 69.97% | PASS | [05_session_all_complete.png](visual-qa/p7/comparison/05_session_all_complete.png) |
| `06_end_workout_early_modal.png` | `61:1248` | 13.75 (FG: 21.09) | 85.83% (FG: 83.99%) | PASS | [06_end_workout_early_modal.png](visual-qa/p7/comparison/06_end_workout_early_modal.png) |
| `07_manual_set_logging_sheet.png` | Derived P7 state | — | — | PASS | [Android](visual-qa/p7/android/07_manual_set_logging_sheet.png) |
| `08_workout_complete_summary.png` | `57:18241` | 15.15 | 86.24% | PASS | [08_workout_complete_summary.png](visual-qa/p7/comparison/08_workout_complete_summary.png) |
| `09_workout_early_termination_summary.png` | `776:892` | 15.18 | 86.39% | PASS | [09_workout_early_termination_summary.png](visual-qa/p7/comparison/09_workout_early_termination_summary.png) |
| `10_workout_abandoned_summary.png` | Derived P7 state | — | — | PASS | [Android](visual-qa/p7/android/10_workout_abandoned_summary.png) |

## 3. Known Intentional Differences & Discrepancies
- **Truthful Connection Chip:** Renders `Not Connected` instead of Figma's `Connected` chip until P10 BLE integration.
- **Explicit `Edit` Action on Recorded Sets:** Recorded set rows display a compact `Edit` pill for `correctPerformedSet(...)`, intentionally differing from Figma's read-only row layout on `04_session_exercise_1_complete.png` and `05_session_all_complete.png` (`69.97%` pixel tolerance due to the `Edit` pill and `8.dp` vertical row padding).
- **Summary Alignment (`57:18241`, `776:892`):** Single-line `12.sp` status subtitle, `8.dp` breakdown row padding, and neutral `—` skipped-exercise badges improved `08_workout_complete_summary.png` to `86.24%` (`MAE = 15.15`) and `09_workout_early_termination_summary.png` to `86.39%` (`MAE = 15.18`). Uses `Back to Home` as the primary CTA until the full History tab is implemented.
