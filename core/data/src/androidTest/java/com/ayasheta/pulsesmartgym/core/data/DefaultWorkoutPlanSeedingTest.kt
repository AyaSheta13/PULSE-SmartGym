package com.ayasheta.pulsesmartgym.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase
import com.ayasheta.pulsesmartgym.core.data.local.database.RoomTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.local.preferences.PulsePreferencesDataSource
import com.ayasheta.pulsesmartgym.core.data.repository.workout.RoomWorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DefaultWorkoutPlanSeedingTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var prefsDir: File
    private lateinit var db: PulseDatabase
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var prefsDataSource: PulsePreferencesDataSource
    private lateinit var txRunner: RoomTransactionRunner

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.filesDir, "seed_test_${System.nanoTime()}.db")
        prefsDir = File(context.cacheDir, "seed_prefs_${System.nanoTime()}").apply { mkdirs() }
        db = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath).build()
        txRunner = RoomTransactionRunner(db)
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { File(prefsDir, "prefs.preferences_pb") },
        )
        prefsDataSource = PulsePreferencesDataSource(dataStore)
        kotlinx.coroutines.runBlocking {
            db.exerciseDao().insertAllIgnore(com.ayasheta.pulsesmartgym.core.data.local.database.ExerciseCatalogSeed.defaultExercises)
        }
    }

    @After
    fun tearDown() {
        db.close()
        dataStoreScope.cancel()
        if (dbFile.exists()) dbFile.delete()
        prefsDir.deleteRecursively()
    }

    private fun newPlanRepo(): RoomWorkoutPlanRepository = RoomWorkoutPlanRepository(
        workoutPlanDao = db.workoutPlanDao(),
        exerciseDao = db.exerciseDao(),
        transactionRunner = txRunner,
        preferencesDataSource = prefsDataSource,
        seedDefaultPlansOnFirstUse = true,
    )

    @Test
    fun firstUseSeedsThreeCrediblePlans_idempotentAcrossReopens_andRespectsDeletes() = runTest {
        val ownerId = "first_time_user"
        val repo1 = newPlanRepo()

        // 1. First observation seeds exactly 3 plans
        val initialPlans = repo1.observePlans(ownerId).first()
        assertEquals(3, initialPlans.size)
        assertEquals(
            setOf("Beginner Full Body", "Upper Body Strength", "Lower Body Foundation"),
            initialPlans.map { it.name }.toSet(),
        )
        assertTrue(initialPlans.all { it.exercises.isNotEmpty() })

        // 2. Re-observing and recreating repository instance never duplicates plans
        val repo2 = newPlanRepo()
        val secondRead = repo2.observePlans(ownerId).first()
        assertEquals(3, secondRead.size)

        // 3. Coexistence with user-created plan
        val customResult = repo2.createPlan(
            ownerId = ownerId,
            draft = WorkoutPlanDraft(
                name = "Custom Core Day",
                exercises = listOf(
                    PlanExerciseDraft(
                        exerciseId = "ex_bench_press",
                        orderIndex = 0,
                        plannedSets = listOf(PlannedSetDraft(setIndex = 1, targetReps = 10, targetRestSeconds = 60)),
                    ),
                ),
            ),
        )
        assertTrue(customResult is DomainResult.Success)
        assertEquals(4, repo2.observePlans(ownerId).first().size)

        // 4. Deleting all plans keeps empty state on subsequent launches (never re-seeds)
        for (plan in repo2.observePlans(ownerId).first()) {
            assertTrue(repo2.deletePlan(ownerId, plan.id) is DomainResult.Success)
        }
        assertEquals(0, repo2.observePlans(ownerId).first().size)

        val repo3 = newPlanRepo()
        assertEquals(0, repo3.observePlans(ownerId).first().size)
    }

    @Test
    fun existingUserWithCustomPlan_doesNotReceiveDuplicateDefaultPlans() = runTest {
        val ownerId = "existing_user"
        val repo = newPlanRepo()
        val created = repo.createPlan(
            ownerId = ownerId,
            draft = WorkoutPlanDraft(
                name = "My Existing Plan",
                exercises = listOf(
                    PlanExerciseDraft(
                        exerciseId = "ex_back_squat",
                        orderIndex = 0,
                        plannedSets = listOf(PlannedSetDraft(setIndex = 1, targetReps = 8, targetRestSeconds = 90)),
                    ),
                ),
            ),
        )
        assertTrue(created is DomainResult.Success)

        val plans = repo.observePlans(ownerId).first()
        assertEquals(1, plans.size)
        assertEquals("My Existing Plan", plans.first().name)
    }
}
