package com.ayasheta.pulsesmartgym.core.data.device.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.ayasheta.pulsesmartgym.core.model.device.DeviceCapabilities
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDescriptor
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationError
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationResult
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

/**
 * Android Bluetooth Low Energy transport backed by [BluetoothLeScanner] and [BluetoothGatt].
 * Contains no UI or exercise/measurement business logic.
 */
class AndroidBleTransport(
    private val context: Context,
) : BleTransport {

    private val appContext = context.applicationContext
    private val _events = MutableSharedFlow<BleTransportEvent>(extraBufferCapacity = 128)
    override val events: Flow<BleTransportEvent> = _events.asSharedFlow()

    private val bluetoothManager: BluetoothManager?
        get() = appContext.getSystemService(BluetoothManager::class.java)

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var activeConfig: BleProtocolConfig = BleProtocolConfig()
    private var userInitiatedDisconnect: Boolean = false

    override fun checkAvailabilityAndPermissions(): DeviceOperationResult {
        if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return DeviceOperationResult.Failure(
                error = DeviceOperationError.BLUETOOTH_UNAVAILABLE,
                message = "Bluetooth Low Energy hardware is unavailable on this device.",
            )
        }
        val adapter = bluetoothAdapter ?: return DeviceOperationResult.Failure(
            error = DeviceOperationError.BLUETOOTH_UNAVAILABLE,
            message = "Bluetooth adapter is unavailable.",
        )
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val missing = requiredPermissions.filter { perm ->
            ContextCompat.checkSelfPermission(appContext, perm) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            return DeviceOperationResult.Failure(
                error = DeviceOperationError.PERMISSION_REQUIRED,
                message = "Missing Bluetooth runtime permissions: ${missing.joinToString()}",
            )
        }
        if (!adapter.isEnabled) {
            return DeviceOperationResult.Failure(
                error = DeviceOperationError.BLUETOOTH_DISABLED,
                message = "Bluetooth is turned off. Enable Bluetooth to scan or connect.",
            )
        }
        return DeviceOperationResult.Success
    }

    @SuppressLint("MissingPermission")
    override suspend fun startScan(config: BleProtocolConfig): DeviceOperationResult {
        val check = checkAvailabilityAndPermissions()
        if (check is DeviceOperationResult.Failure) return check
        activeConfig = config
        val adapter = bluetoothAdapter ?: return DeviceOperationResult.Failure(
            DeviceOperationError.BLUETOOTH_UNAVAILABLE,
            message = "Bluetooth adapter unavailable.",
        )
        val leScanner = adapter.bluetoothLeScanner ?: return DeviceOperationResult.Failure(
            DeviceOperationError.BLUETOOTH_UNAVAILABLE,
            message = "BLE scanner unavailable.",
        )
        stopScan()
        scanner = leScanner
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val name = result.scanRecord?.deviceName ?: device.name
                val matchesPrefix = config.deviceNamePrefixes.isEmpty() ||
                    (!name.isNullOrBlank() && config.deviceNamePrefixes.any { name.startsWith(it, ignoreCase = true) })
                if (!matchesPrefix && config.serviceUuid == null) return
                _events.tryEmit(
                    BleTransportEvent.DeviceDiscovered(
                        DeviceDescriptor(
                            id = device.address,
                            name = name ?: "PULSE BLE Device",
                            profileLabel = "BLE GATT",
                            rssiDbm = result.rssi,
                            signalLabel = "${result.rssi} dBm",
                            providerType = DeviceProviderType.REAL_BLE,
                            isSimulated = false,
                            capabilities = DeviceCapabilities(),
                        ),
                    ),
                )
            }

            override fun onScanFailed(errorCode: Int) {
                _events.tryEmit(
                    BleTransportEvent.Error(
                        DeviceOperationError.UNKNOWN,
                        "BLE scan failed with Android error code $errorCode",
                    ),
                )
            }
        }
        scanCallback = callback
        val filters = config.serviceUuid?.let { uuid ->
            runCatching {
                listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(uuid)).build())
            }.getOrNull()
        }.orEmpty()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        leScanner.startScan(filters, settings, callback)
        return DeviceOperationResult.Success
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopScan(): DeviceOperationResult {
        val callback = scanCallback
        if (callback != null) {
            runCatching { scanner?.stopScan(callback) }
            scanCallback = null
        }
        return DeviceOperationResult.Success
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(deviceId: String, config: BleProtocolConfig): DeviceOperationResult {
        val check = checkAvailabilityAndPermissions()
        if (check is DeviceOperationResult.Failure) return check
        activeConfig = config
        stopScan()
        val adapter = bluetoothAdapter ?: return DeviceOperationResult.Failure(
            DeviceOperationError.BLUETOOTH_UNAVAILABLE,
            message = "Bluetooth adapter unavailable.",
        )
        val remoteDevice: BluetoothDevice = try {
            adapter.getRemoteDevice(deviceId)
        } catch (_: IllegalArgumentException) {
            return DeviceOperationResult.Failure(
                DeviceOperationError.DEVICE_NOT_FOUND,
                message = "Invalid BLE device address: $deviceId",
            )
        }
        closeGattOnly()
        userInitiatedDisconnect = false
        bluetoothGatt = remoteDevice.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        return DeviceOperationResult.Success
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect(): DeviceOperationResult {
        userInitiatedDisconnect = true
        bluetoothGatt?.disconnect()
        closeGattOnly()
        return DeviceOperationResult.Success
    }

    @SuppressLint("MissingPermission")
    override suspend fun writeCommand(characteristicUuid: String, payload: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val serviceUuid = activeConfig.serviceUuid ?: return false
        val service = runCatching { gatt.getService(UUID.fromString(serviceUuid)) }.getOrNull() ?: return false
        val char = runCatching { service.getCharacteristic(UUID.fromString(characteristicUuid)) }.getOrNull() ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(char, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) ==
                android.bluetooth.BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            run {
                char.value = payload
                gatt.writeCharacteristic(char)
            }
        }
    }

    override fun close() {
        runCatching {
            scanCallback?.let { cb -> scanner?.stopScan(cb) }
        }
        scanCallback = null
        closeGattOnly()
    }

    @SuppressLint("MissingPermission")
    private fun closeGattOnly() {
        runCatching { bluetoothGatt?.close() }
        bluetoothGatt = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (gatt !== bluetoothGatt) return
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                _events.tryEmit(BleTransportEvent.GattConnected(gatt.device.address, gatt.device.name))
                val mtuRequested = runCatching { gatt.requestMtu(activeConfig.requestedMtu) }.getOrDefault(false)
                if (!mtuRequested) {
                    _events.tryEmit(BleTransportEvent.MtuNegotiated(mtu = DEFAULT_ATT_MTU, maxPayloadBytes = DEFAULT_ATT_MTU - 3))
                    gatt.discoverServices()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                val unexpected = !userInitiatedDisconnect
                closeGattOnly()
                _events.tryEmit(BleTransportEvent.Disconnected(status = status, unexpected = unexpected))
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (gatt !== bluetoothGatt) return
            val effectiveMtu = if (status == BluetoothGatt.GATT_SUCCESS && mtu >= DEFAULT_ATT_MTU) mtu else DEFAULT_ATT_MTU
            _events.tryEmit(BleTransportEvent.MtuNegotiated(mtu = effectiveMtu, maxPayloadBytes = effectiveMtu - 3))
            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (gatt !== bluetoothGatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _events.tryEmit(
                    BleTransportEvent.Error(
                        DeviceOperationError.HANDSHAKE_FAILED,
                        "GATT service discovery failed (status=$status)",
                    ),
                )
                return
            }
            val discoveredUuids = gatt.services.map { it.uuid.toString() }
            _events.tryEmit(BleTransportEvent.ServicesDiscovered(discoveredUuids))

            val targetServiceUuid = activeConfig.serviceUuid
            val targetCharUuid = activeConfig.telemetryCharacteristicUuid
            if (targetServiceUuid.isNullOrBlank() || targetCharUuid.isNullOrBlank()) {
                _events.tryEmit(
                    BleTransportEvent.Error(
                        DeviceOperationError.PROTOCOL_NOT_CONFIGURED,
                        "Telemetry service/characteristic UUIDs are not configured.",
                    ),
                )
                return
            }
            val service = runCatching { gatt.getService(UUID.fromString(targetServiceUuid)) }.getOrNull()
            val characteristic = runCatching { service?.getCharacteristic(UUID.fromString(targetCharUuid)) }.getOrNull()
            if (service == null || characteristic == null) {
                _events.tryEmit(
                    BleTransportEvent.Error(
                        DeviceOperationError.HANDSHAKE_FAILED,
                        "Expected telemetry characteristic $targetCharUuid not found on device.",
                    ),
                )
                return
            }
            val supportsNotifyOrIndicate =
                (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                    (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
            gatt.setCharacteristicNotification(characteristic, true)
            val cccd = characteristic.getDescriptor(CCCD_UUID)
            if (cccd != null) {
                val enableValue = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 &&
                    (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0
                ) {
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                } else {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(cccd, enableValue)
                } else {
                    @Suppress("DEPRECATION")
                    run {
                        cccd.value = enableValue
                        gatt.writeDescriptor(cccd)
                    }
                }
            } else if (supportsNotifyOrIndicate) {
                _events.tryEmit(
                    BleTransportEvent.Error(
                        DeviceOperationError.HANDSHAKE_FAILED,
                        "Missing CCCD descriptor 0x2902 on telemetry characteristic $targetCharUuid.",
                    ),
                )
            } else {
                _events.tryEmit(BleTransportEvent.NotificationsSubscribed(targetCharUuid))
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (gatt !== bluetoothGatt) return
            if (descriptor.uuid == CCCD_UUID) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _events.tryEmit(
                        BleTransportEvent.NotificationsSubscribed(descriptor.characteristic.uuid.toString()),
                    )
                } else {
                    _events.tryEmit(
                        BleTransportEvent.Error(
                            DeviceOperationError.HANDSHAKE_FAILED,
                            "Failed to subscribe to telemetry notifications (status=$status)",
                        ),
                    )
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (gatt !== bluetoothGatt) return
            _events.tryEmit(
                BleTransportEvent.NotificationReceived(
                    characteristicUuid = characteristic.uuid.toString(),
                    payload = value.copyOf(),
                ),
            )
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (gatt !== bluetoothGatt) return
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION")
                val bytes = characteristic.value?.copyOf() ?: return
                _events.tryEmit(
                    BleTransportEvent.NotificationReceived(
                        characteristicUuid = characteristic.uuid.toString(),
                        payload = bytes,
                    ),
                )
            }
        }
    }

    companion object {
        private const val DEFAULT_ATT_MTU: Int = 23
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
