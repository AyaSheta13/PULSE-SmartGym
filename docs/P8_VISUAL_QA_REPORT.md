# P8 Visual QA Report (`390 × 884`)

## Reference & Actual Screenshots

| Screen | Figma Node | Figma Reference | Actual Capture (`390 × 884`) | Visual Alignment |
| :--- | :--- | :--- | :--- | :--- |
| Ready / Start Set | `61:637` | [01_ready_start_set_61_637.png](visual-qa/p8/figma/01_ready_start_set_61_637.png) | [01_ready_start_set.png](visual-qa/p8/actual/01_ready_start_set.png) | **PASS** — Top bar, `Manual` chip (no fake BLE), `TARGET: 10 REPS @ 80 KG` pill, `Rest: 90s`, `PulseRepCounterGauge` (`0` / `GOAL 10`), `PulseWeightSelectorCard`, `Start Set` CTA, and `(■) End Workout` match layout and spacing. |
| Manual Rep Counter | `57:18098` / `653:447` | [03_manual_rep_counter_modified_653_447.png](visual-qa/p8/figma/03_manual_rep_counter_modified_653_447.png) | [02_manual_rep_counter.png](visual-qa/p8/actual/02_manual_rep_counter.png) | **PASS** — Top bar (`Set 2 of 4 • Manual Tracking`, `Manual` chip), coral progress arc (`8` / `GOAL 10`), `PulseWeightSelectorCard` (`80 kg`), `Finish Set` CTA, and `(■) End Workout` match Figma geometry. |
| Manual Rep Counter (Animated Intermediate) | `653:447` | [03_manual_rep_counter_modified_653_447.png](visual-qa/p8/figma/03_manual_rep_counter_modified_653_447.png) | [05_gauge_anim_intermediate.png](visual-qa/p8/actual/05_gauge_anim_intermediate.png) | **PASS** — Captured at `t = 160ms` with `mainClock.autoAdvance = false` during `0 -> 5` transition; coral arc is visibly mid-animation between `0.0` and `0.5`. |
| Manual Rep Counter (Animated Settled) | `653:447` | [03_manual_rep_counter_modified_653_447.png](visual-qa/p8/figma/03_manual_rep_counter_modified_653_447.png) | [04_manual_rep_counter_animated_settled.png](visual-qa/p8/actual/04_manual_rep_counter_animated_settled.png) | **PASS** — Verified rapid increments (`0 -> 3 -> 7 -> 9`) followed by decrement (`9 -> 8`) settle smoothly to the exact 80% coral arc (`320ms`, `FastOutSlowInEasing`), and `targetReps <= 0` renders safely. |
| Workout Session + Rest Timer | `61:715` | [02_session_1_of_16_rest_visible.png](../p7/figma/02_session_1_of_16_rest_visible.png) | [03_session_rest_visible.png](visual-qa/p8/actual/03_session_rest_visible.png) | **PASS** — `PulseRestTimerCard` (`REST` badge, `Rest after Barbell Bench Press`, `01:28`, `+30s`, `Skip Rest`, coral progress bar) renders cleanly between `SessionProgressCard` and `SessionExerciseCard`. |

## Acceptance Notes
- All screenshots were captured via `P8ScreenshotCaptureTest` at `390 × 884` (`density = 1f`) on `Medium_Phone_API_36.0(AVD)`.
- No BLE connection status or sensor metrics are fabricated; the top-right mode pill accurately displays `Manual` on P8 execution screens.
