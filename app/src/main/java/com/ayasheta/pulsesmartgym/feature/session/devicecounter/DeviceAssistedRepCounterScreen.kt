package com.ayasheta.pulsesmartgym.feature.session.devicecounter

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Plus
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.Button as PulsePrimaryButton
import com.ayasheta.pulsesmartgym.core.designsystem.component.chips.PulseBluetoothChipStatus
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTopAppBarStyle
import com.ayasheta.pulsesmartgym.core.designsystem.component.workout.PulseRepCounterGauge
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.session.SessionSetTarget
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.feature.session.dashboard.EndWorkoutEarlyModal
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionUiMapper
import com.ayasheta.pulsesmartgym.feature.session.recovery.ConnectionLostRecoverySheet
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette
import kotlinx.coroutines.delay

@Composable
fun DeviceAssistedRepCounterScreen(
    viewModel: DeviceAssistedRepCounterViewModel,
    onBackClick: () -> Unit,
    onSetFinished: (sessionId: String, hasRestStarted: Boolean, nextTarget: SessionSetTarget?, restErrorMessage: String?) -> Unit,
    onFallbackToManual: (sessionId: String, exerciseId: String, setIndex: Int) -> Unit,
    onSessionEnded: (sessionId: String, status: SessionStatus) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DeviceAssistedCounterEvent.SetFinished ->
                    onSetFinished(event.sessionId, event.hasRestStarted, event.nextTarget, event.restErrorMessage)
                is DeviceAssistedCounterEvent.FallbackToManual ->
                    onFallbackToManual(event.sessionId, event.exerciseId, event.setIndex)
                is DeviceAssistedCounterEvent.SessionEnded ->
                    onSessionEnded(event.sessionId, event.status)
            }
        }
    }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is DeviceAssistedRepCounterUiState.Terminal -> onSessionEnded(s.sessionId, s.status)
            is DeviceAssistedRepCounterUiState.Completed ->
                onSetFinished(s.sessionId, s.hasRestStarted, null, s.restErrorMessage)
            is DeviceAssistedRepCounterUiState.AlreadyCompleted -> onBackClick()
            is DeviceAssistedRepCounterUiState.Active -> {
                val fallback = s.pendingManualFallback
                if (fallback != null) {
                    viewModel.consumePendingManualFallback()
                    onFallbackToManual(fallback.sessionId, fallback.exerciseId, fallback.setIndex)
                }
            }
            else -> Unit
        }
    }

    DeviceAssistedRepCounterContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onIncrementFullRep = viewModel::incrementManualFullRep,
        onIncrementPartialRep = viewModel::incrementManualPartialRep,
        onDecrementRep = viewModel::decrementManualRep,
        onIncrementWeight = viewModel::incrementWeight,
        onDecrementWeight = viewModel::decrementWeight,
        onSimulateAutoRep = { viewModel.simulateAutoRep(RepClassification.FULL) },
        onSimulateConnectionLoss = viewModel::simulateConnectionLoss,
        onReconnectClick = viewModel::reconnectDevice,
        onContinueManuallyClick = viewModel::continueManually,
        onFinishSetClick = viewModel::finishSet,
        onEndWorkoutClick = viewModel::requestEndWorkoutEarly,
        onDismissEndEarly = viewModel::dismissEndWorkoutEarly,
        onConfirmEndEarly = viewModel::confirmEndWorkoutEarly,
    )
}

