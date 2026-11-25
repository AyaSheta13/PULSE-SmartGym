package com.ayasheta.pulsesmartgym.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.history.SessionSummary
import com.ayasheta.pulsesmartgym.core.model.history.WorkoutAnalytics
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutHistoryRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val workoutHistoryRepository: WorkoutHistoryRepository,
    private val zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() },
    private val dateProvider: (ZoneId) -> LocalDate = { LocalDate.now(it) },
    private val timeProvider: (ZoneId) -> LocalTime = { LocalTime.now(it) },
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = authRepository.observeAuthState()
        .flatMapLatest { authState ->
            when (authState) {
                AuthState.Initializing -> flowOf(HomeUiState.Loading)
                AuthState.Unauthenticated -> flowOf(HomeUiState.SignedOut)
                is AuthState.Authenticated -> observeOwnerHome(
                    ownerId = authState.user.id,
                    fallbackDisplayName = authState.user.displayName,
                )
            }
        }
        .catch { throwable ->
            emit(HomeUiState.Error(throwable.message ?: "Unable to load your home dashboard."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )

    private fun observeOwnerHome(
        ownerId: String,
        fallbackDisplayName: String,
    ): Flow<HomeUiState> = combine(
        userProfileRepository.observeProfile(ownerId),
        workoutPlanRepository.observePlans(ownerId),
        workoutSessionRepository.observeActiveSession(ownerId),
        workoutHistoryRepository.observeHistory(ownerId),
    ) { profile, plans, activeSession, history ->
        val zoneId = zoneIdProvider()
        val today = dateProvider(zoneId)
        val nowTime = timeProvider(zoneId)

        val resolvedName = profile?.fullName?.trim()?.takeIf { it.isNotEmpty() }
            ?: fallbackDisplayName.trim().takeIf { it.isNotEmpty() }
            ?: "Athlete"

        val firstName = resolvedName.substringBefore(' ').ifBlank { resolvedName }
        val greetingPrefix = when (nowTime.hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }

        val avatarInitial = resolvedName.firstOrNull()?.uppercaseChar()?.toString() ?: "P"
        val lastFinished = WorkoutAnalytics.selectLastFinishedSession(history)
        val hasCompletedHistory = lastFinished != null
        val featuredPlan = WorkoutAnalytics.selectFeaturedPlan(plans, activeSession)
        val weeklyProgress = WorkoutAnalytics.calculateWeeklySummary(
            summaries = history,
            weeklyTarget = profile?.weeklyWorkouts,
            referenceDate = today,
            zoneId = zoneId,
        )

        HomeUiState.Loaded(
            ownerId = ownerId,
            displayName = resolvedName,
            avatarInitial = avatarInitial,
            welcomeOverline = if (hasCompletedHistory) "Welcome back" else "Welcome",
            greetingTitle = "$greetingPrefix, $firstName",
            currentDate = today,
            featuredPlan = featuredPlan?.toUiModel(),
            activeSession = activeSession,
            plans = plans.map { it.toUiModel() },
            weeklyProgress = weeklyProgress,
            lastSession = lastFinished?.toUiModel(zoneId),
            hasCompletedHistory = hasCompletedHistory,
        )
    }

    private fun WorkoutPlan.toUiModel(): HomePlanItemUiModel = HomePlanItemUiModel(
        id = id,
        name = name,
        exerciseCount = exercises.size,
        plannedSetsCount = exercises.sumOf { it.plannedSets.size },
    )

    private fun SessionSummary.toUiModel(zoneId: ZoneId): HomeLastSessionUiModel {
        val localDate = Instant.ofEpochMilli(endedAtEpochMs ?: startedAtEpochMs)
            .atZone(zoneId)
            .toLocalDate()
        val formattedDate = DATE_FORMATTER.format(localDate).uppercase(Locale.US)
        val formattedDuration = durationMs?.let(::formatDuration)
        return HomeLastSessionUiModel(
            sessionId = sessionId,
            planName = planNameSnapshot,
            dateLabel = formattedDate,
            totalFullReps = totalFullReps,
            durationLabel = formattedDuration,
            completedSetsCount = completedSetsCount,
            isPartial = status == SessionStatus.PARTIALLY_COMPLETED,
        )
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return if (minutes > 0L) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    private companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    }
}
