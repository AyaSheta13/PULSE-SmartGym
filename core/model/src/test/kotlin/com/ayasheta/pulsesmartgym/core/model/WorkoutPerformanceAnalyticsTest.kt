package com.ayasheta.pulsesmartgym.core.model

import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.history.WorkoutPerformanceAnalytics
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionExercise
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutPerformanceAnalyticsTest {

    @Test
    fun summarizeSession_keepsAngularAndLinearRomSeparate_andExcludesMissingValues() {
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
                            fullReps = 2,
                            partialReps = 1,
                            actualLoad = WeightLoad(80_000L),
                            completedAtEpochMs = 50_000L,
                            trackingMode = TrackingMode.DEVICE_ASSISTED,
                            origin = MeasurementOrigin.SENSOR_TELEMETRY,
                            repRecords = listOf(
                                RepMeasurement(1, RepClassification.FULL, 1500L, angularRomDegrees = 80.0, linearRomCm = 40.0, peakAccelerationMetersPerSecondSquared = 2.4, origin = MeasurementOrigin.SENSOR_TELEMETRY),
                                RepMeasurement(2, RepClassification.FULL, 1700L, angularRomDegrees = null, linearRomCm = 44.0, peakAccelerationMetersPerSecondSquared = 2.8, origin = MeasurementOrigin.SENSOR_TELEMETRY),
                                RepMeasurement(3, RepClassification.PARTIAL, null, angularRomDegrees = 60.0, linearRomCm = null, peakAccelerationMetersPerSecondSquared = null, origin = MeasurementOrigin.SENSOR_TELEMETRY),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val summary = WorkoutPerformanceAnalytics.summarizeSession(session)
        assertEquals(3, summary.measuredRepCount)
        assertEquals(1600L, summary.averageRepDurationMs)
        assertEquals(70.0, summary.averageAngularRomDegrees!!, 0.001)
        assertEquals(42.0, summary.averageLinearRomCm!!, 0.001)
        assertEquals(2.8, summary.maxPeakAccelerationMps2!!, 0.001)
    }

    @Test
    fun calculateExerciseTrends_excludesSimulatedAndActiveSessions() {
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

        val trend = WorkoutPerformanceAnalytics.calculateExerciseTrends(
            sessions = listOf(genuine, simulated, active),
            exerciseKey = "ex_bench_press",
            includeSimulated = false,
        )!!

        assertEquals(1, trend.points.size)
        assertEquals(42.0, trend.averageLinearRomCm!!, 0.001)
        assertEquals(2.5, trend.maxPeakAccelerationMps2!!, 0.001)
        assertNull(trend.averageAngularRomDegrees)
    }
}
