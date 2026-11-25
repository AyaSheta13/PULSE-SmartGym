# PULSE Smart Gym — P5 Figma Implementation Specification

**Figma File:** `UvskyKTyxlIOd68wQkp5cQ` (`PULSE---Smart-Gym`)  
**Reference Viewport:** `390 × 884` dp  
**Font Family:** `Plus Jakarta Sans` via `PulseTheme.typography`

---

## 1. Screen & Sheet Inventory

| Screen / Sheet | Figma Node | Android File | Key Geometry & Tokens |
|---|---|---|---|
| Home — First Workout | `316:290` | `HomeScreen.kt` | `64dp` `PulseTopAppBar`, `#FCF9F8` background, `#1C1B1B` `16dp`-radius hero card, `#FF623E` `48dp` CTA, `16dp`-radius Weekly Progress card (`Day X of 7`), `76dp` plan cards (`52dp` thumbnails), `PulseBottomNavigationBar` |
| Home — Populated | `57:19799` | `HomeScreen.kt` | `208dp` hero image (`home_hero_upper_body`) with `0% -> 40% -> 85%` dark gradient, `TODAY'S WORKOUT` pill, `52dp` `#FF623E` CTA, 7-bar daily volume chart (`6dp` bars, `4dp` gap, `#FF623E` today), `LAST SESSION` card with `"Saved on device"` / `"Ended early"` badge |
| Home — No Plans (Derived) | Derived from `316:290` | `HomeScreen.kt` | `#1C1B1B` `"No Workout Plans Yet"` card with `"Explore Workouts"` CTA + `"No workout plans yet"` empty card |
| Profile & Settings | `283:110` | `ProfileScreen.kt` | `64dp` `TitleOnly("Profile & Settings")` bar, `92dp` Avatar card (`54dp` `#FF623E` circle), `72dp` 4-column stats card, `72dp` `#1E293B` `My PULSE Device` card, 4-row settings card, `48dp` `#E53333` `Log Out` button |
| Edit Profile | `283:197` | `EditProfileScreen.kt` | `64dp` `BackWithTitle("Edit Profile")` bar, `72dp` avatar + `Change Avatar`, `48dp` `12dp`-radius fields (`FULL NAME`, read-only `EMAIL ADDRESS`, `WEIGHT`/`HEIGHT`), `42dp` `GENDER IDENTITY` toggles, `FITNESS GOAL` & `EXPERIENCE LEVEL` selectors, `52dp` `Save Changes` + `44dp` `Cancel / Discard Changes` |
| Privacy & Local Data Sheet | `316:515` | `ProfileSheets.kt` | `rgba(0,0,0,0.55)` scrim, `24dp` top corners, `40×4dp` `#D9D4CF` handle, `40dp` `ic_privacy_shield_badge`, `20sp` Bold title, `50dp` `#FF623E` `Got It` button |
| Units & Measurements Sheet | `316:530` | `ProfileSheets.kt` | Active `Metric System (kg, cm)` (`#FFF1EE`, `1.5dp` `#FF623E` border, `✓`), disabled `Imperial System (lbs, in)` (`#F7F6F2`, `"Coming in future release"`), `50dp` `#FF623E` `Confirm` button |
| Account Preferences Sheet | `528:328` | `ProfileSheets.kt` | `20sp` Bold `"Account Preferences"`, honest local/guest offline status copy, `50dp` `#FF623E` `Got It` button |
| Main Bottom Navigation | `324:392`, `324:430`, `324:696` | `BottomNavigationBar.kt` | 5 tabs (`Workouts`, `Progress`, `Home`, `History`, `Profile`), `#EDE8E3` top `1dp` divider, `#FF623E` active vs `#85807A` inactive, `WindowInsets.navigationBars` |
