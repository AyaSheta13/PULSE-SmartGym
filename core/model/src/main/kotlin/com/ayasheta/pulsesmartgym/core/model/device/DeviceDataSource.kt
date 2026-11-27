package com.ayasheta.pulsesmartgym.core.model.device

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class DeviceOperationError {
    PROTOCOL_NOT_CONFIGURED,
    BLUETOOTH_DISABLED,
    BLUETOOTH_UNAVAILABLE,
    PERMISSION_REQUIRED,
    SCAN_ALREADY_ACTIVE,
    DEVICE_NOT_FOUND,
    CONNECTION_TIMEOUT,
    HANDSHAKE_FAILED,
    ALREADY_CONNECTED,
    CANCELED,
    INVALID_STATE,
    UNKNOWN,
}

sealed interface DeviceOperationResult {
    data object Success : DeviceOperationResult
    data class Failure(
        val error: DeviceOperationError,
        val code: String? = null,
        val message: String,
    ) : DeviceOperationResult
}

enum class DiagnosticStageStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    FAILED,
    ABORTED,
}

data class ConnectionDiagnosticStage(
    val stepNumber: Int,
    val title: String,
    val status: DiagnosticStageStatus,
    val statusLabel: String,
)

data class DeviceLinkState(
    val phase: DeviceConnectionState = DeviceConnectionState.DISCONNECTED,
    val providerType: DeviceProviderType = DeviceProviderType.SIMULATED,
    val selectedDevice: DeviceDescriptor? = null,
    val connectionGeneration: Long = 0L,
    val capabilities: DeviceCapabilities = DeviceCapabilities(),
    val batteryPercent: Int? = null,
    val isTelemetrySimulated: Boolean = false,
    val isCalibrated: Boolean = false,
    val hasSearched: Boolean = false,
    val diagnosticStages: List<ConnectionDiagnosticStage> = emptyList(),
    val failureReason: String? = null,
    val failureCode: String? = null,
    val lastError: DeviceOperationError? = null,
) {
    val isConnected: Boolean get() = phase == DeviceConnectionState.CONNECTED
    val isSimulated: Boolean get() = providerType == DeviceProviderType.SIMULATED
}

interface DeviceDataSource {
    val providerType: DeviceProviderType
    val connectionState: StateFlow<DeviceLinkState>
    val discoveredDevices: StateFlow<List<DeviceDescriptor>>
    val events: Flow<DeviceEvent>

    suspend fun startScan(): DeviceOperationResult
    suspend fun stopScan(): DeviceOperationResult
    suspend fun connect(deviceId: String): DeviceOperationResult
    suspend fun cancelConnection(): DeviceOperationResult
    suspend fun disconnect(): DeviceOperationResult
    suspend fun reconnect(): DeviceOperationResult
    fun resetToDisconnected()
}
