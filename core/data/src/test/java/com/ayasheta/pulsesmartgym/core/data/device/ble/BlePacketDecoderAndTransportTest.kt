package com.ayasheta.pulsesmartgym.core.data.device.ble

import com.ayasheta.pulsesmartgym.core.data.device.RealBleDeviceDataSource
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceDescriptor
import com.ayasheta.pulsesmartgym.core.model.device.DeviceEvent
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationError
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationResult
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlePacketDecoderAndTransportTest {

    private val decoder = BlePacketDecoder()

    @Test
    fun decodeRepTelemetry_decodesFullAndPartialRepsAndSentinels() {
        val fullBytes = BlePacketDecoder.encodeRepTelemetryPacket(
            sequenceNumber = 1L,
            classification = RepClassification.FULL,
            totalDurationMs = 2150L,
            concentricDurationMs = 920L,
            eccentricDurationMs = 1230L,
            angularRomDegrees = 96.4,
            linearRomCm = 44.2,
            peakAccelerationMetersPerSecondSquared = 3.45,
        )
        val decodedFull = decoder.decode(fullBytes) as DecodedBlePacket.RepTelemetry
        assertEquals(1L, decodedFull.sequenceNumber)
        assertEquals(RepClassification.FULL, decodedFull.classification)
        assertEquals(2150L, decodedFull.totalDurationMs)
        assertEquals(920L, decodedFull.concentricDurationMs)
        assertEquals(1230L, decodedFull.eccentricDurationMs)
        assertEquals(96.4, decodedFull.angularRomDegrees!!, 0.01)
        assertEquals(44.2, decodedFull.linearRomCm!!, 0.01)
        assertEquals(3.45, decodedFull.peakAccelerationMetersPerSecondSquared!!, 0.01)

        val partialWithUnavailable = BlePacketDecoder.encodeRepTelemetryPacket(
            sequenceNumber = 2L,
            classification = RepClassification.PARTIAL,
            totalDurationMs = 1400L,
            concentricDurationMs = null,
            eccentricDurationMs = null,
            angularRomDegrees = null,
            linearRomCm = 24.5,
            peakAccelerationMetersPerSecondSquared = null,
        )
        val decodedPartial = decoder.decode(partialWithUnavailable) as DecodedBlePacket.RepTelemetry
        assertEquals(RepClassification.PARTIAL, decodedPartial.classification)
        assertNull(decodedPartial.concentricDurationMs)
        assertNull(decodedPartial.angularRomDegrees)
        assertNull(decodedPartial.peakAccelerationMetersPerSecondSquared)
        assertEquals(24.5, decodedPartial.linearRomCm!!, 0.01)
    }

    @Test
    fun decodeSensorSample_andBattery_validateFieldsAndRejectMalformedPackets() {
        val sampleBytes = BlePacketDecoder.encodeSensorSamplePacket(
            sequenceNumber = 10L,
            timestampOffsetMs = 120,
            accelXMg = 150,
            accelYMg = -80,
            accelZMg = 1250,
            gyroZDpsX10 = 425,
            distanceMm = 415,
        )
        val sample = decoder.decode(sampleBytes) as DecodedBlePacket.SensorSample
        assertEquals(10L, sample.sequenceNumber)
        assertEquals(120, sample.timestampOffsetMs)
        assertEquals(42.5, sample.gyroZDps, 0.01)
        assertEquals(41.5, sample.distanceCm!!, 0.01)

        val batteryBytes = BlePacketDecoder.encodeBatteryPacket(sequenceNumber = 11L, batteryPercent = 84)
        val battery = decoder.decode(batteryBytes) as DecodedBlePacket.BatteryStatus
        assertEquals(84, battery.batteryPercent)

        // Corrupt CRC
        val badCrc = sampleBytes.copyOf()
        badCrc[badCrc.lastIndex] = (badCrc.last().toInt() xor 0xFF).toByte()
        assertTrue(decoder.decode(badCrc) is DecodedBlePacket.Malformed)

        // Truncated
        assertTrue(decoder.decode(byteArrayOf(0x01, 0x01, 0x00)) is DecodedBlePacket.Malformed)

        // Battery > 100
        val badBattery = BlePacketDecoder.encodeBatteryPacket(sequenceNumber = 12L, batteryPercent = 120)
        assertTrue(decoder.decode(badBattery) is DecodedBlePacket.Malformed)
    }

    @Test
    fun realBleDataSource_unconfirmedProtocol_failsHonestlyWithProtocolNotConfigured() = runTest {
        val transport = FakeRawBleTransport()
        val dataSource = RealBleDeviceDataSource(
            transport = transport,
            initialConfig = BleProtocolConfig(isProtocolConfirmed = false),
            scope = backgroundScope,
        )
        val scanResult = dataSource.startScan()
        assertTrue(scanResult is DeviceOperationResult.Failure)
        assertEquals(
            DeviceOperationError.PROTOCOL_NOT_CONFIGURED,
            (scanResult as DeviceOperationResult.Failure).error,
        )
        assertEquals(DeviceConnectionState.FAILED, dataSource.connectionState.value.phase)
    }

    @Test
    fun realBleDataSource_rawTransportPipeline_decodesValidDuplicatesDroppedMalformedAndDisconnect() = runTest(
        UnconfinedTestDispatcher(),
    ) {
        val transport = FakeRawBleTransport()
        val config = BleProtocolConfig.proposedTestConfig()
        val dataSource = RealBleDeviceDataSource(
            transport = transport,
            initialConfig = config,
            scope = backgroundScope,
            timeProviderMs = { 10_000L },
        )
        dataSource.setActiveTrackingWindow("win-1")

        val emittedEvents = mutableListOf<DeviceEvent>()
        val collectJob = backgroundScope.launch {
            dataSource.events.collect { emittedEvents.add(it) }
        }

        assertEquals(DeviceOperationResult.Success, dataSource.startScan())
        assertEquals(DeviceConnectionState.SCANNING, dataSource.connectionState.value.phase)

        val device = DeviceDescriptor(
            id = "AA:BB:CC:DD:EE:01",
            name = "PULSE-POD-01",
            profileLabel = "BLE GATT",
            signalLabel = "-58 dBm",
            providerType = DeviceProviderType.REAL_BLE,
            isSimulated = false,
        )
        transport.emit(BleTransportEvent.DeviceDiscovered(device))
        assertEquals(1, dataSource.discoveredDevices.value.size)

        assertEquals(DeviceOperationResult.Success, dataSource.connect(device.id))
        transport.emit(BleTransportEvent.GattConnected(device.id, device.name))
        transport.emit(BleTransportEvent.ServicesDiscovered(listOf(config.serviceUuid!!)))
        transport.emit(BleTransportEvent.NotificationsSubscribed(config.telemetryCharacteristicUuid!!))
        assertEquals(DeviceConnectionState.CONNECTED, dataSource.connectionState.value.phase)

        // 1. Valid rep seq 1
        val rep1 = BlePacketDecoder.encodeRepTelemetryPacket(
            sequenceNumber = 1L,
            classification = RepClassification.FULL,
            totalDurationMs = 2000L,
            linearRomCm = 42.0,
            peakAccelerationMetersPerSecondSquared = 3.2,
        )
        transport.emit(BleTransportEvent.NotificationReceived(config.telemetryCharacteristicUuid!!, rep1))

        // 2. Duplicate seq 1 (must NOT emit duplicate rep event)
        transport.emit(BleTransportEvent.NotificationReceived(config.telemetryCharacteristicUuid!!, rep1))

        // 3. Seq 3 (skips seq 2 -> droppedPacketCount = 1)
        val rep3 = BlePacketDecoder.encodeRepTelemetryPacket(
            sequenceNumber = 3L,
            classification = RepClassification.PARTIAL,
            totalDurationMs = 1500L,
            linearRomCm = 25.0,
        )
        transport.emit(BleTransportEvent.NotificationReceived(config.telemetryCharacteristicUuid!!, rep3))

        // 4. Malformed packet
        transport.emit(BleTransportEvent.NotificationReceived(config.telemetryCharacteristicUuid!!, byteArrayOf(1, 1, 2)))

        // 5. Battery packet
        val bat = BlePacketDecoder.encodeBatteryPacket(sequenceNumber = 4L, batteryPercent = 91)
        transport.emit(BleTransportEvent.NotificationReceived(config.telemetryCharacteristicUuid!!, bat))

        val repEvents = emittedEvents.filterIsInstance<DeviceEvent.RepRecorded>()
        assertEquals(2, repEvents.size)
        assertEquals(MeasurementOrigin.SENSOR_TELEMETRY, repEvents[0].rep.origin)
        assertEquals(DeviceProviderType.REAL_BLE, repEvents[0].rep.providerType)
        assertEquals("win-1", repEvents[0].rep.trackingWindowId)
        assertEquals(91, dataSource.connectionState.value.batteryPercent)

        val diag = dataSource.diagnostics.value
        assertEquals(5L, diag.incomingPacketCount)
        assertEquals(1L, diag.duplicatePacketCount)
        assertEquals(1L, diag.droppedPacketCount)
        assertEquals(1L, diag.invalidPacketCount)
        assertEquals(2, diag.detectedRepCount)

        // 6. Unexpected disconnect -> CONNECTION_LOST without phantom reps
        transport.emit(BleTransportEvent.Disconnected(status = 8, unexpected = true))
        assertEquals(DeviceConnectionState.CONNECTION_LOST, dataSource.connectionState.value.phase)
        assertEquals(2, emittedEvents.filterIsInstance<DeviceEvent.RepRecorded>().size)

        collectJob.cancel()
    }
}

private class FakeRawBleTransport : BleTransport {
    private val _events = MutableSharedFlow<BleTransportEvent>(extraBufferCapacity = 64)
    override val events: Flow<BleTransportEvent> = _events.asSharedFlow()

    var availabilityResult: DeviceOperationResult = DeviceOperationResult.Success

    suspend fun emit(event: BleTransportEvent) {
        _events.emit(event)
    }

    override fun checkAvailabilityAndPermissions(): DeviceOperationResult = availabilityResult

    override suspend fun startScan(config: BleProtocolConfig): DeviceOperationResult = availabilityResult

    override suspend fun stopScan(): DeviceOperationResult = DeviceOperationResult.Success

    override suspend fun connect(deviceId: String, config: BleProtocolConfig): DeviceOperationResult = availabilityResult

    override suspend fun disconnect(): DeviceOperationResult = DeviceOperationResult.Success

    override suspend fun writeCommand(characteristicUuid: String, payload: ByteArray): Boolean = true

    override fun close() = Unit
}
