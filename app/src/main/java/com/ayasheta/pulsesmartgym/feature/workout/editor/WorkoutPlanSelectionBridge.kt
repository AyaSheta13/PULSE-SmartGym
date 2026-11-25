package com.ayasheta.pulsesmartgym.feature.workout.editor

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PickerTargetContext(
    val ownerId: String,
    val editorSessionId: String,
    val targetPlanId: String?,
    val preselectedExerciseIds: List<String>,
)

data class CommittedPickerSelection(
    val ownerId: String,
    val editorSessionId: String,
    val targetPlanId: String?,
    val selectedExerciseIds: List<String>,
    val nonce: Long,
)

/**
 * Account- and editor-session-scoped in-memory handoff bridge between WorkoutPlanEditor and ExercisePicker.
 * Keyed by `(ownerId, editorSessionId, targetPlanId)` so simultaneous Create flows or two editors
 * targeting the same plan across tabs remain strictly isolated, and logout/account switches clear all state.
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
        editorSessionId: String,
        targetPlanId: String?,
        preselectedExerciseIds: List<String>,
    ) {
        syncOwner(ownerId)
        _pickerTarget.value = PickerTargetContext(
            ownerId = ownerId,
            editorSessionId = editorSessionId,
            targetPlanId = targetPlanId,
            preselectedExerciseIds = preselectedExerciseIds.distinct(),
        )
    }

    fun commitSelection(
        ownerId: String,
        editorSessionId: String,
        targetPlanId: String?,
        selectedExerciseIds: List<String>,
    ) {
        if (activeOwnerId != null && activeOwnerId != ownerId) return
        activeOwnerId = ownerId
        _committedSelection.value = CommittedPickerSelection(
            ownerId = ownerId,
            editorSessionId = editorSessionId,
            targetPlanId = targetPlanId,
            selectedExerciseIds = selectedExerciseIds.distinct(),
            nonce = nextNonce++,
        )
        _pickerTarget.value = null
    }

    fun cancelPicker(
        ownerId: String?,
        editorSessionId: String,
        targetPlanId: String?,
    ) {
        val current = _pickerTarget.value
        if (
            current != null &&
            current.ownerId == ownerId &&
            current.editorSessionId == editorSessionId &&
            current.targetPlanId == targetPlanId
        ) {
            _pickerTarget.value = null
        }
    }

    fun consumeSelection(
        ownerId: String,
        editorSessionId: String,
        targetPlanId: String?,
        nonce: Long,
    ) {
        val current = _committedSelection.value ?: return
        if (
            current.ownerId == ownerId &&
            current.editorSessionId == editorSessionId &&
            current.targetPlanId == targetPlanId &&
            current.nonce == nonce
        ) {
            _committedSelection.value = null
        }
    }
}
