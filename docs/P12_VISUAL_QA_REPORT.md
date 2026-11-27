# P12 Visual QA Report

## Scope
Verified Figma (`UvskyKTyxlIOd68wQkp5cQ`) vs. Android Compose parity across Onboarding, Welcome, My Workouts, Exercise Picker, Set Execution (Ready, InProgress, Connection Lost, Set Load Direct Weight Input), and Developer Hardware Diagnostics.

## P12 Visual Evidence (`docs/visual-qa/p12/` & `docs/visual-qa/p11/`)
- **Set Execution Connection Lost (`p12_set_execution_connection_lost.png`)**: Verifies amber `Connection Lost` chip, `AUTO REPS 3 /10`, `Live Performance` `Rep #3` isolated point after missing Rep #2 metric, and `Device Disconnected` recovery sheet.
- **Set Execution Large Font Scale (`p12_set_execution_480x960_font130.png`)**: Verifies `1.3x` font scaling keeps `WorkoutInfoCard`, `UnifiedRepAndWeightCard`, `LivePerformanceWaveformCard`, `Start Set`, and `End Workout` visible and unclipped.
- **Set Execution Live Metrics & Direct Weight (`p11_set_execution_live_metrics.png`, `p11_set_execution_ready_360x640.png`, `p11_set_load_sheet.png`)**: Verified against Figma `figma_se_ready_v2.png`, `figma_se_inprogress_v2.png`, `figma_set_load_sheet.png`.
- **Developer Hardware Diagnostics (`p11_hardware_diagnostics.png`)**: Verified `RAW REPLAY (DEV)` badge, counters, raw hex, decoded summary, and CRC-8 error display.
