package com.ayasheta.pulsesmartgym.core.model.session

import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    PARTIALLY_COMPLETED,
    ABANDONED,
}

data class SessionPlannedSet(
    val id: String,
    val sessionExerciseId: String,
    val setIndex: Int,
    val targetReps: Int,
    val targetLoad: WeightLoad? = null,
    val targetRestSeconds: Int? = null,
)

data class PerformedSet(
    val id: String,
    val sessionId: String,
    val sessionExerciseId: String,
    val setIndex: Int,
    val fullReps: Int,
    val partialReps: Int,
    val actualLoad: WeightLoad? = null,
    val completedAtEpochMs: Long,
    val trackingMode: TrackingMode,
    val origin: MeasurementOrigin,
    val durationMs: Long? = null,
    val repRecords: List<RepMeasurement> = emptyList(),
)

data class SessionExercise(
    val id: String,
    val sessionId: String,
    val sourceExerciseId: String?,
    val exerciseNameSnapshot: String,
    val muscleGroupSnapshot: MuscleGroup,
    val equipmentSnapshot: EquipmentCategory,
    val trackingCategorySnapshot: TrackingCategory,
    val orderIndex: Int,
    val plannedSets: List<SessionPlannedSet>,
    val performedSets: List<PerformedSet>,
)

data class WorkoutSession(
    val id: String,
    val ownerProfileId: String,
    val sourcePlanId: String?,
    val planNameSnapshot: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val status: SessionStatus,
    val exercises: List<SessionExercise>,
)

enum class RepCorrectionPolicy {
    /** Preserve existing rep records when aggregate counts are unchanged (e.g. weight-only correction). */
    PRESERVE_IF_COUNTS_MATCH,
    /** Replace existing rep records with [RecordSetRequest.repRecords]. */
    REPLACE_WITH_LEDGER,
    /** Explicitly invalidate and clear detailed rep records (aggregate-only correction). */
    INVALIDATE_LEDGER,
}

data class RecordSetRequest(
    val sessionId: String,
    val sessionExerciseId: String,
    val setIndex: Int,
    val fullReps: Int,
    val partialReps: Int = 0,
    val actualLoad: WeightLoad? = null,
    val trackingMode: TrackingMode = TrackingMode.MANUAL,
    val origin: MeasurementOrigin = MeasurementOrigin.MANUAL_INPUT,
    val durationMs: Long? = null,
    val repRecords: List<RepMeasurement> = emptyList(),
    val repCorrectionPolicy: RepCorrectionPolicy = RepCorrectionPolicy.PRESERVE_IF_COUNTS_MATCH,
)

data class SessionSetTarget(
    val exerciseId: String,
    val exerciseName: String,
    val setIndex: Int,
)

data class SessionRestState(
    val sessionId: String,
    val exerciseId: String,
    val exerciseName: String,
    val completedSetIndex: Int,
    val nextExerciseId: String?,
    val nextSetIndex: Int?,
    val durationSeconds: Int,
    val deadlineEpochMs: Long,
)
