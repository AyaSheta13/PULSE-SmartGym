# PULSE Smart Gym — P0 Foundation & Architecture Audit

**Phase:** P0 (Project Foundation & Architecture Audit)  
**Date:** 2026-11-25  
**Status:** Complete — Ready for P1

---

## 1. Initial Repository Structure

Before P0 changes, the repository contained:

- `settings.gradle.kts` — Included only `:app` (`rootProject.name = "PULSE"`).
- `build.gradle.kts` — Root plugin declarations (`android-application`, `kotlin-compose`).
- `gradle/libs.versions.toml` — Version Catalog (AGP `9.1.1`, Kotlin `2.2.10`, Compose BOM `2024.09.00`, `core-ktx:1.19.0`, `lifecycle-runtime-ktx:2.11.0`, `activity-compose:1.13.0`, Material 3, JUnit/Espresso).
- `gradle/wrapper/gradle-wrapper.properties` — Gradle `9.3.1`.
- `app/` — Template Android application (`applicationId = "com.example.pulse"`, `namespace = "com.example.pulse"`, `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36.1`), `MainActivity.kt`, template `ui/theme/*`, `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`.
- `designSystem/` — Copied Android Library module (not registered in `settings.gradle.kts`).

---

## 2. Imported Design System Analysis

- **Original Location:** `PULSE/designSystem/`
- **New Location:** `PULSE/core/designsystem/` (Gradle path `:core:designsystem`)
- **Module Type:** Complete Gradle Android Library module (`namespace = "net.thechance.tcos.designsystem"`, `minSdk = 26`, `compileSdk = 36`).
- **Existing Contents (Preserved in Full):**
  - **Base Tokens (`baseToken/`):** `ColorPalette.kt`, `TonalPalette.kt`, and 18 tonal color files (`apricotColor.kt` through `skyColor.kt`).
  - **Semantic Tokens (`semanticToken/`):** `ColorScheme.kt`, `defaultColors.kt` (light & dark), `Typography.kt`, `defaultTypography.kt` (Rubik font), `Corners.kt`, `defaultCorners.kt`, `Size.kt`, `defaultSizing.kt`, `Space.kt`, `defaultSpacing.kt`.
  - **Theme (`theme/`):** `TCOSTheme.kt`, `Theme.kt` (`Theme.color`, `Theme.typography`, `Theme.corners`, `Theme.space`, `Theme.size`), and `CompositionLocal` providers (`colorProvider.kt`, `cornerProvider.kt`, `sizeProvider.kt`, `spaceProvider.kt`, `typographyProvider.kt`).
  - **Components (`component/`):**
    - `ScreenSkeleton.kt`
    - `button/`: `baseButton.kt`, `PrimaryButton.kt` (`Button`, `IconButton`), `SecondaryButton.kt` (`OutlineButton`, `OutlineIconButton`), `NegativeButton.kt` (`NegativeButton`, `NegativeIconButton`), `TextButton.kt` (`TextButton`, `TextIconButton`)
    - `card/`: `Card.kt`, `CardItem.kt`
    - `chips/`: `Chip.kt`
    - `generic/`: `TopBar.kt`, `BottomNavigationBar.kt`, `BottomNavigationBarTab.kt`, `SectionHeader.kt`, `ToastMessage.kt`
    - `selectable/`: `CheckBox.kt`, `CheckBoxColors.kt`, `CheckBoxStatus.kt`, `RadioButton.kt`, `ToggleButton.kt`
    - `text/`: `InputField.kt` (`TextField`), `Text.kt` (`Text`, `SectionTitle`, `CardTitleText`, `SubTitleText`, `TextContentPrimary`, `TextContentSecondary`, `TextSupportContent`, `Caption`, `Label`, `Hint`)
  - **Vectors (`vectors/`):** `Back`, `Chat`/`ChatFilled`, `CheckMark`, `Close`, `ErrorCircle`, `Favourite`/`FavouriteFilled`, `Home`/`HomeFilled`, `Indeterminate`, `Notification`, `Profile`/`ProfileFilled`, `SuccessCircle`.
  - **Resources (`res/`):** `ic_cart.xml`, `ic_delete.xml`, `ic_user.xml`, `rabbit.jpeg`, `tc_logo.png`, `rubik_medium.ttf`, `rubik_regular.ttf`.
- **Module Isolation:** Verified `:core:designsystem` has **zero** dependencies on `:app`, Room, Navigation, Auth, or BLE.

---

## 3. Final Module Organization

