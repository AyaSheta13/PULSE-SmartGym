# P11 Screen Inventory & Navigation Verification

## Overview
All application screens, dialogs, and bottom sheets in PULSE Smart Gym are wired through Navigation 3 (`PulseNavigationEntryProvider.kt` and `PulseAppRoute.kt`) with clean state restoration and consistent design system tokens.

| Screen / Route | Primary ViewModel / File | P11 Status |
| --- | --- | --- |
| Welcome / Auth Entry | `AuthEntryScreen.kt` | Hero aspect ratio fixed (`378:436`), zero scroll |
| Onboarding (Pages 0–2) | `OnboardingScreen.kt` | Intrinsic aspect ratios (`378:436`, `935:1024`, `377:436`), badges anchored on image, zero scroll |
| Sign Up / Login / Guest | `SignUpScreen.kt`, `LoginScreen.kt` | Verified via `P4AuthAndProfileUiTest` |
| Home Dashboard | `HomeScreen.kt` | Reactive device status, quick workout actions |
| My Workouts | `MyWorkoutsScreen.kt` | Plan list, create/edit/start actions |
| Workout Details | `WorkoutDetailsScreen.kt` | Plan overview, exercise order, edit & start |
| Workout Editor | `WorkoutEditorScreen.kt` | Plan creation/editing, target sets/reps/load |
| Exercise Picker | `ExercisePickerScreen.kt` | Refined card layout (`[Circle Select]` -> `[Photo]` -> `[Title + Badges]` -> `[Chevron >]`), preserved state across `ExerciseDetails` |
| Exercise Details | `ExerciseDetailsScreen.kt` | Accessible from Exercise Picker & Workout Session |
| Workout Session Dashboard | `WorkoutSessionScreen.kt`, `WorkoutSessionViewModel.kt` | Reactive `DeviceRepository.linkState` chip (`Connected (Sim)` / `Disconnected`), set completion, edit set |
| Set Execution (Ready / Active / Manual Fallback) | `SetExecutionScreen.kt`, `SetExecutionViewModel.kt`, `ReadyStartSetScreen.kt` | Decoupled connection chip, `LivePerformanceCard` (2x2), collapsed `ExpandableRepetitionBreakdown` |
| Workout Result Summary | `WorkoutResultScreen.kt`, `WorkoutResultViewModel.kt` | Added `SessionPerformanceSummary` card (Avg ROM, Peak Accel, Avg Rep Duration) |
| History | `HistoryScreen.kt` | Completed/partial sessions list |
| Session Details | `SessionDetailsScreen.kt`, `SessionDetailsViewModel.kt` | Added `SessionPerformanceSummary` card, Repeat Workout |
| Training Progress | `TrainingProgressScreen.kt`, `TrainingProgressViewModel.kt` | Added `PerformanceTrendsCard` with metric selector |
| Device Connection | `DeviceConnectionScreen.kt` | Simulator & BLE-ready pairing/connection states |
| Profile / Settings | `ProfileScreen.kt` | Units (`kg`/`lbs`), account switch, preferences |

## Complete 18-Exercise Catalog Photography & Equipment Audit Matrix

All 18 seeded catalog exercises (`ExerciseCatalogSeed.kt`) resolve to 18 distinct, equipment-accurate drawables in `app/src/main/res/drawable-nodpi/` and `WorkoutVisualHelpers.kt` / `HistoryUiMapper.kt`:

| # | Exercise ID | Display Name | Muscle Group | Equipment | Android Drawable | Equipment Match |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | `ex_bench_press` | Barbell Bench Press | Chest | Barbell | `ex_thumb_bench_press.png` | Exact (Barbell flat bench) |
| 2 | `ex_cable_row` | Cable Row | Back | Cables | `ex_thumb_cable_row.png` | Exact (Seated cable row) |
| 3 | `ex_bicep_curl` | Dumbbell Bicep Curl | Biceps | Dumbbells | `ex_thumb_bicep_curl.png` | Exact (Standing DB curls) |
| 4 | `ex_lateral_raise` | DB Lateral Raises | Shoulders | Dumbbells | `ex_thumb_lateral_raise.png` | Exact (Standing DB lateral raise) |
| 5 | `ex_back_squat` | Barbell Back Squat | Legs | Barbell | `ex_thumb_back_squat.png` | Exact (Free-weight barbell squat) |
| 6 | `ex_romanian_deadlift` | Romanian Deadlift | Legs | Barbell | `ex_thumb_romanian_deadlift.png` | Exact (Barbell RDL) |
| 7 | `ex_lat_pulldown` | Lat Pulldown | Back | Machine | `ex_thumb_lat_pulldown.png` | Exact (Seated cable/machine pulldown) |
| 8 | `ex_tricep_pushdown` | Cable Tricep Pushdown | Triceps | Cables | `ex_thumb_tricep_pushdown.png` | Exact (Cable tricep pushdown) |
| 9 | `ex_cable_fly` | Cable Fly | Chest | Cables | `ex_thumb_cable_fly.png` | Exact (Dual-pulley cable fly) |
| 10 | `ex_db_flat_press` | DB Flat Press | Chest | Dumbbells | `ex_thumb_db_flat_press.png` | Exact (Dedicated DB flat press) |
| 11 | `ex_leg_press` | Leg Press | Legs | Machine | `ex_thumb_leg_press.png` | Exact (45-deg leg press sled) |
| 12 | `ex_smith_squat` | Smith Squat | Legs | Machine | `ex_thumb_smith_squat.png` | Exact (Dedicated Smith machine squat) |
| 13 | `ex_db_preacher_curl` | DB Preacher Curl | Biceps | Dumbbells | `ex_thumb_preacher_curl.png` | Exact (Preacher bench DB curl) |
| 14 | `ex_leg_extension` | Leg Extension | Legs | Machine | `ex_thumb_leg_extension.png` | Exact (Seated leg extension) |
| 15 | `ex_db_incline_press` | DB Incline Press | Chest | Dumbbells | `ex_thumb_incline_press.png` | Exact (Incline DB press) |
| 16 | `ex_db_shoulder_press` | DB Shoulder Press | Shoulders | Dumbbells | `ex_thumb_shoulder_press.png` | Exact (Seated DB shoulder press) |
| 17 | `ex_barbell_row` | Bent-Over Barbell Row | Back | Barbell | `ex_thumb_barbell_row.png` | Exact (Bent-over barbell row) |
| 18 | `ex_calf_raise` | Standing Calf Raises | Legs | Machine | `ex_thumb_calf_raise.png` | Exact (Standing calf raise machine) |
