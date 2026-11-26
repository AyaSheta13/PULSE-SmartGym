package com.ayasheta.pulsesmartgym.feature.session.counter

import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.session.SessionSetTarget
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus

sealed interface ManualRepCounterUiState {
    data object Loading : ManualRepCounterUiState
    data object SignedOut : ManualRepCounterUiState
    data class Missing(val message: String) : ManualRepCounterUiState
    data class AlreadyCompleted(
        val exerciseName: String,
        val setIndex: Int,
    ) : ManualRepCounterUiState

    data class Terminal(
        val sessionId: String,
        val status: SessionStatus,
    ) : ManualRepCounterUiState

    data class Active(
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
        val currentReps: Int = 0,
        val actualLoad: WeightLoad? = null,
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
        val showEndEarlyConfirmation: Boolean = false,
        val isEndingEarly: Boolean = false,
        val recordedSetsCount: Int = 0,
    ) : ManualRepCounterUiState
}

sealed interface ManualRepCounterEvent {
    data class SetFinished(
        val sessionId: String,
        val hasRestStarted: Boolean,
        val nextTarget: SessionSetTarget?,
        val restErrorMessage: String? = null,
    ) : ManualRepCounterEvent
}
