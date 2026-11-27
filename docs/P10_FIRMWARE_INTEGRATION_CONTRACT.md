# P10 Firmware Integration Contract (BLE-Ready Boundary)

## Purpose
This document defines the exact replacement boundary between the PULSE Smart Gym Android application (`P10`) and future physical BLE hardware support. No proprietary firmware protocol, GATT service/characteristic UUIDs, or binary packet schemas are invented in P10. Physical hardware communication depends on an agreed firmware contract and separate verification.

## Architecture Boundary

| Layer | File | Responsibility |
| --- | --- | --- |
| Domain Contracts | `core/model/src/main/kotlin/.../device/DeviceContracts.kt` | `DeviceConnectionState`, `ExecutionEnvironmentKind`, `TrackingMode`, `MeasurementOrigin`, `RepMeasurement` (including `peakAccelerationMetersPerSecondSquared`) |
| Domain Contracts | `core/model/src/main/kotlin/.../device/DeviceCapabilities.kt` | Capability flags (`automaticRepEvents`, `fullPartialClassification`, `angularRom`, `linearRom`, `repDuration`, `peakAcceleration`) |
| Domain Contracts | `core/model/src/main/kotlin/.../device/DeviceDescriptor.kt` | Discovered/connected device metadata (`id`, `name`, `providerType`, `rssiDbm`, `capabilities`) |
| Domain Contracts | `core/model/src/main/kotlin/.../device/DeviceEvent.kt` | Inbound telemetry events (`DeviceRepEvent` with `peakAccelerationMetersPerSecondSquared`, `DeviceBatteryUpdated`, `DeviceLinkLost`, `DeviceDiagnosticEvent`) |
| Domain Contracts | `core/model/src/main/kotlin/.../device/DeviceDataSource.kt` | Hardware-agnostic source interface (`startScan`, `stopScan`, `connect`, `cancelConnection`, `disconnect`, `reconnect`) |
| Domain Contracts | `core/model/src/main/kotlin/.../device/DeviceRepository.kt` | App-facing reactive repository (`linkState`, `discoveredDevices`, `events`, `activeProviderType`, `isDemoSessionActive`) |
| Simulator | `core/data/src/main/java/.../device/FakeDeviceDataSource.kt` | Deterministic `DeviceDataSource` for Demo Mode; emits `MeasurementOrigin.SIMULATED_TELEMETRY` |
| Real BLE Stub | `core/data/src/main/java/.../device/RealBleDeviceDataSource.kt` | Fail-honest `DeviceDataSource` returning `DeviceOperationError.PROTOCOL_NOT_CONFIGURED` until firmware GATT specs are finalized |
| Coordinator | `core/data/src/main/java/.../device/DefaultDeviceRepository.kt` | Switches between `SIMULATED` and `REAL_BLE`, cancels stale jobs on provider/owner change, exposes unified `StateFlow`s |
| Validation | `core/data/src/main/java/.../device/DeviceEventNormalizer.kt` | Enforces auth, active session/set, accepting state, environment boundary, generation, window, deduplication, bounds (`100` reps / `120` IDs), and strict origin provenance |

## Provenance & Environment Rules
- `SIMULATED` provider events MUST carry `MeasurementOrigin.SIMULATED_TELEMETRY` and are ONLY accepted inside `ExecutionEnvironmentKind.DEMO`.
- `REAL_BLE` provider events MUST carry `MeasurementOrigin.SENSOR_TELEMETRY` when hardware integration is added after firmware contract sign-off.
- `SIMULATED` provider never authorizes automatic tracking on `ExecutionEnvironmentKind.GENUINE` sessions.
- **Effective Capability Policy (`ExerciseTrackingCatalog.resolveEffectiveCapabilities`)**: Device capabilities are intersected with per-exercise configuration (`ExerciseTrackingConfiguration`) and execution environment (`ExecutionEnvironmentKind`). Unsupported dimensions (e.g. angular ROM on a linear-only exercise) are gated out before normalization, and unconfigured physical tracking never enables automatic rep detection.

## Android Permission Policy (`BluetoothPermissionPolicy.kt`)
- **API 24–30**: `Manifest.permission.BLUETOOTH`, `BLUETOOTH_ADMIN`, and runtime `ACCESS_FINE_LOCATION` (`maxSdkVersion = 30`).
- **API 31+**: Runtime `BLUETOOTH_SCAN` (`usesPermissionFlags="neverForLocation"`) and `BLUETOOTH_CONNECT`.
- Uses `ActivityCompat.shouldShowRequestPermissionRationale` to distinguish `PERMISSION_DENIED` from `PERMISSION_PERMANENTLY_DENIED`, and refreshes readiness on `Lifecycle.Event.ON_RESUME` when returning from system settings.
- `<uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />` preserves Play Store availability on devices without BLE.
