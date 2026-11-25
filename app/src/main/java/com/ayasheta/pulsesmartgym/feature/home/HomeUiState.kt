package com.ayasheta.pulsesmartgym.feature.home

import com.ayasheta.pulsesmartgym.core.model.history.SessionSummary
import com.ayasheta.pulsesmartgym.core.model.history.WeeklyProgressSummary
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlan
import java.time.LocalDate

data class HomePlanItemUiModel(
    val id: String,
    val name: String,
    val exerciseCount: Int,
    val plannedSetsCount: Int,
)

data class HomeLastSessionUiModel(
    val sessionId: String,
    val planName: String,
    val dateLabel: String,
    val totalFullReps: Int,
    val durationLabel: String?,
    val completedSetsCount: Int,
    val isPartial: Boolean,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data object SignedOut : HomeUiState

    data class Error(val message: String) : HomeUiState

    data class Loaded(
        val ownerId: String,
        val displayName: String,
        val avatarInitial: String,
        val welcomeOverline: String,
        val greetingTitle: String,
        val currentDate: LocalDate,
        val featuredPlan: HomePlanItemUiModel?,
        val activeSession: WorkoutSession?,
        val plans: List<HomePlanItemUiModel>,
        val weeklyProgress: WeeklyProgressSummary,
        val lastSession: HomeLastSessionUiModel?,
        val hasCompletedHistory: Boolean,
    ) : HomeUiState {
        val isNoPlansEmptyState: Boolean
            get() = plans.isEmpty()

        val isPopulatedState: Boolean
            get() = hasCompletedHistory
    }
}
