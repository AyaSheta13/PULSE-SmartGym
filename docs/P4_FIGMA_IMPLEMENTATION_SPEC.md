# PULSE Smart Gym — P4 Figma Implementation Specification

**Phase:** P4 — Splash, Onboarding, Authentication & Profile Setup  
**Figma File:** `UvskyKTyxlIOd68wQkp5cQ` (`PULSE---Smart-Gym`)  
**Reference Viewport:** `390 × 884 px`  
**Font Family:** `Plus Jakarta Sans` (Regular 400, Medium 500, SemiBold 600, Bold 700, ExtraBold 800)

---

## 1. Screen-to-Component & Asset Map

| Screen | Figma Node | Existing DS Components Reused | P4 Screen / Shared Components | Exported Figma Assets |
|---|---|---|---|---|
| Splash / Launch | `57:20479` | `PulseTheme` | `SplashScreen` | `ic_pulse_bolt_splash.xml` |
| Onboarding 1 — Welcome | `57:20531` | `PulseTheme`, `PulseTopAppBar` (title + trailing Skip) | `OnboardingScreen` (Page 0) | `onboarding_hero_1.png` |
| Onboarding 2 — Track Your Way | `57:20254` | `PulseTheme`, `PulseTopAppBarStyle.BackWithTitleAndAction` | `OnboardingScreen` (Page 1) | `onboarding_hero_2.png` |
| Onboarding 3 — Save & Grow | `57:20391` | `PulseTheme`, `PulseTopAppBarStyle.BackWithTitle` | `OnboardingScreen` (Page 2) | `onboarding_hero_3.png` |
| Welcome / Auth Entry | `281:2` | `PulseTheme` | `AuthEntryScreen` | `ic_pulse_logo_mark.xml`, `onboarding_hero_1.png` |
| Log In | `281:25` | `PulseTheme`, `PulseTopAppBarStyle.BackWithTitle`, `CheckBox` | `LoginScreen`, `PulseAuthFormField` | `VisibilityOn`, `VisibilityOff` |
| Log In — Validation Errors | `533:328` | Same `LoginScreen` state | `PulseValidationBanner` + error field styling | — |
| Create Account / Sign Up | `281:64` | `PulseTheme`, `PulseTopAppBarStyle.BackWithTitle` | `SignUpScreen`, `PulseAuthFormField` | `VisibilityOn`, `VisibilityOff` |
| Sign Up — Validation Errors | `533:356` | Same `SignUpScreen` state | `PulseValidationBanner` + error field styling | — |
| Forgot Password | `281:95` | `PulseTheme`, `PulseTopAppBarStyle.BackWithTitle` | `ForgotPasswordScreen` | — |
| Profile Setup — Personal Info | `282:2` | `PulseTheme`, `PulseTopAppBarStyle.BackWithTitle` | `PersonalInfoScreen` | — |
| Profile Setup — Fitness Goals | `282:55` | `PulseTheme`, `PulseTopAppBarStyle.BackWithTitle` | `FitnessGoalsScreen` | `ic_goal_hypertrophy.xml`, `ic_goal_strength.xml`, `ic_goal_fitness.xml` |

---

## 2. Screen-by-Screen Reference Specification (390 × 884)

### 2.1 Splash / Launch (`57:20479`)
- **Background:** `#FCF9F8` with top-center ambient coral radial glow (`rgba(255,98,62,0.10)`, `384dp`) and bottom-center sky radial glow (`rgba(57,184,253,0.10)`, `320dp`).
- **Center Stage:**
  - Tactile squircle (`112 × 112dp`, `28dp` corner radius, `#FF623E`, ambient coral aura + drop shadow) centered horizontally, containing `ic_pulse_bolt_splash` (`50 × 56dp`, white).
  - Wordmark (`24dp` below squircle): `PULSE` (`32sp` ExtraBold, `#1C1B1B`, `-0.8sp` letter spacing) + `10dp` `#FF623E` circle dot (`4dp` gap, baseline-aligned).
  - Subtitle (`4dp` below): `SMART GYM` (`12sp` Bold, `#5A413B`, `3sp` letter spacing, uppercase).