```text
PULSE/
├── app/                          # :app (Application entry point, future features & Navigation 3)
├── core/
│   └── designsystem/             # :core:designsystem (Reusable Compose UI, tokens, theme, icons)
├── docs/
│   ├── P0_FOUNDATION_AUDIT.md
│   └── DESIGN_SYSTEM_MIGRATION_MAP.md
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

Future modules (`:core:model`, `:core:data`, `:core:device`) are intentionally omitted in P0 and will be created in their respective implementation phases.

---

## 4. Gradle, Dependency & Configuration Decisions

| Item | Initial | Final (P0) | Justification |
|---|---|---|---|
| **AGP / Gradle / Kotlin** | `9.1.1` / `9.3.1` / `2.2.10` | Unchanged | Compatible and already configured. |
| **JDK** | `JavaVersion.VERSION_11` target | Unchanged (built with Android Studio JBR 21.0.9) | Local `JAVA_HOME` env pointed to non-existent `jdk-25.0.0.36-hotspot`; Android Studio JBR 21 works cleanly. |
| **`compileSdk`** | `36.1` (`app`), `36` (`designSystem`) | `37` (both `:app` and `:core:designsystem`) | Baseline build failed `:app:checkDebugAarMetadata` because `androidx.core:core-ktx:1.19.0` and `androidx.lifecycle:lifecycle-runtime-compose-android:2.11.0` require `compileSdk >= 37`. `android-37.0` SDK is installed locally. |
| **`minSdk` / `targetSdk`** | `24` / `36` (`app`), `26` (`designSystem`) | `24` / `36` (`app`), `24` (`:core:designsystem`) | Lowered `:core:designsystem` `minSdk` from 26 to 24 so `:app` retains `minSdk = 24` without manifest merger conflicts. |
| **Compose BOM** | `2024.09.00` | `2025.09.00` | `2024.09.00` lacked `Modifier.dropShadow`, `Shadow`, and `TextAutoSize` (Compose 1.9+), causing compilation errors in `BottomNavigationBar.kt`, `ToastMessage.kt`, and `Text.kt`. |
| **Kotlin Android Plugin** | `kotlin.android` in `designSystem` | Removed in `:core:designsystem` | AGP `9.1.1` includes built-in Kotlin support (matching `app/build.gradle.kts`). Also removed deprecated `kotlinOptions { jvmTarget = "11" }`. |
| **Version Catalog** | Missing `android-library` & `compose-foundation` | Added `android-library` plugin and `androidx-compose-foundation` | Centralized all `:core:designsystem` dependencies in `gradle/libs.versions.toml`. |
| **Module Dependency** | None | `implementation(project(":core:designsystem"))` in `app/build.gradle.kts` | Connects `:app` to `:core:designsystem` with no circular dependencies. |

---

## 5. Architectural Conventions

### Feature-First Package Structure (for P2+)

```text
app/src/main/java/com/example/pulse/
├── MainActivity.kt
├── PulseApplication.kt          # Only if needed
├── navigation/                  # P2 (AndroidX Navigation 3)
└── feature/                     # Future phases
    ├── onboarding/
    ├── auth/
    ├── home/
    ├── workouts/
    ├── session/
    ├── counter/
    ├── history/
    ├── progress/
    ├── device/
    └── profile/
```

Each feature follows:

```text
feature/<name>/presentation/
├── screen/       # *Screen.kt composables (stateless UI)
├── component/    # Feature-specific UI components
├── state/        # *UiState.kt (immutable) & *UiEvent.kt / intents
└── viewmodel/    # *ViewModel.kt exposing StateFlow<*UiState>
```

### Naming & State Rules
- **Naming:** PascalCase for classes, camelCase for functions/properties; `*Screen`, `*ViewModel`, `*UiState`, `*UiEvent`, `*Repository`. Avoid vague names (`CommonUtils`, `Helpers`, `BaseViewModel`, `GenericComponent`).
- **State:** `StateFlow` + immutable `UiState` data classes; unidirectional data flow.

### Future Domain & Boundary Contracts (Documented Only)
- **Workout Domain:** Workout plans, exercises, planned sets, active sessions, completed set records, actual reps, actual weight (recorded during/after execution, never pre-planned), session duration, history, and progress analytics.
- **Authentication:** `AuthRepository` interface with `FakeAuthRepository` initially, swappable for a real API repository without UI changes.
- **BLE Hardware Boundary:** Clean separation between UI ↔ Device State ↔ BLE Transport ↔ Sensor Measurements ↔ Workout Calculations. Supports simulated device data source and real ESP32 BLE GATT implementation behind shared interfaces. **No firmware message formats or GATT UUIDs are coupled to UI.**
- **Navigation:** Mandatory **AndroidX Navigation 3** (to be implemented in P2).

---

## 6. Build Verification Results

### Baseline Build (Before Changes)
- Command: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon` (with Android Studio JBR 21)
- Result: **FAILED** at `:app:checkDebugAarMetadata` (`compileSdk` 36.1 incompatible with `core-ktx:1.19.0` and `lifecycle-runtime-compose-android:2.11.0`, which require `compileSdk >= 37`).

### Post-Integration Verification (Final State)
- Command: `.\gradlew.bat :core:designsystem:assembleDebug :app:assembleDebug :app:testDebugUnitTest --no-daemon` (with Android Studio JBR 21)
- Result: **BUILD SUCCESSFUL** (71 actionable tasks: 48 executed, 23 up-to-date).
  - `:core:designsystem:assembleDebug` — **PASSED**
  - `:app:assembleDebug` — **PASSED**
  - `:app:testDebugUnitTest` — **PASSED** (`ExampleUnitTest`)

---

## 7. Issues, Risks & Remaining Tasks

- **Local `JAVA_HOME` Environment:** The machine's default `JAVA_HOME` points to `C:\Program Files\Eclipse Adoptium\jdk-25.0.0.36-hotspot` (missing). CLI builds require setting `JAVA_HOME` to `C:\Program Files\Android\Android Studio\jbr` (or configuring Android Studio Gradle JDK).
- **Design System Naming:** `:core:designsystem` currently retains `net.thechance.tcos.designsystem` and `TCOSTheme` to avoid premature rewrites in P0. Renaming to `PulseTheme` / PULSE package is planned for P1.
- **Pre-existing Component Bugs:** Minor bugs in `TopBar.kt` (leading button enabled check), `BottomNavigationBar.kt` (internal tab state), and `InputField.kt` (`tailingIconColor` typo) are cataloged in `docs/DESIGN_SYSTEM_MIGRATION_MAP.md` for P1.
- **Remaining Phases:**
  - **P1:** Design System Migration (Plus Jakarta Sans, `#FF623E` coral palette, Figma component alignment).
  - **P2:** Navigation 3 setup & core screens.
  - **P3+:** Room database (`:core:model`, `:core:data`), DataStore, `FakeAuthRepository`, and BLE abstraction (`:core:device`).
