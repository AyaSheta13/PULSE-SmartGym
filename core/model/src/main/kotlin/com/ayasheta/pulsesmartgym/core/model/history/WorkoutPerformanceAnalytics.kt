package com.ayasheta.pulsesmartgym.core.model.history

import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseTrackingCatalog
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionExercise
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession

enum class MeasurementCompatibilityStatus {
    COMPATIBLE,
    UNCONFIGURED,
    INCOMPATIBLE,
}

data class ExercisePerformanceSummary(
    val exerciseKey: String,
    val exerciseName: String,
    val trackingCategory: TrackingCategory,
    val configurationCompatibility: MeasurementCompatibilityStatus,
    val calibrationVersion: String? = null,
    val totalRecordedReps: Int,
    val measuredRepCount: Int,
    val sensorClassifiedFullRepCount: Int,
    val sensorClassifiedPartialRepCount: Int,
    val manualRepCount: Int,
    val averageRepDurationMs: Long?,
    val durationSampleCount: Int,
    val maxPeakAccelerationMps2: Double?,
    val accelerationSampleCount: Int,
    val averageAngularRomDegrees: Double?,
    val angularRomSampleCount: Int,
    val averageLinearRomCm: Double?,
    val linearRomSampleCount: Int,
) {
    val hasAnyMeasurements: Boolean
        get() = measuredRepCount > 0 && (
            averageRepDurationMs != null ||
                maxPeakAccelerationMps2 != null ||
                averageAngularRomDegrees != null ||
                averageLinearRomCm != null
            )
}

data class SessionPerformanceSummary(
    val totalRecordedReps: Int,
    val measuredRepCount: Int,
    val fullRepCount: Int,
    val partialRepCount: Int,
    val sensorClassifiedFullRepCount: Int = 0,
    val sensorClassifiedPartialRepCount: Int = 0,
    val manualRepCount: Int = 0,
    val averageRepDurationMs: Long?,
    val maxPeakAccelerationMps2: Double?,
    val averageAngularRomDegrees: Double?,
    val averageLinearRomCm: Double?,
    val exerciseSummaries: List<ExercisePerformanceSummary> = emptyList(),
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
    val angularRomSampleCount: Int = 0,
    val averageLinearRomCm: Double?,
    val linearRomSampleCount: Int = 0,
    val averageRepDurationMs: Long?,
    val durationSampleCount: Int = 0,
    val maxPeakAccelerationMps2: Double?,
    val accelerationSampleCount: Int = 0,
    val fullRepCount: Int,
    val partialRepCount: Int,
    val sensorClassifiedFullRepCount: Int = 0,
    val sensorClassifiedPartialRepCount: Int = 0,
    val manualRepCount: Int = 0,
    val measuredRepCount: Int,
)

data class ExercisePerformanceTrendSummary(
    val exerciseKey: String,
    val exerciseName: String,
    val configurationCompatibility: MeasurementCompatibilityStatus = MeasurementCompatibilityStatus.COMPATIBLE,
    val calibrationVersion: String? = null,
    val isSimulatedIncluded: Boolean = false,
    val points: List<ExercisePerformanceTrendPoint>,
    val averageAngularRomDegrees: Double?,
    val angularRomSampleCount: Int = 0,
    val averageLinearRomCm: Double?,
    val linearRomSampleCount: Int = 0,
    val averageRepDurationMs: Long?,
    val durationSampleCount: Int = 0,
    val maxPeakAccelerationMps2: Double?,
    val accelerationSampleCount: Int = 0,
    val totalFullReps: Int,
    val totalPartialReps: Int,
    val totalSensorFullReps: Int = 0,
    val totalSensorPartialReps: Int = 0,
    val totalManualReps: Int = 0,
    val totalMeasuredReps: Int,
)

object WorkoutPerformanceAnalytics {

    fun stableExerciseKey(exercise: SessionExercise): String =
        exercise.sourceExerciseId ?: "unmapped:${exercise.exerciseNameSnapshot.trim().lowercase()}"

    private fun isSensorOrigin(origin: MeasurementOrigin): Boolean =
        origin == MeasurementOrigin.SENSOR_TELEMETRY || origin == MeasurementOrigin.SIMULATED_TELEMETRY

    private fun RepMeasurement.validDurationMs(): Long? =
        totalDurationMs?.takeIf { it > 0L }

    private fun RepMeasurement.validAngularRom(): Double? =
        angularRomDegrees?.takeIf { it.isFinite() && it > 0.0 }

