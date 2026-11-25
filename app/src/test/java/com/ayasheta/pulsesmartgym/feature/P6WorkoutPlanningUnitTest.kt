package com.ayasheta.pulsesmartgym.feature

import androidx.lifecycle.SavedStateHandle
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
import com.ayasheta.pulsesmartgym.core.model.profile.UserPreferences
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.ExerciseRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExercise
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSet
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlan
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import com.ayasheta.pulsesmartgym.feature.workout.details.WorkoutDetailsUiState
import com.ayasheta.pulsesmartgym.feature.workout.details.WorkoutDetailsViewModel
import com.ayasheta.pulsesmartgym.feature.workout.editor.WorkoutPlanEditorEvent
import com.ayasheta.pulsesmartgym.feature.workout.editor.WorkoutPlanEditorViewModel
import com.ayasheta.pulsesmartgym.feature.workout.editor.WorkoutPlanSelectionBridge
import com.ayasheta.pulsesmartgym.feature.workout.list.MyWorkoutsUiState
import com.ayasheta.pulsesmartgym.feature.workout.list.MyWorkoutsViewModel
import com.ayasheta.pulsesmartgym.feature.workout.picker.ExercisePickerViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class P6WorkoutPlanningUnitTest {

    private val mainThreadSurrogate = newSingleThreadContext("P6TestMain")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

    private fun user(id: String) = AuthUser(
        id = id,
        displayName = "Athlete $id",
        email = "$id@pulse.local",
        isGuest = false,
        isDemoUser = false,
    )

    private suspend fun <T> kotlinx.coroutines.flow.StateFlow<T>.awaitValue(
        predicate: (T) -> Boolean,
    ): T = withTimeout(5_000) {
        first(predicate)
    }

    @Test
    fun myWorkouts_observesOwnerScopedPlansReactively() = runBlocking {
        val authRepo = InMemoryAuthRepo(AuthState.Authenticated(user("owner_a")))
        val planRepo = InMemoryWorkoutPlanRepo()
        val exerciseRepo = InMemoryExerciseRepo()
        val vm = MyWorkoutsViewModel(authRepo, planRepo, exerciseRepo)
        val collectJob = launch { vm.uiState.collect {} }

        val emptyState = vm.uiState.awaitValue { it is MyWorkoutsUiState.Loaded } as MyWorkoutsUiState.Loaded
        assertTrue(emptyState.plans.isEmpty())

        planRepo.seedPlan("owner_a", "plan_1", "Upper Body Power", listOf("ex_bench_press", "ex_cable_row"))
        val onePlan = vm.uiState.awaitValue { it is MyWorkoutsUiState.Loaded && it.plans.size == 1 } as MyWorkoutsUiState.Loaded
        assertEquals("Upper Body Power", onePlan.plans.first().name)
        assertEquals(listOf("Chest", "Back"), onePlan.plans.first().categoryChips)

        // Switching owner isolates list
        authRepo.authState.value = AuthState.Authenticated(user("owner_b"))
        val ownerBState = vm.uiState.awaitValue {
            it is MyWorkoutsUiState.Loaded && it.ownerId == "owner_b"
        } as MyWorkoutsUiState.Loaded
        assertTrue(ownerBState.plans.isEmpty())

        collectJob.cancel()
    }

    @Test
    fun createPlan_validationFailureAndDraftPreservation() = runBlocking {
        val authRepo = InMemoryAuthRepo(AuthState.Authenticated(user("owner_a")))
        val planRepo = InMemoryWorkoutPlanRepo()
        val exerciseRepo = InMemoryExerciseRepo()
        val prefsRepo = InMemoryPrefsRepo()
        val bridge = WorkoutPlanSelectionBridge()

        val vm = WorkoutPlanEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            planId = null,
            authRepository = authRepo,
            workoutPlanRepository = planRepo,
            exerciseRepository = exerciseRepo,
            userPreferencesRepository = prefsRepo,
            selectionBridge = bridge,
        )
        vm.uiState.awaitValue { !it.isLoading && it.ownerId != null }

        // Blank name rejected
        vm.savePlan()
        assertNotNull(vm.uiState.value.validationError)
        assertEquals(0, planRepo.allPlansCount())

        // Empty exercises rejected
        vm.onNameChanged("Push Day")
        vm.onDescriptionChanged("Chest & shoulders")
        vm.savePlan()
        assertNotNull(vm.uiState.value.validationError)
        assertEquals(0, planRepo.allPlansCount())

        // Simulate picker selection handoff
        assertTrue(vm.prepareOpenExercisePicker())
        bridge.commitSelection("owner_a", null, listOf("ex_bench_press", "ex_lateral_raise"))
        vm.uiState.awaitValue { it.draft.exercises.size == 2 }

        // Storage failure retains draft
        planRepo.failNextCreate = true
        vm.savePlan()
        val afterFail = vm.uiState.awaitValue { !it.isSaving && it.actionError != null }
        assertEquals("Push Day", afterFail.draft.name)
        assertEquals(2, afterFail.draft.exercises.size)

        // Valid save succeeds and emits Created
        planRepo.failNextCreate = false
        val events = mutableListOf<WorkoutPlanEditorEvent>()
        val eventJob = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { vm.events.collect { events += it } }
        vm.savePlan()
        vm.uiState.awaitValue { !it.isSaving && !it.hasUnsavedChanges }
        withTimeout(2_000) {
            while (events.none { it is WorkoutPlanEditorEvent.Created }) {
                kotlinx.coroutines.yield()
            }
        }
        assertEquals(1, planRepo.allPlansCount())
        assertTrue(events.any { it is WorkoutPlanEditorEvent.Created })
        eventJob.cancel()
    }

    @Test
    fun editPlan_preservesMixedPerSetValuesAndLoadsAndRepeatedOccurrences() = runBlocking {
        val authRepo = InMemoryAuthRepo(AuthState.Authenticated(user("owner_a")))
        val planRepo = InMemoryWorkoutPlanRepo()
        val exerciseRepo = InMemoryExerciseRepo()
        val prefsRepo = InMemoryPrefsRepo()
        val bridge = WorkoutPlanSelectionBridge()

        // Seed plan with mixed reps/rest, target load, and repeated ex_bench_press occurrences
        val seeded = WorkoutPlan(
            id = "plan_mixed",
            ownerProfileId = "owner_a",
            name = "Mixed Load Routine",
            description = "Keep per-set loads",
            createdAtEpochMs = 100L,
            updatedAtEpochMs = 100L,
            exercises = listOf(
                PlanExercise(
                    id = "pe_1",
                    planId = "plan_mixed",
                    exerciseId = "ex_bench_press",
                    orderIndex = 0,
                    notes = "Pause at bottom",
                    plannedSets = listOf(
                        PlannedSet("ps_1", "pe_1", 1, 10, WeightLoad.fromKilograms(60.0), 90),
                        PlannedSet("ps_2", "pe_1", 2, 8, WeightLoad.fromKilograms(70.0), 120),
                    ),
                ),
                PlanExercise(
                    id = "pe_2",
                    planId = "plan_mixed",
                    exerciseId = "ex_bench_press",
                    orderIndex = 1,
                    notes = "Backoff",
                    plannedSets = listOf(
                        PlannedSet("ps_3", "pe_2", 1, 12, WeightLoad.fromKilograms(50.0), 60),
                    ),
                ),
            ),
        )
        planRepo.putRawPlan(seeded)

        val vm = WorkoutPlanEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            planId = "plan_mixed",
            authRepository = authRepo,
            workoutPlanRepository = planRepo,
            exerciseRepository = exerciseRepo,
            userPreferencesRepository = prefsRepo,
            selectionBridge = bridge,
        )
        val loaded = vm.uiState.awaitValue { !it.isLoading && it.draft.exercises.size == 2 }
        assertEquals("8–10", loaded.draft.exercises[0].repsDisplay)
        assertEquals("Mixed", loaded.draft.exercises[0].restDisplay)

        // Modifying uniform reps on pe_2 must not touch pe_1
        vm.adjustReps("pe_2", 2)
        // Attempting uniform reps on mixed pe_1 must NOT flatten mixed values
        vm.adjustReps("pe_1", 1)
        val afterAdjust = vm.uiState.value
        assertEquals(listOf(10, 8), afterAdjust.draft.exercises[0].sets.map { it.targetReps })
        assertEquals(listOf(14), afterAdjust.draft.exercises[1].sets.map { it.targetReps })
        assertEquals(60_000L, afterAdjust.draft.exercises[0].sets[0].targetLoad?.grams)
        assertEquals(70_000L, afterAdjust.draft.exercises[0].sets[1].targetLoad?.grams)

        // Save preserves loads and mixed sets
        vm.savePlan()
        vm.uiState.awaitValue { !it.isSaving && !it.hasUnsavedChanges }
        val saved = planRepo.getPlan("owner_a", "plan_mixed")!!
        assertEquals(listOf(10, 8), saved.exercises[0].plannedSets.map { it.targetReps })
        assertEquals(listOf(60_000L, 70_000L), saved.exercises[0].plannedSets.map { it.targetLoad?.grams })
        assertEquals(listOf(14), saved.exercises[1].plannedSets.map { it.targetReps })
    }

    @Test
    fun exercisePicker_searchFilterSelectionPersistenceAndAccountSwitch() = runBlocking {
        val authRepo = InMemoryAuthRepo(AuthState.Authenticated(user("owner_a")))
        val exerciseRepo = InMemoryExerciseRepo()
        val bridge = WorkoutPlanSelectionBridge()
        bridge.preparePicker("owner_a", null, listOf("ex_bench_press"))

        val vm = ExercisePickerViewModel(
            savedStateHandle = SavedStateHandle(),
            routePlanId = null,
            authRepository = authRepo,
            exerciseRepository = exerciseRepo,
            selectionBridge = bridge,
        )
        val collectJob = launch { vm.uiState.collect {} }

        val initial = vm.uiState.awaitValue { !it.isLoading }
        assertEquals(1, initial.selectedCount)

        // Search case-insensitive & filter
        vm.onQueryChanged("row")
        val searchState = vm.uiState.awaitValue { it.query == "row" && it.exercises.size == 1 }
        assertEquals("Cable Row", searchState.exercises.first().name)

        // Toggle selection while filtered, then switch filter -> selection persists
        vm.toggleExercise("ex_cable_row")
        vm.onQueryChanged("")
        vm.onFilterSelected(MuscleGroup.LEGS)
        val legsState = vm.uiState.awaitValue {
            it.selectedMuscleFilter == MuscleGroup.LEGS && it.selectedCount == 2
        }
        assertEquals(listOf("ex_bench_press", "ex_cable_row"), legsState.selectedIds)

        // Account switch clears stale selection
        authRepo.authState.value = AuthState.Authenticated(user("owner_b"))
        val switched = vm.uiState.awaitValue { it.selectedCount == 0 }
        assertTrue(switched.selectedIds.isEmpty())

        collectJob.cancel()
    }

    @Test
    fun workoutDetails_exclusiveAccordionAndMissingPlan() = runBlocking {
        val authRepo = InMemoryAuthRepo(AuthState.Authenticated(user("owner_a")))
        val planRepo = InMemoryWorkoutPlanRepo()
        val exerciseRepo = InMemoryExerciseRepo()
        val prefsRepo = InMemoryPrefsRepo()

        planRepo.seedPlan("owner_a", "plan_1", "Lower Body", listOf("ex_back_squat", "ex_romanian_deadlift"))
        val vm = WorkoutDetailsViewModel(
            savedStateHandle = SavedStateHandle(),
            planId = "plan_1",
            authRepository = authRepo,
            workoutPlanRepository = planRepo,
            exerciseRepository = exerciseRepo,
            userPreferencesRepository = prefsRepo,
        )
        val collectJob = launch { vm.uiState.collect {} }

        val loaded = vm.uiState.awaitValue { it is WorkoutDetailsUiState.Loaded } as WorkoutDetailsUiState.Loaded
        assertEquals(2, loaded.exercises.size)
        val firstId = loaded.exercises[0].occurrenceId
        val secondId = loaded.exercises[1].occurrenceId
        assertEquals(firstId, loaded.expandedOccurrenceId)

        // Expand second -> collapses first
        vm.toggleExercise(secondId)
        val ex2 = vm.uiState.awaitValue {
            it is WorkoutDetailsUiState.Loaded && it.expandedOccurrenceId == secondId
        } as WorkoutDetailsUiState.Loaded
        assertEquals(secondId, ex2.expandedOccurrenceId)

        // Collapse second -> all collapsed
        vm.toggleExercise(secondId)
        val collapsed = vm.uiState.awaitValue {
            it is WorkoutDetailsUiState.Loaded && it.expandedOccurrenceId == null
        } as WorkoutDetailsUiState.Loaded
        assertNull(collapsed.expandedOccurrenceId)

        // Owner mismatch -> NotFound
        authRepo.authState.value = AuthState.Authenticated(user("owner_b"))
        val notFound = vm.uiState.awaitValue { it is WorkoutDetailsUiState.NotFound }
        assertTrue(notFound is WorkoutDetailsUiState.NotFound)

        collectJob.cancel()
    }

    @Test
    fun editPlan_preservesUnresolvedCatalogExerciseAndBlocksDestructiveSave() = runBlocking {
        val authRepo = InMemoryAuthRepo(AuthState.Authenticated(user("owner_a")))
        val planRepo = InMemoryWorkoutPlanRepo()
        val exerciseRepo = InMemoryExerciseRepo()
        val prefsRepo = InMemoryPrefsRepo()
        val bridge = WorkoutPlanSelectionBridge()

        planRepo.putRawPlan(
            WorkoutPlan(
                id = "plan_unresolved",
                ownerProfileId = "owner_a",
                name = "Legacy Custom Plan",
                description = null,
                createdAtEpochMs = 1L,
                updatedAtEpochMs = 1L,
                exercises = listOf(
                    PlanExercise(
                        id = "pe_valid",
                        planId = "plan_unresolved",
                        exerciseId = "ex_bench_press",
                        orderIndex = 0,
                        plannedSets = listOf(PlannedSet("ps_1", "pe_valid", 1, 10, null, 90)),
                    ),
                    PlanExercise(
                        id = "pe_missing",
                        planId = "plan_unresolved",
                        exerciseId = "ex_removed_from_catalog",
                        orderIndex = 1,
                        plannedSets = listOf(PlannedSet("ps_2", "pe_missing", 1, 12, WeightLoad.fromKilograms(40.0), 60)),
                    ),
                ),
            ),
        )

        val vm = WorkoutPlanEditorViewModel(
            savedStateHandle = SavedStateHandle(),
            planId = "plan_unresolved",
            authRepository = authRepo,
            workoutPlanRepository = planRepo,
            exerciseRepository = exerciseRepo,
            userPreferencesRepository = prefsRepo,
            selectionBridge = bridge,
        )
        val loaded = vm.uiState.awaitValue { !it.isLoading && it.draft.exercises.size == 2 }
        assertTrue(loaded.hasUnresolvedExercises)
        assertFalse(loaded.canSubmit)
        assertEquals("ex_removed_from_catalog", loaded.draft.exercises[1].exerciseId)
        assertEquals(40_000L, loaded.draft.exercises[1].sets.first().targetLoad?.grams)

        // Attempt save while unresolved -> blocked without mutating repository
        vm.onNameChanged("Updated Name")
        vm.savePlan()
        assertNotNull(vm.uiState.value.validationError)
        assertEquals("Legacy Custom Plan", planRepo.getPlan("owner_a", "plan_unresolved")?.name)

        // Removing the unavailable exercise unblocks saving
        vm.removeExercise("pe_missing")
        assertFalse(vm.uiState.value.hasUnresolvedExercises)
        vm.savePlan()
        vm.uiState.awaitValue { !it.isSaving && !it.hasUnsavedChanges }
        val saved = planRepo.getPlan("owner_a", "plan_unresolved")!!
        assertEquals("Updated Name", saved.name)
        assertEquals(1, saved.exercises.size)
    }

    @Test
    fun pickerSelection_isolatesSimultaneousCreateAndEditFlowsForSameOwner() = runBlocking {
        val authRepo = InMemoryAuthRepo(AuthState.Authenticated(user("owner_a")))
        val planRepo = InMemoryWorkoutPlanRepo()
        val exerciseRepo = InMemoryExerciseRepo()
        val prefsRepo = InMemoryPrefsRepo()
        val bridge = WorkoutPlanSelectionBridge()

        planRepo.seedPlan("owner_a", "plan_shared", "Shared Plan", listOf("ex_bench_press"))

        // Two simultaneous Create flows
        val createA = WorkoutPlanEditorViewModel(SavedStateHandle(), null, authRepo, planRepo, exerciseRepo, prefsRepo, bridge)
        val createB = WorkoutPlanEditorViewModel(SavedStateHandle(), null, authRepo, planRepo, exerciseRepo, prefsRepo, bridge)
        createA.uiState.awaitValue { !it.isLoading && it.ownerId != null }
        createB.uiState.awaitValue { !it.isLoading && it.ownerId != null }

        assertTrue(createA.prepareOpenExercisePicker())
        val pickerA = ExercisePickerViewModel(
            savedStateHandle = SavedStateHandle(),
            routePlanId = null,
            routeEditorSessionId = createA.editorSessionId,
            authRepository = authRepo,
            exerciseRepository = exerciseRepo,
            selectionBridge = bridge,
        )
        val pickerACollect = launch { pickerA.uiState.collect {} }
        pickerA.uiState.awaitValue { !it.isLoading }
        pickerA.toggleExercise("ex_cable_row")
        assertTrue(pickerA.confirmSelection())

        createA.uiState.awaitValue { it.draft.exercises.size == 1 }
        assertEquals(0, createB.uiState.value.draft.exercises.size)
        pickerACollect.cancel()

        // Two simultaneous Edit flows targeting the same planId
        val edit1 = WorkoutPlanEditorViewModel(SavedStateHandle(), "plan_shared", authRepo, planRepo, exerciseRepo, prefsRepo, bridge)
        val edit2 = WorkoutPlanEditorViewModel(SavedStateHandle(), "plan_shared", authRepo, planRepo, exerciseRepo, prefsRepo, bridge)
        edit1.uiState.awaitValue { !it.isLoading && it.draft.exercises.size == 1 }
        edit2.uiState.awaitValue { !it.isLoading && it.draft.exercises.size == 1 }

        assertTrue(edit1.prepareOpenExercisePicker())
        val pickerEdit1 = ExercisePickerViewModel(
            savedStateHandle = SavedStateHandle(),
            routePlanId = "plan_shared",
            routeEditorSessionId = edit1.editorSessionId,
            authRepository = authRepo,
            exerciseRepository = exerciseRepo,
            selectionBridge = bridge,
        )
        val picker1Collect = launch { pickerEdit1.uiState.collect {} }
        pickerEdit1.uiState.awaitValue { !it.isLoading }
        pickerEdit1.toggleExercise("ex_lateral_raise")
        assertTrue(pickerEdit1.confirmSelection())

        edit1.uiState.awaitValue { it.draft.exercises.size == 2 }
        assertEquals(1, edit2.uiState.value.draft.exercises.size)
        picker1Collect.cancel()
    }

    @Test
    fun editorDraft_restoresUnsavedCreateAndEditStateAcrossProcessDeathAndBlocksCrossOwner() = runBlocking {
        val authRepo = InMemoryAuthRepo(AuthState.Authenticated(user("owner_a")))
        val planRepo = InMemoryWorkoutPlanRepo()
        val exerciseRepo = InMemoryExerciseRepo()
        val prefsRepo = InMemoryPrefsRepo()
        val bridge = WorkoutPlanSelectionBridge()

        // 1. Create mode: mutate name, description, and add exercise, then simulate process death (new ViewModel with same SavedStateHandle)
        val createSavedState = SavedStateHandle()
        val createVm1 = WorkoutPlanEditorViewModel(createSavedState, null, authRepo, planRepo, exerciseRepo, prefsRepo, bridge)
        createVm1.uiState.awaitValue { !it.isLoading && it.ownerId != null }
        val originalSessionId = createVm1.editorSessionId

        createVm1.onNameChanged("Draft Push")
        createVm1.onDescriptionChanged("Unsaved notes")
        assertTrue(createVm1.prepareOpenExercisePicker())
        bridge.commitSelection("owner_a", originalSessionId, null, listOf("ex_bench_press"))
        createVm1.uiState.awaitValue { it.draft.exercises.size == 1 }

        val createVm2 = WorkoutPlanEditorViewModel(createSavedState, null, authRepo, planRepo, exerciseRepo, prefsRepo, bridge)
        val restoredCreate = createVm2.uiState.awaitValue { !it.isLoading && it.ownerId != null }
        assertEquals(originalSessionId, createVm2.editorSessionId)
        assertEquals("Draft Push", restoredCreate.draft.name)
        assertEquals("Unsaved notes", restoredCreate.draft.description)
        assertEquals(listOf("ex_bench_press"), restoredCreate.draft.exercises.map { it.exerciseId })
        assertTrue(restoredCreate.hasUnsavedChanges)

        // 2. Edit mode: mutate sets/reps/load, simulate process death -> unsaved edits restored
        planRepo.seedPlan("owner_a", "plan_edit", "Base Plan", listOf("ex_bench_press"))
        val editSavedState = SavedStateHandle()
        val editVm1 = WorkoutPlanEditorViewModel(editSavedState, "plan_edit", authRepo, planRepo, exerciseRepo, prefsRepo, bridge)
        val editLoaded = editVm1.uiState.awaitValue { !it.isLoading && it.draft.exercises.size == 1 }
        val occKey = editLoaded.draft.exercises.first().occurrenceKey
        editVm1.adjustSets(occKey, 1)
        editVm1.updateSingleSet(occKey, 1, targetReps = 12, targetRestSeconds = 105, targetLoad = WeightLoad.fromKilograms(72.5), updateLoad = true)

        val editVm2 = WorkoutPlanEditorViewModel(editSavedState, "plan_edit", authRepo, planRepo, exerciseRepo, prefsRepo, bridge)
        val restoredEdit = editVm2.uiState.awaitValue { !it.isLoading && it.draft.exercises.size == 1 }
        assertEquals(2, restoredEdit.draft.exercises.first().sets.size)
        assertEquals(12, restoredEdit.draft.exercises.first().sets.first().targetReps)
        assertEquals(72_500L, restoredEdit.draft.exercises.first().sets.first().targetLoad?.grams)
        assertTrue(restoredEdit.hasUnsavedChanges)

        // 3. Cross-owner isolation: if owner_b opens with owner_a's SavedStateHandle, owner_a's draft is discarded
        val ownerBAuth = InMemoryAuthRepo(AuthState.Authenticated(user("owner_b")))
        val crossOwnerVm = WorkoutPlanEditorViewModel(createSavedState, null, ownerBAuth, planRepo, exerciseRepo, prefsRepo, bridge)
        val crossLoaded = crossOwnerVm.uiState.awaitValue { !it.isLoading && it.ownerId == "owner_b" }
        assertEquals("", crossLoaded.draft.name)
        assertTrue(crossLoaded.draft.exercises.isEmpty())
    }
}

