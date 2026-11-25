package com.ayasheta.pulsesmartgym.feature.workout.picker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.ExerciseRepository
import com.ayasheta.pulsesmartgym.feature.workout.components.displayLabel
import com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes
import com.ayasheta.pulsesmartgym.feature.workout.editor.WorkoutPlanSelectionBridge
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ExercisePickerViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val routePlanId: String?,
    authRepository: AuthRepository,
    private val exerciseRepository: ExerciseRepository,
    private val selectionBridge: WorkoutPlanSelectionBridge,
) : ViewModel() {

    private val queryFlow = MutableStateFlow(savedStateHandle.get<String>(KEY_QUERY) ?: "")
    private val filterFlow = MutableStateFlow(
        savedStateHandle.get<String>(KEY_FILTER)?.let { name ->
            MuscleGroup.entries.firstOrNull { it.name == name }
        },
    )
    private val selectedIdsFlow = MutableStateFlow(
        savedStateHandle.get<ArrayList<String>>(KEY_SELECTED_IDS)?.toList()
            ?: resolveInitialSelectionFromBridge(),
    )

    private var currentOwnerId: String? = null

    init {
        savedStateHandle[KEY_SELECTED_IDS] = ArrayList(selectedIdsFlow.value)
    }

    val uiState: StateFlow<ExercisePickerUiState> = combine(
        authRepository.observeAuthState(),
        queryFlow,
        filterFlow,
        selectedIdsFlow,
    ) { authState, query, filter, selectedIds ->
        PickerQueryParams(authState = authState, query = query, filter = filter, selectedIds = selectedIds)
    }.flatMapLatest { params ->
        val newOwnerId = (params.authState as? AuthState.Authenticated)?.user?.id
        if (currentOwnerId != null && currentOwnerId != newOwnerId) {
            selectionBridge.syncOwner(newOwnerId)
            updateSelectedIds(emptyList())
        }
        currentOwnerId = newOwnerId

        exerciseRepository.search(
            query = params.query,
            muscleGroup = params.filter,
        ).combine(selectedIdsFlow) { catalog, selectedIds ->
            val selectedSet = selectedIds.toSet()
            ExercisePickerUiState(
                query = params.query,
                selectedMuscleFilter = params.filter,
                exercises = catalog.map { it.toPickerItem(isSelected = it.id in selectedSet) },
                selectedIds = selectedIds,
                isLoading = false,
                errorMessage = null,
            )
        }
    }.catch { throwable ->
        emit(
            ExercisePickerUiState(
                query = queryFlow.value,
                selectedMuscleFilter = filterFlow.value,
                selectedIds = selectedIdsFlow.value,
                isLoading = false,
                errorMessage = throwable.message ?: "Unable to load exercises.",
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExercisePickerUiState(
            query = queryFlow.value,
            selectedMuscleFilter = filterFlow.value,
            selectedIds = selectedIdsFlow.value,
            isLoading = true,
        ),
    )

    private fun resolveInitialSelectionFromBridge(): List<String> {
        val target = selectionBridge.pickerTarget.value
        return if (target != null && target.targetPlanId == routePlanId) {
            target.preselectedExerciseIds.distinct()
        } else {
            emptyList()
        }
    }

    fun onQueryChanged(query: String) {
        queryFlow.value = query
        savedStateHandle[KEY_QUERY] = query
    }

    fun onFilterSelected(muscleGroup: MuscleGroup?) {
        filterFlow.value = muscleGroup
        savedStateHandle[KEY_FILTER] = muscleGroup?.name
    }

    fun toggleExercise(exerciseId: String) {
        val current = selectedIdsFlow.value
        val updated = if (exerciseId in current) {
            current.filterNot { it == exerciseId }
        } else {
            current + exerciseId
        }
        updateSelectedIds(updated)
    }

    fun confirmSelection(): Boolean {
        val ownerId = currentOwnerId ?: return false
        selectionBridge.commitSelection(
            ownerId = ownerId,
            targetPlanId = routePlanId,
            selectedExerciseIds = selectedIdsFlow.value,
        )
        return true
    }

    fun cancelSelection() {
        selectionBridge.cancelPicker(
            ownerId = currentOwnerId,
            targetPlanId = routePlanId,
        )
    }

    private fun updateSelectedIds(ids: List<String>) {
        val distinct = ids.distinct()
        selectedIdsFlow.value = distinct
        savedStateHandle[KEY_SELECTED_IDS] = ArrayList(distinct)
    }

    private fun ExerciseDefinition.toPickerItem(isSelected: Boolean): ExercisePickerItemUiModel =
        ExercisePickerItemUiModel(
            id = id,
            name = name,
            muscleGroup = muscleGroup,
            muscleSubtitle = muscleGroup.displayLabel(),
            equipmentLabel = equipment.displayLabel().uppercase(),
            thumbnailRes = resolveExerciseThumbnailRes(imageKey, muscleGroup),
            isSelected = isSelected,
        )

    private data class PickerQueryParams(
        val authState: AuthState,
        val query: String,
        val filter: MuscleGroup?,
        val selectedIds: List<String>,
    )

    private companion object {
        const val KEY_QUERY = "picker_query"
        const val KEY_FILTER = "picker_filter"
        const val KEY_SELECTED_IDS = "picker_selected_ids"
    }
}
