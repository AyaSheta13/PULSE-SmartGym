package com.ayasheta.pulsesmartgym.feature

import com.ayasheta.pulsesmartgym.core.data.device.DeviceEventNormalizer
import com.ayasheta.pulsesmartgym.core.data.device.NormalizedRepResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.DeviceCapabilities
import com.ayasheta.pulsesmartgym.core.model.device.DeviceProviderType
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepEvent
import com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.feature.session.counter.SetDraftSavedStateCodec
import com.ayasheta.pulsesmartgym.feature.session.counter.SetExecutionDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class P11DeviceAndMetricsUnitTest {

    @Test
    fun normalizer_validatesPeakAcceleration_andDraftCodecRoundTripsIt() {
        val caps = DeviceCapabilities(
            automaticRepEvents = true,
            fullPartialClassification = true,
            angularRom = true,
            linearRom = true,
            repDuration = true,
            peakAcceleration = true,
        )

        val invalidNan = DeviceRepEvent(
            eventId = "ev_nan",
            providerType = DeviceProviderType.SIMULATED,
            connectionGeneration = 1L,
            trackingWindowId = "w1",
            classification = RepClassification.FULL,
            peakAccelerationMetersPerSecondSquared = Double.NaN,
            origin = MeasurementOrigin.SIMULATED_TELEMETRY,
        )
        assertTrue(
            DeviceEventNormalizer.normalizeAndValidate(
                event = invalidNan,
                expectedProviderType = DeviceProviderType.SIMULATED,
                expectedConnectionGeneration = 1L,
                expectedTrackingWindowId = "w1",
                isConnected = true,
                isSubmitting = false,
                capabilities = caps,
                seenEventIds = emptySet(),
                nextRepIndex = 1,
                expectedEnvironment = ExecutionEnvironmentKind.DEMO,
            ) is NormalizedRepResult.Rejected,
        )

        val valid = DeviceRepEvent(
            eventId = "ev_ok",
            providerType = DeviceProviderType.SIMULATED,
            connectionGeneration = 1L,
            trackingWindowId = "w1",
            classification = RepClassification.FULL,
            totalDurationMs = 1800L,
            angularRomDegrees = 82.5,
            linearRomCm = 41.0,
            peakAccelerationMetersPerSecondSquared = 2.75,
            origin = MeasurementOrigin.SIMULATED_TELEMETRY,
        )
        val accepted = DeviceEventNormalizer.normalizeAndValidate(
            event = valid,
            expectedProviderType = DeviceProviderType.SIMULATED,
            expectedConnectionGeneration = 1L,
            expectedTrackingWindowId = "w1",
            isConnected = true,
            isSubmitting = false,
            capabilities = caps,
            seenEventIds = emptySet(),
            nextRepIndex = 1,
            expectedEnvironment = ExecutionEnvironmentKind.DEMO,
        ) as NormalizedRepResult.Accepted

        assertEquals(2.75, accepted.measurement.peakAccelerationMetersPerSecondSquared!!, 0.001)

        val draft = SetExecutionDraft(
            ownerId = "o1",
            environment = ExecutionEnvironmentKind.DEMO,
            sessionId = "s1",
            exerciseId = "e1",
            setIndex = 1,
            trackingWindowId = "s1_e1_1",
            fullReps = 1,
            partialReps = 0,
            repRecords = listOf(accepted.measurement),
            acceptedEventIds = setOf("ev_ok"),
            origin = MeasurementOrigin.SIMULATED_TELEMETRY,
            actualLoad = WeightLoad(80_000L),
            hasInitializedLoad = true,
            setStartedAtElapsedMs = 1_000L,
            trackingMode = TrackingMode.DEVICE_ASSISTED,
            hadDeviceTracking = true,
            isManualFallback = false,
        )

        val encoded = SetDraftSavedStateCodec.encode(draft)
        val decoded = SetDraftSavedStateCodec.decodeAndValidate(
            raw = encoded,
            expectedOwnerId = "o1",
            expectedEnvironment = ExecutionEnvironmentKind.DEMO,
            expectedSessionId = "s1",
            expectedExerciseId = "e1",
            expectedSetIndex = 1,
            currentElapsedMs = 5_000L,
        )
        assertNotNull(decoded)
        assertEquals(2.75, decoded!!.repRecords.first().peakAccelerationMetersPerSecondSquared!!, 0.001)
    }
}
