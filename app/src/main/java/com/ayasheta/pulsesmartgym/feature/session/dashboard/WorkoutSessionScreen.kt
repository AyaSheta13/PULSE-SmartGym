package com.ayasheta.pulsesmartgym.feature.session.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.component.sheet.PulseAnimatedBottomSheetHost
import com.ayasheta.pulsesmartgym.core.designsystem.component.sheet.PulseAnimatedBottomSheetOverlay
import com.ayasheta.pulsesmartgym.core.designsystem.component.workout.PulseRestTimerCard
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Back
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.feature.session.components.SessionExerciseCard
import com.ayasheta.pulsesmartgym.feature.session.components.SessionProgressCard
import com.ayasheta.pulsesmartgym.feature.session.logging.ManualSetEntrySheet
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette
import com.ayasheta.pulsesmartgym.feature.workout.editor.LocalRenderModalsInWindow

@Composable
fun WorkoutSessionScreen(
    viewModel: WorkoutSessionViewModel,
    onBackClick: () -> Unit,
    onSessionCompleted: (String) -> Unit,
    onSessionEndedEarly: (String) -> Unit,
    modifier: Modifier = Modifier,
    onStartSet: ((sessionId: String, exerciseId: String, setIndex: Int) -> Unit)? = null,
    onNavigateToNextSet: ((sessionId: String, exerciseId: String, setIndex: Int) -> Unit)? = null,
    onHowToPerformClick: (sessionId: String, sessionExerciseId: String) -> Unit = { _, _ -> },
    pendingActionError: String? = null,
    onConsumePendingActionError: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val handledTerminalSessionId = androidx.compose.runtime.remember(viewModel) {
        androidx.compose.runtime.mutableStateOf<String?>(null)
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshElapsedTime()
        onPauseOrDispose { }
    }

    val activeState = uiState as? WorkoutSessionUiState.Active
    BackHandler(
        enabled = activeState != null && !activeState.allSetsCompleted && !activeState.showEndEarlyConfirmation,
    ) {
        viewModel.requestEndEarly()
    }

    LaunchedEffect(pendingActionError) {
        if (pendingActionError != null) {
            viewModel.setActionError(pendingActionError)
            onConsumePendingActionError()
        }
    }

    LaunchedEffect(uiState) {
        val terminal = uiState as? WorkoutSessionUiState.Terminal ?: return@LaunchedEffect
        if (handledTerminalSessionId.value != terminal.sessionId) {
            handledTerminalSessionId.value = terminal.sessionId
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
                is WorkoutSessionEvent.Completed -> {
                    if (handledTerminalSessionId.value != event.sessionId) {
                        handledTerminalSessionId.value = event.sessionId
                        onSessionCompleted(event.sessionId)
                    }
                }
                is WorkoutSessionEvent.EndedEarly -> {
                    if (handledTerminalSessionId.value != event.sessionId) {
                        handledTerminalSessionId.value = event.sessionId
                        onSessionEndedEarly(event.sessionId)
                    }
                }
                is WorkoutSessionEvent.RedirectToSummary -> {
                    if (handledTerminalSessionId.value != event.sessionId) {
                        handledTerminalSessionId.value = event.sessionId
                        if (event.status == SessionStatus.COMPLETED) {
                            onSessionCompleted(event.sessionId)
                        } else {
                            onSessionEndedEarly(event.sessionId)
                        }
                    }
                }
                is WorkoutSessionEvent.NavigateToNextSet -> {
                    onNavigateToNextSet?.invoke(event.sessionId, event.exerciseId, event.setIndex)
                }
            }
        }
    }

    LaunchedEffect(
        (uiState as? WorkoutSessionUiState.Active)?.restTimer?.deadlineEpochMs,
        (uiState as? WorkoutSessionUiState.Active)?.restTimer?.remainingSeconds,
    ) {
        val active = uiState as? WorkoutSessionUiState.Active ?: return@LaunchedEffect
        val rest = active.restTimer ?: return@LaunchedEffect
        if (rest.remainingSeconds == 0) {
            viewModel.onRestExpired()
        }
    }

    WorkoutSessionScreenContent(
        uiState = uiState,
        onBackClick = {
            val active = uiState as? WorkoutSessionUiState.Active
            if (active != null && !active.allSetsCompleted) {
                viewModel.requestEndEarly()
            } else {
                onBackClick()
            }
        },
        onSetActionClick = { sessionExerciseId, setIndex ->
            val active = uiState as? WorkoutSessionUiState.Active
            val row = active?.exercises
                ?.firstOrNull { it.sessionExerciseId == sessionExerciseId }
                ?.sets
                ?.firstOrNull { it.setIndex == setIndex }
            if (active != null && row?.performed == null && onStartSet != null) {
                viewModel.startSetFromDashboard(sessionExerciseId, setIndex)
            } else {
                viewModel.openSetEntry(sessionExerciseId, setIndex)
            }
        },
        onAddRestTimeClick = { viewModel.extendRestTimer(30) },
        onSkipRestClick = viewModel::skipRestTimer,
        onHowToPerformClick = { sessionExerciseId ->
            val sessionId = (uiState as? WorkoutSessionUiState.Active)?.sessionId
            if (sessionId != null) {
                onHowToPerformClick(sessionId, sessionExerciseId)
            }
        },
        onDismissActionError = viewModel::dismissActionError,
        onDismissSetEntry = viewModel::dismissSetEntry,
        onConfirmSetEntry = viewModel::submitManualSet,
        onFinishWorkout = viewModel::finishWorkout,
        onRequestEndEarly = viewModel::requestEndEarly,
        onDismissEndEarly = viewModel::dismissEndEarly,
        onConfirmEndEarly = viewModel::confirmEndEarly,
        modifier = modifier,
    )
}

