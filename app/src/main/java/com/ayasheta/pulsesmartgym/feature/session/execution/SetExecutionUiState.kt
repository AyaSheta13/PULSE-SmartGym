package com.ayasheta.pulsesmartgym.feature.session.execution

import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseTrackingConfiguration
import com.ayasheta.pulsesmartgym.core.model.session.SessionSetTarget
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus

enum class SetExecutionPhase {
    Ready,
    InProgress,
    Saving,
    Completed,
    ConnectionLost,
    RecoverableError,
}

sealed interface SetExecutionUiState {
    data object Loading : SetExecutionUiState
    data object SignedOut : SetExecutionUiState
    data class Missing(val message: String) : SetExecutionUiState
    data class AlreadyCompleted(
        val exerciseName: String,
        val setIndex: Int,
    ) : SetExecutionUiState

    data class Terminal(
        val sessionId: String,
        val status: SessionStatus,
    ) : SetExecutionUiState

    data class Completed(
        val sessionId: String,
        val hasRestStarted: Boolean,
        val nextTarget: SessionSetTarget?,
        val restErrorMessage: String?,
    ) : SetExecutionUiState

    data class Active(
        val phase: SetExecutionPhase,
        val sessionId: String,
        val ownerId: String,
        val planName: String,
        val exerciseId: String,
        val exerciseName: String,
        val setIndex: Int,
        val totalSetsForExercise: Int,
        val targetReps: Int,
        val targetLoad: WeightLoad?,
        val targetRestSeconds: Int?,
        val preferredWeightUnit: WeightUnit,
        val recordedSetsCount: Int,
        val actualLoad: WeightLoad?,
        val fullReps: Int,
        val partialReps: Int,
        val isDeviceAssisted: Boolean,
        val canStartDeviceAssisted: Boolean,
        val isFallbackManual: Boolean,
        val modeLabel: String,
        val bluetoothChipStatus: PulseBluetoothChipStatus,
        val isTelemetrySimulated: Boolean = false,
        val trackingCompatibilityNotice: String?,
        val startedElapsedRealtimeMs: Long?,
        val currentElapsedRealtimeMs: Long = 0L,
        val frozenElapsedDurationMs: Long? = null,
        val sessionStartedAtEpochMs: Long = 0L,
        val repRecords: List<RepMeasurement> = emptyList(),
        val trackingConfig: ExerciseTrackingConfiguration? = null,
        val errorMessage: String?,
        val isReconnecting: Boolean,
        val reconnectError: String?,
        val showEndEarlyConfirmation: Boolean,
        val isEndingEarly: Boolean,
        val showSimulateButtons: Boolean,
    ) : SetExecutionUiState {
        val totalDisplayedReps: Int get() = fullReps + partialReps
        val isStarted: Boolean get() = phase != SetExecutionPhase.Ready
        val isSaving: Boolean get() = phase == SetExecutionPhase.Saving
        val showConnectionLostModal: Boolean get() = phase == SetExecutionPhase.ConnectionLost
        val canAdjustRepsManually: Boolean
            get() = phase == SetExecutionPhase.InProgress && (!isDeviceAssisted || isFallbackManual)
        val canAdjustWeight: Boolean
            get() = phase == SetExecutionPhase.Ready || phase == SetExecutionPhase.InProgress || phase == SetExecutionPhase.RecoverableError
    }
}

sealed interface SetExecutionEvent {
    data class SetFinished(
        val sessionId: String,
        val hasRestStarted: Boolean,
        val nextTarget: SessionSetTarget?,
        val restErrorMessage: String?,
    ) : SetExecutionEvent
}
