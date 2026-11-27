package com.ayasheta.pulsesmartgym.core.data.device

import com.ayasheta.pulsesmartgym.core.data.device.ble.BlePacketDecoder
import com.ayasheta.pulsesmartgym.core.data.device.ble.BleProtocolConfig
import com.ayasheta.pulsesmartgym.core.data.device.ble.BleTransport
import com.ayasheta.pulsesmartgym.core.data.device.ble.BleTransportEvent
import com.ayasheta.pulsesmartgym.core.data.device.ble.DecodedBlePacket
import com.ayasheta.pulsesmartgym.core.model.device.DeviceCapabilities
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDataSource
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDescriptor
import com.ayasheta.pulsesmartgym.core.model.device.DeviceEvent
import com.ayasheta.pulsesmartgym.core.model.device.DeviceLinkState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationError
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationResult
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.device.HardwareDataOrigin
import com.ayasheta.pulsesmartgym.core.model.device.HardwareDiagnosticsSnapshot
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Physical BLE [DeviceDataSource] backed by [BleTransport] and [BlePacketDecoder].
 *
 * Until [protocolConfig] is confirmed ([BleProtocolConfig.isProtocolConfirmed] == true with
 * finalized service/characteristic UUIDs), scan/connect operations fail honestly with
 * [DeviceOperationError.PROTOCOL_NOT_CONFIGURED].
 */
