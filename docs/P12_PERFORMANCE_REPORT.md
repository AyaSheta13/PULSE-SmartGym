# P12 Performance & Stability Report

## Inspected Areas & Targeted Safeguards
- **Startup**: Cold launch of `app-debug.apk` on `Medium_Phone_API_36.0` measured via `adb shell am start -W` completed in `3883 ms` (`Status: ok`, `LaunchState: COLD`).
- **Telemetry Buffer Bounds**: `DeviceEventNormalizer` enforces `MAX_ACCEPTED_EVENTS_PER_SET = 100` and `MAX_RETAINED_EVENT_IDS = 120`; `RealBleDeviceDataSource` bounds `seenSequenceNumbers` to `256` entries; `MutableSharedFlow` buffers are bounded (`64`–`128`).
- **BLE Resource Cleanup**: `AndroidBleTransport` closes `BluetoothGatt` on disconnect, cancellation, and `resetToDisconnected()`, and ignores stale GATT callbacks (`gatt !== bluetoothGatt`).
- **Main Thread & Recomposition**: Room & DataStore run off the main thread (`Dispatchers.IO` / Room coroutine dispatchers); `LivePerformanceWaveformCard` memoizes `signalKind`, `seriesValues`, and `validValues` via `remember(...)`.
- **R8 & Resource Shrinking**: Enabled in `release` build (`app-release-unsigned.apk` reduced from `32.7 MB` debug to `15.8 MB` release).
