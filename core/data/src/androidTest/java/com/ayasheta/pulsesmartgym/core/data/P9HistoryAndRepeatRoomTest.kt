package com.ayasheta.pulsesmartgym.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase
import com.ayasheta.pulsesmartgym.core.data.local.database.RoomTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.WorkoutSessionDao
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.repository.exercise.RoomExerciseRepository
import com.ayasheta.pulsesmartgym.core.data.repository.history.RoomWorkoutHistoryRepository
import com.ayasheta.pulsesmartgym.core.data.repository.workout.RoomWorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.data.repository.session.RoomWorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class P9HistoryAndRepeatRoomTest {

    @Test
    fun historyAndRepeatWorkout_survivesPlanEditDeletion_enforcesIsolation_andPersistsAcrossReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbFile = File(context.filesDir, "p9_history_repeat_${System.nanoTime()}.db")
        var nowMs = 1_760_000_000_000L

        val db1 = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath)
            .addMigrations(PulseDatabase.MIGRATION_1_2, PulseDatabase.MIGRATION_2_3)
            .build()

        val completedId: String
        val partialId: String
        val planId: String
        try {
            val tx = RoomTransactionRunner(db1)
            val exRepo = RoomExerciseRepository(db1.exerciseDao())
            val planRepo = RoomWorkoutPlanRepository(db1.workoutPlanDao(), db1.exerciseDao(), tx, { nowMs })
            val sessionRepo = RoomWorkoutSessionRepository(db1.workoutSessionDao(), db1.workoutPlanDao(), db1.exerciseDao(), tx, { nowMs })
            val historyRepo = RoomWorkoutHistoryRepository(db1.workoutSessionDao())

            val exercises = exRepo.observeAll().first()
            val bench = exercises.first { it.name.contains("Bench", ignoreCase = true) }
            val squat = exercises.first { it.name.contains("Squat", ignoreCase = true) }

            val plan = (planRepo.createPlan(
                ownerId = "owner_a",
                draft = WorkoutPlanDraft(
                    name = "Full Power",
                    exercises = listOf(
                        PlanExerciseDraft(bench.id, 0, plannedSets = listOf(PlannedSetDraft(1, 10, WeightLoad.fromKilograms(80.0), 90))),
                        PlanExerciseDraft(squat.id, 1, plannedSets = listOf(PlannedSetDraft(1, 8, WeightLoad.fromKilograms(100.0), 120))),
                    ),
                ),
            ) as DomainResult.Success).data
            planId = plan.id

            // 1. Complete workout
            val s1 = (sessionRepo.startSessionFromPlan("owner_a", planId) as DomainResult.Success).data
            completedId = s1.id
            sessionRepo.recordPerformedSet("owner_a", RecordSetRequest(s1.id, s1.exercises[0].id, 1, 10, 0, WeightLoad.fromKilograms(85.0)))
            sessionRepo.recordPerformedSet("owner_a", RecordSetRequest(s1.id, s1.exercises[1].id, 1, 8, 0, WeightLoad.fromKilograms(105.0)))
            nowMs += 1_800_000L
            sessionRepo.completeSession("owner_a", s1.id)

            // 2. Partial workout (only 1 of 2 exercises)
            nowMs += 3_600_000L
            val s2 = (sessionRepo.startSessionFromPlan("owner_a", planId) as DomainResult.Success).data
            partialId = s2.id
            sessionRepo.recordPerformedSet("owner_a", RecordSetRequest(s2.id, s2.exercises[0].id, 1, 8, 0, WeightLoad.fromKilograms(90.0)))
            nowMs += 300_000L
            sessionRepo.endSessionEarly("owner_a", s2.id)

            // 3. Abandoned workout (0 sets) -> excluded from history
            nowMs += 3_600_000L
            val s3 = (sessionRepo.startSessionFromPlan("owner_a", planId) as DomainResult.Success).data
            sessionRepo.endSessionEarly("owner_a", s3.id)
            assertNull(historyRepo.getSessionDetails("owner_a", s3.id))

            // 4. Delete source plan -> historical details still accessible & repeatable
            planRepo.deletePlan("owner_a", planId)
            val fetchedPartial = historyRepo.getSessionDetails("owner_a", partialId)
            assertNotNull(fetchedPartial)
            assertEquals("Full Power", fetchedPartial!!.planNameSnapshot)
            assertEquals(2, fetchedPartial.exercises.size)

            // 5. Repeat partial workout after source plan deletion -> full planned structure, 0 performed sets
            val repeated = (sessionRepo.startSessionFromHistory("owner_a", partialId) as DomainResult.Success).data
            assertNotEquals(partialId, repeated.id)
            assertEquals(SessionStatus.ACTIVE, repeated.status)
            assertEquals(2, repeated.exercises.size)
            assertEquals(0, repeated.exercises.sumOf { it.performedSets.size })
            assertNotEquals(fetchedPartial.exercises[0].id, repeated.exercises[0].id)

            // 6. Active session conflict when repeating again while active
            val conflict = sessionRepo.startSessionFromHistory("owner_a", completedId)
            assertTrue(conflict is DomainResult.Failure && conflict.error is DataError.ActiveSessionConflict)

            // 7. Owner isolation
            assertTrue(historyRepo.observeHistory("owner_b").first().isEmpty())
            assertNull(historyRepo.getSessionDetails("owner_b", completedId))
        } finally {
            db1.close()
        }

        // Reopen DB and verify history, PRs, and active repeated session survived
        val db2 = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath)
            .addMigrations(PulseDatabase.MIGRATION_1_2, PulseDatabase.MIGRATION_2_3)
            .build()
        try {
            val historyRepo2 = RoomWorkoutHistoryRepository(db2.workoutSessionDao())
            val sessionRepo2 = RoomWorkoutSessionRepository(
                db2.workoutSessionDao(), db2.workoutPlanDao(), db2.exerciseDao(), RoomTransactionRunner(db2),
            )
            val history = historyRepo2.observeHistory("owner_a").first()
            assertEquals(2, history.size)
            val prs = historyRepo2.observePersonalRecords("owner_a").first()
            assertEquals(2, prs.size)
            assertNotNull(sessionRepo2.observeActiveSession("owner_a").first())
        } finally {
            db2.close()
            dbFile.delete()
        }
    }

    @Test
    fun repeatWorkout_concurrentRequests_rollbackOnMidInsertFailure_andUnauthorizedRejection() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbFile = File(context.filesDir, "p9_repeat_integrity_${System.nanoTime()}.db")
        val db = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath)
            .addMigrations(PulseDatabase.MIGRATION_1_2, PulseDatabase.MIGRATION_2_3)
            .build()
        try {
            val tx = RoomTransactionRunner(db)
            val exRepo = RoomExerciseRepository(db.exerciseDao())
            val planRepo = RoomWorkoutPlanRepository(db.workoutPlanDao(), db.exerciseDao(), tx)
            val sessionRepo = RoomWorkoutSessionRepository(db.workoutSessionDao(), db.workoutPlanDao(), db.exerciseDao(), tx)

            val bench = exRepo.observeAll().first().first { it.name.contains("Bench", ignoreCase = true) }
            val plan = (planRepo.createPlan(
                ownerId = "owner_a",
                draft = WorkoutPlanDraft(
                    name = "Original Plan",
                    exercises = listOf(
                        PlanExerciseDraft(bench.id, 0, plannedSets = listOf(PlannedSetDraft(1, 10, WeightLoad.fromKilograms(80.0), 90))),
                    ),
                ),
            ) as DomainResult.Success).data

            val s1 = (sessionRepo.startSessionFromPlan("owner_a", plan.id) as DomainResult.Success).data
            sessionRepo.recordPerformedSet("owner_a", RecordSetRequest(s1.id, s1.exercises[0].id, 1, 10, 0, WeightLoad.fromKilograms(80.0)))
            sessionRepo.completeSession("owner_a", s1.id)

            // 1. Edit original plan -> repeating s1 retains original snapshot (80 kg), not edited (120 kg)
            planRepo.updatePlan(
                ownerId = "owner_a",
                planId = plan.id,
                draft = WorkoutPlanDraft(
                    name = "Edited Plan",
                    exercises = listOf(
                        PlanExerciseDraft(bench.id, 0, plannedSets = listOf(PlannedSetDraft(1, 5, WeightLoad.fromKilograms(120.0), 60))),
                    ),
                ),
            )

            // 2. Unauthorized / invalid repeat requests rejected
            val wrongOwner = sessionRepo.startSessionFromHistory("owner_b", s1.id)
            assertTrue(wrongOwner is DomainResult.Failure && wrongOwner.error is DataError.NotFound)
            val blankOwner = sessionRepo.startSessionFromHistory(" ", s1.id)
            assertTrue(blankOwner is DomainResult.Failure && blankOwner.error is DataError.ValidationError)

            // 3. Mid-transaction insertion failure -> full rollback (no orphan ACTIVE session)
            val failingDao = object : WorkoutSessionDao by db.workoutSessionDao() {
                override suspend fun insertSessionExercises(exercises: List<SessionExerciseEntity>) {
                    throw IllegalStateException("Simulated insertion failure")
                }
            }
            val failingRepo = RoomWorkoutSessionRepository(failingDao, db.workoutPlanDao(), db.exerciseDao(), tx)
            val failedRepeat = failingRepo.startSessionFromHistory("owner_a", s1.id)
            assertTrue(failedRepeat is DomainResult.Failure && failedRepeat.error is DataError.StorageError)
            assertNull(db.workoutSessionDao().getActiveSession("owner_a"))

            // 4. Two concurrent Repeat Workout requests -> only one succeeds, one gets ActiveSessionConflict
            val (r1, r2) = awaitAll(
                async { sessionRepo.startSessionFromHistory("owner_a", s1.id) },
                async { sessionRepo.startSessionFromHistory("owner_a", s1.id) },
            )
            val results = listOf(r1, r2)
            assertEquals(1, results.count { it is DomainResult.Success })
            assertEquals(1, results.count { it is DomainResult.Failure && it.error is DataError.ActiveSessionConflict })
            val activeFromRepeat = (results.first { it is DomainResult.Success } as DomainResult.Success).data
            assertEquals("Original Plan", activeFromRepeat.planNameSnapshot)
            assertEquals(80.0, activeFromRepeat.exercises[0].plannedSets[0].targetLoad?.toKilograms() ?: 0.0, 0.01)
            assertEquals(0, activeFromRepeat.exercises.sumOf { it.performedSets.size })
            assertNull(sessionRepo.observeRestState("owner_a", activeFromRepeat.id).first())

            // Clean up active session, then test Repeat vs Start Workout in both execution orders & concurrently
            sessionRepo.endSessionEarly("owner_a", activeFromRepeat.id)

            // Order A: Repeat first, then Start same plan -> same-plan resume policy returns same ACTIVE session
            val orderARepeat = (sessionRepo.startSessionFromHistory("owner_a", s1.id) as DomainResult.Success).data
            val orderAStart = (sessionRepo.startSessionFromPlan("owner_a", plan.id) as DomainResult.Success).data
            assertEquals(orderARepeat.id, orderAStart.id)
            assertEquals(orderARepeat.id, db.workoutSessionDao().getActiveSession("owner_a")?.session?.id)
            sessionRepo.endSessionEarly("owner_a", orderARepeat.id)

            // Order B: Start first, then Repeat -> Start wins and Repeat gets ActiveSessionConflict for same session
            val orderBStart = (sessionRepo.startSessionFromPlan("owner_a", plan.id) as DomainResult.Success).data
            val orderBRepeat = sessionRepo.startSessionFromHistory("owner_a", s1.id)
            assertTrue(orderBRepeat is DomainResult.Failure && orderBRepeat.error is DataError.ActiveSessionConflict)
            assertEquals(orderBStart.id, (orderBRepeat as DomainResult.Failure).let { (it.error as DataError.ActiveSessionConflict).activeSessionId })
            assertEquals(orderBStart.id, db.workoutSessionDao().getActiveSession("owner_a")?.session?.id)
            sessionRepo.endSessionEarly("owner_a", orderBStart.id)

            // Concurrent execution: either same-plan resume returns same session, or Start wins and Repeat gets conflict
            val (c1, c2) = awaitAll(
                async { sessionRepo.startSessionFromHistory("owner_a", s1.id) },
                async { sessionRepo.startSessionFromPlan("owner_a", plan.id) },
            )
            val persistedActiveId = db.workoutSessionDao().getActiveSession("owner_a")!!.session.id
            val successes = listOf(c1, c2).filterIsInstance<DomainResult.Success<com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession>>()
            assertTrue(successes.isNotEmpty())
            assertTrue(successes.all { it.data.id == persistedActiveId })
            listOf(c1, c2).filterIsInstance<DomainResult.Failure>().forEach { failure ->
                val conflict = failure.error as DataError.ActiveSessionConflict
                assertEquals(persistedActiveId, conflict.activeSessionId)
            }

            // Original historical session s1 remains untouched
            val reloadedS1 = db.workoutSessionDao().getSession("owner_a", s1.id)!!
            assertEquals(SessionStatus.COMPLETED, reloadedS1.session.status)
            assertEquals(1, reloadedS1.exercises.sumOf { it.performedSets.size })
        } finally {
            db.close()
            dbFile.delete()
        }
    }
}
