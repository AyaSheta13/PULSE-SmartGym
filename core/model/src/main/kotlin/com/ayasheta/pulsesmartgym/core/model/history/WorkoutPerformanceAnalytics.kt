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
    val isComparableTrendAvailable: Boolean = true,
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

    const val SIMULATION_COMPATIBILITY_VERSION = "sim-v1"

    /**
     * Conservative identity: only exercises with a stable catalog ID merge across sessions.
     * Unmapped historical exercises are never merged solely because their display names match.
     */
    fun stableExerciseKey(exercise: SessionExercise): String =
        exercise.sourceExerciseId ?: "unmapped:${exercise.sessionId}:${exercise.id}"

    private fun isSensorOrigin(origin: MeasurementOrigin): Boolean =
        origin == MeasurementOrigin.SENSOR_TELEMETRY || origin == MeasurementOrigin.SIMULATED_TELEMETRY

    private fun isManualOrigin(origin: MeasurementOrigin): Boolean =
        origin == MeasurementOrigin.MANUAL_INPUT || origin == MeasurementOrigin.MANUAL_CORRECTION

    // Valid zero measurements (>= 0) are preserved; negative, non-finite, and null are excluded.
    private fun RepMeasurement.validDurationMs(): Long? =
        totalDurationMs?.takeIf { it >= 0L }

    private fun RepMeasurement.validAngularRom(): Double? =
        angularRomDegrees?.takeIf { it.isFinite() && it >= 0.0 }

    private fun RepMeasurement.validLinearRom(): Double? =
        linearRomCm?.takeIf { it.isFinite() && it >= 0.0 }

    private fun RepMeasurement.validPeakAccel(): Double? =
        peakAccelerationMetersPerSecondSquared?.takeIf { it.isFinite() && it >= 0.0 }

    private fun RepMeasurement.hasAnyValidMeasurement(
        supportsAngular: Boolean,
        supportsLinear: Boolean,
        supportsAccel: Boolean,
    ): Boolean =
        validDurationMs() != null ||
            (supportsAngular && validAngularRom() != null) ||
            (supportsLinear && validLinearRom() != null) ||
            (supportsAccel && validPeakAccel() != null)

    private fun assessCompatibility(
        sourceExerciseId: String?,
        trackingCategory: TrackingCategory,
        reps: List<RepMeasurement>,
        calibrationVersion: String? = null,
    ): Pair<MeasurementCompatibilityStatus, String?> {
        if (sourceExerciseId == null) {
            return MeasurementCompatibilityStatus.UNCONFIGURED to null
        }
        val config = ExerciseTrackingCatalog.forExercise(sourceExerciseId, trackingCategory)
        val hasUnsupportedAngular = !config.supportsAngularRom && reps.any { it.validAngularRom() != null }
        val hasUnsupportedLinear = !config.supportsLinearRom && reps.any { it.validLinearRom() != null }
        val hasUnsupportedAccel = !config.supportsPeakAcceleration && reps.any { it.validPeakAccel() != null }
        if (hasUnsupportedAngular || hasUnsupportedLinear || hasUnsupportedAccel) {
            return MeasurementCompatibilityStatus.INCOMPATIBLE to calibrationVersion
        }
        val hasPhysicalSensor = reps.any { it.origin == MeasurementOrigin.SENSOR_TELEMETRY }
        val hasOnlySimulated = reps.isNotEmpty() && reps.all { it.origin == MeasurementOrigin.SIMULATED_TELEMETRY || isManualOrigin(it.origin) }
        return when {
            hasPhysicalSensor && calibrationVersion == null ->
                MeasurementCompatibilityStatus.UNCONFIGURED to null
            hasOnlySimulated && reps.any { it.origin == MeasurementOrigin.SIMULATED_TELEMETRY } ->
                MeasurementCompatibilityStatus.COMPATIBLE to SIMULATION_COMPATIBILITY_VERSION
            calibrationVersion != null ->
                MeasurementCompatibilityStatus.COMPATIBLE to calibrationVersion
            else ->
                MeasurementCompatibilityStatus.UNCONFIGURED to null
        }
    }

    private data class ProvenanceCounts(
        val totalRecordedReps: Int,
        val sensorFull: Int,
        val sensorPartial: Int,
        val manualReps: Int,
    )

    private fun countProvenanceForSets(
        sets: List<PerformedSet>,
        includeSimulated: Boolean,
    ): ProvenanceCounts {
        var totalRecorded = 0
        var sensorFull = 0
        var sensorPartial = 0
        var manualReps = 0
        for (set in sets) {
            if (set.origin == MeasurementOrigin.SIMULATED_TELEMETRY && !includeSimulated) {
                continue
            }
            if (set.repRecords.isNotEmpty()) {
                val eligibleReps = set.repRecords.filter { rep ->
                    includeSimulated || rep.origin != MeasurementOrigin.SIMULATED_TELEMETRY
                }
                totalRecorded += eligibleReps.size
                for (rep in eligibleReps) {
                    if (isSensorOrigin(rep.origin)) {
                        if (rep.classification == RepClassification.FULL) sensorFull += 1
                        else sensorPartial += 1
                    } else if (isManualOrigin(rep.origin)) {
                        manualReps += 1
                    }
                }
            } else {
                val count = set.fullReps + set.partialReps
                totalRecorded += count
                if (isManualOrigin(set.origin)) {
                    manualReps += count
                }
                // Aggregate-only sensor sets without detailed repRecords are NOT counted as
                // individually sensor-classified repetitions.
            }
        }
        return ProvenanceCounts(totalRecorded, sensorFull, sensorPartial, manualReps)
    }

    fun summarizeSession(
        session: WorkoutSession,
        includeSimulated: Boolean = true,
    ): SessionPerformanceSummary {
        val orderedExercises = session.exercises.sortedBy { it.orderIndex }
        val groupedByKey = orderedExercises.groupBy { stableExerciseKey(it) }

        val exerciseSummaries = groupedByKey.map { (key, exercises) ->
            val first = exercises.first()
            val config = ExerciseTrackingCatalog.forExercise(first.sourceExerciseId, first.trackingCategorySnapshot)
            val sets = exercises.flatMap { it.performedSets }.filter {
                includeSimulated || it.origin != MeasurementOrigin.SIMULATED_TELEMETRY
            }
            val provenance = countProvenanceForSets(sets, includeSimulated = includeSimulated)
            val allReps = sets.flatMap { it.repRecords }.filter {
                includeSimulated || it.origin != MeasurementOrigin.SIMULATED_TELEMETRY
            }
            val measuredReps = allReps.filter {
                it.hasAnyValidMeasurement(
                    supportsAngular = config.supportsAngularRom,
                    supportsLinear = config.supportsLinearRom,
                    supportsAccel = config.supportsPeakAcceleration,
                )
            }

            val durations = allReps.mapNotNull { it.validDurationMs() }
            val accels = if (config.supportsPeakAcceleration) allReps.mapNotNull { it.validPeakAccel() } else emptyList()
            val angularRoms = if (config.supportsAngularRom) allReps.mapNotNull { it.validAngularRom() } else emptyList()
            val linearRoms = if (config.supportsLinearRom) allReps.mapNotNull { it.validLinearRom() } else emptyList()
            val (compat, calVersion) = assessCompatibility(
                sourceExerciseId = first.sourceExerciseId,
                trackingCategory = first.trackingCategorySnapshot,
                reps = allReps,
            )

            ExercisePerformanceSummary(
                exerciseKey = key,
                exerciseName = first.exerciseNameSnapshot,
                trackingCategory = first.trackingCategorySnapshot,
                configurationCompatibility = compat,
                calibrationVersion = calVersion,
                totalRecordedReps = provenance.totalRecordedReps,
                measuredRepCount = measuredReps.size,
                sensorClassifiedFullRepCount = provenance.sensorFull,
                sensorClassifiedPartialRepCount = provenance.sensorPartial,
                manualRepCount = provenance.manualReps,
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

        val sets = orderedExercises.flatMap { it.performedSets }.filter {
            includeSimulated || it.origin != MeasurementOrigin.SIMULATED_TELEMETRY
        }
        val allReps = sets.flatMap { it.repRecords }.filter {
            includeSimulated || it.origin != MeasurementOrigin.SIMULATED_TELEMETRY
        }
        val durations = allReps.mapNotNull { it.validDurationMs() }
        val accels = allReps.mapNotNull { it.validPeakAccel() }

        val exercisesWithAngular = exerciseSummaries.filter { it.angularRomSampleCount > 0 }
        val exercisesWithLinear = exerciseSummaries.filter { it.linearRomSampleCount > 0 }

        return SessionPerformanceSummary(
            totalRecordedReps = exerciseSummaries.sumOf { it.totalRecordedReps },
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
     * aggregation and session-start period attribution (consistent with P9 weekly analytics).
     */
    fun calculateExerciseTrends(
        sessions: List<WorkoutSession>,
        exerciseKey: String,
        includeSimulated: Boolean = false,
        windowStartEpochMs: Long? = null,
        windowEndExclusiveEpochMs: Long? = null,
        calibrationVersion: String? = null,
    ): ExercisePerformanceTrendSummary? {
        val eligibleSessions = sessions
            .filter { it.status == SessionStatus.COMPLETED || it.status == SessionStatus.PARTIALLY_COMPLETED }
            .filter { session ->
                val ts = session.startedAtEpochMs
                (windowStartEpochMs == null || ts >= windowStartEpochMs) &&
                    (windowEndExclusiveEpochMs == null || ts < windowEndExclusiveEpochMs)
            }
            .sortedBy { it.startedAtEpochMs }

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
            val config = ExerciseTrackingCatalog.forExercise(resolvedSourceId, resolvedTrackingCategory ?: TrackingCategory.COMPOUND)

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
            val accels = if (config.supportsPeakAcceleration) validReps.mapNotNull { it.validPeakAccel() } else emptyList()
            val angular = if (config.supportsAngularRom) validReps.mapNotNull { it.validAngularRom() } else emptyList()
            val linear = if (config.supportsLinearRom) validReps.mapNotNull { it.validLinearRom() } else emptyList()
            val measuredCount = validReps.count {
                it.hasAnyValidMeasurement(
                    supportsAngular = config.supportsAngularRom,
                    supportsLinear = config.supportsLinearRom,
                    supportsAccel = config.supportsPeakAcceleration,
                )
            }

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

            val provenance = countProvenanceForSets(eligibleSets, includeSimulated = includeSimulated)

            points.add(
                ExercisePerformanceTrendPoint(
                    sessionId = session.id,
                    timestampEpochMs = session.startedAtEpochMs,
                    averageAngularRomDegrees = if (angular.isEmpty()) null else angular.average(),
                    angularRomSampleCount = angular.size,
                    averageLinearRomCm = if (linear.isEmpty()) null else linear.average(),
                    linearRomSampleCount = linear.size,
                    averageRepDurationMs = if (durations.isEmpty()) null else durations.sum() / durations.size,
                    durationSampleCount = durations.size,
                    maxPeakAccelerationMps2 = pointMaxAccel,
                    accelerationSampleCount = accels.size,
                    fullRepCount = provenance.sensorFull + if (validReps.isEmpty()) eligibleSets.sumOf { it.fullReps } else validReps.count { it.classification == RepClassification.FULL },
                    partialRepCount = provenance.sensorPartial + if (validReps.isEmpty()) eligibleSets.sumOf { it.partialReps } else validReps.count { it.classification == RepClassification.PARTIAL },
                    sensorClassifiedFullRepCount = provenance.sensorFull,
                    sensorClassifiedPartialRepCount = provenance.sensorPartial,
                    manualRepCount = provenance.manualReps,
                    measuredRepCount = measuredCount,
                ),
            )
        }

        if (points.isEmpty()) return null

        val (compatibility, resolvedCal) = assessCompatibility(
            sourceExerciseId = resolvedSourceId,
            trackingCategory = resolvedTrackingCategory ?: TrackingCategory.COMPOUND,
            reps = allTrendReps,
            calibrationVersion = calibrationVersion,
        )
        val comparable = compatibility == MeasurementCompatibilityStatus.COMPATIBLE

        return ExercisePerformanceTrendSummary(
            exerciseKey = exerciseKey,
            exerciseName = resolvedName ?: exerciseKey,
            configurationCompatibility = compatibility,
            calibrationVersion = resolvedCal,
            isComparableTrendAvailable = comparable,
            isSimulatedIncluded = anySimulatedIncluded,
            points = points,
            averageAngularRomDegrees = if (!comparable || totalAngularCount == 0) null else totalAngularSum / totalAngularCount,
            angularRomSampleCount = if (comparable) totalAngularCount else 0,
            averageLinearRomCm = if (!comparable || totalLinearCount == 0) null else totalLinearSum / totalLinearCount,
            linearRomSampleCount = if (comparable) totalLinearCount else 0,
            averageRepDurationMs = if (!comparable || totalDurationCount == 0) null else totalDurationSumMs / totalDurationCount,
            durationSampleCount = if (comparable) totalDurationCount else 0,
            maxPeakAccelerationMps2 = if (comparable) maxPeakAccel else null,
            accelerationSampleCount = if (comparable) totalAccelCount else 0,
            totalFullReps = points.sumOf { it.fullRepCount },
            totalPartialReps = points.sumOf { it.partialRepCount },
            totalSensorFullReps = points.sumOf { it.sensorClassifiedFullRepCount },
            totalSensorPartialReps = points.sumOf { it.sensorClassifiedPartialRepCount },
            totalManualReps = points.sumOf { it.manualRepCount },
            totalMeasuredReps = points.sumOf { it.measuredRepCount },
        )
    }
}
