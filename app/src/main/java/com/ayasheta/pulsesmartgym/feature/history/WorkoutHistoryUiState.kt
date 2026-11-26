package com.ayasheta.pulsesmartgym.feature.history

import androidx.annotation.DrawableRes
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus

data class HistorySessionItemUiModel(
    val sessionId: String,
    val planName: String,
    val subtitleLine: String,
    val metricsLine: String,
    val statusBadgeLine: String,
    val isPartial: Boolean,
    val status: SessionStatus,
    @param:DrawableRes val thumbnailRes: Int,
)

data class HistorySectionUiModel(
    val key: String,
    val title: String,
    val items: List<HistorySessionItemUiModel>,
)

sealed interface WorkoutHistoryUiState {
    data object Loading : WorkoutHistoryUiState
    data object SignedOut : WorkoutHistoryUiState
    data class Error(val message: String) : WorkoutHistoryUiState
    data object Empty : WorkoutHistoryUiState
    data class Populated(
        val savedSessionsHeadline: String,
        val aggregateMetricsLine: String,
        val sections: List<HistorySectionUiModel>,
    ) : WorkoutHistoryUiState
}
