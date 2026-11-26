package com.ayasheta.pulsesmartgym.feature

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.data.device.SimulatedDeviceCatalog
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.ConnectionDiagnosticStage
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceLinkState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationError
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.device.DiagnosticStageStatus
import com.ayasheta.pulsesmartgym.feature.device.DeviceUiState
import com.ayasheta.pulsesmartgym.feature.device.DiscoveryScreenStage
import com.ayasheta.pulsesmartgym.feature.device.center.MyDeviceScreenContent
import com.ayasheta.pulsesmartgym.feature.device.discovery.DeviceDiscoveryScreenContent
import com.ayasheta.pulsesmartgym.feature.session.devicecounter.DeviceAssistedRepCounterScreen
import com.ayasheta.pulsesmartgym.feature.session.devicecounter.DeviceAssistedRepCounterContent
import com.ayasheta.pulsesmartgym.feature.session.devicecounter.DeviceAssistedRepCounterUiState
import com.ayasheta.pulsesmartgym.feature.workout.editor.LocalRenderModalsInWindow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class P10ScreenshotCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureAllTenP10ReferenceScreensAt390x884() {
        val pulse1 = SimulatedDeviceCatalog.pulse001
        val pulse2 = SimulatedDeviceCatalog.pulse002
        val baseDemo = DeviceUiState(
            activeProviderType = DeviceProviderType.SIMULATED,
            isDemoSessionActive = true,
        )

        val states = listOf(
            // 01: My Device - Disconnected
            baseDemo.copy(
                linkState = DeviceLinkState(phase = DeviceConnectionState.DISCONNECTED, isTelemetrySimulated = true),
            ),
            // 02: Device Discovery - Searching
            baseDemo.copy(
                linkState = DeviceLinkState(phase = DeviceConnectionState.SCANNING, hasSearched = true, isTelemetrySimulated = true),
                discoveryStage = DiscoveryScreenStage.SEARCHING,
            ),
            // 03: Available Devices
            baseDemo.copy(
                linkState = DeviceLinkState(phase = DeviceConnectionState.DISCONNECTED, hasSearched = true, isTelemetrySimulated = true),
                discoveredDevices = listOf(pulse1, pulse2),
                discoveryStage = DiscoveryScreenStage.AVAILABLE_DEVICES,
            ),
            // 04: No Devices Found
            baseDemo.copy(
                linkState = DeviceLinkState(phase = DeviceConnectionState.DISCONNECTED, hasSearched = true, isTelemetrySimulated = true),
                discoveredDevices = emptyList(),
                discoveryStage = DiscoveryScreenStage.NO_DEVICES,
            ),
            // 05: Connecting
            baseDemo.copy(
                linkState = DeviceLinkState(
                    phase = DeviceConnectionState.CONNECTING,
                    selectedDevice = pulse1,
                    isTelemetrySimulated = true,
                    diagnosticStages = listOf(
                        ConnectionDiagnosticStage(1, "BLE device discovered", DiagnosticStageStatus.DONE, "Done"),
                        ConnectionDiagnosticStage(2, "Connection established", DiagnosticStageStatus.DONE, "Done"),
                        ConnectionDiagnosticStage(3, "Device services verified", DiagnosticStageStatus.IN_PROGRESS, "Verifying..."),
                        ConnectionDiagnosticStage(4, "Device ready for workout tracking", DiagnosticStageStatus.PENDING, "Pending"),
                    ),
                ),
                discoveryStage = DiscoveryScreenStage.CONNECTING,
            ),
            // 06: Connected Success
            baseDemo.copy(
                linkState = DeviceLinkState(
                    phase = DeviceConnectionState.CONNECTED,
                    selectedDevice = pulse1,
                    capabilities = pulse1.capabilities,
                    batteryPercent = 88,
                    isTelemetrySimulated = true,
                ),
                discoveryStage = DiscoveryScreenStage.CONNECTED_SUCCESS,
            ),
            // 07: My Device - Connected
            baseDemo.copy(
                linkState = DeviceLinkState(
                    phase = DeviceConnectionState.CONNECTED,
                    selectedDevice = pulse1,
                    capabilities = pulse1.capabilities,
                    batteryPercent = 88,
                    isTelemetrySimulated = true,
                ),
            ),
            // 09: Connection Failed
            baseDemo.copy(
                linkState = DeviceLinkState(
                    phase = DeviceConnectionState.FAILED,
                    selectedDevice = pulse1,
                    failureCode = "SIM_TIMEOUT_0x18",
                    failureReason = "Simulated handshake timed out with PULSE-001",
                    lastError = DeviceOperationError.CONNECTION_TIMEOUT,
                    isTelemetrySimulated = true,
                    diagnosticStages = listOf(
                        ConnectionDiagnosticStage(1, "BLE device discovered", DiagnosticStageStatus.DONE, "Done"),
                        ConnectionDiagnosticStage(2, "Connection established", DiagnosticStageStatus.DONE, "Done"),
                        ConnectionDiagnosticStage(3, "Simulated handshake", DiagnosticStageStatus.FAILED, "Timed out (Sim)"),
                        ConnectionDiagnosticStage(4, "Telemetry channel sync", DiagnosticStageStatus.ABORTED, "Aborted"),
                    ),
                ),
                discoveryStage = DiscoveryScreenStage.CONNECTION_FAILED,
            ),
        )

        val activeCounter = DeviceAssistedRepCounterUiState.Active(
            sessionId = "s1",
            exerciseId = "ex1",
            setIndex = 1,
            totalSetsForExercise = 4,
            planName = "Upper Body Power",
            exerciseName = "Barbell Bench Press",
            targetReps = 10,
            fullReps = 8,
            partialReps = 0,
            acceptedLedger = emptyList(),
            targetLoad = WeightLoad.fromKilograms(80.0),
            actualLoad = WeightLoad.fromKilograms(80.0),
            targetRestSeconds = 90,
            preferredWeightUnit = WeightUnit.KG,
            startedAtEpochMs = System.currentTimeMillis() - 255_000L,
            setStartedAtElapsedMs = 0L,
            recordedSetsCount = 0,
            deviceName = "PULSE-001",
            connectionPhase = DeviceConnectionState.CONNECTED,
            bluetoothStatus = PulseBluetoothChipStatus.CONNECTED,
            isSimulatedDevice = true,
            showRecoverySheet = false,
        )
        val lostCounter = activeCounter.copy(
            setIndex = 2,
            recordedSetsCount = 1,
            connectionPhase = DeviceConnectionState.CONNECTION_LOST,
            bluetoothStatus = PulseBluetoothChipStatus.CONNECTION_LOST,
            showRecoverySheet = true,
        )

        var mode by mutableIntStateOf(0)

        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = baseDensity.fontScale),
                LocalRenderModalsInWindow provides false,
            ) {
                PulseTheme {
                    Box(
                        modifier = Modifier
                            .size(width = 390.dp, height = 884.dp)
                            .consumeWindowInsets(WindowInsets.systemBars),
                    ) {
                        when (mode) {
                            0 -> MyDeviceScreenContent(
                                uiState = states[0],
                                onBackClick = {}, onSelectProvider = {}, onSearchAndConnectClick = {},
                                onEnterDemoSimulationClick = {}, onStartWorkoutWithAutoTracking = {}, onDisconnectClick = {},
                            )
                            1 -> DeviceDiscoveryScreenContent(
                                uiState = states[1],
                                onBackClick = {}, onRescanClick = {}, onCancelSearchClick = {},
                                onToggleTroubleshooting = {}, onSimulateNoDevicesClick = {}, onConnectDeviceClick = {},
                                onCancelConnectionClick = {}, onSimulateTimeoutClick = {}, onRetryConnectionClick = {},
                                onCancelFailedClick = {}, onContinueToWorkout = {}, onViewInDeviceCenter = {},
                                onContinueWithoutDevice = {}, onSwitchToSimulatorClick = {}, onRequestPermissionsClick = {},
                                onEnableBluetoothClick = {}, enableAnimations = false,
                            )
                            2 -> DeviceDiscoveryScreenContent(
                                uiState = states[2],
                                onBackClick = {}, onRescanClick = {}, onCancelSearchClick = {},
                                onToggleTroubleshooting = {}, onSimulateNoDevicesClick = {}, onConnectDeviceClick = {},
                                onCancelConnectionClick = {}, onSimulateTimeoutClick = {}, onRetryConnectionClick = {},
                                onCancelFailedClick = {}, onContinueToWorkout = {}, onViewInDeviceCenter = {},
                                onContinueWithoutDevice = {}, onSwitchToSimulatorClick = {}, onRequestPermissionsClick = {},
                                onEnableBluetoothClick = {}, enableAnimations = false,
                            )
                            3 -> DeviceDiscoveryScreenContent(
                                uiState = states[3],
                                onBackClick = {}, onRescanClick = {}, onCancelSearchClick = {},
                                onToggleTroubleshooting = {}, onSimulateNoDevicesClick = {}, onConnectDeviceClick = {},
                                onCancelConnectionClick = {}, onSimulateTimeoutClick = {}, onRetryConnectionClick = {},
                                onCancelFailedClick = {}, onContinueToWorkout = {}, onViewInDeviceCenter = {},
                                onContinueWithoutDevice = {}, onSwitchToSimulatorClick = {}, onRequestPermissionsClick = {},
                                onEnableBluetoothClick = {}, enableAnimations = false,
                            )
                            4 -> DeviceDiscoveryScreenContent(
                                uiState = states[4],
                                onBackClick = {}, onRescanClick = {}, onCancelSearchClick = {},
                                onToggleTroubleshooting = {}, onSimulateNoDevicesClick = {}, onConnectDeviceClick = {},
                                onCancelConnectionClick = {}, onSimulateTimeoutClick = {}, onRetryConnectionClick = {},
                                onCancelFailedClick = {}, onContinueToWorkout = {}, onViewInDeviceCenter = {},
                                onContinueWithoutDevice = {}, onSwitchToSimulatorClick = {}, onRequestPermissionsClick = {},
                                onEnableBluetoothClick = {}, enableAnimations = false,
                            )
                            5 -> DeviceDiscoveryScreenContent(
                                uiState = states[5],
                                onBackClick = {}, onRescanClick = {}, onCancelSearchClick = {},
                                onToggleTroubleshooting = {}, onSimulateNoDevicesClick = {}, onConnectDeviceClick = {},
                                onCancelConnectionClick = {}, onSimulateTimeoutClick = {}, onRetryConnectionClick = {},
                                onCancelFailedClick = {}, onContinueToWorkout = {}, onViewInDeviceCenter = {},
                                onContinueWithoutDevice = {}, onSwitchToSimulatorClick = {}, onRequestPermissionsClick = {},
                                onEnableBluetoothClick = {}, enableAnimations = false,
                            )
                            6 -> MyDeviceScreenContent(
                                uiState = states[6],
                                onBackClick = {}, onSelectProvider = {}, onSearchAndConnectClick = {},
                                onEnterDemoSimulationClick = {}, onStartWorkoutWithAutoTracking = {}, onDisconnectClick = {},
                            )
                            7 -> DeviceAssistedRepCounterContent(
                                uiState = lostCounter,
                                onBackClick = {}, onIncrementFullRep = {}, onIncrementPartialRep = {},
                                onDecrementRep = {}, onIncrementWeight = {}, onDecrementWeight = {},
                                onSimulateAutoRep = {}, onSimulateConnectionLoss = {}, onReconnectClick = {},
                                onContinueManuallyClick = {}, onFinishSetClick = {}, onEndWorkoutClick = {},
                                onDismissEndEarly = {}, onConfirmEndEarly = {}, enableTicker = false,
                            )
                            8 -> DeviceDiscoveryScreenContent(
                                uiState = states[7],
                                onBackClick = {}, onRescanClick = {}, onCancelSearchClick = {},
                                onToggleTroubleshooting = {}, onSimulateNoDevicesClick = {}, onConnectDeviceClick = {},
                                onCancelConnectionClick = {}, onSimulateTimeoutClick = {}, onRetryConnectionClick = {},
                                onCancelFailedClick = {}, onContinueToWorkout = {}, onViewInDeviceCenter = {},
                                onContinueWithoutDevice = {}, onSwitchToSimulatorClick = {}, onRequestPermissionsClick = {},
                                onEnableBluetoothClick = {}, enableAnimations = false,
                            )
                            else -> DeviceAssistedRepCounterContent(
                                uiState = activeCounter,
                                onBackClick = {}, onIncrementFullRep = {}, onIncrementPartialRep = {},
                                onDecrementRep = {}, onIncrementWeight = {}, onDecrementWeight = {},
                                onSimulateAutoRep = {}, onSimulateConnectionLoss = {}, onReconnectClick = {},
                                onContinueManuallyClick = {}, onFinishSetClick = {}, onEndWorkoutClick = {},
                                onDismissEndEarly = {}, onConfirmEndEarly = {}, enableTicker = false,
                            )
                        }
                    }
                }
            }
        }

        val outDir = File(ApplicationProvider.getApplicationContext<android.content.Context>().filesDir, "p10_screenshots")
        outDir.mkdirs()
        val names = listOf(
            "01_my_device_disconnected.png",
            "02_device_discovery_searching.png",
            "03_available_devices.png",
            "04_no_devices_found.png",
            "05_connecting.png",
            "06_connected_success.png",
            "07_my_device_connected.png",
            "08_connection_lost_recovery.png",
            "09_connection_failed.png",
            "10_device_assisted_counter.png",
        )
        names.forEachIndexed { idx, fileName ->
            mode = idx
            composeRule.waitForIdle()
            val outFile = File(outDir, fileName)
            FileOutputStream(outFile).use { out ->
                composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            assertTrue("Screenshot $fileName must exist and be non-empty", outFile.exists() && outFile.length() > 0L)
        }
    }
}
