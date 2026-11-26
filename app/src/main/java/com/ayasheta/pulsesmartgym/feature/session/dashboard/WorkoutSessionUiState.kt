package com.ayasheta.pulsesmartgym.feature.session.dashboard

import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode

data class PerformedSetUiModel(
    val fullReps: Int,
    val partialReps: Int,
    val actualLoad: WeightLoad?,
    val trackingMode: TrackingMode,
)

data class SessionSetRowUiModel(
    val sessionExerciseId: String,
    val setIndex: Int,
    val targetReps: Int,
    val targetLoad: WeightLoad?,
    val targetRestSeconds: Int?,
    val performed: PerformedSetUiModel?,
)

data class SessionExerciseUiModel(
    val sessionExerciseId: String,
    val sourceExerciseId: String?,
    val exerciseName: String,
    val completedSetsCount: Int,
    val totalPlannedSetsCount: Int,
    val isCompleted: Boolean,
    val progressFraction: Float,
    val sets: List<SessionSetRowUiModel>,
)

data class ManualSetEntryTarget(
    val sessionExerciseId: String,
    val exerciseName: String,
    val setIndex: Int,
    val targetReps: Int,
    val targetLoad: WeightLoad?,
    val isCorrection: Boolean,
    val existingFullReps: Int? = null,
    val existingPartialReps: Int? = null,
    val existingLoad: WeightLoad? = null,
)

sealed interface WorkoutSessionUiState {
    data object Loading : WorkoutSessionUiState
    data object SignedOut : WorkoutSessionUiState
    data class Missing(val message: String) : WorkoutSessionUiState
    data class Error(val message: String) : WorkoutSessionUiState
    data class Terminal(
        val sessionId: String,
        val status: com.ayasheta.pulsesmartgym.core.model.session.SessionStatus,
    ) : WorkoutSessionUiState

    data class Active(
        val sessionId: String,
        val ownerId: String,
        val planName: String,
        val startedAtEpochMs: Long,
        val elapsedSeconds: Long,
        val totalPlannedSets: Int,
        val totalRecordedSets: Int,
        val progressFraction: Float,
        val allSetsCompleted: Boolean,
        val preferredWeightUnit: WeightUnit,
        val exercises: List<SessionExerciseUiModel>,
        val isSavingSet: Boolean = false,
        val isCompletingSession: Boolean = false,
        val isEndingEarly: Boolean = false,
        val showEndEarlyConfirmation: Boolean = false,
        val manualEntryTarget: ManualSetEntryTarget? = null,
        val manualEntryError: String? = null,
        val actionError: String? = null,
        val connectionLabel: String = "Not Connected",
    ) : WorkoutSessionUiState {
        val formattedElapsedTime: String
            get() = SessionUiMapper.formatElapsedMmSs(elapsedSeconds)
    }
}
