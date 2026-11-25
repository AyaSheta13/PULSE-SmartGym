# PULSE Smart Gym — P5 Visual QA Report

**Phase:** P5 — Home, Main Navigation & Profile  
**Reference Viewport:** `390 × 884` dp  
**Emulator:** `Medium_Phone_API_36.0 (AVD) - Android 16`  
**Figma File:** `UvskyKTyxlIOd68wQkp5cQ` (`PULSE---Smart-Gym`)

---

## 1. Visual Comparison Evidence Artifacts

Normalized Figma references, Android emulator captures, and 3-panel (`Figma | Android | 3× Amplified AbsDiff`) comparison images are stored under:

- `docs/visual-qa/p5/figma/*.png`
- `docs/visual-qa/p5/android/*.png`
- `docs/visual-qa/p5/comparison/*.png`

---

## 2. Screen-by-Screen Visual QA & Measured Pixel Metrics

*Metrics below were computed directly on normalized `390 × 884` RGBA renders (`MAE` = Mean Absolute Error across RGB channels out of 255; `Within ±16` = percentage of pixels whose mean RGB difference from Figma is ≤ 16/255).*

| Screen | Figma Node | Android File | Measured MAE (0–255) | Pixels Within ±16 | Comparison Evidence | Notes |
|---|---|---|---|---|---|---|
| Home — No Plans (Derived) | Derived (`316:290`) | `HomeScreen.kt` | N/A | N/A | [01_home_no_plans.png](visual-qa/p5/android/01_home_no_plans.png) | Derived empty-plan state consistent with First Workout design |
| Home — First Workout | `316:290` | `HomeScreen.kt` | 24.23 | 83.42% | [02_home_first_workout.png](visual-qa/p5/comparison/02_home_first_workout.png) | Omits fabricated `45 min`/`50 min`/`40 min` durations in favor of real planned set counts |
| Home — Populated | `57:19799` | `HomeScreen.kt` | 17.46 | 85.66% | [03_home_populated.png](visual-qa/p5/comparison/03_home_populated.png) | Hero image, `TODAY'S WORKOUT` pill, 7-bar weekly chart, and `Your Workouts` list aligned |
| Profile & Settings | `283:110` | `ProfileScreen.kt` | 5.58 | 93.47% | [04_profile_settings.png](visual-qa/p5/comparison/04_profile_settings.png) | Widened `TARGET` stat column so `"Hypertrophy"` renders without ellipsis; honest BLE P10 subtitle |
| Edit Profile | `283:197` | `EditProfileScreen.kt` | 13.99 | 86.86% | [05_edit_profile.png](visual-qa/p5/comparison/05_edit_profile.png) | Real editable `BasicTextField` for arbitrary names; read-only email field with honest helper caption |
| Privacy & Local Data Sheet | `316:515` | `ProfileSheets.kt` | 25.66 | 79.61% | [06_sheet_privacy.png](visual-qa/p5/comparison/06_sheet_privacy.png) | Captured via real Profile row tap + `uiAutomation.takeScreenshot()` (full window + scrim + sheet); remaining diff is live Profile UI visible behind Android's 55% scrim vs Figma's solid dark canvas |
| Units & Measurements Sheet | `316:530` | `ProfileSheets.kt` | 29.16 | 78.89% | [07_sheet_units.png](visual-qa/p5/comparison/07_sheet_units.png) | Captured via real Profile row tap + `uiAutomation.takeScreenshot()`; Metric active & Imperial disabled rows aligned |
| Account Preferences Sheet | `528:328` | `ProfileSheets.kt` | 37.99 | 72.23% | [08_sheet_account_preferences.png](visual-qa/p5/comparison/08_sheet_account_preferences.png) | Captured via real Profile row tap + `uiAutomation.takeScreenshot()`; copy & CTA aligned |
| Responsive `360dp` & `412dp` (`1.3×` Font Scale) | Responsive | `HomeScreen.kt`, `ProfileScreen.kt` | N/A | N/A | [09_home_360dp.png](visual-qa/p5/android/09_home_360dp.png), [10_home_412dp_font130.png](visual-qa/p5/android/10_home_412dp_font130.png), [11_profile_360dp.png](visual-qa/p5/android/11_profile_360dp.png), [12_profile_412dp_font130.png](visual-qa/p5/android/12_profile_412dp_font130.png) | Verified across 360dp, 412dp, and 1.3× font scale; updated `MyPulseDeviceCard` & `SettingsMenuRow` to `heightIn(min = ...)` so scaled text never clips |

---

## 3. Discrepancies Found & Fixed

1. **Profile Stats `TARGET` Truncation (`283:110`):**
   - **Initial QA:** `"Hypertrop..."` truncated in `PhysicalStatsCard` at `390dp` width (`MAE = 5.79`, `Within ±16 = 93.34%`).
   - **Fix:** Adjusted column weights (`0.9f`, `1f`, `1f`, `1.4f`) and stat value font size (`14sp`).
   - **Re-verified:** `"Hypertrophy"` renders in full (`MAE = 5.58`, `Within ±16 = 93.47%`).
2. **Real Full-Window Sheet Captures (`316:515`, `316:530`, `528:328`):**
   - **Initial QA:** Isolated `inWindow = true` captures lacked the real Profile screen beneath the scrim (`MAE = 54.30–64.97`, `Within ±16 = 28.63%–36.85%`).
   - **Fix:** Updated `P5ScreenshotCaptureTest.captureRealProfileSheetsWithNavigation` to navigate to `Profile`, tap each real settings row, and capture the full rendered window via `uiAutomation.takeScreenshot()`.
   - **Re-verified:** `Within ±16` improved to `79.61%` (Privacy), `78.89%` (Units), and `72.23%` (Account Preferences).
3. **1.3× Font Scale Clipping on `ProfileScreen` (`412dp`):**
   - Switched `MyPulseDeviceCard` (`height(72.dp)`) and `SettingsMenuRow` (`height(44.dp)`) to `heightIn(min = ...)` with `8.dp` vertical row padding and weighted label spacing so `1.3×` scaled text wraps cleanly without clipping.

## 4. Outstanding Visual Differences

- **Home Plan Card Durations (`316:290`, `57:19799`):** Intentionally omit Figma's sample `45 min` / `50 min` / `40 min` durations because `WorkoutPlan` does not store workout durations.
- **Edit Profile Email Field (`283:197`):** Intentionally styled as a muted read-only container (`#F7F6F2`) with an explanatory caption rather than an editable email input, and omits Figma's demo preset chips.
- **Sheets Backdrop (`316:515`, `316:530`, `528:328`):** Live emulator renders show the dimmed `Profile & Settings` screen behind the 55% scrim, whereas the exported Figma frames use a solid dark canvas.
- **P4 Deferred Item:** `AuthEntryScreen.kt` (`281:2`) retains its previously documented P4 residual visual QA note (`MAE = 16.56`, `Within ±16 = 75.71%`).