@Composable
fun DeviceAssistedRepCounterContent(
    uiState: DeviceAssistedRepCounterUiState,
    onBackClick: () -> Unit,
    onIncrementFullRep: () -> Unit,
    onIncrementPartialRep: () -> Unit,
    onDecrementRep: () -> Unit,
    onIncrementWeight: () -> Unit,
    onDecrementWeight: () -> Unit,
    onSimulateAutoRep: () -> Unit,
    onSimulateConnectionLoss: () -> Unit,
    onReconnectClick: () -> Unit,
    onContinueManuallyClick: () -> Unit,
    onFinishSetClick: () -> Unit,
    onEndWorkoutClick: () -> Unit,
    onDismissEndEarly: () -> Unit,
    onConfirmEndEarly: () -> Unit,
    enableTicker: Boolean = true,
) {
    when (uiState) {
        DeviceAssistedRepCounterUiState.Loading,
        is DeviceAssistedRepCounterUiState.Terminal,
        is DeviceAssistedRepCounterUiState.Completed,
        -> {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFFFAF8F7)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = WorkoutPalette.PrimaryCoral)
            }
        }

        DeviceAssistedRepCounterUiState.SignedOut,
        is DeviceAssistedRepCounterUiState.Missing,
        is DeviceAssistedRepCounterUiState.AlreadyCompleted,
        -> {
            val msg = when (uiState) {
                DeviceAssistedRepCounterUiState.SignedOut -> "Please sign in to continue your workout."
                is DeviceAssistedRepCounterUiState.Missing -> uiState.message
                is DeviceAssistedRepCounterUiState.AlreadyCompleted ->
                    "${uiState.exerciseName} Set ${uiState.setIndex} is already completed."
                else -> ""
            }
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFFFAF8F7)).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BasicText(
                    text = msg,
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 15.sp,
                        color = WorkoutPalette.TextPrimary,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(modifier = Modifier.height(16.dp))
                PulsePrimaryButton(
                    caption = "Back",
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth(0.5f).height(46.dp),
                )
            }
        }

        is DeviceAssistedRepCounterUiState.Active -> {
            BackHandler(onBack = onBackClick)

            var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(enableTicker, uiState.startedAtEpochMs) {
                if (enableTicker) {
                    while (true) {
                        nowMs = System.currentTimeMillis()
                        delay(1_000L)
                    }
                }
            }
            val elapsedText = SessionUiMapper.formatElapsedMmSs(
                SessionUiMapper.computeElapsedSeconds(uiState.startedAtEpochMs, nowMs),
            )
            val subtitlePrefix = if (uiState.bluetoothStatus == PulseBluetoothChipStatus.CONNECTION_LOST) {
                "Connection Lost"
            } else if (uiState.isSimulatedDevice) {
                "Set ${uiState.setIndex} of ${uiState.totalSetsForExercise} • Sim"
            } else {
                "Set ${uiState.setIndex} of ${uiState.totalSetsForExercise}"
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFAF8F7)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                ) {
                    PulseTopAppBar(
                        style = PulseTopAppBarStyle.LiveWorkoutHeader(
                            exerciseTitle = uiState.exerciseName,
                            sessionSubtitle = "$subtitlePrefix • $elapsedText",
                            bluetoothStatus = uiState.bluetoothStatus,
                            onBackClick = onBackClick,
                        ),
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Plan name + target badge
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BasicText(
                                text = uiState.planName,
                                style = PulseTheme.typography.cardTitle.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WorkoutPalette.TextPrimary,
                                ),
                            )
                            val targetWeight = SessionUiMapper.formatWeightLoad(uiState.targetLoad, uiState.preferredWeightUnit)
                            val targetLine = if (targetWeight != null) {
                                "TARGET: ${uiState.targetReps} REPS @ ${targetWeight.uppercase()}"
                            } else {
                                "TARGET: ${uiState.targetReps} REPS"
                            }
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(PulseTheme.corners.fullRounded)
                                        .background(Color(0xFFF3F0EE))
                                        .padding(horizontal = 14.dp, vertical = 5.dp),
                                ) {
                                    BasicText(
                                        text = targetLine,
                                        style = PulseTheme.typography.label.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF5A5754),
                                        ),
                                    )
                                }
                            }
                        }

                        // Circular Gauge Card (57:18098)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(22.dp))
                                .padding(vertical = 20.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            PulseRepCounterGauge(
                                currentReps = uiState.fullReps,
                                targetReps = uiState.targetReps,
                                onDecrementRep = onDecrementRep,
                                onIncrementRep = onIncrementFullRep,
                                modifier = Modifier.testTag("device_rep_counter_gauge"),
                            )

                            if (uiState.partialReps > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(PulseTheme.corners.fullRounded)
                                        .background(Color(0xFFFFF4DC))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                        .testTag("device_partial_reps_badge"),
                                ) {
                                    BasicText(
                                        text = "+${uiState.partialReps} partial reps",
                                        style = PulseTheme.typography.label.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFB45309),
                                        ),
                                    )
                                }
                            }
                        }

                        // Current Set Load Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(18.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                BasicText(
                                    text = "CURRENT SET LOAD",
                                    style = PulseTheme.typography.label.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp,
                                        color = Color(0xFF7A7570),
                                    ),
                                )
                                val loadText = SessionUiMapper.formatWeightLoad(uiState.actualLoad, uiState.preferredWeightUnit) ?: "Bodyweight"
                                BasicText(
                                    text = loadText,
                                    style = PulseTheme.typography.numericMetric.copy(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WorkoutPalette.TextPrimary,
                                    ),
                                )
                                BasicText(
                                    text = "Actual load used for Set ${uiState.setIndex}",
                                    style = PulseTheme.typography.contentPrimary.copy(
                                        fontSize = 12.sp,
                                        color = Color(0xFF7A7570),
                                    ),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF2F0EE))
                                        .clickable(onClick = onDecrementWeight),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    BasicText(
                                        text = "—",
                                        style = PulseTheme.typography.ctaPrimary.copy(fontSize = 16.sp, color = WorkoutPalette.TextPrimary),
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF2F0EE))
                                        .clickable(onClick = onIncrementWeight),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        imageVector = Plus,
                                        contentDescription = "Increase weight",
                                        colorFilter = ColorFilter.tint(WorkoutPalette.TextPrimary),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }

                        // Explicit Simulator Demo Controls (never shown for non-simulated real devices)
                        if (uiState.isSimulatedDevice) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFEFEA))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BasicText(
                                    text = "Demo Simulator",
                                    style = PulseTheme.typography.label.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WorkoutPalette.PrimaryCoral,
                                    ),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BasicText(
                                        text = "+ Sim Rep",
                                        style = PulseTheme.typography.label.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WorkoutPalette.PrimaryCoral,
                                        ),
                                        modifier = Modifier
                                            .clickable(onClick = onSimulateAutoRep)
                                            .testTag("button_sim_auto_rep"),
                                    )
                                    BasicText(
                                        text = "+ Partial",
                                        style = PulseTheme.typography.label.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFB45309),
                                        ),
                                        modifier = Modifier
                                            .clickable(onClick = onIncrementPartialRep)
                                            .testTag("button_sim_partial_rep"),
                                    )
                                    BasicText(
                                        text = "Drop Link",
                                        style = PulseTheme.typography.label.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF7A7570),
                                        ),
                                        modifier = Modifier
                                            .clickable(onClick = onSimulateConnectionLoss)
                                            .testTag("button_sim_drop_link"),
                                    )
                                }
                            }
                        }

                        if (uiState.errorMessage != null) {
                            BasicText(
                                text = uiState.errorMessage,
                                style = PulseTheme.typography.label.copy(
                                    fontSize = 12.sp,
                                    color = WorkoutPalette.ErrorText,
                                ),
                            )
                        }

                        PulsePrimaryButton(
                            caption = if (uiState.isSubmitting) "Saving Set..." else "Finish Set",
                            onClick = { if (!uiState.isSubmitting && !uiState.isEndingEarly) onFinishSetClick() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("button_device_finish_set"),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !uiState.isSubmitting && !uiState.isEndingEarly, onClick = onEndWorkoutClick)
                                .padding(vertical = 8.dp)
                                .testTag("button_device_end_workout"),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = "End Workout",
                                style = PulseTheme.typography.label.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF5A5754),
                                ),
                            )
                        }
                    }
                }

                // Connection Lost Recovery Bottom Sheet (420:76)
                ConnectionLostRecoverySheet(
                    visible = uiState.showRecoverySheet,
                    setIndex = uiState.setIndex,
                    fullReps = uiState.fullReps,
                    partialReps = uiState.partialReps,
                    recordedSetsCount = uiState.recordedSetsCount,
                    isReconnecting = uiState.isReconnecting,
                    reconnectErrorMessage = uiState.reconnectErrorMessage,
                    onReconnectClick = onReconnectClick,
                    onContinueManuallyClick = onContinueManuallyClick,
                )

                EndWorkoutEarlyModal(
                    visible = uiState.showEndEarlyConfirmation,
                    recordedSetsCount = uiState.recordedSetsCount,
                    isEnding = uiState.isEndingEarly,
                    onKeepWorkingOut = onDismissEndEarly,
                    onEndWorkout = onConfirmEndEarly,
                )
            }
        }
    }
}
