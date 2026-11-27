package com.ayasheta.pulsesmartgym.feature.session.execution

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.Button as PulsePrimaryButton
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.component.workout.PulseRepCounterGauge
import com.ayasheta.pulsesmartgym.core.designsystem.component.workout.PulseWeightSelectorCard
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.CheckMark
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.session.SessionSetTarget
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import kotlinx.coroutines.delay
import java.util.Locale
import com.ayasheta.pulsesmartgym.feature.session.dashboard.EndWorkoutEarlyModal
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionUiMapper
import com.ayasheta.pulsesmartgym.feature.session.ready.SetExecutionTopBar
import com.ayasheta.pulsesmartgym.feature.session.ready.SetFallbackMessage
import com.ayasheta.pulsesmartgym.feature.session.ready.StopCircleIcon
import com.ayasheta.pulsesmartgym.feature.session.ready.formatLoadNumber
import com.ayasheta.pulsesmartgym.feature.session.recovery.ConnectionLostRecoverySheet
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette
import com.ayasheta.pulsesmartgym.feature.workout.editor.LocalRenderModalsInWindow

@Composable
fun SetExecutionScreen(
    viewModel: SetExecutionViewModel,
    onBackClick: () -> Unit,
    onSetFinished: (sessionId: String, hasRestStarted: Boolean, nextTarget: SessionSetTarget?, restErrorMessage: String?) -> Unit,
    onSessionCompleted: (String) -> Unit,
    onSessionEndedEarly: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val handledTerminalId = remember(viewModel) { mutableStateOf<String?>(null) }
    val handledCompleted = remember(viewModel) { mutableStateOf(false) }

    val activeState = uiState as? SetExecutionUiState.Active
    BackHandler(
        enabled = activeState != null && activeState.isStarted && !activeState.showEndEarlyConfirmation,
    ) {
        viewModel.requestEndEarly()
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SetExecutionUiState.Terminal -> {
                if (handledTerminalId.value != state.sessionId) {
                    handledTerminalId.value = state.sessionId
                    if (state.status == SessionStatus.COMPLETED) {
                        onSessionCompleted(state.sessionId)
                    } else {
                        onSessionEndedEarly(state.sessionId)
                    }
                }
            }
            is SetExecutionUiState.Completed -> {
                if (!handledCompleted.value) {
                    handledCompleted.value = true
                    onSetFinished(state.sessionId, state.hasRestStarted, state.nextTarget, state.restErrorMessage)
                }
            }
            is SetExecutionUiState.AlreadyCompleted -> {
                if (!handledCompleted.value) {
                    handledCompleted.value = true
                    onBackClick()
                }
            }
            else -> Unit
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SetExecutionEvent.SetFinished -> {
                    if (!handledCompleted.value) {
                        handledCompleted.value = true
                        onSetFinished(event.sessionId, event.hasRestStarted, event.nextTarget, event.restErrorMessage)
                    }
                }
            }
        }
    }

    SetExecutionScreenContent(
        uiState = uiState,
        onBackClick = {
            if (activeState != null && activeState.isStarted) {
                viewModel.requestEndEarly()
            } else {
                onBackClick()
            }
        },
        onStartSetClick = viewModel::startSet,
        onFinishSetClick = viewModel::finishSet,
        onIncrementRep = viewModel::incrementRep,
        onDecrementRep = viewModel::decrementRep,
        onIncrementWeight = viewModel::incrementWeight,
        onDecrementWeight = viewModel::decrementWeight,
        onSimulateFullRep = viewModel::simulateFullRep,
        onSimulatePartialRep = viewModel::simulatePartialRep,
        onSimulateDrop = viewModel::simulateConnectionDrop,
        onReconnectDevice = viewModel::reconnectDevice,
        onContinueManually = viewModel::continueManually,
        onRequestEndEarly = viewModel::requestEndEarly,
        onDismissEndEarly = viewModel::dismissEndEarly,
        onConfirmEndEarly = viewModel::confirmEndEarly,
        modifier = modifier,
    )
}

