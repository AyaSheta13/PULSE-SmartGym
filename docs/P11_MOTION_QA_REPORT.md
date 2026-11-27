# P11 Motion QA Report

## Verified Transitions & Animations
- **Bottom Sheet Overlay (`PulseAnimatedBottomSheetOverlay`)**: Smooth 300ms upward enter + scrim fade-in and 240ms downward exit + scrim fade-out; stays mounted until exit completes.
- **Onboarding Pager & Indicator**: Smooth horizontal page transitions with animated indicator pill width/color.
- **Exercise Picker Selection & Navigation**: Immediate circular `+` -> `✓` coral state feedback; search query, muscle filter, selection, and `LazyListState` scroll position are preserved across navigation to `ExerciseDetails` and back.
- **Set Execution Live Metrics & Expandable Breakdown**:
  - Rep ring progress arc animates smoothly as `fullReps` updates.
  - `ExpandableRepetitionBreakdown` defaults to collapsed (`Show`) and expands/collapses cleanly via `button_toggle_rep_breakdown` without layout jumps or overlap with the sticky bottom `Finish Set` bar.
