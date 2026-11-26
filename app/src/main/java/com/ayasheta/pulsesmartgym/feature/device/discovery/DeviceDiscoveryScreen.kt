package com.ayasheta.pulsesmartgym.feature.device.discovery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.Button as PulsePrimaryButton
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.CheckMark
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.device.ConnectionDiagnosticStage
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDescriptor
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.device.DiagnosticStageStatus
import com.ayasheta.pulsesmartgym.feature.device.DeviceUiState
import com.ayasheta.pulsesmartgym.feature.device.DeviceViewModel
import com.ayasheta.pulsesmartgym.feature.device.DiscoveryScreenStage
import com.ayasheta.pulsesmartgym.feature.device.components.BluetoothRadarIndicator
import com.ayasheta.pulsesmartgym.feature.device.components.BluetoothVectorIcon
import com.ayasheta.pulsesmartgym.feature.device.permissions.BluetoothReadinessState
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun DeviceDiscoveryScreen(
    viewModel: DeviceViewModel,
    onBackClick: () -> Unit,
    onContinueToWorkout: () -> Unit,
    onViewInDeviceCenter: () -> Unit,
    onContinueWithoutDevice: () -> Unit,
    onRequestPermissionsClick: () -> Unit = {},
    onEnableBluetoothClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.activeProviderType) {
        val link = uiState.linkState
        if (link.phase == DeviceConnectionState.DISCONNECTED && !link.hasSearched) {
            viewModel.startScan()
        }
    }

    DeviceDiscoveryScreenContent(
        uiState = uiState,
        onBackClick = {
            if (uiState.discoveryStage == DiscoveryScreenStage.SEARCHING) {
                viewModel.cancelSearch()
            } else if (uiState.discoveryStage == DiscoveryScreenStage.CONNECTING) {
                viewModel.cancelConnection()
            }
            onBackClick()
        },
        onRescanClick = { viewModel.startScan(simulateNoDevices = false) },
        onCancelSearchClick = {
            viewModel.cancelSearch()
            onBackClick()
        },
        onToggleTroubleshooting = viewModel::toggleTroubleshooting,
        onSimulateNoDevicesClick = viewModel::simulateNoDevicesFound,
        onConnectDeviceClick = { deviceId -> viewModel.connect(deviceId) },
        onCancelConnectionClick = viewModel::cancelConnection,
        onSimulateTimeoutClick = viewModel::simulateHandshakeTimeout,
        onRetryConnectionClick = viewModel::retryConnection,
        onCancelFailedClick = {
            viewModel.startScan(simulateNoDevices = false)
        },
        onContinueToWorkout = onContinueToWorkout,
        onViewInDeviceCenter = onViewInDeviceCenter,
        onContinueWithoutDevice = onContinueWithoutDevice,
        onSwitchToSimulatorClick = {
            viewModel.enterDemoMode()
            viewModel.startScan(simulateNoDevices = false)
        },
        onRequestPermissionsClick = onRequestPermissionsClick,
        onEnableBluetoothClick = onEnableBluetoothClick,
    )
}