@Composable
fun SetExecutionScreenContent(
    uiState: SetExecutionUiState,
    onBackClick: () -> Unit,
    onStartSetClick: () -> Unit,
    onFinishSetClick: () -> Unit,
    onIncrementRep: () -> Unit,
    onDecrementRep: () -> Unit,
    onIncrementWeight: () -> Unit,
    onDecrementWeight: () -> Unit,
    onSimulateFullRep: () -> Unit = {},
    onSimulatePartialRep: () -> Unit = {},
    onSimulateDrop: () -> Unit = {},
    onReconnectDevice: () -> Unit = {},
    onContinueManually: () -> Unit = {},
    onRequestEndEarly: () -> Unit = {},
    onDismissEndEarly: () -> Unit = {},
    onConfirmEndEarly: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val renderInWindow = LocalRenderModalsInWindow.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WorkoutPalette.Background)
            .testTag("set_execution_screen"),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                SetExecutionUiState.Loading, is SetExecutionUiState.Terminal, is SetExecutionUiState.Completed -> {
                    SetExecutionTopBar(
                        title = "Set Execution",
                        subtitle = "Loading...",
                        modeLabel = "Manual",
                        onBackClick = onBackClick,
                    )
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PulseLoadingIndicator(modifier = Modifier.testTag("set_execution_loading"))
                    }
                }

                SetExecutionUiState.SignedOut -> {
                    SetExecutionTopBar(
                        title = "Set Execution",
                        subtitle = "Signed out",
                        modeLabel = "Manual",
                        onBackClick = onBackClick,
                    )
                    SetFallbackMessage(
                        message = "Sign in to continue your workout session.",
                        actionLabel = "Back",
                        onAction = onBackClick,
                    )
                }

                is SetExecutionUiState.Missing -> {
                    SetExecutionTopBar(
                        title = "Set Execution",
                        subtitle = "Unavailable",
                        modeLabel = "Manual",
                        onBackClick = onBackClick,
                    )
                    SetFallbackMessage(
                        message = uiState.message,
                        actionLabel = "Back to Workout",
                        onAction = onBackClick,
                    )
                }

                is SetExecutionUiState.AlreadyCompleted -> {
                    SetExecutionTopBar(
                        title = uiState.exerciseName,
                        subtitle = "Set ${uiState.setIndex} • Completed",
                        modeLabel = "Manual",
                        onBackClick = onBackClick,
                    )
                    SetFallbackMessage(
                        message = "Set ${uiState.setIndex} is already recorded. Return to the workout dashboard to pick another set or edit it.",
                        actionLabel = "Back to Workout",
                        onAction = onBackClick,
                    )
                }

                is SetExecutionUiState.Active -> {
                    val phaseSubtitle = when (uiState.phase) {
                        SetExecutionPhase.Ready -> "Set ${uiState.setIndex} of ${uiState.totalSetsForExercise} • Ready"
                        SetExecutionPhase.InProgress -> "Set ${uiState.setIndex} of ${uiState.totalSetsForExercise} • In Progress"
                        SetExecutionPhase.Saving -> "Set ${uiState.setIndex} of ${uiState.totalSetsForExercise} • Saving"
                        SetExecutionPhase.ConnectionLost -> "Set ${uiState.setIndex} of ${uiState.totalSetsForExercise} • Connection Lost"
                        SetExecutionPhase.RecoverableError -> "Set ${uiState.setIndex} of ${uiState.totalSetsForExercise} • Retry Save"
                        SetExecutionPhase.Completed -> "Set ${uiState.setIndex} of ${uiState.totalSetsForExercise} • Completed"
                    }

                    SetExecutionTopBar(
                        title = uiState.exerciseName,
                        subtitle = "$phaseSubtitle • ${uiState.modeLabel}",
                        modeLabel = uiState.modeLabel,
                        onBackClick = onBackClick,
                        bluetoothStatus = uiState.bluetoothChipStatus,
                        isSimulated = uiState.isTelemetrySimulated,
                    )

                    val unitLabel = if (uiState.preferredWeightUnit == WeightUnit.KG) "kg" else "lbs"
                    val targetLoadText = SessionUiMapper.formatWeightLoad(uiState.targetLoad, uiState.preferredWeightUnit)
                    val targetPillText = if (targetLoadText != null) {
                        "TARGET: ${uiState.targetReps} REPS @ ${targetLoadText.uppercase()}"
                    } else {
                        "TARGET: ${uiState.targetReps} REPS"
                    }
                    val weightNumberText = formatLoadNumber(uiState.actualLoad, uiState.preferredWeightUnit)
                    val targetCardLabel = if (targetLoadText != null) {
                        "Target: $targetLoadText"
                    } else {
                        "Bodyweight"
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicText(
                                text = uiState.planName,
                                style = PulseTheme.typography.cardTitle.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WorkoutPalette.TextPrimary,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            val restSeconds = uiState.targetRestSeconds
                            if (restSeconds != null && restSeconds > 0) {
                                BasicText(
                                    text = "Rest: ${restSeconds}s",
                                    style = PulseTheme.typography.label.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = WorkoutPalette.TextMuted,
                                    ),
                                    modifier = Modifier.testTag("set_execution_rest_info"),
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(PulseTheme.corners.fullRounded)
                                .background(PulseTheme.color.surfaceMuted)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("set_execution_target_pill"),
                        ) {
                            BasicText(
                                text = targetPillText,
                                style = PulseTheme.typography.label.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp,
                                    color = PulseTheme.color.shadeSecondary,
                                ),
                            )
                        }

                        if (uiState.trackingCompatibilityNotice != null && !uiState.isStarted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF4DC))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                BasicText(
                                    text = uiState.trackingCompatibilityNotice,
                                    style = PulseTheme.typography.contentPrimary.copy(
                                        fontSize = 12.sp,
                                        color = Color(0xFF92400E),
                                    ),
                                )
                            }
                        }

                        if (uiState.errorMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFEECEB))
                                    .border(BorderStroke(1.dp, Color(0xFFF5B7B1)), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("set_execution_error"),
                            ) {
                                BasicText(
                                    text = uiState.errorMessage,
                                    style = PulseTheme.typography.contentPrimary.copy(
                                        fontSize = 13.sp,
                                        color = WorkoutPalette.ErrorText,
                                    ),
                                )
                            }
                        }

                        // Unified Counter Card — stays mounted at identical coordinates across Ready & InProgress
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (uiState.isStarted) 1f else 0.92f)
                                .testTag("set_execution_counter_card"),
                        ) {
                            PulseRepCounterGauge(
                                currentReps = uiState.totalDisplayedReps,
                                targetReps = uiState.targetReps,
                                onDecrementRep = { if (uiState.canAdjustRepsManually) onDecrementRep() },
                                onIncrementRep = { if (uiState.canAdjustRepsManually) onIncrementRep() },
                                overlineLabel = if (uiState.isDeviceAssisted) "AUTO REPS" else "REPS DONE",
                                adjustLabel = when {
                                    !uiState.isStarted -> "Start set to count"
                                    uiState.isDeviceAssisted -> "Tracking via device"
                                    else -> "Adjust count"
                                },
                            )
                        }

                        PulseWeightSelectorCard(
                            weightText = weightNumberText,
                            unitLabel = unitLabel,
                            setSubtitle = "Actual load used for Set ${uiState.setIndex}",
                            targetText = targetCardLabel,
                            onDecrementClick = { if (uiState.canAdjustWeight) onDecrementWeight() },
                            onIncrementClick = { if (uiState.canAdjustWeight) onIncrementWeight() },
                            modifier = Modifier.testTag("set_execution_weight_card"),
                        )

                        var setElapsedSeconds by remember(uiState.sessionId, uiState.exerciseId, uiState.setIndex) {
                            mutableLongStateOf(0L)
                        }
                        LaunchedEffect(uiState.phase, uiState.startedElapsedRealtimeMs) {
                            val startMs = uiState.startedElapsedRealtimeMs
                            if (startMs == null) {
                                setElapsedSeconds = 0L
                            } else {
                                while (true) {
                                    val elapsed = ((android.os.SystemClock.elapsedRealtime() - startMs).coerceAtLeast(0L)) / 1000L
                                    setElapsedSeconds = elapsed
                                    if (uiState.phase != SetExecutionPhase.InProgress &&
                                        uiState.phase != SetExecutionPhase.ConnectionLost
                                    ) {
                                        break
                                    }
                                    delay(500L)
                                }
                            }
                        }

                        LivePerformanceCard(
                            setElapsedSeconds = setElapsedSeconds,
                            repRecords = uiState.repRecords,
                            isFallbackManual = uiState.isFallbackManual,
                            modifier = Modifier.testTag("set_execution_live_performance"),
                        )

                        ExpandableRepetitionBreakdown(
                            repRecords = uiState.repRecords,
                            modifier = Modifier.testTag("set_execution_rep_breakdown"),
                        )

                        if (uiState.showSimulateButtons) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SimActionChip(
                                    label = "+ Full Rep",
                                    onClick = onSimulateFullRep,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("button_sim_full_rep"),
                                )
                                SimActionChip(
                                    label = "+ Partial",
                                    onClick = onSimulatePartialRep,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("button_sim_partial_rep"),
                                )
                                SimActionChip(
                                    label = "Drop BLE",
                                    onClick = onSimulateDrop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("button_sim_drop_ble"),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val primaryCaption = when (uiState.phase) {
                            SetExecutionPhase.Ready -> "Start Set"
                            SetExecutionPhase.Saving -> "Saving Set..."
                            SetExecutionPhase.RecoverableError -> "Retry Finish Set"
                            else -> "Finish Set"
                        }
                        PulsePrimaryButton(
                            caption = primaryCaption,
                            icon = rememberVectorPainter(CheckMark),
                            isEnabled = !uiState.isSaving && !uiState.showConnectionLostModal,
                            isLoading = uiState.isSaving,
                            onClick = {
                                if (uiState.phase == SetExecutionPhase.Ready) {
                                    onStartSetClick()
                                } else {
                                    onFinishSetClick()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("button_set_execution_primary"),
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !uiState.isEndingEarly, onClick = onRequestEndEarly)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("button_set_execution_end_workout"),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StopCircleIcon()
                            BasicText(
                                text = "End Workout",
                                style = PulseTheme.typography.contentPrimary.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WorkoutPalette.TextPrimary,
                                ),
                            )
                        }
                    }

                    ConnectionLostRecoverySheet(
                        visible = uiState.showConnectionLostModal,
                        setIndex = uiState.setIndex,
                        fullReps = uiState.fullReps,
                        partialReps = uiState.partialReps,
                        recordedSetsCount = uiState.recordedSetsCount,
                        isReconnecting = uiState.isReconnecting,
                        reconnectErrorMessage = uiState.reconnectError,
                        onReconnectClick = onReconnectDevice,
                        onContinueManuallyClick = onContinueManually,
                    )

                    EndWorkoutEarlyModal(
                        visible = uiState.showEndEarlyConfirmation,
                        recordedSetsCount = uiState.recordedSetsCount,
                        isEnding = uiState.isEndingEarly,
                        onKeepWorkingOut = onDismissEndEarly,
                        onEndWorkout = onConfirmEndEarly,
                        inWindow = renderInWindow,
                    )
                }
            }
        }
    }
}

