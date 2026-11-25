package com.ayasheta.pulsesmartgym.core.model.workout

/**
 * Pure, deterministic workout duration estimator grounded in actual planned sets and rest durations.
 *
 * Assumptions:
 * - Each planned set takes [SET_EXECUTION_SECONDS] (35s) of active work.
 * - Rest occurs between sets within an exercise (uses set.targetRestSeconds or [DEFAULT_REST_SECONDS] = 60s).
 * - Transitioning between exercises takes [EXERCISE_TRANSITION_SECONDS] (45s).
 * - Returns null when the plan has zero exercises or zero planned sets.
 */
object WorkoutDurationEstimator {
    const val SET_EXECUTION_SECONDS = 35
    const val DEFAULT_REST_SECONDS = 60
    const val EXERCISE_TRANSITION_SECONDS = 45

    fun estimateMinutes(plan: WorkoutPlan): Int? =
        estimateMinutesFromCounts(
            exerciseRestSeconds = plan.exercises.map { ex ->
                ex.plannedSets.map { it.targetRestSeconds }
            },
        )

    fun estimateMinutes(draft: WorkoutPlanDraft): Int? =
        estimateMinutesFromCounts(
            exerciseRestSeconds = draft.exercises.map { ex ->
                ex.plannedSets.map { it.targetRestSeconds }
            },
        )

    fun estimateMinutesFromCounts(exerciseRestSeconds: List<List<Int?>>): Int? {
        val nonEmpty = exerciseRestSeconds.filter { it.isNotEmpty() }
        if (nonEmpty.isEmpty()) return null

        var totalSeconds = 0
        nonEmpty.forEach { setRests ->
            totalSeconds += setRests.size * SET_EXECUTION_SECONDS
            if (setRests.size > 1) {
                totalSeconds += setRests.dropLast(1).sumOf { rest ->
                    (rest ?: DEFAULT_REST_SECONDS).coerceAtLeast(0)
                }
            }
        }
        if (nonEmpty.size > 1) {
            totalSeconds += (nonEmpty.size - 1) * EXERCISE_TRANSITION_SECONDS
        }
        return ((totalSeconds + 30) / 60).coerceAtLeast(1)
    }
}
