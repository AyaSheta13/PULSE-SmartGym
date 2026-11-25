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
| Privacy & Local Data Sheet | `316:515` | `ProfileSheets.kt` | 54.30 | 36.85% | [06_sheet_privacy.png](visual-qa/p5/comparison/06_sheet_privacy.png) | Sheet card geometry & shield badge aligned; MAE reflects transparent scrim backdrop in isolated capture |
| Units & Measurements Sheet | `316:530` | `ProfileSheets.kt` | 61.43 | 30.38% | [07_sheet_units.png](visual-qa/p5/comparison/07_sheet_units.png) | Metric active & Imperial disabled rows aligned; MAE reflects isolated scrim backdrop |
| Account Preferences Sheet | `528:328` | `ProfileSheets.kt` | 64.97 | 28.63% | [08_sheet_account_preferences.png](visual-qa/p5/comparison/08_sheet_account_preferences.png) | Sheet card & CTA aligned; MAE reflects isolated scrim backdrop |

---

## 3. Discrepancies Found & Fixed

1. **Profile Stats `TARGET` Truncation (`283:110`):**
   - **Initial QA:** `"Hypertrop..."` truncated in `PhysicalStatsCard` at `390dp` width (`MAE = 5.79`, `Within ±16 = 93.34%`).
   - **Fix:** Adjusted column weights (`0.9f`, `1f`, `1f`, `1.4f`) and stat value font size (`14sp`).
   - **Re-verified:** `"Hypertrophy"` renders in full (`MAE = 5.58`, `Within ±16 = 93.47%`).