@Composable
fun WorkoutSessionScreenContent(
    uiState: WorkoutSessionUiState,
    onBackClick: () -> Unit,
    onSetActionClick: (sessionExerciseId: String, setIndex: Int) -> Unit,
    onDismissSetEntry: () -> Unit,
    onConfirmSetEntry: (fullReps: String, partialReps: String, load: String) -> Unit,
    onFinishWorkout: () -> Unit,
    onRequestEndEarly: () -> Unit,
    onDismissEndEarly: () -> Unit,
    onConfirmEndEarly: () -> Unit,
    modifier: Modifier = Modifier,
    onAddRestTimeClick: () -> Unit = {},
    onSkipRestClick: () -> Unit = {},
    onHowToPerformClick: (sessionExerciseId: String) -> Unit = {},
    onDismissActionError: () -> Unit = {},
) {
    val renderInWindow = LocalRenderModalsInWindow.current
    val connectionLabel = (uiState as? WorkoutSessionUiState.Active)?.connectionLabel ?: "Not Connected"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WorkoutPalette.Background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SessionTopBar(
                connectionLabel = connectionLabel,
                onBackClick = onBackClick,
            )

            when (uiState) {
                WorkoutSessionUiState.Loading, is WorkoutSessionUiState.Terminal -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        PulseLoadingIndicator(modifier = Modifier.testTag("session_loading"))
                    }
                }

                WorkoutSessionUiState.SignedOut -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "Sign in to resume your workout session.",
                            style = PulseTheme.typography.contentPrimary.copy(
                                fontSize = 15.sp,
                                color = WorkoutPalette.TextMuted,
                            ),
                            modifier = Modifier.testTag("session_signed_out"),
                        )
                    }
                }

                is WorkoutSessionUiState.Missing -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = uiState.message,
                            style = PulseTheme.typography.contentPrimary.copy(
                                fontSize = 15.sp,
                                color = WorkoutPalette.TextMuted,
                            ),
                            modifier = Modifier.testTag("session_missing"),
                        )
                    }
                }

                is WorkoutSessionUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = uiState.message,
                            style = PulseTheme.typography.contentPrimary.copy(
                                fontSize = 15.sp,
                                color = WorkoutPalette.ErrorText,
                            ),
                            modifier = Modifier.testTag("session_error"),
                        )
                    }
                }

                is WorkoutSessionUiState.Active -> {
                    val listState = rememberSaveable(
                        uiState.sessionId,
                        saver = LazyListState.Saver,
                    ) {
                        LazyListState()
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("session_exercise_list"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (uiState.actionError != null) {
                            item(key = "session_action_error") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFEECEB))
                                        .border(BorderStroke(1.dp, Color(0xFFF5B7B1)), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("session_action_error"),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        BasicText(
                                            text = uiState.actionError,
                                            style = PulseTheme.typography.contentPrimary.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = WorkoutPalette.ErrorText,
                                            ),
                                            modifier = Modifier.weight(1f),
                                        )
                                        BasicText(
                                            text = "Dismiss",
                                            style = PulseTheme.typography.label.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = WorkoutPalette.ErrorText,
                                            ),
                                            modifier = Modifier
                                                .clickable(onClick = onDismissActionError)
                                                .padding(4.dp),
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "session_progress") {
                            SessionProgressCard(
                                planName = uiState.planName,
                                elapsedTimeText = uiState.formattedElapsedTime,
                                recordedSetsCount = uiState.totalRecordedSets,
                                plannedSetsCount = uiState.totalPlannedSets,
                                progressFraction = uiState.progressFraction,
                            )
                        }

                        uiState.restTimer?.let { rest ->
                            item(key = "session_rest_timer") {
                                PulseRestTimerCard(
                                    remainingFormatted = rest.remainingFormatted,
                                    progress = rest.progress,
                                    subtitle = "Rest after ${rest.exerciseName}",
                                    onAddTimeClick = onAddRestTimeClick,
                                    onSkipClick = onSkipRestClick,
                                    modifier = Modifier.testTag("session_rest_timer_card"),
                                )
                            }
                        }

                        items(
                            items = uiState.exercises,
                            key = { it.sessionExerciseId },
                        ) { exercise ->
                            SessionExerciseCard(
                                exercise = exercise,
                                preferredWeightUnit = uiState.preferredWeightUnit,
                                actionsEnabled = !uiState.isSavingSet && !uiState.isCompletingSession && !uiState.isEndingEarly,
                                onSetActionClick = onSetActionClick,
                                onExerciseDetailsClick = onHowToPerformClick,
                            )
                        }
                    }

                    SessionBottomActionBar(
                        allSetsCompleted = uiState.allSetsCompleted,
                        isBusy = uiState.isCompletingSession || uiState.isEndingEarly,
                        isCompleting = uiState.isCompletingSession,
                        onFinishWorkout = onFinishWorkout,
                        onEndWorkout = onRequestEndEarly,
                        consumeNavBar = renderInWindow,
                    )
                }
            }
        }

        if (uiState is WorkoutSessionUiState.Active) {
            ManualSetEntrySheet(
                target = uiState.manualEntryTarget,
                preferredWeightUnit = uiState.preferredWeightUnit,
                isSaving = uiState.isSavingSet,
                errorMessage = uiState.manualEntryError,
                onDismiss = onDismissSetEntry,
                onConfirm = onConfirmSetEntry,
                inWindow = renderInWindow,
            )

            EndWorkoutEarlyModal(
                visible = uiState.showEndEarlyConfirmation,
                recordedSetsCount = uiState.totalRecordedSets,
                isEnding = uiState.isEndingEarly,
                onKeepWorkingOut = onDismissEndEarly,
                onEndWorkout = onConfirmEndEarly,
                inWindow = renderInWindow,
            )
        }
    }
}

