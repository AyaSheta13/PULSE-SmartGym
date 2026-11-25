# PULSE Smart Gym — P4 Visual QA Report

**Phase:** P4 — Splash, Onboarding, Authentication & Profile Setup  
**Reference Viewport:** `390 × 884` dp  
**Emulator:** `Medium_Phone_API_36.0 (AVD) - Android 16`  
**Figma File:** `UvskyKTyxlIOd68wQkp5cQ` (`PULSE---Smart-Gym`)

---

## 1. Screen-by-Screen Visual QA Matrix

| Screen | Figma Node | Android Implementation | Reference Captured | Emulator Screenshot Captured | Layout Compared | Typography Compared | Icons/Assets Compared | Form/Selection States Tested | Remaining Difference | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| Splash / Launch | `57:20479` | `SplashScreen.kt` | Yes | Yes (`01_splash.png`) | Yes | Yes | Yes (`ic_pulse_bolt_splash`) | N/A (Startup resolution) | Subtle radial glow blur rendered via `Brush.radialGradient` instead of CSS blur filter | PASS |
| Onboarding 1 — Welcome | `57:20531` | `OnboardingScreen.kt` | Yes | Yes (`02_onboarding_1.png`) | Yes | Yes | Yes (`onboarding_hero_1`) | Yes (Next, Skip) | None significant | PASS |
| Onboarding 2 — Track Your Way | `57:20254` | `OnboardingScreen.kt` | Yes | Yes (`03_onboarding_2.png`) | Yes | Yes | Yes (`onboarding_hero_2`) | Yes (Back, Next, Skip) | None significant | PASS |
| Onboarding 3 — Save & Grow | `57:20391` | `OnboardingScreen.kt` | Yes | Yes (`04_onboarding_3.png`) | Yes | Yes | Yes (`onboarding_hero_3`) | Yes (Back, Get Started) | None significant | PASS |
| Welcome / Auth Entry | `281:2` | `AuthEntryScreen.kt` | Yes | Yes (`05_auth_entry.png`) | Yes | Yes | Yes (`ic_pulse_logo_mark`, `onboarding_hero_1`) | Yes (Create Account, Log In, Guest) | Fixed 3-pill horizontal row alignment on 390dp; `FlowRow` wraps cleanly on narrower 360dp | PASS |
| Log In | `281:25` | `LoginScreen.kt` | Yes | Yes (`06_login.png`) | Yes | Yes | Yes (`VisibilityOn`/`Off`) | Yes (Email, Password, Remember Me, Toggle) | Full-width Email input matches intended layout (Figma `281:25` had unstretched box) | PASS |
| Log In — Validation Errors | `533:328` | `LoginScreen.kt` | Yes | Yes (`07_login_errors.png`) | Yes | Yes | Yes | Yes (Banner + field error states) | Error banner wraps cleanly on 2 lines without horizontal clipping | PASS |
| Create Account / Sign Up | `281:64` | `SignUpScreen.kt` | Yes | Yes (`08_signup.png`) | Yes | Yes | Yes (`VisibilityOn`/`Off`) | Yes (All 4 fields, toggles, CTA) | None significant | PASS |
| Sign Up — Validation Errors | `533:356` | `SignUpScreen.kt` | Yes | Yes (`09_signup_errors.png`) | Yes | Yes | Yes | Yes (All 4 error scenarios + banner) | Error banner wraps cleanly without right-edge truncation | PASS |
| Forgot Password | `281:95` | `ForgotPasswordScreen.kt` | Yes | Yes (`10_forgot_password.png`) | Yes | Yes | Yes (`VisibilityOn`/`Off`) | Yes (Reset blocked honestly when unauthenticated) | Added password visibility toggle consistent with Login/Sign Up | PASS |
| Profile Setup — Personal Info | `282:2` | `PersonalInfoScreen.kt` | Yes | Yes (`11_profile_personal_info.png`) | Yes | Yes | N/A | Yes (Name, Gender, Age/Height/Weight, Coming Soon notice) | None significant | PASS |
| Profile Setup — Fitness Goals | `282:55` | `FitnessGoalsScreen.kt` | Yes | Yes (`12_profile_fitness_goals.png`) | Yes | Yes | Yes (`ic_goal_hypertrophy`, `ic_goal_strength`, `ic_goal_fitness`) | Yes (Goal, Experience, Weekly frequency, Back preservation) | Fixed `heightIn(min = 68.dp)` so 2-line subtitle on 3rd card never clips | PASS |

---

## 2. Visual Discrepancies Found & Fixed During Emulator QA

1. **Auth Entry (`281:2`) Feature Pills Wrapping at 390dp:**
   - **Found in `05_auth_entry.png`:** `✦ Local Analytics` wrapped onto a second row due to `10dp` horizontal pill padding and `8dp` spacing.
   - **Fix:** Adjusted `FeaturePill` horizontal padding to `8dp`, font size to `10.5sp`, and spacing to `6dp` so all 3 pills fit on one horizontal row at `390dp` while retaining `FlowRow` wrapping on narrower `360dp` screens.
   - **Verified:** Re-captured `05_auth_entry.png` and confirmed single-row alignment.

2. **Fitness Goals (`282:55`) 3rd Card Subtitle Clipping:**
   - **Found in `12_profile_fitness_goals.png`:** Fixed `height(68.dp)` clipped the bottom of `"momentum"` when `"Maintain health & consistent workout momentum"` wrapped to two lines.
   - **Fix:** Switched card height to `heightIn(min = 68.dp)` with `11.5sp` / `15sp` line height on the subtitle.
   - **Verified:** Re-captured `12_profile_fitness_goals.png` and confirmed zero text clipping.
