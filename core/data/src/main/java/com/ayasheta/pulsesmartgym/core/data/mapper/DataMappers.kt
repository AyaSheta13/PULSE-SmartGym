package com.ayasheta.pulsesmartgym.core.data.mapper

import com.ayasheta.pulsesmartgym.core.data.local.database.entity.ExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PerformedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PlannedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.RepRecordEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionPlannedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.relation.PerformedSetWithReps
import com.ayasheta.pulsesmartgym.core.data.local.database.relation.PlanExerciseWithSets
import com.ayasheta.pulsesmartgym.core.data.local.database.relation.SessionExerciseWithSets
import com.ayasheta.pulsesmartgym.core.data.local.database.relation.WorkoutPlanWithExercises
import com.ayasheta.pulsesmartgym.core.data.local.database.relation.WorkoutSessionWithExercises
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionExercise
import com.ayasheta.pulsesmartgym.core.model.session.SessionPlannedSet
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExercise
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSet
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlan

internal fun ExerciseEntity.toDomain(): ExerciseDefinition = ExerciseDefinition(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    equipment = equipment,
    trackingCategory = trackingCategory,
    description = description,
    imageKey = imageKey,
    isHardwareTrackingSupported = isHardwareTrackingSupported,
)

internal fun PlannedSetEntity.toDomain(): PlannedSet = PlannedSet(
    id = id,
    planExerciseId = planExerciseId,
    setIndex = setIndex,
    targetReps = targetReps,
    targetLoad = targetLoadGrams?.let(WeightLoad::fromGrams),
    targetRestSeconds = targetRestSeconds,
)

internal fun PlanExerciseWithSets.toDomain(): PlanExercise = PlanExercise(
    id = planExercise.id,
    planId = planExercise.planId,
    exerciseId = planExercise.exerciseId,
    orderIndex = planExercise.orderIndex,
    notes = planExercise.notes,
    plannedSets = plannedSets.sortedBy { it.setIndex }.map { it.toDomain() },
)

internal fun WorkoutPlanWithExercises.toDomain(): WorkoutPlan = WorkoutPlan(
    id = plan.id,
    ownerProfileId = plan.ownerProfileId,
    name = plan.name,
    description = plan.description,
    createdAtEpochMs = plan.createdAtEpochMs,
    updatedAtEpochMs = plan.updatedAtEpochMs,
    exercises = exercises.sortedBy { it.planExercise.orderIndex }.map { it.toDomain() },
)

internal fun SessionPlannedSetEntity.toDomain(): SessionPlannedSet = SessionPlannedSet(
    id = id,
    sessionExerciseId = sessionExerciseId,
    setIndex = setIndex,
    targetReps = targetReps,
    targetLoad = targetLoadGrams?.let(WeightLoad::fromGrams),
    targetRestSeconds = targetRestSeconds,
)

internal fun RepRecordEntity.toDomain(): RepMeasurement = RepMeasurement(
    repIndex = repIndex,
    classification = classification,
    totalDurationMs = totalDurationMs,
    concentricDurationMs = concentricDurationMs,
    eccentricDurationMs = eccentricDurationMs,
    angularRomDegrees = angularRomDegrees,
    linearRomCm = linearRomCm,
    origin = origin,
)

internal fun PerformedSetEntity.toDomain(repRecords: List<RepMeasurement> = emptyList()): PerformedSet = PerformedSet(
    id = id,
    sessionId = sessionId,
    sessionExerciseId = sessionExerciseId,
    setIndex = setIndex,
    fullReps = fullReps,
    partialReps = partialReps,
    actualLoad = actualLoadGrams?.let(WeightLoad::fromGrams),
    completedAtEpochMs = completedAtEpochMs,
    trackingMode = trackingMode,
    origin = origin,
    durationMs = durationMs,
    repRecords = repRecords,
)

internal fun PerformedSetWithReps.toDomain(): PerformedSet = performedSet.toDomain(
    repRecords = repRecords.sortedBy { it.repIndex }.map { it.toDomain() },
)

internal fun SessionExerciseWithSets.toDomain(): SessionExercise = SessionExercise(
    id = sessionExercise.id,
    sessionId = sessionExercise.sessionId,
    sourceExerciseId = sessionExercise.sourceExerciseId,
    exerciseNameSnapshot = sessionExercise.exerciseNameSnapshot,
    muscleGroupSnapshot = sessionExercise.muscleGroupSnapshot,
    equipmentSnapshot = sessionExercise.equipmentSnapshot,
    trackingCategorySnapshot = sessionExercise.trackingCategorySnapshot,
    orderIndex = sessionExercise.orderIndex,
    plannedSets = plannedSets.sortedBy { it.setIndex }.map { it.toDomain() },
    performedSets = performedSets.sortedBy { it.performedSet.setIndex }.map { it.toDomain() },
)

internal fun WorkoutSessionWithExercises.toDomain(): WorkoutSession = WorkoutSession(
    id = session.id,
    ownerProfileId = session.ownerProfileId,
    sourcePlanId = session.sourcePlanId,
    planNameSnapshot = session.planNameSnapshot,
    startedAtEpochMs = session.startedAtEpochMs,
    endedAtEpochMs = session.endedAtEpochMs,
    status = session.status,
    exercises = exercises.sortedBy { it.sessionExercise.orderIndex }.map { it.toDomain() },
)

internal fun com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionRestStateEntity.toDomain():
    com.ayasheta.pulsesmartgym.core.model.session.SessionRestState =
    com.ayasheta.pulsesmartgym.core.model.session.SessionRestState(
        sessionId = sessionId,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        completedSetIndex = completedSetIndex,
        nextExerciseId = nextExerciseId,
        nextSetIndex = nextSetIndex,
        durationSeconds = durationSeconds,
        deadlineEpochMs = deadlineEpochMs,
    )
