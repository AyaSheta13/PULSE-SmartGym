package com.ayasheta.pulsesmartgym.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.data.local.database.ExerciseCatalogSeed
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase
import com.ayasheta.pulsesmartgym.core.data.local.database.RoomTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.repository.exercise.RoomExerciseRepository
import com.ayasheta.pulsesmartgym.core.data.repository.session.RoomWorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.data.repository.workout.RoomWorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P7WorkoutSessionRoomTest {

    private lateinit var database: PulseDatabase
    private lateinit var planRepo: RoomWorkoutPlanRepository
    private lateinit var sessionRepo: RoomWorkoutSessionRepository
    private var nowEpochMs: Long = 1_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val txRunner = RoomTransactionRunner(database)
        val exerciseRepo = RoomExerciseRepository(database.exerciseDao())
        kotlinx.coroutines.runBlocking<Unit> {
            exerciseRepo.observeAll().first()
        }
        planRepo = RoomWorkoutPlanRepository(database.workoutPlanDao(), database.exerciseDao(), txRunner) { nowEpochMs }
        sessionRepo = RoomWorkoutSessionRepository(database.workoutSessionDao(), database.workoutPlanDao(), database.exerciseDao(), txRunner) { nowEpochMs }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun singleActiveSession_startResumeConflictConcurrentAndMultiOwner() = runTest {
        val planA = (planRepo.createPlan(
            "owner_a",
            WorkoutPlanDraft(
                name = "Plan A",
                exercises = listOf(PlanExerciseDraft("ex_bench_press", 0, plannedSets = (1..2).map { PlannedSetDraft(it, 10, null, 90) })),
            ),
        ) as DomainResult.Success).data
        val planB = (planRepo.createPlan(
            "owner_a",
            WorkoutPlanDraft(
                name = "Plan B",
                exercises = listOf(PlanExerciseDraft("ex_back_squat", 0, plannedSets = listOf(PlannedSetDraft(1, 8, null, 120)))),
            ),
        ) as DomainResult.Success).data
        val planOtherOwner = (planRepo.createPlan(
            "owner_b",
            WorkoutPlanDraft(
                name = "Owner B Plan",
                exercises = listOf(PlanExerciseDraft("ex_bench_press", 0, plannedSets = listOf(PlannedSetDraft(1, 10, null, 90)))),
            ),
        ) as DomainResult.Success).data

        // 1. Concurrent starts on Plan A -> same single ACTIVE session
        val results = listOf(
            async { sessionRepo.startSessionFromPlan("owner_a", planA.id) },
            async { sessionRepo.startSessionFromPlan("owner_a", planA.id) },
        ).awaitAll()
        val firstSession = (results[0] as DomainResult.Success).data
        val secondSession = (results[1] as DomainResult.Success).data
        assertEquals(firstSession.id, secondSession.id)

        // 2. Starting Plan B while Plan A is ACTIVE -> ActiveSessionConflict, no second ACTIVE session
        val conflictResult = sessionRepo.startSessionFromPlan("owner_a", planB.id)
        assertTrue(conflictResult is DomainResult.Failure)
        val conflict = (conflictResult as DomainResult.Failure).error as DataError.ActiveSessionConflict
        assertEquals(firstSession.id, conflict.activeSessionId)
        assertEquals("Plan A", conflict.activePlanName)

        // 3. owner_b starts independently without conflict
        val ownerBSession = (sessionRepo.startSessionFromPlan("owner_b", planOtherOwner.id) as DomainResult.Success).data
        assertEquals("owner_b", ownerBSession.ownerProfileId)
        assertNull(sessionRepo.getSession("owner_b", firstSession.id))
    }

    @Test
    fun sessionLifecycle_nonSequentialCorrectionPlanMutationAndAtomicFinalization() = runTest {
        val plan = (planRepo.createPlan(
            "owner_a",
            WorkoutPlanDraft(
                name = "Upper Body",
                exercises = listOf(
                    PlanExerciseDraft("ex_bench_press", 0, plannedSets = (1..2).map { PlannedSetDraft(it, 10, WeightLoad.fromKilograms(80.0), 90) }),
                    PlanExerciseDraft("ex_barbell_row", 1, plannedSets = listOf(PlannedSetDraft(1, 8, WeightLoad.fromKilograms(70.0), 90))),
                ),
            ),
        ) as DomainResult.Success).data

        val started = (sessionRepo.startSessionFromPlan("owner_a", plan.id) as DomainResult.Success).data
        val ex1Id = started.exercises[0].id
        val ex2Id = started.exercises[1].id

        // Edit & delete original plan while session is ACTIVE -> snapshot remains intact
        planRepo.deletePlan("owner_a", plan.id)
        val afterPlanDelete = sessionRepo.getSession("owner_a", started.id)!!
        assertEquals("Upper Body", afterPlanDelete.planNameSnapshot)
        assertEquals(2, afterPlanDelete.exercises.size)

        // Non-sequential recording: ex2 set 1, then ex1 set 2, then ex1 set 1
        assertTrue(sessionRepo.recordPerformedSet("owner_a", RecordSetRequest(started.id, ex2Id, 1, 8, 1, WeightLoad.fromKilograms(70.0), TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT)) is RecordSetResult.Recorded)
        assertTrue(sessionRepo.recordPerformedSet("owner_a", RecordSetRequest(started.id, ex1Id, 2, 10, 0, WeightLoad.fromKilograms(80.0), TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT)) is RecordSetResult.Recorded)
        assertTrue(sessionRepo.recordPerformedSet("owner_a", RecordSetRequest(started.id, ex1Id, 1, 10, 0, WeightLoad.fromKilograms(80.0), TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT)) is RecordSetResult.Recorded)

        // Duplicate record rejected
        assertTrue(sessionRepo.recordPerformedSet("owner_a", RecordSetRequest(started.id, ex1Id, 1, 12, 0, null, TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT)) is RecordSetResult.AlreadyRecorded)

        // Correct ex1 set 1 explicitly
        val corrected = sessionRepo.correctPerformedSet("owner_a", RecordSetRequest(started.id, ex1Id, 1, 11, 1, WeightLoad.fromKilograms(82.5), TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT))
        assertTrue(corrected is DomainResult.Success)

        nowEpochMs = 1_500_000L
        val completionRace = listOf(
            async { sessionRepo.completeSession("owner_a", started.id) },
            async { sessionRepo.completeSession("owner_a", started.id) },
        ).awaitAll()
        assertEquals(1, completionRace.count { it is DomainResult.Success })
        assertEquals(1, completionRace.count { it is DomainResult.Failure })

        val completed = sessionRepo.getSession("owner_a", started.id)!!
        assertEquals(SessionStatus.COMPLETED, completed.status)
        assertEquals(1_500_000L, completed.endedAtEpochMs)

        // Later writes & corrections rejected after terminal status
        assertTrue(sessionRepo.correctPerformedSet("owner_a", RecordSetRequest(started.id, ex1Id, 1, 15, 0, null, TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT)) is DomainResult.Failure)

        // Early termination with 1 set -> PARTIALLY_COMPLETED; 0 sets -> ABANDONED
        val p2 = (planRepo.createPlan("owner_a", WorkoutPlanDraft("P2", exercises = listOf(PlanExerciseDraft("ex_bench_press", 0, plannedSets = (1..2).map { PlannedSetDraft(it, 10, null, 90) })))) as DomainResult.Success).data
        val s2 = (sessionRepo.startSessionFromPlan("owner_a", p2.id) as DomainResult.Success).data
        sessionRepo.recordPerformedSet("owner_a", RecordSetRequest(s2.id, s2.exercises.first().id, 1, 10, 0, null, TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT))
        val partial = (sessionRepo.endSessionEarly("owner_a", s2.id) as DomainResult.Success).data
        assertEquals(SessionStatus.PARTIALLY_COMPLETED, partial.status)

        val s3 = (sessionRepo.startSessionFromPlan("owner_a", p2.id) as DomainResult.Success).data
        val abandoned = (sessionRepo.endSessionEarly("owner_a", s3.id) as DomainResult.Success).data
        assertEquals(SessionStatus.ABANDONED, abandoned.status)
        assertTrue(sessionRepo.observeActiveSession("owner_a").first() == null)
    }
}
