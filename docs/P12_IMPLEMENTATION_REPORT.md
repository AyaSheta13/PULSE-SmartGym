# P12 Implementation Report — Final QA, Stabilization & Release Preparation

**Branch**: `feat/p12-final-qa-release`  
**Base Commit**: `604b0dc3fef25814929d86909d770e9b3283e9dd` (`feat/p11-integration-visual-motion-qa`)

## Summary of P12 Engineering & Stabilization Changes

### 1. Hardware Integration Readiness Defect Fixes
- **BLE ATT MTU Negotiation & 20-Byte Safe Framing** (`AndroidBleTransport.kt`, `BlePacketDecoder.kt`, `BleTransport.kt`):
  - `AndroidBleTransport` requests `gatt.requestMtu(64)` after `STATE_CONNECTED` and falls back to `DEFAULT_ATT_MTU = 23` (20-byte payload) if negotiation fails, emitting `BleTransportEvent.MtuNegotiated`.
  - `BlePacketDecoder` supports both **20-byte MTU-safe** `0x01` Rep Telemetry (`REP_PACKET_SIZE_COMPACT = 20`) / **19-byte** `0x02` Sensor Sample (`SAMPLE_PACKET_SIZE_COMPACT = 19`) and 22-byte extended frames.
- **Calibration Integrity** (`BleProtocolConfig.kt`, `RealBleDeviceDataSource.kt`, `HardwareDiagnostics.kt`):
  - Separated `isSensorCalibrated` from `isProtocolConfirmed`. Both default to `false`, preventing uncalibrated automatic rep detection in `GENUINE` mode.
- **Raw Replay Isolation** (`RealBleDeviceDataSource.kt`):
  - `replayRawPayloadForDiagnostics` updates `HardwareDiagnosticsSnapshot` (`HardwareDataOrigin.RAW_REPLAY`) ONLY and never emits `SENSOR_TELEMETRY` onto `events` or fakes `DeviceConnectionState.CONNECTED`.
- **GATT Connection Lifecycle & Tracking Window Integrity** (`AndroidBleTransport.kt`, `RealBleDeviceDataSource.kt`, `SetExecutionViewModel.kt`):
  - Added `gatt !== bluetoothGatt` stale-callback guards, explicit missing-CCCD (`0x2902`) failure, sequence-number reset on reconnect, and clearing `activeTrackingWindowId` (`""`) on `SetExecutionViewModel.onCleared()`.
- **Live Performance Null-Gap Preservation** (`SetExecutionComponents.kt`):
  - Replaced `mapNotNull` with index-preserving `List<Double?>` so missing rep measurements render as visual gaps instead of shifting subsequent repetitions to incorrect x-axis indices.

### 2. Data Integrity, Security & Compatibility Fixes
- **API 24+ Desugaring & Lint Fixes**:
  - Enabled `isCoreLibraryDesugaringEnabled = true` (`desugar_jdk_libs:2.1.5`) across `:app`, `:core:data`, and `:core:designsystem`, resolving 124 `NewApi` `java.time.*` lint errors.
  - Guarded `ValueAnimator.areAnimatorsEnabled()` in `PulseNavigationTransitions.kt` with `Build.VERSION.SDK_INT < Build.VERSION_CODES.O` check.
- **Backup & Release Hardening**:
  - Set `android:allowBackup="false"` in `AndroidManifest.xml` and excluded `pulse_smart_gym.db*` and `datastore` in `backup_rules.xml` and `data_extraction_rules.xml`.
  - Enabled `isMinifyEnabled = true` and `isShrinkResources = true` in `app/build.gradle.kts` with explicit R8 rules in `proguard-rules.pro`.

## Final Verification Summary
- **206 automated tests** (101 JVM unit + 105 Android instrumentation): **0 failures, 0 errors, 0 skipped**.
- **Android Lint (`:app:lintDebug`)**: **0 errors**.
- **Release Builds (`:app:assembleRelease`, `:app:bundleRelease`)**: **PASS** (R8 minified + resource-shrunk).