@Composable
fun DeviceDiscoveryScreenContent(
    uiState: DeviceUiState,
    onBackClick: () -> Unit,
    onRescanClick: () -> Unit,
    onCancelSearchClick: () -> Unit,
    onToggleTroubleshooting: () -> Unit,
    onSimulateNoDevicesClick: () -> Unit,
    onConnectDeviceClick: (String) -> Unit,
    onCancelConnectionClick: () -> Unit,
    onSimulateTimeoutClick: () -> Unit,
    onRetryConnectionClick: () -> Unit,
    onCancelFailedClick: () -> Unit,
    onContinueToWorkout: () -> Unit,
    onViewInDeviceCenter: () -> Unit,
    onContinueWithoutDevice: () -> Unit,
    onSwitchToSimulatorClick: () -> Unit,
    onRequestPermissionsClick: () -> Unit,
    onEnableBluetoothClick: () -> Unit,
    enableAnimations: Boolean = true,
) {
    val stage = uiState.discoveryStage
    val title = when (stage) {
        DiscoveryScreenStage.SEARCHING -> "Searching for Devices"
        DiscoveryScreenStage.AVAILABLE_DEVICES -> "Available Devices"
        DiscoveryScreenStage.NO_DEVICES -> "Device Discovery"
        DiscoveryScreenStage.CONNECTING -> "Connecting"
        DiscoveryScreenStage.CONNECTED_SUCCESS -> "Device Connected"
        DiscoveryScreenStage.CONNECTION_FAILED -> "Device Discovery"
        DiscoveryScreenStage.PROTOCOL_NOT_CONFIGURED,
        DiscoveryScreenStage.PERMISSION_OR_BLUETOOTH_BLOCKED,
        -> "Device Discovery"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF8F7))
            .navigationBarsPadding(),
    ) {
        // 64dp Top App Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBackClick)
                            .testTag("discovery_back_button"),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "‹",
                            style = PulseTheme.typography.sectionTitle.copy(
                                fontSize = 26.sp,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                    }
                    BasicText(
                        text = title,
                        style = PulseTheme.typography.sectionTitle.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                }
                if (stage == DiscoveryScreenStage.AVAILABLE_DEVICES) {
                    Box(
                        modifier = Modifier
                            .clip(PulseTheme.corners.fullRounded)
                            .background(Color(0xFFF3F1EF))
                            .clickable(onClick = onRescanClick)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("button_header_rescan"),
                    ) {
                        BasicText(
                            text = "Rescan",
                            style = PulseTheme.typography.label.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFECE8E3)),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (stage) {
                DiscoveryScreenStage.SEARCHING -> {
                    Spacer(modifier = Modifier.height(36.dp))
                    BluetoothRadarIndicator(isAnimating = enableAnimations)
                    BasicText(
                        text = "Scanning for Nearby Pods",
                        style = PulseTheme.typography.sectionTitle.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                    BasicText(
                        text = if (uiState.activeProviderType == DeviceProviderType.SIMULATED) {
                            "Searching simulated PULSE demo pods nearby (within 5 meters in real mode)."
                        } else {
                            "Make sure your PULSE device is powered on and within 5 meters of your phone."
                        },
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF7A7570),
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    OutlinedActionButton(
                        caption = "Cancel Search",
                        onClick = onCancelSearchClick,
                        modifier = Modifier.testTag("button_cancel_search"),
                    )
                    BasicText(
                        text = "Device not showing up? Tap here",
                        style = PulseTheme.typography.label.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = WorkoutPalette.PrimaryCoral,
                        ),
                        modifier = Modifier
                            .clickable(onClick = onToggleTroubleshooting)
                            .testTag("button_troubleshooting"),
                    )
                    if (uiState.showTroubleshootingInfo) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            BulletLine("Ensure pod battery is charged and LED blinks")
                            BulletLine("Keep pod within 5 meters of this phone")
                            BulletLine("Use Demo Simulator if physical hardware is unavailable")
                        }
                    }
                    if (uiState.activeProviderType == DeviceProviderType.SIMULATED) {
                        BasicText(
                            text = "Simulate No Devices Found ›",
                            style = PulseTheme.typography.label.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = WorkoutPalette.PrimaryCoral,
                            ),
                            modifier = Modifier
                                .clickable(onClick = onSimulateNoDevicesClick)
                                .testTag("button_simulate_no_devices"),
                        )
                    }
                }

                DiscoveryScreenStage.AVAILABLE_DEVICES -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val count = uiState.discoveredDevices.size
                        BasicText(
                            text = "Found $count nearby ${if (count == 1) "device" else "devices"}",
                            style = PulseTheme.typography.contentPrimary.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B6763),
                            ),
                        )
                        BasicText(
                            text = "Rescan",
                            style = PulseTheme.typography.label.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WorkoutPalette.PrimaryCoral,
                            ),
                            modifier = Modifier
                                .clickable(onClick = onRescanClick)
                                .testTag("button_inline_rescan"),
                        )
                    }
                    uiState.discoveredDevices.forEachIndexed { idx, device ->
                        AvailableDeviceCard(
                            device = device,
                            isPrimary = idx == 0,
                            onConnectClick = { onConnectDeviceClick(device.id) },
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF3F5F8))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        BasicText(
                            text = "Pairing tip",
                            style = PulseTheme.typography.cardTitle.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                        BasicText(
                            text = "Select a device to test simulated BLE connection. PULSE-001 and PULSE-002 demonstrate prototype pairing.",
                            style = PulseTheme.typography.contentPrimary.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF6B6763),
                            ),
                        )
                    }
                }

                DiscoveryScreenStage.NO_DEVICES -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(20.dp))
                            .padding(24.dp)
                            .testTag("no_devices_card"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3F5F8)),
                            contentAlignment = Alignment.Center,
                        ) {
                            BluetoothVectorIcon(
                                color = Color(0xFF4B5563),
                                size = 28.dp,
                                crossedOut = true,
                            )
                        }
                        BasicText(
                            text = "No PULSE Devices Found",
                            style = PulseTheme.typography.sectionTitle.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                        BasicText(
                            text = "We could not detect an active PULSE Bluetooth beacon nearby. Check the troubleshooting steps below.",
                            style = PulseTheme.typography.contentPrimary.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFF7A7570),
                                textAlign = TextAlign.Center,
                            ),
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF8F7F6))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            BulletLine("Ensure device battery has charge and LED blinks")
                            BulletLine("Keep device within 5 meters of this phone")
                            BulletLine("Confirm Android Bluetooth is turned on")
                        }
                        PulsePrimaryButton(
                            caption = "Scan Again",
                            onClick = onRescanClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("button_scan_again"),
                        )
                        BasicText(
                            text = "Continue Without Device",
                            style = PulseTheme.typography.label.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B6763),
                            ),
                            modifier = Modifier
                                .clickable(onClick = onContinueWithoutDevice)
                                .padding(vertical = 4.dp)
                                .testTag("button_continue_without_device"),
                        )
                    }
                }

                DiscoveryScreenStage.CONNECTING -> {
                    val deviceName = uiState.linkState.selectedDevice?.name ?: "PULSE-001"
                    Spacer(modifier = Modifier.height(16.dp))
                    BluetoothRadarIndicator(isAnimating = enableAnimations)
                    BasicText(
                        text = buildAnnotatedString {
                            append("Connecting to ")
                            withStyle(SpanStyle(color = WorkoutPalette.PrimaryCoral)) {
                                append(deviceName)
                            }
                        },
                        style = PulseTheme.typography.sectionTitle.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                    BasicText(
                        text = "Simulated BLE Pairing • Tap Cancel anytime",
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 14.sp,
                            color = Color(0xFF7A7570),
                        ),
                    )
                    DiagnosticsCard(
                        title = null,
                        stages = uiState.linkState.diagnosticStages,
                        footerNote = "* Simulated prototype connection sequence",
                        errorLine = null,
                    )
                    OutlinedActionButton(
                        caption = "Cancel Connection",
                        onClick = onCancelConnectionClick,
                        modifier = Modifier.testTag("button_cancel_connection"),
                    )
                    if (uiState.activeProviderType == DeviceProviderType.SIMULATED) {
                        BasicText(
                            text = "Simulate Handshake Timeout ›",
                            style = PulseTheme.typography.label.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = WorkoutPalette.PrimaryCoral,
                            ),
                            modifier = Modifier
                                .clickable(onClick = onSimulateTimeoutClick)
                                .testTag("button_simulate_handshake_timeout"),
                        )
                    }
                }

                DiscoveryScreenStage.CONNECTION_FAILED -> {
                    val link = uiState.linkState
                    val deviceName = link.selectedDevice?.name ?: "PULSE-001"
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(2.dp, WorkoutPalette.PrimaryCoral), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "!",
                            style = PulseTheme.typography.sectionTitle.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.PrimaryCoral,
                            ),
                        )
                    }
                    BasicText(
                        text = "Connection Failed",
                        style = PulseTheme.typography.sectionTitle.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                    BasicText(
                        text = buildAnnotatedString {
                            append("Simulated handshake timed out with ")
                            withStyle(SpanStyle(color = WorkoutPalette.PrimaryCoral, fontWeight = FontWeight.SemiBold)) {
                                append(deviceName)
                            }
                        },
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 15.sp,
                            color = Color(0xFF6B6763),
                            textAlign = TextAlign.Center,
                        ),
                    )
                    BasicText(
                        text = "Ensure the sensor is nearby, turned on, and not connected to another phone.",
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFF7A7570),
                            textAlign = TextAlign.Center,
                        ),
                    )
                    DiagnosticsCard(
                        title = "Connection Diagnostics",
                        stages = link.diagnosticStages,
                        footerNote = "* Simulated failure scenario for prototype demonstration",
                        errorLine = "Error: ${link.failureCode ?: "SIM_TIMEOUT_0x18"}",
                    )
                    PulsePrimaryButton(
                        caption = "Retry Connection",
                        onClick = onRetryConnectionClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("button_retry_connection"),
                    )
                    OutlinedActionButton(
                        caption = "Cancel (Return to Devices)",
                        onClick = onCancelFailedClick,
                        modifier = Modifier.testTag("button_cancel_failed_return"),
                    )
                }

                DiscoveryScreenStage.CONNECTED_SUCCESS -> {
                    val link = uiState.linkState
                    val deviceName = link.selectedDevice?.name ?: "PULSE-001"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(18.dp))
                            .padding(24.dp)
                            .testTag("connected_success_card"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE7F8F1)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                imageVector = CheckMark,
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(Color(0xFF0A8F63)),
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        BasicText(
                            text = "$deviceName Connected!",
                            style = PulseTheme.typography.sectionTitle.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0A8F63),
                            ),
                        )
                        BasicText(
                            text = "Simulated workout auto-tracking demonstration is ready.",
                            style = PulseTheme.typography.contentPrimary.copy(
                                fontSize = 13.sp,
                                color = Color(0xFF0A8F63),
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(18.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BasicText(
                            text = "Connection Details (Demonstration Profile)",
                            style = PulseTheme.typography.cardTitle.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                        DetailRow("Selected Device", deviceName)
                        DetailRow("Connection State", "Connected • Simulated Link")
                        if (link.batteryPercent != null) {
                            DetailRow("Simulated Battery", "${link.batteryPercent}% • Sample Telemetry")
                        }
                        DetailRow(
                            "Tracking Demo",
                            "Auto Reps & Classification Ready",
                            valueColor = Color(0xFF0A8F63),
                        )
                    }

                    PulsePrimaryButton(
                        caption = "Continue to Workout",
                        onClick = onContinueToWorkout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("button_continue_to_workout"),
                    )
                    OutlinedActionButton(
                        caption = "View in Device Center",
                        onClick = onViewInDeviceCenter,
                        modifier = Modifier.testTag("button_view_in_device_center"),
                    )
                }

                DiscoveryScreenStage.PROTOCOL_NOT_CONFIGURED -> {
                    DerivedBlockedCard(
                        title = "Physical Hardware Not Configured",
                        message = uiState.linkState.failureReason
                            ?: "Real ESP32 GATT communication is deferred until the firmware protocol is agreed. Switch to Device Simulation or continue manually.",
                        primaryCaption = "Switch to Device Simulation",
                        onPrimaryClick = onSwitchToSimulatorClick,
                        secondaryCaption = "Continue Without Device",
                        onSecondaryClick = onContinueWithoutDevice,
                    )
                }

                DiscoveryScreenStage.PERMISSION_OR_BLUETOOTH_BLOCKED -> {
                    val (titleTxt, msgTxt, btnTxt, act) = when (uiState.bluetoothReadiness) {
                        BluetoothReadinessState.BLUETOOTH_DISABLED -> Quad(
                            "Bluetooth Is Turned Off",
                            "Enable Android Bluetooth to scan for physical devices, or switch to Device Simulation.",
                            "Enable Bluetooth",
                            onEnableBluetoothClick,
                        )
                        BluetoothReadinessState.HARDWARE_UNAVAILABLE -> Quad(
                            "BLE Hardware Unavailable",
                            "This device does not report Bluetooth Low Energy support. You can still use Device Simulation or manual workouts.",
                            "Switch to Device Simulation",
                            onSwitchToSimulatorClick,
                        )
                        BluetoothReadinessState.PERMISSION_PERMANENTLY_DENIED -> Quad(
                            "Bluetooth Permission Required",
                            "Bluetooth permission was permanently denied. Enable it in system settings or use Device Simulation.",
                            "Switch to Device Simulation",
                            onSwitchToSimulatorClick,
                        )
                        else -> Quad(
                            "Bluetooth Permission Required",
                            "Grant nearby device permission to scan for physical PULSE pods, or use Device Simulation.",
                            "Grant Bluetooth Permission",
                            onRequestPermissionsClick,
                        )
                    }
                    DerivedBlockedCard(
                        title = titleTxt,
                        message = msgTxt,
                        primaryCaption = btnTxt,
                        onPrimaryClick = act,
                        secondaryCaption = "Continue Without Device",
                        onSecondaryClick = onContinueWithoutDevice,
                    )
                }
            }
        }
    }
}

