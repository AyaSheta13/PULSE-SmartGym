# P11 Screen Inventory & Navigation Verification

## Overview
All application screens, dialogs, and bottom sheets in PULSE Smart Gym are wired through Navigation 3 (`PulseNavigationEntryProvider.kt` and `PulseAppRoute.kt`) with clean state restoration and consistent design system tokens.

| Screen / Route | Primary ViewModel / File | P11 Status |
| --- | --- | --- |
| Welcome / Auth Entry | `AuthEntryScreen.kt` | Hero aspect ratio fixed (`378:436`), zero scroll |
| Onboarding (Pages 0–2) | `OnboardingScreen.kt` | Intrinsic aspect ratios (`378:436`, `1672:941`, `377:436`), badges anchored on image, zero scroll |
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
