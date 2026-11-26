package com.ayasheta.pulsesmartgym.feature.session.counter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ayasheta.pulsesmartgym.core.model.session.SessionSetTarget
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.feature.session.dashboard.EndWorkoutEarlyModal
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionUiMapper
import com.ayasheta.pulsesmartgym.feature.session.ready.SetExecutionTopBar
import com.ayasheta.pulsesmartgym.feature.session.ready.SetFallbackMessage
import com.ayasheta.pulsesmartgym.feature.session.ready.StopCircleIcon
import com.ayasheta.pulsesmartgym.feature.session.ready.formatLoadNumber
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette
import com.ayasheta.pulsesmartgym.feature.workout.editor.LocalRenderModalsInWindow

@Composable
fun ManualRepCounterScreen(
    viewModel: ManualRepCounterViewModel,
    onBackClick: () -> Unit,
    onSetFinished: (sessionId: String, hasRestStarted: Boolean, nextTarget: SessionSetTarget?) -> Unit,
    onSessionCompleted: (String) -> Unit,
    onSessionEndedEarly: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val handledTerminalId = remember(viewModel) { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        val terminal = uiState as? ManualRepCounterUiState.Terminal ?: return@LaunchedEffect
        if (handledTerminalId.value != terminal.sessionId) {
            handledTerminalId.value = terminal.sessionId
            if (terminal.status == SessionStatus.COMPLETED) {
                onSessionCompleted(terminal.sessionId)
            } else {
                onSessionEndedEarly(terminal.sessionId)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ManualRepCounterEvent.SetFinished -> {
                    onSetFinished(event.sessionId, event.hasRestStarted, event.nextTarget)
                }
            }
        }
    }

    ManualRepCounterScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onIncrementRep = viewModel::incrementRep,
        onDecrementRep = viewModel::decrementRep,
        onIncrementWeight = viewModel::incrementWeight,
        onDecrementWeight = viewModel::decrementWeight,
        onFinishSet = viewModel::finishSet,
        onRequestEndEarly = viewModel::requestEndEarly,
        onDismissEndEarly = viewModel::dismissEndEarly,
        onConfirmEndEarly = viewModel::confirmEndEarly,
        modifier = modifier,
    )
}

@Composable
fun ManualRepCounterScreenContent(
    uiState: ManualRepCounterUiState,
    onBackClick: () -> Unit,
    onIncrementRep: () -> Unit,
    onDecrementRep: () -> Unit,
    onIncrementWeight: () -> Unit,
    onDecrementWeight: () -> Unit,
    onFinishSet: () -> Unit,
    onRequestEndEarly: () -> Unit,
    onDismissEndEarly: () -> Unit,
    onConfirmEndEarly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val renderInWindow = LocalRenderModalsInWindow.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WorkoutPalette.Background)
            .testTag("manual_rep_counter_screen"),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                ManualRepCounterUiState.Loading, is ManualRepCounterUiState.Terminal -> {
                    SetExecutionTopBar(
                        title = "Manual Counter",
                        subtitle = "Loading...",
                        modeLabel = "Manual",
                        onBackClick = onBackClick,
                    )
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PulseLoadingIndicator(modifier = Modifier.testTag("counter_loading"))
                    }
                }

                ManualRepCounterUiState.SignedOut -> {
                    SetExecutionTopBar(
                        title = "Manual Counter",
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

                is ManualRepCounterUiState.Missing -> {
                    SetExecutionTopBar(
                        title = "Manual Counter",
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

                is ManualRepCounterUiState.AlreadyCompleted -> {
                    SetExecutionTopBar(
                        title = uiState.exerciseName,
                        subtitle = "Set ${uiState.setIndex} • Completed",
                        modeLabel = "Manual",
                        onBackClick = onBackClick,
                    )
                    SetFallbackMessage(
                        message = "Set ${uiState.setIndex} is already recorded.",
                        actionLabel = "Back to Workout",
                        onAction = onBackClick,
                    )
                }

                is ManualRepCounterUiState.Active -> {
                    SetExecutionTopBar(
                        title = uiState.exerciseName,
                        subtitle = "Set ${uiState.setIndex} of ${uiState.totalSetsForExercise} • Manual Tracking",
                        modeLabel = "Manual",
                        onBackClick = onBackClick,
                    )

                    val unitLabel = if (uiState.preferredWeightUnit == WeightUnit.KG) "kg" else "lbs"
                    val targetLoadText = SessionUiMapper.formatWeightLoad(uiState.targetLoad, uiState.preferredWeightUnit)
                    val targetPillText = if (targetLoadText != null) {
                        "TARGET: ${uiState.targetReps} REPS @ ${targetLoadText.uppercase()}"
                    } else {
                        "TARGET: ${uiState.targetReps} REPS"
                    }
                    val actualWeightText = formatLoadNumber(uiState.actualLoad, uiState.preferredWeightUnit)
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
                        BasicText(
                            text = uiState.planName,
                            style = PulseTheme.typography.cardTitle.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Box(
                            modifier = Modifier
                                .clip(PulseTheme.corners.fullRounded)
                                .background(PulseTheme.color.surfaceMuted)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
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

                        if (uiState.errorMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFEECEB))
                                    .border(BorderStroke(1.dp, Color(0xFFF5B7B1)), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("counter_error_banner"),
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

                        PulseRepCounterGauge(
                            currentReps = uiState.currentReps,
                            targetReps = uiState.targetReps,
                            onDecrementRep = onDecrementRep,
                            onIncrementRep = onIncrementRep,
                            modifier = Modifier.testTag("counter_rep_gauge"),
                        )

                        PulseWeightSelectorCard(
                            weightText = actualWeightText,
                            unitLabel = unitLabel,
                            setSubtitle = "Actual load used for Set ${uiState.setIndex}",
                            targetText = targetCardLabel,
                            onDecrementClick = onDecrementWeight,
                            onIncrementClick = onIncrementWeight,
                            modifier = Modifier.testTag("counter_weight_card"),
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        PulsePrimaryButton(
                            caption = if (uiState.isSubmitting) "Saving Set..." else "Finish Set",
                            icon = rememberVectorPainter(CheckMark),
                            isLoading = uiState.isSubmitting,
                            isEnabled = !uiState.isSubmitting && !uiState.isEndingEarly,
                            onClick = onFinishSet,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("button_finish_set"),
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    enabled = !uiState.isSubmitting && !uiState.isEndingEarly,
                                    onClick = onRequestEndEarly,
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("button_counter_end_workout"),
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

                    if (uiState.showEndEarlyConfirmation) {
                        EndWorkoutEarlyModal(
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
}
