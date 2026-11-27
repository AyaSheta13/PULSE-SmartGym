package com.ayasheta.pulsesmartgym.core.model.history

import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession

data class SessionPerformanceSummary(
    val totalRecordedReps: Int,
    val measuredRepCount: Int,
    val fullRepCount: Int,
    val partialRepCount: Int,
    val averageRepDurationMs: Long?,
    val maxPeakAccelerationMps2: Double?,
    val averageAngularRomDegrees: Double?,
    val averageLinearRomCm: Double?,
) {
    val hasAnyMeasurements: Boolean
        get() = measuredRepCount > 0 && (
            averageRepDurationMs != null ||
                maxPeakAccelerationMps2 != null ||
                averageAngularRomDegrees != null ||
                averageLinearRomCm != null
            )
}

data class ExercisePerformanceTrendPoint(
    val sessionId: String,
    val timestampEpochMs: Long,
    val averageAngularRomDegrees: Double?,
    val averageLinearRomCm: Double?,
    val averageRepDurationMs: Long?,
    val maxPeakAccelerationMps2: Double?,
    val fullRepCount: Int,
    val partialRepCount: Int,
    val measuredRepCount: Int,
)

data class ExercisePerformanceTrendSummary(
    val exerciseKey: String,
    val exerciseName: String,
    val points: List<ExercisePerformanceTrendPoint>,
    val averageAngularRomDegrees: Double?,
    val averageLinearRomCm: Double?,
    val averageRepDurationMs: Long?,
    val maxPeakAccelerationMps2: Double?,
    val totalFullReps: Int,
    val totalPartialReps: Int,
    val totalMeasuredReps: Int,
)

object WorkoutPerformanceAnalytics {

    fun summarizeSession(session: WorkoutSession): SessionPerformanceSummary {
        val sets = session.exercises.flatMap { it.performedSets }
        val totalRecordedReps = sets.sumOf { it.fullReps + it.partialReps }
        val allReps = sets.flatMap { it.repRecords }
        val sensorOrMeasuredReps = allReps.filter { rep ->
            rep.totalDurationMs != null ||
                rep.angularRomDegrees != null ||
                rep.linearRomCm != null ||
                rep.peakAccelerationMetersPerSecondSquared != null
        }

        val durations = allReps.mapNotNull { it.totalDurationMs?.takeIf { d -> d >= 0L } }
        val accels = allReps.mapNotNull {
            it.peakAccelerationMetersPerSecondSquared?.takeIf { a -> a.isFinite() && a >= 0.0 }
        }
        val angularRoms = allReps.mapNotNull {
            it.angularRomDegrees?.takeIf { r -> r.isFinite() && r >= 0.0 }
        }
        val linearRoms = allReps.mapNotNull {
            it.linearRomCm?.takeIf { r -> r.isFinite() && r >= 0.0 }
        }

        return SessionPerformanceSummary(
            totalRecordedReps = totalRecordedReps,
            measuredRepCount = sensorOrMeasuredReps.size,
            fullRepCount = sets.sumOf { it.fullReps },
            partialRepCount = sets.sumOf { it.partialReps },
            averageRepDurationMs = if (durations.isEmpty()) null else durations.sum() / durations.size,
            maxPeakAccelerationMps2 = accels.maxOrNull(),
            averageAngularRomDegrees = if (angularRoms.isEmpty()) null else angularRoms.average(),
            averageLinearRomCm = if (linearRoms.isEmpty()) null else linearRoms.average(),
        )
    }

