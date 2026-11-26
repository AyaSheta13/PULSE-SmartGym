package com.ayasheta.pulsesmartgym.feature.workout.exercise

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.repository.ExerciseRepository
import com.ayasheta.pulsesmartgym.feature.workout.components.displayLabel
import com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface ExerciseDetailsUiState {
    data object Loading : ExerciseDetailsUiState
    data class NotFound(val exerciseId: String) : ExerciseDetailsUiState
    data class Loaded(
        val exerciseId: String,
        val name: String,
        val bodyPartLabel: String,
        val equipmentLabel: String,
        val categoryLabel: String,
        @param:DrawableRes val imageRes: Int,
        val guide: ExerciseGuide,
    ) : ExerciseDetailsUiState
}

class ExerciseDetailsViewModel(
    private val exerciseId: String,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val uiState: StateFlow<ExerciseDetailsUiState> = exerciseRepository.observeAll()
        .map { exercises ->
            val match = exercises.firstOrNull { it.id == exerciseId }
            if (match == null) {
                ExerciseDetailsUiState.NotFound(exerciseId)
            } else {
                ExerciseDetailsUiState.Loaded(
                    exerciseId = match.id,
                    name = match.name,
                    bodyPartLabel = match.muscleGroup.displayLabel(),
                    equipmentLabel = match.equipment.displayLabel(),
                    categoryLabel = match.trackingCategory.displayLabel(),
                    imageRes = resolveExerciseThumbnailRes(match.imageKey, match.muscleGroup),
                    guide = ExerciseGuideCatalog.forExercise(match),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ExerciseDetailsUiState.Loading,
        )
}