private data class Quad(
    val a: String,
    val b: String,
    val c: String,
    val d: () -> Unit,
)

@Composable
private fun AvailableDeviceCard(
    device: DeviceDescriptor,
    isPrimary: Boolean,
    onConnectClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(
                BorderStroke(if (isPrimary) 1.5.dp else 1.dp, if (isPrimary) WorkoutPalette.PrimaryCoral else Color(0xFFF1EEEA)),
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 14.dp)
            .testTag("device_card_${device.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isPrimary) Color(0xFFFFEFEA) else Color(0xFFF1F2F4)),
            contentAlignment = Alignment.Center,
        ) {
            BluetoothVectorIcon(
                color = if (isPrimary) WorkoutPalette.PrimaryCoral else Color(0xFF6B7280),
                size = 22.dp,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            BasicText(
                text = device.name,
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
            )
            BasicText(
                text = "${device.signalLabel} • ${device.profileLabel}",
                style = PulseTheme.typography.contentPrimary.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF7A7570),
                ),
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isPrimary) WorkoutPalette.PrimaryCoral else Color(0xFFF1F2F4))
                .clickable(onClick = onConnectClick)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("button_connect_${device.id}"),
        ) {
            BasicText(
                text = "Connect",
                style = PulseTheme.typography.label.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPrimary) Color.White else WorkoutPalette.TextPrimary,
                ),
            )
        }
    }
}

