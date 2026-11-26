package com.ayasheta.pulsesmartgym.feature.session.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionProgression
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ManualRepCounterViewModel(
    private val sessionId: String,
    private val exerciseId: String,
    private val setIndex: Int,
    private val authRepository: AuthRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val elapsedRealtimeMs: () -> Long = { android.os.SystemClock.elapsedRealtime() },
) : ViewModel() {

    private val _uiState = MutableStateFlow<ManualRepCounterUiState>(ManualRepCounterUiState.Loading)
    val uiState: StateFlow<ManualRepCounterUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ManualRepCounterEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ManualRepCounterEvent> = _events.asSharedFlow()

    private val currentRepsFlow = MutableStateFlow(0)
    private val actualLoadFlow = MutableStateFlow<WeightLoad?>(null)
    private val isSubmittingFlow = MutableStateFlow(false)
    private val errorMessageFlow = MutableStateFlow<String?>(null)
    private val showEndEarlyFlow = MutableStateFlow(false)
    private val isEndingEarlyFlow = MutableStateFlow(false)

    private var activeOwnerId: String? = null
    private var hasInitializedLoad = false
    private var latestSession: WorkoutSession? = null
    private val setStartedAtElapsedMs: Long = elapsedRealtimeMs()

    init {
        observeState()
    }

    private fun observeState() {
        viewModelScope.launch {
            authRepository.observeAuthState().flatMapLatest { authState ->
                when (authState) {
                    AuthState.Initializing -> {
                        activeOwnerId = null
                        flowOf(ManualRepCounterUiState.Loading)
                    }
                    AuthState.Unauthenticated -> {
                        activeOwnerId = null
                        clearTransient()
                        flowOf(ManualRepCounterUiState.SignedOut)
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
                        ) { session, prefs ->
                            latestSession = session
                            if (session == null) {
                                ManualRepCounterUiState.Missing("Workout session not found.")
                            } else if (session.status != SessionStatus.ACTIVE) {
                                ManualRepCounterUiState.Terminal(session.id, session.status)
                            } else {
                                val exercise = session.exercises.firstOrNull { it.id == exerciseId }
                                val plannedSet = exercise?.plannedSets?.firstOrNull { it.setIndex == setIndex }
                                if (exercise == null || plannedSet == null) {
                                    ManualRepCounterUiState.Missing("Set $setIndex could not be found in this session.")
                                } else if (exercise.performedSets.any { it.setIndex == setIndex } && !isSubmittingFlow.value) {
                                    ManualRepCounterUiState.AlreadyCompleted(
                                        exerciseName = exercise.exerciseNameSnapshot,
                                        setIndex = setIndex,
                                    )
                                } else {
                                    if (!hasInitializedLoad) {
                                        actualLoadFlow.value = plannedSet.targetLoad
                                        hasInitializedLoad = true
                                    }
                                    ManualRepCounterUiState.Active(
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
                                        currentReps = currentRepsFlow.value,
                                        actualLoad = actualLoadFlow.value,
                                        isSubmitting = isSubmittingFlow.value,
                                        errorMessage = errorMessageFlow.value,
                                        showEndEarlyConfirmation = showEndEarlyFlow.value,
                                        isEndingEarly = isEndingEarlyFlow.value,
                                        recordedSetsCount = session.exercises.sumOf { it.performedSets.size },
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
            combine(
                currentRepsFlow,
                actualLoadFlow,
                isSubmittingFlow,
                errorMessageFlow,
            ) { reps, load, submitting, err ->
                CounterLocalState(reps, load, submitting, err)
            }.combine(
                combine(showEndEarlyFlow, isEndingEarlyFlow) { show, ending -> show to ending },
            ) { first, (show, ending) ->
                first.copy(showEndEarly = show, isEndingEarly = ending)
            }.collectLatest { local ->
                _uiState.update { current ->
                    if (current is ManualRepCounterUiState.Active) {
                        current.copy(
                            currentReps = local.reps,
                            actualLoad = local.load,
                            isSubmitting = local.isSubmitting,
                            errorMessage = local.errorMessage,
                            showEndEarlyConfirmation = local.showEndEarly,
                            isEndingEarly = local.isEndingEarly,
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun applyTransient(base: ManualRepCounterUiState): ManualRepCounterUiState {
        if (base !is ManualRepCounterUiState.Active) return base
        return base.copy(
            currentReps = currentRepsFlow.value,
            actualLoad = actualLoadFlow.value,
            isSubmitting = isSubmittingFlow.value,
            errorMessage = errorMessageFlow.value,
            showEndEarlyConfirmation = showEndEarlyFlow.value,
            isEndingEarly = isEndingEarlyFlow.value,
        )
    }

    private fun clearTransient() {
        currentRepsFlow.value = 0
        actualLoadFlow.value = null
        hasInitializedLoad = false
        isSubmittingFlow.value = false
        errorMessageFlow.value = null
        showEndEarlyFlow.value = false
        isEndingEarlyFlow.value = false
    }

    fun incrementRep() {
        if (_uiState.value !is ManualRepCounterUiState.Active || isSubmittingFlow.value) return
        errorMessageFlow.value = null
        currentRepsFlow.update { (it + 1).coerceAtMost(999) }
    }

    fun decrementRep() {
        if (_uiState.value !is ManualRepCounterUiState.Active || isSubmittingFlow.value) return
        errorMessageFlow.value = null
        currentRepsFlow.update { (it - 1).coerceAtLeast(0) }
    }

    fun incrementWeight() {
        val active = _uiState.value as? ManualRepCounterUiState.Active ?: return
        if (isSubmittingFlow.value) return
        errorMessageFlow.value = null
        actualLoadFlow.value = stepWeight(active.actualLoad, active.preferredWeightUnit, deltaSign = +1)
    }

    fun decrementWeight() {
        val active = _uiState.value as? ManualRepCounterUiState.Active ?: return
        if (isSubmittingFlow.value) return
        errorMessageFlow.value = null
        actualLoadFlow.value = stepWeight(active.actualLoad, active.preferredWeightUnit, deltaSign = -1)
    }

    fun finishSet() {
        val active = _uiState.value as? ManualRepCounterUiState.Active ?: return
        val ownerId = activeOwnerId ?: return
        if (isSubmittingFlow.value || isEndingEarlyFlow.value) return

        val elapsedMs = (elapsedRealtimeMs() - setStartedAtElapsedMs).coerceAtLeast(0L)
        val request = RecordSetRequest(
            sessionId = active.sessionId,
            sessionExerciseId = active.exerciseId,
            setIndex = active.setIndex,
            fullReps = active.currentReps,
            partialReps = 0,
            actualLoad = active.actualLoad,
            trackingMode = TrackingMode.MANUAL,
            origin = MeasurementOrigin.MANUAL_INPUT,
            durationMs = elapsedMs,
            repRecords = emptyList(),
        )

        isSubmittingFlow.value = true
        errorMessageFlow.value = null

        viewModelScope.launch {
            when (val result = workoutSessionRepository.recordPerformedSet(ownerId, request)) {
                is RecordSetResult.Recorded -> {
                    if (activeOwnerId != ownerId) {
                        isSubmittingFlow.value = false
                        return@launch
                    }
                    val refreshedSession = workoutSessionRepository.getSession(ownerId, active.sessionId)
                        ?: latestSession
                    val nextTarget = refreshedSession?.let {
                        SessionProgression.findNextUncompletedSet(
                            session = it,
                            afterExerciseId = active.exerciseId,
                            afterSetIndex = active.setIndex,
                        )
                    }
                    val restSeconds = active.targetRestSeconds ?: 0
                    val shouldStartRest = restSeconds > 0 && nextTarget != null
                    val (hasRestStarted, restErrorMessage) = if (shouldStartRest) {
                        when (
                            val restResult = workoutSessionRepository.startRestTimer(
                                ownerId = ownerId,
                                sessionId = active.sessionId,
                                exerciseId = active.exerciseId,
                                exerciseName = active.exerciseName,
                                completedSetIndex = active.setIndex,
                                durationSeconds = restSeconds,
                                nextExerciseId = nextTarget?.exerciseId,
                                nextSetIndex = nextTarget?.setIndex,
                            )
                        ) {
                            is DomainResult.Success -> true to null
                            is DomainResult.Failure -> {
                                false to "Set saved, but rest timer could not be started: ${restResult.error.userMessage()}"
                            }
                        }
                    } else {
                        false to null
                    }
                    _events.tryEmit(
                        ManualRepCounterEvent.SetFinished(
                            sessionId = active.sessionId,
                            hasRestStarted = hasRestStarted,
                            nextTarget = nextTarget,
                            restErrorMessage = restErrorMessage,
                        ),
                    )
                }
                is RecordSetResult.AlreadyRecorded -> {
                    errorMessageFlow.value = "Set ${result.setIndex} was already recorded."
                    isSubmittingFlow.value = false
                }
                is RecordSetResult.ValidationError -> {
                    errorMessageFlow.value = result.message
                    isSubmittingFlow.value = false
                }
                is RecordSetResult.NotFound -> {
                    errorMessageFlow.value = result.message
                    isSubmittingFlow.value = false
                }
                is RecordSetResult.InvalidState -> {
                    errorMessageFlow.value = result.message
                    isSubmittingFlow.value = false
                }
            }
        }
    }

    fun requestEndEarly() {
        if (_uiState.value !is ManualRepCounterUiState.Active || isSubmittingFlow.value || isEndingEarlyFlow.value) return
        errorMessageFlow.value = null
        showEndEarlyFlow.value = true
    }

    fun dismissEndEarly() {
        if (isEndingEarlyFlow.value) return
        showEndEarlyFlow.value = false
    }

    fun confirmEndEarly() {
        val ownerId = activeOwnerId ?: return
        if (_uiState.value !is ManualRepCounterUiState.Active || isSubmittingFlow.value || isEndingEarlyFlow.value) return
        isEndingEarlyFlow.value = true
        errorMessageFlow.value = null
        viewModelScope.launch {
            when (val result = workoutSessionRepository.endSessionEarly(ownerId, sessionId)) {
                is DomainResult.Success -> {
                    showEndEarlyFlow.value = false
                }
                is DomainResult.Failure -> {
                    showEndEarlyFlow.value = false
                    errorMessageFlow.value = result.error.userMessage()
                }
            }
            isEndingEarlyFlow.value = false
        }
    }

    private fun stepWeight(
        current: WeightLoad?,
        unit: WeightUnit,
        deltaSign: Int,
    ): WeightLoad {
        return when (unit) {
            WeightUnit.KG -> {
                val currentKg = current?.toKilograms() ?: 0.0
                val nextKg = (currentKg + deltaSign * 2.5).coerceAtLeast(0.0)
                WeightLoad.fromKilograms(nextKg)
            }
            WeightUnit.LBS -> {
                val currentLbs = current?.toPounds() ?: 0.0
                val nextLbs = (currentLbs + deltaSign * 5.0).coerceAtLeast(0.0)
                WeightLoad.fromPounds(nextLbs)
            }
        }
    }

    private data class CounterLocalState(
        val reps: Int,
        val load: WeightLoad?,
        val isSubmitting: Boolean,
        val errorMessage: String?,
        val showEndEarly: Boolean = false,
        val isEndingEarly: Boolean = false,
    )
}
