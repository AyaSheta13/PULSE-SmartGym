# PULSE Smart Gym — Design System Migration Map (P1 Guide)

This document maps the **PULSE Figma Design System** against the imported Kotlin Design System (`:core:designsystem`, currently under package `net.thechance.tcos.designsystem`). It serves as the blueprint for **Phase P1 — Design System Migration**.

> **Scope Note (P0):** No visual redesign or component rewrites were performed in P0. All existing components and resources in `:core:designsystem` were preserved as-is.

---

## 1. Figma Reference Nodes

| Reference | Node ID | Purpose |
|---|---|---|
| Design System Showcase | `697:367` | Core tokens, typography specimen, spacing/radii, component showcase, architecture rules |
| Master Components Library | `710:367` | Reconciled production component definitions |
| Shared Top App Bar | `470:57` | 9-variant Top App Bar component set + Back (`468:3`) & Dismiss (`468:6`) buttons |
| Shared Rest Timer | `740:593` | `PULSE / Workout / In-Page Rest Timer Card` |

---

## 2. Component Migration Matrix

| Figma Component | Existing Kotlin Component | Reusable? | Required P1 Changes |
|---|---|---|---|
| **PULSE / Button / Primary (Default)** (`710:370`, 52dp, Bold 15sp) & **Compact** (`710:372`, 48dp, SemiBold 14sp) | `Button` & `IconButton` (`component/button/PrimaryButton.kt`, `baseButton.kt`) | **Adapt** | Update default height to 52dp (add compact 48dp variant), corner radius to `12.dp`, typography to Bold 15sp / SemiBold 14sp, brand color `#FF623E`, and add pressed state styling. |
| **PULSE / Button / Secondary (Default)** (`710:374`, 52dp) & Destructive Outline | `OutlineButton`, `OutlineIconButton`, `NegativeButton`, `TextButton` (`component/button/*.kt`) | **Adapt** | Align heights (52dp/48dp), `12.dp` corner radius, `#E5E0DC` border token, and `#DC2626` destructive outline variant. |
| **PULSE / Navigation / Top App Bar** (`470:57`, 9 variants, 64dp) + Back (`468:3`) / Dismiss (`468:6`) | `TopBar` (`component/generic/TopBar.kt`) | **Adapt** | Support all 9 Figma variants (Back+Title, Back+Title+Trailing, Title+Trailing, Root Nav Header, Home Greeting, Live Workout Header with BLE chip, Workout Summary, Back+Title+Delete, Title Only), 64dp height, 44dp circular/rounded icon buttons, and fix existing `TopBar.kt` line 58 bug (`enabled = onClickTrailingButton != null` used for leading button). |
| **PULSE / Navigation / Bottom Bar** (`721:463`, 84dp, 5 tabs) | `BottomNavigationBar` & `BottomNavigationBarTab` (`component/generic/BottomNavigationBar.kt`) | **Adapt** | Hoist `selectedTabIndex` to caller state (currently internal `remember` state), support 5 PULSE destinations (Home, Workouts, Progress, History, Profile), 84dp height (64dp bar + navigation bar inset), and coral active indicator. |
| **PULSE / Form / Text Field (Default & Password)** (`710:398`, `710:402`, 48dp, R12) | `TextField` (`component/text/InputField.kt`) | **Adapt** | Align 48dp input height, `12.dp` corner radius, `#F6F3F2` / `#FFFFFF` surface + `#E5E0DC` border, add password visibility toggle, error state support, and fix `tailingIconColor` typo. |
| **PULSE / Form / Search Bar** (`721:428`, 48dp, R12, `#F6F3F2`) | *None* | **Missing** | Create `SearchBar` (or `PulseSearchBar`) with 48dp height, `12.dp` corner radius, `#F6F3F2` surface fill, leading search vector icon, and clear action. |
| **PULSE / Status / Bluetooth Chip** (Disconnected `710:376`, Connected `710:380`, Connecting, Connection Lost, Failed — 24dp/32dp pills) | `Chip` (`component/chips/Chip.kt`) | **Adapt / Extend** | Keep generic `Chip` for filter/category pills (Chest, Back, Legs, Core, Auto/Manual mode); add `BluetoothStatusChip` (or status variants) supporting Disconnected, Connected, Connecting, Connection Lost, and Failed states with status dot & telemetry colors. |
| **PULSE / Workout / Set Row** (Unstarted `710:384` 49dp, Completed `710:391` 46dp/44dp, R10) | *None* | **Missing** | Create `ExerciseSetRow` with Unstarted, Active/In-Progress, and Completed states (`#FFF1EC` light-orange tint or `#FFFFFF` + `#E5E0DC` border per state, `#10B870` / `#059669` green check indicator, reps & weight badge). |
| **PULSE / Workout / In-Page Rest Timer Card** (`740:593`, 91dp) | *None* | **Missing** | Create `RestTimerCard` with `REST` pill badge, exercise subtitle, `MM:SS` countdown display, `+30s` & `Skip Rest` actions, and horizontal progress track. |
| **PULSE / Workout / Exercise Selection Row** (`721:484`, 82dp) | *None* | **Missing** | Create `ExerciseSelectionRow` for exercise picker/selection lists. |
| **PULSE / Workout / Rep Counter Gauge** (`721:518`, 340dp) | *None* | **Missing** | Create `RepCounterGauge` circular arc gauge displaying current reps, target reps, and hardware/manual fallback state. |
| **PULSE / Workout / Weight Selector Card** (`721:535`, 106dp) | *None* | **Missing** | Create `WeightSelectorCard` numeric stepper (`-` / `+` 2.5 kg or 5 lbs increments) with load display. |
| **Exercise Progress Card** (`702:429` showcase) | *None* | **Missing** | Create `ExerciseProgressCard` composing exercise header, set progress bar, `X/Y` pill, and `ExerciseSetRow` items. |
| **PULSE / Card / Workout Plan Card** (`721:559`, 292dp) | `Card` & `CardItem` (`component/card/Card.kt`, `CardItem.kt`) | **Adapt / New** | Preserve `Card` container primitive (update default corner radius/tokens); create `WorkoutPlanCard` for PULSE workout plan summaries (preserve `CardItem` if needed or deprecate after migration). |
| **PULSE / Feedback / Empty State Card** (`721:573`, 420dp) | *None* | **Missing** | Create `EmptyStateCard` with illustration/icon slot, title, description, and primary CTA. |
| **PulseBottomSheet / PulseDialog** (`704:398`) | *None* | **Missing** | Create `PulseBottomSheet` (drag handle, `#000000` 50% scrim, title, summary card, primary + secondary actions) and/or `PulseDialog`. |
| **Section Header** | `SectionHeader` (`component/generic/SectionHeader.kt`) | **Reuse / Adapt** | Reuse directly; update typography/colors to PULSE tokens. |
| **Toast / Feedback Banner** | `ToastMessage` (`component/generic/ToastMessage.kt`) | **Reuse / Adapt** | Reuse; align colors (`#059669` / `#10B870` success, `#DC2626` error) and corner radii. |
| **Screen Container** | `ScreenSkeleton` (`component/ScreenSkeleton.kt`) | **Reuse / Adapt** | Reuse; update background color token and system bar handling as needed. |
| **Selection Controls** (CheckBox, RadioButton, ToggleButton) | `CheckBox`, `RadioButton`, `ToggleButton` (`component/selectable/*.kt`) | **Reuse** | Directly reusable; will inherit updated `Theme.color` coral primary (`#FF623E`) automatically. |
| **Text Primitives** | `Text`, `SectionTitle`, `CardTitleText`, `SubTitleText`, etc. (`component/text/Text.kt`) | **Adapt** | Map to new PULSE typography scale (Display/Hero, Screen Title, Section Header, Card Title/CTA, Body, Supporting/Metadata, Label/Overline, Numeric/Metric). |

