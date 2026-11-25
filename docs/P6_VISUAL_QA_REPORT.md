# PULSE Smart Gym — P6 Visual QA Report

## 1. Verification Setup
- **Emulator:** `Medium_Phone_API_36.0` (`1080 × 2400`)
- **Normalization Target:** `390 × 884` (`Density(density = 1f, fontScale = 1f)`)
- **Capture Test:** [P6ScreenshotCaptureTest.kt](../app/src/androidTest/java/com/ayasheta/pulsesmartgym/feature/P6ScreenshotCaptureTest.kt) (fails automatically if any PNG is missing or empty)
- **Evidence Directories:**
  - Figma references: [docs/visual-qa/p6/figma](visual-qa/p6/figma)
  - Android captures: [docs/visual-qa/p6/android](visual-qa/p6/android)
  - Side-by-side + 4x amplified diffs: [docs/visual-qa/p6/comparison](visual-qa/p6/comparison)

## 2. Screen-by-Screen Comparison Results

| Capture | Figma Node | MAE (0–255) | Within ±16 | Comparison Image |
|---|---|---|---|---|
| `01_my_workouts_populated.png` | `57:19917` | 11.09 | 87.40% | [01_my_workouts_populated.png](visual-qa/p6/comparison/01_my_workouts_populated.png) |
| `02_my_workouts_empty.png` | Derived state | — | — | [Android](visual-qa/p6/android/02_my_workouts_empty.png) |
| `03_my_workouts_deleted.png` | `61:1842` | 8.78 | 89.97% | [03_my_workouts_deleted.png](visual-qa/p6/comparison/03_my_workouts_deleted.png) |
| `04_create_plan_empty.png` | `605:338` | 17.77 | 74.55% | [04_create_plan_empty.png](visual-qa/p6/comparison/04_create_plan_empty.png) |
| `05_create_plan_selected.png` | Derived state | — | — | [Android](visual-qa/p6/android/05_create_plan_selected.png) |
| `06_exercise_picker_unselected.png` | Derived state | — | — | [Android](visual-qa/p6/android/06_exercise_picker_unselected.png) |
| `07_exercise_picker_3_selected.png` | `57:19181` | 32.90 | 68.46% | [07_exercise_picker_3_selected.png](visual-qa/p6/comparison/07_exercise_picker_3_selected.png) |
| `08_edit_plan_populated.png` | `57:18885` | 24.05 | 73.04% | [08_edit_plan_populated.png](visual-qa/p6/comparison/08_edit_plan_populated.png) |
| `09_workout_details_all_collapsed.png` | `626:336` | 13.47 | 87.26% | [09_workout_details_all_collapsed.png](visual-qa/p6/comparison/09_workout_details_all_collapsed.png) |
| `10_workout_details_ex1_expanded.png` | `335:2` | 16.49 | 82.12% | [10_workout_details_ex1_expanded.png](visual-qa/p6/comparison/10_workout_details_ex1_expanded.png) |
| `11_workout_details_ex2_expanded.png` | `335:65` | 20.09 | 78.70% | [11_workout_details_ex2_expanded.png](visual-qa/p6/comparison/11_workout_details_ex2_expanded.png) |
| `12_workout_details_ex3_expanded.png` | `626:322` | 23.72 | 75.88% | [12_workout_details_ex3_expanded.png](visual-qa/p6/comparison/12_workout_details_ex3_expanded.png) |
| `13_workout_details_ex4_expanded.png` | `626:329` | 22.57 | 77.14% | [13_workout_details_ex4_expanded.png](visual-qa/p6/comparison/13_workout_details_ex4_expanded.png) |
| `14_delete_plan_confirmation.png` | `61:1454` | 39.07 | 60.62% | [14_delete_plan_confirmation.png](visual-qa/p6/comparison/14_delete_plan_confirmation.png) |

## 3. Visual Fixes & Truthful Deviations
- **Exercise Picker Row Height (`57:19181`):** Tightened `ExercisePickerRowCard` vertical padding from `14.dp` to `9.dp` (`horizontal = 14.dp`) so rows match Figma's `82.dp` height.
- **Sticky Bottom Actions (`605:338`, `57:18885`):** Pinned `Create Workout Plan` / `Save Changes` and `Cancel` in a bottom container so primary actions remain reachable on small screens and when 4+ exercises are configured.
- **Truthful Duration Label (`WorkoutDurationEstimator`):** Plans display `"Est. X min"` computed deterministically from planned sets and rest durations instead of hardcoded Figma sample durations.
- **Delete Confirmation Backdrop (`61:1454`):** Modal sheet geometry, scrim opacity (`0.55f`), `Keep Plan`, and `Delete Plan` buttons match Figma; MAE reflects the updated `Edit Workout Plan` top bar underneath (Figma's older `61:1454` backdrop showed the legacy `ROUTINE BUILDER` sub-header).
