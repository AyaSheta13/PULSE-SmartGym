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
| `01_session_0_of_16.png` | `57:20057` | 12.30 | 85.37% | PASS | [01_session_0_of_16.png](visual-qa/p7/comparison/01_session_0_of_16.png) |
| `02_session_1_of_16.png` | `776:639` | 12.88 | 84.24% | PASS | [02_session_1_of_16.png](visual-qa/p7/comparison/02_session_1_of_16.png) |
| `03_session_non_sequential.png` | `61:793` | 13.47 | 83.84% | PASS | [03_session_non_sequential.png](visual-qa/p7/comparison/03_session_non_sequential.png) |
| `04_session_exercise_1_complete.png` | `61:871` | 17.21 | 79.51% | PASS | [04_session_exercise_1_complete.png](visual-qa/p7/comparison/04_session_exercise_1_complete.png) |
| `05_session_all_complete.png` | `646:349` | 22.51 | 73.86% | PASS | [05_session_all_complete.png](visual-qa/p7/comparison/05_session_all_complete.png) |
| `06_end_workout_early_modal.png` | `61:1248` | 13.34 (FG: 20.57) | 86.13% (FG: 84.19%) | PASS | [06_end_workout_early_modal.png](visual-qa/p7/comparison/06_end_workout_early_modal.png) |
| `07_manual_set_logging_sheet.png` | Derived P7 state | — | — | PASS | [Android](visual-qa/p7/android/07_manual_set_logging_sheet.png) |
| `08_workout_complete_summary.png` | `57:18241` | 19.06 | 82.88% | PASS | [08_workout_complete_summary.png](visual-qa/p7/comparison/08_workout_complete_summary.png) |
| `09_workout_early_termination_summary.png` | `776:892` | 21.33 | 80.94% | PASS | [09_workout_early_termination_summary.png](visual-qa/p7/comparison/09_workout_early_termination_summary.png) |
| `10_workout_abandoned_summary.png` | Derived P7 state | — | — | PASS | [Android](visual-qa/p7/android/10_workout_abandoned_summary.png) |

## 3. Known Intentional Differences & Discrepancies
- **Truthful Connection Chip:** Renders `Not Connected` instead of Figma's `Connected` chip until P10 BLE integration.
- **Explicit `Edit` Action on Recorded Sets:** Recorded set rows display a compact `Edit` pill for `correctPerformedSet(...)`, slightly shifting text metrics on `04_session_exercise_1_complete.png` and `05_session_all_complete.png`.
- **End Early Modal Backdrop (`61:1248`):** Figma's backdrop shows the P8 Live Workout rep counter; P7 opens the exact modal foreground over the real Session Dashboard (`FG Within16 = 84.19%`).
- **Summary CTA (`57:18241`, `776:892`):** Uses `Back to Home` as the primary CTA until the full History tab is implemented.
