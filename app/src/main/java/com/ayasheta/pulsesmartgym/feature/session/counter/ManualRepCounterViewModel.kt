package com.ayasheta.pulsesmartgym.feature.session.counter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
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
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val elapsedRealtimeMs: () -> Long = { android.os.SystemClock.elapsedRealtime() },
    private val setDraftStore: SetDraftStore? = null,
    private val executionEnvironment: ExecutionEnvironmentKind = ExecutionEnvironmentKind.GENUINE,
) : ViewModel() {

    private val stateKeyPrefix = "counter_${executionEnvironment.name}_${sessionId}_${exerciseId}_${setIndex}"
    private val repsKey = "${stateKeyPrefix}_reps"
    private val loadGramsKey = "${stateKeyPrefix}_load_grams"
    private val loadInitKey = "${stateKeyPrefix}_load_init"
    private val startElapsedKey = "${stateKeyPrefix}_start_elapsed"
    private val draftJsonKey = "${stateKeyPrefix}_draft_json"
    private val completedKey = "${stateKeyPrefix}_completed"
    private val completedRestStartedKey = "${stateKeyPrefix}_completed_rest"
    private val completedRestErrorKey = "${stateKeyPrefix}_completed_rest_err"

    private val _uiState = MutableStateFlow<ManualRepCounterUiState>(ManualRepCounterUiState.Loading)
    val uiState: StateFlow<ManualRepCounterUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ManualRepCounterEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ManualRepCounterEvent> = _events.asSharedFlow()

    private val currentRepsFlow = MutableStateFlow(savedStateHandle.get<Int>(repsKey) ?: 0)
    private val actualLoadFlow = MutableStateFlow<WeightLoad?>(
        savedStateHandle.get<Long>(loadGramsKey)?.let { WeightLoad(it) },
    )
    private val isSubmittingFlow = MutableStateFlow(false)
    private val errorMessageFlow = MutableStateFlow<String?>(null)
    private val showEndEarlyFlow = MutableStateFlow(false)
    private val isEndingEarlyFlow = MutableStateFlow(false)
    private val completionStateFlow = MutableStateFlow<ManualRepCounterUiState.Completed?>(
        if (savedStateHandle.get<Boolean>(completedKey) == true) {
            ManualRepCounterUiState.Completed(
                sessionId = sessionId,
                hasRestStarted = savedStateHandle.get<Boolean>(completedRestStartedKey) ?: false,
                restErrorMessage = savedStateHandle.get<String>(completedRestErrorKey),
            )
        } else {
            null
        },
    )

    @Volatile
    private var activeOwnerId: String? = null

    @Volatile
    private var restoredForOwnerId: String? = null

    private var hasInitializedLoad = savedStateHandle.get<Boolean>(loadInitKey) ?: false
    private var latestSession: WorkoutSession? = null
    private var setStartedAtElapsedMs: Long = (savedStateHandle.get<Long>(startElapsedKey) ?: elapsedRealtimeMs()).let { saved ->
        val now = elapsedRealtimeMs()
        val safe = if (saved in 0L..now) saved else now
        savedStateHandle[startElapsedKey] = safe
        safe
    }
    private var partialRepsCount: Int = 0
    private var preservedLedger: MutableList<RepMeasurement> = mutableListOf()
    private var preservedEventIds: Set<String> = emptySet()
    private var hadDeviceTracking: Boolean = false

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
                        restoredForOwnerId = null
                        clearTransient()
                        flowOf(ManualRepCounterUiState.SignedOut)
                    }
                    is AuthState.Authenticated -> {
                        val ownerId = authState.user.id
                        if (activeOwnerId != null && activeOwnerId != ownerId) {
                            restoredForOwnerId = null
                            clearTransient()
                        }
                        activeOwnerId = ownerId
                        restoreDraftIfNeeded(ownerId)
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
                                val completedOutcome = completionStateFlow.value
                                if (completedOutcome != null) {
                                    completedOutcome
                                } else if (exercise == null || plannedSet == null) {
                                    ManualRepCounterUiState.Missing("Set $setIndex could not be found in this session.")
                                } else if (exercise.performedSets.any { it.setIndex == setIndex } && !isSubmittingFlow.value) {
                                    ManualRepCounterUiState.AlreadyCompleted(
                                        exerciseName = exercise.exerciseNameSnapshot,
                                        setIndex = setIndex,
                                    )
                                } else {
                                    if (!hasInitializedLoad) {
                                        actualLoadFlow.value = plannedSet.targetLoad
                                        savedStateHandle[loadGramsKey] = plannedSet.targetLoad?.grams
                                        savedStateHandle[loadInitKey] = true
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

    private fun restoreDraftIfNeeded(ownerId: String) {
        if (restoredForOwnerId == ownerId) return
        restoredForOwnerId = ownerId
        val fromStore = setDraftStore?.consume(ownerId, executionEnvironment, sessionId, exerciseId, setIndex)
        val fromSaved = SetDraftSavedStateCodec.decodeAndValidate(
            raw = savedStateHandle.get<String>(draftJsonKey),
            expectedOwnerId = ownerId,
            expectedEnvironment = executionEnvironment,
            expectedSessionId = sessionId,
            expectedExerciseId = exerciseId,
            expectedSetIndex = setIndex,
            currentElapsedMs = elapsedRealtimeMs(),
        )
        val draft = fromStore ?: fromSaved ?: return
        applyRestoredDraft(draft)
    }

    private fun applyRestoredDraft(draft: SetExecutionDraft) {
        hadDeviceTracking = draft.hadDeviceTracking
        preservedLedger = draft.repRecords.toMutableList()
        preservedEventIds = draft.acceptedEventIds
        partialRepsCount = if (draft.hadDeviceTracking) {
            draft.repRecords.count { it.classification == RepClassification.PARTIAL }
        } else {
            draft.partialReps
        }
        val restoredFull = if (draft.hadDeviceTracking) {
            draft.repRecords.count { it.classification == RepClassification.FULL }
        } else {
            draft.fullReps
        }
        currentRepsFlow.value = restoredFull
        savedStateHandle[repsKey] = restoredFull
        setStartedAtElapsedMs = draft.setStartedAtElapsedMs
        savedStateHandle[startElapsedKey] = draft.setStartedAtElapsedMs
        if (draft.hasInitializedLoad) {
            actualLoadFlow.value = draft.actualLoad
            savedStateHandle[loadGramsKey] = draft.actualLoad?.grams
            savedStateHandle[loadInitKey] = true
            hasInitializedLoad = true
        }
        if (draft.hadDeviceTracking) {
            persistFallbackDraftToSavedState()
        }
    }

    private fun persistFallbackDraftToSavedState() {
        val ownerId = activeOwnerId ?: return
        if (!hadDeviceTracking) return
        val normalized = preservedLedger.mapIndexed { idx, rep -> rep.copy(repIndex = idx + 1) }
        val draft = SetExecutionDraft(
            ownerId = ownerId,
            environment = executionEnvironment,
            sessionId = sessionId,
            exerciseId = exerciseId,
            setIndex = setIndex,
            fullReps = currentRepsFlow.value,
            partialReps = partialRepsCount,
            repRecords = normalized,
            acceptedEventIds = preservedEventIds,
            origin = deriveSetOrigin(normalized),
            actualLoad = actualLoadFlow.value,
            hasInitializedLoad = hasInitializedLoad,
            setStartedAtElapsedMs = setStartedAtElapsedMs,
            trackingMode = TrackingMode.DEVICE_ASSISTED,
            hadDeviceTracking = true,
            isManualFallback = true,
        )
        savedStateHandle[draftJsonKey] = SetDraftSavedStateCodec.encode(draft)
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
        partialRepsCount = 0
        preservedLedger.clear()
        preservedEventIds = emptySet()
        hadDeviceTracking = false
        isSubmittingFlow.value = false
        errorMessageFlow.value = null
        showEndEarlyFlow.value = false
        isEndingEarlyFlow.value = false
        completionStateFlow.value = null
        savedStateHandle[repsKey] = 0
        savedStateHandle[loadGramsKey] = null
        savedStateHandle[loadInitKey] = false
        savedStateHandle.remove<String>(draftJsonKey)
        savedStateHandle[completedKey] = false
        savedStateHandle[completedRestStartedKey] = false
        savedStateHandle[completedRestErrorKey] = null
    }

    fun incrementRep() {
        if (_uiState.value !is ManualRepCounterUiState.Active || isSubmittingFlow.value) return
        errorMessageFlow.value = null
        currentRepsFlow.update { next ->
            (next + 1).coerceAtMost(999).also { savedStateHandle[repsKey] = it }
        }
        if (hadDeviceTracking) {
            preservedLedger.add(
                RepMeasurement(
                    repIndex = preservedLedger.size + 1,
                    classification = RepClassification.FULL,
                    origin = MeasurementOrigin.MANUAL_INPUT,
                ),
            )
            persistFallbackDraftToSavedState()
        }
    }

    fun decrementRep() {
        if (_uiState.value !is ManualRepCounterUiState.Active || isSubmittingFlow.value) return
        errorMessageFlow.value = null
        currentRepsFlow.update { next ->
            (next - 1).coerceAtLeast(0).also { savedStateHandle[repsKey] = it }
        }
        if (hadDeviceTracking) {
            val lastFullIdx = preservedLedger.indexOfLast { it.classification == RepClassification.FULL }
            if (lastFullIdx >= 0) {
                preservedLedger.removeAt(lastFullIdx)
            }
            persistFallbackDraftToSavedState()
        }
    }

    fun incrementWeight() {
        val active = _uiState.value as? ManualRepCounterUiState.Active ?: return
        if (isSubmittingFlow.value) return
        errorMessageFlow.value = null
        val next = stepWeight(active.actualLoad, active.preferredWeightUnit, deltaSign = +1)
        actualLoadFlow.value = next
        savedStateHandle[loadGramsKey] = next.grams
        savedStateHandle[loadInitKey] = true
        hasInitializedLoad = true
        persistFallbackDraftToSavedState()
    }

    fun decrementWeight() {
        val active = _uiState.value as? ManualRepCounterUiState.Active ?: return
        if (isSubmittingFlow.value) return
        errorMessageFlow.value = null
        val next = stepWeight(active.actualLoad, active.preferredWeightUnit, deltaSign = -1)
        actualLoadFlow.value = next
        savedStateHandle[loadGramsKey] = next.grams
        savedStateHandle[loadInitKey] = true
        hasInitializedLoad = true
        persistFallbackDraftToSavedState()
    }

    fun finishSet() {
        val active = _uiState.value as? ManualRepCounterUiState.Active ?: return
        val ownerId = activeOwnerId ?: return
        if (isSubmittingFlow.value || isEndingEarlyFlow.value) return

        val elapsedMs = (elapsedRealtimeMs() - setStartedAtElapsedMs).coerceAtLeast(0L)
        val normalizedRecords = if (hadDeviceTracking && preservedLedger.isNotEmpty()) {
            preservedLedger.mapIndexed { idx, rep -> rep.copy(repIndex = idx + 1) }
        } else {
            emptyList()
        }
        val trackingMode = if (hadDeviceTracking) TrackingMode.DEVICE_ASSISTED else TrackingMode.MANUAL
        val setOrigin = deriveSetOrigin(normalizedRecords)
        val fullReps = if (hadDeviceTracking && normalizedRecords.isNotEmpty()) {
            normalizedRecords.count { it.classification == RepClassification.FULL }
        } else {
            active.currentReps
        }
        val partialReps = if (hadDeviceTracking && normalizedRecords.isNotEmpty()) {
            normalizedRecords.count { it.classification == RepClassification.PARTIAL }
        } else if (hadDeviceTracking) {
            partialRepsCount
        } else {
            0
        }
        val request = RecordSetRequest(
            sessionId = active.sessionId,
            sessionExerciseId = active.exerciseId,
            setIndex = active.setIndex,
            fullReps = fullReps,
            partialReps = partialReps,
            actualLoad = active.actualLoad,
            trackingMode = trackingMode,
            origin = setOrigin,
            durationMs = elapsedMs,
            repRecords = normalizedRecords,
        )

        isSubmittingFlow.value = true
        errorMessageFlow.value = null

        viewModelScope.launch {
            when (
                val outcome = SetFinishHelper.submitSetAndStartRest(
                    ownerId = ownerId,
                    request = request,
                    exerciseName = active.exerciseName,
                    targetRestSeconds = active.targetRestSeconds,
                    fallbackSession = latestSession,
                    workoutSessionRepository = workoutSessionRepository,
                    currentOwnerProvider = { activeOwnerId },
                )
            ) {
                is SetSubmissionOutcome.Success -> {
                    savedStateHandle.remove<String>(draftJsonKey)
                    val completedState = ManualRepCounterUiState.Completed(
                        sessionId = outcome.sessionId,
                        hasRestStarted = outcome.hasRestStarted,
                        restErrorMessage = outcome.restErrorMessage,
                    )
                    savedStateHandle[completedKey] = true
                    savedStateHandle[completedRestStartedKey] = outcome.hasRestStarted
                    savedStateHandle[completedRestErrorKey] = outcome.restErrorMessage
                    completionStateFlow.value = completedState
                    _uiState.value = completedState
                    _events.tryEmit(
                        ManualRepCounterEvent.SetFinished(
                            sessionId = outcome.sessionId,
                            hasRestStarted = outcome.hasRestStarted,
                            nextTarget = outcome.nextTarget,
                            restErrorMessage = outcome.restErrorMessage,
                        ),
                    )
                }
                is SetSubmissionOutcome.AlreadyCompleted -> {
                    isSubmittingFlow.value = false
                    savedStateHandle.remove<String>(draftJsonKey)
                    _uiState.value = ManualRepCounterUiState.AlreadyCompleted(
                        exerciseName = active.exerciseName,
                        setIndex = outcome.setIndex,
                    )
                }
                is SetSubmissionOutcome.Failure -> {
                    errorMessageFlow.value = outcome.message
                    isSubmittingFlow.value = false
                }
                SetSubmissionOutcome.OwnerChanged -> {
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

    private fun deriveSetOrigin(records: List<RepMeasurement>): MeasurementOrigin = when {
        records.any { it.origin == MeasurementOrigin.SENSOR_TELEMETRY } -> MeasurementOrigin.SENSOR_TELEMETRY
        records.any { it.origin == MeasurementOrigin.SIMULATED_TELEMETRY } -> MeasurementOrigin.SIMULATED_TELEMETRY
        else -> MeasurementOrigin.MANUAL_INPUT
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