private class InMemoryAuthRepo(initial: AuthState) : AuthRepository {
    val authState = MutableStateFlow(initial)
    override fun observeAuthState(): Flow<AuthState> = authState
    override suspend fun restoreSession(): AuthState = authState.value
    override suspend fun continueAsGuest() = throw UnsupportedOperationException()
    override suspend fun registerLocalAccount(fullName: String, email: String, password: String) =
        throw UnsupportedOperationException()
    override suspend fun signInWithEmail(email: String, password: String, rememberMe: Boolean) =
        throw UnsupportedOperationException()
    override suspend fun resetPassword(newPassword: String) = throw UnsupportedOperationException()
    override suspend fun signInDemo(displayName: String, email: String) = throw UnsupportedOperationException()
    override suspend fun signOut() { authState.value = AuthState.Unauthenticated }
}

private class InMemoryPrefsRepo : UserPreferencesRepository {
    private val prefs = MutableStateFlow(UserPreferences(preferredWeightUnit = WeightUnit.KG))
    override fun observePreferences(): Flow<UserPreferences> = prefs
    override suspend fun updatePreferredWeightUnit(unit: WeightUnit) {}
    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override suspend fun updateDefaultRestSeconds(seconds: Int) {}
}

private class InMemoryExerciseRepo : ExerciseRepository {
    private val items = MutableStateFlow(
        listOf(
            ExerciseDefinition("ex_bench_press", "Barbell Bench Press", MuscleGroup.CHEST, EquipmentCategory.BARBELL, TrackingCategory.LINEAR),
            ExerciseDefinition("ex_cable_row", "Cable Row", MuscleGroup.BACK, EquipmentCategory.CABLE, TrackingCategory.LINEAR),
            ExerciseDefinition("ex_lateral_raise", "Dumbbell Lateral Raise", MuscleGroup.SHOULDERS, EquipmentCategory.DUMBBELL, TrackingCategory.ANGULAR),
            ExerciseDefinition("ex_back_squat", "Barbell Back Squat", MuscleGroup.LEGS, EquipmentCategory.BARBELL, TrackingCategory.COMPOUND),
            ExerciseDefinition("ex_romanian_deadlift", "Romanian Deadlift", MuscleGroup.LEGS, EquipmentCategory.BARBELL, TrackingCategory.COMPOUND),
        ),
    )

