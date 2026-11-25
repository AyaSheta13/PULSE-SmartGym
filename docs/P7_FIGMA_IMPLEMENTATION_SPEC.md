# PULSE Smart Gym — P7 Figma Implementation Specification

## 1. Authoritative Figma References
- **Primary Section:** Section F — Workout Execution (`73:6`)
- **Exit Summary References:** Section G — History & Analytics (`57:18241`, `776:892`)
- **Reference Viewport:** `390 × 884` (`64dp` top bar, `732dp` scrollable content, `88dp` fixed bottom action bar)
- **Theme & Typography:** `PulseTheme` + Plus Jakarta Sans (`WorkoutPalette.Background = #FCF9F8`, `WorkoutPalette.PrimaryCoral = #FF623E`, `WorkoutPalette.TextPrimary = #191919`)

## 2. Node-to-Implementation Mapping

| State / Screen | Figma Node | Compose Implementation |
|---|---|---|
| Workout Session — New Session (`0/16`) | `57:20057` | `WorkoutSessionScreen` (`WorkoutSessionUiState.Active`, 0 recorded sets) |
| Workout Session — `1/16` Complete, Rest Dismissed | `61:715`, `776:639` | `WorkoutSessionScreen` (1 recorded set, remaining sets active, no P8 countdown) |
| Workout Session — Non-Sequential Sets | `61:793` | `WorkoutSessionScreen` (independent per-row `PerformedSet` matching by `setIndex`) |
| Workout Session — Exercise 1 Complete | `61:871` | `SessionExerciseCard` (`✓ 4/4` completion pill, full coral progress bar) |
| Workout Session — All Complete | `646:349` | `WorkoutSessionScreen` (bottom CTA switches to Coral `Finish Workout`) |
| End Workout Early — Confirmation | `61:1248` | `EndWorkoutEarlyModal` (`0.55f` scrim, `16.dp` top corners, `Keep Working Out` + `End Workout`) |
| Workout Complete Summary | `57:18241` | `WorkoutResultScreen` (`SessionStatus.COMPLETED`) |
| Workout Summary — Early Termination | `776:892` | `WorkoutResultScreen` (`SessionStatus.PARTIALLY_COMPLETED` / `ABANDONED`) |
| P7 Manual Set Entry Bridge | Derived state | `ManualSetEntrySheet` (temporary P7 manual logging & set correction sheet) |

## 3. Intentional P7 Scope & Truthful Deviations
- **Device Status Chip:** Displays truthful `Not Connected` instead of Figma's hardcoded `Connected` chip (BLE/ESP32 deferred to P10).
- **Rest Countdown (`61:715`):** Active countdown timer, `+30s`, and `Skip Rest` are deferred to P8; P7 implements the `776:639` rest-dismissed layout.
- **Summary Exit Action:** Displays `Back to Home` as the primary CTA (`57:18241` / `776:892` `View History` is deferred until the History tab is implemented).
- **Set Correction:** Recorded set rows include a compact `Edit` pill calling `correctPerformedSet(...)` without silently overwriting recorded reps.
