# P11 Visual QA Report

## Summary
Visual QA was conducted across Welcome, Onboarding (3 pages), Auth/Validation, Home, Workouts, Workout Details, Exercise Picker, Exercise Details, Workout Session, Unified Set Execution (Ready, InProgress Connected, InProgress Manual, Connection Lost), Workout Complete & Partial Summary, History, Session Details, Training Progress, Device Discovery/Connection, Settings, and Bottom Sheets on `Medium_Phone_API_36.0` (360dp, 390dp, 412dp widths and 640dp, 844dp, 915dp heights) and verified against Figma (`UvskyKTyxlIOd68wQkp5cQ`).

## Resolved Visual & Integration Defects
1. **Welcome & Onboarding Hero Aspect Ratios**:
   - **Implementation**: Each hero container matches its intrinsic drawable aspect ratio (`378:436` for Welcome & Onboarding 1, `1672:941` for Onboarding 2, `377:436` for Onboarding 3) using `ContentScale.Fit` inside an aspect-ratio-constrained container (`Modifier.aspectRatio(ratio, matchHeightConstraintsFirst = ratio < 1f)`), rounded `24.dp` corners, badges anchored inside the image bounds, and zero vertical scroll across `360x640`, `390x844`, and `412x915` viewports.
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

## Evidence Artifacts (`docs/visual-qa/p11/`)
- **Android P11 & Pre-P11 Captures**: `p11_welcome_390.png`, `p11_onboarding_0.png`, `p11_onboarding_1.png`, `p11_onboarding_2.png`, `p11_exercise_picker_after.png`, `p11_set_execution_live_metrics.png`, `p11_full_device_system_bars.png`, `08_home_nav_selected.png`, `09_home_nav_unselected.png`, `10_workout_details.png`, `11_rep_counter_halos_manual.png`, `12_rep_counter_halos_device.png`, `13_device_discovery_scanning.png`, `14_login_inline_validation.png`, `15_signup_inline_validation.png`, `16_bottom_sheet_open.png`, `17_bottom_sheet_mid_exit.png`
- **Historical Phase Captures**: `docs/visual-qa/p4/` through `docs/visual-qa/p10/` and `docs/visual-qa/pre-p11-exercise-guide/`
- **Figma Exports**: `figma_welcome.png`, `figma_onboarding_1.png`, `figma_onboarding_2.png`, `figma_onboarding_3.png`, `figma_exercise_picker.png`, `figma_set_exec_ready.png`, `figma_set_exec_connected.png`, `figma_workout_session_a.png`, `figma_workout_complete.png`, `figma_session_details.png`, `figma_progress.png`
