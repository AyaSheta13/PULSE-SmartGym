package com.ayasheta.pulsesmartgym.feature.workout.editor

import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutDurationEstimator

enum class WorkoutPlanEditorMode {
    CREATE,
    EDIT,
}

data class WorkoutPlanEditorUiState(
    val mode: WorkoutPlanEditorMode,
    val planId: String? = null,
    val ownerId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isPlanMissing: Boolean = false,
    val isSignedOut: Boolean = false,
    val draft: WorkoutPlanDraftState = WorkoutPlanDraftState(),
    val hasUnsavedChanges: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val showDiscardConfirmation: Boolean = false,
    val validationError: String? = null,
    val actionError: String? = null,
    val preferredWeightUnit: WeightUnit = WeightUnit.KG,
    val expandedSetDetailsKeys: Set<String> = emptySet(),
) {
    val estimatedMinutes: Int?
        get() = WorkoutDurationEstimator.estimateMinutes(draft.toDomainDraft())

    val canSubmit: Boolean
        get() = !isLoading && !isSaving && !isDeleting && !isPlanMissing && !isSignedOut && ownerId != null
}
