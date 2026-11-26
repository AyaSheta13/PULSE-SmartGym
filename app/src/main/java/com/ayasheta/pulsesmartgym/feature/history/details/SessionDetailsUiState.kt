package com.ayasheta.pulsesmartgym.feature.history.details

import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus

data class RecordedSetDetailUiModel(
    val setIndex: Int,
    val repsText: String,
    val weightText: String,
    val durationText: String?,
    val modeLabel: String,
)

data class SessionDetailExerciseUiModel(
    val sessionExerciseId: String,
    val exerciseName: String,
    val summaryLine: String,
    val loadBadgeText: String,
    val modeBadgeText: String,
    val isSkipped: Boolean,
    val recordedSets: List<RecordedSetDetailUiModel>,
)

sealed interface SessionDetailsUiState {
    data object Loading : SessionDetailsUiState
    data object SignedOut : SessionDetailsUiState
    data class Unavailable(val message: String) : SessionDetailsUiState
    data class Error(val message: String) : SessionDetailsUiState

    data class Loaded(
        val sessionId: String,
        val planName: String,
        val overviewSubtitle: String,
        val status: SessionStatus,
        val durationText: String,
        val setsChipText: String,
        val repsChipText: String,
        val volumeChipText: String,
        val breakdownHeaderRight: String,
        val exercises: List<SessionDetailExerciseUiModel>,
    ) : SessionDetailsUiState
}
