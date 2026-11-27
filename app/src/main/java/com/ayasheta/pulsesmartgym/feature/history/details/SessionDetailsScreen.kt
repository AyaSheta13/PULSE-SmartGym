package com.ayasheta.pulsesmartgym.feature.history.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.Button as PulsePrimaryButton
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Back
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun SessionDetailsScreen(
    viewModel: SessionDetailsViewModel,
    onBackClick: () -> Unit,
    onRepeatWorkoutClick: (String) -> Unit,
    isRepeating: Boolean = false,
    repeatErrorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SessionDetailsScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRepeatWorkoutClick = onRepeatWorkoutClick,
        onRetryClick = viewModel::retry,
        isRepeating = isRepeating,
        repeatErrorMessage = repeatErrorMessage,
        modifier = modifier,
    )
}

@Composable
fun SessionDetailsScreenContent(
    uiState: SessionDetailsUiState,
    onBackClick: () -> Unit,
    onRepeatWorkoutClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    isRepeating: Boolean = false,
    repeatErrorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    val expandedExercises = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorkoutPalette.Background)
            .testTag("session_details_screen"),
    ) {
        SessionDetailsTopBar(onBackClick = onBackClick)

        when (uiState) {
            SessionDetailsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PulseLoadingIndicator(modifier = Modifier.testTag("session_details_loading"))
                }
            }

            SessionDetailsUiState.SignedOut -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    BasicText(
                        text = "Sign in to view historical session details.",
                        style = PulseTheme.typography.contentPrimary.copy(fontSize = 15.sp, color = WorkoutPalette.TextMuted),
                        modifier = Modifier.testTag("session_details_signed_out"),
                    )
                }
            }

            is SessionDetailsUiState.Unavailable -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("session_details_unavailable"),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BasicText(
                        text = uiState.message,
                        style = PulseTheme.typography.contentPrimary.copy(fontSize = 15.sp, color = WorkoutPalette.TextMuted),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PulsePrimaryButton(
                        caption = "Back",
                        onClick = onBackClick,
                        modifier = Modifier.fillMaxWidth(0.5f).height(46.dp),
                    )
                }
            }

            is SessionDetailsUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("session_details_error"),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BasicText(
                        text = uiState.message,
                        style = PulseTheme.typography.contentPrimary.copy(fontSize = 15.sp, color = WorkoutPalette.ErrorText),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PulsePrimaryButton(
                        caption = "Retry",
                        onClick = onRetryClick,
                        modifier = Modifier.fillMaxWidth(0.5f).height(46.dp),
                    )
                }
            }

            is SessionDetailsUiState.Loaded -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (repeatErrorMessage != null) {
                        item(key = "repeat_error") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFEECEB))
                                    .border(BorderStroke(1.dp, Color(0xFFF5B7B1)), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                BasicText(
                                    text = repeatErrorMessage,
                                    style = PulseTheme.typography.contentPrimary.copy(
                                        fontSize = 13.sp,
                                        color = WorkoutPalette.ErrorText,
                                    ),
                                )
                            }
                        }
                    }

                    // 1. Overview Card
                    item(key = "details_overview") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("session_details_overview_card"),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            BasicText(
                                text = uiState.planName,
                                style = PulseTheme.typography.cardTitle.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WorkoutPalette.TextPrimary,
                                ),
                            )
                            BasicText(
                                text = uiState.overviewSubtitle,
                                style = PulseTheme.typography.contentPrimary.copy(
                                    fontSize = 13.sp,
                                    color = WorkoutPalette.TextMuted,
                                ),
                            )
                        }
                    }

                    // 2. Recorded Session Totals Card
                    item(key = "details_totals") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                                .testTag("session_details_totals_card"),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BasicText(
                                    text = "SESSION DURATION",
                                    style = PulseTheme.typography.label.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = WorkoutPalette.TextMuted,
                                    ),
                                )
                                BasicText(
                                    text = uiState.durationText,
                                    style = PulseTheme.typography.numericMetric.copy(
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = WorkoutPalette.TextPrimary,
                                    ),
                                )
                            }
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TotalsChip(text = uiState.setsChipText)
                                TotalsChip(text = uiState.repsChipText)
                                TotalsChip(text = uiState.volumeChipText)
                            }
                        }
                    }

                    // 3. Exercise Breakdown Card
                    item(key = "details_breakdown") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                                .testTag("session_details_breakdown_card"),
                        ) {
                            uiState.performanceSummary?.let { perf ->
                                val measuredExercises = perf.exerciseSummaries.filter { it.hasAnyMeasurements }
                                if (measuredExercises.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFFAF8F6))
                                            .border(BorderStroke(1.dp, Color(0xFFECE8E3)), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                            .testTag("details_performance_summary"),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        BasicText(
                                            text = "Session Performance (${perf.measuredRepCount}/${perf.totalRecordedReps} measured reps)",
                                            style = PulseTheme.typography.label.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WorkoutPalette.TextPrimary,
                                            ),
                                        )
                                        measuredExercises.forEach { exPerf ->
                                            val exLine = listOfNotNull(
                                                exPerf.averageRepDurationMs?.let {
                                                    String.format(java.util.Locale.US, "%.2fs rep", it / 1000.0)
                                                },
                                                exPerf.averageAngularRomDegrees?.let {
                                                    String.format(java.util.Locale.US, "%.1f° ROM", it)
                                                },
                                                exPerf.averageLinearRomCm?.let {
                                                    String.format(java.util.Locale.US, "%.1f cm ROM", it)
                                                },
                                                exPerf.maxPeakAccelerationMps2?.let {
                                                    String.format(java.util.Locale.US, "%.2f m/s²", it)
                                                },
                                            ).joinToString(" · ")
                                            if (exLine.isNotEmpty()) {
                                                BasicText(
                                                    text = "${exPerf.exerciseName}: $exLine",
                                                    style = PulseTheme.typography.label.copy(
                                                        fontSize = 11.sp,
                                                        color = WorkoutPalette.TextMuted,
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BasicText(
                                    text = "Exercise Breakdown",
                                    style = PulseTheme.typography.cardTitle.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WorkoutPalette.TextPrimary,
                                    ),
                                )
                                BasicText(
                                    text = uiState.breakdownHeaderRight,
                                    style = PulseTheme.typography.label.copy(
                                        fontSize = 12.sp,
                                        color = WorkoutPalette.TextMuted,
                                    ),
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            uiState.exercises.forEachIndexed { index, exercise ->
                                val isExpanded = expandedExercises[exercise.sessionExerciseId] == true
                                SessionDetailExerciseRow(
                                    exercise = exercise,
                                    isExpanded = isExpanded,
                                    onToggleExpand = {
                                        if (exercise.recordedSets.isNotEmpty()) {
                                            expandedExercises[exercise.sessionExerciseId] = !isExpanded
                                        }
                                    },
                                )
                                if (index < uiState.exercises.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp)
                                            .height(1.dp)
                                            .background(Color(0xFFF3F0EC)),
                                    )
                                }
                            }
                        }
                    }

                    // 4. Repeat This Workout CTA
                    item(key = "details_repeat_cta") {
                        Spacer(modifier = Modifier.height(6.dp))
                        PulsePrimaryButton(
                            caption = if (isRepeating) "Starting Workout..." else "Repeat This Workout",
                            isLoading = isRepeating,
                            isEnabled = !isRepeating,
                            onClick = { onRepeatWorkoutClick(uiState.sessionId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("button_repeat_workout"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionDetailsTopBar(onBackClick: () -> Unit) {
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
                    .testTag("session_details_back_button"),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    imageVector = Back,
                    contentDescription = "Back",
                    colorFilter = ColorFilter.tint(WorkoutPalette.TextPrimary),
                    modifier = Modifier.size(18.dp),
                )
            }
            BasicText(
                text = "Session Details",
                style = PulseTheme.typography.sectionTitle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(PulseTheme.corners.fullRounded)
                    .background(Color(0xFFF6F4F2))
                    .border(BorderStroke(1.dp, Color(0xFFE4E0DC)), PulseTheme.corners.fullRounded)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                BasicText(
                    text = "Saved locally",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WorkoutPalette.TextPrimary,
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
private fun TotalsChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F3F1))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = PulseTheme.typography.label.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = WorkoutPalette.TextPrimary,
            ),
            maxLines = 2,
        )
    }
}

@Composable
private fun SessionDetailExerciseRow(
    exercise: SessionDetailExerciseUiModel,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = exercise.recordedSets.isNotEmpty(), onClick = onToggleExpand)
            .testTag("details_exercise_row_${exercise.sessionExerciseId}"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = exercise.exerciseName,
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        text = exercise.summaryLine,
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 13.sp,
                            color = WorkoutPalette.TextMuted,
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE7F6EF))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        BasicText(
                            text = exercise.loadBadgeText,
                            style = PulseTheme.typography.label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B8A5A),
                            ),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F3F1))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                BasicText(
                    text = exercise.modeBadgeText,
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WorkoutPalette.TextMuted,
                    ),
                )
            }
        }

        if (isExpanded && exercise.recordedSets.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFAF8F7))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("details_exercise_sets_${exercise.sessionExerciseId}"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                exercise.recordedSets.forEach { set ->
                    val durPart = if (set.durationText != null) " • ${set.durationText}" else ""
                    BasicText(
                        text = "Set ${set.setIndex}: ${set.repsText} @ ${set.weightText}$durPart • ${set.modeLabel}",
                        style = PulseTheme.typography.label.copy(
                            fontSize = 12.sp,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                    if (set.repMeasurements.isNotEmpty()) {
                        val measured = set.repMeasurements.filter {
                            it.totalDurationMs != null ||
                                it.angularRomDegrees != null ||
                                it.linearRomCm != null ||
                                it.peakAccelerationMetersPerSecondSquared != null
                        }
                        if (measured.isNotEmpty()) {
                            Column(
                                modifier = Modifier.padding(start = 10.dp, top = 2.dp, bottom = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                measured.forEach { rep ->
                                    val rom = listOfNotNull(
                                        rep.angularRomDegrees?.let { String.format(java.util.Locale.US, "%.0f°", it) },
                                        rep.linearRomCm?.let { String.format(java.util.Locale.US, "%.1f cm", it) },
                                    ).joinToString(" / ")
                                    val accel = rep.peakAccelerationMetersPerSecondSquared?.let {
                                        String.format(java.util.Locale.US, "%.2f m/s²", it)
                                    }
                                    val dur = rep.totalDurationMs?.let {
                                        String.format(java.util.Locale.US, "%.2fs", it / 1000.0)
                                    }
                                    val parts = listOfNotNull(
                                        rom.takeIf { it.isNotEmpty() },
                                        accel,
                                        dur,
                                    ).joinToString(" · ")
                                    BasicText(
                                        text = "Rep #${rep.repIndex}: $parts",
                                        style = PulseTheme.typography.label.copy(
                                            fontSize = 11.sp,
                                            color = WorkoutPalette.TextMuted,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
