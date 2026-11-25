package com.ayasheta.pulsesmartgym.core.model

import com.ayasheta.pulsesmartgym.core.model.common.PulseValidators
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.history.WorkoutAnalytics
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionExercise
import com.ayasheta.pulsesmartgym.core.model.session.SessionPlannedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DomainModelsAndAnalyticsTest {

    @Test
    fun weightLoad_convertsCanonicalGramsAccurately() {
        val load = WeightLoad.fromKilograms(82.5)
        assertEquals(82_500L, load.grams)
        assertEquals(82.5, load.toKilograms(), 0.0001)
    }

    @Test
    fun validators_enforcePlanNameAndOneBasedSetIndexAndReps() {
        assertNotNull(PulseValidators.validatePlanName("   "))
        assertNull(PulseValidators.validatePlanName("Upper Body Power"))

        assertNotNull(PulseValidators.validateSetIndex(0))
        assertNotNull(PulseValidators.validateSetIndex(-1))
        assertNull(PulseValidators.validateSetIndex(1))

        assertNotNull(PulseValidators.validatePerformedReps(fullReps = 0, partialReps = 0))
        assertNotNull(PulseValidators.validatePerformedReps(fullReps = -1, partialReps = 2))
        assertNull(PulseValidators.validatePerformedReps(fullReps = 8, partialReps = 2))
        assertNull(PulseValidators.validatePerformedReps(fullReps = 0, partialReps = 3))
    }

    @Test
    fun analytics_calculatesVolumeFromActualFullRepsAndKnownLoadOnly() {
        val performed = listOf(
            PerformedSet(
                id = "ps1",
                sessionId = "s1",
                sessionExerciseId = "se1",
                setIndex = 1,
                fullReps = 10,
                partialReps = 2, // excluded from weighted volume
                actualLoad = WeightLoad.fromKilograms(80.0),
                completedAtEpochMs = 1000L,
                trackingMode = TrackingMode.MANUAL,
                origin = MeasurementOrigin.MANUAL_INPUT,
            ),
            PerformedSet(
                id = "ps2",
                sessionId = "s1",
                sessionExerciseId = "se1",
                setIndex = 2,
                fullReps = 12,
                partialReps = 0,
                actualLoad = null, // unknown/bodyweight load -> excluded, not 0 kg
                completedAtEpochMs = 2000L,
                trackingMode = TrackingMode.MANUAL,
                origin = MeasurementOrigin.MANUAL_INPUT,
            ),
        )

        val session = WorkoutSession(
            id = "s1",
            ownerProfileId = "local",
            sourcePlanId = "p1",
            planNameSnapshot = "Upper Body Power",
            startedAtEpochMs = 1_000L,
            endedAtEpochMs = 61_000L,
            status = SessionStatus.PARTIALLY_COMPLETED,
            exercises = listOf(
                SessionExercise(
                    id = "se1",
                    sessionId = "s1",
                    sourceExerciseId = "ex_bench_press",
                    exerciseNameSnapshot = "Barbell Bench Press",
                    muscleGroupSnapshot = MuscleGroup.CHEST,
                    equipmentSnapshot = EquipmentCategory.BARBELL,
                    trackingCategorySnapshot = TrackingCategory.LINEAR,
                    orderIndex = 0,
                    plannedSets = listOf(
                        SessionPlannedSet("sps1", "se1", setIndex = 1, targetReps = 10),
                        SessionPlannedSet("sps2", "se1", setIndex = 2, targetReps = 10),
                        SessionPlannedSet("sps3", "se1", setIndex = 3, targetReps = 10),
                    ),
                    performedSets = performed,
                ),
            ),
        )

        val summary = WorkoutAnalytics.summarizeSession(session)
        assertEquals(800.0, summary.totalVolumeKg, 0.001)
        assertEquals(22, summary.totalFullReps)
        assertEquals(2, summary.totalPartialReps)
        assertEquals(2, summary.completedSetsCount)
        assertEquals(3, summary.totalPlannedSetsCount)
        assertEquals(60_000L, summary.durationMs)
    }
}
