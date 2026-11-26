package com.ayasheta.pulsesmartgym.core.data

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
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
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
class Migration2To3Test {

    private val testDbName = "pulse_migration_2_3_test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PulseDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate2To3_preservesExistingSessionAndCreatesRestTable() {
        helper.createDatabase(testDbName, 2).apply {
            execSQL(
                """
                INSERT INTO workout_sessions (id, ownerProfileId, sourcePlanId, planNameSnapshot, status, startedAtEpochMs, endedAtEpochMs)
                VALUES ('sess_v2', 'user_1', 'plan_1', 'Upper Power', 'ACTIVE', 2000, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            testDbName,
            3,
            true,
            PulseDatabase.MIGRATION_2_3,
        )
        migratedDb.query("SELECT id, planNameSnapshot FROM workout_sessions WHERE id = 'sess_v2'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Upper Power", cursor.getString(1))
        }
        migratedDb.query("SELECT COUNT(*) FROM session_rest_states").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migratedDb.close()
    }

    @Test
    fun restState_persistsAcrossDatabaseReopen_andIsOwnerAndSessionScoped() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbFile = File(context.filesDir, "p8_rest_persist_${System.nanoTime()}.db")
        var nowMs = 50_000L

        val db1 = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath)
            .addMigrations(PulseDatabase.MIGRATION_1_2, PulseDatabase.MIGRATION_2_3)
            .build()
        try {
            db1.openHelper.writableDatabase.execSQL(
                """
                INSERT INTO workout_sessions (id, ownerProfileId, sourcePlanId, planNameSnapshot, status, startedAtEpochMs, endedAtEpochMs)
                VALUES ('sess_a', 'owner_a', NULL, 'Push Day', 'ACTIVE', 1000, NULL)
                """.trimIndent(),
            )
            val repo1 = RoomWorkoutSessionRepository(
                sessionDao = db1.workoutSessionDao(),
                planDao = db1.workoutPlanDao(),
                exerciseDao = db1.exerciseDao(),
                transactionRunner = RoomTransactionRunner(db1),
                clockMs = { nowMs },
            )
            val started = repo1.startRestTimer(
                ownerId = "owner_a",
                sessionId = "sess_a",
                exerciseId = "ex_1",
                exerciseName = "Bench Press",
                completedSetIndex = 1,
                durationSeconds = 90,
                nextExerciseId = "ex_1",
                nextSetIndex = 2,
            )
            assertTrue(started is DomainResult.Success)
        } finally {
            db1.close()
        }

        // Reopen database (simulating process death) and verify rest timer survives
        val db2 = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath)
            .addMigrations(PulseDatabase.MIGRATION_1_2, PulseDatabase.MIGRATION_2_3)
            .build()
        try {
            val repo2 = RoomWorkoutSessionRepository(
                sessionDao = db2.workoutSessionDao(),
                planDao = db2.workoutPlanDao(),
                exerciseDao = db2.exerciseDao(),
                transactionRunner = RoomTransactionRunner(db2),
                clockMs = { nowMs },
            )
            val restored = repo2.observeRestState("owner_a", "sess_a").first()
            assertNotNull(restored)
            assertEquals(50_000L + 90_000L, restored!!.deadlineEpochMs)
            assertEquals("ex_1", restored.nextExerciseId)
            assertEquals(2, restored.nextSetIndex)

            // Wrong owner cannot see or clear rest state
            assertNull(repo2.observeRestState("owner_b", "sess_a").first())
            val extendResult = repo2.extendRestTimer("owner_a", "sess_a", 30)
            assertTrue(extendResult is DomainResult.Success)
            assertEquals(50_000L + 120_000L, (extendResult as DomainResult.Success).data.deadlineEpochMs)

            repo2.clearRestTimer("owner_a", "sess_a")
            assertNull(repo2.observeRestState("owner_a", "sess_a").first())
        } finally {
            db2.close()
            dbFile.delete()
        }
    }

    @Test
    fun correctPerformedSet_preservesRecordedDurationMs() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbFile = File(context.filesDir, "p8_correct_duration_${System.nanoTime()}.db")
        val db = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath)
            .addMigrations(PulseDatabase.MIGRATION_1_2, PulseDatabase.MIGRATION_2_3)
            .build()
        try {
            val writable = db.openHelper.writableDatabase
            writable.execSQL(
                "INSERT INTO workout_sessions (id, ownerProfileId, sourcePlanId, planNameSnapshot, status, startedAtEpochMs, endedAtEpochMs) VALUES ('sess_c', 'owner_a', NULL, 'Push', 'ACTIVE', 1000, NULL)",
            )
            writable.execSQL(
                "INSERT INTO session_exercises (id, sessionId, sourceExerciseId, exerciseNameSnapshot, muscleGroupSnapshot, equipmentSnapshot, trackingCategorySnapshot, orderIndex) VALUES ('ex_c', 'sess_c', NULL, 'Bench', 'CHEST', 'BARBELL', 'LINEAR', 0)",
            )
            writable.execSQL(
                "INSERT INTO session_planned_sets (id, sessionExerciseId, setIndex, targetReps, targetLoadGrams, targetRestSeconds) VALUES ('ps_c', 'ex_c', 1, 10, 80000, 90)",
            )

            val repo = RoomWorkoutSessionRepository(
                sessionDao = db.workoutSessionDao(),
                planDao = db.workoutPlanDao(),
                exerciseDao = db.exerciseDao(),
                transactionRunner = RoomTransactionRunner(db),
                clockMs = { 5_000L },
            )
            val recordResult = repo.recordPerformedSet(
                ownerId = "owner_a",
                request = RecordSetRequest(
                    sessionId = "sess_c",
                    sessionExerciseId = "ex_c",
                    setIndex = 1,
                    fullReps = 8,
                    partialReps = 0,
                    actualLoad = WeightLoad.fromKilograms(80.0),
                    trackingMode = TrackingMode.MANUAL,
                    origin = MeasurementOrigin.MANUAL_INPUT,
                    durationMs = 18_500L,
                ),
            )
            assertTrue(recordResult is RecordSetResult.Recorded)

            val corrected = repo.correctPerformedSet(
                ownerId = "owner_a",
                request = RecordSetRequest(
                    sessionId = "sess_c",
                    sessionExerciseId = "ex_c",
                    setIndex = 1,
                    fullReps = 9,
                    partialReps = 0,
                    actualLoad = WeightLoad.fromKilograms(82.5),
                    trackingMode = TrackingMode.MANUAL,
                    origin = MeasurementOrigin.MANUAL_INPUT,
                    durationMs = null,
                ),
            )
            assertTrue(corrected is DomainResult.Success)
            val updated = (corrected as DomainResult.Success).data
            assertEquals(9, updated.fullReps)
            assertEquals(18_500L, updated.durationMs)
        } finally {
            db.close()
            dbFile.delete()
        }
    }
}
