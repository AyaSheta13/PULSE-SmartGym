package com.ayasheta.pulsesmartgym.core.data.device.ble

import com.ayasheta.pulsesmartgym.core.model.device.DeviceDescriptor
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationError
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationResult
import kotlinx.coroutines.flow.Flow

sealed interface BleTransportEvent {
    data class DeviceDiscovered(val device: DeviceDescriptor) : BleTransportEvent
    data class GattConnected(val deviceId: String, val deviceName: String?) : BleTransportEvent
    data class ServicesDiscovered(val serviceUuids: List<String>) : BleTransportEvent
    data class NotificationsSubscribed(val characteristicUuid: String) : BleTransportEvent
    data class NotificationReceived(
        val characteristicUuid: String,
        val payload: ByteArray,
        val receivedAtEpochMs: Long = System.currentTimeMillis(),
    ) : BleTransportEvent
    data class Disconnected(val status: Int, val unexpected: Boolean) : BleTransportEvent
    data class Error(
        val error: DeviceOperationError,
        val message: String,
    ) : BleTransportEvent
}

interface BleTransport {
    val events: Flow<BleTransportEvent>

    fun checkAvailabilityAndPermissions(): DeviceOperationResult
    suspend fun startScan(config: BleProtocolConfig): DeviceOperationResult
    suspend fun stopScan(): DeviceOperationResult
    suspend fun connect(deviceId: String, config: BleProtocolConfig): DeviceOperationResult
    suspend fun disconnect(): DeviceOperationResult
    suspend fun writeCommand(characteristicUuid: String, payload: ByteArray): Boolean
    fun close()
}
