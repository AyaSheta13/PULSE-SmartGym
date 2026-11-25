package com.ayasheta.pulsesmartgym.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase
import com.ayasheta.pulsesmartgym.core.data.local.database.RoomTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PlanExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.WorkoutPlanEntity
import com.ayasheta.pulsesmartgym.core.data.local.preferences.PulsePreferencesDataSource
import com.ayasheta.pulsesmartgym.core.data.repository.exercise.RoomExerciseRepository
import com.ayasheta.pulsesmartgym.core.data.repository.history.RoomWorkoutHistoryRepository
import com.ayasheta.pulsesmartgym.core.data.repository.profile.DataStoreUserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.data.repository.session.RoomWorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.data.repository.workout.RoomWorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlan
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
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
class PulseRoomIntegrationTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var db: PulseDatabase
    private lateinit var exerciseRepo: RoomExerciseRepository
    private lateinit var planRepo: RoomWorkoutPlanRepository
    private lateinit var sessionRepo: RoomWorkoutSessionRepository
    private lateinit var historyRepo: RoomWorkoutHistoryRepository

    private val ownerA = "user_alpha"
    private val ownerB = "user_beta"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.filesDir, "pulse_integration_test_${System.nanoTime()}.db")
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

    private suspend fun createSamplePlan(
        ownerId: String = ownerA,
        name: String = "Upper Body Power",
    ): WorkoutPlan {
        val exercises = exerciseRepo.observeAll().first()
        assertTrue(exercises.isNotEmpty())
        val result = planRepo.createPlan(
            ownerId = ownerId,
            draft = WorkoutPlanDraft(
                name = name,
                description = "Strength focus",
                exercises = listOf(
                    PlanExerciseDraft(
                        exerciseId = "ex_bench_press",
                        orderIndex = 0,
                        plannedSets = listOf(
                            PlannedSetDraft(setIndex = 1, targetReps = 10),
                            PlannedSetDraft(setIndex = 2, targetReps = 10),
                            PlannedSetDraft(setIndex = 3, targetReps = 8),
                        ),
                    ),
                    PlanExerciseDraft(
                        exerciseId = "ex_cable_row",
                        orderIndex = 1,
                        plannedSets = listOf(
                            PlannedSetDraft(setIndex = 1, targetReps = 12),
                        ),
                    ),
                ),
            ),
        )
        return (result as DomainResult.Success).data
    }

    @Test
    fun workoutLifecycle_createPlanStartSessionRecordSetsCompleteAndReadHistory() = runTest {
        val plan = createSamplePlan(ownerA)
        val session = (sessionRepo.startSessionFromPlan(ownerA, plan.id) as DomainResult.Success).data
        assertEquals(SessionStatus.ACTIVE, session.status)

        val ex1 = session.exercises[0]
        val ex2 = session.exercises[1]

        assertTrue(
            sessionRepo.recordPerformedSet(
                ownerA,
                RecordSetRequest(session.id, ex1.id, setIndex = 3, fullReps = 8, partialReps = 1, actualLoad = WeightLoad.fromKilograms(85.0)),
            ) is RecordSetResult.Recorded,
        )
        assertTrue(
            sessionRepo.recordPerformedSet(
                ownerA,
                RecordSetRequest(session.id, ex1.id, setIndex = 1, fullReps = 10, partialReps = 0, actualLoad = WeightLoad.fromKilograms(80.0)),
            ) is RecordSetResult.Recorded,
        )
        assertTrue(
            sessionRepo.recordPerformedSet(
                ownerA,
                RecordSetRequest(session.id, ex1.id, setIndex = 2, fullReps = 10, partialReps = 0, actualLoad = WeightLoad.fromKilograms(80.0)),
            ) is RecordSetResult.Recorded,
        )
        assertTrue(
            sessionRepo.recordPerformedSet(
                ownerA,
                RecordSetRequest(session.id, ex2.id, setIndex = 1, fullReps = 12, partialReps = 0, actualLoad = WeightLoad.fromKilograms(60.0)),
            ) is RecordSetResult.Recorded,
        )

        val completed = (sessionRepo.completeSession(ownerA, session.id) as DomainResult.Success).data
        assertEquals(SessionStatus.COMPLETED, completed.status)

        val history = historyRepo.observeHistory(ownerA).first()
        assertEquals(1, history.size)
        val summary = history.first()
        assertEquals("Upper Body Power", summary.planNameSnapshot)
        assertEquals(4, summary.completedSetsCount)
        assertEquals(40, summary.totalFullReps)
        assertEquals(1, summary.totalPartialReps)
        assertEquals(3000.0, summary.totalVolumeKg, 0.001)
    }

    @Test
    fun recordSetAfterSessionEnded_isRejectedWithInvalidState() = runTest {
        val plan = createSamplePlan(ownerA)
        val session = (sessionRepo.startSessionFromPlan(ownerA, plan.id) as DomainResult.Success).data
        val ex1 = session.exercises[0]

        val ended = sessionRepo.endSessionEarly(ownerA, session.id)
        assertTrue(ended is DomainResult.Success)
        assertEquals(SessionStatus.ABANDONED, (ended as DomainResult.Success).data.status)

        val postEndRecord = sessionRepo.recordPerformedSet(
            ownerA,
            RecordSetRequest(session.id, ex1.id, setIndex = 1, fullReps = 10, actualLoad = WeightLoad.fromKilograms(80.0)),
        )
        assertTrue(postEndRecord is RecordSetResult.InvalidState)
        assertEquals(0, sessionRepo.getSession(ownerA, session.id)!!.exercises[0].performedSets.size)
    }

    @Test
    fun concurrentDuplicateFinishSet_onDispatchersIO_recordsExactlyOnce() = runTest {
        val plan = createSamplePlan(ownerA)
        val session = (sessionRepo.startSessionFromPlan(ownerA, plan.id) as DomainResult.Success).data
        val ex1 = session.exercises[0]

        val results = withContext(Dispatchers.IO) {
            (1..8).map { attempt ->
                async {
                    sessionRepo.recordPerformedSet(
                        ownerA,
                        RecordSetRequest(
                            sessionId = session.id,
                            sessionExerciseId = ex1.id,
                            setIndex = 1,
                            fullReps = 10 + attempt,
                            actualLoad = WeightLoad.fromKilograms(80.0),
                        ),
                    )
                }
            }.awaitAll()
        }

        assertEquals(1, results.count { it is RecordSetResult.Recorded })
        assertEquals(7, results.count { it is RecordSetResult.AlreadyRecorded })
        assertEquals(1, sessionRepo.getSession(ownerA, session.id)!!.exercises[0].performedSets.size)
    }

    @Test
    fun concurrentRecordSetAndEndSession_onDispatchersIO_maintainsConsistentTerminalState() = runTest {
        repeat(5) { iteration ->
            val plan = createSamplePlan(ownerA, "Concurrent Race $iteration")
            val session = (sessionRepo.startSessionFromPlan(ownerA, plan.id) as DomainResult.Success).data
            val ex1 = session.exercises[0]

            val (recordResult, endResult) = withContext(Dispatchers.IO) {
                val recordDeferred = async {
                    sessionRepo.recordPerformedSet(
                        ownerA,
                        RecordSetRequest(session.id, ex1.id, setIndex = 1, fullReps = 10, actualLoad = WeightLoad.fromKilograms(80.0)),
                    )
                }
                val endDeferred = async {
                    sessionRepo.endSessionEarly(ownerA, session.id)
                }
                recordDeferred.await() to endDeferred.await()
            }

            assertTrue(endResult is DomainResult.Success)
            val finalSession = sessionRepo.getSession(ownerA, session.id)!!
            val recordedCount = finalSession.exercises[0].performedSets.size
            when (recordResult) {
                is RecordSetResult.Recorded -> {
                    assertEquals(1, recordedCount)
                    assertEquals(SessionStatus.PARTIALLY_COMPLETED, finalSession.status)
                }
                is RecordSetResult.InvalidState -> {
                    assertEquals(0, recordedCount)
                    assertEquals(SessionStatus.ABANDONED, finalSession.status)
                }
                else -> throw AssertionError("Unexpected recordResult: $recordResult")
            }
        }
    }

    @Test
    fun concurrentCompleteAndEndSession_onDispatchersIO_allowsExactlyOneTerminalTransition() = runTest {
        val plan = createSamplePlan(ownerA)
        val session = (sessionRepo.startSessionFromPlan(ownerA, plan.id) as DomainResult.Success).data
        val ex1 = session.exercises[0]
        val ex2 = session.exercises[1]

        // Record all 4 sets first so both completeSession and endSessionEarly are eligible
        sessionRepo.recordPerformedSet(ownerA, RecordSetRequest(session.id, ex1.id, 1, 10))
        sessionRepo.recordPerformedSet(ownerA, RecordSetRequest(session.id, ex1.id, 2, 10))
        sessionRepo.recordPerformedSet(ownerA, RecordSetRequest(session.id, ex1.id, 3, 8))
        sessionRepo.recordPerformedSet(ownerA, RecordSetRequest(session.id, ex2.id, 1, 12))

        val (completeResult, endResult) = withContext(Dispatchers.IO) {
            val completeDeferred = async { sessionRepo.completeSession(ownerA, session.id) }
            val endDeferred = async { sessionRepo.endSessionEarly(ownerA, session.id) }
            completeDeferred.await() to endDeferred.await()
        }

        val successes = listOf(completeResult, endResult).count { it is DomainResult.Success }
        val failures = listOf(completeResult, endResult).count { it is DomainResult.Failure }
        assertEquals(1, successes)
        assertEquals(1, failures)
    }

    @Test
    fun concurrentPlanMutationDuringSessionStart_neverProducesHalfWrittenSession() = runTest {
        val plan = createSamplePlan(ownerA)

        val (startResult, deleteResult) = withContext(Dispatchers.IO) {
            val startDeferred = async { sessionRepo.startSessionFromPlan(ownerA, plan.id) }
            val deleteDeferred = async { planRepo.deletePlan(ownerA, plan.id) }
            startDeferred.await() to deleteDeferred.await()
        }

        assertTrue(deleteResult is DomainResult.Success)
        when (startResult) {
            is DomainResult.Success -> {
                // Started before delete -> must have full 2 exercises & 4 planned sets snapshot
                val persisted = sessionRepo.getSession(ownerA, startResult.data.id)!!
                assertEquals(2, persisted.exercises.size)
                assertEquals(4, persisted.exercises.sumOf { it.plannedSets.size })
            }
            is DomainResult.Failure -> {
                assertTrue(startResult.error is DataError.NotFound)
            }
        }
    }

    @Test
    fun ownerIsolation_preventsCrossOwnerReadsMutationsAndHistoryAccess() = runTest {
        val planA = createSamplePlan(ownerA, "Alpha Plan")
        val sessionA = (sessionRepo.startSessionFromPlan(ownerA, planA.id) as DomainResult.Success).data
        val ex1 = sessionA.exercises[0]

        // Owner B cannot see or mutate Owner A's plan
        assertTrue(planRepo.observePlans(ownerB).first().isEmpty())
        assertNull(planRepo.getPlan(ownerB, planA.id))
        assertTrue(planRepo.deletePlan(ownerB, planA.id) is DomainResult.Failure)
        assertTrue(
            planRepo.updatePlan(
                ownerB,
                planA.id,
                WorkoutPlanDraft("Hijacked", exercises = emptyList()),
            ) is DomainResult.Failure,
        )

        // Owner B cannot start session from Owner A's plan or access/mutate Owner A's session
        assertTrue(sessionRepo.startSessionFromPlan(ownerB, planA.id) is DomainResult.Failure)
        assertNull(sessionRepo.observeActiveSession(ownerB).first())
        assertNull(sessionRepo.getSession(ownerB, sessionA.id))
        assertTrue(
            sessionRepo.recordPerformedSet(
                ownerB,
                RecordSetRequest(sessionA.id, ex1.id, setIndex = 1, fullReps = 10),
            ) is RecordSetResult.NotFound,
        )
        assertTrue(sessionRepo.endSessionEarly(ownerB, sessionA.id) is DomainResult.Failure)

        // Owner A finishes session -> Owner B still sees empty history & cannot view details
        sessionRepo.recordPerformedSet(ownerA, RecordSetRequest(sessionA.id, ex1.id, 1, 10, actualLoad = WeightLoad.fromKilograms(80.0)))
        sessionRepo.endSessionEarly(ownerA, sessionA.id)

        assertEquals(1, historyRepo.observeHistory(ownerA).first().size)
        assertTrue(historyRepo.observeHistory(ownerB).first().isEmpty())
        assertNull(historyRepo.getSessionDetails(ownerB, sessionA.id))
    }

    @Test
    fun genuineTransactionRollback_afterFirstWrite_leavesZeroPersistedRows() = runTest {
        exerciseRepo.observeAll().first()

        val thrown = runCatching {
            db.withTransaction {
                db.workoutPlanDao().insertPlan(
                    WorkoutPlanEntity(
                        id = "rollback_plan_1",
                        ownerProfileId = ownerA,
                        name = "Partial Plan",
                        description = null,
                        createdAtEpochMs = 1L,
                        updatedAtEpochMs = 1L,
                    ),
                )
                // Valid first child write succeeded; next write violates FK constraint on missing exerciseId
                db.workoutPlanDao().insertPlanExercises(
                    listOf(
                        PlanExerciseEntity(
                            id = "rollback_ex_1",
                            planId = "rollback_plan_1",
                            exerciseId = "ex_does_not_exist",
                            orderIndex = 0,
                            notes = null,
                        ),
                    ),
                )
            }
        }

        assertTrue(thrown.isFailure)
        assertNull(planRepo.getPlan(ownerA, "rollback_plan_1"))
        assertTrue(planRepo.observePlans(ownerA).first().isEmpty())
    }

    @Test
    fun planModificationAndDeletion_doNotAffectCompletedSessionHistory() = runTest {
        val plan = createSamplePlan(ownerA, "Upper Body Power")
        val session = (sessionRepo.startSessionFromPlan(ownerA, plan.id) as DomainResult.Success).data
        val ex1 = session.exercises[0]
        sessionRepo.recordPerformedSet(
            ownerA,
            RecordSetRequest(session.id, ex1.id, setIndex = 1, fullReps = 10, actualLoad = WeightLoad.fromKilograms(80.0)),
        )
        sessionRepo.endSessionEarly(ownerA, session.id)

        assertTrue(planRepo.deletePlan(ownerA, plan.id) is DomainResult.Success)
        assertNull(planRepo.getPlan(ownerA, plan.id))

        val savedSession = historyRepo.getSessionDetails(ownerA, session.id)
        assertNotNull(savedSession)
        assertNull(savedSession!!.sourcePlanId)
        assertEquals("Upper Body Power", savedSession.planNameSnapshot)
        assertEquals(800.0, historyRepo.observeHistory(ownerA).first().first().totalVolumeKg, 0.001)
    }

    @Test
    fun explicitCorrection_updatesPerformedSetWithoutDuplicate() = runTest {
        val plan = createSamplePlan(ownerA)
        val session = (sessionRepo.startSessionFromPlan(ownerA, plan.id) as DomainResult.Success).data
        val ex1 = session.exercises[0]

        sessionRepo.recordPerformedSet(
            ownerA,
            RecordSetRequest(session.id, ex1.id, setIndex = 1, fullReps = 7, actualLoad = WeightLoad.fromKilograms(75.0)),
        )
        val corrected = sessionRepo.correctPerformedSet(
            ownerA,
            RecordSetRequest(
                sessionId = session.id,
                sessionExerciseId = ex1.id,
                setIndex = 1,
                fullReps = 9,
                actualLoad = WeightLoad.fromKilograms(77.5),
                trackingMode = TrackingMode.MANUAL,
                origin = MeasurementOrigin.MANUAL_CORRECTION,
            ),
        )
        assertTrue(corrected is DomainResult.Success)
        val reloaded = sessionRepo.getSession(ownerA, session.id)!!
        assertEquals(1, reloaded.exercises[0].performedSets.size)
        assertEquals(9, reloaded.exercises[0].performedSets[0].fullReps)
        assertEquals(77_500L, reloaded.exercises[0].performedSets[0].actualLoad?.grams)
    }

    @Test
    fun fileBackedDatabase_persistsAcrossCloseAndReopen() = runTest {
        val restartFile = File(context.filesDir, "pulse_restart_test.db")
        if (restartFile.exists()) restartFile.delete()

        val db1 = Room.databaseBuilder(context, PulseDatabase::class.java, restartFile.absolutePath).build()
        val exRepo1 = RoomExerciseRepository(db1.exerciseDao())
        val planRepo1 = RoomWorkoutPlanRepository(db1.workoutPlanDao(), db1.exerciseDao(), RoomTransactionRunner(db1))
        exRepo1.observeAll().first()
        val created = (planRepo1.createPlan(
            ownerA,
            WorkoutPlanDraft(
                name = "Restart Proof Plan",
                exercises = listOf(
                    PlanExerciseDraft("ex_bench_press", 0, plannedSets = listOf(PlannedSetDraft(1, 10))),
                ),
            ),
        ) as DomainResult.Success).data
        db1.close()

        val db2 = Room.databaseBuilder(context, PulseDatabase::class.java, restartFile.absolutePath).build()
        val planRepo2 = RoomWorkoutPlanRepository(db2.workoutPlanDao(), db2.exerciseDao(), RoomTransactionRunner(db2))
        val loaded = planRepo2.getPlan(ownerA, created.id)
        assertNotNull(loaded)
        assertEquals("Restart Proof Plan", loaded!!.name)
        db2.close()
        restartFile.delete()
    }

    @Test
    fun dataStorePreferences_persistsAndEmitsUpdates() = runTest {
        val prefsFile = File(context.filesDir, "pulse_prefs_test.preferences_pb")
        if (prefsFile.exists()) prefsFile.delete()

        val dataStore = PreferenceDataStoreFactory.create(produceFile = { prefsFile })
        val repo = DataStoreUserPreferencesRepository(PulsePreferencesDataSource(dataStore))

        repo.updatePreferredWeightUnit(WeightUnit.LBS)
        repo.setOnboardingCompleted(true)
        repo.updateDefaultRestSeconds(120)

        val prefs = repo.observePreferences().first()
        assertEquals(WeightUnit.LBS, prefs.preferredWeightUnit)
        assertTrue(prefs.onboardingCompleted)
        assertEquals(120, prefs.defaultRestSeconds)
        prefsFile.delete()
    }
}