- **Functional Behavior:** Resolves startup destination from `UserPreferencesRepository` (onboarding status) and `AuthRepository` + `UserProfileRepository` (restored guest/local session and profile completion).

### 2.2 Onboarding 1 — Welcome (`57:20531`)
- **Background:** `#FCF9F8`.
- **Top Bar (`64dp`, `#FFFFFF`, `1dp` bottom border `#EDE8E3`):** No back button; trailing `Skip` (`14sp` Medium `#1C1B1B`).
- **Hero Visual Module (`20dp` horizontal margin, `340dp` height, `24dp` radius, `1dp` `rgba(0,0,0,0.06)` border):** `onboarding_hero_1`, bottom `100dp` dark scrim (`0% -> 25%`), top-left `15dp` badge (`rgba(25,25,25,0.75)`, `10dp` radius, `6dp` `#FF623E` dot + `STRENGTH` `10sp` Bold white).
- **Copy (`24dp` below hero):** `Welcome to PULSE` (`25sp` Bold `#191919`, `32sp` line height) + `10dp` gap + `Smart workout tracking built for focused strength training. Log every rep and build consistent gym momentum.` (`15sp` Regular `#66615C`, `22sp` line height).
- **Bottom Controls (`20dp` horizontal margin):** Progress dots (`24 × 7dp` `#FF623E` pill + two `7dp` `#D9D4CF` dots, `8dp` gap) + `28dp` gap + Primary CTA `Next →` (`54dp` height, `14dp` radius, `#FF623E`, `16sp` Bold white).

### 2.3 Onboarding 2 — Track Your Way (`57:20254`)
- **Top Bar (`64dp`):** Back button + trailing `Skip` (`12sp` SemiBold `#1C1B1B`).
- **Hero Visual Module:** `onboarding_hero_2`, badge `SMART LOGGING`.
- **Copy:** `Track Every Workout` + `Log sets, weights, and reps with clean, fluid controls. Track your training volume and personal bests with zero friction.`
- **Progress Dots:** Middle dot active (`24 × 7dp` `#FF623E`).
- **Primary CTA:** `Next →`.

### 2.4 Onboarding 3 — Save & Grow (`57:20391`)
- **Top Bar (`64dp`):** Back button only (no `Skip` button).
- **Hero Visual Module:** `onboarding_hero_3`, badge `DATA & ANALYTICS`.
- **Copy:** `Analyze Your Progress` + `Visualize workout volume, personal records, and strength progress. All data stays fast, private, and always available.`
- **Progress Dots:** Third dot active (`24 × 7dp` `#FF623E`).
- **Primary CTA:** `Get Started →`.

### 2.5 Welcome / Auth Entry (`281:2`)
- **Background:** `#FCFCFC`.
- **Brand Header (`16dp` horizontal margin):** `32dp` `ic_pulse_logo_mark` + `10dp` gap + `PULSE` (`20sp` Bold `#141414`, `-0.5sp` tracking) + `SMART GYM` badge (`#F0F0F0`, `6dp` radius, `10sp` Bold `#4D4D4D`).
- **Hero Visual Card (`16dp` horizontal margin, `320dp` height, `24dp` radius):** `onboarding_hero_1` with bottom dark scrim (`0% -> 88%`).
- **Value Proposition (`16dp` below hero, centered):** `Elevate Every Rep` (`26sp` Bold `#141414`) + `8dp` gap + `Smart workout tracking built for focused strength. Log every set with precision, track progressive overload, and keep all data private.` (`13.5sp` Medium `#737373`, `20sp` line height).
- **Feature Pills Row (`16dp` below text):** `✦ Custom Routines`, `✦ Precision Logging`, `✦ Local Analytics` (`#F2F2F2`, `16dp` radius, `11sp` SemiBold `#404040`, `8dp` gap; wraps cleanly on narrow screens).
- **Action Buttons (`16dp` horizontal margin, `10dp` gap):**
  - `Create Free Account` (`52dp` height, `14dp` radius, `#FF5733`, `16sp` Bold white)
  - `Log In` (`50dp` height, `14dp` radius, `#FFFFFF`, `1.5dp` `#E0E0E0` border, `15sp` SemiBold `#1F1F1F`)
  - `Continue as Guest (Offline Mode) →` (`32dp` height, `13sp` Medium `#737373`)

