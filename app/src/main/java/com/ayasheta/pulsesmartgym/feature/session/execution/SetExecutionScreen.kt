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
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.CheckMark
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.session.SessionSetTarget
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import kotlinx.coroutines.delay
import com.ayasheta.pulsesmartgym.feature.session.dashboard.EndWorkoutEarlyModal
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
        onApplyDirectWeight = viewModel::setDirectWeight,
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
    onApplyDirectWeight: (String) -> String? = { null },
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
    var showDirectWeightSheet by rememberSaveable { mutableStateOf(false) }

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
                    SetExecutionTopBar(
                        title = uiState.exerciseName,
                        subtitle = uiState.planName,
                        modeLabel = uiState.modeLabel,
                        onBackClick = onBackClick,
                        bluetoothStatus = uiState.bluetoothChipStatus,
                        isSimulated = uiState.isTelemetrySimulated,
                    )

                    val unitLabel = if (uiState.preferredWeightUnit == WeightUnit.KG) "kg" else "lbs"
                    val weightNumberText = formatLoadNumber(uiState.actualLoad, uiState.preferredWeightUnit)

                    var workoutElapsedSec by remember(uiState.sessionId, uiState.sessionStartedAtEpochMs) {
                        mutableLongStateOf(
                            if (uiState.sessionStartedAtEpochMs > 0L) {
                                ((System.currentTimeMillis() - uiState.sessionStartedAtEpochMs).coerceAtLeast(0L)) / 1000L
                            } else {
                                0L
                            },
                        )
                    }
                    LaunchedEffect(uiState.sessionId, uiState.sessionStartedAtEpochMs) {
                        if (uiState.sessionStartedAtEpochMs > 0L) {
                            while (true) {
                                workoutElapsedSec =
                                    ((System.currentTimeMillis() - uiState.sessionStartedAtEpochMs).coerceAtLeast(0L)) / 1000L
                                delay(1_000L)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        WorkoutInfoCard(
                            workoutElapsedSec = workoutElapsedSec,
                            restSeconds = uiState.targetRestSeconds,
                        )

                        if (uiState.trackingCompatibilityNotice != null) {
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

                        // Unified Repetition & Weight Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (uiState.isStarted) 1f else 0.94f),
                        ) {
                            UnifiedRepAndWeightCard(
                                currentReps = uiState.totalDisplayedReps,
                                targetReps = uiState.targetReps,
                                weightNumberText = weightNumberText,
                                unitLabel = unitLabel,
                                overlineLabel = if (uiState.isDeviceAssisted) "AUTO REPS" else "REPS DONE",
                                canAdjustReps = uiState.canAdjustRepsManually,
                                canAdjustWeight = uiState.canAdjustWeight,
                                onDecrementRep = { if (uiState.canAdjustRepsManually) onDecrementRep() },
                                onIncrementRep = { if (uiState.canAdjustRepsManually) onIncrementRep() },
                                onDecrementWeight = { if (uiState.canAdjustWeight) onDecrementWeight() },
                                onIncrementWeight = { if (uiState.canAdjustWeight) onIncrementWeight() },
                                onEditWeightClick = { if (uiState.canAdjustWeight) showDirectWeightSheet = true },
                            )
                        }

                        LivePerformanceWaveformCard(
                            repRecords = uiState.repRecords,
                            targetReps = uiState.targetReps,
                            isStarted = uiState.isStarted,
                            isDeviceAssisted = uiState.isDeviceAssisted,
                            isFallbackManual = uiState.isFallbackManual,
                            isSimulated = uiState.isTelemetrySimulated,
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
                    }

                    // Pinned Bottom Action Area — always visible without scrolling, respects system navigation insets
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WorkoutPalette.Background)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
                }
            }
        }

        if (uiState is SetExecutionUiState.Active) {
            val unitLabel = if (uiState.preferredWeightUnit == WeightUnit.KG) "kg" else "lbs"
            val weightNumberText = formatLoadNumber(uiState.actualLoad, uiState.preferredWeightUnit)

            SetLoadBottomSheet(
                visible = showDirectWeightSheet,
                initialWeightText = weightNumberText,
                unitLabel = unitLabel,
                inWindow = renderInWindow,
                onDismiss = { showDirectWeightSheet = false },
                onApply = onApplyDirectWeight,
            )

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