---

## 3. Design Tokens Migration

### 3.1 Color Tokens

| Semantic Role | Existing (`defaultLightColors`) | Target PULSE Figma Token |
|---|---|---|
| Primary / Brand | `ColorPalette.sky.t800` (Blue) | `#FF623E` (Coral) |
| Primary Dark / Pressed | — | `#B22C0B` |
| Primary Container / Variant | `ColorPalette.sky.t50` | `#FFF1EE` / `#FFF1EC` (completed set row tint) |
| Background / Surface Variant | `ColorPalette.gray.t100` | `#F6F3F2` (Warm light surface) |
| Container / Card Surface | `ColorPalette.white` | `#FFFFFF` |
| Border / Outline | `ColorPalette.gray.t300` (`shadeQuaternary`) | `#E5E0DC` |
| Text / On-Surface (`shadePrimary`) | `ColorPalette.gray.t800` | `#1C1B1B` |
| Text Muted (`shadeSecondary`) | `ColorPalette.gray.t600` | `#7A7570` |
| Success / Completed | `ColorPalette.lime.t600` | `#10B870` (indicator) / `#059669` (swatch) |
| Warning / Caution | `ColorPalette.honey.t500` | `#D97706` |
| Error / Alert | `ColorPalette.cherry.t600` | `#DC2626` |
| Device / Telemetry (BLE) | — | `#0284C7` |

### 3.2 Spacing & Radii Tokens

| Token Type | Existing (`defaultSpacing` / `defaultCorners`) | Target PULSE Figma Scale |
|---|---|---|
| **Spacing** | `none=0, tiny=2, small=4, medium=8, large=12, huge=16, giant=24, massive=32` | `xxs=4dp, xs=8dp, sm=12dp, md=16dp, lg=20dp, xl=24dp, xxl=32dp` (add `20.dp` step) |
| **Corners / Radii** | `xSmall=2, small=4, medium=8, large=12, xLarge=16, xxLarge=24, full=100%` | `Small/Badge=6dp, Element=10dp, Input/CTA=12dp, Card=16dp, Sheet=24dp, Pill=fullRounded` |

---

## 4. Typography Migration

