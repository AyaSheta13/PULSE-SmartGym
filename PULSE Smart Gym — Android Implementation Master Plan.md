# PULSE Smart Gym — Android Implementation Master Plan

**Project:** [https://github.com/AyaSheta13/PULSE-SmartGym](https://github.com/AyaSheta13/PULSE-SmartGym)  
**Figma:** [https://www.figma.com/design/UvskyKTyxlIOd68wQkp5cQ/PULSE---Smart-Gym?node-id=0-1](https://www.figma.com/design/UvskyKTyxlIOd68wQkp5cQ/PULSE---Smart-Gym?node-id=0-1)  
**Execution model:** Run only ONE phase per Antigravity prompt. User reviews the build, screenshots, tests and code changes before authorizing the next phase.

## Non-negotiable decisions

- Kotlin + Jetpack Compose + MVVM + Repository Pattern + Kotlin Coroutines/Flow.
- **Navigation 3**, not Navigation Compose 2.
- Functional offline application using **Room from the beginning**; DataStore for lightweight preferences.
- **FakeAuthRepository** behind a replaceable authentication contract; no pretending authentication uses a real server.
- No hardware/firmware yet: `DeviceDataSource` / `DeviceRepository` contract with a **clearly labeled simulator** and later BLE implementation. No fabricated sensor measurements.
- Import the user's existing **design-system Gradle module** from ZIP/folder, inspect compatibility and adjust it to match actual Figma components; never reinvent the library unnecessarily.
- Figma is the visual source of truth; implement the logical behavior rather than copying its static demo transitions.
- Every phase: compile, verify on emulator/device, test critical logic, provide screenshot evidence and changed files. No silent phase creep.
- Do not assume finalized ESP32/MPU6050/ToF device characteristics or GATT protocol; future BLE capability depends on firmware validation.

## Proposed Gradle modules

```
PULSE/
├── app/                       # Navigation3, app shell, feature-first UI/ViewModels
├── core/
│   ├── designsystem/          # IMPORTED module adapted to approved Figma system
│   ├── model/                 # Pure Kotlin domain models + repository contracts
│   ├── data/                  # Room, entities, DAOs, local repos, FakeAuthRepository
│   └── device/                # Device contracts + fake simulator + later Android BLE adapter
└── gradle/                    # Version catalog, build configuration
```

`app/src/main/java/<package>/feature/` should contain clearly grouped `onboarding`, `auth`, `home`, `workouts`, `session`, `counter`, `history`, `progress`, `device`, and `profile` features. Each feature owns its `presentation/` screen, UI state, ViewModel, and local components, while business models and persistence contracts live in `core`. Introduce no extra modules unless actual coupling/testability justifies them. The exact legacy design-system module path is confirmed in Phase 0.

## Phased implementation

### P0 — Repository & imported module assessment (foundation gate)

- Inspect current project, GitHub branch, Gradle/AGP/Kotlin/Compose versions, namespace/application ID, manifests and build status.
- User places the ZIP/folder in the working project; inspect source, dependencies, assets, fonts and APIs before changing it.
- Review current Figma master component inventory and screen-to-component mapping; capture stable node references.
- Decide final Gradle module layout, source-set boundaries and dependency graph; create baseline docs, `.gitignore` checks, and branch strategy.
- Verify compatibility of old design-system Gradle module with project, then build the unmodified or minimally adapted starter first.
- **Done:** reproducible Debug build, baseline audit, dependency matrix, checked-in plan; NO screens implemented.

### P1 — Design System migration & Compose primitives

- Integrate existing design-system module; remove unrelated old-brand styles safely, map Figma tokens to Compose theme.
- Typography, color, spacing, radii, icons, imagery strategy, dark-mode policy (match design), edge-to-edge/insets.
- Reuse master patterns: Top App Bar, Bottom Bar, buttons, inputs, chips, cards, progress bars, set rows, sheet shell, states, visual feedback.
- Component states/parameters and Compose `@Preview` gallery; screenshot comparisons with actual Figma screen elements.
- **Done:** buildable module, tokens documented, reusable components with previews, no emoji UI icons, no fabricated components.

### P2 — App shell & Navigation 3 contracts

- Set up Navigation3 with typed/serializable destination keys and saved back stack, correct tab/back behavior and transitions matching Figma.
- App-wide scaffold, root auth/onboarding/main flow, fixed navigation and bottom-sheet presentation contracts.
- Define DI composition root and lifecycle/viewmodel ownership. Introduce **interfaces only** for auth, session and device contracts where appropriate.
- Screen placeholders only; wire all destinations, not full screen implementation.
- **Done:** navigation tests for back stack, tabs, process recreation/entry conditions and no incorrect route loops.

### P3 — Domain + Room + fake data infrastructure

- Models: WorkoutPlan, PlannedExercise, Session, CompletedSet, RestState, local UserProfile, device capability/status.
- Room entities, DAOs, type converters, transactional set completion, reactive queries, schema export and migration policy.
- Unique completed-set identity `(sessionId, planExerciseId, setIndex)`; no unqualified `REPLACE` for duplicate prevention. Use explicit idempotency/transactional behavior.
- Repository interfaces and implementations; deterministic seed data with the actual exercise catalog; distinguish planned rep targets from _per-set actual load_.
- Fake auth implementation with clearly labeled local/demo identity and DataStore preferences; no plaintext real credentials.
- Device source abstraction and FakeDeviceDataSource contract (do not implement GATT yet).
- **Done:** Room unit/instrumented DAO and migration tests; seed data; realistic offline persistence and restoration tests.

### P4 — Splash, Onboarding, Authentication, Profile Setup (Figma B/C)

- Implement all specified Figma screens and state variants, input validation, transitions and animations.
- FakeAuthRepository makes login/signup flow testable without real endpoints; isolate all backend assumptions in auth contract.
- DataStore onboarding flag, local user profile and fitness preferences, state restoration.
- **Done:** authentic-looking UI with working fake auth, clear failure states, valid back stack and no network requirement.

### P5 — Main app shell, Home & Profile (Figma D + relevant I)

- Home: empty/returning states; main bottom tabs Home/Workouts/Progress/History/Profile (confirm from current Figma).
- Profile, settings, edits, preferences, sheets and local device-status indicator; read real seeded/Room data.
- Shell, Top App Bar, Bottom Bar and transitions composed only from shared design-system primitives.
- **Done:** tab navigation, state changes, persistent profile/preferences and correctly reactive empty/populated displays.

### P6 — Workout planning (Figma E)

- My Workouts, Workout Details accordions, Create/Edit/Delete plan, exercise picker.
- Exercise catalog, plan exercise ordering, sets/rep targets/rest timing; no planned lifted weight.
- Room-backed create/update/delete transactions, undo/confirmation where specified; no hardcoded list-only behavior.
- **Done:** reboot-safe CRUD, validations, all real plans visible on Home and Workouts, consistent navigation.

### P7 — Session Dashboard & session state foundation (Figma F, first half)

- Start/resume workout; session state machine, selected arbitrary exercise/set, session summary and progress.
- Dashboard rows in states unstarted/completed/rest active/all done, fixed controls, End Workout Early sheet and 0-set/partial/full exit policy.
- Active-session snapshot/persistence and process-death recovery; non-sequential set scheduling.
- UI driven by Room/ViewModel state, not static Figma demo A–E frame numbers.
- **Done:** session creation/resume and progress display tested, early-exit policy specified/tested; counter integration completed in P8.

### P8 — Rep Counter, rest & workout completion (Figma F, second half)

- Manual ready/active counter, +/- rep correction, per-set actual load control, Finish Set, optional user-approved rest skip.
- Single transaction to finish the selected set, correct progress recalculation; exact in-dashboard rest deadline; cancel/back behaviors.
- Device-assisted UI reuses same underlying set completion logic via an independent data-source interface, initially simulator-only.
- Set completion idempotency and safe lifecycle recovery; cannot fabricate ROM/tempo from manual tracking.
- **Done:** real end-to-end workout lifecycle works fully offline without BLE; arbitrary set order validated.

### P9 — Workout Complete, History & Progress (Figma G + History tab)

- Complete/partial summaries, History populated/empty, Session Details, Repeat Workout, Progress/Analytics.
- Derive reps, volume, completion ratio and duration from recorded persisted sets; skipped exercises have no recorded load.
- Correct partial session save/discard handling; stable real data flows, not fake dashboard numbers.
- **Done:** historical records survive restart and metrics reconcile with data; repeat starts a clean session.

### P10 — Device Connection UI + BLE-ready simulator (Figma H)

- My Device, scanning/searching, found/none, connecting, connected, failed, lost connection and manual fallback.
- FakeDeviceDataSource simulates test cases: changing state, optional sample events, disconnect/reconnect and invalid/unavailable measurements; distinguish simulated from real device readings.
- BLE permission strategy by Android SDK level, lifecycle handling and runtime permission UI; no premature GATT UUID, packet schema, sensor accuracy or battery promises.
- Establish future adapter seam `RealBleDeviceDataSource` and protocol version/normalization boundary; actual GATT integration waits for firmware specification.
- **Done:** UI and session fallback tested via simulator; no dependency on hardware for basic workouts.

### P11 — Integration, Figma fidelity & motion QA

- Screenshots side-by-side with latest Figma, correct 360–412dp layouts and system insets.
- Bottom sheets, navigation transitions, accordion animations, timers and fixed action bars according to Figma; avoid decorative animation additions.
- Complete asset/photo mapping, loading/error/empty states, accessibility and text scaling.
- **Done:** visual parity report, known gaps fixed, device simulator and auth fake clearly disclosed in docs.

### P12 — Full QA, stability & delivery

- End-to-end tests: auth/onboarding, CRUD, active session, non-sequential sets, resume/restart, timer, partial/full end, analytics, simulated BLE connection/disconnection.
- Unit + DAO/instrumented UI tests, concurrency/idempotency, back stack/rotation/process restart, performance and accessibility.
- Validate build, code quality, clean GitHub commits, README, demo steps and implementation notes for real auth/BLE integration.
- **Done:** release-quality offline demo + explicit backlog for replacing FakeAuthRepository and FakeDeviceDataSource.

## Cross-phase completion contract

Every Antigravity phase prompt must require:

1. Inspect existing repository + Figma nodes and reuse previous work, without rewriting unrelated modules.
2. Work **only** within the phase's named scope; list any necessary cross-cutting changes explicitly.
3. Keep the project compiling after each logical unit; run build/tests or explicitly state blockers.
4. Provide changed files and dependencies, screenshots/previews, what is genuinely functional vs simulated, risks and how to test manually.
5. Do not overwrite hand-written code or the imported design-system module without reviewing it.
6. Commit work in an isolated branch/checkpoint, never silently push/merge or modify remote without explicit instruction.
7. Mark PASS / FAIL / NOT TESTED for each phase acceptance criterion.

## Known integration caveats

- GitHub starter currently has `:app`, namespace `com.example.pulse`, minSdk 24, AGP 9.1.1, Kotlin 2.2.10 and an older Compose BOM. Phase 0 must establish a verified compatible dependency baseline before any blind upgrades.
- AGP 9.x uses built-in Kotlin support; don't add Kotlin Android Gradle plugin inconsistently.
- Current Figma includes authentication and five main tabs; older research/handoff notes describing three tabs or no login are superseded by the user's current requirements.
- Existing research describes ESP32 + MPU6050 + ToF as a **design direction**, not measured production accuracy. Actual Bluetooth data format and sensor validity must remain protocol-dependent.
- The ZIP design-system module has not yet been supplied; final paths / APIs may be adjusted only after inspection in P0.