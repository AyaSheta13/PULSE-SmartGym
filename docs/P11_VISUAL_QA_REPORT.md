# P11 Visual QA Report

## Summary
Visual QA was conducted across Welcome, Onboarding (3 pages), Auth/Validation, Home, Workouts, Workout Details, Exercise Picker, Exercise Details, Workout Session, Unified Set Execution (Ready, InProgress Connected, InProgress Manual, Connection Lost), Workout Complete & Partial Summary, History, Session Details, Training Progress, Device Discovery/Connection, Settings, and Bottom Sheets on `Medium_Phone_API_36.0` (360dp, 390dp, 412dp widths and 640dp, 844dp, 915dp heights) and verified against Figma (`UvskyKTyxlIOd68wQkp5cQ`).

## Resolved Visual & Integration Defects
1. **Welcome & Onboarding Hero Aspect Ratios**:
   - **Implementation**: Each hero container matches its intrinsic drawable aspect ratio (`378:436` for Welcome & Onboarding 1, `935:1024` for Onboarding 2, `377:436` for Onboarding 3) using `ContentScale.Fit` inside an aspect-ratio-constrained container (`Modifier.aspectRatio(ratio, matchHeightConstraintsFirst = ratio < 1f)`), rounded `24.dp` corners, badges anchored inside the image bounds, and zero vertical scroll across `360x640`, `390x844`, and `412x915` viewports.
2. **Exercise Picker Card Layout**:
   - `[26dp Circular Selection Control (48dp touch target, left)] -> [64dp Exercise Photo] -> [2-line Wrapped Title + Muscle/Equipment Badges] -> [Right Chevron >]`, removing the `i` info button.
3. **Reactive Device Status Chip**:
   - `WorkoutSessionViewModel` and `SetExecutionViewModel` observe `DeviceRepository.linkState` reactively and display `Connected (Sim)` when simulated hardware is linked, independent of manual vs automatic rep tracking mode.
4. **Unified Set Execution Layout Stability & Time/Rep Presentation**:
   - `LivePerformanceCard` is mounted in both `Ready` and `InProgress` phases with unavailable placeholders (`00:00`, `—`, `No measured reps`) before tracking begins, preventing layout shifts on `Start Set`.
   - Top context row displays live-ticking **Workout elapsed time** (`Workout MM:SS`) distinct from **Set Duration** and **Rep Duration**.
   - `LivePerformanceCard` identifies the exact repetition (`Rep #N`, `Earlier Rep #K`, or `No measured reps`), and `ExpandableRepetitionBreakdown` displays concentric/eccentric durations when genuinely available.
5. **Exercise-Specific Performance Summaries & Weekly Trends**:
   - `WorkoutResultScreen` and `SessionDetailsScreen` display exercise-specific ROM, rep duration, peak acceleration, and sensor-classified full/partial counts.
   - `TrainingProgressScreen` displays week-filtered, repetition-weighted `Performance Trends` with chronological points and stable exercise-key selection.

6. **18-Exercise Equipment-Accurate Photography & Starter Plan Duration Alignment**:
   - Added dedicated equipment-accurate thumbnails for `DB Flat Press` (`ex_thumb_db_flat_press.png`) and `Smith Squat` (`ex_thumb_smith_squat.png`), so all **18** catalog exercises map to 18 distinct drawables (`WorkoutVisualHelpers.kt`, `HistoryUiMapper.kt`, and Figma Photography Collection `670:367`).
   - Verified `My Workouts` displays `Est. 17 min` / `Est. 21 min` / `Est. 19 min` matching `WorkoutDurationEstimator` and Figma (`57:19917`).
7. **Set Execution Redesign, Visual Fidelity Refinement & Direct Weight Input**:
   - Verified `WorkoutInfoCard` (`Workout Time` | `Rest`), `UnifiedRepAndWeightCard` (`8.dp` gauge-to-pill spacing, `250–260.dp` weight capsule proportions, coral divider clearance, subtle border), integrated `LivePerformanceWaveformCard` (continuous smooth Bézier wave + gradient fill + axes directly inside the main card with no nested rounded container), and pinned `Start Set` / `Finish Set` + `End Workout` bar across `360x640` and `390x844` viewports (`p11_set_execution_live_metrics.png`, `p11_set_execution_ready_360x640.png`).
   - Verified tapping the weight value opens the `Set Load` bottom sheet (`p11_set_load_sheet.png`) supporting decimal input (`42.75 kg`), validation, `Cancel`/`Apply`, `+`/`-` continuation, and kg/lbs preference conversion.

## Evidence Artifacts (`docs/visual-qa/p11/`)
- **Android P11 & Pre-P11 Captures**: `p11_welcome_390.png`, `p11_onboarding_0.png`, `p11_onboarding_1.png`, `p11_onboarding_2.png`, `p11_my_workouts_seeded.png`, `p11_my_workouts_empty_after_delete.png`, `p11_create_plan_empty_plus.png`, `p11_exercise_picker_plus.png`, `p11_exercise_picker_after.png`, `p11_set_execution_live_metrics.png`, `p11_set_execution_ready_360x640.png`, `p11_set_load_sheet.png`, `p11_full_device_system_bars.png`, `08_home_nav_selected.png`, `09_home_nav_unselected.png`, `10_workout_details.png`, `11_rep_counter_halos_manual.png`, `12_rep_counter_halos_device.png`, `13_device_discovery_scanning.png`, `14_login_inline_validation.png`, `15_signup_inline_validation.png`, `16_bottom_sheet_open.png`, `17_bottom_sheet_mid_exit.png`
- **Historical Phase Captures**: `docs/visual-qa/p4/` through `docs/visual-qa/p10/` and `docs/visual-qa/pre-p11-exercise-guide/`
- **Figma Exports**: `figma_welcome.png`, `figma_onboarding_1.png`, `figma_onboarding_2.png`, `figma_onboarding_2_portrait.png`, `figma_onboarding_3.png`, `figma_exercise_picker.png`, `figma_set_exec_ready.png`, `figma_set_exec_connected.png`, `figma_se_ready_v2.png`, `figma_se_inprogress_v2.png`, `figma_se_target_v2.png`, `figma_set_load_sheet.png`, `figma_workout_session_a.png`, `figma_workout_complete.png`, `figma_session_details.png`, `figma_progress.png`