### 2.6 Log In (`281:25`) & Log In — Validation Errors (`533:328`)
- **Top App Bar (`64dp`):** Back + `Log In` (`18sp` SemiBold `#1C1B1B`).
- **Content (`#FCF9F8`, `16dp` horizontal padding, `20dp` top padding, `20dp` vertical gap):**
  - Heading: `Welcome Back` (`26sp` Bold `#1C1B1B`) + `Sign in to access your saved routines and training history on this device.` (`14sp` Regular `#7A7570`).
  - `EMAIL ADDRESS` (`11sp` SemiBold `#7A7570`) + `48dp` input (`#F6F3F2`, `1dp` `#E5E0DC`, `12dp` radius).
  - `PASSWORD` (`11sp` SemiBold `#7A7570`, `0.6sp` tracking) + `48dp` input with eye visibility toggle.
  - Options Row: `Remember me` (`18dp` `#FF623E` checkbox, `13sp` Medium `#1C1B1B`) + `Forgot password?` (`13sp` SemiBold `#FF623E`).
  - Primary CTA: `Log In` (`52dp`, `14dp` radius, `#FF623E`, `15sp` Bold white).
  - Debug-only helper (`isDebugEntryEnabled` only): `⚠ Test Invalid Login & Error Feedback` (`44dp`, `#FFF0EB`, `1dp` `#FF623E`, `10dp` radius, `12sp` SemiBold `#D9331A`).
  - Footer: `Don't have an account?` (`13sp` Regular `#7A7570`) + `Sign Up` (`13sp` Bold `#FF623E`).
- **Validation Error State (`533:328`):**
  - Alert banner (`#FFF0EB`, `1dp` `#FF4D4D`, `10dp` radius, `14dp × 12dp` padding): bold `#D92626` `13sp` title + regular `#8C3333` `12sp` body.
  - Error fields: `#FFFFFF` bg, `1.5dp` `#FF4D4D` border, `10dp` radius, `11sp` Medium `#D92626` `✕ ...` message below.

### 2.7 Create Account / Sign Up (`281:64`) & Sign Up — Validation Errors (`533:356`)
- **Top App Bar (`64dp`):** Back + `Create Account`.
- **Content (`16dp` horizontal padding, `16dp` top padding, `16dp` vertical gap):**
  - Heading: `Start Your Training` (`26sp` Bold `#1C1B1B`) + `Set up your account to track workouts & volume.` (`14sp` Regular `#7A7570`).
  - Fields (`48dp` height): `FULL NAME`, `EMAIL ADDRESS`, `PASSWORD` (with eye toggle), `CONFIRM PASSWORD` (with eye toggle).
  - Primary CTA: `Create Account` (`52dp`, `14dp` radius, `#FF623E`, `15sp` Bold white).
  - Debug-only helper (`isDebugEntryEnabled` only): `⚠ Test Sign Up Validation Errors (4 Scenarios)` (`44dp`, `#FFF0EB`, `1dp` `#FF623E`, `10dp` radius).
  - Footer: `Already have an account?` + `Log In`.
- **Validation Error State (`533:356`):**
  - Alert banner: `Registration Incomplete (4 Errors)` / `Please correct the highlighted fields below to create your account.`
  - 4 field errors (`10sp` Bold label, `1dp` `#FF4D4D` white box, `10sp` Medium `#D92626` `✕ ...` error text):
    1. `✕ Missing required field: Name cannot be blank.`
    2. `✕ Invalid email format: Missing domain extension.`
    3. `✕ Weak password: Minimum 8 characters required with symbol & number.`
    4. `✕ Confirmation mismatch: Passwords do not match.`

### 2.8 Forgot Password (`281:95`)
- **Top App Bar (`64dp`):** Back + `Reset Password`.
- **Content (`24dp` horizontal padding, `28dp` top padding, `24dp` vertical gap):**
  - Heading: `Reset Password` (`26sp` Bold `#1C1B1B`) + `Set a new password for this device. Remote email recovery is not supported in this offline prototype.` (`14sp` Regular `#7A7570`).
  - Field: `NEW PASSWORD` (`48dp` input with eye toggle).
  - Primary CTA: `Save New Password` (`52dp`, `14dp` radius, `#FF623E`, `15sp` Bold white).
  - Link: `← Back to Log In` (`13sp` SemiBold `#FF623E`).
