package com.ayasheta.pulsesmartgym.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase
import com.ayasheta.pulsesmartgym.core.data.local.database.RoomTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.WorkoutSessionDao
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.RepRecordEntity
import com.ayasheta.pulsesmartgym.core.data.repository.exercise.RoomExerciseRepository
import com.ayasheta.pulsesmartgym.core.data.repository.history.RoomWorkoutHistoryRepository
import com.ayasheta.pulsesmartgym.core.data.repository.session.RoomWorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.data.repository.workout.RoomWorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class P10RepRecordsAndIsolationRoomTest {

    @Test
    fun repRecords_persistAcrossReopen_validateDuplicates_rollbackOnFailure_andPreserveOnWeightEdit() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.filesDir, "p10_rep_records_${System.nanoTime()}.db")
        val db1 = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath)
            .addMigrations(PulseDatabase.MIGRATION_1_2, PulseDatabase.MIGRATION_2_3)
            .build()

        val sessionId: String
        val exerciseId: String
        try {
            val tx = RoomTransactionRunner(db1)
            val exRepo = RoomExerciseRepository(db1.exerciseDao())
            val planRepo = RoomWorkoutPlanRepository(db1.workoutPlanDao(), db1.exerciseDao(), tx)
            val sessionRepo = RoomWorkoutSessionRepository(db1.workoutSessionDao(), db1.workoutPlanDao(), db1.exerciseDao(), tx)

            val bench = exRepo.observeAll().first().first { it.name.contains("Bench", ignoreCase = true) }
            val plan = (planRepo.createPlan(
                ownerId = "owner_a",
                draft = WorkoutPlanDraft(
                    name = "P10 Bench",
                    exercises = listOf(
                        PlanExerciseDraft(
                            bench.id, 0,
                            plannedSets = listOf(
                                PlannedSetDraft(1, 10, WeightLoad.fromKilograms(80.0), 60),
                                PlannedSetDraft(2, 10, WeightLoad.fromKilograms(80.0), 60),
                            ),
                        ),
                    ),
                ),
            ) as DomainResult.Success).data

            val s1 = (sessionRepo.startSessionFromPlan("owner_a", plan.id) as DomainResult.Success).data
            sessionId = s1.id
            exerciseId = s1.exercises[0].id

            // 1. Reject duplicate rep index
            val dupIdxRes = sessionRepo.recordPerformedSet(
                "owner_a",
                RecordSetRequest(
                    sessionId, exerciseId, 1, 2, 0, WeightLoad.fromKilograms(80.0),
                    TrackingMode.DEVICE_ASSISTED, MeasurementOrigin.SIMULATED_TELEMETRY, 12_000L,
                    repRecords = listOf(
                        RepMeasurement(1, RepClassification.FULL, 1_500L, origin = MeasurementOrigin.SIMULATED_TELEMETRY),
                        RepMeasurement(1, RepClassification.FULL, 1_500L, origin = MeasurementOrigin.SIMULATED_TELEMETRY),
                    ),
                ),
            )
            assertTrue(dupIdxRes is RecordSetResult.ValidationError)

            // 2. Rollback on rep-record insertion failure -> no orphan performed set
            val failingDao = object : WorkoutSessionDao by db1.workoutSessionDao() {
                override suspend fun insertRepRecords(records: List<RepRecordEntity>) {
                    throw IllegalStateException("Simulated rep insert failure")
                }
            }
            val failingRepo = RoomWorkoutSessionRepository(failingDao, db1.workoutPlanDao(), db1.exerciseDao(), tx)
            val rollbackFailed = runCatching {
                failingRepo.recordPerformedSet(
                    "owner_a",
                    RecordSetRequest(
                        sessionId, exerciseId, 1, 1, 0, WeightLoad.fromKilograms(80.0),
                        TrackingMode.DEVICE_ASSISTED, MeasurementOrigin.SIMULATED_TELEMETRY, 5_000L,
                        repRecords = listOf(RepMeasurement(1, RepClassification.FULL, 1_500L, origin = MeasurementOrigin.SIMULATED_TELEMETRY)),
                    ),
                )
            }.isFailure
            assertTrue(rollbackFailed)
            assertEquals(0, sessionRepo.getSession("owner_a", sessionId)!!.exercises[0].performedSets.size)

            // 3. Valid set with mixed SIMULATED_TELEMETRY + MANUAL_CORRECTION & null optional ROMs
            val validRecords = listOf(
                RepMeasurement(1, RepClassification.FULL, totalDurationMs = 1_600L, origin = MeasurementOrigin.SIMULATED_TELEMETRY),
                RepMeasurement(2, RepClassification.PARTIAL, totalDurationMs = null, origin = MeasurementOrigin.MANUAL_CORRECTION),
            )
            val recRes = sessionRepo.recordPerformedSet(
                "owner_a",
                RecordSetRequest(
                    sessionId, exerciseId, 1, 1, 1, WeightLoad.fromKilograms(80.0),
                    TrackingMode.DEVICE_ASSISTED, MeasurementOrigin.SIMULATED_TELEMETRY, 18_000L,
                    repRecords = validRecords,
                ),
            )
            assertTrue(recRes is RecordSetResult.Recorded)

            // Duplicate submission prevented
            val dupSet = sessionRepo.recordPerformedSet(
                "owner_a",
                RecordSetRequest(sessionId, exerciseId, 1, 1, 1, WeightLoad.fromKilograms(80.0), TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT, 1000L),
            )
            assertTrue(dupSet is RecordSetResult.AlreadyRecorded)

            // 4. Weight-only correction preserves repRecords & durationMs
            val weightEdited = (sessionRepo.correctPerformedSet(
                "owner_a",
                RecordSetRequest(sessionId, exerciseId, 1, 1, 1, WeightLoad.fromKilograms(85.0), TrackingMode.DEVICE_ASSISTED, MeasurementOrigin.SIMULATED_TELEMETRY, null, emptyList()),
            ) as DomainResult.Success).data
            assertEquals(85.0, weightEdited.actualLoad!!.toKilograms(), 0.01)
            assertEquals(18_000L, weightEdited.durationMs)
            assertEquals(2, weightEdited.repRecords.size)
            assertEquals(MeasurementOrigin.SIMULATED_TELEMETRY, weightEdited.repRecords[0].origin)
            assertNull(weightEdited.repRecords[0].angularRomDegrees)

            // 5. Another owner cannot mutate set
            val wrongOwner = sessionRepo.correctPerformedSet(
                "owner_b",
                RecordSetRequest(sessionId, exerciseId, 1, 1, 1, WeightLoad.fromKilograms(90.0), TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT, null),
            )
            assertTrue(wrongOwner is DomainResult.Failure && wrongOwner.error is DataError.NotFound)

            // 6. Count correction without new repRecords clears contradictory old repRecords
            val countEdited = (sessionRepo.correctPerformedSet(
                "owner_a",
                RecordSetRequest(sessionId, exerciseId, 1, 5, 0, WeightLoad.fromKilograms(85.0), TrackingMode.MANUAL, MeasurementOrigin.MANUAL_CORRECTION, null, emptyList()),
            ) as DomainResult.Success).data
            assertEquals(5, countEdited.fullReps)
            assertTrue(countEdited.repRecords.isEmpty())
            assertEquals(18_000L, countEdited.durationMs)

            // Re-attach 1 rep record and complete session -> terminal rejects further writes
            sessionRepo.correctPerformedSet(
                "owner_a",
                RecordSetRequest(
                    sessionId, exerciseId, 1, 1, 0, WeightLoad.fromKilograms(85.0),
                    TrackingMode.DEVICE_ASSISTED, MeasurementOrigin.SIMULATED_TELEMETRY, null,
                    listOf(RepMeasurement(1, RepClassification.FULL, 1_400L, origin = MeasurementOrigin.SIMULATED_TELEMETRY)),
                ),
            )
            assertTrue(sessionRepo.endSessionEarly("owner_a", sessionId) is DomainResult.Success)
            val afterTerminal = sessionRepo.correctPerformedSet(
                "owner_a",
                RecordSetRequest(sessionId, exerciseId, 1, 2, 0, WeightLoad.fromKilograms(85.0), TrackingMode.MANUAL, MeasurementOrigin.MANUAL_INPUT, null),
            )
            assertTrue(afterTerminal is DomainResult.Failure && afterTerminal.error is DataError.InvalidSessionState)
        } finally {
            db1.close()
        }

        // Reopen DB and verify rep records survived
        val db2 = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath)
            .addMigrations(PulseDatabase.MIGRATION_1_2, PulseDatabase.MIGRATION_2_3)
            .build()
        try {
            val historyRepo2 = RoomWorkoutHistoryRepository(db2.workoutSessionDao())
            val loaded = historyRepo2.getSessionDetails("owner_a", sessionId)!!
            val reps = loaded.exercises[0].performedSets[0].repRecords
            assertEquals(1, reps.size)
            assertEquals(MeasurementOrigin.SIMULATED_TELEMETRY, reps[0].origin)
            assertNull(reps[0].angularRomDegrees)
        } finally {
            db2.close()
            dbFile.delete()
        }
    }

    @Test
    fun isolatedSimulatorContainer_leavesGenuineHistoryAndPrsUncontaminated() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val genuine = PulseDataContainer.createIsolatedForTesting(context)
        val sim = PulseDataContainer.createIsolatedForTesting(context)
        try {
            val genuineUser = (genuine.container.authRepository.continueAsGuest() as DomainResult.Success).data
            val simUser = (sim.container.authRepository.continueAsGuest() as DomainResult.Success).data
            val bench = sim.container.exerciseRepository.observeAll().first().first { it.name.contains("Bench", ignoreCase = true) }

            val simPlan = (sim.container.workoutPlanRepository.createPlan(
                simUser.id,
                WorkoutPlanDraft("Demo Plan", exercises = listOf(PlanExerciseDraft(bench.id, 0, plannedSets = listOf(PlannedSetDraft(1, 10, WeightLoad.fromKilograms(100.0), 60))))),
            ) as DomainResult.Success).data
            val simSession = (sim.container.workoutSessionRepository.startSessionFromPlan(simUser.id, simPlan.id) as DomainResult.Success).data
            sim.container.workoutSessionRepository.recordPerformedSet(
                simUser.id,
                RecordSetRequest(simSession.id, simSession.exercises[0].id, 1, 10, 0, WeightLoad.fromKilograms(100.0), TrackingMode.DEVICE_ASSISTED, MeasurementOrigin.SIMULATED_TELEMETRY, 20_000L),
            )
            sim.container.workoutSessionRepository.completeSession(simUser.id, simSession.id)

            // Genuine container has 0 sessions and 0 PRs
            assertTrue(genuine.container.workoutHistoryRepository.observeHistory(genuineUser.id).first().isEmpty())
            assertTrue(genuine.container.workoutHistoryRepository.observePersonalRecords(genuineUser.id).first().isEmpty())
            assertEquals(1, sim.container.workoutHistoryRepository.observeHistory(simUser.id).first().size)
        } finally {
            sim.close()
            genuine.close()
        }
    }

    @Test
    fun correctPerformedSet_preservesDeviceProvenanceOnWeightEdit_andHonorsExplicitCorrectionPolicies() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val isolated = PulseDataContainer.createIsolatedForTesting(context)
        try {
            val user = (isolated.container.authRepository.continueAsGuest() as DomainResult.Success).data
            val bench = isolated.container.exerciseRepository.observeAll().first().first { it.name.contains("Bench", ignoreCase = true) }
            val plan = (isolated.container.workoutPlanRepository.createPlan(
                user.id,
                WorkoutPlanDraft("Bench", exercises = listOf(PlanExerciseDraft(bench.id, 0, plannedSets = listOf(PlannedSetDraft(1, 3, WeightLoad.fromKilograms(80.0), 60))))),
            ) as DomainResult.Success).data
            val session = (isolated.container.workoutSessionRepository.startSessionFromPlan(user.id, plan.id) as DomainResult.Success).data
            val exId = session.exercises[0].id

            val mixedReps = listOf(
                RepMeasurement(1, RepClassification.FULL, totalDurationMs = 1_500L, origin = MeasurementOrigin.SIMULATED_TELEMETRY),
                RepMeasurement(2, RepClassification.PARTIAL, totalDurationMs = null, origin = MeasurementOrigin.MANUAL_CORRECTION),
            )
            isolated.container.workoutSessionRepository.recordPerformedSet(
                user.id,
                RecordSetRequest(session.id, exId, 1, 1, 1, WeightLoad.fromKilograms(80.0), TrackingMode.DEVICE_ASSISTED, MeasurementOrigin.SIMULATED_TELEMETRY, 12_000L, mixedReps),
            )

            // Weight-only edit even with default MANUAL request fields MUST preserve DEVICE_ASSISTED, SIMULATED_TELEMETRY, durationMs, and mixed repRecords
            val weightOnly = (isolated.container.workoutSessionRepository.correctPerformedSet(
                user.id,
                RecordSetRequest(
                    sessionId = session.id,
                    sessionExerciseId = exId,
                    setIndex = 1,
                    fullReps = 1,
                    partialReps = 1,
                    actualLoad = WeightLoad.fromKilograms(90.0),
                    trackingMode = TrackingMode.MANUAL,
                    origin = MeasurementOrigin.MANUAL_INPUT,
                    repCorrectionPolicy = com.ayasheta.pulsesmartgym.core.model.session.RepCorrectionPolicy.PRESERVE_IF_COUNTS_MATCH,
                ),
            ) as DomainResult.Success).data
            assertEquals(TrackingMode.DEVICE_ASSISTED, weightOnly.trackingMode)
            assertEquals(MeasurementOrigin.SIMULATED_TELEMETRY, weightOnly.origin)
            assertEquals(12_000L, weightOnly.durationMs)
            assertEquals(2, weightOnly.repRecords.size)

            // REPLACE_WITH_LEDGER with empty list is rejected
            val badReplace = isolated.container.workoutSessionRepository.correctPerformedSet(
                user.id,
                RecordSetRequest(
                    session.id, exId, 1, 2, 1, WeightLoad.fromKilograms(90.0),
                    TrackingMode.DEVICE_ASSISTED, MeasurementOrigin.SIMULATED_TELEMETRY, null,
                    emptyList(), com.ayasheta.pulsesmartgym.core.model.session.RepCorrectionPolicy.REPLACE_WITH_LEDGER,
                ),
            )
            assertTrue(badReplace is DomainResult.Failure && badReplace.error is DataError.ValidationError)

            // INVALIDATE_LEDGER explicitly clears repRecords
            val invalidated = (isolated.container.workoutSessionRepository.correctPerformedSet(
                user.id,
                RecordSetRequest(
                    session.id, exId, 1, 3, 0, WeightLoad.fromKilograms(90.0),
                    TrackingMode.MANUAL, MeasurementOrigin.MANUAL_CORRECTION, null,
                    emptyList(), com.ayasheta.pulsesmartgym.core.model.session.RepCorrectionPolicy.INVALIDATE_LEDGER,
                ),
            ) as DomainResult.Success).data
            assertEquals(3, invalidated.fullReps)
            assertTrue(invalidated.repRecords.isEmpty())
        } finally {
            isolated.close()
        }
    }
}
