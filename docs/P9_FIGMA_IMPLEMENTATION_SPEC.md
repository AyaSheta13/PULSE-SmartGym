# P9 Figma Implementation Spec

- **Figma File Key**: `UvskyKTyxlIOd68wQkp5cQ`
- **Reference Viewport**: `390 × 884`

## Screen & State Mapping

| # | State | Figma Node | Reference PNG | Android Implementation |
|---|---|---|---|---|
| 1 | History (Populated) | `57:18450` | `docs/visual-qa/p9/figma/01_history_populated.png` | `WorkoutHistoryScreen` (`WorkoutHistoryUiState.Populated`) |
| 2 | History (Empty) | `61:284` | `docs/visual-qa/p9/figma/02_history_empty.png` | `WorkoutHistoryScreen` (`WorkoutHistoryUiState.Empty`) |
| 3 | History (Partial Session) | `788:703` | `docs/visual-qa/p9/figma/03_history_partial.png` | `WorkoutHistoryScreen` (partial session row + `RECENT` header) |
| 4 | Session Details (Complete) | `57:18737` | `docs/visual-qa/p9/figma/04_session_details_complete.png` | `SessionDetailsScreen` (`COMPLETED`) |
| 5 | Session Details (Partial) | `788:632` | `docs/visual-qa/p9/figma/05_session_details_partial.png` | `SessionDetailsScreen` (`PARTIALLY_COMPLETED`, `Skipped` badges) |
| 6 | Training Progress (Populated) | `283:2` | `docs/visual-qa/p9/figma/06_progress_populated.png` | `TrainingProgressScreen` (`TrainingProgressUiState.Populated`) |
| 7 | Training Progress (Empty) | `310:312` | `docs/visual-qa/p9/figma/07_progress_empty.png` | `TrainingProgressScreen` (`TrainingProgressUiState.Empty`) |
| 8 | Workout Complete Summary | `57:18241` | `docs/visual-qa/p9/figma/08_workout_complete_summary.png` | `WorkoutResultScreen` (`COMPLETED` + `View History`) |
| 9 | Partial Workout Summary | `776:892` | `docs/visual-qa/p9/figma/09_partial_workout_summary.png` | `WorkoutResultScreen` (`PARTIALLY_COMPLETED` + `View History`) |
