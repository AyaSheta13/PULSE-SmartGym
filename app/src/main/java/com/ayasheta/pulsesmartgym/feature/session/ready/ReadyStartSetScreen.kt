package com.ayasheta.pulsesmartgym.feature.session.ready

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.ColorFilter
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
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Back
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.CheckMark
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.feature.session.dashboard.EndWorkoutEarlyModal
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionUiMapper
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette
import com.ayasheta.pulsesmartgym.feature.workout.editor.LocalRenderModalsInWindow
import kotlin.math.roundToInt

@Composable
fun ReadyStartSetScreen(
    viewModel: ReadyStartSetViewModel,
    onBackClick: () -> Unit,
    onStartSetClick: (sessionId: String, exerciseId: String, setIndex: Int) -> Unit,
    onSessionCompleted: (String) -> Unit,
    onSessionEndedEarly: (String) -> Unit,
    modifier: Modifier = Modifier,
    onStartDeviceAssistedClick: ((sessionId: String, exerciseId: String, setIndex: Int) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val handledTerminalId = remember(viewModel) { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        val terminal = uiState as? ReadyStartSetUiState.Terminal ?: return@LaunchedEffect
        if (handledTerminalId.value != terminal.sessionId) {
            handledTerminalId.value = terminal.sessionId
            if (terminal.status == SessionStatus.COMPLETED) {
                onSessionCompleted(terminal.sessionId)
            } else {
                onSessionEndedEarly(terminal.sessionId)
            }
        }
    }

    ReadyStartSetScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onStartSetClick = { state ->
            if (state.canStartDeviceAssisted && onStartDeviceAssistedClick != null) {
                onStartDeviceAssistedClick(state.sessionId, state.exerciseId, state.setIndex)
            } else {
                onStartSetClick(state.sessionId, state.exerciseId, state.setIndex)
            }
        },
        onRequestEndEarly = viewModel::requestEndEarly,
        onDismissEndEarly = viewModel::dismissEndEarly,
        onConfirmEndEarly = viewModel::confirmEndEarly,
        modifier = modifier,
    )
}

@Composable
fun ReadyStartSetScreenContent(
    uiState: ReadyStartSetUiState,
    onBackClick: () -> Unit,
    onStartSetClick: (ReadyStartSetUiState.Ready) -> Unit,
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
            .testTag("ready_start_set_screen"),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                ReadyStartSetUiState.Loading, is ReadyStartSetUiState.Terminal -> {
                    SetExecutionTopBar(
                        title = "Prepare Set",
                        subtitle = "Loading...",
                        modeLabel = "Manual",
                        onBackClick = onBackClick,
                    )
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PulseLoadingIndicator(modifier = Modifier.testTag("ready_loading"))
                    }
                }

                ReadyStartSetUiState.SignedOut -> {
                    SetExecutionTopBar(
                        title = "Prepare Set",
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

                is ReadyStartSetUiState.Missing -> {
                    SetExecutionTopBar(
                        title = "Prepare Set",
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

                is ReadyStartSetUiState.AlreadyCompleted -> {
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

                is ReadyStartSetUiState.Ready -> {
                    SetExecutionTopBar(
                        title = uiState.exerciseName,
                        subtitle = "Set ${uiState.setIndex} of ${uiState.totalSetsForExercise} • Ready",
                        modeLabel = uiState.trackingModeLabel,
                        onBackClick = onBackClick,
                    )

                    val unitLabel = if (uiState.preferredWeightUnit == WeightUnit.KG) "kg" else "lbs"
                    val targetLoadText = SessionUiMapper.formatWeightLoad(uiState.targetLoad, uiState.preferredWeightUnit)
                    val targetPillText = if (targetLoadText != null) {
                        "TARGET: ${uiState.targetReps} REPS @ ${targetLoadText.uppercase()}"
                    } else {
                        "TARGET: ${uiState.targetReps} REPS"
                    }
                    val weightNumberText = formatLoadNumber(uiState.targetLoad, uiState.preferredWeightUnit)
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
                                    modifier = Modifier.testTag("ready_rest_info"),
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(PulseTheme.corners.fullRounded)
                                .background(PulseTheme.color.surfaceMuted)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("ready_target_pill"),
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

                        if (uiState.actionError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFEECEB))
                                    .border(BorderStroke(1.dp, Color(0xFFF5B7B1)), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                BasicText(
                                    text = uiState.actionError,
                                    style = PulseTheme.typography.contentPrimary.copy(
                                        fontSize = 13.sp,
                                        color = WorkoutPalette.ErrorText,
                                    ),
                                )
                            }
                        }

                        PulseRepCounterGauge(
                            currentReps = 0,
                            targetReps = uiState.targetReps,
                            onDecrementRep = {},
                            onIncrementRep = {},
                        )

                        PulseWeightSelectorCard(
                            weightText = weightNumberText,
                            unitLabel = unitLabel,
                            setSubtitle = "Actual load used for Set ${uiState.setIndex}",
                            targetText = targetCardLabel,
                            onDecrementClick = {},
                            onIncrementClick = {},
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        PulsePrimaryButton(
                            caption = "Start Set",
                            icon = rememberVectorPainter(CheckMark),
                            onClick = { onStartSetClick(uiState) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("button_ready_start_set"),
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !uiState.isEndingEarly, onClick = onRequestEndEarly)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("button_ready_end_workout"),
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
internal fun SetExecutionTopBar(
    title: String,
    subtitle: String,
    modeLabel: String,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick)
                    .testTag("button_set_execution_back"),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    imageVector = Back,
                    contentDescription = "Back",
                    colorFilter = ColorFilter.tint(WorkoutPalette.TextPrimary),
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = title,
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = subtitle,
                    style = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = WorkoutPalette.TextMuted,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .clip(PulseTheme.corners.fullRounded)
                    .background(Color(0xFFF6F4F2))
                    .border(BorderStroke(1.dp, Color(0xFFE4E0DC)), PulseTheme.corners.fullRounded)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                BasicText(
                    text = modeLabel,
                    style = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WorkoutPalette.TextMuted,
                    ),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFECE8E3)),
        )
    }
}

@Composable
internal fun SetFallbackMessage(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = message,
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 15.sp,
                color = WorkoutPalette.TextMuted,
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        PulsePrimaryButton(
            caption = actionLabel,
            onClick = onAction,
            modifier = Modifier.fillMaxWidth(0.7f).height(48.dp),
        )
    }
}

@Composable
internal fun StopCircleIcon() {
    Box(
        modifier = Modifier
            .size(16.dp)
            .border(1.5.dp, WorkoutPalette.TextPrimary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(WorkoutPalette.TextPrimary, RoundedCornerShape(1.5.dp)),
        )
    }
}

internal fun formatLoadNumber(load: WeightLoad?, unit: WeightUnit): String {
    if (load == null) return "0"
    val value = when (unit) {
        WeightUnit.KG -> load.toKilograms()
        WeightUnit.LBS -> load.toPounds()
    }
    val rounded = (value * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}
