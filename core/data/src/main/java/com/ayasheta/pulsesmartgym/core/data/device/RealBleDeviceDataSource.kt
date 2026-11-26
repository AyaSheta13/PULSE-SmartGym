package com.ayasheta.pulsesmartgym.core.data.device

import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDataSource
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDescriptor
import com.ayasheta.pulsesmartgym.core.model.device.DeviceEvent
import com.ayasheta.pulsesmartgym.core.model.device.DeviceLinkState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationError
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationResult
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Replaceable boundary for future physical BLE hardware integration.
 * Real GATT connection, UUIDs, and packet decoding are deferred until a hardware firmware contract is finalized.
 */
class RealBleDeviceDataSource : DeviceDataSource {

    override val providerType: DeviceProviderType = DeviceProviderType.REAL_BLE

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

    override val events: Flow<DeviceEvent> = emptyFlow()

    private fun notConfiguredFailure(): DeviceOperationResult.Failure {
        val msg = "Physical BLE hardware protocol is not configured yet. Use Device Simulation or continue manually."
        _connectionState.value = _connectionState.value.copy(
            phase = DeviceConnectionState.FAILED,
            failureReason = msg,
            lastError = DeviceOperationError.PROTOCOL_NOT_CONFIGURED,
            selectedDevice = null,
            batteryPercent = null,
        )
        return DeviceOperationResult.Failure(
            error = DeviceOperationError.PROTOCOL_NOT_CONFIGURED,
            message = msg,
        )
    }

    override suspend fun startScan(): DeviceOperationResult = notConfiguredFailure()

    override suspend fun stopScan(): DeviceOperationResult = DeviceOperationResult.Success

    override suspend fun connect(deviceId: String): DeviceOperationResult = notConfiguredFailure()

    override suspend fun cancelConnection(): DeviceOperationResult = DeviceOperationResult.Success

    override suspend fun disconnect(): DeviceOperationResult {
        resetToDisconnected()
        return DeviceOperationResult.Success
    }

    override suspend fun reconnect(): DeviceOperationResult = notConfiguredFailure()

    override fun resetToDisconnected() {
        _discoveredDevices.value = emptyList()
        _connectionState.value = DeviceLinkState(
            phase = DeviceConnectionState.DISCONNECTED,
            providerType = DeviceProviderType.REAL_BLE,
            isTelemetrySimulated = false,
        )
    }
}
