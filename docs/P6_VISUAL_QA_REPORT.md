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
| `04_create_plan_empty.png` | `605:338` | 6.10 | 92.46% | [04_create_plan_empty.png](visual-qa/p6/comparison/04_create_plan_empty.png) |
| `05_create_plan_selected.png` | Derived state | — | — | [Android](visual-qa/p6/android/05_create_plan_selected.png) |
| `06_exercise_picker_unselected.png` | Derived state | — | — | [Android](visual-qa/p6/android/06_exercise_picker_unselected.png) |
| `07_exercise_picker_3_selected.png` | `57:19181` | 20.56 | 77.91% | [07_exercise_picker_3_selected.png](visual-qa/p6/comparison/07_exercise_picker_3_selected.png) |
| `08_edit_plan_populated.png` | `57:18885` | 19.58 | 79.90% | [08_edit_plan_populated.png](visual-qa/p6/comparison/08_edit_plan_populated.png) |
| `09_workout_details_all_collapsed.png` | `626:336` | 13.47 | 87.26% | [09_workout_details_all_collapsed.png](visual-qa/p6/comparison/09_workout_details_all_collapsed.png) |
| `10_workout_details_ex1_expanded.png` | `335:2` | 16.49 | 82.12% | [10_workout_details_ex1_expanded.png](visual-qa/p6/comparison/10_workout_details_ex1_expanded.png) |
| `11_workout_details_ex2_expanded.png` | `335:65` | 20.09 | 78.70% | [11_workout_details_ex2_expanded.png](visual-qa/p6/comparison/11_workout_details_ex2_expanded.png) |
| `12_workout_details_ex3_expanded.png` | `626:322` | 23.72 | 75.88% | [12_workout_details_ex3_expanded.png](visual-qa/p6/comparison/12_workout_details_ex3_expanded.png) |
| `13_workout_details_ex4_expanded.png` | `626:329` | 22.57 | 77.14% | [13_workout_details_ex4_expanded.png](visual-qa/p6/comparison/13_workout_details_ex4_expanded.png) |
| `14_delete_plan_confirmation.png` | `61:1454` | 68.22 (FG: 81.01) | 13.41% (FG: 36.47%) | [14_delete_plan_confirmation.png](visual-qa/p6/comparison/14_delete_plan_confirmation.png) |

## 3. Visual Fixes & Truthful Deviations
- **Create & Edit Plan CTA Flow (`605:338`, `57:18885`):** Moved `Create Workout Plan` / `Save Changes` and `Cancel` into the scrollable `LazyColumn` footer (with `navigationBarsPadding()`) and added a dashed border to `EmptyExercisesCard`, raising `04_create_plan_empty.png` similarity from `74.55%` to `92.46%` (`MAE = 6.10`) and `08_edit_plan_populated.png` from `73.04%` to `79.90%` (`MAE = 19.58`).
- **Exercise Picker Layout & Row Height (`57:19181`):** Replaced the magnifying-glass emoji with a crisp vector `PickerSearchIcon` (`ImageVector`), refined header spacing (`18.dp` before search, `12.dp` before chips, `16.dp` bottom), fixed `ExercisePickerRowCard` to `height(82.dp)` (`padding(horizontal = 14.dp)`), and tightened sticky drawer inset handling (`bottomInsetOverride` for normalized captures), raising `07_exercise_picker_3_selected.png` from `68.47%` (`MAE = 32.89`) to `77.91%` (`MAE = 20.56`). Remaining pixel difference reflects the full 18-item Room catalog order vs Figma's 6-row mock subset and Android font/navigation-bar insets.
- **Truthful Duration Label (`WorkoutDurationEstimator`):** Plans display `"Est. X min"` computed deterministically from planned sets and rest durations instead of hardcoded Figma sample durations.
- **Delete Confirmation (`61:1454`):** Captured the real `DeletePlanConfirmationModal` opened over the real `EditWorkoutPlan` screen in `PulseApp` via `uiAutomation.takeScreenshot()`. The whole-frame and foreground-sheet (`y=587..884`) metrics (`13.41%` / `FG: 36.47%`) reflect three measurement differences: (1) `uiAutomation.takeScreenshot()` captures the full `1080 × 2400` (`411.4 × 914.3dp`) emulator window including Android status and gesture bars whereas Figma `61:1454` is `390 × 884` without OS bars; (2) Figma's `61:1454` backdrop shows the legacy `ROUTINE BUILDER` header while the real `EditWorkoutPlan` screen uses `Edit Workout Plan` (`57:18885`); (3) at `411.4dp` width the confirmation description wraps to 2 lines instead of 3 lines at `390dp`, making the Android sheet slightly shorter (`~265dp` vs Figma `297px`) so `Keep Plan` / `Delete Plan` sit ~20px lower inside `y=587..884`.
