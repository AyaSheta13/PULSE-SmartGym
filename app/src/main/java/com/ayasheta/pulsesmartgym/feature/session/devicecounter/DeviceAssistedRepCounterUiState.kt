package com.ayasheta.pulsesmartgym.feature.session.devicecounter

import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.session.SessionSetTarget
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus

sealed interface DeviceAssistedRepCounterUiState {
    data object Loading : DeviceAssistedRepCounterUiState
    data object SignedOut : DeviceAssistedRepCounterUiState
    data class Missing(val message: String) : DeviceAssistedRepCounterUiState
    data class Terminal(val sessionId: String, val status: SessionStatus) : DeviceAssistedRepCounterUiState
    data class AlreadyCompleted(val exerciseName: String, val setIndex: Int) : DeviceAssistedRepCounterUiState
    data class Completed(
        val sessionId: String,
        val hasRestStarted: Boolean,
        val restErrorMessage: String? = null,
    ) : DeviceAssistedRepCounterUiState

    data class Active(
        val sessionId: String,
        val exerciseId: String,
        val setIndex: Int,
        val totalSetsForExercise: Int,
        val planName: String,
        val exerciseName: String,
        val targetReps: Int,
        val fullReps: Int,
        val partialReps: Int,
        val acceptedLedger: List<RepMeasurement>,
        val targetLoad: WeightLoad?,
        val actualLoad: WeightLoad?,
        val targetRestSeconds: Int?,
        val preferredWeightUnit: WeightUnit,
        val startedAtEpochMs: Long,
        val setStartedAtElapsedMs: Long,
        val recordedSetsCount: Int,
        val deviceName: String,
        val connectionPhase: DeviceConnectionState,
        val bluetoothStatus: PulseBluetoothChipStatus,
        val isSimulatedDevice: Boolean,
        val showRecoverySheet: Boolean = false,
        val isReconnecting: Boolean = false,
        val reconnectErrorMessage: String? = null,
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
        val showEndEarlyConfirmation: Boolean = false,
        val isEndingEarly: Boolean = false,
        val pendingManualFallback: DeviceAssistedCounterEvent.FallbackToManual? = null,
    ) : DeviceAssistedRepCounterUiState {
        val totalAcceptedReps: Int get() = fullReps + partialReps
    }
}

sealed interface DeviceAssistedCounterEvent {
    data class SetFinished(
        val sessionId: String,
        val hasRestStarted: Boolean,
        val nextTarget: SessionSetTarget?,
        val restErrorMessage: String? = null,
    ) : DeviceAssistedCounterEvent

    data class FallbackToManual(
        val sessionId: String,
        val exerciseId: String,
        val setIndex: Int,
    ) : DeviceAssistedCounterEvent

    data class SessionEnded(
        val sessionId: String,
        val status: SessionStatus,
    ) : DeviceAssistedCounterEvent
}
