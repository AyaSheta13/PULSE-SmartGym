# PULSE Smart Gym — P6 Figma Implementation Specification

**Target Viewport:** `390 × 884` (`Plus Jakarta Sans` via `PulseTheme`)  
**Figma File:** `UvskyKTyxlIOd68wQkp5cQ` (`node-id=73-4`, `73:3`, `73:7`)

## 1. Screen & State Inventory

| # | Screen / State | Figma Node | Compose Implementation |
|---|---|---|---|
| 1 | My Workouts | `57:19917` | [MyWorkoutsScreen.kt](../app/src/main/java/com/ayasheta/pulsesmartgym/feature/workout/list/MyWorkoutsScreen.kt) |
| 2 | My Workouts — Plan Deleted | `61:1842` | [MyWorkoutsScreen.kt](../app/src/main/java/com/ayasheta/pulsesmartgym/feature/workout/list/MyWorkoutsScreen.kt) (reactive list state) |
| 3 | Create Workout Plan | `605:338` | [WorkoutPlanEditorScreen.kt](../app/src/main/java/com/ayasheta/pulsesmartgym/feature/workout/editor/WorkoutPlanEditorScreen.kt) (`CREATE` mode) |
| 4 | Edit Workout Plan | `57:18885` | [WorkoutPlanEditorScreen.kt](../app/src/main/java/com/ayasheta/pulsesmartgym/feature/workout/editor/WorkoutPlanEditorScreen.kt) (`EDIT` mode) |
| 5 | Exercise Picker | `57:19181` | [ExercisePickerScreen.kt](../app/src/main/java/com/ayasheta/pulsesmartgym/feature/workout/picker/ExercisePickerScreen.kt) |
| 6 | Workout Details — Exercise 1 Expanded | `335:2` | [WorkoutDetailsScreen.kt](../app/src/main/java/com/ayasheta/pulsesmartgym/feature/workout/details/WorkoutDetailsScreen.kt) |
| 7 | Workout Details — Exercise 2 Expanded | `335:65` | [WorkoutDetailsScreen.kt](../app/src/main/java/com/ayasheta/pulsesmartgym/feature/workout/details/WorkoutDetailsScreen.kt) |
| 8 | Workout Details — Exercise 3 Expanded | `626:322` | [WorkoutDetailsScreen.kt](../app/src/main/java/com/ayasheta/pulsesmartgym/feature/workout/details/WorkoutDetailsScreen.kt) |
| 9 | Workout Details — Exercise 4 Expanded | `626:329` | [WorkoutDetailsScreen.kt](../app/src/main/java/com/ayasheta/pulsesmartgym/feature/workout/details/WorkoutDetailsScreen.kt) |
| 10 | Workout Details — All Collapsed | `626:336` | [WorkoutDetailsScreen.kt](../app/src/main/java/com/ayasheta/pulsesmartgym/feature/workout/details/WorkoutDetailsScreen.kt) |
| 11 | Delete Plan Confirmation | `61:1454` | `DeletePlanConfirmationModal` in [WorkoutPlanEditorScreen.kt](../app/src/main/java/com/ayasheta/pulsesmartgym/feature/workout/editor/WorkoutPlanEditorScreen.kt) |

## 2. Design Tokens & Layout Measurements

- **Background Surfaces:** `#FCF9F8` (My Workouts, Edit Plan, Exercise Picker, Workout Details), `#FAFAFA` (Create Plan), `#FFFFFF` (Cards & Top/Bottom Bars).
- **Primary Accent:** `#FF623E` (CTAs, selected states, expanded badges, active chips).
- **Secondary / Neutral:** `#F0EDED` (inputs & muscle chips), `#F2F2F5` (stepper tiles), `#F9FAFB` (details metric tiles), `#6FFBBE` / `#002113` (equipment pill), `#EAE7E7` (Cancel), `#EEEAE6` (Delete Plan).
- **Top App Bars:** `64.dp` height (`PulseTopAppBar`), `1.dp` `#EDE8E3` bottom divider.
- **Typography:** `Plus Jakarta Sans` (`18.sp` SemiBold/Bold titles, `15.sp` Bold/Medium exercise names, `14.sp` Medium subtitles, `11.sp` Bold uppercase field labels & chips, `26.sp` ExtraBold stepper numbers).
- **Corner Radii:** `16.dp` (Details cards, Exercise Picker cards, Delete modal top corners, Details primary/secondary buttons), `12.dp` (My Workouts cards, Overview card, Editor CTA buttons, Picker toggle button), `10.dp` (Stepper & Metric tiles), `9999.dp` (Filter & category pills).
