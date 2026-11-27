package com.ayasheta.pulsesmartgym.feature.device.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.data.device.RealBleDeviceDataSource
import com.ayasheta.pulsesmartgym.core.data.device.ble.BlePacketDecoder
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.Button as PulsePrimaryButton
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepository
import com.ayasheta.pulsesmartgym.core.model.device.HardwareDataOrigin
import com.ayasheta.pulsesmartgym.core.model.device.HardwareDiagnosticsSnapshot
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun HardwareDiagnosticsScreen(
    deviceRepository: DeviceRepository,
    realBleDeviceDataSource: RealBleDeviceDataSource,
    onBackClick: () -> Unit,
) {
    val diagnostics by deviceRepository.diagnostics.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var replaySeq by remember { mutableStateOf(1L) }
    var copiedFeedback by remember { mutableStateOf(false) }

    HardwareDiagnosticsContent(
        diagnostics = diagnostics,
        copiedFeedback = copiedFeedback,
        onBackClick = onBackClick,
        onReplaySampleRep = {
            val payload = BlePacketDecoder.encodeRepTelemetryPacket(
                sequenceNumber = replaySeq++,
                classification = RepClassification.FULL,
                totalDurationMs = 2150L,
                concentricDurationMs = 920L,
                eccentricDurationMs = 1230L,
                angularRomDegrees = 96.4,
                linearRomCm = 44.2,
                peakAccelerationMetersPerSecondSquared = 3.45,
            )
            realBleDeviceDataSource.replayRawPayloadForDiagnostics(payload)
            copiedFeedback = false
        },
        onReplayMalformedPacket = {
            val badPayload = byteArrayOf(0x01, 0x01, 0x00, 0x01, 0xFF.toByte())
            realBleDeviceDataSource.replayRawPayloadForDiagnostics(badPayload)
            copiedFeedback = false
        },
        onResetCounters = {
            realBleDeviceDataSource.resetDiagnosticsCounters()
            copiedFeedback = false
        },
        onCopyReport = {
            val reportText = formatDiagnosticsReport(diagnostics)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("PULSE Hardware Diagnostics", reportText))
            copiedFeedback = true
        },
    )
}

@Composable
fun HardwareDiagnosticsContent(
    diagnostics: HardwareDiagnosticsSnapshot,
    copiedFeedback: Boolean,
    onBackClick: () -> Unit,
    onReplaySampleRep: () -> Unit,
    onReplayMalformedPacket: () -> Unit,
    onResetCounters: () -> Unit,
    onCopyReport: () -> Unit,
) {
    val originBadge = when (diagnostics.dataOrigin) {
        HardwareDataOrigin.REAL_BLE -> "REAL BLE" to Color(0xFF16A34A)
        HardwareDataOrigin.RAW_REPLAY -> "RAW REPLAY (DEV)" to Color(0xFF2563EB)
        HardwareDataOrigin.SIMULATED -> "SIMULATED" to Color(0xFFD97706)
        HardwareDataOrigin.NONE -> "NO TELEMETRY" to Color(0xFF6B7280)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF8F7))
            .navigationBarsPadding(),
    ) {
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
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onBackClick)
                            .testTag("button_diagnostics_back"),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "<",
                            style = PulseTheme.typography.cardTitle.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                    }
                    BasicText(
                        text = "Hardware Diagnostics (Dev)",
                        style = PulseTheme.typography.cardTitle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(originBadge.second.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("diagnostics_origin_badge"),
                ) {
                    BasicText(
                        text = originBadge.first,
                        style = PulseTheme.typography.label.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = originBadge.second,
                        ),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DiagCard(title = "Connection & GATT Pipeline") {
                DiagRow("Provider", diagnostics.providerType.name)
                DiagRow("Connection State", diagnostics.connectionState.name)
                DiagRow("Device Name", diagnostics.deviceName ?: "—")
                DiagRow("Device Address", diagnostics.deviceAddress ?: "—")
                DiagRow("Protocol Confirmed", if (diagnostics.protocolConfirmed) "Yes" else "No (Unconfigured)")
                DiagRow("Service Discovery", diagnostics.serviceDiscoveryStatus)
                DiagRow("Notifications", diagnostics.notificationSubscriptionStatus)
                DiagRow("Calibration", diagnostics.calibrationStatus)
            }

            DiagCard(title = "Telemetry Stream & Packet Counters") {
                DiagRow("Incoming Packets", diagnostics.incomingPacketCount.toString(), tag = "diag_incoming_packets")
                DiagRow("Invalid Packets", diagnostics.invalidPacketCount.toString(), tag = "diag_invalid_packets")
                DiagRow("Dropped Packets", diagnostics.droppedPacketCount.toString())
                DiagRow("Duplicate Packets", diagnostics.duplicatePacketCount.toString())
                DiagRow("Data Rate", "${"%.2f".format(diagnostics.packetsPerSecond)} pkt/s")
                DiagRow("Detected Reps", diagnostics.detectedRepCount.toString(), tag = "diag_detected_reps")
                DiagRow("Units", diagnostics.measurementUnits)
                DiagRow("Last Timestamp", diagnostics.lastReceivedTimestampMs?.toString() ?: "—")
            }

            DiagCard(title = "Last Raw & Decoded Packet") {
                BasicText(
                    text = "Raw Hex: ${diagnostics.lastPacketHex ?: "None"}",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = WorkoutPalette.TextPrimary,
                    ),
                    modifier = Modifier.testTag("diag_last_packet_hex"),
                )
                BasicText(
                    text = "Raw Sensor: ${diagnostics.lastRawSensorSummary ?: "—"}",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        color = WorkoutPalette.TextMuted,
                    ),
                )
                BasicText(
                    text = "Decoded: ${diagnostics.lastDecodedValuesSummary ?: "—"}",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                    modifier = Modifier.testTag("diag_last_decoded"),
                )
                if (!diagnostics.lastError.isNullOrBlank()) {
                    BasicText(
                        text = "Last Error: ${diagnostics.lastError}",
                        style = PulseTheme.typography.label.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626),
                        ),
                        modifier = Modifier.testTag("diag_last_error"),
                    )
                }
            }

            DiagCard(title = "Developer Pipeline Tools (Pre-Hardware)") {
                PulsePrimaryButton(
                    caption = "Replay Valid Rep Telemetry Packet",
                    onClick = onReplaySampleRep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("button_diag_replay_valid"),
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SecondaryActionButton(
                        label = "Inject Malformed",
                        onClick = onReplayMalformedPacket,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("button_diag_replay_malformed"),
                    )
                    SecondaryActionButton(
                        label = "Reset Counters",
                        onClick = onResetCounters,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("button_diag_reset"),
                    )
                }
                SecondaryActionButton(
                    label = if (copiedFeedback) "Copied Diagnostics Report!" else "Copy Diagnostics Report",
                    onClick = onCopyReport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("button_diag_copy"),
                )
            }
        }
    }
}