    private fun RepMeasurement.validLinearRom(): Double? =
        linearRomCm?.takeIf { it.isFinite() && it > 0.0 }

    private fun RepMeasurement.validPeakAccel(): Double? =
        peakAccelerationMetersPerSecondSquared?.takeIf { it.isFinite() && it >= 0.0 }

    private fun RepMeasurement.hasAnyValidMeasurement(): Boolean =
        validDurationMs() != null ||
            validAngularRom() != null ||
            validLinearRom() != null ||
            validPeakAccel() != null

    private fun assessCompatibility(
        sourceExerciseId: String?,
        trackingCategory: TrackingCategory,
        reps: List<RepMeasurement>,
    ): MeasurementCompatibilityStatus {
        if (sourceExerciseId == null) return MeasurementCompatibilityStatus.UNCONFIGURED
        val config = ExerciseTrackingCatalog.forExercise(sourceExerciseId, trackingCategory)
        val hasUnsupportedAngular = !config.supportsAngularRom && reps.any { it.validAngularRom() != null }
        val hasUnsupportedLinear = !config.supportsLinearRom && reps.any { it.validLinearRom() != null }
        val hasUnsupportedAccel = !config.supportsPeakAcceleration && reps.any { it.validPeakAccel() != null }
        return if (hasUnsupportedAngular || hasUnsupportedLinear || hasUnsupportedAccel) {
            MeasurementCompatibilityStatus.INCOMPATIBLE
        } else {
            MeasurementCompatibilityStatus.COMPATIBLE
        }
    }

    fun summarizeSession(session: WorkoutSession): SessionPerformanceSummary {
        val orderedExercises = session.exercises.sortedBy { it.orderIndex }
        val groupedByKey = orderedExercises.groupBy { stableExerciseKey(it) }

        val exerciseSummaries = groupedByKey.map { (key, exercises) ->
            val first = exercises.first()
            val sets = exercises.flatMap { it.performedSets }
            val totalRecordedReps = sets.sumOf { it.fullReps + it.partialReps }
            val allReps = sets.flatMap { it.repRecords }
            val measuredReps = allReps.filter { it.hasAnyValidMeasurement() }

            val durations = allReps.mapNotNull { it.validDurationMs() }
            val accels = allReps.mapNotNull { it.validPeakAccel() }
            val angularRoms = allReps.mapNotNull { it.validAngularRom() }
            val linearRoms = allReps.mapNotNull { it.validLinearRom() }

            var sensorFull = 0
            var sensorPartial = 0
            var manualReps = 0
            for (set in sets) {
                val sensorRepsInSet = set.repRecords.filter { isSensorOrigin(it.origin) }
                if (sensorRepsInSet.isNotEmpty()) {
                    sensorFull += sensorRepsInSet.count { it.classification == RepClassification.FULL }
                    sensorPartial += sensorRepsInSet.count { it.classification == RepClassification.PARTIAL }
                } else if (isSensorOrigin(set.origin)) {
                    sensorFull += set.fullReps
                    sensorPartial += set.partialReps
                } else {
                    manualReps += set.fullReps + set.partialReps
                }
            }

            ExercisePerformanceSummary(
                exerciseKey = key,
                exerciseName = first.exerciseNameSnapshot,
                trackingCategory = first.trackingCategorySnapshot,
                configurationCompatibility = assessCompatibility(
                    sourceExerciseId = first.sourceExerciseId,
                    trackingCategory = first.trackingCategorySnapshot,
                    reps = allReps,
                ),
                calibrationVersion = null,
                totalRecordedReps = totalRecordedReps,
                measuredRepCount = measuredReps.size,
                sensorClassifiedFullRepCount = sensorFull,
                sensorClassifiedPartialRepCount = sensorPartial,
                manualRepCount = manualReps,
                averageRepDurationMs = if (durations.isEmpty()) null else durations.sum() / durations.size,
                durationSampleCount = durations.size,
                maxPeakAccelerationMps2 = accels.maxOrNull(),
                accelerationSampleCount = accels.size,
                averageAngularRomDegrees = if (angularRoms.isEmpty()) null else angularRoms.average(),
                angularRomSampleCount = angularRoms.size,
                averageLinearRomCm = if (linearRoms.isEmpty()) null else linearRoms.average(),
                linearRomSampleCount = linearRoms.size,
            )
        }

        val sets = orderedExercises.flatMap { it.performedSets }
        val allReps = sets.flatMap { it.repRecords }
        val durations = allReps.mapNotNull { it.validDurationMs() }
        val accels = allReps.mapNotNull { it.validPeakAccel() }

        // Only surface a single session-level ROM average if at most one exercise has ROM data,
        // preventing misleading cross-exercise ROM aggregation.
        val exercisesWithAngular = exerciseSummaries.filter { it.angularRomSampleCount > 0 }
        val exercisesWithLinear = exerciseSummaries.filter { it.linearRomSampleCount > 0 }

        return SessionPerformanceSummary(
            totalRecordedReps = sets.sumOf { it.fullReps + it.partialReps },
            measuredRepCount = exerciseSummaries.sumOf { it.measuredRepCount },
            fullRepCount = sets.sumOf { it.fullReps },
            partialRepCount = sets.sumOf { it.partialReps },
            sensorClassifiedFullRepCount = exerciseSummaries.sumOf { it.sensorClassifiedFullRepCount },
            sensorClassifiedPartialRepCount = exerciseSummaries.sumOf { it.sensorClassifiedPartialRepCount },
            manualRepCount = exerciseSummaries.sumOf { it.manualRepCount },
            averageRepDurationMs = if (durations.isEmpty()) null else durations.sum() / durations.size,
            maxPeakAccelerationMps2 = accels.maxOrNull(),
            averageAngularRomDegrees = if (exercisesWithAngular.size == 1) exercisesWithAngular.first().averageAngularRomDegrees else null,
            averageLinearRomCm = if (exercisesWithLinear.size == 1) exercisesWithLinear.first().averageLinearRomCm else null,
            exerciseSummaries = exerciseSummaries,
        )
    }

