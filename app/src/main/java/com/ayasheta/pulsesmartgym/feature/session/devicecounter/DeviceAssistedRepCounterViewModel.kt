package com.ayasheta.pulsesmartgym.feature.session.devicecounter

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceAssistedRepCounterViewModel(
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
) : ViewModel() {

    private val stateKeyPrefix = "device_counter_${sessionId}_${exerciseId}_${setIndex}"
    private val fullRepsKey = "${stateKeyPrefix}_full_reps"
    private val partialRepsKey = "${stateKeyPrefix}_partial_reps"
    private val loadGramsKey = "${stateKeyPrefix}_load_grams"
    private val loadInitKey = "${stateKeyPrefix}_load_init"
    private val startElapsedKey = "${stateKeyPrefix}_start_elapsed"
    private val completedKey = "${stateKeyPrefix}_completed"
    private val completedRestStartedKey = "${stateKeyPrefix}_completed_rest"
    private val completedRestErrorKey = "${stateKeyPrefix}_completed_rest_err"

    private val trackingWindowId = "${sessionId}_${exerciseId}_${setIndex}"

    private val _uiState = MutableStateFlow<DeviceAssistedRepCounterUiState>(DeviceAssistedRepCounterUiState.Loading)
    val uiState: StateFlow<DeviceAssistedRepCounterUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DeviceAssistedCounterEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<DeviceAssistedCounterEvent> = _events.asSharedFlow()

    private val fullRepsFlow = MutableStateFlow(savedStateHandle.get<Int>(fullRepsKey) ?: 0)
    private val partialRepsFlow = MutableStateFlow(savedStateHandle.get<Int>(partialRepsKey) ?: 0)
    private val ledgerFlow = MutableStateFlow<List<RepMeasurement>>(emptyList())
    private val seenEventIds = HashSet<String>()

    private val actualLoadFlow = MutableStateFlow<WeightLoad?>(
        savedStateHandle.get<Long>(loadGramsKey)?.let { WeightLoad(it) },
    )
    private val isReconnectingFlow = MutableStateFlow(false)
    private val reconnectErrorFlow = MutableStateFlow<String?>(null)
    private val isSubmittingFlow = MutableStateFlow(false)
    private val errorMessageFlow = MutableStateFlow<String?>(null)
    private val showEndEarlyFlow = MutableStateFlow(false)
    private val isEndingEarlyFlow = MutableStateFlow(false)

    private val completionStateFlow = MutableStateFlow<DeviceAssistedRepCounterUiState.Completed?>(
        if (savedStateHandle.get<Boolean>(completedKey) == true) {
            DeviceAssistedRepCounterUiState.Completed(
                sessionId = sessionId,
                hasRestStarted = savedStateHandle.get<Boolean>(completedRestStartedKey) ?: false,
                restErrorMessage = savedStateHandle.get<String>(completedRestErrorKey),
            )
        } else {
            null
        },
    )

    private var activeOwnerId: String? = null
    private var hasInitializedLoad = savedStateHandle.get<Boolean>(loadInitKey) ?: false
    private var latestSession: WorkoutSession? = null
    private val setStartedAtElapsedMs: Long = savedStateHandle.get<Long>(startElapsedKey)
        ?: elapsedRealtimeMs().also { savedStateHandle[startElapsedKey] = it }

    init {
        observeState()
        collectDeviceEvents()
    }

    private fun observeState() {
        viewModelScope.launch {
            authRepository.observeAuthState().flatMapLatest { authState ->
                when (authState) {
                    AuthState.Initializing -> {
                        activeOwnerId = null
                        flowOf(DeviceAssistedRepCounterUiState.Loading)
                    }
                    AuthState.Unauthenticated -> {
                        activeOwnerId = null
                        clearTransient()
                        flowOf(DeviceAssistedRepCounterUiState.SignedOut)
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
                            deviceRepository.linkState,
                        ) { session, prefs, link ->
                            latestSession = session
                            if (session == null) {
                                DeviceAssistedRepCounterUiState.Missing("Workout session not found.")
                            } else if (session.status != SessionStatus.ACTIVE) {
                                DeviceAssistedRepCounterUiState.Terminal(session.id, session.status)
                            } else {
                                val exercise = session.exercises.firstOrNull { it.id == exerciseId }
                                val plannedSet = exercise?.plannedSets?.firstOrNull { it.setIndex == setIndex }
                                if (exercise == null || plannedSet == null) {
                                    DeviceAssistedRepCounterUiState.Missing("Set $setIndex could not be found in this session.")
                                } else if (exercise.performedSets.any { it.setIndex == setIndex }) {
                                    DeviceAssistedRepCounterUiState.AlreadyCompleted(exercise.exerciseNameSnapshot, setIndex)
                                } else {
                                    if (!hasInitializedLoad) {
                                        hasInitializedLoad = true
                                        savedStateHandle[loadInitKey] = true
                                        actualLoadFlow.value = plannedSet.targetLoad
                                        plannedSet.targetLoad?.let { savedStateHandle[loadGramsKey] = it.grams }
                                    }
                                    val btChip = when (link.phase) {
                                        DeviceConnectionState.CONNECTED -> PulseBluetoothChipStatus.CONNECTED
                                        DeviceConnectionState.CONNECTING,
                                        DeviceConnectionState.SCANNING,
                                        -> PulseBluetoothChipStatus.CONNECTING
                                        DeviceConnectionState.CONNECTION_LOST,
                                        DeviceConnectionState.FAILED,
                                        -> PulseBluetoothChipStatus.CONNECTION_LOST
                                        DeviceConnectionState.DISCONNECTED -> PulseBluetoothChipStatus.DISCONNECTED
                                    }
                                    val showRecovery = link.phase == DeviceConnectionState.CONNECTION_LOST ||
                                        link.phase == DeviceConnectionState.FAILED ||
                                        link.phase == DeviceConnectionState.DISCONNECTED
                                    DeviceAssistedRepCounterUiState.Active(
                                        sessionId = session.id,
                                        exerciseId = exercise.id,
                                        setIndex = plannedSet.setIndex,
                                        totalSetsForExercise = exercise.plannedSets.size,
                                        planName = session.planNameSnapshot,
                                        exerciseName = exercise.exerciseNameSnapshot,
                                        targetReps = plannedSet.targetReps,
                                        fullReps = fullRepsFlow.value,
                                        partialReps = partialRepsFlow.value,
                                        acceptedLedger = ledgerFlow.value,
                                        targetLoad = plannedSet.targetLoad,
                                        actualLoad = actualLoadFlow.value,
                                        targetRestSeconds = plannedSet.targetRestSeconds,
                                        preferredWeightUnit = prefs.preferredWeightUnit,
                                        startedAtEpochMs = session.startedAtEpochMs,
                                        setStartedAtElapsedMs = setStartedAtElapsedMs,
                                        recordedSetsCount = session.exercises.sumOf { it.performedSets.size },
                                        deviceName = link.selectedDevice?.name ?: "PULSE Pod",
                                        connectionPhase = link.phase,
                                        bluetoothStatus = btChip,
                                        isSimulatedDevice = link.isSimulated,
                                        showRecoverySheet = showRecovery && !isSubmittingFlow.value,
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
                combine(fullRepsFlow, partialRepsFlow, ledgerFlow) { f, p, l -> Triple(f, p, l) },
                combine(actualLoadFlow, isReconnectingFlow, reconnectErrorFlow) { load, reconn, rErr -> Triple(load, reconn, rErr) },
                combine(isSubmittingFlow, errorMessageFlow, showEndEarlyFlow, isEndingEarlyFlow) { sub, err, showEnd, ending ->
                    Quad(sub, err, showEnd, ending)
                },
            ) { (full, partial, ledger), (load, reconn, rErr), (sub, err, showEnd, ending) ->
                _uiState.update { current ->
                    val completed = completionStateFlow.value
                    if (completed != null) return@update completed
                    if (current is DeviceAssistedRepCounterUiState.Active) {
                        current.copy(
                            fullReps = full,
                            partialReps = partial,
                            acceptedLedger = ledger,
                            actualLoad = load,
                            isReconnecting = reconn,
                            reconnectErrorMessage = rErr,
                            isSubmitting = sub,
                            errorMessage = err,
                            showEndEarlyConfirmation = showEnd,
                            isEndingEarly = ending,
                        )
                    } else {
                        current
                    }
                }
            }.collectLatest { }
        }
    }

    private fun collectDeviceEvents() {
        viewModelScope.launch {
            deviceRepository.events.collect { event ->
                if (event !is DeviceEvent.RepRecorded) return@collect
                val link = deviceRepository.linkState.value
                val nextIndex = ledgerFlow.value.size + 1
                val normalized = DeviceEventNormalizer.normalizeAndValidate(
                    event = event.rep,
                    expectedProviderType = link.providerType,
                    expectedConnectionGeneration = link.connectionGeneration,
                    expectedTrackingWindowId = trackingWindowId,
                    isConnected = link.phase == DeviceConnectionState.CONNECTED,
                    isSubmitting = isSubmittingFlow.value || completionStateFlow.value != null,
                    capabilities = link.capabilities,
                    seenEventIds = seenEventIds,
                    nextRepIndex = nextIndex,
                )
                if (normalized is NormalizedRepResult.Accepted) {
                    seenEventIds.add(event.rep.eventId)
                    val measurement = normalized.measurement
                    ledgerFlow.value = ledgerFlow.value + measurement
                    when (measurement.classification) {
                        RepClassification.FULL -> {
                            val nextFull = fullRepsFlow.value + 1
                            fullRepsFlow.value = nextFull
                            savedStateHandle[fullRepsKey] = nextFull
                        }
                        RepClassification.PARTIAL -> {
                            val nextPartial = partialRepsFlow.value + 1
                            partialRepsFlow.value = nextPartial
                            savedStateHandle[partialRepsKey] = nextPartial
                        }
                    }
                }
            }
        }
    }

    private fun applyTransient(base: DeviceAssistedRepCounterUiState): DeviceAssistedRepCounterUiState {
        val completed = completionStateFlow.value
        if (completed != null && base !is DeviceAssistedRepCounterUiState.Terminal) {
            return completed
        }
        return if (base is DeviceAssistedRepCounterUiState.Active) {
            base.copy(
                fullReps = fullRepsFlow.value,
                partialReps = partialRepsFlow.value,
                acceptedLedger = ledgerFlow.value,
                actualLoad = actualLoadFlow.value,
                isReconnecting = isReconnectingFlow.value,
                reconnectErrorMessage = reconnectErrorFlow.value,
                isSubmitting = isSubmittingFlow.value,
                errorMessage = errorMessageFlow.value,
                showEndEarlyConfirmation = showEndEarlyFlow.value,
                isEndingEarly = isEndingEarlyFlow.value,
            )
        } else {
            base
        }
    }

    private fun clearTransient() {
        fullRepsFlow.value = 0
        partialRepsFlow.value = 0
        ledgerFlow.value = emptyList()
        seenEventIds.clear()
        actualLoadFlow.value = null
        isReconnectingFlow.value = false
        reconnectErrorFlow.value = null
        isSubmittingFlow.value = false
        errorMessageFlow.value = null
        showEndEarlyFlow.value = false
        isEndingEarlyFlow.value = false
        completionStateFlow.value = null
    }

    fun incrementManualFullRep() {
        if (isSubmittingFlow.value || completionStateFlow.value != null) return
        errorMessageFlow.value = null
        val nextFull = fullRepsFlow.value + 1
        val nextIndex = ledgerFlow.value.size + 1
        fullRepsFlow.value = nextFull
        savedStateHandle[fullRepsKey] = nextFull
        ledgerFlow.value = ledgerFlow.value + RepMeasurement(
            repIndex = nextIndex,
            classification = RepClassification.FULL,
            origin = MeasurementOrigin.MANUAL_CORRECTION,
        )
    }

    fun incrementManualPartialRep() {
        if (isSubmittingFlow.value || completionStateFlow.value != null) return
        errorMessageFlow.value = null
        val nextPartial = partialRepsFlow.value + 1
        val nextIndex = ledgerFlow.value.size + 1
        partialRepsFlow.value = nextPartial
        savedStateHandle[partialRepsKey] = nextPartial
        ledgerFlow.value = ledgerFlow.value + RepMeasurement(
            repIndex = nextIndex,
            classification = RepClassification.PARTIAL,
            origin = MeasurementOrigin.MANUAL_CORRECTION,
        )
    }

    fun decrementManualRep() {
        if (isSubmittingFlow.value || completionStateFlow.value != null) return
        errorMessageFlow.value = null
        val currentLedger = ledgerFlow.value
        if (currentLedger.isNotEmpty()) {
            val last = currentLedger.last()
            ledgerFlow.value = currentLedger.dropLast(1)
            when (last.classification) {
                RepClassification.FULL -> {
                    val next = (fullRepsFlow.value - 1).coerceAtLeast(0)
                    fullRepsFlow.value = next
                    savedStateHandle[fullRepsKey] = next
                }
                RepClassification.PARTIAL -> {
                    val next = (partialRepsFlow.value - 1).coerceAtLeast(0)
                    partialRepsFlow.value = next
                    savedStateHandle[partialRepsKey] = next
                }
            }
        } else if (fullRepsFlow.value > 0) {
            val next = fullRepsFlow.value - 1
            fullRepsFlow.value = next
            savedStateHandle[fullRepsKey] = next
        } else if (partialRepsFlow.value > 0) {
            val next = partialRepsFlow.value - 1
            partialRepsFlow.value = next
            savedStateHandle[partialRepsKey] = next
        }
    }

    fun incrementWeight() {
        val active = _uiState.value as? DeviceAssistedRepCounterUiState.Active ?: return
        if (isSubmittingFlow.value) return
        val next = stepWeight(active.actualLoad, active.preferredWeightUnit, +1)
        actualLoadFlow.value = next
        savedStateHandle[loadGramsKey] = next.grams
        savedStateHandle[loadInitKey] = true
    }

    fun decrementWeight() {
        val active = _uiState.value as? DeviceAssistedRepCounterUiState.Active ?: return
        if (isSubmittingFlow.value) return
        val next = stepWeight(active.actualLoad, active.preferredWeightUnit, -1)
        actualLoadFlow.value = next
        savedStateHandle[loadGramsKey] = next.grams
        savedStateHandle[loadInitKey] = true
    }

    fun simulateAutoRep(classification: RepClassification = RepClassification.FULL) {
        viewModelScope.launch {
            fakeDeviceDataSource?.emitSimulatedRep(
                trackingWindowId = trackingWindowId,
                classification = classification,
            )
        }
    }

    fun simulateConnectionLoss() {
        viewModelScope.launch {
            fakeDeviceDataSource?.simulateConnectionLoss()
        }
    }

    fun reconnectDevice() {
        if (isReconnectingFlow.value || isSubmittingFlow.value) return
        isReconnectingFlow.value = true
        reconnectErrorFlow.value = null
        viewModelScope.launch {
            val res = deviceRepository.reconnect()
            if (res is DeviceOperationResult.Failure) {
                reconnectErrorFlow.value = res.message
            }
            isReconnectingFlow.value = false
        }
    }

    fun continueManually() {
        val ownerId = activeOwnerId ?: return
        val active = _uiState.value as? DeviceAssistedRepCounterUiState.Active ?: return
        if (isSubmittingFlow.value || isReconnectingFlow.value) return
        setDraftStore?.put(
            SetExecutionDraft(
                ownerId = ownerId,
                sessionId = active.sessionId,
                exerciseId = active.exerciseId,
                setIndex = active.setIndex,
                fullReps = active.fullReps,
                partialReps = active.partialReps,
                repRecords = active.acceptedLedger,
                actualLoad = active.actualLoad,
                setStartedAtElapsedMs = setStartedAtElapsedMs,
                hadDeviceTracking = true,
            ),
        )
        _events.tryEmit(
            DeviceAssistedCounterEvent.FallbackToManual(
                sessionId = active.sessionId,
                exerciseId = active.exerciseId,
                setIndex = active.setIndex,
            ),
        )
    }

    fun finishSet() {
        val active = _uiState.value as? DeviceAssistedRepCounterUiState.Active ?: return
        val ownerId = activeOwnerId ?: return
        if (isSubmittingFlow.value || isEndingEarlyFlow.value) return

        val elapsedMs = (elapsedRealtimeMs() - setStartedAtElapsedMs).coerceAtLeast(0L)
        val normalizedRecords = active.acceptedLedger.mapIndexed { idx, rep ->
            rep.copy(repIndex = idx + 1)
        }
        val setOrigin = when {
            normalizedRecords.any { it.origin == MeasurementOrigin.SENSOR_TELEMETRY } ->
                MeasurementOrigin.SENSOR_TELEMETRY
            normalizedRecords.any { it.origin == MeasurementOrigin.SIMULATED_TELEMETRY } ->
                MeasurementOrigin.SIMULATED_TELEMETRY
            else -> MeasurementOrigin.MANUAL_INPUT
        }
        val request = RecordSetRequest(
            sessionId = active.sessionId,
            sessionExerciseId = active.exerciseId,
            setIndex = active.setIndex,
            fullReps = active.fullReps,
            partialReps = active.partialReps,
            actualLoad = active.actualLoad,
            trackingMode = TrackingMode.DEVICE_ASSISTED,
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
                    val completedState = DeviceAssistedRepCounterUiState.Completed(
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
                        DeviceAssistedCounterEvent.SetFinished(
                            sessionId = outcome.sessionId,
                            hasRestStarted = outcome.hasRestStarted,
                            nextTarget = outcome.nextTarget,
                            restErrorMessage = outcome.restErrorMessage,
                        ),
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

    fun requestEndWorkoutEarly() {
        if (isSubmittingFlow.value || isEndingEarlyFlow.value) return
        showEndEarlyFlow.value = true
    }

    fun dismissEndWorkoutEarly() {
        if (isEndingEarlyFlow.value) return
        showEndEarlyFlow.value = false
    }

    fun confirmEndWorkoutEarly() {
        val active = _uiState.value as? DeviceAssistedRepCounterUiState.Active ?: return
        val ownerId = activeOwnerId ?: return
        if (isEndingEarlyFlow.value || isSubmittingFlow.value) return

        isEndingEarlyFlow.value = true
        errorMessageFlow.value = null

        viewModelScope.launch {
            when (val res = workoutSessionRepository.endSessionEarly(ownerId, active.sessionId)) {
                is DomainResult.Success -> {
                    showEndEarlyFlow.value = false
                    isEndingEarlyFlow.value = false
                    _events.tryEmit(DeviceAssistedCounterEvent.SessionEnded(res.data.id, res.data.status))
                }
                is DomainResult.Failure -> {
                    showEndEarlyFlow.value = false
                    isEndingEarlyFlow.value = false
                    errorMessageFlow.value = res.error.userMessage()
                }
            }
        }
    }

    private data class Quad(
        val a: Boolean,
        val b: String?,
        val c: Boolean,
        val d: Boolean,
    )

    private fun stepWeight(current: WeightLoad?, unit: WeightUnit, deltaSign: Int): WeightLoad {
        return when (unit) {
            WeightUnit.KG -> {
                val baseKg = current?.toKilograms() ?: 0.0
                val nextKg = (baseKg + deltaSign * 2.5).coerceAtLeast(0.0)
                WeightLoad.fromKilograms(nextKg)
            }
            WeightUnit.LBS -> {
                val baseLbs = current?.toPounds() ?: 0.0
                val nextLbs = (baseLbs + deltaSign * 5.0).coerceAtLeast(0.0)
                WeightLoad.fromPounds(nextLbs)
            }
        }
    }
}
