package com.ayasheta.pulsesmartgym.feature.workout.picker

import androidx.annotation.DrawableRes
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup

data class ExercisePickerItemUiModel(
    val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val muscleSubtitle: String,
    val equipmentLabel: String,
    @DrawableRes val thumbnailRes: Int,
    val isSelected: Boolean,
)

data class ExercisePickerUiState(
    val query: String = "",
    val selectedMuscleFilter: MuscleGroup? = null,
    val exercises: List<ExercisePickerItemUiModel> = emptyList(),
    val selectedIds: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val selectedCount: Int get() = selectedIds.size
}
