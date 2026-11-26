package com.ayasheta.pulsesmartgym.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.history.WorkoutAnalytics
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryViewModel(
    private val authRepository: AuthRepository,
    private val workoutHistoryRepository: WorkoutHistoryRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkoutHistoryUiState>(WorkoutHistoryUiState.Loading)
    val uiState: StateFlow<WorkoutHistoryUiState> = _uiState.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    init {
        observeHistory()
    }

    fun retry() {
        _uiState.value = WorkoutHistoryUiState.Loading
        refreshTrigger.value += 1
    }

    private fun observeHistory() {
        viewModelScope.launch {
            combine(
                authRepository.observeAuthState(),
                refreshTrigger,
            ) { auth, _ -> auth }
                .flatMapLatest { authState ->
                    when (authState) {
                        AuthState.Initializing -> flowOf(WorkoutHistoryUiState.Loading)
                        AuthState.Unauthenticated -> flowOf(WorkoutHistoryUiState.SignedOut)
                        is AuthState.Authenticated -> {
                            val ownerId = authState.user.id
                            workoutHistoryRepository.observeHistory(ownerId)
                                .map<_, WorkoutHistoryUiState> { summaries ->
                                    HistoryUiMapper.mapToUiState(
                                        summaries = summaries,
                                        aggregates = WorkoutAnalytics.aggregateSummaries(summaries),
                                        today = todayProvider(),
                                        zoneId = zoneProvider(),
                                    )
                                }
                                .onStart {
                                    emit(WorkoutHistoryUiState.Loading)
                                }
                                .catch { throwable ->
                                    emit(
                                        WorkoutHistoryUiState.Error(
                                            throwable.message ?: "Unable to load workout history.",
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
