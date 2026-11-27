package com.ayasheta.pulsesmartgym.core.data.device.ble

import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepEvent
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed interface DecodedBlePacket {
    data class RepTelemetry(
        val protocolVersion: Int,
        val sequenceNumber: Long,
        val classification: RepClassification,
        val totalDurationMs: Long?,
        val concentricDurationMs: Long?,
        val eccentricDurationMs: Long?,
        val angularRomDegrees: Double?,
        val linearRomCm: Double?,
        val peakAccelerationMetersPerSecondSquared: Double?,
        val rawHex: String,
    ) : DecodedBlePacket {
        fun toDeviceRepEvent(
            connectionGeneration: Long,
            trackingWindowId: String,
            origin: MeasurementOrigin = MeasurementOrigin.SENSOR_TELEMETRY,
        ): DeviceRepEvent = DeviceRepEvent(
            eventId = "ble-gen$connectionGeneration-seq$sequenceNumber",
            providerType = DeviceProviderType.REAL_BLE,
            connectionGeneration = connectionGeneration,
            trackingWindowId = trackingWindowId,
            sequenceNumber = sequenceNumber,
            classification = classification,
            totalDurationMs = totalDurationMs,
            concentricDurationMs = concentricDurationMs,
            eccentricDurationMs = eccentricDurationMs,
            angularRomDegrees = angularRomDegrees,
            linearRomCm = linearRomCm,
            peakAccelerationMetersPerSecondSquared = peakAccelerationMetersPerSecondSquared,
            origin = origin,
        )
    }

    data class SensorSample(
        val protocolVersion: Int,
        val sequenceNumber: Long,
        val timestampOffsetMs: Int,
        val accelXMs2: Double,
        val accelYMs2: Double,
        val accelZMs2: Double,
        val gyroZDps: Double,
        val distanceCm: Double?,
        val rawHex: String,
    ) : DecodedBlePacket

    data class BatteryStatus(
        val protocolVersion: Int,
        val sequenceNumber: Long,
        val batteryPercent: Int,
        val rawHex: String,
    ) : DecodedBlePacket

    data class Malformed(
        val reason: String,
        val rawHex: String,
    ) : DecodedBlePacket
}

/**
 * Pure-Kotlin BLE packet decoder implementing the proposed v1 PULSE binary framing.
 * All multi-byte fields are little-endian with trailing CRC-8 (poly 0x07).
 */
