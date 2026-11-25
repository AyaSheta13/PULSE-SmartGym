package com.ayasheta.pulsesmartgym.feature.workout.list

import androidx.annotation.DrawableRes

data class WorkoutPlanCardUiModel(
    val id: String,
    val name: String,
    val description: String?,
    val exerciseCount: Int,
    val totalSetsCount: Int,
    val estimatedMinutes: Int?,
    val categoryChips: List<String>,
    @DrawableRes val thumbnailRes: Int,
) {
    val subtitleText: String
        get() = buildString {
            append("$exerciseCount ${if (exerciseCount == 1) "exercise" else "exercises"}")
            if (estimatedMinutes != null) {
                append(" • Est. $estimatedMinutes min")
            } else {
                append(" • $totalSetsCount ${if (totalSetsCount == 1) "set" else "sets"}")
            }
        }
}

sealed interface MyWorkoutsUiState {
    data object Loading : MyWorkoutsUiState
    data object SignedOut : MyWorkoutsUiState
    data class Error(val message: String) : MyWorkoutsUiState
    data class Loaded(
        val ownerId: String,
        val plans: List<WorkoutPlanCardUiModel>,
    ) : MyWorkoutsUiState
}