class RealBleDeviceDataSource(
    private val transport: BleTransport? = null,
    initialConfig: BleProtocolConfig = BleProtocolConfig(),
    private val packetDecoder: BlePacketDecoder = BlePacketDecoder(initialConfig.protocolVersion),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val timeProviderMs: () -> Long = { System.currentTimeMillis() },
) : DeviceDataSource {

    override val providerType: DeviceProviderType = DeviceProviderType.REAL_BLE

    @Volatile
    private var protocolConfig: BleProtocolConfig = initialConfig

    @Volatile
    private var activeTrackingWindowId: String = ""

    private val generationCounter = AtomicLong(0L)
    private var lastSequenceNumber: Long? = null
    private val seenSequenceNumbers = LinkedHashSet<Long>()
    private var firstPacketTimestampMs: Long? = null
    private var lastConnectedDeviceId: String? = null
    private var scanTimeoutJob: Job? = null
    private var connectTimeoutJob: Job? = null

    private val _connectionState = MutableStateFlow(
        DeviceLinkState(
            phase = DeviceConnectionState.DISCONNECTED,
            providerType = DeviceProviderType.REAL_BLE,
            isTelemetrySimulated = false,
        ),
    )
    override val connectionState: StateFlow<DeviceLinkState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DeviceDescriptor>>(emptyList())
    override val discoveredDevices: StateFlow<List<DeviceDescriptor>> = _discoveredDevices.asStateFlow()

    private val _events = MutableSharedFlow<DeviceEvent>(extraBufferCapacity = 128)
    override val events: Flow<DeviceEvent> = _events.asSharedFlow()

    private val _diagnostics = MutableStateFlow(
        HardwareDiagnosticsSnapshot(
            providerType = DeviceProviderType.REAL_BLE,
            protocolConfirmed = initialConfig.isProtocolConfirmed,
        ),
    )
    val diagnostics: StateFlow<HardwareDiagnosticsSnapshot> = _diagnostics.asStateFlow()

    init {
        transport?.let { t ->
            scope.launch {
                t.events.collect { event -> handleTransportEvent(event) }
            }
        }
    }

    fun configureProtocol(config: BleProtocolConfig) {
        protocolConfig = config
        _diagnostics.value = _diagnostics.value.copy(
            protocolConfirmed = config.isProtocolConfirmed,
        )
    }

    fun setActiveTrackingWindow(trackingWindowId: String) {
        activeTrackingWindowId = trackingWindowId.trim()
    }

    fun resetDiagnosticsCounters() {
        lastSequenceNumber = null
        seenSequenceNumbers.clear()
        firstPacketTimestampMs = null
        _diagnostics.value = _diagnostics.value.copy(
            incomingPacketCount = 0L,
            invalidPacketCount = 0L,
            droppedPacketCount = 0L,
            duplicatePacketCount = 0L,
            packetsPerSecond = 0.0,
            lastPacketHex = null,
            lastRawSensorSummary = null,
            lastDecodedValuesSummary = null,
            lastReceivedTimestampMs = null,
            detectedRepCount = 0,
            lastError = null,
        )
    }

    /**
     * Developer-only replay helper that feeds raw BLE bytes through [BlePacketDecoder]
     * for [HardwareDiagnosticsSnapshot] inspection ONLY.
     *
     * Never emits [MeasurementOrigin.SENSOR_TELEMETRY] onto [_events] and never sets
     * [_connectionState] to [DeviceConnectionState.CONNECTED], guaranteeing raw replay
     * cannot pollute genuine workout history, personal records, or performance trends.
     */
    fun replayRawPayloadForDiagnostics(payload: ByteArray) {
        onNotificationPayloadReceived(payload, HardwareDataOrigin.RAW_REPLAY, emitEvents = false)
    }

    private fun notConfiguredFailure(): DeviceOperationResult.Failure {
        val msg = "Physical BLE hardware protocol is not configured yet. Use Device Simulation or continue manually."
        _connectionState.value = _connectionState.value.copy(
            phase = DeviceConnectionState.FAILED,
            failureReason = msg,
            lastError = DeviceOperationError.PROTOCOL_NOT_CONFIGURED,
            selectedDevice = null,
            batteryPercent = null,
        )
        _diagnostics.value = _diagnostics.value.copy(
            connectionState = DeviceConnectionState.FAILED,
            protocolConfirmed = false,
            lastError = msg,
        )
        return DeviceOperationResult.Failure(
            error = DeviceOperationError.PROTOCOL_NOT_CONFIGURED,
            message = msg,
        )
    }

    override suspend fun startScan(): DeviceOperationResult {
        val t = transport ?: return notConfiguredFailure()
        val readiness = t.checkAvailabilityAndPermissions()
        if (readiness is DeviceOperationResult.Failure) {
            _connectionState.value = _connectionState.value.copy(
                phase = DeviceConnectionState.FAILED,
                failureReason = readiness.message,
                lastError = readiness.error,
            )
            _diagnostics.value = _diagnostics.value.copy(
                connectionState = DeviceConnectionState.FAILED,
                lastError = readiness.message,
            )
            return readiness
        }
        if (!protocolConfig.isProtocolConfirmed || !protocolConfig.hasValidUuids()) {
            return notConfiguredFailure()
        }
        _discoveredDevices.value = emptyList()
        _connectionState.value = _connectionState.value.copy(
            phase = DeviceConnectionState.SCANNING,
            hasSearched = true,
            failureReason = null,
            lastError = null,
        )
        _diagnostics.value = _diagnostics.value.copy(
            dataOrigin = HardwareDataOrigin.REAL_BLE,
            connectionState = DeviceConnectionState.SCANNING,
            protocolConfirmed = true,
            lastError = null,
        )
        val result = t.startScan(protocolConfig)
        if (result is DeviceOperationResult.Failure) {
            _connectionState.value = _connectionState.value.copy(
                phase = DeviceConnectionState.FAILED,
                failureReason = result.message,
                lastError = result.error,
            )
            _diagnostics.value = _diagnostics.value.copy(
                connectionState = DeviceConnectionState.FAILED,
                lastError = result.message,
            )
            return result
        }
        scanTimeoutJob?.cancel()
        scanTimeoutJob = scope.launch {
            delay(protocolConfig.scanTimeoutMs)
            if (_connectionState.value.phase == DeviceConnectionState.SCANNING) {
                t.stopScan()
                _connectionState.value = _connectionState.value.copy(
                    phase = DeviceConnectionState.DISCONNECTED,
                    hasSearched = true,
                )
                _diagnostics.value = _diagnostics.value.copy(
                    connectionState = DeviceConnectionState.DISCONNECTED,
                )
            }
        }
        return DeviceOperationResult.Success
    }

    override suspend fun stopScan(): DeviceOperationResult {
        scanTimeoutJob?.cancel()
        transport?.stopScan()
        if (_connectionState.value.phase == DeviceConnectionState.SCANNING) {
            _connectionState.value = _connectionState.value.copy(phase = DeviceConnectionState.DISCONNECTED)
            _diagnostics.value = _diagnostics.value.copy(connectionState = DeviceConnectionState.DISCONNECTED)
        }
        return DeviceOperationResult.Success
    }

    override suspend fun connect(deviceId: String): DeviceOperationResult {
        val t = transport ?: return notConfiguredFailure()
        val readiness = t.checkAvailabilityAndPermissions()
        if (readiness is DeviceOperationResult.Failure) return readiness
        if (!protocolConfig.isProtocolConfirmed || !protocolConfig.hasValidUuids()) {
            return notConfiguredFailure()
        }
        scanTimeoutJob?.cancel()
        lastConnectedDeviceId = deviceId
        val selected = _discoveredDevices.value.firstOrNull { it.id == deviceId } ?: DeviceDescriptor(
            id = deviceId,
            name = "PULSE BLE Device",
            profileLabel = "BLE GATT",
            signalLabel = "Connected",
            providerType = DeviceProviderType.REAL_BLE,
            isSimulated = false,
            capabilities = defaultBleCapabilities(),
        )
        _connectionState.value = _connectionState.value.copy(
            phase = DeviceConnectionState.CONNECTING,
            selectedDevice = selected,
            failureReason = null,
            lastError = null,
        )
        _diagnostics.value = _diagnostics.value.copy(
            dataOrigin = HardwareDataOrigin.REAL_BLE,
            connectionState = DeviceConnectionState.CONNECTING,
            deviceName = selected.name,
            deviceAddress = selected.id,
            serviceDiscoveryStatus = "Connecting GATT...",
            notificationSubscriptionStatus = "Pending",
            lastError = null,
        )
        val result = t.connect(deviceId, protocolConfig)
        if (result is DeviceOperationResult.Failure) {
            _connectionState.value = _connectionState.value.copy(
                phase = DeviceConnectionState.FAILED,
                failureReason = result.message,
                lastError = result.error,
            )
            _diagnostics.value = _diagnostics.value.copy(
                connectionState = DeviceConnectionState.FAILED,
                lastError = result.message,
            )
            return result
        }
        connectTimeoutJob?.cancel()
        connectTimeoutJob = scope.launch {
            delay(protocolConfig.connectTimeoutMs)
            if (_connectionState.value.phase == DeviceConnectionState.CONNECTING) {
                t.disconnect()
                val msg = "BLE connection timed out after ${protocolConfig.connectTimeoutMs} ms"
                _connectionState.value = _connectionState.value.copy(
                    phase = DeviceConnectionState.FAILED,
                    failureReason = msg,
                    lastError = DeviceOperationError.CONNECTION_TIMEOUT,
                )
                _diagnostics.value = _diagnostics.value.copy(
                    connectionState = DeviceConnectionState.FAILED,
                    lastError = msg,
                )
            }
        }
        return DeviceOperationResult.Success
    }

    override suspend fun cancelConnection(): DeviceOperationResult {
        connectTimeoutJob?.cancel()
        transport?.disconnect()
        _connectionState.value = _connectionState.value.copy(
            phase = DeviceConnectionState.DISCONNECTED,
            selectedDevice = null,
        )
        _diagnostics.value = _diagnostics.value.copy(
            connectionState = DeviceConnectionState.DISCONNECTED,
            serviceDiscoveryStatus = "Canceled",
        )
        return DeviceOperationResult.Success
    }

    override suspend fun disconnect(): DeviceOperationResult {
        scanTimeoutJob?.cancel()
        connectTimeoutJob?.cancel()
        transport?.disconnect()
        resetToDisconnected()
        return DeviceOperationResult.Success
    }

    override suspend fun reconnect(): DeviceOperationResult {
        val targetId = lastConnectedDeviceId ?: _connectionState.value.selectedDevice?.id
        return if (targetId != null && protocolConfig.isProtocolConfirmed) {
            connect(targetId)
        } else {
            notConfiguredFailure()
        }
    }

    override fun resetToDisconnected() {
        scanTimeoutJob?.cancel()
        connectTimeoutJob?.cancel()
        transport?.close()
        lastSequenceNumber = null
        seenSequenceNumbers.clear()
        _discoveredDevices.value = emptyList()
        _connectionState.value = DeviceLinkState(
            phase = DeviceConnectionState.DISCONNECTED,
            providerType = DeviceProviderType.REAL_BLE,
            isTelemetrySimulated = false,
        )
        _diagnostics.value = _diagnostics.value.copy(
            connectionState = DeviceConnectionState.DISCONNECTED,
            serviceDiscoveryStatus = "Disconnected",
            notificationSubscriptionStatus = "Not subscribed",
        )
    }

    private fun handleTransportEvent(event: BleTransportEvent) {
        when (event) {
            is BleTransportEvent.DeviceDiscovered -> {
                val existing = _discoveredDevices.value
                if (existing.none { it.id == event.device.id }) {
                    _discoveredDevices.value = existing + event.device
                }
            }
            is BleTransportEvent.GattConnected -> {
                _diagnostics.value = _diagnostics.value.copy(
                    deviceName = event.deviceName ?: _diagnostics.value.deviceName,
                    deviceAddress = event.deviceId,
                    serviceDiscoveryStatus = "Negotiating MTU...",
                )
            }
            is BleTransportEvent.MtuNegotiated -> {
                _diagnostics.value = _diagnostics.value.copy(
                    negotiatedMtu = event.mtu,
                    serviceDiscoveryStatus = "MTU ${event.mtu} (${event.maxPayloadBytes}B payload) — Discovering services...",
                )
            }
            is BleTransportEvent.ServicesDiscovered -> {
                _diagnostics.value = _diagnostics.value.copy(
                    serviceDiscoveryStatus = "Discovered ${event.serviceUuids.size} service(s)",
                    notificationSubscriptionStatus = "Subscribing...",
                )
            }
            is BleTransportEvent.NotificationsSubscribed -> {
                connectTimeoutJob?.cancel()
                lastSequenceNumber = null
                seenSequenceNumbers.clear()
                val gen = generationCounter.incrementAndGet()
                val caps = defaultBleCapabilities()
                val calibrated = protocolConfig.isProtocolConfirmed && protocolConfig.isSensorCalibrated
                val currentDevice = _connectionState.value.selectedDevice?.copy(capabilities = caps)
                _connectionState.value = _connectionState.value.copy(
                    phase = DeviceConnectionState.CONNECTED,
                    selectedDevice = currentDevice,
                    connectionGeneration = gen,
                    capabilities = caps,
                    isCalibrated = calibrated,
                    failureReason = null,
                    lastError = null,
                )
                _diagnostics.value = _diagnostics.value.copy(
                    dataOrigin = HardwareDataOrigin.REAL_BLE,
                    connectionState = DeviceConnectionState.CONNECTED,
                    isCalibrated = calibrated,
                    calibrationStatus = if (calibrated) {
                        "Hardware calibration confirmed"
                    } else {
                        "Uncalibrated — automatic rep detection gated in GENUINE mode"
                    },
                    notificationSubscriptionStatus = "Subscribed (${event.characteristicUuid})",
                    lastError = null,
                )
            }
            is BleTransportEvent.NotificationReceived -> {
                onNotificationPayloadReceived(event.payload, HardwareDataOrigin.REAL_BLE, emitEvents = true)
            }
            is BleTransportEvent.Disconnected -> {
                connectTimeoutJob?.cancel()
                val nextPhase = if (event.unexpected) {
                    DeviceConnectionState.CONNECTION_LOST
                } else {
                    DeviceConnectionState.DISCONNECTED
                }
                _connectionState.value = _connectionState.value.copy(
                    phase = nextPhase,
                    failureReason = if (event.unexpected) "BLE connection lost (status=${event.status})" else null,
                )
                _diagnostics.value = _diagnostics.value.copy(
                    connectionState = nextPhase,
                    notificationSubscriptionStatus = "Unsubscribed",
                    lastError = if (event.unexpected) "Unexpected disconnect (GATT status=${event.status})" else null,
                )
            }
            is BleTransportEvent.Error -> {
                connectTimeoutJob?.cancel()
                _connectionState.value = _connectionState.value.copy(
                    phase = DeviceConnectionState.FAILED,
                    failureReason = event.message,
                    lastError = event.error,
                )
                _diagnostics.value = _diagnostics.value.copy(
                    connectionState = DeviceConnectionState.FAILED,
                    lastError = event.message,
                )
            }
        }
    }

    private fun onNotificationPayloadReceived(
        payload: ByteArray,
        originKind: HardwareDataOrigin,
        emitEvents: Boolean = true,
    ) {
        val nowMs = timeProviderMs()
        if (firstPacketTimestampMs == null) firstPacketTimestampMs = nowMs
        val currentDiag = _diagnostics.value
        val newIncoming = currentDiag.incomingPacketCount + 1L
        val elapsedSec = ((nowMs - (firstPacketTimestampMs ?: nowMs)).coerceAtLeast(1000L)) / 1000.0
        val rate = newIncoming / elapsedSec

        when (val decoded = packetDecoder.decode(payload)) {
            is DecodedBlePacket.RepTelemetry -> {
                val (dupDelta, dropDelta) = trackSequence(decoded.sequenceNumber)
                val gen = _connectionState.value.connectionGeneration
                val windowId = activeTrackingWindowId
                if (emitEvents && dupDelta == 0L && windowId.isNotBlank() && originKind == HardwareDataOrigin.REAL_BLE) {
                    val repEvent = decoded.toDeviceRepEvent(
                        connectionGeneration = gen,
                        trackingWindowId = windowId,
                        origin = MeasurementOrigin.SENSOR_TELEMETRY,
                    )
                    _events.tryEmit(DeviceEvent.RepRecorded(repEvent))
                }
                _diagnostics.value = currentDiag.copy(
                    dataOrigin = originKind,
                    incomingPacketCount = newIncoming,
                    duplicatePacketCount = currentDiag.duplicatePacketCount + dupDelta,
                    droppedPacketCount = currentDiag.droppedPacketCount + dropDelta,
                    packetsPerSecond = rate,
                    lastPacketHex = decoded.rawHex,
                    lastRawSensorSummary = "seq=${decoded.sequenceNumber}, class=${decoded.classification}",
                    lastDecodedValuesSummary = buildString {
                        append("Rep ${decoded.classification}")
                        decoded.linearRomCm?.let { append(" | LinROM=${it}cm") }
                        decoded.angularRomDegrees?.let { append(" | AngROM=${it}deg") }
                        decoded.peakAccelerationMetersPerSecondSquared?.let { append(" | PeakA=${it}m/s^2") }
                        decoded.totalDurationMs?.let { append(" | Dur=${it}ms") }
                    },
                    lastReceivedTimestampMs = nowMs,
                    detectedRepCount = if (dupDelta == 0L) currentDiag.detectedRepCount + 1 else currentDiag.detectedRepCount,
                )
            }
            is DecodedBlePacket.SensorSample -> {
                val (dupDelta, dropDelta) = trackSequence(decoded.sequenceNumber)
                _diagnostics.value = currentDiag.copy(
                    dataOrigin = originKind,
                    incomingPacketCount = newIncoming,
                    duplicatePacketCount = currentDiag.duplicatePacketCount + dupDelta,
                    droppedPacketCount = currentDiag.droppedPacketCount + dropDelta,
                    packetsPerSecond = rate,
                    lastPacketHex = decoded.rawHex,
                    lastRawSensorSummary = "ax=${"%.2f".format(decoded.accelXMs2)}, ay=${"%.2f".format(decoded.accelYMs2)}, az=${"%.2f".format(decoded.accelZMs2)}, gz=${decoded.gyroZDps}",
                    lastDecodedValuesSummary = "dist=${decoded.distanceCm ?: "N/A"} cm @ +${decoded.timestampOffsetMs}ms",
                    lastReceivedTimestampMs = nowMs,
                )
            }
            is DecodedBlePacket.BatteryStatus -> {
                val (dupDelta, dropDelta) = trackSequence(decoded.sequenceNumber)
                val gen = _connectionState.value.connectionGeneration
                _connectionState.value = _connectionState.value.copy(batteryPercent = decoded.batteryPercent)
                _events.tryEmit(DeviceEvent.BatteryUpdated(connectionGeneration = gen, percent = decoded.batteryPercent))
                _diagnostics.value = currentDiag.copy(
                    dataOrigin = originKind,
                    incomingPacketCount = newIncoming,
                    duplicatePacketCount = currentDiag.duplicatePacketCount + dupDelta,
                    droppedPacketCount = currentDiag.droppedPacketCount + dropDelta,
                    packetsPerSecond = rate,
                    lastPacketHex = decoded.rawHex,
                    lastRawSensorSummary = "Battery=${decoded.batteryPercent}%",
                    lastDecodedValuesSummary = "BatteryUpdated(${decoded.batteryPercent}%)",
                    lastReceivedTimestampMs = nowMs,
                )
            }
            is DecodedBlePacket.Malformed -> {
                _diagnostics.value = currentDiag.copy(
                    dataOrigin = originKind,
                    incomingPacketCount = newIncoming,
                    invalidPacketCount = currentDiag.invalidPacketCount + 1L,
                    packetsPerSecond = rate,
                    lastPacketHex = decoded.rawHex,
                    lastReceivedTimestampMs = nowMs,
                    lastError = "Malformed packet: ${decoded.reason}",
                )
            }
        }
    }

    private fun trackSequence(seq: Long): Pair<Long, Long> {
        val isDup = !seenSequenceNumbers.add(seq)
        if (seenSequenceNumbers.size > 256) {
            val first = seenSequenceNumbers.firstOrNull()
            if (first != null) seenSequenceNumbers.remove(first)
        }
        val prev = lastSequenceNumber
        val dropped = if (!isDup && prev != null && seq > prev + 1L) {
            seq - prev - 1L
        } else {
            0L
        }
        if (!isDup && (prev == null || seq > prev)) {
            lastSequenceNumber = seq
        }
        return (if (isDup) 1L else 0L) to dropped
    }

    companion object {
        fun defaultBleCapabilities(): DeviceCapabilities = DeviceCapabilities(
            automaticRepEvents = true,
            fullPartialClassification = true,
            angularRom = true,
            linearRom = true,
            repDuration = true,
            concentricDuration = true,
            eccentricDuration = true,
            batteryTelemetry = true,
            velocity = false,
            peakAcceleration = true,
            supportedTrackingCategories = TrackingCategory.entries.toSet(),
        )
    }
}
