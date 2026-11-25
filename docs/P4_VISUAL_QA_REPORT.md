# PULSE Smart Gym — P4 Visual QA Report

**Phase:** P4 — Splash, Onboarding, Authentication & Profile Setup  
**Reference Viewport:** `390 × 884` dp  
**Emulator:** `Medium_Phone_API_36.0 (AVD) - Android 16`  
**Figma File:** `UvskyKTyxlIOd68wQkp5cQ` (`PULSE---Smart-Gym`)

---

## 1. Visual Comparison Evidence Artifacts

All normalized Figma reference PNGs, captured Android emulator PNGs, and 3-panel (`Figma | Android | 3× Amplified AbsDiff`) comparison images are stored in the repository under:

- `docs/visual-qa/p4/figma/*.png`
- `docs/visual-qa/p4/android/*.png`
- `docs/visual-qa/p4/comparison/*.png`

---

## 2. Screen-by-Screen Visual QA & Measured Pixel Metrics

*Metrics below were computed directly on normalized `390 × 884` RGBA renders (`MAE` = Mean Absolute Error across RGB channels out of 255; `Within ±16` = percentage of pixels whose mean RGB difference from Figma is ≤ 16/255).*

| Screen | Figma Node | Android File | Measured MAE (0–255) | Pixels Within ±16 | Comparison Evidence | Remaining Difference | Status |
|---|---|---|---|---|---|---|---|
| Splash / Launch | `57:20479` | `SplashScreen.kt` | 6.64 | 96.23% | [01_splash.png](visual-qa/p4/comparison/01_splash.png) | Subtle radial glow gradient vs CSS blur | PASS |
| Onboarding 1 — Welcome | `57:20531` | `OnboardingScreen.kt` | 12.50 | 88.34% | [02_onboarding_1.png](visual-qa/p4/comparison/02_onboarding_1.png) | Font rasterization & flexible bottom spacer | PASS |
| Onboarding 2 — Track Your Way | `57:20254` | `OnboardingScreen.kt` | 13.50 | 87.21% | [03_onboarding_2.png](visual-qa/p4/comparison/03_onboarding_2.png) | Font rasterization & flexible bottom spacer | PASS |
| Onboarding 3 — Save & Grow | `57:20391` | `OnboardingScreen.kt` | 13.67 | 86.94% | [04_onboarding_3.png](visual-qa/p4/comparison/04_onboarding_3.png) | Font rasterization & flexible bottom spacer | PASS |
| Welcome / Auth Entry | `281:2` | `AuthEntryScreen.kt` | 16.56 | 75.71% | [05_auth_entry.png](visual-qa/p4/comparison/05_auth_entry.png) | Tightened `Elevate Every Rep` (`31sp` line height, `includeFontPadding = false`) & description (`19sp` line height) vertical rhythm; CTA stack aligned at `660dp` | PASS |
| Log In | `281:25` | `LoginScreen.kt` | 9.71 | 90.72% | [06_login.png](visual-qa/p4/comparison/06_login.png) | Restored `OR CONTINUE WITH` (`281:52`) + `Google` & `Apple` (`281:56`) buttons; full-width Email field | PASS |
| Log In — Validation Errors | `533:328` | `LoginScreen.kt` | 13.70 | 86.45% | [07_login_errors.png](visual-qa/p4/comparison/07_login_errors.png) | Error banner wraps cleanly on 2 lines without horizontal clipping | PASS |
| Create Account / Sign Up | `281:64` | `SignUpScreen.kt` | 17.66 | 83.14% | [08_signup.png](visual-qa/p4/comparison/08_signup.png) | Minor vertical rhythm difference from `48dp` touch-safe fields | PASS |
| Sign Up — Validation Errors | `533:356` | `SignUpScreen.kt` | 20.38 | 81.21% | [09_signup_errors.png](visual-qa/p4/comparison/09_signup_errors.png) | Retained `48dp` accessible touch-target input height vs `40px` in Figma error frame; error banner wraps cleanly | PASS |
| Forgot Password | `281:95` | `ForgotPasswordScreen.kt` | 10.42 | 90.09% | [10_forgot_password.png](visual-qa/p4/comparison/10_forgot_password.png) | Added password visibility toggle consistent with Login/Sign Up | PASS |
| Profile Setup — Personal Info | `282:2` | `PersonalInfoScreen.kt` | 5.43 | 94.07% | [11_profile_personal_info.png](visual-qa/p4/comparison/11_profile_personal_info.png) | Minor font rasterization differences only | PASS |
| Profile Setup — Fitness Goals | `282:55` | `FitnessGoalsScreen.kt` | 14.93 | 85.42% | [12_profile_fitness_goals.png](visual-qa/p4/comparison/12_profile_fitness_goals.png) | `heightIn(min = 68.dp)` prevents 2-line subtitle clipping on 3rd card | PASS |

---

## 3. Discrepancies Found & Fixed

1. **Auth Entry (`281:2`) Bottom CTA Vertical Alignment & Typography Rhythm:**
   - **Initial QA:** `MAE = 28.87`, `Within ±16 = 66.50%` due to bottom CTA stack sitting ~16–36dp too low.
   - **Fix:** Set top spacing to `50dp`, tightened `Elevate Every Rep` (`includeFontPadding = false`, `31sp` line height), description (`19sp` line height), and `14dp`/`6dp`/`12dp` spacing, and distributed remaining vertical space with `weight(0.615f)` above and `weight(0.385f)` below the CTA stack so it lands at `660dp` on `390 × 884` while scaling proportionally on smaller/larger screens.
   - **Re-verified:** `MAE = 16.56` and `Within ±16 = 75.71%`, with heading, subtitle, feature pills, and bottom CTA edges aligned to Figma.
2. **Restored Missing Login Elements (`281:52`, `281:56`):**
   - Added `OR CONTINUE WITH` divider (`1dp` `#F1EEEA` lines, `11sp` SemiBold `#A6A19C`) and `Google` / `Apple` buttons (`48dp` height, `#FFFFFF` background, `1dp` `#F1EEEA` border, `14dp` corner radius, `15sp` Bold `#1C1B1B`) directly below the `Log In` CTA, producing honest unavailable feedback on tap.
3. **Auth Entry (`281:2`) Feature Pills Wrapping & Fitness Goals (`282:55`) Subtitle Clipping:**
   - Adjusted `FeaturePill` padding (`8dp`), font size (`10.5sp`), and spacing (`6dp`) for single-row alignment at `390dp`, and switched Fitness Goals cards to `heightIn(min = 68.dp)` to eliminate 2-line subtitle clipping.
