# PULSE Smart Gym — Design System Migration Map (P1 Status)

This document tracks the migration status of the **PULSE Figma Design System** in `:core:designsystem` (`com.ayasheta.pulsesmartgym.core.designsystem`).

---

## 1. Figma Reference Nodes

| Reference | Node ID | Purpose |
|---|---|---|
| Design System Showcase | `697:367` | Core tokens, typography specimen, spacing/radii, component showcase |
| Master Components Library | `710:367` | Reconciled production component definitions |
| Top App Bar Component Set | `470:57` | 9-variant Top App Bar component set + Back (`468:3`) & Dismiss (`468:6`) |
| Rest Timer Component | `740:593` | `PULSE / Workout / In-Page Rest Timer Card` |
| Bottom Sheet Specimen | `704:398` | `PulseBottomSheet` layout and destructive/resume actions |

---

## 2. Component Migration Matrix

| Figma Component | Kotlin Component | P1 Status | Notes |
|---|---|---|---|
| **PULSE / Button / Primary (Default & Compact)** (`710:370`, `710:372`) | `Button`, `CompactButton`, `IconButton` | **Migrated** | 52dp/16dp (Bold 15sp) & 48dp/12dp (SemiBold 14sp), `#FF623E` coral, `#B22C0B` pressed state, centered content. |
| **PULSE / Button / Secondary & Destructive** (`710:374`, `704:405`) | `OutlineButton`, `OutlineIconButton`, `NegativeButton`, `NegativeIconButton`, `TextButton`, `TextIconButton` | **Migrated** | Aligned heights, borders (`#E5E0DC`), pressed states, and filled/outline destructive variants. |
| **PULSE / Navigation / Top App Bar** (`470:57`, 9 variants) | `PulseTopAppBar` + `PulseTopAppBarStyle` | **Migrated** | All 9 Figma variants, 64dp height, 44dp icon targets, independent leading/trailing callbacks (fixed old `TopBar.kt` bug). |
| **PULSE / Navigation / Bottom Bar** (`721:463`, 5 tabs) | `PulseBottomNavigationBar` + `PulseTab` | **Migrated** | Stateless (`selectedTab` hoisted), 5 PULSE destinations (Workouts, Progress, Home, History, Profile), `WindowInsets.navigationBars` support. |
| **PULSE / Form / Text Field (Default & Password)** (`710:398`, `710:402`) | `TextField` | **Migrated** | 48dp height, 12dp radius, `#F6F3F2` fill, `#E5E0DC`/`#FF623E`/`#DC2626` borders, error message slot, fixed `trailingIconColor` typo. |
| **PULSE / Form / Search Bar** (`721:428`) | `PulseSearchBar` | **Newly implemented** | 48dp height, 12dp radius, `#F6F3F2` fill, leading `Search` vector, clear button. |
| **PULSE / Status / Bluetooth Chip** (`710:376`, `710:380`, `700:399`–`409`) | `PulseBluetoothChip` + `PulseBluetoothChipStatus` | **Newly implemented** | 24dp pill supporting Disconnected, Connecting, Connected, Connection Lost, Failed. |
| **Filter / Mode Chips** | `Chip` | **Migrated** | Updated to `#FF623E` selected, `#E5E0DC` border, `PulseTheme` tokens. |
| **PULSE / Workout / Set Row** (`710:384`, `710:391`) | `PulseExerciseSetRow` + `SetRowStatus` | **Newly implemented** | Unstarted (49dp, `#F9FAFB`, Start pill), In-Progress (`#FFF1EE`), Completed (46dp, `#FFF1EC`, `#10B870` check). |
| **PULSE / Workout / In-Page Rest Timer Card** (`740:593`) | `PulseRestTimerCard` | **Newly implemented** | Stateless countdown card with `REST` pill, `+30s` / `Skip Rest` callbacks, and coral `PulseProgressBar`. |
| **PULSE / Workout / Weight Selector Card** (`721:535`) | `PulseWeightSelectorCard` | **Newly implemented** | Load display, target pill, `-` / `+` vector stepper buttons. |
| **PULSE / Workout / Rep Counter Gauge** (`721:518`) | `PulseRepCounterGauge` | **Newly implemented** | 224dp Canvas coral arc gauge, `REPS DONE`, `GOAL` pill, `- 1` / `+ 1` adjust buttons. |
| **PULSE / Workout / Exercise Selection Row** (`721:484`) | `PulseExerciseSelectionRow` | **Newly implemented** | Exercise thumbnail/placeholder, muscle & equipment pills, 36×44dp toggle button. |
| **Exercise Progress Card** (`702:429`) | `PulseExerciseProgressCard` | **Newly implemented** | Exercise header, `X/Y` badge, coral `PulseProgressBar`, set row slot. |
| **PULSE / Card / Workout Plan Card** (`721:559`) | `PulseWorkoutPlanCard` | **Newly implemented** | 208dp hero header with gradient & badge pill, metadata, 52dp `Start Workout` CTA. |
| **PULSE / Feedback / Empty State Card** (`721:573`) | `PulseEmptyStateCard` | **Newly implemented** | 68dp `#FFF1EE` icon circle, title, description, full-width `CompactButton`. |
| **Progress & Loading Indicators** | `PulseProgressBar`, `PulseLoadingIndicator` | **Newly implemented** | Coral progress track (remains coral at 100%) and Canvas spinning arc loader. |
| **Bottom Sheet & Dialog** (`704:398`) | `PulseBottomSheet`, `PulseConfirmationDialog` | **Newly implemented** | 24dp top-corner sheet with 50% scrim, drag handle, slide-up motion, summary slot, and confirmation dialog. |
| **Generic Containers & Controls** | `Card`, `CardItem`, `ScreenSkeleton`, `SectionHeader`, `ToastMessage`, `CheckBox`, `RadioButton`, `ToggleButton`, `Text` helpers | **Migrated** | Updated to `PulseTheme`, `Plus Jakarta Sans`, PULSE colors/corners/spacing. |
| **Legacy TCOS Assets & Models** | `BottomNavigationBarTab`, `rabbit.jpeg`, `tc_logo.png`, `ic_cart.xml`, `ic_delete.xml`, `ic_user.xml`, `rubik_*.ttf` | **Removed with justification** | Replaced by `PulseTab`, `Plus Jakarta Sans`, and PULSE vector icons (`PulseVectors.kt`). |
