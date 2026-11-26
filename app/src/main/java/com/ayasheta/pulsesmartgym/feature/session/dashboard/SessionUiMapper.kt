package com.ayasheta.pulsesmartgym.feature.session.dashboard

import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object SessionUiMapper {

    private const val LBS_PER_KG = 2.20462262185

    fun formatVolumeNumber(volumeKg: Double, unit: WeightUnit): String {
        val value = when (unit) {
            WeightUnit.KG -> volumeKg
            WeightUnit.LBS -> volumeKg * LBS_PER_KG
        }
        val rounded = if (value.isFinite()) value.roundToLong().coerceAtLeast(0L) else 0L
        return NumberFormat.getIntegerInstance(Locale.US).format(rounded)
    }

    fun formatVolumeWithUnit(volumeKg: Double, unit: WeightUnit): String {
        val unitLabel = if (unit == WeightUnit.KG) "kg" else "lbs"
        return "${formatVolumeNumber(volumeKg, unit)} $unitLabel"
    }

    fun computeElapsedSeconds(
        startedAtEpochMs: Long,
        nowEpochMs: Long,
    ): Long = ((nowEpochMs - startedAtEpochMs) / 1000L).coerceAtLeast(0L)

    fun formatElapsedMmSs(elapsedSeconds: Long): String {
        val safe = elapsedSeconds.coerceAtLeast(0L)
        val minutes = safe / 60L
        val seconds = safe % 60L
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun formatDurationReadable(durationMs: Long?): String {
        val totalSeconds = ((durationMs ?: 0L) / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return if (minutes > 0L) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    fun formatWeightLoad(load: WeightLoad?, unit: WeightUnit): String? {
        load ?: return null
        val value = when (unit) {
            WeightUnit.KG -> load.toKilograms()
            WeightUnit.LBS -> load.toPounds()
        }
        val unitLabel = if (unit == WeightUnit.KG) "kg" else "lbs"
        val rounded = (value * 10.0).roundToInt() / 10.0
        val numberText = if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", rounded)
        }
        return "$numberText $unitLabel"
    }

    fun formatEditableWeight(load: WeightLoad?, unit: WeightUnit): String {
        load ?: return ""
        val value = when (unit) {
            WeightUnit.KG -> load.toKilograms()
            WeightUnit.LBS -> load.toPounds()
        }
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", rounded)
        }
    }

    fun mapToActiveUiState(
        session: WorkoutSession,
        preferredWeightUnit: WeightUnit,
        nowEpochMs: Long,
        previous: WorkoutSessionUiState.Active? = null,
    ): WorkoutSessionUiState.Active {
        val orderedExercises = session.exercises.sortedBy { it.orderIndex }
        var totalPlanned = 0
        var totalRecorded = 0

        val exerciseModels = orderedExercises.map { exercise ->
            val performedBySetIndex = exercise.performedSets.associateBy { it.setIndex }
            val orderedPlanned = exercise.plannedSets.sortedBy { it.setIndex }
            val rows = orderedPlanned.map { planned ->
                val performed = performedBySetIndex[planned.setIndex]?.let { rec ->
                    PerformedSetUiModel(
                        fullReps = rec.fullReps,
                        partialReps = rec.partialReps,
                        actualLoad = rec.actualLoad,
                        trackingMode = rec.trackingMode,
                        hasDetailedMeasurements = rec.repRecords.isNotEmpty(),
                    )
                }
                SessionSetRowUiModel(
                    sessionExerciseId = exercise.id,
                    setIndex = planned.setIndex,
                    targetReps = planned.targetReps,
                    targetLoad = planned.targetLoad,
                    targetRestSeconds = planned.targetRestSeconds,
                    performed = performed,
                )
            }
            val exPlanned = rows.size
            val exDone = rows.count { it.performed != null }
            totalPlanned += exPlanned
            totalRecorded += exDone
            SessionExerciseUiModel(
                sessionExerciseId = exercise.id,
                sourceExerciseId = exercise.sourceExerciseId,
                exerciseName = exercise.exerciseNameSnapshot,
                completedSetsCount = exDone,
                totalPlannedSetsCount = exPlanned,
                isCompleted = exPlanned > 0 && exDone >= exPlanned,
                progressFraction = if (exPlanned > 0) (exDone.toFloat() / exPlanned.toFloat()).coerceIn(0f, 1f) else 0f,
                sets = rows,
            )
        }

        val overallFraction = if (totalPlanned > 0) {
            (totalRecorded.toFloat() / totalPlanned.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        return WorkoutSessionUiState.Active(
            sessionId = session.id,
            ownerId = session.ownerProfileId,
            planName = session.planNameSnapshot,
            startedAtEpochMs = session.startedAtEpochMs,
            elapsedSeconds = computeElapsedSeconds(session.startedAtEpochMs, nowEpochMs),
            totalPlannedSets = totalPlanned,
            totalRecordedSets = totalRecorded,
            progressFraction = overallFraction,
            allSetsCompleted = totalPlanned > 0 && totalRecorded >= totalPlanned,
            preferredWeightUnit = preferredWeightUnit,
            exercises = exerciseModels,
            isSavingSet = previous?.isSavingSet ?: false,
            isCompletingSession = previous?.isCompletingSession ?: false,
            isEndingEarly = previous?.isEndingEarly ?: false,
            showEndEarlyConfirmation = previous?.showEndEarlyConfirmation ?: false,
            manualEntryTarget = previous?.manualEntryTarget,
            manualEntryError = previous?.manualEntryError,
            actionError = previous?.actionError,
        )
    }
}
