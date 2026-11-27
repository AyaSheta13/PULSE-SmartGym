# P12 Execution Plan — Final Application QA, Stabilization & Release Preparation

**Branch**: `feat/p12-final-qa-release` (branched from `feat/p11-integration-visual-motion-qa` @ `604b0dc3fef25814929d86909d770e9b3283e9dd`)  
**Target**: Release Candidate Readiness (Gates A & B; Gate C Physical Hardware Validation remains explicitly pending until ESP32 + sensors are available).

---

## Checklist, Priorities & Dependencies

| Priority | Workstream | Scope & Deliverables | Acceptance Gate |
| --- | --- | --- | --- |
| **P0** | **Hardware Integration Readiness Defect Fixes** | Fix MTU negotiation (`requestMtu` + 20-byte ATT-safe framing), separate `isSensorCalibrated` from `protocolConfig.isProtocolConfirmed`, isolate `RAW_REPLAY` from genuine `SENSOR_TELEMETRY`/Room, harden GATT lifecycle (stale callbacks, missing CCCD, seq reset on reconnect), enforce blank tracking-window guard, fix `LivePerformanceWaveformCard` null-gap index preservation. | Gate B |
| **P0** | **Data Integrity & Security Review** | Disable cloud backup / exclude sensitive DB & DataStore in `AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml`; verify ProGuard/R8 rules, exported components, minimum permissions, and debug-only diagnostics gating. | Gate A & B |
| **P1** | **Full Application Functional Audit & E2E Scenarios** | Audit Auth, Onboarding, Home, Workouts, Set Execution, Results, History, Progress, Profile/Settings, Device Center. Extend automated E2E tests covering Scenarios A–G (First-Time, Returning, Custom Plan, Device-Assisted, Connection Failure, Data Recovery, Edge Cases). | Gate A & B |
| **P1** | **Visual, Responsive, Accessibility & Performance QA** | Verify Figma vs Android parity, 360×640 / 390×844 / 412×915 / tablet-large 480×960 viewports, font scaling (`1.3x`+), TalkBack semantics, touch targets, recomposition/buffer bounds. | Gate A |
| **P2** | **Release Preparation & Verification** | Review `applicationId`, `versionCode`, `versionName`, icon, splash, R8/ProGuard, generate Debug & Release APKs/AAB, record SHA-256 checksums, verify clean install & launch on emulator. | Gate A |
| **P2** | **Final P12 Documentation & GitHub Sync** | Produce all required `docs/P12_*.md`, `docs/APP_INSTALLATION_GUIDE.md`, `docs/KNOWN_LIMITATIONS.md`, updated `HARDWARE_INTEGRATION_CONTRACT.md` & `PHYSICAL_HARDWARE_TESTING_GUIDE.md`, commit in logical batches, push `feat/p12-final-qa-release`, verify remote SHA. | Gates A & B |

---

## Independent Acceptance Gates
- **Gate A — Android Application Readiness**: All non-hardware features work, approved designs preserved, data integrity verified, installable APK/AAB built and smoke-tested.
- **Gate B — Android Hardware Integration Readiness**: BLE transport, packet decoder, calibration gating, replay isolation, waveform index fidelity, and raw-telemetry E2E tests verified.
- **Gate C — Physical Hardware Validation**: **PENDING** (requires physical ESP32 firmware, sensors, finalized UUIDs, calibration, and live BLE testing).
