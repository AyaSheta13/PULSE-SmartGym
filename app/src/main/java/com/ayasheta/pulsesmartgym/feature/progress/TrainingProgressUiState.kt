package com.ayasheta.pulsesmartgym.feature.progress

data class ProgressMetricCardUiModel(
    val title: String,
    val primaryValue: String,
    val primaryUnit: String,
    val deltaText: String,
    val deltaTone: DeltaTone,
)

enum class DeltaTone {
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
}

data class WeeklyVolumeBarUiModel(
    val dayIndex: Int,
    val dayLabel: String,
    val fullDayName: String,
    val volumeKg: Double,
    val heightFraction: Float,
    val isHighlighted: Boolean,
    val isMeasured: Boolean,
    val accessibilityText: String,
)

data class PersonalRecordRowUiModel(
    val exerciseKey: String,
    val exerciseName: String,
    val dateLabel: String,
    val weightLabel: String,
    val detailLabel: String,
)

data class WeekOptionUiModel(
    val weekOffset: Int,
    val label: String,
)

sealed interface TrainingProgressUiState {
    data object Loading : TrainingProgressUiState
    data object SignedOut : TrainingProgressUiState
    data class Error(val message: String) : TrainingProgressUiState
    data object Empty : TrainingProgressUiState

    data class Populated(
        val selectedWeekOffset: Int,
        val weekBadgeLabel: String,
        val weekOptions: List<WeekOptionUiModel>,
        val workoutsCard: ProgressMetricCardUiModel,
        val volumeCard: ProgressMetricCardUiModel,
        val setsCard: ProgressMetricCardUiModel,
        val avgDurationCard: ProgressMetricCardUiModel,
        val chartHeaderVolumeText: String,
        val bars: List<WeeklyVolumeBarUiModel>,
        val personalRecords: List<PersonalRecordRowUiModel>,
    ) : TrainingProgressUiState
}