    /**
     * Computes historical performance trends for a specific exercise using repetition-weighted
     * aggregation directly from eligible measured repetitions.
     * Excludes ACTIVE, ABANDONED, and (unless [includeSimulated] is true) SIMULATED_TELEMETRY records.
     */
    fun calculateExerciseTrends(
        sessions: List<WorkoutSession>,
        exerciseKey: String,
        includeSimulated: Boolean = false,
        windowStartEpochMs: Long? = null,
        windowEndExclusiveEpochMs: Long? = null,
    ): ExercisePerformanceTrendSummary? {
        val eligibleSessions = sessions
            .filter { it.status == SessionStatus.COMPLETED || it.status == SessionStatus.PARTIALLY_COMPLETED }
            .filter { session ->
                val ts = session.endedAtEpochMs ?: session.startedAtEpochMs
                (windowStartEpochMs == null || ts >= windowStartEpochMs) &&
                    (windowEndExclusiveEpochMs == null || ts < windowEndExclusiveEpochMs)
            }
            .sortedBy { it.endedAtEpochMs ?: it.startedAtEpochMs }

        val points = mutableListOf<ExercisePerformanceTrendPoint>()
        var resolvedName: String? = null
        var resolvedSourceId: String? = null
        var resolvedTrackingCategory: TrackingCategory? = null
        var anySimulatedIncluded = false

        var totalDurationSumMs = 0L
        var totalDurationCount = 0
        var totalAngularSum = 0.0
        var totalAngularCount = 0
        var totalLinearSum = 0.0
        var totalLinearCount = 0
        var maxPeakAccel: Double? = null
        var totalAccelCount = 0
        val allTrendReps = mutableListOf<RepMeasurement>()

        for (session in eligibleSessions) {
            val matchingExercises = session.exercises.filter { stableExerciseKey(it) == exerciseKey }
            if (matchingExercises.isEmpty()) continue
            if (resolvedName == null) {
                resolvedName = matchingExercises.first().exerciseNameSnapshot
                resolvedSourceId = matchingExercises.first().sourceExerciseId
                resolvedTrackingCategory = matchingExercises.first().trackingCategorySnapshot
            }

            val eligibleSets: List<PerformedSet> = matchingExercises.flatMap { it.performedSets }.filter { set ->
                includeSimulated || set.origin != MeasurementOrigin.SIMULATED_TELEMETRY
            }
            if (eligibleSets.isEmpty()) continue

            val validReps: List<RepMeasurement> = eligibleSets.flatMap { it.repRecords }.filter { rep ->
                includeSimulated || rep.origin != MeasurementOrigin.SIMULATED_TELEMETRY
            }
            if (eligibleSets.any { it.origin == MeasurementOrigin.SIMULATED_TELEMETRY } ||
                validReps.any { it.origin == MeasurementOrigin.SIMULATED_TELEMETRY }
            ) {
                anySimulatedIncluded = true
            }
            allTrendReps.addAll(validReps)

            val durations = validReps.mapNotNull { it.validDurationMs() }
            val accels = validReps.mapNotNull { it.validPeakAccel() }
            val angular = validReps.mapNotNull { it.validAngularRom() }
            val linear = validReps.mapNotNull { it.validLinearRom() }
            val measuredCount = validReps.count { it.hasAnyValidMeasurement() }

            totalDurationSumMs += durations.sum()
            totalDurationCount += durations.size
            totalAngularSum += angular.sum()
            totalAngularCount += angular.size
            totalLinearSum += linear.sum()
            totalLinearCount += linear.size
            totalAccelCount += accels.size
            val pointMaxAccel = accels.maxOrNull()
            if (pointMaxAccel != null) {
                maxPeakAccel = maxOf(maxPeakAccel ?: pointMaxAccel, pointMaxAccel)
            }

            var sensorFull = 0
            var sensorPartial = 0
            var manualReps = 0
            for (set in eligibleSets) {
                val sensorReps = set.repRecords.filter { isSensorOrigin(it.origin) }
                if (sensorReps.isNotEmpty()) {
                    sensorFull += sensorReps.count { it.classification == RepClassification.FULL }
                    sensorPartial += sensorReps.count { it.classification == RepClassification.PARTIAL }
                } else if (isSensorOrigin(set.origin)) {
                    sensorFull += set.fullReps
                    sensorPartial += set.partialReps
                } else {
                    manualReps += set.fullReps + set.partialReps
                }
            }

            points.add(
                ExercisePerformanceTrendPoint(
                    sessionId = session.id,
                    timestampEpochMs = session.endedAtEpochMs ?: session.startedAtEpochMs,
                    averageAngularRomDegrees = if (angular.isEmpty()) null else angular.average(),
                    angularRomSampleCount = angular.size,
                    averageLinearRomCm = if (linear.isEmpty()) null else linear.average(),
                    linearRomSampleCount = linear.size,
                    averageRepDurationMs = if (durations.isEmpty()) null else durations.sum() / durations.size,
                    durationSampleCount = durations.size,
                    maxPeakAccelerationMps2 = pointMaxAccel,
                    accelerationSampleCount = accels.size,
                    fullRepCount = eligibleSets.sumOf { it.fullReps },
                    partialRepCount = eligibleSets.sumOf { it.partialReps },
                    sensorClassifiedFullRepCount = sensorFull,
                    sensorClassifiedPartialRepCount = sensorPartial,
                    manualRepCount = manualReps,
                    measuredRepCount = measuredCount,
                ),
            )
        }

        if (points.isEmpty()) return null

        val compatibility = assessCompatibility(
            sourceExerciseId = resolvedSourceId,
            trackingCategory = resolvedTrackingCategory ?: TrackingCategory.COMPOUND,
            reps = allTrendReps,
        )

        return ExercisePerformanceTrendSummary(
            exerciseKey = exerciseKey,
            exerciseName = resolvedName ?: exerciseKey,
            configurationCompatibility = compatibility,
            calibrationVersion = null,
            isSimulatedIncluded = anySimulatedIncluded,
            points = points,
            averageAngularRomDegrees = if (totalAngularCount == 0) null else totalAngularSum / totalAngularCount,
            angularRomSampleCount = totalAngularCount,
            averageLinearRomCm = if (totalLinearCount == 0) null else totalLinearSum / totalLinearCount,
            linearRomSampleCount = totalLinearCount,
            averageRepDurationMs = if (totalDurationCount == 0) null else totalDurationSumMs / totalDurationCount,
            durationSampleCount = totalDurationCount,
            maxPeakAccelerationMps2 = maxPeakAccel,
            accelerationSampleCount = totalAccelCount,
            totalFullReps = points.sumOf { it.fullRepCount },
            totalPartialReps = points.sumOf { it.partialRepCount },
            totalSensorFullReps = points.sumOf { it.sensorClassifiedFullRepCount },
            totalSensorPartialReps = points.sumOf { it.sensorClassifiedPartialRepCount },
            totalManualReps = points.sumOf { it.manualRepCount },
            totalMeasuredReps = points.sumOf { it.measuredRepCount },
        )
    }
}
