package com.ayasheta.pulsesmartgym.feature.workout.details

import androidx.annotation.DrawableRes
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit

data class WorkoutDetailSetRowUiModel(
    val setIndex: Int,
    val targetReps: Int,
    val restSeconds: Int?,
    val loadLabel: String?,
)

data class WorkoutDetailExerciseUiModel(
    val occurrenceId: String,
    val orderNumber: Int,
    val exerciseId: String,
    val name: String,
    val muscleSubtitle: String,
    @DrawableRes val photoRes: Int,
    val setsCount: Int,
    val targetRepsLabel: String,
    val restLabel: String,
    val collapsedSummary: String,
    val perSetRows: List<WorkoutDetailSetRowUiModel>,
    val hasMixedOrLoadDetails: Boolean,
)

sealed interface WorkoutDetailsUiState {
    data object Loading : WorkoutDetailsUiState
    data object SignedOut : WorkoutDetailsUiState
    data class NotFound(val message: String) : WorkoutDetailsUiState
    data class Loaded(
        val planId: String,
        val categoryTag: String,
        val planName: String,
        val description: String?,
        val exerciseCount: Int,
        val totalSetsCount: Int,
        val estimatedMinutes: Int?,
        val exercises: List<WorkoutDetailExerciseUiModel>,
        val expandedOccurrenceId: String?,
    ) : WorkoutDetailsUiState {
        val metaLine: String
            get() = buildString {
                append("$exerciseCount ${if (exerciseCount == 1) "Exercise" else "Exercises"}")
                if (estimatedMinutes != null) {
                    append(" • Est. $estimatedMinutes min")
                }
                append(" • $totalSetsCount ${if (totalSetsCount == 1) "Set" else "Sets"} total")
            }
    }
}
