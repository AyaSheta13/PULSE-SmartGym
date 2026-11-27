package com.ayasheta.pulsesmartgym.core.model

import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.DeviceCapabilities
import com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseTrackingCatalog
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.history.MeasurementCompatibilityStatus
import com.ayasheta.pulsesmartgym.core.model.history.WorkoutPerformanceAnalytics
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionExercise
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class WorkoutPerformanceAnalyticsTest {

    @Test
    fun summarizeSession_gatesUnsupportedDimensions_preservesValidZero_andExcludesInvalidValues() {
        // ex_bench_press is LINEAR-only: angularRomDegrees must be excluded from bench aggregates
        val session = WorkoutSession(
            id = "s1",
            ownerProfileId = "o1",
            sourcePlanId = "p1",
            planNameSnapshot = "Upper",
            status = SessionStatus.COMPLETED,
            startedAtEpochMs = 1_000L,
            endedAtEpochMs = 60_000L,
            exercises = listOf(
                SessionExercise(
                    id = "se1",
                    sessionId = "s1",
                    sourceExerciseId = "ex_bench_press",
                    exerciseNameSnapshot = "Barbell Bench Press",
                    muscleGroupSnapshot = MuscleGroup.CHEST,
                    equipmentSnapshot = EquipmentCategory.BARBELL,
                    trackingCategorySnapshot = TrackingCategory.LINEAR,
                    orderIndex = 0,
                    plannedSets = emptyList(),
                    performedSets = listOf(
                        PerformedSet(
                            id = "ps1",
                            sessionId = "s1",
                            sessionExerciseId = "se1",
                            setIndex = 1,
                            fullReps = 3,
                            partialReps = 1,
                            actualLoad = WeightLoad(80_000L),
                            completedAtEpochMs = 50_000L,
                            trackingMode = TrackingMode.DEVICE_ASSISTED,
                            origin = MeasurementOrigin.SENSOR_TELEMETRY,
                            repRecords = listOf(
                                RepMeasurement(1, RepClassification.FULL, 1500L, angularRomDegrees = 80.0, linearRomCm = 40.0, peakAccelerationMetersPerSecondSquared = 2.4, origin = MeasurementOrigin.SENSOR_TELEMETRY),
                                RepMeasurement(2, RepClassification.FULL, 1700L, angularRomDegrees = null, linearRomCm = 44.0, peakAccelerationMetersPerSecondSquared = 2.8, origin = MeasurementOrigin.SENSOR_TELEMETRY),
                                // Valid zero linear ROM & zero duration must be preserved
                                RepMeasurement(3, RepClassification.PARTIAL, 0L, angularRomDegrees = null, linearRomCm = 0.0, peakAccelerationMetersPerSecondSquared = 0.0, origin = MeasurementOrigin.SENSOR_TELEMETRY),
                                // Negative & NaN values must be excluded
                                RepMeasurement(4, RepClassification.FULL, -50L, angularRomDegrees = null, linearRomCm = Double.NaN, peakAccelerationMetersPerSecondSquared = -1.0, origin = MeasurementOrigin.SENSOR_TELEMETRY),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val summary = WorkoutPerformanceAnalytics.summarizeSession(session)
        assertEquals(3, summary.measuredRepCount)
        assertEquals((1500L + 1700L + 0L) / 3L, summary.averageRepDurationMs)
        assertNull(summary.averageAngularRomDegrees)
        assertEquals((40.0 + 44.0 + 0.0) / 3.0, summary.averageLinearRomCm!!, 0.001)
        assertEquals(2.8, summary.maxPeakAccelerationMps2!!, 0.001)
    }

    @Test
    fun mixedSensorAndManualLedger_countsProvenancePerIndividualRecord() {
        val mixedReps = (1..7).map { idx ->
            RepMeasurement(idx, RepClassification.FULL, 1600L, linearRomCm = 42.0, origin = MeasurementOrigin.SENSOR_TELEMETRY)
        } + listOf(
            RepMeasurement(8, RepClassification.PARTIAL, 1200L, linearRomCm = 28.0, origin = MeasurementOrigin.SENSOR_TELEMETRY),
            RepMeasurement(9, RepClassification.FULL, origin = MeasurementOrigin.MANUAL_CORRECTION),
            RepMeasurement(10, RepClassification.FULL, origin = MeasurementOrigin.MANUAL_INPUT),
        )

        val session = WorkoutSession(
            id = "s_mix",
            ownerProfileId = "o1",
            sourcePlanId = "p1",
            planNameSnapshot = "Push",
            status = SessionStatus.COMPLETED,
            startedAtEpochMs = 1_000L,
            endedAtEpochMs = 60_000L,
            exercises = listOf(
                SessionExercise(
                    id = "se1",
                    sessionId = "s_mix",
                    sourceExerciseId = "ex_bench_press",
                    exerciseNameSnapshot = "Barbell Bench Press",
                    muscleGroupSnapshot = MuscleGroup.CHEST,
                    equipmentSnapshot = EquipmentCategory.BARBELL,
                    trackingCategorySnapshot = TrackingCategory.LINEAR,
                    orderIndex = 0,
                    plannedSets = emptyList(),
                    performedSets = listOf(
                        PerformedSet(
                            id = "ps1",
                            sessionId = "s_mix",
                            sessionExerciseId = "se1",
                            setIndex = 1,
                            fullReps = 9,
                            partialReps = 1,
                            actualLoad = WeightLoad(80_000L),
                            completedAtEpochMs = 50_000L,
                            trackingMode = TrackingMode.MANUAL,
                            origin = MeasurementOrigin.MANUAL_CORRECTION,
                            repRecords = mixedReps,
                        ),
                        // Pure manual aggregate set
                        PerformedSet(
                            id = "ps2",
                            sessionId = "s_mix",
                            sessionExerciseId = "se1",
                            setIndex = 2,
                            fullReps = 5,
                            partialReps = 0,
                            actualLoad = WeightLoad(80_000L),
                            completedAtEpochMs = 55_000L,
                            trackingMode = TrackingMode.MANUAL,
                            origin = MeasurementOrigin.MANUAL_INPUT,
                            repRecords = emptyList(),
                        ),
                    ),
                ),
            ),
        )

        val summary = WorkoutPerformanceAnalytics.summarizeSession(session)
        assertEquals(15, summary.totalRecordedReps)
        assertEquals(7, summary.sensorClassifiedFullRepCount)
        assertEquals(1, summary.sensorClassifiedPartialRepCount)
        assertEquals(7, summary.manualRepCount) // 2 from mixed ledger + 5 from pure manual set
        assertEquals(8, summary.measuredRepCount)
    }

    @Test
    fun calculateExerciseTrends_excludesSimulatedAndActiveSessions_andRespectsCalibrationCompatibility() {
        val genuine = WorkoutSession(
            id = "s_gen",
            ownerProfileId = "o1",
            sourcePlanId = "p1",
            planNameSnapshot = "Upper",
            status = SessionStatus.COMPLETED,
            startedAtEpochMs = 1_000L,
            endedAtEpochMs = 2_000L,
            exercises = listOf(
                SessionExercise(
                    id = "se1",
                    sessionId = "s_gen",
                    sourceExerciseId = "ex_bench_press",
                    exerciseNameSnapshot = "Barbell Bench Press",
                    muscleGroupSnapshot = MuscleGroup.CHEST,
                    equipmentSnapshot = EquipmentCategory.BARBELL,
                    trackingCategorySnapshot = TrackingCategory.LINEAR,
                    orderIndex = 0,
                    plannedSets = emptyList(),
                    performedSets = listOf(
                        PerformedSet(
                            id = "ps1",
                            sessionId = "s_gen",
                            sessionExerciseId = "se1",
                            setIndex = 1,
                            fullReps = 1,
                            partialReps = 0,
                            actualLoad = WeightLoad(80_000L),
                            completedAtEpochMs = 2_000L,
                            trackingMode = TrackingMode.DEVICE_ASSISTED,
                            origin = MeasurementOrigin.SENSOR_TELEMETRY,
                            repRecords = listOf(
                                RepMeasurement(1, RepClassification.FULL, 1800L, linearRomCm = 42.0, peakAccelerationMetersPerSecondSquared = 2.5, origin = MeasurementOrigin.SENSOR_TELEMETRY),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val simulated = genuine.copy(
            id = "s_sim",
            exercises = listOf(
                genuine.exercises.first().copy(
                    id = "se_sim",
                    sessionId = "s_sim",
                    performedSets = listOf(
                        genuine.exercises.first().performedSets.first().copy(
                            id = "ps_sim",
                            sessionId = "s_sim",
                            origin = MeasurementOrigin.SIMULATED_TELEMETRY,
                            repRecords = listOf(
                                RepMeasurement(1, RepClassification.FULL, 900L, linearRomCm = 99.0, peakAccelerationMetersPerSecondSquared = 9.9, origin = MeasurementOrigin.SIMULATED_TELEMETRY),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val active = genuine.copy(id = "s_act", status = SessionStatus.ACTIVE)

        // Without calibrationVersion, physical SENSOR_TELEMETRY is UNCONFIGURED -> comparable trend unavailable
        val uncalibrated = WorkoutPerformanceAnalytics.calculateExerciseTrends(
            sessions = listOf(genuine, simulated, active),
            exerciseKey = "ex_bench_press",
            includeSimulated = false,
            calibrationVersion = null,
        )!!
        assertEquals(MeasurementCompatibilityStatus.UNCONFIGURED, uncalibrated.configurationCompatibility)
        assertFalse(uncalibrated.isComparableTrendAvailable)
        assertNull(uncalibrated.averageLinearRomCm)

        // With explicit calibrationVersion, comparable trend is computed and simulated is excluded
        val calibrated = WorkoutPerformanceAnalytics.calculateExerciseTrends(
            sessions = listOf(genuine, simulated, active),
            exerciseKey = "ex_bench_press",
            includeSimulated = false,
            calibrationVersion = "cal-1",
        )!!
        assertEquals(1, calibrated.points.size)
        assertTrue(calibrated.isComparableTrendAvailable)
        assertEquals(42.0, calibrated.averageLinearRomCm!!, 0.001)
        assertEquals(2.5, calibrated.maxPeakAccelerationMps2!!, 0.001)
        assertNull(calibrated.averageAngularRomDegrees)
    }

    @Test
    fun calculateExerciseTrends_usesRepetitionWeightedAverages_andSessionStartPeriodAttribution() {
        val zone = ZoneId.of("America/New_York")
        fun epoch(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
            ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli()

        fun makeSession(id: String, startMs: Long, endMs: Long, repCount: Int, linearCm: Double): WorkoutSession {
            val reps = (1..repCount).map { idx ->
                RepMeasurement(
                    repIndex = idx,
                    classification = RepClassification.FULL,
                    totalDurationMs = 1500L,
                    linearRomCm = linearCm,
                    origin = MeasurementOrigin.SIMULATED_TELEMETRY,
                )
            }
            return WorkoutSession(
                id = id,
                ownerProfileId = "o1",
                sourcePlanId = "p1",
                planNameSnapshot = "Upper",
                status = SessionStatus.COMPLETED,
                startedAtEpochMs = startMs,
                endedAtEpochMs = endMs,
                exercises = listOf(
                    SessionExercise(
                        id = "se_$id",
                        sessionId = id,
                        sourceExerciseId = "ex_bench_press",
                        exerciseNameSnapshot = "Barbell Bench Press",
                        muscleGroupSnapshot = MuscleGroup.CHEST,
                        equipmentSnapshot = EquipmentCategory.BARBELL,
                        trackingCategorySnapshot = TrackingCategory.LINEAR,
                        orderIndex = 0,
                        plannedSets = emptyList(),
                        performedSets = listOf(
                            PerformedSet(
                                id = "ps_$id",
                                sessionId = id,
                                sessionExerciseId = "se_$id",
                                setIndex = 1,
                                fullReps = repCount,
                                partialReps = 0,
                                actualLoad = WeightLoad(80_000L),
                                completedAtEpochMs = endMs,
                                trackingMode = TrackingMode.DEVICE_ASSISTED,
                                origin = MeasurementOrigin.SIMULATED_TELEMETRY,
                                repRecords = reps,
                            ),
                        ),
                    ),
                ),
            )
        }

        // Sunday Dec 28 2025 23:50 -> Monday Dec 29 2025 00:15 (must belong to Sunday's week!)
        val sundayNight = makeSession("s_sun", epoch(2025, 12, 28, 23, 50), epoch(2025, 12, 29, 0, 15), 10, 40.0)
        // Wednesday Dec 31 2025 23:45 -> Thursday Jan 1 2026 00:15 (next week, crosses year boundary)
        val newYearsEve = makeSession("s_nye", epoch(2025, 12, 31, 23, 45), epoch(2026, 1, 1, 0, 15), 2, 20.0)

        val prevWeekStart = LocalDate.of(2025, 12, 22).atStartOfDay(zone).toInstant().toEpochMilli()
        val currWeekStart = LocalDate.of(2025, 12, 29).atStartOfDay(zone).toInstant().toEpochMilli()
        val nextWeekStart = LocalDate.of(2026, 1, 5).atStartOfDay(zone).toInstant().toEpochMilli()

        val prevWeekTrend = WorkoutPerformanceAnalytics.calculateExerciseTrends(
            sessions = listOf(sundayNight, newYearsEve),
            exerciseKey = "ex_bench_press",
            includeSimulated = true,
            windowStartEpochMs = prevWeekStart,
            windowEndExclusiveEpochMs = currWeekStart,
        )!!
        assertEquals(1, prevWeekTrend.points.size)
        assertEquals("s_sun", prevWeekTrend.points.first().sessionId)

        val currWeekTrend = WorkoutPerformanceAnalytics.calculateExerciseTrends(
            sessions = listOf(sundayNight, newYearsEve),
            exerciseKey = "ex_bench_press",
            includeSimulated = true,
            windowStartEpochMs = currWeekStart,
            windowEndExclusiveEpochMs = nextWeekStart,
        )!!
        assertEquals(1, currWeekTrend.points.size)
        assertEquals("s_nye", currWeekTrend.points.first().sessionId)

        // Combined across both weeks: 10*40 + 2*20 = 440 / 12 = 36.6667 cm (NOT 30 cm)
        val combined = WorkoutPerformanceAnalytics.calculateExerciseTrends(
            sessions = listOf(sundayNight, newYearsEve),
            exerciseKey = "ex_bench_press",
            includeSimulated = true,
        )!!
        assertEquals(36.6667, combined.averageLinearRomCm!!, 0.001)
        assertEquals(12, combined.linearRomSampleCount)
    }

    @Test
    fun unmappedHistoricalExercises_areNotMergedSolelyByName() {
        val ex1 = SessionExercise("se1", "s1", null, "Custom Press", MuscleGroup.CHEST, EquipmentCategory.OTHER, TrackingCategory.COMPOUND, 0, emptyList(), emptyList())
        val ex2 = SessionExercise("se2", "s2", null, "Custom Press", MuscleGroup.CHEST, EquipmentCategory.OTHER, TrackingCategory.COMPOUND, 0, emptyList(), emptyList())
        assertTrue(WorkoutPerformanceAnalytics.stableExerciseKey(ex1) != WorkoutPerformanceAnalytics.stableExerciseKey(ex2))
    }

    @Test
    fun effectiveCapabilityPolicy_gatesUnsupportedDimensionsAndUnconfiguredPhysicalTracking() {
        val deviceCaps = DeviceCapabilities(
            automaticRepEvents = true,
            fullPartialClassification = true,
            angularRom = true,
            linearRom = true,
            repDuration = true,
            peakAcceleration = true,
        )
        val linearConfig = ExerciseTrackingCatalog.forExercise("ex_bench_press", TrackingCategory.LINEAR)
        val demoEffective = ExerciseTrackingCatalog.resolveEffectiveCapabilities(
            deviceCapabilities = deviceCaps,
            exerciseConfig = linearConfig,
            environment = ExecutionEnvironmentKind.DEMO,
        )
        assertTrue(demoEffective.automaticRepEvents)
        assertTrue(demoEffective.linearRom)
        assertFalse(demoEffective.angularRom)

        val uncalibratedGenuine = ExerciseTrackingCatalog.resolveEffectiveCapabilities(
            deviceCapabilities = deviceCaps,
            exerciseConfig = linearConfig,
            environment = ExecutionEnvironmentKind.GENUINE,
            hasCalibration = false,
        )
        assertFalse(uncalibratedGenuine.automaticRepEvents)
    }

    @Test
    fun pureManualAndInvalidatedSets_neverInflateSensorClassifiedCounts() {
        val session = WorkoutSession(
            id = "s_pure",
            ownerProfileId = "o1",
            sourcePlanId = "p1",
            planNameSnapshot = "Push",
            status = SessionStatus.COMPLETED,
            startedAtEpochMs = 1_000L,
            endedAtEpochMs = 60_000L,
            exercises = listOf(
                SessionExercise(
                    id = "se1",
                    sessionId = "s_pure",
                    sourceExerciseId = "ex_bench_press",
                    exerciseNameSnapshot = "Barbell Bench Press",
                    muscleGroupSnapshot = MuscleGroup.CHEST,
                    equipmentSnapshot = EquipmentCategory.BARBELL,
                    trackingCategorySnapshot = TrackingCategory.LINEAR,
                    orderIndex = 0,
                    plannedSets = emptyList(),
                    performedSets = listOf(
                        // Pure manual aggregate set
                        PerformedSet(
                            id = "ps_manual",
                            sessionId = "s_pure",
                            sessionExerciseId = "se1",
                            setIndex = 1,
                            fullReps = 8,
                            partialReps = 2,
                            actualLoad = WeightLoad(60_000L),
                            completedAtEpochMs = 20_000L,
                            trackingMode = TrackingMode.MANUAL,
                            origin = MeasurementOrigin.MANUAL_INPUT,
                            repRecords = emptyList(),
                        ),
                        // Invalidated ledger after aggregate edit (repRecords cleared)
                        PerformedSet(
                            id = "ps_invalidated",
                            sessionId = "s_pure",
                            sessionExerciseId = "se1",
                            setIndex = 2,
                            fullReps = 6,
                            partialReps = 0,
                            actualLoad = WeightLoad(65_000L),
                            completedAtEpochMs = 40_000L,
                            trackingMode = TrackingMode.DEVICE_ASSISTED,
                            origin = MeasurementOrigin.MANUAL_CORRECTION,
                            repRecords = emptyList(),
                        ),
                    ),
                ),
            ),
        )

        val summary = WorkoutPerformanceAnalytics.summarizeSession(session)
        assertEquals(16, summary.totalRecordedReps)
        assertEquals(0, summary.sensorClassifiedFullRepCount)
        assertEquals(0, summary.sensorClassifiedPartialRepCount)
        assertEquals(16, summary.manualRepCount)
        assertEquals(0, summary.measuredRepCount)
        assertNull(summary.averageLinearRomCm)
    }
}