- **Security Policy:** Unauthenticated password overwrite is blocked; submitting displays honest capability feedback explaining that unauthenticated local account takeover is disabled and remote recovery requires a future backend.

### 2.9 Profile Setup — Personal Info (`282:2`)
- **Top App Bar (`64dp`):** Back + `Profile Setup (1/2)`.
- **Progress Bar:** `6dp` height, `#EBE5E0` track, `3dp` radius, 50% `#FF623E` fill.
- **Heading:** `About You` (`26sp` Bold `#1C1B1B`) + `Set up your profile and training preferences to personalize your experience.` (`14sp` Regular `#7A7570`).
- **YOUR NAME:** `48dp` input (`#F6F3F2`, `1dp` `#E5E0DC`, `12dp` radius).
- **GENDER IDENTITY:** 2 equal cards (`44dp` height, `10dp` radius, `12dp` gap): `Male`, `Female`. Active: `rgba(255,98,62,0.12)` bg, `1.5dp` `#FF623E` border, `#FF623E` SemiBold. Inactive: `#FFFFFF`, `1dp` `#F1EEEA`, `#7A7570` Medium.
- **Stats Row Card (`92dp` height, `#FFFFFF`, `1dp` `#F1EEEA`, `16dp` radius):** 3 equal columns with `1dp × 40dp` `#F1EEEA` vertical dividers: `AGE` (`yrs`), `HEIGHT` (`cm`), `WEIGHT` (`kg`) with `22sp` Bold numeric input + `12sp` Regular `#7A7570` suffix.
- **Unit Toggle (`44dp` height, `#F6F3F2`, `12dp` radius, `4dp` padding):** `Metric (kg, cm)` active `#FFFFFF` pill (`10dp` radius, `12sp` Bold `#1C1B1B`) vs `Imperial (Coming Soon)` disabled (`50%` opacity, `12sp` Medium `#7A7570`).
- **Primary CTA:** `Continue to Goals →` (`52dp`, `14dp` radius, `#FF623E`, `15sp` Bold white).

### 2.10 Profile Setup — Fitness Goals (`282:55`)
- **Top App Bar (`64dp`):** Back + `Training Goals (2/2)`.
- **Progress Bar:** `6dp` height, 100% `#FF623E` fill.
- **Heading:** `Your Fitness Goals` (`26sp` Bold `#1C1B1B`) + `Select your training focus to tailor routines and targets.` (`14sp` Regular `#7A7570`).
- **Goals Group (`10dp` vertical gap):** 3 exclusive cards (`68dp` height, `14dp` radius, `#FFFFFF`, `16dp × 12dp` padding):
  - `Build Muscle & Hypertrophy` (`Focus on volume & muscle growth`, `ic_goal_hypertrophy`)
  - `Increase Strength & Power` (`Heavy compound lifts & progressive PRs`, `ic_goal_strength`)
  - `Fitness & Consistency` (`Maintain health & consistent workout momentum`, `ic_goal_fitness`)
  - Selected: `1.8dp` `#FF623E` border, `38dp` icon container `rgba(255,98,62,0.15)` with `#FF623E` icon tint, `22dp` `#FF623E` circle with white `✓`.
  - Unselected: `1dp` `#F1EEEA` border, `38dp` `#F6F3F2` icon container with `#1C1B1B` icon tint, `22dp` `#E5E0DB` circle.
- **EXPERIENCE LEVEL (`11sp` SemiBold `#7A7570`):** 3 equal chips (`38dp` height, `10dp` radius, `8dp` gap): `Beginner`, `Intermediate`, `Advanced`.
- **WEEKLY WORKOUTS (`11sp` SemiBold `#7A7570`):** 4 equal chips (`38dp` height, `10dp` radius, `8dp` gap): `3 Days`, `4 Days`, `5 Days`, `6 Days`.
- **Primary CTA:** `Complete Setup & Start →` (`52dp`, `14dp` radius, `#FF623E`, `15sp` Bold white).