@Composable
private fun SimActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF6F4F2))
            .border(BorderStroke(1.dp, Color(0xFFE4E0DC)), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = PulseTheme.typography.label.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = WorkoutPalette.TextPrimary,
            ),
        )
    }
}

@Composable
private fun LivePerformanceCard(
    setElapsedSeconds: Long,
    repRecords: List<RepMeasurement>,
    isFallbackManual: Boolean,
    modifier: Modifier = Modifier,
) {
    val latestMeasuredRep = repRecords.lastOrNull {
        it.angularRomDegrees != null ||
            it.linearRomCm != null ||
            it.peakAccelerationMetersPerSecondSquared != null ||
            it.totalDurationMs != null
    }
    val romValueText = when {
        latestMeasuredRep == null -> "—"
        latestMeasuredRep.angularRomDegrees != null && latestMeasuredRep.linearRomCm != null ->
            String.format(Locale.US, "%.0f° / %.0f cm", latestMeasuredRep.angularRomDegrees, latestMeasuredRep.linearRomCm)
        latestMeasuredRep.angularRomDegrees != null ->
            String.format(Locale.US, "%.1f°", latestMeasuredRep.angularRomDegrees)
        latestMeasuredRep.linearRomCm != null ->
            String.format(Locale.US, "%.1f cm", latestMeasuredRep.linearRomCm)
        else -> "—"
    }
    val accelValueText = latestMeasuredRep?.peakAccelerationMetersPerSecondSquared?.let {
        String.format(Locale.US, "%.2f m/s²", it)
    } ?: "—"
    val repDurValueText = latestMeasuredRep?.totalDurationMs?.let {
        String.format(Locale.US, "%.2f s", it / 1000.0)
    } ?: "—"

    val repTag = when {
        latestMeasuredRep == null -> "Awaiting rep"
        isFallbackManual -> "Earlier Rep #${latestMeasuredRep.repIndex}"
        else -> "Rep #${latestMeasuredRep.repIndex}"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFECE8E3)), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Live Performance",
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
            )
            BasicText(
                text = repTag,
                style = PulseTheme.typography.label.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WorkoutPalette.TextMuted,
                ),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricTile(
                label = "SET DURATION",
                value = SessionUiMapper.formatElapsedMmSs(setElapsedSeconds),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "RANGE OF MOTION",
                value = romValueText,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricTile(
                label = "PEAK ACCEL",
                value = accelValueText,
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "REP DURATION",
                value = repDurValueText,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFAF8F6))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            text = label,
            style = PulseTheme.typography.label.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                color = WorkoutPalette.TextMuted,
            ),
            maxLines = 1,
        )
        BasicText(
            text = value,
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = WorkoutPalette.TextPrimary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ExpandableRepetitionBreakdown(
    repRecords: List<RepMeasurement>,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFECE8E3)), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 2.dp)
                .testTag("button_toggle_rep_breakdown"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Repetition Breakdown (${repRecords.size})",
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
            )
            BasicText(
                text = if (expanded) "Hide ▲" else "Show ▼",
                style = PulseTheme.typography.label.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WorkoutPalette.PrimaryCoral,
                ),
            )
        }

        if (expanded) {
            if (repRecords.isEmpty()) {
                BasicText(
                    text = "No accepted repetitions yet.",
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 13.sp,
                        color = WorkoutPalette.TextMuted,
                    ),
                )
            } else {
                repRecords.forEach { rep ->
                    val clsLabel = if (rep.classification == RepClassification.FULL) "Full" else "Partial"
                    val romPart = listOfNotNull(
                        rep.angularRomDegrees?.let { String.format(Locale.US, "%.0f°", it) },
                        rep.linearRomCm?.let { String.format(Locale.US, "%.1f cm", it) },
                    ).joinToString(" / ").ifEmpty { "—" }
                    val accelPart = rep.peakAccelerationMetersPerSecondSquared?.let {
                        String.format(Locale.US, "%.2f m/s²", it)
                    } ?: "—"
                    val durPart = rep.totalDurationMs?.let {
                        String.format(Locale.US, "%.2fs", it / 1000.0)
                    } ?: "—"
                    val originLabel = when (rep.origin) {
                        MeasurementOrigin.SIMULATED_TELEMETRY -> "Sim"
                        MeasurementOrigin.SENSOR_TELEMETRY -> "BLE"
                        MeasurementOrigin.MANUAL_CORRECTION -> "Corrected"
                        MeasurementOrigin.MANUAL_INPUT -> "Manual"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFAF8F6))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = "#${rep.repIndex} • $clsLabel ($originLabel)",
                            style = PulseTheme.typography.label.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                        BasicText(
                            text = "$romPart · $accelPart · $durPart",
                            style = PulseTheme.typography.label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = WorkoutPalette.TextMuted,
                            ),
                        )
                    }
                }
            }
        }
    }
}