@Composable
private fun DiagnosticsCard(
    title: String?,
    stages: List<ConnectionDiagnosticStage>,
    footerNote: String,
    errorLine: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("connection_diagnostics_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) {
            BasicText(
                text = title,
                style = PulseTheme.typography.label.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF7A7570),
                ),
            )
        }
        stages.forEach { st ->
            val statusColor = when (st.status) {
                DiagnosticStageStatus.DONE -> Color(0xFF0A8F63)
                DiagnosticStageStatus.IN_PROGRESS -> WorkoutPalette.PrimaryCoral
                DiagnosticStageStatus.FAILED -> WorkoutPalette.PrimaryCoral
                DiagnosticStageStatus.PENDING, DiagnosticStageStatus.ABORTED -> Color(0xFF9CA3AF)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = "${st.stepNumber}. ${st.title}",
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 13.sp,
                        color = WorkoutPalette.TextPrimary,
                    ),
                    modifier = Modifier.weight(1f),
                )
                BasicText(
                    text = st.statusLabel,
                    style = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    ),
                )
            }
        }
        if (errorLine != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFECE8E3)),
            )
            BasicText(
                text = errorLine,
                style = PulseTheme.typography.label.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF7A7570),
                ),
            )
        }
        BasicText(
            text = footerNote,
            style = PulseTheme.typography.label.copy(
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF),
            ),
        )
    }
}

@Composable
private fun DerivedBlockedCard(
    title: String,
    message: String,
    primaryCaption: String,
    onPrimaryClick: () -> Unit,
    secondaryCaption: String,
    onSecondaryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(18.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(
            text = title,
            style = PulseTheme.typography.sectionTitle.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = WorkoutPalette.TextPrimary,
            ),
        )
        BasicText(
            text = message,
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF7A7570),
                textAlign = TextAlign.Center,
            ),
        )
        PulsePrimaryButton(
            caption = primaryCaption,
            onClick = onPrimaryClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        )
        OutlinedActionButton(
            caption = secondaryCaption,
            onClick = onSecondaryClick,
        )
    }
}

@Composable
private fun OutlinedActionButton(
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFE5DFD9)), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = caption,
            style = PulseTheme.typography.ctaPrimary.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = WorkoutPalette.TextPrimary,
            ),
        )
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(WorkoutPalette.PrimaryCoral),
        )
        BasicText(
            text = text,
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 13.sp,
                color = WorkoutPalette.TextPrimary,
            ),
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = WorkoutPalette.TextPrimary,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 13.sp,
                color = Color(0xFF7A7570),
            ),
        )
        BasicText(
            text = value,
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
            ),
        )
    }
}
