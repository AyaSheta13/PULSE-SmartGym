# P10 Firmware Integration Contract (BLE-Ready Boundary)

## Purpose
This document defines the exact replacement boundary between the PULSE Smart Gym Android application (`P10`) and future ESP32 / physical BLE hardware firmware (`P11`/`P12`). No proprietary firmware protocol, GATT service/characteristic UUIDs, or binary packet schemas are invented in P10.

## Architecture Boundary

| Layer | File | Responsibility |
| --- | --- | --- |
| Domain Contracts | `core/model/src/main/kotlin/.../device/DeviceCapabilities.kt` | Capability flags (`automaticRepEvents`, `fullPartialClassification`, `angularRom`, `linearRom`, `repDuration`) |
| Domain Contracts | `core/model/src/main/kotlin/.../device/DeviceDescriptor.kt` | Discovered/connected device metadata (`id`, `name`, `providerType`, `rssiDbm`, `capabilities`) |
| Domain Contracts | `core/model/src/main/kotlin/.../device/DeviceEvent.kt` | Inbound telemetry events (`DeviceRepEvent`, `DeviceBatteryUpdated`, `DeviceLinkLost`, `DeviceDiagnosticEvent`) |
| Domain Contracts | `core/model/src/main/kotlin/.../device/DeviceDataSource.kt` | Hardware-agnostic source interface (`startScan`, `stopScan`, `connect`, `cancelConnection`, `disconnect`, `reconnect`) |
| Domain Contracts | `core/model/src/main/kotlin/.../device/DeviceRepository.kt` | App-facing reactive repository (`linkState`, `discoveredDevices`, `events`, `activeProviderType`, `isDemoSessionActive`) |
| Simulator | `core/data/src/main/java/.../device/FakeDeviceDataSource.kt` | Deterministic `DeviceDataSource` for Demo Mode; emits `MeasurementOrigin.SIMULATED_TELEMETRY` |
| Real BLE Stub | `core/data/src/main/java/.../device/RealBleDeviceDataSource.kt` | Fail-honest `DeviceDataSource` returning `DeviceOperationError.PROTOCOL_NOT_CONFIGURED` until firmware GATT specs are finalized |
| Coordinator | `core/data/src/main/java/.../device/DefaultDeviceRepository.kt` | Switches between `SIMULATED` and `REAL_BLE`, resets on owner change, exposes unified `StateFlow`s |
| Validation | `core/data/src/main/java/.../device/DeviceEventNormalizer.kt` | Rejects stale `connectionGeneration`, wrong `trackingWindowId`, duplicate `eventId`, invalid durations/NaN, and origin mismatches; strips unsupported capabilities |

## What Firmware Team Must Supply Later
1. **Advertising & GATT UUIDs**: Service UUID and characteristic UUIDs for rep events, battery level, and control commands.
2. **Binary / Frame Schema**: Byte layout, endianness, scaling factors for rep classification, duration, and optional angular/linear ROM.
3. **Device Capabilities**: Mapping from advertised hardware/firmware version to `DeviceCapabilities`.
4. **Origin Tagging**: `RealBleDeviceDataSource` must emit `MeasurementOrigin.SENSOR_TELEMETRY` (never `SIMULATED_TELEMETRY`).

## Android Permission Policy (`BluetoothPermissionPolicy.kt`)
- **API 24–30**: `Manifest.permission.BLUETOOTH`, `BLUETOOTH_ADMIN`, and runtime `ACCESS_FINE_LOCATION` (`maxSdkVersion = 30`).
- **API 31+**: Runtime `BLUETOOTH_SCAN` (`usesPermissionFlags="neverForLocation"`) and `BLUETOOTH_CONNECT`.
- `<uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />` preserves Play Store availability on devices without BLE.
