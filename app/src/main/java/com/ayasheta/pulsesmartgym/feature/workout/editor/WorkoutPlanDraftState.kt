package com.ayasheta.pulsesmartgym.feature.workout.editor

import com.ayasheta.pulsesmartgym.core.model.common.PulseValidators
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import com.ayasheta.pulsesmartgym.feature.workout.components.displayLabel
import com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes
import com.ayasheta.pulsesmartgym.feature.workout.components.trackingSupportBadge
import java.util.UUID

data class EditorExerciseDraftItem(
    val occurrenceKey: String,
    val exerciseId: String,
    val exercise: ExerciseDefinition?,
    val notes: String? = null,
    val sets: List<PlannedSetDraft>,
) {
    val isUnavailable: Boolean get() = exercise == null

    val displayName: String
        get() = exercise?.name ?: "Unavailable Exercise ($exerciseId)"

    val setsCount: Int get() = sets.size

    val hasMixedReps: Boolean
        get() = sets.map { it.targetReps }.distinct().size > 1

    val hasMixedRest: Boolean
        get() = sets.map { it.targetRestSeconds ?: DEFAULT_REST_SECONDS }.distinct().size > 1

    val hasAnyLoad: Boolean
        get() = sets.any { it.targetLoad != null }

    val repsDisplay: String
        get() = if (sets.isEmpty()) {
            "0"
        } else if (!hasMixedReps) {
            sets.first().targetReps.toString()
        } else {
            val min = sets.minOf { it.targetReps }
            val max = sets.maxOf { it.targetReps }
            "$min–$max"
        }

    val restDisplay: String
        get() = if (sets.isEmpty()) {
            "0s"
        } else if (!hasMixedRest) {
            "${sets.first().targetRestSeconds ?: DEFAULT_REST_SECONDS}s"
        } else {
            "Mixed"
        }

    val metadataLine: String
        get() = exercise?.let { "${it.muscleGroup.displayLabel().uppercase()} • ${it.trackingSupportBadge()}" }
            ?: "UNAVAILABLE IN CATALOG"

    val equipmentLabel: String
        get() = exercise?.equipment?.displayLabel() ?: "Missing reference"

    val thumbnailRes: Int
        get() = resolveExerciseThumbnailRes(exercise?.imageKey, exercise?.muscleGroup)

    fun adjustSetCount(delta: Int): EditorExerciseDraftItem {
        val nextCount = (sets.size + delta).coerceIn(1, PulseValidators.MAX_SETS_PER_EXERCISE)
        if (nextCount == sets.size) return this
        val updatedSets = if (nextCount > sets.size) {
            val template = sets.lastOrNull() ?: PlannedSetDraft(
                setIndex = 1,
                targetReps = DEFAULT_TARGET_REPS,
                targetLoad = null,
                targetRestSeconds = DEFAULT_REST_SECONDS,
            )
            sets + ((sets.size + 1)..nextCount).map { idx ->
                template.copy(setIndex = idx)
            }
        } else {
            sets.take(nextCount).mapIndexed { index, set ->
                set.copy(setIndex = index + 1)
            }
        }
        return copy(sets = updatedSets)
    }

    fun adjustUniformReps(delta: Int): EditorExerciseDraftItem {
        if (hasMixedReps || sets.isEmpty()) return this
        val current = sets.first().targetReps
        val next = (current + delta).coerceIn(1, PulseValidators.MAX_TARGET_REPS)
        if (next == current) return this
        return copy(sets = sets.map { it.copy(targetReps = next) })
    }

    fun adjustUniformRest(deltaSeconds: Int): EditorExerciseDraftItem {
        if (hasMixedRest || sets.isEmpty()) return this
        val current = sets.first().targetRestSeconds ?: DEFAULT_REST_SECONDS
        val next = (current + deltaSeconds).coerceIn(0, PulseValidators.MAX_REST_SECONDS)
        if (next == current) return this
        return copy(sets = sets.map { it.copy(targetRestSeconds = next) })
    }

    fun updateSingleSet(
        setIndex: Int,
        targetReps: Int? = null,
        targetRestSeconds: Int? = null,
        targetLoad: WeightLoad? = sets.firstOrNull { it.setIndex == setIndex }?.targetLoad,
    ): EditorExerciseDraftItem {
        val updated = sets.map { set ->
            if (set.setIndex == setIndex) {
                set.copy(
                    targetReps = (targetReps ?: set.targetReps).coerceIn(1, PulseValidators.MAX_TARGET_REPS),
                    targetRestSeconds = (targetRestSeconds ?: set.targetRestSeconds ?: DEFAULT_REST_SECONDS)
                        .coerceIn(0, PulseValidators.MAX_REST_SECONDS),
                    targetLoad = targetLoad,
                )
            } else {
                set
            }
        }
        return copy(sets = updated)
    }

    fun toDomainDraft(orderIndex: Int): PlanExerciseDraft = PlanExerciseDraft(
        exerciseId = exerciseId,
        orderIndex = orderIndex,
        notes = notes?.takeIf { it.isNotBlank() },
        plannedSets = sets.mapIndexed { idx, set ->
            set.copy(setIndex = idx + 1)
        },
    )

    companion object {
        const val DEFAULT_SETS_COUNT = 3
        const val DEFAULT_TARGET_REPS = 10
        const val DEFAULT_REST_SECONDS = 90

        fun defaultFor(
            exercise: ExerciseDefinition,
            occurrenceKey: String = UUID.randomUUID().toString(),
        ): EditorExerciseDraftItem = EditorExerciseDraftItem(
            occurrenceKey = occurrenceKey,
            exerciseId = exercise.id,
            exercise = exercise,
            notes = null,
            sets = (1..DEFAULT_SETS_COUNT).map { idx ->
                PlannedSetDraft(
                    setIndex = idx,
                    targetReps = DEFAULT_TARGET_REPS,
                    targetLoad = null,
                    targetRestSeconds = DEFAULT_REST_SECONDS,
                )
            },
        )
    }
}

data class WorkoutPlanDraftState(
    val name: String = "",
    val description: String = "",
    val exercises: List<EditorExerciseDraftItem> = emptyList(),
    val preferredWeightUnit: WeightUnit = WeightUnit.KG,
) {
    val totalSetsCount: Int get() = exercises.sumOf { it.sets.size }

    fun toDomainDraft(): WorkoutPlanDraft = WorkoutPlanDraft(
        name = name.trim(),
        description = description.trim().takeIf { it.isNotEmpty() },
        exercises = exercises.mapIndexed { index, item ->
            item.toDomainDraft(orderIndex = index)
        },
    )
}