    override fun observeAll(): Flow<List<ExerciseDefinition>> = items

    override fun search(
        query: String,
        muscleGroup: MuscleGroup?,
        equipment: EquipmentCategory?,
        trackingCategory: TrackingCategory?,
    ): Flow<List<ExerciseDefinition>> = items.map { list ->
        val q = query.trim().lowercase()
        list.filter { ex ->
            (q.isEmpty() || ex.name.lowercase().contains(q)) &&
                (muscleGroup == null || ex.muscleGroup == muscleGroup) &&
                (equipment == null || ex.equipment == equipment) &&
                (trackingCategory == null || ex.trackingCategory == trackingCategory)
        }
    }

    override suspend fun getById(id: String): ExerciseDefinition? = items.value.firstOrNull { it.id == id }
}

private class InMemoryWorkoutPlanRepo : WorkoutPlanRepository {
    private val plans = MutableStateFlow<List<WorkoutPlan>>(emptyList())
    var failNextCreate: Boolean = false
    private var seq = 1

    fun allPlansCount(): Int = plans.value.size

    fun putRawPlan(plan: WorkoutPlan) {
        plans.value = plans.value.filterNot { it.id == plan.id } + plan
    }

    fun seedPlan(ownerId: String, id: String, name: String, exerciseIds: List<String>) {
        putRawPlan(
            WorkoutPlan(
                id = id,
                ownerProfileId = ownerId,
                name = name,
                description = null,
                createdAtEpochMs = 1L,
                updatedAtEpochMs = 1L,
                exercises = exerciseIds.mapIndexed { idx, exId ->
                    PlanExercise(
                        id = "${id}_pe_$idx",
                        planId = id,
                        exerciseId = exId,
                        orderIndex = idx,
                        plannedSets = listOf(PlannedSet("${id}_ps_$idx", "${id}_pe_$idx", 1, 10, null, 90)),
                    )
                },
            ),
        )
    }

