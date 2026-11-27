package com.ayasheta.pulsesmartgym.feature.session.execution

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.data.device.DeviceEventNormalizer
import com.ayasheta.pulsesmartgym.core.data.device.FakeDeviceDataSource
import com.ayasheta.pulsesmartgym.core.data.device.NormalizedRepResult
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceEvent
import com.ayasheta.pulsesmartgym.core.model.device.DeviceOperationResult
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepository
import com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseTrackingCatalog
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.feature.session.counter.SetDraftSavedStateCodec
import com.ayasheta.pulsesmartgym.feature.session.counter.SetDraftStore
import com.ayasheta.pulsesmartgym.feature.session.counter.SetExecutionDraft
import com.ayasheta.pulsesmartgym.feature.session.counter.SetFinishHelper
import com.ayasheta.pulsesmartgym.feature.session.counter.SetSubmissionOutcome
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SetExecutionViewModel(
    private val sessionId: String,
    private val exerciseId: String,
    private val setIndex: Int,
    private val authRepository: AuthRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val deviceRepository: DeviceRepository,
    private val fakeDeviceDataSource: FakeDeviceDataSource? = null,
    private val setDraftStore: SetDraftStore? = null,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val elapsedRealtimeMs: () -> Long = { android.os.SystemClock.elapsedRealtime() },
    private val executionEnvironment: ExecutionEnvironmentKind = ExecutionEnvironmentKind.GENUINE,
    private val initialPhase: SetExecutionPhase = SetExecutionPhase.Ready,
    private val forceManualMode: Boolean = false,
) : ViewModel() {

    private val stateKeyPrefix = "set_exec_${executionEnvironment.name}_${sessionId}_${exerciseId}_${setIndex}"
    private val phaseKey = "${stateKeyPrefix}_phase"
    private val loadGramsKey = "${stateKeyPrefix}_load_grams"
    private val loadInitKey = "${stateKeyPrefix}_load_init"
    private val startElapsedKey = "${stateKeyPrefix}_start_elapsed"
    private val frozenDurationKey = "${stateKeyPrefix}_frozen_duration"
    private val fallbackKey = "${stateKeyPrefix}_fallback"
    private val deviceAssistedKey = "${stateKeyPrefix}_device_assisted"
    private val draftJsonKey = "${stateKeyPrefix}_draft_json"
    private val completedKey = "${stateKeyPrefix}_completed"
    private val completedRestStartedKey = "${stateKeyPrefix}_completed_rest"
    private val completedRestErrorKey = "${stateKeyPrefix}_completed_rest_err"

    private val trackingWindowId = "${sessionId}_${exerciseId}_${setIndex}"

    private val _uiState = MutableStateFlow<SetExecutionUiState>(SetExecutionUiState.Loading)
    val uiState: StateFlow<SetExecutionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SetExecutionEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SetExecutionEvent> = _events.asSharedFlow()

    private val restoredPhase = savedStateHandle.get<String>(phaseKey)
        ?.let { runCatching { SetExecutionPhase.valueOf(it) }.getOrNull() }
        ?: initialPhase

    private val phaseFlow = MutableStateFlow(
        if (restoredPhase == SetExecutionPhase.Saving) SetExecutionPhase.InProgress else restoredPhase,
    )
    private val fullRepsFlow = MutableStateFlow(0)
    private val partialRepsFlow = MutableStateFlow(0)
    private val ledgerFlow = MutableStateFlow<List<RepMeasurement>>(emptyList())
    private val seenEventIds = LinkedHashSet<String>()
    private val actualLoadFlow = MutableStateFlow<WeightLoad?>(
        savedStateHandle.get<Long>(loadGramsKey)?.let { WeightLoad(it) },
    )
    private var hasInitializedLoad = savedStateHandle.get<Boolean>(loadInitKey) == true
    private val isFallbackManualFlow = MutableStateFlow(
        forceManualMode || (savedStateHandle.get<Boolean>(fallbackKey) == true),
    )
    private val isDeviceAssistedFlow = MutableStateFlow(
        savedStateHandle.get<Boolean>(deviceAssistedKey) ?: false,
    )
    private val isReconnectingFlow = MutableStateFlow(false)
    private val reconnectErrorFlow = MutableStateFlow<String?>(null)
    private val errorMessageFlow = MutableStateFlow<String?>(null)
    private val showEndEarlyFlow = MutableStateFlow(false)
    private val isEndingEarlyFlow = MutableStateFlow(false)
    private val frozenDurationMsFlow = MutableStateFlow<Long?>(
        savedStateHandle.get<Long>(frozenDurationKey),
    )

    private val completionStateFlow = MutableStateFlow<SetExecutionUiState.Completed?>(
        if (savedStateHandle.get<Boolean>(completedKey) == true) {
            SetExecutionUiState.Completed(
                sessionId = sessionId,
                hasRestStarted = savedStateHandle.get<Boolean>(completedRestStartedKey) ?: false,
                nextTarget = null,
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

    @Volatile
    private var latestSession: WorkoutSession? = null

    @Volatile
    private var isDisposed = false

    init {
        if (phaseFlow.value == SetExecutionPhase.InProgress &&
            savedStateHandle.get<Long>(startElapsedKey) == null
        ) {
            savedStateHandle[startElapsedKey] = elapsedRealtimeMs()
        }
        observeState()
        observeDeviceEvents()
    }

    private fun observeState() {
        viewModelScope.launch {
            authRepository.observeAuthState()
                .flatMapLatest { auth ->
                    when (auth) {
                        AuthState.Initializing -> flowOf(SetExecutionUiState.Loading)
                        AuthState.Unauthenticated -> {
                            activeOwnerId = null
                            flowOf(SetExecutionUiState.SignedOut)
                        }
                        is AuthState.Authenticated -> {
                            val ownerId = auth.user.id
                            activeOwnerId = ownerId
                            restoreDraftIfNeeded(ownerId)

                            val sessionFlow = workoutSessionRepository.observeSession(ownerId, sessionId)
                            val prefsFlow = userPreferencesRepository.observePreferences()
                            val linkFlow = deviceRepository.linkState
                            val controlsCombine = combine(
                                phaseFlow,
                                fullRepsFlow,
                                partialRepsFlow,
                                actualLoadFlow,
                                isFallbackManualFlow,
                            ) { phase, full, partial, load, fallback ->
                                ControlSnapshot(phase, full, partial, load, fallback)
                            }
                            val statusCombine = combine(
                                isReconnectingFlow,
                                reconnectErrorFlow,
                                errorMessageFlow,
                                showEndEarlyFlow,
                                isEndingEarlyFlow,
                            ) { reconnecting, reconnectErr, errMsg, showEndEarly, endingEarly ->
                                StatusSnapshot(reconnecting, reconnectErr, errMsg, showEndEarly, endingEarly)
                            }
                            val ledgerCombine = combine(ledgerFlow, statusCombine) { ledger, statuses ->
                                ledger to statuses
                            }

                            combine(
                                sessionFlow,
                                prefsFlow,
                                linkFlow,
                                controlsCombine,
                                ledgerCombine,
                            ) { session, prefs, link, controls, (ledger, statuses) ->
                                latestSession = session

                                val completed = completionStateFlow.value
                                if (completed != null) return@combine completed

                                if (session == null) {
                                    return@combine SetExecutionUiState.Missing("Workout session not found.")
                                }
                                if (session.status != SessionStatus.ACTIVE) {
                                    return@combine SetExecutionUiState.Terminal(session.id, session.status)
                                }

                                val exercise = session.exercises.firstOrNull { it.id == exerciseId }
                                    ?: return@combine SetExecutionUiState.Missing("Exercise not found in this workout session.")
                                val planned = exercise.plannedSets.firstOrNull { it.setIndex == setIndex }
                                    ?: return@combine SetExecutionUiState.Missing("Set $setIndex not found for ${exercise.exerciseNameSnapshot}.")

                                val alreadyDone = exercise.performedSets.any { it.setIndex == setIndex }
                                if (alreadyDone) {
                                    return@combine SetExecutionUiState.AlreadyCompleted(
                                        exerciseName = exercise.exerciseNameSnapshot,
                                        setIndex = setIndex,
                                    )
                                }

                                val resolvedLoad = if (!hasInitializedLoad) {
                                    hasInitializedLoad = true
                                    savedStateHandle[loadInitKey] = true
                                    actualLoadFlow.value = planned.targetLoad
                                    savedStateHandle[loadGramsKey] = planned.targetLoad?.grams
                                    planned.targetLoad
                                } else {
                                    controls.load
                                }

                                val exerciseConfig = ExerciseTrackingCatalog.forExercise(
                                    exerciseId = exercise.sourceExerciseId,
                                    trackingCategory = exercise.trackingCategorySnapshot,
                                )
                                val effectiveCaps = ExerciseTrackingCatalog.resolveEffectiveCapabilities(
                                    deviceCapabilities = link.capabilities,
                                    exerciseConfig = exerciseConfig,
                                    environment = executionEnvironment,
                                )
                                val isDeviceConnected = link.phase == DeviceConnectionState.CONNECTED
                                val canStartDevice = !forceManualMode &&
                                    executionEnvironment == ExecutionEnvironmentKind.DEMO &&
                                    isDeviceConnected &&
                                    effectiveCaps.automaticRepEvents

                                if (controls.phase == SetExecutionPhase.Ready) {
                                    isDeviceAssistedFlow.value = canStartDevice
                                    savedStateHandle[deviceAssistedKey] = canStartDevice
                                }

                                val effectiveDeviceAssisted = isDeviceAssistedFlow.value && !controls.isFallbackManual

                                var effectivePhase = controls.phase
                                if (effectivePhase == SetExecutionPhase.InProgress &&
                                    effectiveDeviceAssisted &&
                                    !isDeviceConnected
                                ) {
                                    effectivePhase = SetExecutionPhase.ConnectionLost
                                    phaseFlow.value = SetExecutionPhase.ConnectionLost
                                    savedStateHandle[phaseKey] = SetExecutionPhase.ConnectionLost.name
                                }

                                if (effectivePhase == SetExecutionPhase.ConnectionLost && isDeviceConnected) {
                                    effectivePhase = SetExecutionPhase.InProgress
                                    phaseFlow.value = SetExecutionPhase.InProgress
                                    savedStateHandle[phaseKey] = SetExecutionPhase.InProgress.name
                                    isReconnectingFlow.value = false
                                    reconnectErrorFlow.value = null
                                }

                                val chipStatus = when (link.phase) {
                                    DeviceConnectionState.CONNECTED -> PulseBluetoothChipStatus.CONNECTED
                                    DeviceConnectionState.SCANNING,
                                    DeviceConnectionState.CONNECTING -> PulseBluetoothChipStatus.CONNECTING
                                    DeviceConnectionState.CONNECTION_LOST -> PulseBluetoothChipStatus.CONNECTION_LOST
                                    DeviceConnectionState.FAILED -> PulseBluetoothChipStatus.FAILED
                                    DeviceConnectionState.DISCONNECTED -> PulseBluetoothChipStatus.DISCONNECTED
                                }

                                val modeLabel = when {
                                    controls.isFallbackManual -> "Manual"
                                    effectiveDeviceAssisted -> "Device Connected"
                                    else -> "Manual"
                                }

                                val compatibilityNotice = if (!isDeviceConnected && executionEnvironment == ExecutionEnvironmentKind.DEMO) {
                                    "Smart Gym device is not connected. Manual counter mode will be used."
                                } else {
                                    null
                                }

                                SetExecutionUiState.Active(
                                    phase = effectivePhase,
                                    sessionId = session.id,
                                    ownerId = ownerId,
                                    planName = session.planNameSnapshot,
                                    exerciseId = exercise.id,
                                    exerciseName = exercise.exerciseNameSnapshot,
                                    setIndex = setIndex,
                                    totalSetsForExercise = exercise.plannedSets.size,
                                    targetReps = planned.targetReps,
                                    targetLoad = planned.targetLoad,
                                    targetRestSeconds = planned.targetRestSeconds,
                                    preferredWeightUnit = prefs.preferredWeightUnit,
                                    recordedSetsCount = session.exercises.sumOf { it.performedSets.size },
                                    actualLoad = resolvedLoad,
                                    fullReps = controls.full,
                                    partialReps = controls.partial,
                                    isDeviceAssisted = effectiveDeviceAssisted,
                                    canStartDeviceAssisted = canStartDevice,
                                    isFallbackManual = controls.isFallbackManual,
                                    modeLabel = modeLabel,
                                    bluetoothChipStatus = chipStatus,
                                    isTelemetrySimulated = link.isTelemetrySimulated,
                                    trackingCompatibilityNotice = compatibilityNotice,
                                    startedElapsedRealtimeMs = savedStateHandle.get<Long>(startElapsedKey),
                                    currentElapsedRealtimeMs = elapsedRealtimeMs(),
                                    frozenElapsedDurationMs = frozenDurationMsFlow.value,
                                    sessionStartedAtEpochMs = session.startedAtEpochMs,
                                    repRecords = ledger,
                                    trackingConfig = exerciseConfig,
                                    errorMessage = statuses.errorMessage,
                                    isReconnecting = statuses.isReconnecting,
                                    reconnectError = statuses.reconnectError,
                                    showEndEarlyConfirmation = statuses.showEndEarly,
                                    isEndingEarly = statuses.endingEarly,
                                    showSimulateButtons = executionEnvironment == ExecutionEnvironmentKind.DEMO &&
                                        effectivePhase == SetExecutionPhase.InProgress &&
                                        effectiveDeviceAssisted,
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

    private fun observeDeviceEvents() {
        viewModelScope.launch {
            deviceRepository.events.collect { event ->
                if (event !is DeviceEvent.RepRecorded) return@collect
                if (phaseFlow.value != SetExecutionPhase.InProgress) return@collect
                if (!isDeviceAssistedFlow.value || isFallbackManualFlow.value) return@collect

                val ownerId = activeOwnerId
                val session = latestSession
                val exercise = session?.exercises?.firstOrNull { it.id == exerciseId }
                val hasPlannedSet = exercise?.plannedSets?.any { it.setIndex == setIndex } == true
                val isAlreadyRecorded = exercise?.performedSets?.any { it.setIndex == setIndex } == true
                val link = deviceRepository.linkState.value

                if (ledgerFlow.value.size >= SetDraftSavedStateCodec.MAX_SAVED_LEDGER_RECORDS) {
                    errorMessageFlow.value =
                        "Maximum supported set size (${SetDraftSavedStateCodec.MAX_SAVED_LEDGER_RECORDS} reps) reached."
                    return@collect
                }

                val exerciseConfig = ExerciseTrackingCatalog.forExercise(
                    exerciseId = exercise?.sourceExerciseId,
                    trackingCategory = exercise?.trackingCategorySnapshot
                        ?: com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory.COMPOUND,
                )
                val effectiveCaps = ExerciseTrackingCatalog.resolveEffectiveCapabilities(
                    deviceCapabilities = link.capabilities,
                    exerciseConfig = exerciseConfig,
                    environment = executionEnvironment,
                )

                val normalized = DeviceEventNormalizer.normalizeAndValidate(
                    event = event.rep,
                    expectedProviderType = link.providerType,
                    expectedConnectionGeneration = link.connectionGeneration,
                    expectedTrackingWindowId = trackingWindowId,
                    isConnected = link.phase == DeviceConnectionState.CONNECTED,
                    isSubmitting = phaseFlow.value == SetExecutionPhase.Saving || completionStateFlow.value != null,
                    capabilities = effectiveCaps,
                    seenEventIds = seenEventIds,
                    nextRepIndex = ledgerFlow.value.size + 1,
                    isOwnerAuthenticated = ownerId != null,
                    isSessionActive = session?.status == SessionStatus.ACTIVE,
                    isSetValidAndUnrecorded = hasPlannedSet && !isAlreadyRecorded,
                    isAcceptingEvents = !isDisposed && !isEndingEarlyFlow.value && !isFallbackManualFlow.value,
                    expectedEnvironment = executionEnvironment,
                )

                when (normalized) {
                    is NormalizedRepResult.Accepted -> {
                        seenEventIds.add(event.rep.eventId)
                        val updatedLedger = ledgerFlow.value + normalized.measurement
                        ledgerFlow.value = updatedLedger
                        fullRepsFlow.value = updatedLedger.count { it.classification == RepClassification.FULL }
                        partialRepsFlow.value = updatedLedger.count { it.classification == RepClassification.PARTIAL }
                        persistDraft()
                    }
                    is NormalizedRepResult.Rejected -> Unit
                }
            }
        }
    }

    private fun restoreDraftIfNeeded(ownerId: String) {
        if (restoredForOwnerId == ownerId) return
        restoredForOwnerId = ownerId

        val fromSaved = SetDraftSavedStateCodec.decodeAndValidate(
            raw = savedStateHandle.get<String>(draftJsonKey),
            expectedOwnerId = ownerId,
            expectedEnvironment = executionEnvironment,
            expectedSessionId = sessionId,
            expectedExerciseId = exerciseId,
            expectedSetIndex = setIndex,
            currentElapsedMs = elapsedRealtimeMs(),
        )
        val fromStore = setDraftStore?.peek(ownerId, executionEnvironment, sessionId, exerciseId, setIndex)
        val restored = when {
            fromSaved != null && fromStore != null ->
                if (fromStore.repRecords.size >= fromSaved.repRecords.size) fromStore else fromSaved
            fromSaved != null -> fromSaved
            else -> fromStore
        }

        if (restored != null) {
            ledgerFlow.value = restored.repRecords
            if (restored.repRecords.isNotEmpty()) {
                fullRepsFlow.value = restored.repRecords.count { it.classification == RepClassification.FULL }
                partialRepsFlow.value = restored.repRecords.count { it.classification == RepClassification.PARTIAL }
            } else {
                fullRepsFlow.value = restored.fullReps
                partialRepsFlow.value = restored.partialReps
            }
            seenEventIds.clear()
            seenEventIds.addAll(restored.acceptedEventIds)
            if (restored.hasInitializedLoad) {
                actualLoadFlow.value = restored.actualLoad
                savedStateHandle[loadGramsKey] = restored.actualLoad?.grams
                hasInitializedLoad = true
                savedStateHandle[loadInitKey] = true
            }
            if (restored.hadDeviceTracking) {
                isDeviceAssistedFlow.value = true
                savedStateHandle[deviceAssistedKey] = true
            }
            if (restored.isManualFallback) {
                isFallbackManualFlow.value = true
                savedStateHandle[fallbackKey] = true
            }
            val nowElapsed = elapsedRealtimeMs()
            if (restored.setStartedAtElapsedMs != null && savedStateHandle.get<Long>(startElapsedKey) == null) {
                val safeStart = restored.setStartedAtElapsedMs.coerceAtMost(nowElapsed)
                savedStateHandle[startElapsedKey] = safeStart
            }
        }
    }

    private fun persistDraft() {
        val ownerId = activeOwnerId ?: return
        val ledger = ledgerFlow.value
        val draft = SetExecutionDraft(
            ownerId = ownerId,
            environment = executionEnvironment,
            sessionId = sessionId,
            exerciseId = exerciseId,
            setIndex = setIndex,
            trackingWindowId = trackingWindowId,
            fullReps = fullRepsFlow.value,
            partialReps = partialRepsFlow.value,
            repRecords = ledger,
            acceptedEventIds = seenEventIds.toSet(),
            origin = when {
                isFallbackManualFlow.value -> MeasurementOrigin.MANUAL_CORRECTION
                isDeviceAssistedFlow.value && executionEnvironment == ExecutionEnvironmentKind.DEMO ->
                    MeasurementOrigin.SIMULATED_TELEMETRY
                isDeviceAssistedFlow.value -> MeasurementOrigin.SENSOR_TELEMETRY
                else -> MeasurementOrigin.MANUAL_INPUT
            },
            actualLoad = actualLoadFlow.value,
            hasInitializedLoad = hasInitializedLoad,
            setStartedAtElapsedMs = savedStateHandle.get<Long>(startElapsedKey) ?: 0L,
            trackingMode = if (isDeviceAssistedFlow.value && !isFallbackManualFlow.value) {
                TrackingMode.DEVICE_ASSISTED
            } else {
                TrackingMode.MANUAL
            },
            hadDeviceTracking = isDeviceAssistedFlow.value,
            isManualFallback = isFallbackManualFlow.value,
        )
        savedStateHandle[draftJsonKey] = SetDraftSavedStateCodec.encode(draft)
        setDraftStore?.put(draft)
    }

    /**
     * Transitions Ready -> InProgress in-place on the same screen.
     * Starts the monotonic set timer once and enables tracking/controls.
     */
    fun startSet() {
        val current = _uiState.value as? SetExecutionUiState.Active ?: return
        if (current.phase != SetExecutionPhase.Ready) return

        val startTime = savedStateHandle.get<Long>(startElapsedKey) ?: elapsedRealtimeMs()
        savedStateHandle[startElapsedKey] = startTime

        val deviceAssisted = current.canStartDeviceAssisted && !forceManualMode
        isDeviceAssistedFlow.value = deviceAssisted
        savedStateHandle[deviceAssistedKey] = deviceAssisted

        phaseFlow.value = SetExecutionPhase.InProgress
        savedStateHandle[phaseKey] = SetExecutionPhase.InProgress.name
        errorMessageFlow.value = null
        persistDraft()
    }

    fun incrementRep() {
        val current = _uiState.value as? SetExecutionUiState.Active ?: return
        if (!current.canAdjustRepsManually) return
        val isFallbackWithLedger = isDeviceAssistedFlow.value
        if (!isFallbackWithLedger) {
            if (fullRepsFlow.value + partialRepsFlow.value >= 999) {
                errorMessageFlow.value = "Maximum supported manual set size (999 reps) reached."
                return
            }
            fullRepsFlow.value += 1
            errorMessageFlow.value = null
            persistDraft()
            return
        }
        if (ledgerFlow.value.size >= SetDraftSavedStateCodec.MAX_SAVED_LEDGER_RECORDS) {
            errorMessageFlow.value =
                "Maximum supported set size (${SetDraftSavedStateCodec.MAX_SAVED_LEDGER_RECORDS} reps) reached."
            return
        }
        val nextIndex = ledgerFlow.value.size + 1
        seenEventIds.add("manual_${trackingWindowId}_$nextIndex")
        val rep = RepMeasurement(
            repIndex = nextIndex,
            classification = RepClassification.FULL,
            origin = MeasurementOrigin.MANUAL_CORRECTION,
        )
        ledgerFlow.value = ledgerFlow.value + rep
        fullRepsFlow.value = ledgerFlow.value.count { it.classification == RepClassification.FULL }
        errorMessageFlow.value = null
        persistDraft()
    }

    fun decrementRep() {
        val current = _uiState.value as? SetExecutionUiState.Active ?: return
        if (!current.canAdjustRepsManually) return
        val isFallbackWithLedger = isDeviceAssistedFlow.value
        if (!isFallbackWithLedger) {
            if (fullRepsFlow.value > 0) {
                fullRepsFlow.value -= 1
                errorMessageFlow.value = null
                persistDraft()
            }
            return
        }
        if (ledgerFlow.value.isNotEmpty()) {
            val updated = ledgerFlow.value.dropLast(1)
            ledgerFlow.value = updated
            fullRepsFlow.value = updated.count { it.classification == RepClassification.FULL }
            partialRepsFlow.value = updated.count { it.classification == RepClassification.PARTIAL }
            errorMessageFlow.value = null
            persistDraft()
        }
    }

    fun incrementWeight() {
        val current = _uiState.value as? SetExecutionUiState.Active ?: return
        if (!current.canAdjustWeight) return
        val stepGrams = if (current.preferredWeightUnit == WeightUnit.KG) 2_500L else 2_268L
        val currentGrams = actualLoadFlow.value?.grams ?: 0L
        val updated = WeightLoad(grams = currentGrams + stepGrams)
        actualLoadFlow.value = updated
        savedStateHandle[loadGramsKey] = updated.grams
        hasInitializedLoad = true
        savedStateHandle[loadInitKey] = true
        persistDraft()
    }

    fun decrementWeight() {
        val current = _uiState.value as? SetExecutionUiState.Active ?: return
        if (!current.canAdjustWeight) return
        val stepGrams = if (current.preferredWeightUnit == WeightUnit.KG) 2_500L else 2_268L
        val currentGrams = actualLoadFlow.value?.grams ?: 0L
        val updatedGrams = (currentGrams - stepGrams).coerceAtLeast(0L)
        val updated = WeightLoad(grams = updatedGrams)
        actualLoadFlow.value = updated
        savedStateHandle[loadGramsKey] = updated.grams
        hasInitializedLoad = true
        savedStateHandle[loadInitKey] = true
        persistDraft()
    }

    /**
     * Validates and applies a directly entered weight string in the user's current [WeightUnit].
     * Returns `null` on success, or a user-facing validation error message on invalid input.
     */
    fun setDirectWeight(rawInput: String): String? {
        val current = _uiState.value as? SetExecutionUiState.Active ?: return "Set is not active."
        if (!current.canAdjustWeight) return "Weight cannot be edited right now."

        val trimmed = rawInput.trim().replace(',', '.')
        if (trimmed.isEmpty()) {
            return "Enter a weight value."
        }
        val parsed = trimmed.toDoubleOrNull()
        if (parsed == null || parsed.isNaN() || parsed.isInfinite()) {
            return "Enter a valid numeric weight."
        }
        if (parsed < 0.0) {
            return "Weight cannot be negative."
        }
        val maxSupported = if (current.preferredWeightUnit == WeightUnit.KG) 500.0 else 1100.0
        val unitLabel = if (current.preferredWeightUnit == WeightUnit.KG) "kg" else "lbs"
        if (parsed > maxSupported) {
            return "Enter a weight up to ${maxSupported.toInt()} $unitLabel."
        }

        val updated = when (current.preferredWeightUnit) {
            WeightUnit.KG -> WeightLoad.fromKilograms(parsed)
            WeightUnit.LBS -> WeightLoad.fromPounds(parsed)
        }
        actualLoadFlow.value = updated
        savedStateHandle[loadGramsKey] = updated.grams
        hasInitializedLoad = true
        savedStateHandle[loadInitKey] = true
        persistDraft()
        return null
    }

    fun simulateFullRep() {
        val current = _uiState.value as? SetExecutionUiState.Active ?: return
        if (!current.showSimulateButtons) return
        viewModelScope.launch {
            fakeDeviceDataSource?.emitSimulatedRep(
                trackingWindowId = trackingWindowId,
                classification = RepClassification.FULL,
            )
        }
    }

    fun simulatePartialRep() {
        val current = _uiState.value as? SetExecutionUiState.Active ?: return
        if (!current.showSimulateButtons) return
        viewModelScope.launch {
            fakeDeviceDataSource?.emitSimulatedRep(
                trackingWindowId = trackingWindowId,
                classification = RepClassification.PARTIAL,
            )
        }
    }

    fun simulateConnectionDrop() {
        val current = _uiState.value as? SetExecutionUiState.Active ?: return
        if (!current.showSimulateButtons) return
        viewModelScope.launch {
            fakeDeviceDataSource?.simulateConnectionLoss()
        }
    }

    fun reconnectDevice() {
        val current = _uiState.value as? SetExecutionUiState.Active ?: return
        if (current.phase != SetExecutionPhase.ConnectionLost || isReconnectingFlow.value) return
        viewModelScope.launch {
            isReconnectingFlow.value = true
            reconnectErrorFlow.value = null
            when (val res = deviceRepository.reconnect()) {
                is DeviceOperationResult.Success -> {
                    isReconnectingFlow.value = false
                    phaseFlow.value = SetExecutionPhase.InProgress
                    savedStateHandle[phaseKey] = SetExecutionPhase.InProgress.name
                }
                is DeviceOperationResult.Failure -> {
                    isReconnectingFlow.value = false
                    reconnectErrorFlow.value = res.message
                }
            }
        }
    }

    /**
     * In-place manual fallback preserving all reps, ledger, timing, and selected load.
     */
    fun continueManually() {
        val current = _uiState.value as? SetExecutionUiState.Active ?: return
        if (current.phase != SetExecutionPhase.ConnectionLost) return
        isFallbackManualFlow.value = true
        savedStateHandle[fallbackKey] = true
        isReconnectingFlow.value = false
        reconnectErrorFlow.value = null
        phaseFlow.value = SetExecutionPhase.InProgress
        savedStateHandle[phaseKey] = SetExecutionPhase.InProgress.name
        persistDraft()
    }

    fun finishSet() {
        val current = _uiState.value as? SetExecutionUiState.Active ?: return
        if (current.phase != SetExecutionPhase.InProgress &&
            current.phase != SetExecutionPhase.RecoverableError
        ) return

        val ownerId = activeOwnerId ?: return
        phaseFlow.value = SetExecutionPhase.Saving
        savedStateHandle[phaseKey] = SetExecutionPhase.Saving.name
        errorMessageFlow.value = null

        viewModelScope.launch {
            val startedMs = savedStateHandle.get<Long>(startElapsedKey)
            val frozenMs = frozenDurationMsFlow.value
                ?: startedMs?.let { (elapsedRealtimeMs() - it).coerceAtLeast(0L) }
            if (frozenMs != null && frozenDurationMsFlow.value == null) {
                frozenDurationMsFlow.value = frozenMs
                savedStateHandle[frozenDurationKey] = frozenMs
            }
            val durationMs = frozenMs
            val trackingMode = if (current.isDeviceAssisted && !current.isFallbackManual) {
                TrackingMode.DEVICE_ASSISTED
            } else {
                TrackingMode.MANUAL
            }
            val origin = when {
                current.isFallbackManual -> MeasurementOrigin.MANUAL_CORRECTION
                current.isDeviceAssisted && executionEnvironment == ExecutionEnvironmentKind.DEMO ->
                    MeasurementOrigin.SIMULATED_TELEMETRY
                current.isDeviceAssisted -> MeasurementOrigin.SENSOR_TELEMETRY
                else -> MeasurementOrigin.MANUAL_INPUT
            }

            val request = RecordSetRequest(
                sessionId = sessionId,
                sessionExerciseId = exerciseId,
                setIndex = setIndex,
                fullReps = current.fullReps,
                partialReps = current.partialReps,
                actualLoad = current.actualLoad,
                trackingMode = trackingMode,
                origin = origin,
                durationMs = durationMs,
                repRecords = ledgerFlow.value,
            )

            val outcome = SetFinishHelper.submitSetAndStartRest(
                ownerId = ownerId,
                request = request,
                exerciseName = current.exerciseName,
                targetRestSeconds = current.targetRestSeconds,
                fallbackSession = latestSession,
                workoutSessionRepository = workoutSessionRepository,
                currentOwnerProvider = { activeOwnerId },
            )

            when (outcome) {
                is SetSubmissionOutcome.Success -> {
                    setDraftStore?.consume(ownerId, executionEnvironment, sessionId, exerciseId, setIndex)
                    savedStateHandle[completedKey] = true
                    savedStateHandle[completedRestStartedKey] = outcome.hasRestStarted
                    savedStateHandle[completedRestErrorKey] = outcome.restErrorMessage
                    phaseFlow.value = SetExecutionPhase.Completed
                    savedStateHandle[phaseKey] = SetExecutionPhase.Completed.name
                    val completedState = SetExecutionUiState.Completed(
                        sessionId = outcome.sessionId,
                        hasRestStarted = outcome.hasRestStarted,
                        nextTarget = outcome.nextTarget,
                        restErrorMessage = outcome.restErrorMessage,
                    )
                    completionStateFlow.value = completedState
                    _events.emit(
                        SetExecutionEvent.SetFinished(
                            sessionId = outcome.sessionId,
                            hasRestStarted = outcome.hasRestStarted,
                            nextTarget = outcome.nextTarget,
                            restErrorMessage = outcome.restErrorMessage,
                        ),
                    )
                }
                is SetSubmissionOutcome.AlreadyCompleted -> {
                    phaseFlow.value = SetExecutionPhase.InProgress
                    savedStateHandle[phaseKey] = SetExecutionPhase.InProgress.name
                    errorMessageFlow.value = "Set ${outcome.setIndex} was already recorded."
                }
                is SetSubmissionOutcome.Failure -> {
                    phaseFlow.value = SetExecutionPhase.RecoverableError
                    savedStateHandle[phaseKey] = SetExecutionPhase.RecoverableError.name
                    errorMessageFlow.value = outcome.message
                }
                SetSubmissionOutcome.OwnerChanged -> Unit
            }
        }
    }

    fun requestEndEarly() {
        showEndEarlyFlow.value = true
    }

    fun dismissEndEarly() {
        showEndEarlyFlow.value = false
    }

    fun confirmEndEarly() {
        val ownerId = activeOwnerId ?: return
        if (isEndingEarlyFlow.value) return
        viewModelScope.launch {
            isEndingEarlyFlow.value = true
            when (val res = workoutSessionRepository.endSessionEarly(ownerId, sessionId)) {
                is DomainResult.Success -> {
                    setDraftStore?.consume(ownerId, executionEnvironment, sessionId, exerciseId, setIndex)
                    isEndingEarlyFlow.value = false
                    showEndEarlyFlow.value = false
                }
                is DomainResult.Failure -> {
                    isEndingEarlyFlow.value = false
                    showEndEarlyFlow.value = false
                    errorMessageFlow.value = res.error.userMessage()
                }
            }
        }
    }

    override fun onCleared() {
        isDisposed = true
        super.onCleared()
    }

    private data class ControlSnapshot(
        val phase: SetExecutionPhase,
        val full: Int,
        val partial: Int,
        val load: WeightLoad?,
        val isFallbackManual: Boolean,
    )

    private data class StatusSnapshot(
        val isReconnecting: Boolean,
        val reconnectError: String?,
        val errorMessage: String?,
        val showEndEarly: Boolean,
        val endingEarly: Boolean,
    )
}
