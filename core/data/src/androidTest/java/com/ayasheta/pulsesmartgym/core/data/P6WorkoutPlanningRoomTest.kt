package com.ayasheta.pulsesmartgym.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.data.local.database.ExerciseCatalogSeed
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase
import com.ayasheta.pulsesmartgym.core.data.local.database.RoomTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.repository.exercise.RoomExerciseRepository
import com.ayasheta.pulsesmartgym.core.data.repository.history.RoomWorkoutHistoryRepository
import com.ayasheta.pulsesmartgym.core.data.repository.session.RoomWorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.data.repository.workout.RoomWorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class P6WorkoutPlanningRoomTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var db: PulseDatabase
    private lateinit var exerciseRepo: RoomExerciseRepository
    private lateinit var planRepo: RoomWorkoutPlanRepository
    private lateinit var sessionRepo: RoomWorkoutSessionRepository
    private lateinit var historyRepo: RoomWorkoutHistoryRepository

    private val ownerA = "p6_owner_a"
    private val ownerB = "p6_owner_b"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.filesDir, "p6_room_test_${System.nanoTime()}.db")
        db = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath).build()
        val txRunner = RoomTransactionRunner(db)
        exerciseRepo = RoomExerciseRepository(db.exerciseDao())
        planRepo = RoomWorkoutPlanRepository(db.workoutPlanDao(), db.exerciseDao(), txRunner)
        sessionRepo = RoomWorkoutSessionRepository(db.workoutSessionDao(), db.workoutPlanDao(), db.exerciseDao(), txRunner)
        historyRepo = RoomWorkoutHistoryRepository(db.workoutSessionDao())
    }

    @After
    fun tearDown() {
        db.close()
        if (dbFile.exists()) dbFile.delete()
    }

    @Test
    fun catalogSeeding_upgradesPrePopulatedDatabaseIdempotentlyWithoutDuplicates() = runTest {
        // Simulate pre-P6 database containing only first 8 exercises
        val legacySubset = ExerciseCatalogSeed.defaultExercises.take(8)
        db.exerciseDao().insertAllIgnore(legacySubset)
        assertEquals(8, db.exerciseDao().count())

        // Create a plan referencing a legacy exercise ID before upgrade
        val planResult = planRepo.createPlan(
            ownerA,
            WorkoutPlanDraft(
                name = "Legacy Plan",
                exercises = listOf(
                    PlanExerciseDraft(
                        exerciseId = "ex_bench_press",
                        orderIndex = 0,
                        plannedSets = listOf(PlannedSetDraft(1, 10, null, 90)),
                    ),
                ),
            ),
        )
        assertTrue(planResult is DomainResult.Success)

        // Observing via RoomExerciseRepository upgrades catalog to all canonical exercises
        val allExercises = exerciseRepo.observeAll().first()
        assertEquals(ExerciseCatalogSeed.defaultExercises.size, allExercises.size)
        assertTrue(allExercises.any { it.id == "ex_leg_press" })
        assertTrue(allExercises.any { it.id == "ex_cable_fly" })

        // Re-observing in a fresh repo instance causes zero duplicates
        val secondRepo = RoomExerciseRepository(db.exerciseDao())
        val afterSecondSeed = secondRepo.observeAll().first()
        assertEquals(ExerciseCatalogSeed.defaultExercises.size, afterSecondSeed.size)

        // Existing plan remains valid
        val persistedPlan = planRepo.getPlan(ownerA, (planResult as DomainResult.Success).data.id)
        assertNotNull(persistedPlan)
        assertEquals("ex_bench_press", persistedPlan!!.exercises.first().exerciseId)
    }

    @Test
    fun planLifecycle_createUpdateDeletePreservesHistorySnapshotsAndOwnerIsolation() = runTest {
        exerciseRepo.observeAll().first()

        // Create plan with repeated exercise occurrences and mixed sets/loads
        val createResult = planRepo.createPlan(
            ownerA,
            WorkoutPlanDraft(
                name = "Upper Body Power",
                description = "Heavy + Backoff",
                exercises = listOf(
                    PlanExerciseDraft(
                        exerciseId = "ex_bench_press",
                        orderIndex = 0,
                        notes = "Pause reps",
                        plannedSets = listOf(
                            PlannedSetDraft(1, 8, WeightLoad.fromKilograms(80.0), 120),
                            PlannedSetDraft(2, 6, WeightLoad.fromKilograms(85.0), 120),
                        ),
                    ),
                    PlanExerciseDraft(
                        exerciseId = "ex_bench_press",
                        orderIndex = 1,
                        notes = "Backoff",
                        plannedSets = listOf(
                            PlannedSetDraft(1, 12, WeightLoad.fromKilograms(60.0), 60),
                        ),
                    ),
                    PlanExerciseDraft(
                        exerciseId = "ex_cable_row",
                        orderIndex = 2,
                        plannedSets = listOf(
                            PlannedSetDraft(1, 10, WeightLoad.fromKilograms(50.0), 90),
                        ),
                    ),
                ),
            ),
        )
        val created = (createResult as DomainResult.Success).data
        assertEquals(3, created.exercises.size)
        assertEquals(listOf(80_000L, 85_000L), created.exercises[0].plannedSets.map { it.targetLoad?.grams })

        // Start & end-early a historical session snapshot, AND start an active session snapshot
        val historicalSession = (sessionRepo.startSessionFromPlan(ownerA, created.id) as DomainResult.Success).data
        sessionRepo.recordPerformedSet(
            ownerA,
            RecordSetRequest(
                sessionId = historicalSession.id,
                sessionExerciseId = historicalSession.exercises.first().id,
                setIndex = 1,
                fullReps = 8,
                partialReps = 0,
                actualLoad = WeightLoad.fromKilograms(80.0),
                trackingMode = TrackingMode.MANUAL,
                origin = MeasurementOrigin.MANUAL_INPUT,
            ),
        )
        assertTrue(sessionRepo.endSessionEarly(ownerA, historicalSession.id) is DomainResult.Success)
        val activeSession = (sessionRepo.startSessionFromPlan(ownerA, created.id) as DomainResult.Success).data

        // Successful Room update: modify name, remove ex_cable_row, add ex_lateral_raise, update occurrence 0, keep occurrence 1 untouched
        val updateResult = planRepo.updatePlan(
            ownerA,
            created.id,
            WorkoutPlanDraft(
                name = "Upper Body Power v2",
                description = "Updated split",
                exercises = listOf(
                    PlanExerciseDraft(
                        exerciseId = "ex_bench_press",
                        orderIndex = 0,
                        notes = "Pause reps",
                        plannedSets = listOf(
                            PlannedSetDraft(1, 10, WeightLoad.fromKilograms(82.5), 105),
                            PlannedSetDraft(2, 8, WeightLoad.fromKilograms(85.0), 120),
                        ),
                    ),
                    PlanExerciseDraft(
                        exerciseId = "ex_bench_press",
                        orderIndex = 1,
                        notes = "Backoff",
                        plannedSets = listOf(
                            PlannedSetDraft(1, 12, WeightLoad.fromKilograms(60.0), 60),
                        ),
                    ),
                    PlanExerciseDraft(
                        exerciseId = "ex_lateral_raise",
                        orderIndex = 2,
                        plannedSets = listOf(
                            PlannedSetDraft(1, 15, WeightLoad.fromKilograms(10.0), 60),
                        ),
                    ),
                ),
            ),
        )
        assertTrue(updateResult is DomainResult.Success)

        // Reload full graph from Room and verify modified + untouched fields
        val reloaded = planRepo.getPlan(ownerA, created.id)!!
        assertEquals("Upper Body Power v2", reloaded.name)
        assertEquals("Updated split", reloaded.description)
        assertEquals(listOf("ex_bench_press", "ex_bench_press", "ex_lateral_raise"), reloaded.exercises.map { it.exerciseId })
        assertEquals(listOf(10, 8), reloaded.exercises[0].plannedSets.map { it.targetReps })
        assertEquals(listOf(82_500L, 85_000L), reloaded.exercises[0].plannedSets.map { it.targetLoad?.grams })
        assertEquals(listOf(105, 120), reloaded.exercises[0].plannedSets.map { it.targetRestSeconds })
        // Untouched occurrence 1 preserved
        assertEquals("Backoff", reloaded.exercises[1].notes)
        assertEquals(listOf(12), reloaded.exercises[1].plannedSets.map { it.targetReps })
        assertEquals(listOf(60_000L), reloaded.exercises[1].plannedSets.map { it.targetLoad?.grams })

        // Active and historical session snapshots retain original pre-update plan name and exercise count
        val activeAfterUpdate = sessionRepo.getSession(ownerA, activeSession.id)!!
        assertEquals("Upper Body Power", activeAfterUpdate.planNameSnapshot)
        assertEquals(3, activeAfterUpdate.exercises.size)

        // Owner B cannot read, update, or delete Owner A's plan
        assertNull(planRepo.getPlan(ownerB, created.id))
        val crossUpdate = planRepo.updatePlan(
            ownerB,
            created.id,
            WorkoutPlanDraft(
                name = "Hijacked",
                exercises = listOf(PlanExerciseDraft("ex_cable_row", 0, plannedSets = listOf(PlannedSetDraft(1, 10)))),
            ),
        )
        assertTrue(crossUpdate is DomainResult.Failure && crossUpdate.error is DataError.NotFound)
        assertTrue(planRepo.deletePlan(ownerB, created.id) is DomainResult.Failure)

        // Genuine mid-transaction rollback: install a SQLite trigger that fails AFTER updatePlan() and deletePlanExercisesByPlanId() have already executed
        db.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_mid_tx_planned_sets
            BEFORE INSERT ON planned_sets
            BEGIN
                SELECT RAISE(ABORT, 'Simulated mid-transaction write failure');
            END;
            """.trimIndent(),
        )
        var rollbackFailureCaught = false
        try {
            planRepo.updatePlan(
                ownerA,
                created.id,
                WorkoutPlanDraft(
                    name = "Should Roll Back",
                    description = "Should not persist",
                    exercises = listOf(
                        PlanExerciseDraft(
                            exerciseId = "ex_cable_row",
                            orderIndex = 0,
                            plannedSets = listOf(PlannedSetDraft(1, 12, null, 60)),
                        ),
                    ),
                ),
            )
        } catch (_: Throwable) {
            rollbackFailureCaught = true
        } finally {
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS fail_mid_tx_planned_sets")
        }
        assertTrue("Expected SQLite trigger exception during mid-transaction insert", rollbackFailureCaught)

        // Verify BOTH plan header AND child exercises/sets were rolled back atomically
        val afterRollback = planRepo.getPlan(ownerA, created.id)!!
        assertEquals("Upper Body Power v2", afterRollback.name)
        assertEquals("Updated split", afterRollback.description)
        assertEquals(3, afterRollback.exercises.size)
        assertEquals(listOf("ex_bench_press", "ex_bench_press", "ex_lateral_raise"), afterRollback.exercises.map { it.exerciseId })

        // Delete plan -> plan gone, historical workout session snapshot preserved!
        assertTrue(planRepo.deletePlan(ownerA, created.id) is DomainResult.Success)
        assertNull(planRepo.getPlan(ownerA, created.id))

        val history = historyRepo.observeHistory(ownerA).first()
        assertEquals(1, history.size)
        assertEquals("Upper Body Power", history.first().planNameSnapshot)
    }
}
