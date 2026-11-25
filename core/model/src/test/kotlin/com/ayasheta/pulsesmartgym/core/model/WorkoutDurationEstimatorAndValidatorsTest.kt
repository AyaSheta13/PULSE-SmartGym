package com.ayasheta.pulsesmartgym.core.model

import com.ayasheta.pulsesmartgym.core.model.common.PulseValidators
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutDurationEstimator
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutDurationEstimatorAndValidatorsTest {

    @Test
    fun estimator_returnsNullForEmptyPlanOrZeroSets() {
        val emptyDraft = WorkoutPlanDraft(name = "Empty", exercises = emptyList())
        assertNull(WorkoutDurationEstimator.estimateMinutes(emptyDraft))

        val zeroSetsDraft = WorkoutPlanDraft(
            name = "Zero sets",
            exercises = listOf(PlanExerciseDraft(exerciseId = "ex_1", orderIndex = 0, plannedSets = emptyList())),
        )
        assertNull(WorkoutDurationEstimator.estimateMinutes(zeroSetsDraft))
    }

    @Test
    fun estimator_isDeterministicAndGroundedInSetsAndRest() {
        // 2 exercises, 3 sets each, 90s rest between sets:
        // Ex1: 3*35 + 2*90 = 105 + 180 = 285s
        // Ex2: 3*35 + 2*90 = 105 + 180 = 285s
        // Transition: 45s -> total = 615s -> (615+30)/60 = 10 min
        val draft = WorkoutPlanDraft(
            name = "Upper",
            exercises = listOf(
                PlanExerciseDraft(
                    exerciseId = "ex_1",
                    orderIndex = 0,
                    plannedSets = (1..3).map { PlannedSetDraft(it, 10, targetRestSeconds = 90) },
                ),
                PlanExerciseDraft(
                    exerciseId = "ex_2",
                    orderIndex = 1,
                    plannedSets = (1..3).map { PlannedSetDraft(it, 10, targetRestSeconds = 90) },
                ),
            ),
        )
        assertEquals(10, WorkoutDurationEstimator.estimateMinutes(draft))
    }

    @Test
    fun validators_enforceBoundsForSetsRepsAndRest() {
        assertNotNull(PulseValidators.validateSetCount(0))
        assertNotNull(PulseValidators.validateSetCount(PulseValidators.MAX_SETS_PER_EXERCISE + 1))
        assertNull(PulseValidators.validateSetCount(4))

        assertNotNull(PulseValidators.validateTargetReps(0))
        assertNotNull(PulseValidators.validateTargetReps(PulseValidators.MAX_TARGET_REPS + 1))
        assertNull(PulseValidators.validateTargetReps(12))

        assertNotNull(PulseValidators.validateRestSeconds(-1))
        assertNotNull(PulseValidators.validateRestSeconds(PulseValidators.MAX_REST_SECONDS + 1))
        assertNull(PulseValidators.validateRestSeconds(null))
        assertNull(PulseValidators.validateRestSeconds(90))
    }
}
