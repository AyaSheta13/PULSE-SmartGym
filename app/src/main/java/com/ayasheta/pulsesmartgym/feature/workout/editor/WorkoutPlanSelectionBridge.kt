package com.ayasheta.pulsesmartgym.feature.workout.editor

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PickerTargetContext(
    val ownerId: String,
    val targetPlanId: String?,
    val preselectedExerciseIds: List<String>,
)

data class CommittedPickerSelection(
    val ownerId: String,
    val targetPlanId: String?,
    val selectedExerciseIds: List<String>,
    val nonce: Long,
)

/**
 * Account-scoped in-memory handoff bridge between WorkoutPlanEditor and ExercisePicker.
 * Lives in a ViewModel scoped to the authenticated session epoch so logout or account switching
 * clears any pending or committed picker selections automatically.
 */
class WorkoutPlanSelectionBridge : ViewModel() {
    private var activeOwnerId: String? = null
    private var nextNonce: Long = 1L

    private val _pickerTarget = MutableStateFlow<PickerTargetContext?>(null)
    val pickerTarget: StateFlow<PickerTargetContext?> = _pickerTarget.asStateFlow()

    private val _committedSelection = MutableStateFlow<CommittedPickerSelection?>(null)
    val committedSelection: StateFlow<CommittedPickerSelection?> = _committedSelection.asStateFlow()

    fun syncOwner(ownerId: String?) {
        if (activeOwnerId != ownerId) {
            _pickerTarget.value = null
            _committedSelection.value = null
            activeOwnerId = ownerId
        }
    }

    fun preparePicker(
        ownerId: String,
        targetPlanId: String?,
        preselectedExerciseIds: List<String>,
    ) {
        syncOwner(ownerId)
        _pickerTarget.value = PickerTargetContext(
            ownerId = ownerId,
            targetPlanId = targetPlanId,
            preselectedExerciseIds = preselectedExerciseIds.distinct(),
        )
    }

    fun commitSelection(
        ownerId: String,
        targetPlanId: String?,
        selectedExerciseIds: List<String>,
    ) {
        if (activeOwnerId != null && activeOwnerId != ownerId) return
        activeOwnerId = ownerId
        _committedSelection.value = CommittedPickerSelection(
            ownerId = ownerId,
            targetPlanId = targetPlanId,
            selectedExerciseIds = selectedExerciseIds.distinct(),
            nonce = nextNonce++,
        )
        _pickerTarget.value = null
    }

    fun cancelPicker(ownerId: String?, targetPlanId: String?) {
        val current = _pickerTarget.value
        if (current != null && current.ownerId == ownerId && current.targetPlanId == targetPlanId) {
            _pickerTarget.value = null
        }
    }

    fun consumeSelection(
        ownerId: String,
        targetPlanId: String?,
        nonce: Long,
    ) {
        val current = _committedSelection.value ?: return
        if (current.ownerId == ownerId && current.targetPlanId == targetPlanId && current.nonce == nonce) {
            _committedSelection.value = null
        }
    }
}
