package com.ayasheta.pulsesmartgym.feature.session.ready

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceLinkState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepository
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ReadyStartSetViewModel(
    private val sessionId: String,
    private val exerciseId: String,
    private val setIndex: Int,
    private val authRepository: AuthRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val deviceRepository: DeviceRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReadyStartSetUiState>(ReadyStartSetUiState.Loading)
    val uiState: StateFlow<ReadyStartSetUiState> = _uiState.asStateFlow()

    private val showEndEarlyFlow = MutableStateFlow(false)
    private val isEndingEarlyFlow = MutableStateFlow(false)
    private val actionErrorFlow = MutableStateFlow<String?>(null)
    private var activeOwnerId: String? = null

    init {
        observeState()
    }

    private fun observeState() {
        viewModelScope.launch {
            authRepository.observeAuthState().flatMapLatest { authState ->
                when (authState) {
                    AuthState.Initializing -> {
                        activeOwnerId = null
                        flowOf(ReadyStartSetUiState.Loading)
                    }
                    AuthState.Unauthenticated -> {
                        activeOwnerId = null
                        clearTransient()
                        flowOf(ReadyStartSetUiState.SignedOut)
                    }
                    is AuthState.Authenticated -> {
                        val ownerId = authState.user.id
                        if (activeOwnerId != null && activeOwnerId != ownerId) {
                            clearTransient()
                        }
                        activeOwnerId = ownerId
                        combine(
                            workoutSessionRepository.observeSession(ownerId, sessionId),
                            userPreferencesRepository.observePreferences(),
                            deviceRepository?.linkState ?: flowOf(DeviceLinkState()),
                        ) { session, prefs, link ->
                            if (session == null) {
                                ReadyStartSetUiState.Missing("Workout session not found.")
                            } else if (session.status != SessionStatus.ACTIVE) {
                                ReadyStartSetUiState.Terminal(session.id, session.status)
                            } else {
                                val exercise = session.exercises.firstOrNull { it.id == exerciseId }
                                val plannedSet = exercise?.plannedSets?.firstOrNull { it.setIndex == setIndex }
                                if (exercise == null || plannedSet == null) {
                                    ReadyStartSetUiState.Missing("Set $setIndex could not be found in this session.")
                                } else if (exercise.performedSets.any { it.setIndex == setIndex }) {
                                    ReadyStartSetUiState.AlreadyCompleted(
                                        exerciseName = exercise.exerciseNameSnapshot,
                                        setIndex = setIndex,
                                    )
                                } else {
                                    val isDeviceConnected = link.phase == DeviceConnectionState.CONNECTED
                                    val supportsCategory = isDeviceConnected &&
                                        link.capabilities.supportsTrackingCategory(exercise.trackingCategorySnapshot)
                                    val notice = when {
                                        isDeviceConnected && !supportsCategory ->
                                            "Auto-tracking unavailable for ${exercise.trackingCategorySnapshot.name.lowercase()} exercises. Using manual mode."
                                        link.phase == DeviceConnectionState.CONNECTION_LOST ||
                                            link.phase == DeviceConnectionState.FAILED ->
                                            "Device connection lost. Starting in manual mode."
                                        else -> null
                                    }
                                    val modeLabel = when {
                                        supportsCategory && link.isSimulated -> "Sim Pod"
                                        supportsCategory -> "Connected"
                                        else -> "Manual"
                                    }
                                    ReadyStartSetUiState.Ready(
                                        sessionId = session.id,
                                        exerciseId = exercise.id,
                                        setIndex = plannedSet.setIndex,
                                        totalSetsForExercise = exercise.plannedSets.size,
                                        planName = session.planNameSnapshot,
                                        exerciseName = exercise.exerciseNameSnapshot,
                                        targetReps = plannedSet.targetReps,
                                        targetLoad = plannedSet.targetLoad,
                                        targetRestSeconds = plannedSet.targetRestSeconds,
                                        preferredWeightUnit = prefs.preferredWeightUnit,
                                        recordedSetsCount = session.exercises.sumOf { it.performedSets.size },
                                        canStartDeviceAssisted = supportsCategory,
                                        trackingModeLabel = modeLabel,
                                        trackingCompatibilityNotice = notice,
                                    )
                                }
                            }
                        }
                    }
                }
            }.collectLatest { base ->
                _uiState.value = applyTransient(base)
            }
        }

        viewModelScope.launch {
            combine(showEndEarlyFlow, isEndingEarlyFlow, actionErrorFlow) { show, ending, err ->
                Triple(show, ending, err)
            }.collectLatest { (show, ending, err) ->
                _uiState.update { current ->
                    if (current is ReadyStartSetUiState.Ready) {
                        current.copy(
                            showEndEarlyConfirmation = show,
                            isEndingEarly = ending,
                            actionError = err,
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun applyTransient(base: ReadyStartSetUiState): ReadyStartSetUiState {
        if (base !is ReadyStartSetUiState.Ready) return base
        return base.copy(
            showEndEarlyConfirmation = showEndEarlyFlow.value,
            isEndingEarly = isEndingEarlyFlow.value,
            actionError = actionErrorFlow.value,
        )
    }

    private fun clearTransient() {
        showEndEarlyFlow.value = false
        isEndingEarlyFlow.value = false
        actionErrorFlow.value = null
    }

    fun requestEndEarly() {
        if (_uiState.value !is ReadyStartSetUiState.Ready || isEndingEarlyFlow.value) return
        actionErrorFlow.value = null
        showEndEarlyFlow.value = true
    }

    fun dismissEndEarly() {
        if (isEndingEarlyFlow.value) return
        showEndEarlyFlow.value = false
    }

    fun confirmEndEarly() {
        val ownerId = activeOwnerId ?: return
        if (_uiState.value !is ReadyStartSetUiState.Ready || isEndingEarlyFlow.value) return
        isEndingEarlyFlow.value = true
        actionErrorFlow.value = null
        viewModelScope.launch {
            when (val result = workoutSessionRepository.endSessionEarly(ownerId, sessionId)) {
                is DomainResult.Success -> {
                    showEndEarlyFlow.value = false
                }
                is DomainResult.Failure -> {
                    showEndEarlyFlow.value = false
                    actionErrorFlow.value = result.error.userMessage()
                }
            }
            isEndingEarlyFlow.value = false
        }
    }
}
