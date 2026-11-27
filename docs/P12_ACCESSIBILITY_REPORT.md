# P12 Accessibility Report

## Automated & Inspected Checks
- **Font Scaling (`1.3f`)**: Verified on `SetExecutionScreen` (`P12RegressionAndAccessibilityUiTest.responsiveLargeViewport480x960_andFontScale130_preserveControlsAndSemantics`), confirming all primary workout controls, weight input trigger, and `End Workout` remain unclipped and readable.
- **Responsive Viewports**: Verified at `360 × 640 dp`, `390 × 844 dp`, and `411 × 914 dp` (`Medium_Phone_API_36.0`).
- **Content Descriptions & Semantics**: Verified `"Add one rep"`, `"Subtract one rep"`, `"Decrease weight"`, `"Increase weight"`, `"Edit load weight..."`, `"Motion trend chart info"`, and `button_set_execution_back`.
- **Reduced Motion**: `PulseNavigationTransitions` respects `ValueAnimator.areAnimatorsEnabled()` (guarded for API 24–25 compatibility).

## Checks Not Performed
- **Live TalkBack Screen-Reader Walkthrough**: Not performed interactively with audio output on a physical device; verified via Compose semantics tree assertions instead.
- **Physical 480×960 Tablet / Foldable**: Tested at emulator native `411 × 914 dp` (`Medium_Phone_API_36.0`) and constrained `360 × 640 dp` / `390 × 844 dp` Compose test bounds.
