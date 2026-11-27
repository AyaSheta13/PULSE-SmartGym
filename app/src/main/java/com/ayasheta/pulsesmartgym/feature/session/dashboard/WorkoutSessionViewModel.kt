package com.ayasheta.pulsesmartgym.feature.session.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.PulseValidators
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.DeviceConnectionState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceLinkState
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepository
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionProgression
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface WorkoutSessionEvent {
    data class Completed(val sessionId: String) : WorkoutSessionEvent
    data class EndedEarly(val sessionId: String, val status: SessionStatus) : WorkoutSessionEvent
    data class RedirectToSummary(val sessionId: String, val status: SessionStatus) : WorkoutSessionEvent
    data class NavigateToNextSet(
        val sessionId: String,
        val exerciseId: String,
        val setIndex: Int,
    ) : WorkoutSessionEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionViewModel(
    private val sessionId: String?,
    private val authRepository: AuthRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val deviceRepository: DeviceRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkoutSessionUiState>(WorkoutSessionUiState.Loading)
    val uiState: StateFlow<WorkoutSessionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WorkoutSessionEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<WorkoutSessionEvent> = _events.asSharedFlow()

    private val manualEntryTargetFlow = MutableStateFlow<ManualSetEntryTarget?>(null)
    private val manualEntryErrorFlow = MutableStateFlow<String?>(null)
    private val actionErrorFlow = MutableStateFlow<String?>(null)
    private val showEndEarlyModalFlow = MutableStateFlow(false)
    private val isSavingSetFlow = MutableStateFlow(false)
    private val isCompletingFlow = MutableStateFlow(false)
    private val isEndingEarlyFlow = MutableStateFlow(false)
    private val isRestBusyFlow = MutableStateFlow(false)

    private val restMutex = Mutex()
    private var activeOwnerId: String? = null
    private var handledRestDeadlineMs: Long? = null

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            authRepository.observeAuthState().flatMapLatest { authState ->
                when (authState) {
                    AuthState.Initializing -> {
                        activeOwnerId = null
                        flowOf(WorkoutSessionUiState.Loading)
                    }
                    AuthState.Unauthenticated -> {
                        activeOwnerId = null
                        clearTransientDialogs()
                        flowOf(WorkoutSessionUiState.SignedOut)
                    }
                    is AuthState.Authenticated -> {
                        val ownerId = authState.user.id
                        if (activeOwnerId != null && activeOwnerId != ownerId) {
                            clearTransientDialogs()
                        }
                        activeOwnerId = ownerId
                        if (sessionId == null) {
                            flowOf(WorkoutSessionUiState.Missing("Workout session ID is missing."))
                        } else {
                            val sessionFlow = workoutSessionRepository.observeSession(ownerId, sessionId)
                            val restFlow = workoutSessionRepository.observeRestState(ownerId, sessionId)
                            val linkFlow = deviceRepository?.linkState ?: MutableStateFlow(DeviceLinkState())
                            val tickerFlow: Flow<Long> = flow {
                                while (true) {
                                    emit(clockMs())
                                    delay(1_000L)
                                }
                            }
                            combine(
                                sessionFlow,
                                restFlow,
                                userPreferencesRepository.observePreferences(),
                                tickerFlow,
                                linkFlow,
                            ) { session, restState, prefs, nowMs, link ->
                                if (session == null) {
                                    WorkoutSessionUiState.Missing("Workout session not found or no longer active.")
                                } else if (session.status != SessionStatus.ACTIVE) {
                                    WorkoutSessionUiState.Terminal(
                                        sessionId = session.id,
                                        status = session.status,
                                    )
                                } else {
                                    val activeUi = SessionUiMapper.mapToActiveUiState(
                                        session = session,
                                        preferredWeightUnit = prefs.preferredWeightUnit,
                                        nowEpochMs = nowMs,
                                    )
                                    val restUi = restState?.let { rest ->
                                        val remaining = SessionProgression.computeRemainingRestSeconds(
                                            deadlineEpochMs = rest.deadlineEpochMs,
                                            nowEpochMs = nowMs,
                                        )
                                        SessionRestTimerUiModel(
                                            exerciseId = rest.exerciseId,
                                            exerciseName = rest.exerciseName,
                                            completedSetIndex = rest.completedSetIndex,
                                            remainingSeconds = remaining,
                                            remainingFormatted = SessionProgression.formatMmSs(remaining.toLong()),
                                            progress = SessionProgression.computeRestProgress(
                                                durationSeconds = rest.durationSeconds,
                                                deadlineEpochMs = rest.deadlineEpochMs,
                                                nowEpochMs = nowMs,
                                            ),
                                            deadlineEpochMs = rest.deadlineEpochMs,
                                            nextExerciseId = rest.nextExerciseId,
                                            nextSetIndex = rest.nextSetIndex,
                                        )
                                    }
                                    val chipStatus = when (link.phase) {
                                        DeviceConnectionState.CONNECTED -> PulseBluetoothChipStatus.CONNECTED
                                        DeviceConnectionState.SCANNING,
                                        DeviceConnectionState.CONNECTING -> PulseBluetoothChipStatus.CONNECTING
                                        DeviceConnectionState.CONNECTION_LOST -> PulseBluetoothChipStatus.CONNECTION_LOST
                                        DeviceConnectionState.FAILED -> PulseBluetoothChipStatus.FAILED
                                        DeviceConnectionState.DISCONNECTED -> PulseBluetoothChipStatus.DISCONNECTED
                                    }
                                    val connLabel = when (chipStatus) {
                                        PulseBluetoothChipStatus.CONNECTED ->
                                            if (link.isTelemetrySimulated) "Connected (Sim)" else "Connected"
                                        PulseBluetoothChipStatus.CONNECTING -> "Connecting..."
                                        PulseBluetoothChipStatus.CONNECTION_LOST -> "Connection Lost"
                                        PulseBluetoothChipStatus.FAILED -> "Failed"
                                        PulseBluetoothChipStatus.DISCONNECTED -> "Disconnected"
                                    }
                                    activeUi.copy(
                                        restTimer = restUi,
                                        connectionLabel = connLabel,
                                        bluetoothChipStatus = chipStatus,
                                        isTelemetrySimulated = link.isTelemetrySimulated,
                                    )
                                }
                            }
                        }
                    }
                }
            }.collectLatest { baseState ->
                _uiState.value = applyTransientFlags(baseState)
            }
        }
        viewModelScope.launch {
            combine(
                manualEntryTargetFlow,
                manualEntryErrorFlow,
                actionErrorFlow,
                showEndEarlyModalFlow,
                isSavingSetFlow,
            ) { target, entryError, actionError, showEndEarly, isSaving ->
                TransientState(target, entryError, actionError, showEndEarly, isSaving)
            }.combine(
                combine(isCompletingFlow, isEndingEarlyFlow, isRestBusyFlow) { c, e, r -> Triple(c, e, r) },
            ) { first, (completing, ending, restBusy) ->
                first.copy(isCompleting = completing, isEndingEarly = ending, isRestBusy = restBusy)
            }.collectLatest { transient ->
                _uiState.update { current ->
                    if (current is WorkoutSessionUiState.Active) {
                        current.copy(
                            manualEntryTarget = transient.target,
                            manualEntryError = transient.entryError,
                            actionError = transient.actionError,
                            showEndEarlyConfirmation = transient.showEndEarly,
                            isSavingSet = transient.isSaving,
                            isCompletingSession = transient.isCompleting,
                            isEndingEarly = transient.isEndingEarly,
                            isRestBusy = transient.isRestBusy,
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun applyTransientFlags(base: WorkoutSessionUiState): WorkoutSessionUiState {
        if (base !is WorkoutSessionUiState.Active) return base
        return base.copy(
            manualEntryTarget = manualEntryTargetFlow.value,
            manualEntryError = manualEntryErrorFlow.value,
            actionError = actionErrorFlow.value,
            showEndEarlyConfirmation = showEndEarlyModalFlow.value,
            isSavingSet = isSavingSetFlow.value,
            isCompletingSession = isCompletingFlow.value,
            isEndingEarly = isEndingEarlyFlow.value,
            isRestBusy = isRestBusyFlow.value,
        )
    }

    private fun clearTransientDialogs() {
        manualEntryTargetFlow.value = null
        manualEntryErrorFlow.value = null
        actionErrorFlow.value = null
        showEndEarlyModalFlow.value = false
        isSavingSetFlow.value = false
        isCompletingFlow.value = false
        isEndingEarlyFlow.value = false
        isRestBusyFlow.value = false
        handledRestDeadlineMs = null
    }

    fun refreshElapsedTime() {
        val current = _uiState.value
        if (current is WorkoutSessionUiState.Active) {
            val nowMs = clockMs()
            val updatedRest = current.restTimer?.let { rest ->
                val remaining = SessionProgression.computeRemainingRestSeconds(rest.deadlineEpochMs, nowMs)
                rest.copy(
                    remainingSeconds = remaining,
                    remainingFormatted = SessionProgression.formatMmSs(remaining.toLong()),
                )
            }
            _uiState.value = current.copy(
                elapsedSeconds = SessionUiMapper.computeElapsedSeconds(current.startedAtEpochMs, nowMs),
                restTimer = updatedRest,
            )
        }
    }

    fun extendRestTimer(additionalSeconds: Int = 30) {
        val active = _uiState.value as? WorkoutSessionUiState.Active ?: return
        val rest = active.restTimer ?: return
        val ownerId = activeOwnerId ?: return
        if (isRestBusyFlow.value || isCompletingFlow.value || isEndingEarlyFlow.value) return
        isRestBusyFlow.value = true
        actionErrorFlow.value = null
        viewModelScope.launch {
            restMutex.withLock {
                try {
                    if (activeOwnerId != ownerId) return@withLock
                    val latest = workoutSessionRepository.getSession(ownerId, active.sessionId)
                    if (latest == null || latest.status != SessionStatus.ACTIVE) return@withLock
                    when (val result = workoutSessionRepository.extendRestTimer(ownerId, active.sessionId, additionalSeconds)) {
                        is DomainResult.Success -> {
                            if (activeOwnerId == ownerId) {
                                handledRestDeadlineMs = null
                            }
                        }
                        is DomainResult.Failure -> {
                            if (activeOwnerId == ownerId) {
                                actionErrorFlow.value = "Could not extend rest timer: ${result.error.userMessage()}"
                            }
                        }
                    }
                } finally {
                    isRestBusyFlow.value = false
                }
            }
        }
    }

    fun setActionError(message: String) {
        actionErrorFlow.value = message
    }

    fun dismissActionError() {
        actionErrorFlow.value = null
    }

    fun skipRestTimer() {
        val active = _uiState.value as? WorkoutSessionUiState.Active ?: return
        val rest = active.restTimer ?: return
        val ownerId = activeOwnerId ?: return
        if (isRestBusyFlow.value || isCompletingFlow.value || isEndingEarlyFlow.value) return
        isRestBusyFlow.value = true
        actionErrorFlow.value = null
        viewModelScope.launch {
            restMutex.withLock {
                try {
                    if (activeOwnerId != ownerId) return@withLock
                    val latest = workoutSessionRepository.getSession(ownerId, active.sessionId)
                    if (latest == null || latest.status != SessionStatus.ACTIVE) return@withLock
                    when (val result = workoutSessionRepository.clearRestTimer(ownerId, active.sessionId)) {
                        is DomainResult.Success -> {
                            if (activeOwnerId == ownerId) {
                                handledRestDeadlineMs = rest.deadlineEpochMs
                            }
                        }
                        is DomainResult.Failure -> {
                            if (activeOwnerId == ownerId) {
                                actionErrorFlow.value = "Could not skip rest timer: ${result.error.userMessage()}"
                            }
                        }
                    }
                } finally {
                    isRestBusyFlow.value = false
                }
            }
        }
    }

    fun onRestExpired() {
        val active = _uiState.value as? WorkoutSessionUiState.Active ?: return
        val rest = active.restTimer ?: return
        if (rest.remainingSeconds > 0) return
        val triggeringDeadlineMs = rest.deadlineEpochMs
        if (handledRestDeadlineMs == triggeringDeadlineMs) return
        val ownerId = activeOwnerId ?: return
        handledRestDeadlineMs = triggeringDeadlineMs
        viewModelScope.launch {
            restMutex.withLock {
                if (activeOwnerId != ownerId) return@withLock
                val latest = workoutSessionRepository.getSession(ownerId, active.sessionId)
                if (latest == null || latest.status != SessionStatus.ACTIVE) return@withLock
                val persistedRest = workoutSessionRepository.observeRestState(ownerId, active.sessionId).first()
                    ?: return@withLock
                if (persistedRest.deadlineEpochMs != triggeringDeadlineMs) return@withLock
                if (clockMs() < persistedRest.deadlineEpochMs) return@withLock
                when (val result = workoutSessionRepository.clearRestTimer(ownerId, active.sessionId)) {
                    is DomainResult.Success -> Unit
                    is DomainResult.Failure -> {
                        if (activeOwnerId == ownerId) {
                            actionErrorFlow.value = "Rest timer finished, but could not clear timer: ${result.error.userMessage()}"
                        }
                    }
                }
            }
        }
    }

    fun startSetFromDashboard(sessionExerciseId: String, setIndex: Int) {
        val active = _uiState.value as? WorkoutSessionUiState.Active ?: return
        val ownerId = activeOwnerId ?: return
        if (isRestBusyFlow.value || isCompletingFlow.value || isEndingEarlyFlow.value || isSavingSetFlow.value) return
        val targetRow = active.exercises
            .firstOrNull { it.sessionExerciseId == sessionExerciseId }
            ?.sets
            ?.firstOrNull { it.setIndex == setIndex }
        if (targetRow == null || targetRow.performed != null) return

        if (active.restTimer == null) {
            _events.tryEmit(WorkoutSessionEvent.NavigateToNextSet(active.sessionId, sessionExerciseId, setIndex))
            return
        }

        isRestBusyFlow.value = true
        actionErrorFlow.value = null
        viewModelScope.launch {
            restMutex.withLock {
                try {
                    if (activeOwnerId != ownerId) return@withLock
                    val latest = workoutSessionRepository.getSession(ownerId, active.sessionId)
                    if (latest == null || latest.status != SessionStatus.ACTIVE) return@withLock
                    val stillUncompleted = latest.exercises
                        .firstOrNull { it.id == sessionExerciseId }
                        ?.plannedSets?.any { it.setIndex == setIndex } == true &&
                        latest.exercises
                            .firstOrNull { it.id == sessionExerciseId }
                            ?.performedSets?.none { it.setIndex == setIndex } == true
                    if (!stillUncompleted) return@withLock

                    when (val result = workoutSessionRepository.clearRestTimer(ownerId, active.sessionId)) {
                        is DomainResult.Success -> {
                            if (activeOwnerId == ownerId) {
                                _events.tryEmit(
                                    WorkoutSessionEvent.NavigateToNextSet(
                                        sessionId = active.sessionId,
                                        exerciseId = sessionExerciseId,
                                        setIndex = setIndex,
                                    ),
                                )
                            }
                        }
                        is DomainResult.Failure -> {
                            if (activeOwnerId == ownerId) {
                                actionErrorFlow.value = "Could not clear active rest timer: ${result.error.userMessage()}"
                            }
                        }
                    }
                } finally {
                    isRestBusyFlow.value = false
                }
            }
        }
    }

    fun openSetEntry(sessionExerciseId: String, setIndex: Int) {
        val active = _uiState.value as? WorkoutSessionUiState.Active ?: return
        if (isSavingSetFlow.value || isCompletingFlow.value || isEndingEarlyFlow.value) return
        val exercise = active.exercises.firstOrNull { it.sessionExerciseId == sessionExerciseId } ?: return
        val row = exercise.sets.firstOrNull { it.setIndex == setIndex } ?: return
        val performed = row.performed
        manualEntryErrorFlow.value = null
        actionErrorFlow.value = null
        manualEntryTargetFlow.value = ManualSetEntryTarget(
            sessionExerciseId = sessionExerciseId,
            exerciseName = exercise.exerciseName,
            setIndex = setIndex,
            targetReps = row.targetReps,
            targetLoad = row.targetLoad,
            isCorrection = performed != null,
            existingFullReps = performed?.fullReps,
            existingPartialReps = performed?.partialReps,
            existingLoad = performed?.actualLoad,
            hasDetailedMeasurements = performed?.hasDetailedMeasurements == true,
            existingTrackingMode = performed?.trackingMode,
        )
    }

    fun dismissSetEntry() {
        if (isSavingSetFlow.value) return
        manualEntryTargetFlow.value = null
        manualEntryErrorFlow.value = null
    }

    fun submitManualSet(
        fullRepsInput: String,
        partialRepsInput: String,
        loadInput: String,
    ) {
        val active = _uiState.value as? WorkoutSessionUiState.Active ?: return
        val target = manualEntryTargetFlow.value ?: return
        val ownerId = activeOwnerId ?: return
        if (isSavingSetFlow.value || isCompletingFlow.value || isEndingEarlyFlow.value) return

        val fullReps = fullRepsInput.trim().toIntOrNull()
        if (fullReps == null) {
            manualEntryErrorFlow.value = "Enter a valid whole number for full repetitions."
            return
        }
        val partialReps = if (partialRepsInput.isBlank()) {
            0
        } else {
            partialRepsInput.trim().toIntOrNull() ?: run {
                manualEntryErrorFlow.value = "Enter a valid whole number for partial repetitions."
                return
            }
        }
        PulseValidators.validatePerformedReps(fullReps, partialReps)?.let { msg ->
            manualEntryErrorFlow.value = msg
            return
        }

        val parsedLoad: WeightLoad? = if (loadInput.isBlank()) {
            null
        } else {
            val numeric = loadInput.trim().toDoubleOrNull()
            if (numeric == null || !numeric.isFinite() || numeric < 0.0) {
                manualEntryErrorFlow.value = "Enter a valid non-negative weight or leave blank."
                return
            }
            when (active.preferredWeightUnit) {
                WeightUnit.KG -> WeightLoad.fromKilograms(numeric)
                WeightUnit.LBS -> WeightLoad.fromPounds(numeric)
            }
        }

        val countsUnchanged = target.isCorrection &&
            fullReps == target.existingFullReps &&
            partialReps == target.existingPartialReps
        val correctionPolicy = if (countsUnchanged) {
            com.ayasheta.pulsesmartgym.core.model.session.RepCorrectionPolicy.PRESERVE_IF_COUNTS_MATCH
        } else {
            com.ayasheta.pulsesmartgym.core.model.session.RepCorrectionPolicy.INVALIDATE_LEDGER
        }
        val request = RecordSetRequest(
            sessionId = active.sessionId,
            sessionExerciseId = target.sessionExerciseId,
            setIndex = target.setIndex,
            fullReps = fullReps,
            partialReps = partialReps,
            actualLoad = parsedLoad,
            trackingMode = if (countsUnchanged) {
                target.existingTrackingMode ?: TrackingMode.MANUAL
            } else {
                TrackingMode.MANUAL
            },
            origin = if (target.isCorrection && !countsUnchanged) {
                MeasurementOrigin.MANUAL_CORRECTION
            } else {
                MeasurementOrigin.MANUAL_INPUT
            },
            durationMs = null,
            repRecords = emptyList(),
            repCorrectionPolicy = correctionPolicy,
        )

        viewModelScope.launch {
            isSavingSetFlow.value = true
            manualEntryErrorFlow.value = null
            actionErrorFlow.value = null
            if (target.isCorrection) {
                when (val result = workoutSessionRepository.correctPerformedSet(ownerId, request)) {
                    is DomainResult.Success -> {
                        if (activeOwnerId == ownerId) {
                            manualEntryTargetFlow.value = null
                        }
                    }
                    is DomainResult.Failure -> {
                        if (activeOwnerId == ownerId) {
                            manualEntryErrorFlow.value = result.error.userMessage()
                        }
                    }
                }
            } else {
                when (val result = workoutSessionRepository.recordPerformedSet(ownerId, request)) {
                    is RecordSetResult.Recorded -> {
                        if (activeOwnerId == ownerId) {
                            manualEntryTargetFlow.value = null
                        }
                    }
                    is RecordSetResult.AlreadyRecorded -> {
                        if (activeOwnerId == ownerId) {
                            manualEntryTargetFlow.value = null
                            actionErrorFlow.value = "Set ${result.setIndex} was already recorded."
                        }
                    }
                    is RecordSetResult.InvalidState -> {
                        if (activeOwnerId == ownerId) manualEntryErrorFlow.value = result.message
                    }
                    is RecordSetResult.NotFound -> {
                        if (activeOwnerId == ownerId) manualEntryErrorFlow.value = result.message
                    }
                    is RecordSetResult.ValidationError -> {
                        if (activeOwnerId == ownerId) manualEntryErrorFlow.value = result.message
                    }
                }
            }
            isSavingSetFlow.value = false
        }
    }

    fun finishWorkout() {
        val active = _uiState.value as? WorkoutSessionUiState.Active ?: return
        val ownerId = activeOwnerId ?: return
        if (!active.allSetsCompleted || isCompletingFlow.value || isEndingEarlyFlow.value || isSavingSetFlow.value) return

        viewModelScope.launch {
            isCompletingFlow.value = true
            actionErrorFlow.value = null
            when (val result = workoutSessionRepository.completeSession(ownerId, active.sessionId)) {
                is DomainResult.Success -> {
                    if (activeOwnerId == ownerId) {
                        _uiState.value = WorkoutSessionUiState.Terminal(result.data.id, result.data.status)
                        _events.emit(WorkoutSessionEvent.Completed(result.data.id))
                    }
                }
                is DomainResult.Failure -> {
                    if (activeOwnerId == ownerId) {
                        actionErrorFlow.value = result.error.userMessage()
                    }
                }
            }
            isCompletingFlow.value = false
        }
    }

    fun requestEndEarly() {
        val active = _uiState.value as? WorkoutSessionUiState.Active ?: return
        if (isCompletingFlow.value || isEndingEarlyFlow.value || isSavingSetFlow.value) return
        if (active.allSetsCompleted) return
        actionErrorFlow.value = null
        showEndEarlyModalFlow.value = true
    }

    fun dismissEndEarly() {
        if (isEndingEarlyFlow.value) return
        showEndEarlyModalFlow.value = false
    }

    fun confirmEndEarly() {
        val active = _uiState.value as? WorkoutSessionUiState.Active ?: return
        val ownerId = activeOwnerId ?: return
        if (isEndingEarlyFlow.value || isCompletingFlow.value || isSavingSetFlow.value) return

        viewModelScope.launch {
            isEndingEarlyFlow.value = true
            actionErrorFlow.value = null
            when (val result = workoutSessionRepository.endSessionEarly(ownerId, active.sessionId)) {
                is DomainResult.Success -> {
                    if (activeOwnerId == ownerId) {
                        showEndEarlyModalFlow.value = false
                        _uiState.value = WorkoutSessionUiState.Terminal(result.data.id, result.data.status)
                        _events.emit(WorkoutSessionEvent.EndedEarly(result.data.id, result.data.status))
                    }
                }
                is DomainResult.Failure -> {
                    if (activeOwnerId == ownerId) {
                        showEndEarlyModalFlow.value = false
                        actionErrorFlow.value = result.error.userMessage()
                    }
                }
            }
            isEndingEarlyFlow.value = false
        }
    }

    private data class TransientState(
        val target: ManualSetEntryTarget?,
        val entryError: String?,
        val actionError: String?,
        val showEndEarly: Boolean,
        val isSaving: Boolean,
        val isCompleting: Boolean = false,
        val isEndingEarly: Boolean = false,
        val isRestBusy: Boolean = false,
    )
}
