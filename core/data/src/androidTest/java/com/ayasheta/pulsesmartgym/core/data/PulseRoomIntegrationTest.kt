package com.ayasheta.pulsesmartgym.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase
import com.ayasheta.pulsesmartgym.core.data.local.preferences.PulsePreferencesDataSource
import com.ayasheta.pulsesmartgym.core.data.repository.exercise.RoomExerciseRepository
import com.ayasheta.pulsesmartgym.core.data.repository.history.RoomWorkoutHistoryRepository
import com.ayasheta.pulsesmartgym.core.data.repository.profile.DataStoreUserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.data.repository.session.RoomWorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.data.repository.workout.RoomWorkoutPlanRepository
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
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
class PulseRoomIntegrationTest {

    private lateinit var context: Context
    private lateinit var db: PulseDatabase
    private lateinit var exerciseRepo: RoomExerciseRepository
    private lateinit var planRepo: RoomWorkoutPlanRepository
    private lateinit var sessionRepo: RoomWorkoutSessionRepository
    private lateinit var historyRepo: RoomWorkoutHistoryRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        exerciseRepo = RoomExerciseRepository(db.exerciseDao())
        planRepo = RoomWorkoutPlanRepository(db.workoutPlanDao(), db.exerciseDao())
        sessionRepo = RoomWorkoutSessionRepository(db.workoutSessionDao(), db.workoutPlanDao(), db.exerciseDao())
        historyRepo = RoomWorkoutHistoryRepository(db.workoutSessionDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun createSamplePlan(name: String = "Upper Body Power"): com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlan {
        // Seed catalog
        val exercises = exerciseRepo.observeAll().first()
        assertTrue(exercises.isNotEmpty())
        val result = planRepo.createPlan(
            WorkoutPlanDraft(
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
        val plan = createSamplePlan()
        val session = (sessionRepo.startSessionFromPlan(plan.id) as DomainResult.Success).data
        assertEquals(SessionStatus.ACTIVE, session.status)

        val ex1 = session.exercises[0]
        val ex2 = session.exercises[1]

        // Out-of-order set execution: Set 3 before Set 1
        assertTrue(
            sessionRepo.recordPerformedSet(
                RecordSetRequest(session.id, ex1.id, setIndex = 3, fullReps = 8, partialReps = 1, actualLoad = WeightLoad.fromKilograms(85.0)),
            ) is RecordSetResult.Recorded,
        )
        assertTrue(
            sessionRepo.recordPerformedSet(
                RecordSetRequest(session.id, ex1.id, setIndex = 1, fullReps = 10, partialReps = 0, actualLoad = WeightLoad.fromKilograms(80.0)),
            ) is RecordSetResult.Recorded,
        )
        assertTrue(
            sessionRepo.recordPerformedSet(
                RecordSetRequest(session.id, ex1.id, setIndex = 2, fullReps = 10, partialReps = 0, actualLoad = WeightLoad.fromKilograms(80.0)),
            ) is RecordSetResult.Recorded,
        )
        assertTrue(
            sessionRepo.recordPerformedSet(
                RecordSetRequest(session.id, ex2.id, setIndex = 1, fullReps = 12, partialReps = 0, actualLoad = WeightLoad.fromKilograms(60.0)),
            ) is RecordSetResult.Recorded,
        )

        val completed = (sessionRepo.completeSession(session.id) as DomainResult.Success).data
        assertEquals(SessionStatus.COMPLETED, completed.status)

        val history = historyRepo.observeHistory().first()
        assertEquals(1, history.size)
        val summary = history.first()
        assertEquals("Upper Body Power", summary.planNameSnapshot)
        assertEquals(4, summary.completedSetsCount)
        assertEquals(40, summary.totalFullReps)
        assertEquals(1, summary.totalPartialReps)
        // 8*85 + 10*80 + 10*80 + 12*60 = 680 + 800 + 800 + 720 = 3000 kg
        assertEquals(3000.0, summary.totalVolumeKg, 0.001)
    }

    @Test
    fun duplicateFinishSet_andConcurrentFinishSet_preventDuplicatesWithoutReplace() = runTest {
        val plan = createSamplePlan()
        val session = (sessionRepo.startSessionFromPlan(plan.id) as DomainResult.Success).data
        val ex1 = session.exercises[0]

        val results = (1..5).map { attempt ->
            async {
                sessionRepo.recordPerformedSet(
                    RecordSetRequest(
                        sessionId = session.id,
                        sessionExerciseId = ex1.id,
                        setIndex = 1,
                        fullReps = 10 - attempt,
                        actualLoad = WeightLoad.fromKilograms(80.0),
                    ),
                )
            }
        }.awaitAll()

        assertEquals(1, results.count { it is RecordSetResult.Recorded })
        assertEquals(4, results.count { it is RecordSetResult.AlreadyRecorded })

        val reloaded = sessionRepo.getSession(session.id)!!
        assertEquals(1, reloaded.exercises[0].performedSets.size)
    }

    @Test
    fun partialSession_andExplicitCorrection_preserveOnlyActualWork() = runTest {
        val plan = createSamplePlan()
        val session = (sessionRepo.startSessionFromPlan(plan.id) as DomainResult.Success).data
        val ex1 = session.exercises[0]

        sessionRepo.recordPerformedSet(
            RecordSetRequest(session.id, ex1.id, setIndex = 1, fullReps = 7, actualLoad = WeightLoad.fromKilograms(75.0)),
        )
        // Explicit correction updates set 1 to 8 reps
        val corrected = sessionRepo.correctPerformedSet(
            RecordSetRequest(
                sessionId = session.id,
                sessionExerciseId = ex1.id,
                setIndex = 1,
                fullReps = 8,
                actualLoad = WeightLoad.fromKilograms(75.0),
                trackingMode = TrackingMode.MANUAL,
                origin = MeasurementOrigin.MANUAL_CORRECTION,
            ),
        )
        assertTrue(corrected is DomainResult.Success)

        // completeSession fails if not all planned sets are recorded
        assertTrue(sessionRepo.completeSession(session.id) is DomainResult.Failure)

        // endSessionEarly marks PARTIALLY_COMPLETED
        val ended = (sessionRepo.endSessionEarly(session.id) as DomainResult.Success).data
        assertEquals(SessionStatus.PARTIALLY_COMPLETED, ended.status)

        val partialHistory = historyRepo.observeHistory(SessionStatus.PARTIALLY_COMPLETED).first()
        assertEquals(1, partialHistory.size)
        assertEquals(1, partialHistory.first().completedSetsCount)
        assertEquals(8, partialHistory.first().totalFullReps)
        assertEquals(600.0, partialHistory.first().totalVolumeKg, 0.001)
    }

    @Test
    fun planModificationAndDeletion_doNotAffectCompletedSessionHistory() = runTest {
        val plan = createSamplePlan("Upper Body Power")
        val session = (sessionRepo.startSessionFromPlan(plan.id) as DomainResult.Success).data
        val ex1 = session.exercises[0]
        sessionRepo.recordPerformedSet(
            RecordSetRequest(session.id, ex1.id, setIndex = 1, fullReps = 10, actualLoad = WeightLoad.fromKilograms(80.0)),
        )
        sessionRepo.endSessionEarly(session.id)

        // Modify plan name & exercises
        val updateResult = planRepo.updatePlan(
            plan.id,
            WorkoutPlanDraft(
                name = "Renamed Leg Day",
                exercises = listOf(
                    PlanExerciseDraft(
                        exerciseId = "ex_back_squat",
                        orderIndex = 0,
                        plannedSets = listOf(PlannedSetDraft(1, 5)),
                    ),
                ),
            ),
        )
        assertTrue(updateResult is DomainResult.Success)

        // Delete plan completely
        assertTrue(planRepo.deletePlan(plan.id) is DomainResult.Success)
        assertNull(planRepo.getPlan(plan.id))

        // History and session snapshot remain intact with sourcePlanId = null
        val savedSession = historyRepo.getSessionDetails(session.id)
        assertNotNull(savedSession)
        assertNull(savedSession!!.sourcePlanId)
        assertEquals("Upper Body Power", savedSession.planNameSnapshot)
        assertEquals(2, savedSession.exercises.size)
        assertEquals(800.0, historyRepo.observeHistory().first().first().totalVolumeKg, 0.001)
    }

    @Test
    fun transactionRollback_andInvalidInputs_leaveNoPartialPlan() = runTest {
        exerciseRepo.observeAll().first()
        val badResult = planRepo.createPlan(
            WorkoutPlanDraft(
                name = "Invalid Plan",
                exercises = listOf(
                    PlanExerciseDraft(
                        exerciseId = "ex_nonexistent",
                        orderIndex = 0,
                        plannedSets = listOf(PlannedSetDraft(1, 10)),
                    ),
                ),
            ),
        )
        assertTrue(badResult is DomainResult.Failure)
        assertTrue(planRepo.observePlans().first().isEmpty())
    }

    @Test
    fun fileBackedDatabase_persistsAcrossCloseAndReopen() = runTest {
        val dbFile = File(context.filesDir, "pulse_restart_test.db")
        if (dbFile.exists()) dbFile.delete()

        val db1 = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath).build()
        val exRepo1 = RoomExerciseRepository(db1.exerciseDao())
        val planRepo1 = RoomWorkoutPlanRepository(db1.workoutPlanDao(), db1.exerciseDao())
        exRepo1.observeAll().first()
        val created = (planRepo1.createPlan(
            WorkoutPlanDraft(
                name = "Restart Proof Plan",
                exercises = listOf(
                    PlanExerciseDraft("ex_bench_press", 0, plannedSets = listOf(PlannedSetDraft(1, 10))),
                ),
            ),
        ) as DomainResult.Success).data
        db1.close()

        val db2 = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath).build()
        val planRepo2 = RoomWorkoutPlanRepository(db2.workoutPlanDao(), db2.exerciseDao())
        val loaded = planRepo2.getPlan(created.id)
        assertNotNull(loaded)
        assertEquals("Restart Proof Plan", loaded!!.name)
        db2.close()
        dbFile.delete()
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
