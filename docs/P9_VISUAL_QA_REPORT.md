# P9 Visual QA Report

All 9 P9 screen states were re-captured on `Medium_Phone_API_36.0` (`emulator-5554`) at `390 × 884` via `P9ScreenshotCaptureTest` after applying the P9 Final Acceptance Patch visual fixes and compared against the Figma exports in `docs/visual-qa/p9/figma/`.

## Visual Fixes Applied
- **Session Details (`SessionDetailsScreen.kt`) & Workout Summary (`WorkoutResultScreen.kt`)**: Replaced fixed-weight chip `Row`s with `FlowRow` and removed `maxLines = 1, Ellipsis` clipping so `16 sets`, `140 repetitions`, `7,472 kg volume`, and `16 completed sets` render completely without truncation and wrap cleanly for large KG/LBS or `(partial)` values.
- **Training Progress (`TrainingProgressScreen.kt`)**: Updated `ProgressMetricCard` to use `FlowRow` for value + unit and `maxLines = 2` on unit and delta labels so `tonnes (14,872 kg)` and `No prior volume • 75% weekly target` display without ellipsis truncation.
- **PR Fixture Dates (`P9ScreenshotCaptureTest.kt`)**: Passed each sample session's real October timestamp (`Oct 24`, `Oct 22`, `Oct 19`) instead of `1_000L` (`Jan 1, 1970`).

## Pixel-Tolerance Comparison (`±16` RGB tolerance)

| State | Dimensions | Within ±16 | Comparison Artifact |
|---|---|---|---|
| `01_history_populated.png` | `390 × 884` | `88.49%` | `docs/visual-qa/p9/comparison/01_history_populated_compare.png` |
| `02_history_empty.png` | `390 × 884` | `90.31%` | `docs/visual-qa/p9/comparison/02_history_empty_compare.png` |
| `03_history_partial.png` | `390 × 884` | `88.05%` | `docs/visual-qa/p9/comparison/03_history_partial_compare.png` |
| `04_session_details_complete.png` | `390 × 884` | `87.08%` | `docs/visual-qa/p9/comparison/04_session_details_complete_compare.png` |
| `05_session_details_partial.png` | `390 × 884` | `86.95%` | `docs/visual-qa/p9/comparison/05_session_details_partial_compare.png` |
| `06_progress_populated.png` | `390 × 884` | `85.22%` | `docs/visual-qa/p9/comparison/06_progress_populated_compare.png` |
| `07_progress_empty.png` | `390 × 884` | `86.89%` | `docs/visual-qa/p9/comparison/07_progress_empty_compare.png` |
| `08_workout_complete_summary.png` | `390 × 884` | `86.06%` | `docs/visual-qa/p9/comparison/08_workout_complete_summary_compare.png` |
| `09_partial_workout_summary.png` | `390 × 884` | `86.22%` | `docs/visual-qa/p9/comparison/09_partial_workout_summary_compare.png` |