    /**
     * Computes genuine historical performance trends for a specific exercise.
     * Excludes ACTIVE, ABANDONED, and SIMULATED_TELEMETRY records.
     * Never substitutes missing values with zero and never averages angular & linear ROM together.
     */
    fun calculateExerciseTrends(
        sessions: List<WorkoutSession>,
        exerciseKey: String,
        includeSimulated: Boolean = false,
    ): ExercisePerformanceTrendSummary? {
        val eligibleSessions = sessions
            .filter { it.status == SessionStatus.COMPLETED || it.status == SessionStatus.PARTIALLY_COMPLETED }
            .sortedBy { it.endedAtEpochMs ?: it.startedAtEpochMs }

        val points = mutableListOf<ExercisePerformanceTrendPoint>()
        var resolvedName: String? = null

        for (session in eligibleSessions) {
            val matchingExercises = session.exercises.filter {
                (it.sourceExerciseId ?: it.exerciseNameSnapshot.lowercase()) == exerciseKey
            }
            if (matchingExercises.isEmpty()) continue
            if (resolvedName == null) resolvedName = matchingExercises.first().exerciseNameSnapshot

            val genuineSets: List<PerformedSet> = matchingExercises.flatMap { it.performedSets }.filter { set ->
                includeSimulated || set.origin != MeasurementOrigin.SIMULATED_TELEMETRY
            }
            if (genuineSets.isEmpty()) continue

            val validReps: List<RepMeasurement> = genuineSets.flatMap { it.repRecords }.filter { rep ->
                includeSimulated || rep.origin != MeasurementOrigin.SIMULATED_TELEMETRY
            }

            val durations = validReps.mapNotNull { it.totalDurationMs?.takeIf { d -> d >= 0L } }
            val accels = validReps.mapNotNull {
                it.peakAccelerationMetersPerSecondSquared?.takeIf { a -> a.isFinite() && a >= 0.0 }
            }
            val angular = validReps.mapNotNull {
                it.angularRomDegrees?.takeIf { r -> r.isFinite() && r >= 0.0 }
            }
            val linear = validReps.mapNotNull {
                it.linearRomCm?.takeIf { r -> r.isFinite() && r >= 0.0 }
            }
            val measuredCount = validReps.count { rep ->
                rep.totalDurationMs != null ||
                    rep.angularRomDegrees != null ||
                    rep.linearRomCm != null ||
                    rep.peakAccelerationMetersPerSecondSquared != null
            }

            points.add(
                ExercisePerformanceTrendPoint(
                    sessionId = session.id,
                    timestampEpochMs = session.endedAtEpochMs ?: session.startedAtEpochMs,
                    averageAngularRomDegrees = if (angular.isEmpty()) null else angular.average(),
                    averageLinearRomCm = if (linear.isEmpty()) null else linear.average(),
                    averageRepDurationMs = if (durations.isEmpty()) null else durations.sum() / durations.size,
                    maxPeakAccelerationMps2 = accels.maxOrNull(),
                    fullRepCount = genuineSets.sumOf { it.fullReps },
                    partialRepCount = genuineSets.sumOf { it.partialReps },
                    measuredRepCount = measuredCount,
                ),
            )
        }

        if (points.isEmpty()) return null

        val allAngular = points.mapNotNull { it.averageAngularRomDegrees }
        val allLinear = points.mapNotNull { it.averageLinearRomCm }
        val allDur = points.mapNotNull { it.averageRepDurationMs }
        val allAccel = points.mapNotNull { it.maxPeakAccelerationMps2 }

        return ExercisePerformanceTrendSummary(
            exerciseKey = exerciseKey,
            exerciseName = resolvedName ?: exerciseKey,
            points = points,
            averageAngularRomDegrees = if (allAngular.isEmpty()) null else allAngular.average(),
            averageLinearRomCm = if (allLinear.isEmpty()) null else allLinear.average(),
            averageRepDurationMs = if (allDur.isEmpty()) null else allDur.sum() / allDur.size,
            maxPeakAccelerationMps2 = allAccel.maxOrNull(),
            totalFullReps = points.sumOf { it.fullRepCount },
            totalPartialReps = points.sumOf { it.partialRepCount },
            totalMeasuredReps = points.sumOf { it.measuredRepCount },
        )
    }
}
