package com.ayasheta.pulsesmartgym.core.model.workout

import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad

data class PlannedSet(
    val id: String,
    val planExerciseId: String,
    val setIndex: Int,
    val targetReps: Int,
    val targetLoad: WeightLoad? = null,
    val targetRestSeconds: Int? = null,
)

data class PlanExercise(
    val id: String,
    val planId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val notes: String? = null,
    val plannedSets: List<PlannedSet>,
)

data class WorkoutPlan(
    val id: String,
    val ownerProfileId: String,
    val name: String,
    val description: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val exercises: List<PlanExercise>,
)

data class PlannedSetDraft(
    val setIndex: Int,
    val targetReps: Int,
    val targetLoad: WeightLoad? = null,
    val targetRestSeconds: Int? = null,
)

data class PlanExerciseDraft(
    val exerciseId: String,
    val orderIndex: Int,
    val notes: String? = null,
    val plannedSets: List<PlannedSetDraft>,
)

data class WorkoutPlanDraft(
    val name: String,
    val description: String? = null,
    val ownerProfileId: String = "local_default",
    val exercises: List<PlanExerciseDraft>,
)