| Role | Existing (`Rubik`) | Target PULSE (`Plus Jakarta Sans`) |
|---|---|---|
| Font Family | `rubik_regular.ttf`, `rubik_medium.ttf` | **Plus Jakarta Sans** (Regular 400, Medium 500, SemiBold 600, Bold 700, ExtraBold 800) |
| Display / Hero | *Missing* | ExtraBold 32sp / 40sp lineHeight |
| Numeric / Metric Display | *Missing* | Bold 26sp / 32sp lineHeight |
| Screen Title | `sectionTitle` (Medium 20sp / 24sp) | Bold 20sp / 28sp lineHeight |
| Section Header | `cardTitle` (Medium 18sp / 22sp) | Bold 16sp / 22sp lineHeight |
| Card Title / CTA | `subtitle` (Medium 16sp / 20sp) | Bold 15sp / 20sp lineHeight (Compact CTA: SemiBold 14sp / 18sp) |
| Body / Form Text | `supportingContent` (Regular 14sp / 18sp) | Regular 14sp / 20sp lineHeight |
| Supporting / Metadata | `hint` (Medium 12sp / 16sp) | Medium 12sp / 16sp lineHeight |
| Label / Overline | *Missing* | SemiBold 11sp uppercase |

---

## 5. Missing Assets & Icons

### Font Assets Needed (`res/font/`)
- `plus_jakarta_sans_regular.ttf` (400)
- `plus_jakarta_sans_medium.ttf` (500)
- `plus_jakarta_sans_semibold.ttf` (600)
- `plus_jakarta_sans_bold.ttf` (700)
- `plus_jakarta_sans_extrabold.ttf` (800)

### Vector Icons Needed (`vectors/` or `res/drawable/`)
Existing vectors (`Back`, `Chat`, `CheckMark`, `Close`, `ErrorCircle`, `Favourite`, `Home`, `Indeterminate`, `Notification`, `Profile`, `SuccessCircle`) cover basic navigation and status. P1 will require PULSE-specific vector icons:
- **Navigation tabs:** Workouts (dumbbell), Progress (chart/trend), History (clock/calendar), Home, Profile.
- **Actions & UI:** Search, Delete/Trash (replace `ic_delete.xml` or migrate to `ImageVector`), Password Eye / Eye-Off, Plus (`+`), Minus (`-`), Play / Start, Skip, Settings/Gear.
- **Hardware / Telemetry:** Bluetooth, Battery/Signal, Sensor/Pulse logo.
- **Cleanup in P1:** Remove sample/legacy assets (`rabbit.jpeg`, `tc_logo.png`, `ic_cart.xml`) once no longer referenced by previews.

---

## 6. Component Dependencies & Cleanup Notes

- **Module Isolation:** `:core:designsystem` currently depends **only** on `compose-bom`, `compose.ui`, `compose.ui.graphics`, `compose.ui.tooling.preview`, `compose.foundation` (plus debug `ui.tooling` & `ui.test.manifest`). Keep it free of Material 3, Room, Navigation, Auth, or BLE dependencies unless `ModalBottomSheet` requires Material 3 or is built on `androidx.compose.ui.window` / Material 3 explicitly in P1.
- **Package / Theme Naming (P1 Decision):** Currently `net.thechance.tcos.designsystem` and `TCOSTheme` / `Theme`. In P1, consider renaming to `com.example.pulse.core.designsystem` (or `com.pulse.designsystem`) and `PulseTheme` while migrating tokens.
- **Pre-existing Code Issues to Fix in P1:**
  - `TopBar.kt`: Line 58 passes `enabled = onClickTrailingButton != null` to the leading button instead of `onClickLeadingButton != null`.
  - `BottomNavigationBar.kt`: Holds `selectedTabIndex` internally instead of accepting `selectedTab` / `selectedTabIndex` as a stateless parameter.
  - `InputField.kt`: Parameter typo `tailingIconColor` -> `trailingIconColor`.
  - `NegativeButton.kt`: Unused imports (`ColorPalette`, `defaultDarkColors`).

---

## 7. P1 Migration Priorities

1. **Priority 1 (Foundations):** Add Plus Jakarta Sans font files; update `ColorPalette`, `ColorScheme`, `defaultLightColors`, `Typography`, `Space`, `Corners`, `Size`, and `PulseTheme`.
2. **Priority 2 (Core Primitives):** Update `Button` / `OutlineButton` / `NegativeButton` / `TextButton`, `TextField`, `SearchBar`, `TopBar` (9 variants), `BottomNavigationBar` (5 tabs, stateless), and `Chip` + `BluetoothStatusChip`.
3. **Priority 3 (Workout & Feedback Components):** Implement `ExerciseSetRow`, `RestTimerCard`, `WeightSelectorCard`, `RepCounterGauge`, `ExerciseProgressCard`, `ExerciseSelectionRow`, `WorkoutPlanCard`, `EmptyStateCard`, and `PulseBottomSheet`.
4. **Priority 4 (Cleanup):** Remove unused legacy TCOS assets (`rabbit.jpeg`, `tc_logo.png`, `ic_cart.xml`, `rubik_*.ttf`) and add `@Preview` coverage for all new/updated PULSE components.
