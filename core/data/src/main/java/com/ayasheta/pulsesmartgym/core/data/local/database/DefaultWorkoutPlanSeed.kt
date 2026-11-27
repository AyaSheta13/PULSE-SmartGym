package com.ayasheta.pulsesmartgym.core.data.local.database

import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft

/**
 * Credible beginner-friendly default workout plans grounded in ACSM, ACE, NASM, and Mayo Clinic
 * strength-training guidance (compound movement patterns, 2–3 sets, 8–15 reps, 60–90s rest).
 */
object DefaultWorkoutPlanSeed {

    fun defaultPlanDrafts(): List<WorkoutPlanDraft> = listOf(
        WorkoutPlanDraft(
            name = "Beginner Full Body",
            description = "Balanced 10-set full-body routine covering squat, push, pull, hinge, and overhead press.",
            exercises = listOf(
                planExercise("ex_back_squat", orderIndex = 0, sets = 2, reps = 10, restSeconds = 120),
                planExercise("ex_db_flat_press", orderIndex = 1, sets = 2, reps = 10, restSeconds = 90),
                planExercise("ex_cable_row", orderIndex = 2, sets = 2, reps = 10, restSeconds = 90),
                planExercise("ex_romanian_deadlift", orderIndex = 3, sets = 2, reps = 10, restSeconds = 120),
                planExercise("ex_db_shoulder_press", orderIndex = 4, sets = 2, reps = 12, restSeconds = 60),
            ),
        ),
        WorkoutPlanDraft(
            name = "Upper Body Strength",
            description = "Balanced upper-body push and pull routine targeting chest, back, shoulders, and arms.",
            exercises = listOf(
                planExercise("ex_bench_press", orderIndex = 0, sets = 3, reps = 8, restSeconds = 90),
                planExercise("ex_lat_pulldown", orderIndex = 1, sets = 3, reps = 10, restSeconds = 90),
                planExercise("ex_db_shoulder_press", orderIndex = 2, sets = 3, reps = 10, restSeconds = 60),
                planExercise("ex_bicep_curl", orderIndex = 3, sets = 2, reps = 12, restSeconds = 60),
                planExercise("ex_tricep_pushdown", orderIndex = 4, sets = 2, reps = 12, restSeconds = 60),
            ),
        ),
        WorkoutPlanDraft(
            name = "Lower Body Foundation",
            description = "Foundational lower-body routine targeting quads, hamstrings, glutes, and calves.",
            exercises = listOf(
                planExercise("ex_leg_press", orderIndex = 0, sets = 3, reps = 12, restSeconds = 90),
                planExercise("ex_romanian_deadlift", orderIndex = 1, sets = 3, reps = 10, restSeconds = 120),
                planExercise("ex_leg_extension", orderIndex = 2, sets = 2, reps = 12, restSeconds = 60),
                planExercise("ex_calf_raise", orderIndex = 3, sets = 3, reps = 15, restSeconds = 60),
            ),
        ),
    )

    private fun planExercise(
        exerciseId: String,
        orderIndex: Int,
        sets: Int,
        reps: Int,
        restSeconds: Int,
    ): PlanExerciseDraft = PlanExerciseDraft(
        exerciseId = exerciseId,
        orderIndex = orderIndex,
        plannedSets = (1..sets).map { setIdx ->
            PlannedSetDraft(
                setIndex = setIdx,
                targetReps = reps,
                targetRestSeconds = restSeconds,
            )
        },
    )
}
