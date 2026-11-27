package com.ayasheta.pulsesmartgym.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.data.local.database.DefaultWorkoutPlanSeed
import com.ayasheta.pulsesmartgym.core.data.local.database.ExerciseCatalogSeed
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase
import com.ayasheta.pulsesmartgym.core.data.local.database.RoomTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.local.preferences.PulsePreferencesDataSource
import com.ayasheta.pulsesmartgym.core.data.repository.workout.RoomWorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.PulseValidators
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DefaultWorkoutPlanSeedingTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var prefsFile: File
    private var db: PulseDatabase? = null
    private var dataStoreScope: CoroutineScope? = null
    private var prefsDataSource: PulsePreferencesDataSource? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val id = System.nanoTime()
        dbFile = File(context.filesDir, "seed_test_$id.db")
        prefsFile = File(context.cacheDir, "seed_prefs_$id.preferences_pb")
        openResources()
    }

    @After
    fun tearDown() {
        closeResources()
        if (dbFile.exists()) dbFile.delete()
        if (prefsFile.exists()) prefsFile.delete()
    }

    private fun openResources() {
        val openedDb = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { prefsFile },
        )
        db = openedDb
        dataStoreScope = scope
        prefsDataSource = PulsePreferencesDataSource(dataStore)
        runBlocking {
            openedDb.exerciseDao().insertAllIgnore(ExerciseCatalogSeed.defaultExercises)
        }
    }

    private fun closeResources() {
        db?.close()
        db = null
        dataStoreScope?.cancel()
        dataStoreScope = null
        prefsDataSource = null
    }

    private fun newPlanRepo(
        customPrefs: PulsePreferencesDataSource = requireNotNull(prefsDataSource),
    ): RoomWorkoutPlanRepository {
        val currentDb = requireNotNull(db)
        return RoomWorkoutPlanRepository(
            workoutPlanDao = currentDb.workoutPlanDao(),
            exerciseDao = currentDb.exerciseDao(),
            transactionRunner = RoomTransactionRunner(currentDb),
            preferencesDataSource = customPrefs,
            seedDefaultPlansOnFirstUse = true,
            ownerSeedStateDao = currentDb.ownerSeedStateDao(),
        )
    }

    @Test
    fun starterDrafts_resolveAllCatalogExercises_andPassDomainValidators() {
        val catalogIds = ExerciseCatalogSeed.defaultExercises.map { it.id }.toSet()
        val drafts = DefaultWorkoutPlanSeed.defaultPlanDrafts()
        assertEquals(3, drafts.size)
        for (draft in drafts) {
            assertEquals(null, PulseValidators.validatePlanName(draft.name))
            assertTrue(draft.exercises.isNotEmpty())
            for (ex in draft.exercises) {
                assertTrue("Missing exercise ${ex.exerciseId}", ex.exerciseId in catalogIds)
                assertTrue(ex.plannedSets.isNotEmpty())
                for (set in ex.plannedSets) {
                    assertTrue(set.targetReps in 1..PulseValidators.MAX_TARGET_REPS)
                    assertTrue((set.targetRestSeconds ?: 0) in 0..PulseValidators.MAX_REST_SECONDS)
                }
            }
        }
    }

    @Test
    fun concurrentFirstUse_andGenuineDbReopen_seedOnce_andRespectEditsAndDeletes() = runTest {
        val guestOwner = "guest_owner_1"
        val registeredOwner = "account_owner_2"
        val repo1 = newPlanRepo()

        // 1. Concurrent first-time observation across multiple collectors
        val concurrentResults = coroutineScope {
            List(4) { async { repo1.observePlans(guestOwner).first() } }.awaitAll()
        }
        assertTrue(concurrentResults.all { it.size == 3 })
        assertEquals(
            setOf("Beginner Full Body", "Upper Body Strength", "Lower Body Foundation"),
            concurrentResults.first().map { it.name }.toSet(),
        )

        // 2. Owner isolation: registered account gets its own 3 starter plans
        val regPlans = repo1.observePlans(registeredOwner).first()
        assertEquals(3, regPlans.size)

        // 3. Edit one starter plan -> edit persists across genuine DB + DataStore reopen
        val fullBody = concurrentResults.first().first { it.name == "Beginner Full Body" }
        val editedDraft = WorkoutPlanDraft(
            name = "Beginner Full Body Edited",
            description = fullBody.description,
            exercises = fullBody.exercises.map { ex ->
                PlanExerciseDraft(
                    exerciseId = ex.exerciseId,
                    orderIndex = ex.orderIndex,
                    notes = ex.notes,
                    plannedSets = ex.plannedSets.map { s ->
                        PlannedSetDraft(s.setIndex, s.targetReps, s.targetLoad, s.targetRestSeconds)
                    },
                )
            },
        )
        assertTrue(repo1.updatePlan(guestOwner, fullBody.id, editedDraft) is DomainResult.Success)

        // 4. Delete one starter plan -> 2 plans remain
        val upper = concurrentResults.first().first { it.name == "Upper Body Strength" }
        assertTrue(repo1.deletePlan(guestOwner, upper.id) is DomainResult.Success)
        assertEquals(2, repo1.observePlans(guestOwner).first().size)

        // 5. Genuine close & reopen of Room DB and DataStore -> no duplicates, no re-seed of deleted plan
        closeResources()
        openResources()
        val repoAfterReopen = newPlanRepo()
        val reopenedGuestPlans = repoAfterReopen.observePlans(guestOwner).first()
        assertEquals(2, reopenedGuestPlans.size)
        assertTrue(reopenedGuestPlans.any { it.name == "Beginner Full Body Edited" })
        assertEquals(3, repoAfterReopen.observePlans(registeredOwner).first().size)

        // 6. Delete all remaining guest plans, close & reopen -> stays 0 plans (true empty state)
        for (plan in reopenedGuestPlans) {
            assertTrue(repoAfterReopen.deletePlan(guestOwner, plan.id) is DomainResult.Success)
        }
        assertEquals(0, repoAfterReopen.observePlans(guestOwner).first().size)

        closeResources()
        openResources()
        val repoAfterDeleteReopen = newPlanRepo()
        assertEquals(0, repoAfterDeleteReopen.observePlans(guestOwner).first().size)
        // Registered owner remains completely isolated
        assertEquals(3, repoAfterDeleteReopen.observePlans(registeredOwner).first().size)
    }

    @Test
    fun existingUserWithCustomPlan_andCrashRecoveryBeforeMarker_neverDuplicates() = runTest {
        val existingOwner = "existing_owner"
        val repo = newPlanRepo()
        val created = repo.createPlan(
            ownerId = existingOwner,
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

        val plans = repo.observePlans(existingOwner).first()
        assertEquals(1, plans.size)
        assertEquals("My Existing Plan", plans.first().name)

        // Simulate recovery when Room already committed starter plans before DataStore marker was written:
        // a brand-new DataStore (marker = false) with existing plans in Room must NOT insert duplicates.
        val currentDb = requireNotNull(db)
        val freshPrefsFile = File(context.cacheDir, "recovery_prefs_${System.nanoTime()}.preferences_pb")
        val recScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val freshPrefs = PulsePreferencesDataSource(
                PreferenceDataStoreFactory.create(scope = recScope, produceFile = { freshPrefsFile }),
            )
            val recoveredRepo = RoomWorkoutPlanRepository(
                workoutPlanDao = currentDb.workoutPlanDao(),
                exerciseDao = currentDb.exerciseDao(),
                transactionRunner = RoomTransactionRunner(currentDb),
                preferencesDataSource = freshPrefs,
                seedDefaultPlansOnFirstUse = true,
                ownerSeedStateDao = currentDb.ownerSeedStateDao(),
            )
            val recoveredPlans = recoveredRepo.observePlans(existingOwner).first()
            assertEquals(1, recoveredPlans.size)
            assertEquals(true, freshPrefs.readDefaultPlansSeededState(existingOwner))
        } finally {
            recScope.cancel()
            if (freshPrefsFile.exists()) freshPrefsFile.delete()
        }
    }

    @Test
    fun dataStoreMarkerWriteFailure_deleteAllAndGenuineReopen_neverReseeds() = runTest {
        val owner = "owner_write_fail"
        val realDataStore = PreferenceDataStoreFactory.create(
            scope = requireNotNull(dataStoreScope),
            produceFile = { File(context.cacheDir, "write_fail_${System.nanoTime()}.preferences_pb") },
        )
        val failingWritePrefs = object : PulsePreferencesDataSource(realDataStore) {
            override suspend fun markDefaultPlansSeeded(ownerId: String) {
                throw IOException("Simulated DataStore write failure")
            }
        }
        val repo1 = newPlanRepo(failingWritePrefs)
        val initialPlans = repo1.observePlans(owner).first()
        assertEquals(3, initialPlans.size)
        assertEquals(null, failingWritePrefs.readDefaultPlansSeededState(owner).let { if (it == false) null else it })

        // User deletes all 3 starter plans
        for (plan in initialPlans) {
            assertTrue(repo1.deletePlan(owner, plan.id) is DomainResult.Success)
        }
        assertEquals(0, repo1.observePlans(owner).first().size)

        // Genuine close & reopen of Room + DataStore (marker was never written to DataStore)
        closeResources()
        openResources()
        val repo2 = newPlanRepo()
        assertEquals(0, repo2.observePlans(owner).first().size)
    }

    @Test
    fun dataStoreMarkerReadFailure_preservesTrueEmptyState_withoutDuplicates() = runTest {
        val owner = "owner_read_fail"
        val realDataStore = PreferenceDataStoreFactory.create(
            scope = requireNotNull(dataStoreScope),
            produceFile = { File(context.cacheDir, "read_fail_${System.nanoTime()}.preferences_pb") },
        )
        val failingReadPrefs = object : PulsePreferencesDataSource(realDataStore) {
            override suspend fun readDefaultPlansSeededState(ownerId: String): Boolean? = null
        }
        val repo1 = newPlanRepo(failingReadPrefs)
        val initial = repo1.observePlans(owner).first()
        assertEquals(3, initial.size)

        for (plan in initial) {
            assertTrue(repo1.deletePlan(owner, plan.id) is DomainResult.Success)
        }

        closeResources()
        openResources()
        val failingReadPrefsReopened = object : PulsePreferencesDataSource(
            PreferenceDataStoreFactory.create(
                scope = requireNotNull(dataStoreScope),
                produceFile = { File(context.cacheDir, "read_fail2_${System.nanoTime()}.preferences_pb") },
            ),
        ) {
            override suspend fun readDefaultPlansSeededState(ownerId: String): Boolean? = null
        }
        val repo2 = newPlanRepo(failingReadPrefsReopened)
        assertEquals(0, repo2.observePlans(owner).first().size)
    }

    @Test
    fun cancellationException_isPropagated_andRecoveryDoesNotDuplicate() = runTest {
        val owner = "owner_cancel"
        val realDataStore = PreferenceDataStoreFactory.create(
            scope = requireNotNull(dataStoreScope),
            produceFile = { File(context.cacheDir, "cancel_${System.nanoTime()}.preferences_pb") },
        )
        val cancelOnReadPrefs = object : PulsePreferencesDataSource(realDataStore) {
            override suspend fun readDefaultPlansSeededState(ownerId: String): Boolean? {
                throw CancellationException("Simulated read cancellation")
            }
        }
        val repoReadCancel = newPlanRepo(cancelOnReadPrefs)
        try {
            repoReadCancel.observePlans(owner).first()
            fail("Expected CancellationException to propagate from readDefaultPlansSeededState")
        } catch (_: CancellationException) {
            // Expected
        }

        val cancelOnWritePrefs = object : PulsePreferencesDataSource(realDataStore) {
            override suspend fun markDefaultPlansSeeded(ownerId: String) {
                throw CancellationException("Simulated write cancellation")
            }
        }
        val repoWriteCancel = newPlanRepo(cancelOnWritePrefs)
        try {
            repoWriteCancel.observePlans(owner).first()
            fail("Expected CancellationException to propagate from markDefaultPlansSeeded")
        } catch (_: CancellationException) {
            // Expected: Room already committed starter plans + owner_seed_state atomically
        }

        // Subsequent retry with healthy prefs recovers cleanly with exact 3 starter plans (no duplicates)
        val healthyRepo = newPlanRepo()
        val recovered = healthyRepo.observePlans(owner).first()
        assertEquals(3, recovered.size)
    }
}
