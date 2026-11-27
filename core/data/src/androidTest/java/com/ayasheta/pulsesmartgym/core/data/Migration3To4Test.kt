package com.ayasheta.pulsesmartgym.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase
import com.ayasheta.pulsesmartgym.core.data.local.database.RoomTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.repository.session.RoomWorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.history.WorkoutPerformanceAnalytics
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.RepCorrectionPolicy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class Migration3To4Test {

    private val testDbName = "pulse_migration_3_4_test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PulseDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate3To4_preservesExistingData_andLeavesExistingAccelerationNull() {
        helper.createDatabase(testDbName, 3).apply {
            execSQL(
                """
                INSERT INTO workout_sessions (id, ownerProfileId, sourcePlanId, planNameSnapshot, status, startedAtEpochMs, endedAtEpochMs)
                VALUES ('sess_v3', 'owner_1', 'plan_1', 'Push Day', 'COMPLETED', 1000, 50000)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO session_exercises (id, sessionId, sourceExerciseId, exerciseNameSnapshot, muscleGroupSnapshot, equipmentSnapshot, trackingCategorySnapshot, orderIndex)
                VALUES ('se_v3', 'sess_v3', 'ex_bench_press', 'Barbell Bench Press', 'CHEST', 'BARBELL', 'LINEAR', 0)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO performed_sets (id, sessionId, sessionExerciseId, setIndex, fullReps, partialReps, actualLoadGrams, completedAtEpochMs, trackingMode, origin, durationMs)
                VALUES ('ps_v3', 'sess_v3', 'se_v3', 1, 1, 0, 80000, 45000, 'DEVICE_ASSISTED', 'SENSOR_TELEMETRY', 15000)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO rep_records (id, performedSetId, repIndex, classification, totalDurationMs, concentricDurationMs, eccentricDurationMs, angularRomDegrees, linearRomCm, origin)
                VALUES ('rr_v3', 'ps_v3', 1, 'FULL', 1800, 800, 1000, NULL, 42.5, 'SENSOR_TELEMETRY')
                """.trimIndent(),
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            testDbName,
            4,
            true,
            PulseDatabase.MIGRATION_3_4,
        )

        migratedDb.query("SELECT planNameSnapshot FROM workout_sessions WHERE id = 'sess_v3'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Push Day", c.getString(0))
        }
        migratedDb.query(
            "SELECT linearRomCm, peakAccelerationMetersPerSecondSquared, origin FROM rep_records WHERE id = 'rr_v3'",
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(42.5, c.getDouble(0), 0.001)
            assertTrue(c.isNull(1))
            assertEquals("SENSOR_TELEMETRY", c.getString(2))
        }
        migratedDb.close()
    }

    @Test
    fun persistenceAndCorrections_surviveReopen_andIsolateGenuineFromSimulated() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.filesDir, "p11_persist_${System.nanoTime()}.db")
        var nowMs = 10_000L

        fun openDb(): PulseDatabase = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath)
            .addMigrations(PulseDatabase.MIGRATION_1_2, PulseDatabase.MIGRATION_2_3, PulseDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

        val db1 = openDb()
        val tx1 = RoomTransactionRunner(db1)
        val exRepo1 = com.ayasheta.pulsesmartgym.core.data.repository.exercise.RoomExerciseRepository(db1.exerciseDao())
        db1.exerciseDao().insertAllIgnore(
            listOf(
                com.ayasheta.pulsesmartgym.core.data.local.database.entity.ExerciseEntity(
                    id = "ex_bench_press",
                    name = "Barbell Bench Press",
                    muscleGroup = MuscleGroup.CHEST,
                    equipment = EquipmentCategory.BARBELL,
                    trackingCategory = TrackingCategory.LINEAR,
                    description = null,
                    imageKey = null,
                    isHardwareTrackingSupported = true,
                ),
            ),
        )
        val planRepo1 = com.ayasheta.pulsesmartgym.core.data.repository.workout.RoomWorkoutPlanRepository(
            db1.workoutPlanDao(),
            db1.exerciseDao(),
            tx1,
        )
        val repo1 = RoomWorkoutSessionRepository(
            sessionDao = db1.workoutSessionDao(),
            planDao = db1.workoutPlanDao(),
            exerciseDao = db1.exerciseDao(),
            transactionRunner = tx1,
            clockMs = { nowMs },
        )

        val planRes = planRepo1.createPlan(
            ownerId = "owner_gen",
            draft = com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft(
                name = "Upper Power",
                exercises = listOf(
                    com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft(
                        exerciseId = "ex_bench_press",
                        orderIndex = 0,
                        plannedSets = listOf(
                            com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft(1, 10, WeightLoad(80_000L), 90),
                            com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft(2, 10, WeightLoad(80_000L), 90),
                        ),
                    ),
                ),
            ),
        ) as DomainResult.Success
        val planId = planRes.data.id

        val started = repo1.startSessionFromPlan("owner_gen", planId) as DomainResult.Success
        val sessId = started.data.id
        val seId = started.data.exercises.first().id

        // Record set 1 with sensor measurements + manual fallback origin
        val reps = listOf(
            RepMeasurement(
                repIndex = 1,
                classification = RepClassification.FULL,
                totalDurationMs = 1750L,
                concentricDurationMs = 750L,
                eccentricDurationMs = 1000L,
                angularRomDegrees = 84.0,
                linearRomCm = 43.0,
                peakAccelerationMetersPerSecondSquared = 2.85,
                origin = MeasurementOrigin.SENSOR_TELEMETRY,
            ),
            RepMeasurement(
                repIndex = 2,
                classification = RepClassification.FULL,
                origin = MeasurementOrigin.MANUAL_CORRECTION,
            ),
        )
        assertTrue(
            repo1.recordPerformedSet(
                "owner_gen",
                RecordSetRequest(
                    sessionId = sessId,
                    sessionExerciseId = seId,
                    setIndex = 1,
                    fullReps = 2,
                    partialReps = 0,
                    actualLoad = WeightLoad(80_000L),
                    trackingMode = TrackingMode.MANUAL,
                    origin = MeasurementOrigin.MANUAL_CORRECTION,
                    durationMs = 14_000L,
                    repRecords = reps,
                ),
            ) is RecordSetResult.Recorded,
        )

        // Weight-only correction preserves measurements
        assertTrue(
            repo1.correctPerformedSet(
                "owner_gen",
                RecordSetRequest(
                    sessionId = sessId,
                    sessionExerciseId = seId,
                    setIndex = 1,
                    fullReps = 2,
                    partialReps = 0,
                    actualLoad = WeightLoad(85_000L),
                    trackingMode = TrackingMode.MANUAL,
                    origin = MeasurementOrigin.MANUAL_CORRECTION,
                    repCorrectionPolicy = RepCorrectionPolicy.PRESERVE_IF_COUNTS_MATCH,
                ),
            ) is DomainResult.Success,
        )

        // Failed transaction (0 reps validation error) must not corrupt existing record
        assertTrue(
            repo1.recordPerformedSet(
                "owner_gen",
                RecordSetRequest(
                    sessionId = sessId,
                    sessionExerciseId = seId,
                    setIndex = 2,
                    fullReps = 0,
                    partialReps = 0,
                    actualLoad = WeightLoad(80_000L),
                    trackingMode = TrackingMode.MANUAL,
                    origin = MeasurementOrigin.MANUAL_INPUT,
                ),
            ) is RecordSetResult.ValidationError,
        )
        db1.close()

        // Reopen database and verify persistence
        val db2 = openDb()
        val repo2 = RoomWorkoutSessionRepository(
            sessionDao = db2.workoutSessionDao(),
            planDao = db2.workoutPlanDao(),
            exerciseDao = db2.exerciseDao(),
            transactionRunner = RoomTransactionRunner(db2),
            clockMs = { nowMs },
        )
        val reopened = repo2.observeSession("owner_gen", sessId).first()!!
        val ex = reopened.exercises.first()
        assertEquals(1, ex.performedSets.size)
        val persistedSet = ex.performedSets.first()
        assertEquals(85_000L, persistedSet.actualLoad?.grams)
        assertEquals(2, persistedSet.repRecords.size)
        val rep1 = persistedSet.repRecords[0]
        assertEquals(2.85, rep1.peakAccelerationMetersPerSecondSquared!!, 0.001)
        assertEquals(84.0, rep1.angularRomDegrees!!, 0.001)
        assertEquals(43.0, rep1.linearRomCm!!, 0.001)
        assertEquals(1750L, rep1.totalDurationMs)
        assertEquals(750L, rep1.concentricDurationMs)
        assertEquals(1000L, rep1.eccentricDurationMs)
        assertEquals(MeasurementOrigin.SENSOR_TELEMETRY, rep1.origin)

        // Aggregate rep correction invalidates detailed measurements
        assertTrue(
            repo2.correctPerformedSet(
                "owner_gen",
                RecordSetRequest(
                    sessionId = sessId,
                    sessionExerciseId = seId,
                    setIndex = 1,
                    fullReps = 6,
                    partialReps = 0,
                    actualLoad = WeightLoad(85_000L),
                    trackingMode = TrackingMode.MANUAL,
                    origin = MeasurementOrigin.MANUAL_CORRECTION,
                    repCorrectionPolicy = RepCorrectionPolicy.INVALIDATE_LEDGER,
                ),
            ) is DomainResult.Success,
        )
        val afterInvalidation = repo2.observeSession("owner_gen", sessId).first()!!
            .exercises.first().performedSets.first()
        assertEquals(6, afterInvalidation.fullReps)
        assertTrue(afterInvalidation.repRecords.isEmpty())

        // Genuine analytics excludes simulator-originated sessions
        val completed = repo2.endSessionEarly("owner_gen", sessId) as DomainResult.Success
        val simSession = completed.data.copy(
            id = "sess_sim",
            exercises = listOf(
                completed.data.exercises.first().copy(
                    id = "se_sim",
                    sessionId = "sess_sim",
                    performedSets = listOf(
                        completed.data.exercises.first().performedSets.first().copy(
                            id = "ps_sim",
                            sessionId = "sess_sim",
                            origin = MeasurementOrigin.SIMULATED_TELEMETRY,
                            repRecords = listOf(
                                RepMeasurement(
                                    repIndex = 1,
                                    classification = RepClassification.FULL,
                                    linearRomCm = 99.0,
                                    peakAccelerationMetersPerSecondSquared = 9.9,
                                    origin = MeasurementOrigin.SIMULATED_TELEMETRY,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val genuineTrend = WorkoutPerformanceAnalytics.calculateExerciseTrends(
            sessions = listOf(completed.data, simSession),
            exerciseKey = "ex_bench_press",
            includeSimulated = false,
        )
        // Genuine session had measurements invalidated, and simulated session is excluded
        assertEquals(0, genuineTrend?.totalMeasuredReps ?: 0)

        db2.close()
        dbFile.delete()
    }

    @Test
    fun migrate4To5_createsOwnerSeedStateTable_andPreservesExistingPlans() {
        val dbName = "pulse_migration_4_5_test.db"
        helper.createDatabase(dbName, 4).apply {
            execSQL(
                """
                INSERT INTO workout_plans (id, ownerProfileId, name, description, createdAtEpochMs, updatedAtEpochMs)
                VALUES ('plan_v4', 'owner_v4', 'Custom V4 Plan', 'Existing user plan', 1000, 2000)
                """.trimIndent(),
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            dbName,
            5,
            true,
            PulseDatabase.MIGRATION_4_5,
        )

        migratedDb.query("SELECT name FROM workout_plans WHERE id = 'plan_v4'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Custom V4 Plan", c.getString(0))
        }
        migratedDb.execSQL(
            """
            INSERT INTO owner_seed_state (ownerProfileId, starterPlansSeeded, seededAtEpochMs)
            VALUES ('owner_v4', 1, 3000)
            """.trimIndent(),
        )
        migratedDb.query("SELECT starterPlansSeeded FROM owner_seed_state WHERE ownerProfileId = 'owner_v4'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
        migratedDb.close()
    }
}