class BlePacketDecoder(
    private val expectedProtocolVersion: Int = 1,
) {
    fun decode(payload: ByteArray): DecodedBlePacket {
        val hex = payload.toHexString()
        if (payload.size < 4) {
            return DecodedBlePacket.Malformed("Packet too short (${payload.size} bytes)", hex)
        }
        val version = payload[0].toInt() and 0xFF
        if (version != expectedProtocolVersion) {
            return DecodedBlePacket.Malformed("Unsupported protocol version $version", hex)
        }
        val expectedCrc = crc8(payload, payload.size - 1)
        val actualCrc = payload.last().toInt() and 0xFF
        if (expectedCrc != actualCrc) {
            return DecodedBlePacket.Malformed(
                "CRC-8 mismatch (expected 0x${expectedCrc.toString(16)}, got 0x${actualCrc.toString(16)})",
                hex,
            )
        }

        val packetType = payload[1].toInt() and 0xFF
        return when (packetType) {
            TYPE_REP_TELEMETRY -> decodeRepTelemetry(payload, version, hex)
            TYPE_SENSOR_SAMPLE -> decodeSensorSample(payload, version, hex)
            TYPE_BATTERY_STATUS -> decodeBatteryStatus(payload, version, hex)
            else -> DecodedBlePacket.Malformed("Unknown packet type 0x${packetType.toString(16)}", hex)
        }
    }

    private fun decodeRepTelemetry(payload: ByteArray, version: Int, hex: String): DecodedBlePacket {
        if (payload.size != REP_PACKET_SIZE) {
            return DecodedBlePacket.Malformed(
                "Invalid rep telemetry length ${payload.size} (expected $REP_PACKET_SIZE)",
                hex,
            )
        }
        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        buf.position(2)
        val seq = buf.int.toLong() and 0xFFFF_FFFFL
        val flags = buf.get().toInt() and 0xFF
        val totalMsRaw = buf.short.toInt() and 0xFFFF
        val concMsRaw = buf.short.toInt() and 0xFFFF
        val eccMsRaw = buf.short.toInt() and 0xFFFF
        val angRomRaw = buf.short.toInt() and 0xFFFF
        val linRomRaw = buf.short.toInt() and 0xFFFF
        val peakAccelRaw = buf.short.toInt() and 0xFFFF

        val classification = if ((flags and 0x01) != 0) RepClassification.PARTIAL else RepClassification.FULL
        return DecodedBlePacket.RepTelemetry(
            protocolVersion = version,
            sequenceNumber = seq,
            classification = classification,
            totalDurationMs = totalMsRaw.takeIf { it != UINT16_UNAVAILABLE }?.toLong(),
            concentricDurationMs = concMsRaw.takeIf { it != UINT16_UNAVAILABLE }?.toLong(),
            eccentricDurationMs = eccMsRaw.takeIf { it != UINT16_UNAVAILABLE }?.toLong(),
            angularRomDegrees = angRomRaw.takeIf { it != UINT16_UNAVAILABLE }?.div(10.0),
            linearRomCm = linRomRaw.takeIf { it != UINT16_UNAVAILABLE }?.div(10.0),
            peakAccelerationMetersPerSecondSquared = peakAccelRaw.takeIf { it != UINT16_UNAVAILABLE }?.div(100.0),
            rawHex = hex,
        )
    }

    private fun decodeSensorSample(payload: ByteArray, version: Int, hex: String): DecodedBlePacket {
        if (payload.size != SAMPLE_PACKET_SIZE) {
            return DecodedBlePacket.Malformed(
                "Invalid sensor sample length ${payload.size} (expected $SAMPLE_PACKET_SIZE)",
                hex,
            )
        }
        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        buf.position(2)
        val seq = buf.int.toLong() and 0xFFFF_FFFFL
        val tsOffsetMs = buf.short.toInt() and 0xFFFF
        val axMg = buf.short.toInt()
        val ayMg = buf.short.toInt()
        val azMg = buf.short.toInt()
        val gzDpsX10 = buf.short.toInt()
        val distMmRaw = buf.short.toInt() and 0xFFFF

        return DecodedBlePacket.SensorSample(
            protocolVersion = version,
            sequenceNumber = seq,
            timestampOffsetMs = tsOffsetMs,
            accelXMs2 = (axMg / 1000.0) * GRAVITY_MS2,
            accelYMs2 = (ayMg / 1000.0) * GRAVITY_MS2,
            accelZMs2 = (azMg / 1000.0) * GRAVITY_MS2,
            gyroZDps = gzDpsX10 / 10.0,
            distanceCm = distMmRaw.takeIf { it != UINT16_UNAVAILABLE }?.div(10.0),
            rawHex = hex,
        )
    }

    private fun decodeBatteryStatus(payload: ByteArray, version: Int, hex: String): DecodedBlePacket {
        if (payload.size != BATTERY_PACKET_SIZE) {
            return DecodedBlePacket.Malformed(
                "Invalid battery packet length ${payload.size} (expected $BATTERY_PACKET_SIZE)",
                hex,
            )
        }
        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        buf.position(2)
        val seq = buf.int.toLong() and 0xFFFF_FFFFL
        val pct = buf.get().toInt() and 0xFF
        if (pct > 100) {
            return DecodedBlePacket.Malformed("Battery percent out of range: $pct", hex)
        }
        return DecodedBlePacket.BatteryStatus(
            protocolVersion = version,
            sequenceNumber = seq,
            batteryPercent = pct,
            rawHex = hex,
        )
    }

    companion object {
        const val TYPE_REP_TELEMETRY: Int = 0x01
        const val TYPE_SENSOR_SAMPLE: Int = 0x02
        const val TYPE_BATTERY_STATUS: Int = 0x03

        const val REP_PACKET_SIZE: Int = 22
        const val SAMPLE_PACKET_SIZE: Int = 22
        const val BATTERY_PACKET_SIZE: Int = 8
        const val UINT16_UNAVAILABLE: Int = 0xFFFF
        private const val GRAVITY_MS2: Double = 9.80665

        fun crc8(bytes: ByteArray, length: Int = bytes.size): Int {
            var crc = 0x00
            for (i in 0 until length) {
                crc = crc xor (bytes[i].toInt() and 0xFF)
                repeat(8) {
                    crc = if ((crc and 0x80) != 0) {
                        ((crc shl 1) xor 0x07) and 0xFF
                    } else {
                        (crc shl 1) and 0xFF
                    }
                }
            }
            return crc and 0xFF
        }

        fun encodeRepTelemetryPacket(
            sequenceNumber: Long,
            classification: RepClassification,
            totalDurationMs: Long? = null,
            concentricDurationMs: Long? = null,
            eccentricDurationMs: Long? = null,
            angularRomDegrees: Double? = null,
            linearRomCm: Double? = null,
            peakAccelerationMetersPerSecondSquared: Double? = null,
            protocolVersion: Int = 1,
        ): ByteArray {
            val buf = ByteBuffer.allocate(REP_PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)
            buf.put(protocolVersion.toByte())
            buf.put(TYPE_REP_TELEMETRY.toByte())
            buf.putInt(sequenceNumber.toInt())
            buf.put(if (classification == RepClassification.PARTIAL) 0x01.toByte() else 0x00.toByte())
            buf.putShort((totalDurationMs?.toInt() ?: UINT16_UNAVAILABLE).toShort())
            buf.putShort((concentricDurationMs?.toInt() ?: UINT16_UNAVAILABLE).toShort())
            buf.putShort((eccentricDurationMs?.toInt() ?: UINT16_UNAVAILABLE).toShort())
            buf.putShort((angularRomDegrees?.let { (it * 10.0).toInt() } ?: UINT16_UNAVAILABLE).toShort())
            buf.putShort((linearRomCm?.let { (it * 10.0).toInt() } ?: UINT16_UNAVAILABLE).toShort())
            buf.putShort((peakAccelerationMetersPerSecondSquared?.let { (it * 100.0).toInt() } ?: UINT16_UNAVAILABLE).toShort())
            buf.putShort(0.toShort())
            val bytes = buf.array()
            bytes[REP_PACKET_SIZE - 1] = crc8(bytes, REP_PACKET_SIZE - 1).toByte()
            return bytes
        }

        fun encodeSensorSamplePacket(
            sequenceNumber: Long,
            timestampOffsetMs: Int,
            accelXMg: Int,
            accelYMg: Int,
            accelZMg: Int,
            gyroZDpsX10: Int,
            distanceMm: Int? = null,
            protocolVersion: Int = 1,
        ): ByteArray {
            val buf = ByteBuffer.allocate(SAMPLE_PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)
            buf.put(protocolVersion.toByte())
            buf.put(TYPE_SENSOR_SAMPLE.toByte())
            buf.putInt(sequenceNumber.toInt())
            buf.putShort(timestampOffsetMs.toShort())
            buf.putShort(accelXMg.toShort())
            buf.putShort(accelYMg.toShort())
            buf.putShort(accelZMg.toShort())
            buf.putShort(gyroZDpsX10.toShort())
            buf.putShort((distanceMm ?: UINT16_UNAVAILABLE).toShort())
            buf.put(0.toByte()); buf.put(0.toByte()); buf.put(0.toByte())
            val bytes = buf.array()
            bytes[SAMPLE_PACKET_SIZE - 1] = crc8(bytes, SAMPLE_PACKET_SIZE - 1).toByte()
            return bytes
        }

        fun encodeBatteryPacket(
            sequenceNumber: Long,
            batteryPercent: Int,
            protocolVersion: Int = 1,
        ): ByteArray {
            val buf = ByteBuffer.allocate(BATTERY_PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)
            buf.put(protocolVersion.toByte())
            buf.put(TYPE_BATTERY_STATUS.toByte())
            buf.putInt(sequenceNumber.toInt())
            buf.put(batteryPercent.toByte())
            val bytes = buf.array()
            bytes[BATTERY_PACKET_SIZE - 1] = crc8(bytes, BATTERY_PACKET_SIZE - 1).toByte()
            return bytes
        }
    }
}

private fun ByteArray.toHexString(): String =
    joinToString(" ") { b -> "%02X".format(b.toInt() and 0xFF) }
