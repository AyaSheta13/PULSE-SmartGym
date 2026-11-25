package com.ayasheta.pulsesmartgym.feature.session.summary

import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus

data class ResultExerciseBreakdownUiModel(
    val sessionExerciseId: String,
    val exerciseName: String,
    val recordedSetsCount: Int,
    val totalFullReps: Int,
    val totalPartialReps: Int,
    val loadBadgeText: String?,
    val modeBadgeText: String,
    val isSkipped: Boolean,
)

sealed interface WorkoutResultUiState {
    data object Loading : WorkoutResultUiState
    data object SignedOut : WorkoutResultUiState
    data class Missing(val message: String) : WorkoutResultUiState
    data class Error(val message: String) : WorkoutResultUiState

    data class Loaded(
        val sessionId: String,
        val planName: String,
        val status: SessionStatus,
        val statusHeadline: String,
        val statusSubtitle: String,
        val totalFullReps: Int,
        val totalPartialReps: Int,
        val durationText: String,
        val setsChipText: String,
        val volumeChipText: String,
        val breakdownHeaderRight: String,
        val preferredWeightUnit: WeightUnit,
        val exercises: List<ResultExerciseBreakdownUiModel>,
    ) : WorkoutResultUiState
}
