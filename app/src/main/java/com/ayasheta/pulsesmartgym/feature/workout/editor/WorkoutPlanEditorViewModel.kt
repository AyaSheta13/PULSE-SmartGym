package com.ayasheta.pulsesmartgym.feature.workout.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.PulseValidators
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.ExerciseRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlan
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface WorkoutPlanEditorEvent {
    data class Created(val planId: String) : WorkoutPlanEditorEvent
    data class Updated(val planId: String) : WorkoutPlanEditorEvent
    data object Deleted : WorkoutPlanEditorEvent
    data object Discarded : WorkoutPlanEditorEvent
}

class WorkoutPlanEditorViewModel(
    private val planId: String?,
    private val authRepository: AuthRepository,
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val selectionBridge: WorkoutPlanSelectionBridge,
) : ViewModel() {

    private val mode: WorkoutPlanEditorMode =
        if (planId == null) WorkoutPlanEditorMode.CREATE else WorkoutPlanEditorMode.EDIT

    private val _uiState = MutableStateFlow(
        WorkoutPlanEditorUiState(
            mode = mode,
            planId = planId,
            isLoading = true,
        ),
    )
    val uiState: StateFlow<WorkoutPlanEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WorkoutPlanEditorEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<WorkoutPlanEditorEvent> = _events.asSharedFlow()

    private var activeOwnerId: String? = null
    private var initialSnapshot: WorkoutPlanDraftState? = null
    private var loadedForOwnerId: String? = null

    init {
        observeAuthAndPreferences()
        observeCommittedPickerSelections()
    }

    private fun observeAuthAndPreferences() {
        viewModelScope.launch {
            userPreferencesRepository.observePreferences().collectLatest { prefs ->
                _uiState.update { state ->
                    state.copy(
                        preferredWeightUnit = prefs.preferredWeightUnit,
                        draft = state.draft.copy(preferredWeightUnit = prefs.preferredWeightUnit),
                    )
                }
            }
        }

        viewModelScope.launch {
            authRepository.observeAuthState().collectLatest { authState ->
                when (authState) {
                    AuthState.Initializing -> {
                        _uiState.update { it.copy(isLoading = true, isSignedOut = false) }
                    }
                    AuthState.Unauthenticated -> {
                        activeOwnerId = null
                        loadedForOwnerId = null
                        initialSnapshot = null
                        selectionBridge.syncOwner(null)
                        _uiState.update {
                            WorkoutPlanEditorUiState(
                                mode = mode,
                                planId = planId,
                                isLoading = false,
                                isSignedOut = true,
                                preferredWeightUnit = it.preferredWeightUnit,
                            )
                        }
                    }
                    is AuthState.Authenticated -> {
                        val ownerId = authState.user.id
                        val ownerChanged = activeOwnerId != null && activeOwnerId != ownerId
                        activeOwnerId = ownerId
                        selectionBridge.syncOwner(ownerId)

                        if (ownerChanged) {
                            loadedForOwnerId = null
                            initialSnapshot = null
                            _uiState.update {
                                WorkoutPlanEditorUiState(
                                    mode = mode,
                                    planId = planId,
                                    ownerId = ownerId,
                                    isLoading = planId != null,
                                    preferredWeightUnit = it.preferredWeightUnit,
                                )
                            }
                        } else {
                            _uiState.update { it.copy(ownerId = ownerId, isSignedOut = false) }
                        }

                        if (planId != null && loadedForOwnerId != ownerId) {
                            loadExistingPlan(ownerId, planId)
                        } else if (planId == null) {
                            if (initialSnapshot == null) {
                                initialSnapshot = _uiState.value.draft
                            }
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadExistingPlan(ownerId: String, existingPlanId: String) {
        _uiState.update { it.copy(isLoading = true, isPlanMissing = false, actionError = null) }
        val plan = workoutPlanRepository.getPlan(ownerId, existingPlanId)
        if (activeOwnerId != ownerId) return
        if (plan == null) {
            loadedForOwnerId = ownerId
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isPlanMissing = true,
                    actionError = "Workout plan not found or no longer available.",
                )
            }
            return
        }

        val catalog = exerciseRepository.observeAll().first().associateBy { it.id }
        if (activeOwnerId != ownerId) return

        val loadedDraft = plan.toEditorDraftState(
            catalogById = catalog,
            preferredWeightUnit = _uiState.value.preferredWeightUnit,
        )
        loadedForOwnerId = ownerId
        initialSnapshot = loadedDraft
        _uiState.update {
            it.copy(
                ownerId = ownerId,
                isLoading = false,
                isPlanMissing = false,
                draft = loadedDraft,
                hasUnsavedChanges = false,
                validationError = null,
                actionError = null,
            )
        }
    }

    private fun observeCommittedPickerSelections() {
        viewModelScope.launch {
            selectionBridge.committedSelection.collectLatest { selection ->
                if (selection == null) return@collectLatest
                val ownerId = activeOwnerId ?: return@collectLatest
                if (selection.ownerId != ownerId || selection.targetPlanId != planId) return@collectLatest

                val catalog = exerciseRepository.observeAll().first().associateBy { it.id }
                applySelectedExerciseIds(
                    selectedIds = selection.selectedExerciseIds,
                    catalogById = catalog,
                )
                selectionBridge.consumeSelection(
                    ownerId = ownerId,
                    targetPlanId = planId,
                    nonce = selection.nonce,
                )
            }
        }
    }

    private fun applySelectedExerciseIds(
        selectedIds: List<String>,
        catalogById: Map<String, ExerciseDefinition>,
    ) {
        updateDraft { current ->
            val selectedSet = selectedIds.toSet()
            val keptExisting = current.exercises.filter { it.exercise.id in selectedSet }
            val existingIds = keptExisting.map { it.exercise.id }.toSet()
            val newlyAdded = selectedIds
                .filter { it !in existingIds }
                .mapNotNull { id -> catalogById[id]?.let { EditorExerciseDraftItem.defaultFor(it) } }
            current.copy(exercises = keptExisting + newlyAdded)
        }
    }

    fun onNameChanged(name: String) {
        updateDraft { it.copy(name = name) }
    }

    fun onDescriptionChanged(description: String) {
        updateDraft { it.copy(description = description) }
    }

    fun prepareOpenExercisePicker(): Boolean {
        val ownerId = activeOwnerId ?: return false
        val currentIds = _uiState.value.draft.exercises.map { it.exercise.id }.distinct()
        selectionBridge.preparePicker(
            ownerId = ownerId,
            targetPlanId = planId,
            preselectedExerciseIds = currentIds,
        )
        return true
    }

    fun removeExercise(occurrenceKey: String) {
        updateDraft { current ->
            current.copy(exercises = current.exercises.filterNot { it.occurrenceKey == occurrenceKey })
        }
    }

    fun adjustSets(occurrenceKey: String, delta: Int) {
        updateDraft { current ->
            current.copy(
                exercises = current.exercises.map { item ->
                    if (item.occurrenceKey == occurrenceKey) item.adjustSetCount(delta) else item
                },
            )
        }
    }

    fun adjustReps(occurrenceKey: String, delta: Int) {
        updateDraft { current ->
            current.copy(
                exercises = current.exercises.map { item ->
                    if (item.occurrenceKey == occurrenceKey) {
                        if (item.hasMixedReps) {
                            _uiState.update {
                                it.copy(
                                    expandedSetDetailsKeys = it.expandedSetDetailsKeys + occurrenceKey,
                                    validationError = "This exercise has per-set reps. Edit individual sets below to keep custom targets.",
                                )
                            }
                            item
                        } else {
                            item.adjustUniformReps(delta)
                        }
                    } else {
                        item
                    }
                },
            )
        }
    }

    fun adjustRest(occurrenceKey: String, deltaSeconds: Int) {
        updateDraft { current ->
            current.copy(
                exercises = current.exercises.map { item ->
                    if (item.occurrenceKey == occurrenceKey) {
                        if (item.hasMixedRest) {
                            _uiState.update {
                                it.copy(
                                    expandedSetDetailsKeys = it.expandedSetDetailsKeys + occurrenceKey,
                                    validationError = "This exercise has per-set rest durations. Edit individual sets below.",
                                )
                            }
                            item
                        } else {
                            item.adjustUniformRest(deltaSeconds)
                        }
                    } else {
                        item
                    }
                },
            )
        }
    }

    fun toggleSetDetails(occurrenceKey: String) {
        _uiState.update { state ->
            val updated = if (occurrenceKey in state.expandedSetDetailsKeys) {
                state.expandedSetDetailsKeys - occurrenceKey
            } else {
                state.expandedSetDetailsKeys + occurrenceKey
            }
            state.copy(expandedSetDetailsKeys = updated)
        }
    }

    fun updateSingleSet(
        occurrenceKey: String,
        setIndex: Int,
        targetReps: Int? = null,
        targetRestSeconds: Int? = null,
        targetLoad: WeightLoad? = null,
        updateLoad: Boolean = false,
    ) {
        updateDraft { current ->
            current.copy(
                exercises = current.exercises.map { item ->
                    if (item.occurrenceKey == occurrenceKey) {
                        val existingLoad = item.sets.firstOrNull { it.setIndex == setIndex }?.targetLoad
                        item.updateSingleSet(
                            setIndex = setIndex,
                            targetReps = targetReps,
                            targetRestSeconds = targetRestSeconds,
                            targetLoad = if (updateLoad) targetLoad else existingLoad,
                        )
                    } else {
                        item
                    }
                },
            )
        }
    }

    fun savePlan() {
        val current = _uiState.value
        if (!current.canSubmit) return
        val ownerId = activeOwnerId ?: return

        val domainDraft = current.draft.toDomainDraft()
        validateLocalDraft(domainDraft)?.let { message ->
            _uiState.update { it.copy(validationError = message, actionError = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, validationError = null, actionError = null) }
            val result = if (planId == null) {
                workoutPlanRepository.createPlan(ownerId, domainDraft)
            } else {
                workoutPlanRepository.updatePlan(ownerId, planId, domainDraft)
            }
            if (activeOwnerId != ownerId) {
                _uiState.update { it.copy(isSaving = false) }
                return@launch
            }
            when (result) {
                is DomainResult.Success -> {
                    initialSnapshot = current.draft
                    _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                    if (planId == null) {
                        _events.emit(WorkoutPlanEditorEvent.Created(result.data.id))
                    } else {
                        _events.emit(WorkoutPlanEditorEvent.Updated(result.data.id))
                    }
                }
                is DomainResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            actionError = result.error.userMessage(),
                        )
                    }
                }
            }
        }
    }

    fun requestDeleteConfirmation() {
        if (planId == null || _uiState.value.isSaving || _uiState.value.isDeleting) return
        _uiState.update { it.copy(showDeleteConfirmation = true, actionError = null) }
    }

    fun dismissDeleteConfirmation() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun confirmDeletePlan() {
        val targetPlanId = planId ?: return
        val current = _uiState.value
        if (current.isDeleting || current.isSaving) return
        val ownerId = activeOwnerId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, actionError = null) }
            val result = workoutPlanRepository.deletePlan(ownerId, targetPlanId)
            if (activeOwnerId != ownerId) {
                _uiState.update { it.copy(isDeleting = false, showDeleteConfirmation = false) }
                return@launch
            }
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            showDeleteConfirmation = false,
                            hasUnsavedChanges = false,
                        )
                    }
                    _events.emit(WorkoutPlanEditorEvent.Deleted)
                }
                is DomainResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            showDeleteConfirmation = false,
                            actionError = result.error.userMessage(),
                        )
                    }
                }
            }
        }
    }

    fun requestBackOrCancel() {
        val current = _uiState.value
        if (current.isSaving || current.isDeleting) return
        if (current.hasUnsavedChanges) {
            _uiState.update { it.copy(showDiscardConfirmation = true) }
        } else {
            viewModelScope.launch { _events.emit(WorkoutPlanEditorEvent.Discarded) }
        }
    }

    fun dismissDiscardConfirmation() {
        _uiState.update { it.copy(showDiscardConfirmation = false) }
    }

    fun confirmDiscardChanges() {
        _uiState.update { it.copy(showDiscardConfirmation = false, hasUnsavedChanges = false) }
        viewModelScope.launch { _events.emit(WorkoutPlanEditorEvent.Discarded) }
    }

    private fun validateLocalDraft(draft: com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft): String? {
        PulseValidators.validatePlanName(draft.name)?.let { return it }
        if (draft.exercises.isEmpty()) {
            return "Add at least one exercise before saving your workout plan."
        }
        draft.exercises.forEach { ex ->
            PulseValidators.validateSetCount(ex.plannedSets.size)?.let { return it }
            ex.plannedSets.forEach { set ->
                PulseValidators.validateTargetReps(set.targetReps)?.let { return it }
                PulseValidators.validateRestSeconds(set.targetRestSeconds)?.let { return it }
            }
        }
        return null
    }

    private inline fun updateDraft(transform: (WorkoutPlanDraftState) -> WorkoutPlanDraftState) {
        _uiState.update { state ->
            val updatedDraft = transform(state.draft)
            state.copy(
                draft = updatedDraft,
                hasUnsavedChanges = initialSnapshot != updatedDraft,
                validationError = null,
                actionError = null,
            )
        }
    }

    private fun WorkoutPlan.toEditorDraftState(
        catalogById: Map<String, ExerciseDefinition>,
        preferredWeightUnit: WeightUnit,
    ): WorkoutPlanDraftState = WorkoutPlanDraftState(
        name = name,
        description = description.orEmpty(),
        exercises = exercises.sortedBy { it.orderIndex }.mapNotNull { planExercise ->
            val def = catalogById[planExercise.exerciseId] ?: return@mapNotNull null
            EditorExerciseDraftItem(
                occurrenceKey = planExercise.id,
                exercise = def,
                notes = planExercise.notes,
                sets = planExercise.plannedSets.sortedBy { it.setIndex }.map { set ->
                    PlannedSetDraft(
                        setIndex = set.setIndex,
                        targetReps = set.targetReps,
                        targetLoad = set.targetLoad,
                        targetRestSeconds = set.targetRestSeconds,
                    )
                },
            )
        },
        preferredWeightUnit = preferredWeightUnit,
    )
}