@Composable
private fun SessionTopBar(
    connectionLabel: String,
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WorkoutPalette.Background)
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 16.dp)
            .testTag("session_top_bar"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onBackClick)
                .testTag("session_back_button"),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = Back,
                contentDescription = "Back",
                colorFilter = ColorFilter.tint(WorkoutPalette.TextPrimary),
                modifier = Modifier.size(20.dp),
            )
        }
        BasicText(
            text = "Workout Session",
            style = PulseTheme.typography.cardTitle.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = WorkoutPalette.TextPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF6F3F2))
                .border(BorderStroke(1.dp, Color(0xFFE5E0DC)), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .testTag("session_connection_status"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9C9691)),
            )
            BasicText(
                text = connectionLabel,
                style = PulseTheme.typography.contentPrimary.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF5A5754),
                ),
            )
        }
    }
}

@Composable
private fun SessionBottomActionBar(
    allSetsCompleted: Boolean,
    isBusy: Boolean,
    isCompleting: Boolean,
    onFinishWorkout: () -> Unit,
    onEndWorkout: () -> Unit,
    consumeNavBar: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .let { if (consumeNavBar) it else it.navigationBarsPadding() }
            .testTag("session_bottom_bar"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFEFECE9)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (allSetsCompleted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .alpha(if (isBusy) 0.65f else 1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WorkoutPalette.PrimaryCoral)
                        .clickable(enabled = !isBusy, onClick = onFinishWorkout)
                        .testTag("button_finish_workout"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCompleting) {
                        PulseLoadingIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        BasicText(
                            text = "Finish Workout",
                            style = PulseTheme.typography.ctaPrimary.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .alpha(if (isBusy) 0.65f else 1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(12.dp))
                        .clickable(enabled = !isBusy, onClick = onEndWorkout)
                        .testTag("button_end_workout"),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "End Workout",
                        style = PulseTheme.typography.ctaPrimary.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5A5754),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun EndWorkoutEarlyModal(
    recordedSetsCount: Int,
    isEnding: Boolean,
    onKeepWorkingOut: () -> Unit,
    onEndWorkout: () -> Unit,
    inWindow: Boolean = false,
    visible: Boolean = true,
) {
    PulseAnimatedBottomSheetHost(targetState = if (visible) recordedSetsCount else null) { setsCount, sheetVisible, onExitComplete ->
        val actionDispatched = androidx.compose.runtime.remember(setsCount, sheetVisible) {
            androidx.compose.runtime.mutableStateOf(false)
        }
        val modalBody: @Composable () -> Unit = {
            PulseAnimatedBottomSheetOverlay(
                visible = sheetVisible,
                onDismissRequest = onKeepWorkingOut,
                onExitComplete = onExitComplete,
                scrimAlphaMax = 0.55f,
                dismissEnabled = !isEnding,
                modifier = Modifier.testTag("modal_end_early_scrim"),
            ) { sheetModifier ->
            val navBarBottom = if (inWindow) {
                0.dp
            } else {
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            }
            val sheetBottomPadding = (navBarBottom + 8.dp).coerceAtLeast(32.dp)
            Column(
                modifier = sheetModifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, WorkoutPalette.InputBackground), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 25.dp, bottom = sheetBottomPadding)
                    .testTag("modal_end_workout_early"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BasicText(
                    text = "End workout early?",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191919),
                    ),
                )
                BasicText(
                    text = if (recordedSetsCount > 0) {
                        "Recorded sets will remain in your summary. Unfinished sets will not be counted."
                    } else {
                        "No sets have been recorded yet. Ending now will mark this workout as abandoned and it will not be added to workout history."
                    },
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF191919),
                    ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WorkoutPalette.PrimaryCoral)
                        .clickable(enabled = !isEnding && sheetVisible, onClick = onKeepWorkingOut)
                        .testTag("button_keep_working_out"),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Keep Working Out",
                        style = PulseTheme.typography.ctaPrimary.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .alpha(if (isEnding) 0.6f else 1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEEAE6))
                        .clickable(
                            enabled = !isEnding && sheetVisible && !actionDispatched.value,
                            onClick = {
                                if (!actionDispatched.value) {
                                    actionDispatched.value = true
                                    onEndWorkout()
                                }
                            },
                        )
                        .testTag("button_confirm_end_workout"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isEnding) {
                        PulseLoadingIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        BasicText(
                            text = "End Workout",
                            style = PulseTheme.typography.ctaPrimary.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191919),
                            ),
                        )
                    }
                }
            }
        }
    }

    if (inWindow) {
        modalBody()
    } else {
        Dialog(
            onDismissRequest = onKeepWorkingOut,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            modalBody()
        }
    }
    }
}
