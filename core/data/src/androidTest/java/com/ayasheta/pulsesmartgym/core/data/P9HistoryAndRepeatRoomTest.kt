package com.ayasheta.pulsesmartgym.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase
import com.ayasheta.pulsesmartgym.core.data.local.database.RoomTransactionRunner
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
}
