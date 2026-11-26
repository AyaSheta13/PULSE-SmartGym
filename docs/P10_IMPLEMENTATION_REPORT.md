# P10 Implementation Report — Device Connection, BLE-Ready Architecture & Simulator

## Branch & Scope
- **Branch:** `feat/p10-device-connection-and-simulator`
- **Scope:** P10 only (Device Center, Discovery/Connecting/Failed/Connected flows, Connection-Lost Recovery + Manual Fallback, Device-Assisted Rep Counter, BLE-ready `DeviceDataSource`/`DeviceRepository` boundary, `FakeDeviceDataSource` simulator, `RealBleDeviceDataSource` fail-honest stub, API-aware Bluetooth permissions, isolated demo container).

## Architecture & Key Changes
1. **Domain Contracts (`:core:model`)**:
   - Added `MeasurementOrigin.SIMULATED_TELEMETRY` in `DeviceContracts.kt` (`PulseTypeConverters` uses `.name`/`Enum.valueOf()`, keeping Room schema at version 3).
   - Added `DeviceCapabilities.kt`, `DeviceDescriptor.kt`, `DeviceEvent.kt`, `DeviceDataSource.kt`, `DeviceRepository.kt`.
2. **Data & Simulation (`:core:data`)**:
   - Implemented `FakeDeviceDataSource.kt`, `DeviceSimulationScenario.kt`, `RealBleDeviceDataSource.kt`, `DefaultDeviceRepository.kt`, `DeviceEventNormalizer.kt`.
   - Updated `WorkoutSessionDao.kt` (`getRepRecordsForSet`) and `RoomWorkoutSessionRepository.kt` (`validateRepRecords`, weight-only correction preserves `repRecords`, count change clears contradictory `repRecords`).
   - Wired `fakeDeviceDataSource` and `deviceRepository` into `PulseDataContainer.kt`.
3. **Android App (`:app`)**:
   - Updated `AndroidManifest.xml` with API-split Bluetooth permissions (`maxSdkVersion="30"` legacy permissions + API 31+ `BLUETOOTH_SCAN` `neverForLocation` / `BLUETOOTH_CONNECT`, `bluetooth_le required="false"`).
   - Added `BluetoothPermissionPolicy.kt`, `DeviceViewModel.kt`, `PulsePodIllustration.kt`, `MyDeviceScreen.kt`, `DeviceDiscoveryScreen.kt`.
   - Added `SetDraftStore.kt`, `SetFinishHelper.kt`, `ConnectionLostRecoverySheet.kt`, `DeviceAssistedRepCounterUiState.kt`, `DeviceAssistedRepCounterViewModel.kt`, `DeviceAssistedRepCounterScreen.kt`.
   - Updated `ManualRepCounterViewModel.kt`, `ReadyStartSetUiState.kt`/`ViewModel`/`Screen`, `HomeScreen.kt`, `ProfileScreen.kt`, `TopBar.kt` (`home_bluetooth_chip` tag), `PulseNavigator.kt`, and `PulseNavigationEntryProvider.kt` (isolated `demoIsolated` `PulseDataContainer.createIsolatedForTesting` for Demo Mode).

## Verification Results

| Suite | Total | Passed | Failed | Errors | Skipped |
| --- | --- | --- | --- | --- | --- |
| `:core:model:test` | 6 | 6 | 0 | 0 | 0 |
| `:core:data:testDebugUnitTest` | 4 | 4 | 0 | 0 | 0 |
| `:core:designsystem:testDebugUnitTest` | 4 | 4 | 0 | 0 | 0 |
| `:app:testDebugUnitTest` | 54 | 54 | 0 | 0 | 0 |
| `:core:data:connectedDebugAndroidTest` | 26 | 26 | 0 | 0 | 0 |
| `:app:connectedDebugAndroidTest` | 40 | 40 | 0 | 0 | 0 |
| **Total** | **134** | **134** | **0** | **0** | **0** |

- `:app:assembleDebug` — BUILD SUCCESSFUL
- `:app:assembleRelease` — BUILD SUCCESSFUL
- Visual QA (`P10ScreenshotCaptureTest`): All 10 Figma comparison screenshots verified in `docs/P10_VISUAL_QA_REPORT.md`.
