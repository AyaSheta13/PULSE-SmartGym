package com.ayasheta.pulsesmartgym.core.data.device

import com.ayasheta.pulsesmartgym.core.model.device.ConnectionDiagnosticStage
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDataSource
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDescriptor
import com.ayasheta.pulsesmartgym.core.model.device.DeviceEvent
import com.ayasheta.pulsesmartgym.core.model.device.DeviceLinkState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationError
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationResult
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepEvent
import com.ayasheta.pulsesmartgym.core.model.device.DiagnosticStageStatus
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

class FakeDeviceDataSource(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val scanDelayMs: Long = DEFAULT_SCAN_DELAY_MS,
    private val connectStepDelayMs: Long = 350L,
) : DeviceDataSource {

    override val providerType: DeviceProviderType = DeviceProviderType.SIMULATED

    private val _connectionState = MutableStateFlow(
        DeviceLinkState(
            phase = DeviceConnectionState.DISCONNECTED,
            providerType = DeviceProviderType.SIMULATED,
            isTelemetrySimulated = true,
        ),
    )
    override val connectionState: StateFlow<DeviceLinkState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DeviceDescriptor>>(emptyList())
    override val discoveredDevices: StateFlow<List<DeviceDescriptor>> = _discoveredDevices.asStateFlow()

    private val _events = MutableSharedFlow<DeviceEvent>(extraBufferCapacity = 64)
    override val events: Flow<DeviceEvent> = _events.asSharedFlow()

    private val mutex = Mutex()
    private val generationCounter = AtomicLong(0L)
    private val eventSeqCounter = AtomicLong(0L)

    @Volatile
    private var scenario: DeviceSimulationScenario = DeviceSimulationScenario.DEFAULT_AVAILABLE

    private var scanJob: Job? = null
    private var connectJob: Job? = null

    fun setScenario(newScenario: DeviceSimulationScenario) {
        scenario = newScenario
    }

    override suspend fun startScan(): DeviceOperationResult {
        val deferred = CompletableDeferred<DeviceOperationResult>()
        mutex.withLock {
            if (_connectionState.value.phase == DeviceConnectionState.SCANNING) {
                return DeviceOperationResult.Failure(
                    error = DeviceOperationError.SCAN_ALREADY_ACTIVE,
                    message = "Device scan is already in progress.",
                )
            }
            connectJob?.cancel()
            connectJob = null
            _discoveredDevices.value = emptyList()
            _connectionState.value = _connectionState.value.copy(
                phase = DeviceConnectionState.SCANNING,
                providerType = DeviceProviderType.SIMULATED,
                hasSearched = true,
                failureReason = null,
                failureCode = null,
                lastError = null,
                diagnosticStages = emptyList(),
            )
            val gen = generationCounter.incrementAndGet()
            scanJob = scope.launch {
                try {
                    if (scanDelayMs > 0L) delay(scanDelayMs)
                    mutex.withLock {
                        if (generationCounter.get() != gen || _connectionState.value.phase != DeviceConnectionState.SCANNING) {
                            deferred.complete(
                                DeviceOperationResult.Failure(DeviceOperationError.CANCELED, message = "Search canceled."),
                            )
                            return@withLock
                        }
                        val devices = if (scenario == DeviceSimulationScenario.NO_DEVICES) {
                            emptyList()
                        } else {
                            SimulatedDeviceCatalog.defaultDevices.distinctBy { it.id }
                        }
                        _discoveredDevices.value = devices
                        _connectionState.value = _connectionState.value.copy(
                            phase = DeviceConnectionState.DISCONNECTED,
                            hasSearched = true,
                        )
                        deferred.complete(DeviceOperationResult.Success)
                    }
                } catch (ce: CancellationException) {
                    deferred.complete(
                        DeviceOperationResult.Failure(DeviceOperationError.CANCELED, message = "Search canceled."),
                    )
                    throw ce
                }
            }
        }
        return deferred.await()
    }

    override suspend fun stopScan(): DeviceOperationResult {
        mutex.withLock {
            scanJob?.cancel()
            scanJob = null
            generationCounter.incrementAndGet()
            if (_connectionState.value.phase == DeviceConnectionState.SCANNING) {
                _connectionState.value = _connectionState.value.copy(
                    phase = DeviceConnectionState.DISCONNECTED,
                )
            }
        }
        return DeviceOperationResult.Success
    }

    override suspend fun connect(deviceId: String): DeviceOperationResult {
        val target = _discoveredDevices.value.firstOrNull { it.id == deviceId }
            ?: SimulatedDeviceCatalog.defaultDevices.firstOrNull { it.id == deviceId }
            ?: return DeviceOperationResult.Failure(
                error = DeviceOperationError.DEVICE_NOT_FOUND,
                message = "Simulated device '$deviceId' was not found.",
            )
        return performConnect(target, isReconnect = false)
    }

    override suspend fun reconnect(): DeviceOperationResult {
        val selected = _connectionState.value.selectedDevice
            ?: return DeviceOperationResult.Failure(
                error = DeviceOperationError.DEVICE_NOT_FOUND,
                message = "No previously selected device to reconnect.",
            )
        return performConnect(selected, isReconnect = true)
    }

    private suspend fun performConnect(
        device: DeviceDescriptor,
        isReconnect: Boolean,
    ): DeviceOperationResult {
        val deferred = CompletableDeferred<DeviceOperationResult>()
        mutex.withLock {
            val currentPhase = _connectionState.value.phase
            if (currentPhase == DeviceConnectionState.CONNECTING) {
                return DeviceOperationResult.Failure(
                    error = DeviceOperationError.INVALID_STATE,
                    message = "Connection attempt already in progress.",
                )
            }
            if (!isReconnect && currentPhase == DeviceConnectionState.CONNECTED && _connectionState.value.selectedDevice?.id == device.id) {
                return DeviceOperationResult.Success
            }
            scanJob?.cancel()
            scanJob = null
            val gen = generationCounter.incrementAndGet()

            _connectionState.value = DeviceLinkState(
                phase = DeviceConnectionState.CONNECTING,
                providerType = DeviceProviderType.SIMULATED,
                selectedDevice = device,
                connectionGeneration = gen,
                capabilities = device.capabilities,
                batteryPercent = null,
                isTelemetrySimulated = true,
                hasSearched = true,
                diagnosticStages = connectingStages(step = 1),
            )

            connectJob = scope.launch {
                try {
                    if (connectStepDelayMs > 0L) delay(connectStepDelayMs)
                    if (!updateStageIfCurrent(gen, connectingStages(step = 2))) {
                        deferred.complete(DeviceOperationResult.Failure(DeviceOperationError.CANCELED, message = "Connection canceled."))
                        return@launch
                    }
                    if (connectStepDelayMs > 0L) delay(connectStepDelayMs)
                    if (!updateStageIfCurrent(gen, connectingStages(step = 3))) {
                        deferred.complete(DeviceOperationResult.Failure(DeviceOperationError.CANCELED, message = "Connection canceled."))
                        return@launch
                    }
                    if (connectStepDelayMs > 0L) delay(connectStepDelayMs)

                    val activeScenario = scenario
                    val shouldFailTimeout = activeScenario == DeviceSimulationScenario.CONNECTION_TIMEOUT
                    val shouldFailHandshake = activeScenario == DeviceSimulationScenario.CONNECTION_FAILURE ||
                        (isReconnect && activeScenario == DeviceSimulationScenario.RECONNECT_FAILURE)

                    mutex.withLock {
                        if (generationCounter.get() != gen || _connectionState.value.phase != DeviceConnectionState.CONNECTING) {
                            deferred.complete(DeviceOperationResult.Failure(DeviceOperationError.CANCELED, message = "Connection canceled."))
                            return@withLock
                        }
                        if (shouldFailTimeout || shouldFailHandshake) {
                            val err = if (shouldFailTimeout) DeviceOperationError.CONNECTION_TIMEOUT else DeviceOperationError.HANDSHAKE_FAILED
                            val reason = if (shouldFailTimeout) {
                                "Simulated handshake timed out with ${device.name}."
                            } else {
                                "Simulated handshake failed with ${device.name}."
                            }
                            _connectionState.value = _connectionState.value.copy(
                                phase = DeviceConnectionState.FAILED,
                                failureReason = reason,
                                failureCode = "SIM_TIMEOUT_0x18",
                                lastError = err,
                                diagnosticStages = failedStages(isTimeout = shouldFailTimeout),
                            )
                            deferred.complete(
                                DeviceOperationResult.Failure(
                                    error = err,
                                    code = "SIM_TIMEOUT_0x18",
                                    message = reason,
                                ),
                            )
                            return@withLock
                        }

                        val battery = SimulatedDeviceCatalog.sampleBatteryFor(device.id)
                        _connectionState.value = _connectionState.value.copy(
                            phase = DeviceConnectionState.CONNECTED,
                            batteryPercent = battery,
                            isTelemetrySimulated = true,
                            diagnosticStages = connectingStages(step = 4),
                            failureReason = null,
                            failureCode = null,
                            lastError = null,
                        )
                        deferred.complete(DeviceOperationResult.Success)
                    }
                } catch (ce: CancellationException) {
                    deferred.complete(DeviceOperationResult.Failure(DeviceOperationError.CANCELED, message = "Connection canceled."))
                    throw ce
                }
            }
        }
        return deferred.await()
    }

    private suspend fun updateStageIfCurrent(
        gen: Long,
        stages: List<ConnectionDiagnosticStage>,
    ): Boolean = mutex.withLock {
        if (generationCounter.get() != gen || _connectionState.value.phase != DeviceConnectionState.CONNECTING) {
            return@withLock false
        }
        _connectionState.value = _connectionState.value.copy(diagnosticStages = stages)
        true
    }

    override suspend fun cancelConnection(): DeviceOperationResult {
        mutex.withLock {
            connectJob?.cancel()
            connectJob = null
            generationCounter.incrementAndGet()
            if (_connectionState.value.phase == DeviceConnectionState.CONNECTING) {
                _connectionState.value = _connectionState.value.copy(
                    phase = DeviceConnectionState.DISCONNECTED,
                    diagnosticStages = emptyList(),
                    failureReason = null,
                    failureCode = null,
                    lastError = DeviceOperationError.CANCELED,
                )
            }
        }
        return DeviceOperationResult.Success
    }

    override suspend fun disconnect(): DeviceOperationResult {
        mutex.withLock {
            scanJob?.cancel()
            scanJob = null
            connectJob?.cancel()
            connectJob = null
            generationCounter.incrementAndGet()
            _connectionState.value = DeviceLinkState(
                phase = DeviceConnectionState.DISCONNECTED,
                providerType = DeviceProviderType.SIMULATED,
                isTelemetrySimulated = true,
                hasSearched = _connectionState.value.hasSearched,
            )
        }
        return DeviceOperationResult.Success
    }

    override fun resetToDisconnected() {
        scanJob?.cancel()
        scanJob = null
        connectJob?.cancel()
        connectJob = null
        generationCounter.incrementAndGet()
        _discoveredDevices.value = emptyList()
        _connectionState.value = DeviceLinkState(
            phase = DeviceConnectionState.DISCONNECTED,
            providerType = DeviceProviderType.SIMULATED,
            isTelemetrySimulated = true,
        )
    }

    suspend fun simulateConnectionLoss() {
        mutex.withLock {
            val current = _connectionState.value
            if (current.phase == DeviceConnectionState.CONNECTED) {
                _connectionState.value = current.copy(
                    phase = DeviceConnectionState.CONNECTION_LOST,
                    failureReason = "Simulated connection signal lost.",
                    lastError = DeviceOperationError.CONNECTION_TIMEOUT,
                )
            }
        }
    }

    suspend fun emitSimulatedRep(
        trackingWindowId: String,
        classification: RepClassification = RepClassification.FULL,
        totalDurationMs: Long? = 1_800L,
        concentricDurationMs: Long? = 820L,
        eccentricDurationMs: Long? = 980L,
        angularRomDegrees: Double? = 84.0,
        linearRomCm: Double? = 42.5,
        peakAccelerationMetersPerSecondSquared: Double? = 2.65,
        eventId: String = "sim_rep_${eventSeqCounter.incrementAndGet()}",
        sequenceNumber: Long = eventSeqCounter.get(),
        connectionGenerationOverride: Long? = null,
        originOverride: MeasurementOrigin = MeasurementOrigin.SIMULATED_TELEMETRY,
    ): Boolean {
        val state = _connectionState.value
        if (state.phase != DeviceConnectionState.CONNECTED && connectionGenerationOverride == null) {
            return false
        }
        val rep = DeviceRepEvent(
            eventId = eventId,
            providerType = DeviceProviderType.SIMULATED,
            connectionGeneration = connectionGenerationOverride ?: state.connectionGeneration,
            trackingWindowId = trackingWindowId,
            sequenceNumber = sequenceNumber,
            classification = classification,
            totalDurationMs = totalDurationMs,
            concentricDurationMs = concentricDurationMs,
            eccentricDurationMs = eccentricDurationMs,
            angularRomDegrees = angularRomDegrees,
            linearRomCm = linearRomCm,
            peakAccelerationMetersPerSecondSquared = peakAccelerationMetersPerSecondSquared,
            origin = originOverride,
        )
        _events.emit(DeviceEvent.RepRecorded(rep))
        return true
    }

    private fun connectingStages(step: Int): List<ConnectionDiagnosticStage> = listOf(
        ConnectionDiagnosticStage(
            1,
            "BLE device discovered (simulated)",
            if (step >= 1) DiagnosticStageStatus.DONE else DiagnosticStageStatus.PENDING,
            if (step >= 1) "Done" else "Pending",
        ),
        ConnectionDiagnosticStage(
            2,
            "Connection established (simulated)",
            when {
                step >= 2 -> DiagnosticStageStatus.DONE
                step == 1 -> DiagnosticStageStatus.IN_PROGRESS
                else -> DiagnosticStageStatus.PENDING
            },
            when {
                step >= 2 -> "Done"
                step == 1 -> "Connecting..."
                else -> "Pending"
            },
        ),
        ConnectionDiagnosticStage(
            3,
            "Device services verified (simulated)",
            when {
                step >= 4 -> DiagnosticStageStatus.DONE
                step in 2..3 -> DiagnosticStageStatus.IN_PROGRESS
                else -> DiagnosticStageStatus.PENDING
            },
            when {
                step >= 4 -> "Done"
                step in 2..3 -> "Verifying..."
                else -> "Pending"
            },
        ),
        ConnectionDiagnosticStage(
            4,
            "Device ready for workout tracking",
            if (step >= 4) DiagnosticStageStatus.DONE else DiagnosticStageStatus.PENDING,
            if (step >= 4) "Done" else "Pending",
        ),
    )

    private fun failedStages(isTimeout: Boolean): List<ConnectionDiagnosticStage> = listOf(
        ConnectionDiagnosticStage(1, "BLE device discovered (simulated)", DiagnosticStageStatus.DONE, "Done"),
        ConnectionDiagnosticStage(2, "Connection established (simulated)", DiagnosticStageStatus.DONE, "Done"),
        ConnectionDiagnosticStage(
            3,
            "Simulated handshake",
            DiagnosticStageStatus.FAILED,
            if (isTimeout) "Timed out (Sim)" else "Failed (Sim)",
        ),
        ConnectionDiagnosticStage(4, "Telemetry channel sync", DiagnosticStageStatus.ABORTED, "Aborted"),
    )

    companion object {
        const val DEFAULT_SCAN_DELAY_MS = 3_500L
    }
}
