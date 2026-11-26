# P9 Visual QA Report

All 9 P9 screen states were captured on `Medium_Phone_API_36.0` (`emulator-5554`) at `390 × 884` via `P9ScreenshotCaptureTest` and compared against the Figma exports in `docs/visual-qa/p9/figma/`.

## Pixel-Tolerance Comparison (`±16` RGB tolerance)

| State | Dimensions | Within ±16 | Comparison Artifact |
|---|---|---|---|
| `01_history_populated.png` | `390 × 884` | `88.49%` | `docs/visual-qa/p9/comparison/01_history_populated_compare.png` |
| `02_history_empty.png` | `390 × 884` | `90.31%` | `docs/visual-qa/p9/comparison/02_history_empty_compare.png` |
| `03_history_partial.png` | `390 × 884` | `88.05%` | `docs/visual-qa/p9/comparison/03_history_partial_compare.png` |
| `04_session_details_complete.png` | `390 × 884` | `87.13%` | `docs/visual-qa/p9/comparison/04_session_details_complete_compare.png` |
| `05_session_details_partial.png` | `390 × 884` | `86.97%` | `docs/visual-qa/p9/comparison/05_session_details_partial_compare.png` |
| `06_progress_populated.png` | `390 × 884` | `87.13%` | `docs/visual-qa/p9/comparison/06_progress_populated_compare.png` |
| `07_progress_empty.png` | `390 × 884` | `86.89%` | `docs/visual-qa/p9/comparison/07_progress_empty_compare.png` |
| `08_workout_complete_summary.png` | `390 × 884` | `86.10%` | `docs/visual-qa/p9/comparison/08_workout_complete_summary_compare.png` |
| `09_partial_workout_summary.png` | `390 × 884` | `86.25%` | `docs/visual-qa/p9/comparison/09_partial_workout_summary_compare.png` |

## Visual Findings
- All 9 screens match the Figma warm off-white surface palette, card radii, coral primary CTAs, typography hierarchy, bottom navigation active states, bar chart proportions, and partial/skipped badge treatments.
- Minor pixel differences stem from Android system font rasterization and test-fixture date/truncation strings (`tonnes (14,872 ...`, `Jan 1, 1970` from epoch `0L` in fixture).