@Composable
private fun DiagCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFECE6E0)), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(
            text = title,
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = WorkoutPalette.TextPrimary,
            ),
        )
        content()
    }
}

@Composable
private fun DiagRow(
    label: String,
    value: String,
    tag: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = PulseTheme.typography.label.copy(
                fontSize = 12.sp,
                color = WorkoutPalette.TextMuted,
            ),
        )
        BasicText(
            text = value,
            style = PulseTheme.typography.label.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = WorkoutPalette.TextPrimary,
            ),
            modifier = if (tag != null) Modifier.testTag(tag) else Modifier,
        )
    }
}

@Composable
private fun SecondaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F2F0))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = PulseTheme.typography.label.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = WorkoutPalette.TextPrimary,
            ),
        )
    }
}

fun formatDiagnosticsReport(d: HardwareDiagnosticsSnapshot): String = buildString {
    appendLine("PULSE Hardware Diagnostics Report")
    appendLine("dataOrigin=${d.dataOrigin}")
    appendLine("providerType=${d.providerType}")
    appendLine("connectionState=${d.connectionState}")
    appendLine("deviceName=${d.deviceName}")
    appendLine("deviceAddress=${d.deviceAddress}")
    appendLine("protocolConfirmed=${d.protocolConfirmed}")
    appendLine("serviceDiscoveryStatus=${d.serviceDiscoveryStatus}")
    appendLine("notificationSubscriptionStatus=${d.notificationSubscriptionStatus}")
    appendLine("incomingPacketCount=${d.incomingPacketCount}")
    appendLine("invalidPacketCount=${d.invalidPacketCount}")
    appendLine("droppedPacketCount=${d.droppedPacketCount}")
    appendLine("duplicatePacketCount=${d.duplicatePacketCount}")
    appendLine("packetsPerSecond=${"%.2f".format(d.packetsPerSecond)}")
    appendLine("detectedRepCount=${d.detectedRepCount}")
    appendLine("measurementUnits=${d.measurementUnits}")
    appendLine("lastReceivedTimestampMs=${d.lastReceivedTimestampMs}")
    appendLine("lastPacketHex=${d.lastPacketHex}")
    appendLine("lastRawSensorSummary=${d.lastRawSensorSummary}")
    appendLine("lastDecodedValuesSummary=${d.lastDecodedValuesSummary}")
    appendLine("calibrationStatus=${d.calibrationStatus}")
    appendLine("lastError=${d.lastError}")
}
