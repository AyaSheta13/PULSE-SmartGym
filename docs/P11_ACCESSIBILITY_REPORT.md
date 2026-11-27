# P11 Accessibility Report

## Touch Targets & Semantics
- **Exercise Picker Cards (`ExercisePickerScreen.kt`)**:
  - Left circular selection control uses a `48.dp` minimum touch target (`Modifier.size(48.dp)`) with a `26.dp` visual circle and explicit `Role.Checkbox` / selection state semantics.
  - Card body & right chevron (`picker_details_<id>`, `picker_chevron_<id>`) remain clickable with clear content descriptions to open `ExerciseDetails`.
  - Exercise titles allow up to 2 lines (`maxLines = 2`, `TextOverflow.Ellipsis`) to prevent truncation at standard and large font scales.
- **Set Execution & Live Metrics (`SetExecutionScreen.kt`, `ReadyStartSetScreen.kt`)**:
  - `PulseBluetoothChip` clearly distinguishes connection status (`Connected (Sim)`, `Disconnected`) from tracking mode (`Manual`, `Automatic`) via distinct semantic labels.
  - `ExpandableRepetitionBreakdown` toggle (`button_toggle_rep_breakdown`) exposes clear `Show` / `Hide` action text and state.
- **Welcome & Onboarding (`AuthEntryScreen.kt`, `OnboardingScreen.kt`)**:
  - Zero vertical scrolling required on 360dp, 390dp, and 412dp viewports; primary CTAs and guest entry remain immediately reachable.
