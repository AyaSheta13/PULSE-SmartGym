package com.ayasheta.pulsesmartgym.feature.session.ready

import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus

sealed interface ReadyStartSetUiState {
    data object Loading : ReadyStartSetUiState
    data object SignedOut : ReadyStartSetUiState
    data class Missing(val message: String) : ReadyStartSetUiState
    data class AlreadyCompleted(
        val exerciseName: String,
        val setIndex: Int,
    ) : ReadyStartSetUiState

    data class Terminal(
        val sessionId: String,
        val status: SessionStatus,
    ) : ReadyStartSetUiState

    data class Ready(
        val sessionId: String,
        val exerciseId: String,
        val setIndex: Int,
        val totalSetsForExercise: Int,
        val planName: String,
        val exerciseName: String,
        val targetReps: Int,
        val targetLoad: WeightLoad?,
        val targetRestSeconds: Int?,
        val preferredWeightUnit: WeightUnit,
        val showEndEarlyConfirmation: Boolean = false,
        val isEndingEarly: Boolean = false,
        val recordedSetsCount: Int = 0,
        val actionError: String? = null,
    ) : ReadyStartSetUiState
}
