package com.ayasheta.pulsesmartgym.feature.progress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingProgressViewModel(
    private val authRepository: AuthRepository,
    private val workoutHistoryRepository: WorkoutHistoryRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
    private val isDemoEnvironment: Boolean = false,
) : ViewModel() {

    private val weekOffsetKey = "progress_selected_week_offset"

    private val _uiState = MutableStateFlow<TrainingProgressUiState>(TrainingProgressUiState.Loading)
    val uiState: StateFlow<TrainingProgressUiState> = _uiState.asStateFlow()

    private val selectedWeekOffsetFlow = MutableStateFlow(
        (savedStateHandle.get<Int>(weekOffsetKey) ?: 0).coerceAtLeast(0),
    )
    private val refreshTrigger = MutableStateFlow(0)

    init {
        observeProgress()
    }

    fun selectWeekOffset(offset: Int) {
        val safe = offset.coerceIn(0, 4)
        savedStateHandle[weekOffsetKey] = safe
        selectedWeekOffsetFlow.value = safe
    }

    fun refresh() {
        refreshTrigger.value += 1
    }

    fun retry() {
        _uiState.value = TrainingProgressUiState.Loading
        refreshTrigger.value += 1
    }

    private fun observeProgress() {
        viewModelScope.launch {
            combine(
                authRepository.observeAuthState(),
                refreshTrigger,
            ) { auth, _ -> auth }
                .flatMapLatest { authState ->
                    when (authState) {
                        AuthState.Initializing -> flowOf(TrainingProgressUiState.Loading)
                        AuthState.Unauthenticated -> flowOf(TrainingProgressUiState.SignedOut)
                        is AuthState.Authenticated -> {
                            val ownerId = authState.user.id
                            combine(
                                workoutHistoryRepository.observeFinishedSessions(ownerId),
                                userProfileRepository.observeProfile(ownerId),
                                userPreferencesRepository.observePreferences(),
                                selectedWeekOffsetFlow,
                            ) { sessions, profile, prefs, weekOffset ->
                                val mapped = ProgressUiMapper.mapToUiState(
                                    sessions = sessions,
                                    weeklyTarget = profile?.weeklyWorkouts,
                                    unit = prefs.preferredWeightUnit,
                                    selectedWeekOffset = weekOffset,
                                    today = todayProvider(),
                                    zoneId = zoneProvider(),
                                )
                                if (mapped is TrainingProgressUiState.Populated) {
                                    val today = todayProvider()
                                    val zoneId = zoneProvider()
                                    val referenceDate = today.minusWeeks(weekOffset.coerceAtLeast(0).toLong())
                                    val weekStart = referenceDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                                    val weekStartMs = weekStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
                                    val weekEndExclusiveMs = weekStart.plusDays(7).atStartOfDay(zoneId).toInstant().toEpochMilli()
                                    val exerciseKeys = sessions
                                        .flatMap { it.exercises }
                                        .map { com.ayasheta.pulsesmartgym.core.model.history.WorkoutPerformanceAnalytics.stableExerciseKey(it) }
                                        .distinct()
                                    val trends = exerciseKeys.mapNotNull { exKey ->
                                        com.ayasheta.pulsesmartgym.core.model.history.WorkoutPerformanceAnalytics
                                            .calculateExerciseTrends(
                                                sessions = sessions,
                                                exerciseKey = exKey,
                                                includeSimulated = isDemoEnvironment,
                                                windowStartEpochMs = weekStartMs,
                                                windowEndExclusiveEpochMs = weekEndExclusiveMs,
                                            )
                                            ?.takeIf { it.totalMeasuredReps > 0 }
                                    }
                                    mapped.copy(performanceTrends = trends)
                                } else {
                                    mapped
                                }
                            }.onStart {
                                emit(TrainingProgressUiState.Loading)
                            }.catch { throwable ->
                                emit(
                                    TrainingProgressUiState.Error(
                                        throwable.message ?: "Unable to load training progress.",
                                    ),
                                )
                            }
                        }
                    }
                }
                .collectLatest { state ->
                    _uiState.value = state
                }
        }
    }
}