    override fun observePlans(ownerId: String): Flow<List<WorkoutPlan>> =
        plans.map { list -> list.filter { it.ownerProfileId == ownerId } }

    override fun observePlan(ownerId: String, planId: String): Flow<WorkoutPlan?> =
        plans.map { list -> list.firstOrNull { it.ownerProfileId == ownerId && it.id == planId } }

    override suspend fun getPlan(ownerId: String, planId: String): WorkoutPlan? =
        plans.value.firstOrNull { it.ownerProfileId == ownerId && it.id == planId }

    override suspend fun createPlan(ownerId: String, draft: WorkoutPlanDraft): DomainResult<WorkoutPlan> {
        if (failNextCreate) return DomainResult.Failure(DataError.StorageError("Simulated disk error"))
        val id = "created_${seq++}"
        val plan = WorkoutPlan(
            id = id,
            ownerProfileId = ownerId,
            name = draft.name,
            description = draft.description,
            createdAtEpochMs = 10L,
            updatedAtEpochMs = 10L,
            exercises = draft.exercises.mapIndexed { idx, ex ->
                val peId = "${id}_pe_$idx"
                PlanExercise(
                    id = peId,
                    planId = id,
                    exerciseId = ex.exerciseId,
                    orderIndex = ex.orderIndex,
                    notes = ex.notes,
                    plannedSets = ex.plannedSets.map { s ->
                        PlannedSet("${peId}_s_${s.setIndex}", peId, s.setIndex, s.targetReps, s.targetLoad, s.targetRestSeconds)
                    },
                )
            },
        )
        putRawPlan(plan)
        return DomainResult.Success(plan)
    }

    override suspend fun updatePlan(ownerId: String, planId: String, draft: WorkoutPlanDraft): DomainResult<WorkoutPlan> {
        val existing = getPlan(ownerId, planId) ?: return DomainResult.Failure(DataError.NotFound("Missing"))
        val updated = existing.copy(
            name = draft.name,
            description = draft.description,
            exercises = draft.exercises.mapIndexed { idx, ex ->
                val peId = "${planId}_pe_$idx"
                PlanExercise(
                    id = peId,
                    planId = planId,
                    exerciseId = ex.exerciseId,
                    orderIndex = ex.orderIndex,
                    notes = ex.notes,
                    plannedSets = ex.plannedSets.map { s ->
                        PlannedSet("${peId}_s_${s.setIndex}", peId, s.setIndex, s.targetReps, s.targetLoad, s.targetRestSeconds)
                    },
                )
            },
        )
        putRawPlan(updated)
        return DomainResult.Success(updated)
    }

    override suspend fun deletePlan(ownerId: String, planId: String): DomainResult<Unit> {
        val existing = getPlan(ownerId, planId) ?: return DomainResult.Failure(DataError.NotFound("Missing"))
        plans.value = plans.value - existing
        return DomainResult.Success(Unit)
    }
}
