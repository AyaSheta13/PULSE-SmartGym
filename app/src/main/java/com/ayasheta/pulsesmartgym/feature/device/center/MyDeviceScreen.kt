package com.ayasheta.pulsesmartgym.feature.device.center

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.Button as PulsePrimaryButton
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.feature.device.DeviceUiState
import com.ayasheta.pulsesmartgym.feature.device.DeviceViewModel
import com.ayasheta.pulsesmartgym.feature.device.components.PulsePodIllustration
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun MyDeviceScreen(
    viewModel: DeviceViewModel,
    onBackClick: () -> Unit,
    onSearchAndConnectClick: () -> Unit,
    onStartWorkoutWithAutoTracking: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MyDeviceScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onSelectProvider = viewModel::selectProvider,
        onSearchAndConnectClick = {
            if (uiState.activeProviderType == DeviceProviderType.SIMULATED) {
                viewModel.enterDemoMode()
            }
            onSearchAndConnectClick()
        },
        onEnterDemoSimulationClick = {
            viewModel.enterDemoMode()
            onSearchAndConnectClick()
        },
        onStartWorkoutWithAutoTracking = onStartWorkoutWithAutoTracking,
        onDisconnectClick = viewModel::disconnect,
    )
}

@Composable
fun MyDeviceScreenContent(
    uiState: DeviceUiState,
    onBackClick: () -> Unit,
    onSelectProvider: (DeviceProviderType) -> Unit,
    onSearchAndConnectClick: () -> Unit,
    onEnterDemoSimulationClick: () -> Unit,
    onStartWorkoutWithAutoTracking: () -> Unit,
    onDisconnectClick: () -> Unit,
) {
    val link = uiState.linkState
    val isConnected = link.phase == DeviceConnectionState.CONNECTED

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
                            .testTag("my_device_back_button"),
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
                        text = "My Device",
                        style = PulseTheme.typography.sectionTitle.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                }
                if (uiState.isDemoSessionActive || (isConnected && link.isSimulated)) {
                    Box(
                        modifier = Modifier
                            .clip(PulseTheme.corners.fullRounded)
                            .background(Color(0xFFFFEFEA))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        BasicText(
                            text = "Demo Mode",
                            style = PulseTheme.typography.label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WorkoutPalette.PrimaryCoral,
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
        ) {
            // 358 x 220dp Hardware Visual Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(18.dp))
                    .padding(16.dp)
                    .testTag("my_device_visual_card"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val deviceName = link.selectedDevice?.name
                PulsePodIllustration(
                    label = if (isConnected && deviceName != null) deviceName else "PULSE BLE",
                    isConnected = isConnected,
                )
                Spacer(modifier = Modifier.height(18.dp))
                val badgeBg = if (isConnected) Color(0xFFE7F8F1) else Color(0xFFF3F1EF)
                val dotColor = if (isConnected) Color(0xFF00B87C) else Color(0xFF9CA3AF)
                val badgeTextColor = if (isConnected) Color(0xFF0A8F63) else Color(0xFF6B7280)
                val badgeText = when {
                    isConnected && deviceName != null && link.isSimulated -> "$deviceName Connected (Simulated)"
                    isConnected && deviceName != null -> "$deviceName Connected"
                    else -> "No Device Connected"
                }
                Row(
                    modifier = Modifier
                        .clip(PulseTheme.corners.fullRounded)
                        .background(badgeBg)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("my_device_status_badge"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                    BasicText(
                        text = badgeText,
                        style = PulseTheme.typography.label.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeTextColor,
                        ),
                    )
                }
            }

            if (isConnected) {
                // Connected Hardware Status Card (420:75)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(18.dp))
                        .padding(18.dp)
                        .testTag("my_device_connected_card"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BasicText(
                        text = if (link.isSimulated) {
                            "Hardware Status (Prototype Demo Data)"
                        } else {
                            "Hardware Status"
                        },
                        style = PulseTheme.typography.cardTitle.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                    StatusRow("Active Device", link.selectedDevice?.name ?: "—")
                    StatusRow(
                        "Connection Status",
                        if (link.isSimulated) "Simulated Link Active" else "Connected",
                    )
                    if (link.capabilities.batteryTelemetry && link.batteryPercent != null) {
                        StatusRow(
                            if (link.isSimulated) "Simulated Battery" else "Battery",
                            if (link.isSimulated) "${link.batteryPercent}% • Sample Telemetry" else "${link.batteryPercent}%",
                            valueColor = Color(0xFF0A8F63),
                        )
                    }
                    StatusRow(
                        "Tracking Mode",
                        if (link.isSimulated) "Simulated Auto-Tracking Ready" else "Auto-Tracking Ready",
                    )
                }

                PulsePrimaryButton(
                    caption = "Start Workout with Auto-Tracking",
                    onClick = onStartWorkoutWithAutoTracking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("button_start_workout_auto_tracking"),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, Color(0xFFE5DFD9)), RoundedCornerShape(14.dp))
                        .clickable(onClick = onDisconnectClick)
                        .testTag("button_disconnect_device"),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Disconnect Device",
                        style = PulseTheme.typography.ctaPrimary.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WorkoutPalette.PrimaryCoral,
                        ),
                    )
                }
            } else {
                // Disconnected: Smart Workout Automation Card (420:68)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(18.dp))
                        .padding(18.dp)
                        .testTag("my_device_automation_card"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BasicText(
                        text = "Smart Workout Automation",
                        style = PulseTheme.typography.cardTitle.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                    BasicText(
                        text = "Pair your PULSE smart pod to demonstrate automatic repetition tracking and simulated rest intervals. Physical ESP32 hardware support will connect when firmware is finalized.",
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF6B6763),
                        ),
                    )

                    // Explicit Provider Selection (Simulator vs Physical BLE Boundary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProviderChip(
                            label = "Demo Simulator",
                            selected = uiState.activeProviderType == DeviceProviderType.SIMULATED,
                            onClick = { onSelectProvider(DeviceProviderType.SIMULATED) },
                            modifier = Modifier.weight(1f).testTag("provider_chip_simulated"),
                        )
                        ProviderChip(
                            label = "Physical BLE",
                            selected = uiState.activeProviderType == DeviceProviderType.REAL_BLE,
                            onClick = { onSelectProvider(DeviceProviderType.REAL_BLE) },
                            modifier = Modifier.weight(1f).testTag("provider_chip_real_ble"),
                        )
                    }

                    PulsePrimaryButton(
                        caption = "Search & Connect Device",
                        onClick = onSearchAndConnectClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("button_search_connect_device"),
                    )

                    if (uiState.activeProviderType == DeviceProviderType.REAL_BLE) {
                        BasicText(
                            text = "Want to preview without hardware? Enter Device Simulation →",
                            style = PulseTheme.typography.label.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WorkoutPalette.PrimaryCoral,
                            ),
                            modifier = Modifier
                                .clickable(onClick = onEnterDemoSimulationClick)
                                .testTag("button_enter_device_simulation"),
                        )
                    }
                }

                // Always usable without hardware card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF3F5F8))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    BasicText(
                        text = "Always usable without hardware",
                        style = PulseTheme.typography.cardTitle.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                    BasicText(
                        text = "You can log and execute all workout routines manually at any time without connecting a physical device.",
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFF6B6763),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFFFEFEA) else Color(0xFFF5F3F1))
            .border(
                BorderStroke(1.dp, if (selected) WorkoutPalette.PrimaryCoral else Color(0xFFE5DFD9)),
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = PulseTheme.typography.label.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) WorkoutPalette.PrimaryCoral else WorkoutPalette.TextMuted,
            ),
        )
    }
}

@Composable
private fun StatusRow(
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
                fontSize = 14.sp,
                color = Color(0xFF7A7570),
            ),
        )
        BasicText(
            text = value,
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
            ),
        )
    }
}
