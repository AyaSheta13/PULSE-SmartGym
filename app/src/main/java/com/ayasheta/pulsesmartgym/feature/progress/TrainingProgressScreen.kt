package com.ayasheta.pulsesmartgym.feature.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayasheta.pulsesmartgym.core.designsystem.component.button.Button as PulsePrimaryButton
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette

@Composable
fun TrainingProgressScreen(
    viewModel: TrainingProgressViewModel,
    onStartWorkoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    TrainingProgressScreenContent(
        uiState = uiState,
        onStartWorkoutClick = onStartWorkoutClick,
        onSelectWeekOffset = viewModel::selectWeekOffset,
        onRetryClick = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun TrainingProgressScreenContent(
    uiState: TrainingProgressUiState,
    onStartWorkoutClick: () -> Unit,
    onSelectWeekOffset: (Int) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showWeekPicker by remember { mutableStateOf(false) }

    val badgeLabel = when (uiState) {
        TrainingProgressUiState.Empty -> "No Data Yet"
        is TrainingProgressUiState.Populated -> uiState.weekBadgeLabel
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorkoutPalette.Background)
            .testTag("training_progress_screen"),
    ) {
        ProgressTopBar(
            badgeLabel = badgeLabel,
            isInteractive = uiState is TrainingProgressUiState.Populated,
            onBadgeClick = {
                if (uiState is TrainingProgressUiState.Populated) {
                    showWeekPicker = true
                }
            },
        )

        when (uiState) {
            TrainingProgressUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PulseLoadingIndicator(modifier = Modifier.testTag("progress_loading"))
                }
            }

            TrainingProgressUiState.SignedOut -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    BasicText(
                        text = "Sign in to view your training progress.",
                        style = PulseTheme.typography.contentPrimary.copy(fontSize = 15.sp, color = WorkoutPalette.TextMuted),
                        modifier = Modifier.testTag("progress_signed_out"),
                    )
                }
            }

            is TrainingProgressUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("progress_error"),
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
                        modifier = Modifier.fillMaxWidth(0.6f).height(46.dp),
                    )
                }
            }

            TrainingProgressUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(20.dp))
                            .padding(horizontal = 24.dp, vertical = 36.dp)
                            .testTag("progress_empty_card"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(PulseTheme.corners.fullRounded)
                                .background(PulseTheme.color.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            TrendLineIcon(color = PulseTheme.color.primary)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        BasicText(
                            text = "Your progress starts here",
                            style = PulseTheme.typography.sectionTitle.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        BasicText(
                            text = "Complete and save your first workout to see your training trends, volume, and personal records.",
                            style = PulseTheme.typography.contentSecondary.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = WorkoutPalette.TextMuted,
                                textAlign = TextAlign.Center,
                            ),
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        PulsePrimaryButton(
                            caption = "Start a Workout",
                            onClick = onStartWorkoutClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("button_progress_start_workout"),
                        )
                    }
                }
            }

            is TrainingProgressUiState.Populated -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("progress_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 2x2 Metric Cards
                    item(key = "progress_row_1") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ProgressMetricCard(
                                model = uiState.workoutsCard,
                                modifier = Modifier.weight(1f).testTag("metric_card_workouts"),
                            )
                            ProgressMetricCard(
                                model = uiState.volumeCard,
                                modifier = Modifier.weight(1f).testTag("metric_card_volume"),
                            )
                        }
                    }
                    item(key = "progress_row_2") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ProgressMetricCard(
                                model = uiState.setsCard,
                                modifier = Modifier.weight(1f).testTag("metric_card_sets"),
                            )
                            ProgressMetricCard(
                                model = uiState.avgDurationCard,
                                modifier = Modifier.weight(1f).testTag("metric_card_duration"),
                            )
                        }
                    }

                    // Weekly Volume Breakdown Chart
                    item(key = "progress_chart") {
                        WeeklyVolumeChartCard(
                            headerVolumeText = uiState.chartHeaderVolumeText,
                            bars = uiState.bars,
                        )
                    }

                    // Key Lift Personal Records
                    item(key = "progress_prs") {
                        PersonalRecordsCard(records = uiState.personalRecords)
                    }

                    item(key = "progress_performance_trends") {
                        PerformanceTrendsCard(
                            trends = uiState.performanceTrends,
                            weekLabel = uiState.weekBadgeLabel,
                        )
                    }
                }

                if (showWeekPicker) {
                    Dialog(onDismissRequest = { showWeekPicker = false }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .padding(20.dp)
                                .testTag("dialog_week_picker"),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            BasicText(
                                text = "Select Week",
                                style = PulseTheme.typography.sectionTitle.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WorkoutPalette.TextPrimary,
                                ),
                            )
                            uiState.weekOptions.forEach { option ->
                                val selected = option.weekOffset == uiState.selectedWeekOffset
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) PulseTheme.color.primaryContainer else Color(0xFFF6F4F2))
                                        .clickable {
                                            showWeekPicker = false
                                            onSelectWeekOffset(option.weekOffset)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                ) {
                                    BasicText(
                                        text = option.label,
                                        style = PulseTheme.typography.contentPrimary.copy(
                                            fontSize = 14.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) PulseTheme.color.primary else WorkoutPalette.TextPrimary,
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

@Composable
private fun ProgressTopBar(
    badgeLabel: String,
    isInteractive: Boolean,
    onBadgeClick: () -> Unit,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Training Progress",
                style = PulseTheme.typography.sectionTitle.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
            )
            if (badgeLabel.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(PulseTheme.corners.fullRounded)
                        .background(Color(0xFFF6F4F2))
                        .border(BorderStroke(1.dp, Color(0xFFE4E0DC)), PulseTheme.corners.fullRounded)
                        .clickable(enabled = isInteractive, onClick = onBadgeClick)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("progress_header_badge"),
                ) {
                    BasicText(
                        text = badgeLabel,
                        style = PulseTheme.typography.label.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressMetricCard(
    model: ProgressMetricCardUiModel,
    modifier: Modifier = Modifier,
) {
    val deltaColor = when (model.deltaTone) {
        DeltaTone.POSITIVE -> Color(0xFF1B8A5A)
        DeltaTone.NEGATIVE -> WorkoutPalette.ErrorText
        DeltaTone.NEUTRAL -> WorkoutPalette.TextMuted
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(
            text = model.title,
            style = PulseTheme.typography.label.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
                color = WorkoutPalette.TextMuted,
            ),
            maxLines = 2,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText(
                text = model.primaryValue,
                style = PulseTheme.typography.numericMetric.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
                modifier = Modifier.alignByBaseline(),
            )
            if (model.primaryUnit.isNotEmpty()) {
                BasicText(
                    text = model.primaryUnit,
                    style = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        color = WorkoutPalette.TextMuted,
                    ),
                    maxLines = 2,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        BasicText(
            text = model.deltaText,
            style = PulseTheme.typography.label.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = deltaColor,
            ),
            maxLines = 2,
        )
    }
}

@Composable
private fun WeeklyVolumeChartCard(
    headerVolumeText: String,
    bars: List<WeeklyVolumeBarUiModel>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("progress_weekly_chart"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Weekly Volume Breakdown",
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
            )
            BasicText(
                text = headerVolumeText,
                style = PulseTheme.typography.label.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PulseTheme.color.primary,
                ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { bar ->
                val barColor = when {
                    bar.isHighlighted -> PulseTheme.color.primary
                    bar.volumeKg > 0.0 -> Color(0xFFCFC9C3)
                    else -> Color(0xFFECE8E3)
                }
                val labelColor = if (bar.isHighlighted) PulseTheme.color.primary else WorkoutPalette.TextMuted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { contentDescription = bar.accessibilityText }
                        .testTag("chart_bar_${bar.dayIndex}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.68f)
                                .fillMaxHeight(bar.heightFraction)
                                .clip(RoundedCornerShape(6.dp))
                                .background(barColor),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicText(
                        text = bar.dayLabel,
                        style = PulseTheme.typography.label.copy(
                            fontSize = 12.sp,
                            fontWeight = if (bar.isHighlighted) FontWeight.Bold else FontWeight.Medium,
                            color = labelColor,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceTrendsCard(
    trends: List<com.ayasheta.pulsesmartgym.core.model.history.ExercisePerformanceTrendSummary>,
    weekLabel: String,
) {
    var selectedExerciseKey by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf<String?>(null)
    }
    val active = trends.firstOrNull { it.exerciseKey == selectedExerciseKey } ?: trends.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, WorkoutPalette.CardBorder), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("progress_performance_trends_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Performance Trends",
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
            )
            BasicText(
                text = weekLabel,
                style = PulseTheme.typography.label.copy(
                    fontSize = 11.sp,
                    color = WorkoutPalette.TextMuted,
                ),
            )
        }

        if (active == null) {
            BasicText(
                text = "No measured sensor performance data recorded in this period.",
                style = PulseTheme.typography.contentPrimary.copy(
                    fontSize = 13.sp,
                    color = WorkoutPalette.TextMuted,
                ),
            )
        } else {
            if (active.isSimulatedIncluded) {
                BasicText(
                    text = "Simulated Demo Trends",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WorkoutPalette.PrimaryCoral,
                    ),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                trends.forEach { trend ->
                    val isSelected = trend.exerciseKey == active.exerciseKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (isSelected) WorkoutPalette.PrimaryCoral else Color(0xFFF6F4F2))
                            .clickable { selectedExerciseKey = trend.exerciseKey }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        BasicText(
                            text = trend.exerciseName,
                            style = PulseTheme.typography.label.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else WorkoutPalette.TextPrimary,
                            ),
                        )
                    }
                }
            }
            val romText = if (!active.isComparableTrendAvailable) {
                "Unverified comparison (${active.configurationCompatibility.name.lowercase()})"
            } else {
                listOfNotNull(
                    active.averageAngularRomDegrees?.let { String.format(java.util.Locale.US, "%.1f° avg (%d)", it, active.angularRomSampleCount) },
                    active.averageLinearRomCm?.let { String.format(java.util.Locale.US, "%.1f cm avg (%d)", it, active.linearRomSampleCount) },
                ).joinToString(" · ").ifEmpty { "Unavailable" }
            }
            val durText = if (!active.isComparableTrendAvailable) {
                "Unavailable"
            } else {
                active.averageRepDurationMs?.let {
                    String.format(java.util.Locale.US, "%.2f s avg (%d)", it / 1000.0, active.durationSampleCount)
                } ?: "Unavailable"
            }
            val accelText = if (!active.isComparableTrendAvailable) {
                "Unavailable"
            } else {
                active.maxPeakAccelerationMps2?.let {
                    String.format(java.util.Locale.US, "%.2f m/s² peak", it)
                } ?: "Unavailable"
            }
            val sensorRatio = if (active.totalSensorFullReps + active.totalSensorPartialReps > 0) {
                "${active.totalSensorFullReps} full / ${active.totalSensorPartialReps} partial sensor"
            } else {
                "No sensor classification"
            }
            val manualSuffix = if (active.totalManualReps > 0) " • ${active.totalManualReps} manual" else ""

            BasicText(
                text = "ROM: $romText",
                style = PulseTheme.typography.contentPrimary.copy(fontSize = 13.sp, color = WorkoutPalette.TextPrimary),
            )
            BasicText(
                text = "Rep Duration: $durText  •  Peak Accel: $accelText",
                style = PulseTheme.typography.contentPrimary.copy(fontSize = 13.sp, color = WorkoutPalette.TextPrimary),
            )
            BasicText(
                text = "Classification: $sensorRatio$manualSuffix (${active.totalMeasuredReps} measured)",
                style = PulseTheme.typography.label.copy(fontSize = 12.sp, color = WorkoutPalette.TextMuted),
            )

            // Chronological session trend points
            val dateFmt = remember {
                java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.US)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFAF8F6))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                active.points.forEach { pt ->
                    val dateStr = java.time.Instant.ofEpochMilli(pt.timestampEpochMs)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .format(dateFmt)
                    val ptRom = listOfNotNull(
                        pt.averageAngularRomDegrees?.let { String.format(java.util.Locale.US, "%.0f°", it) },
                        pt.averageLinearRomCm?.let { String.format(java.util.Locale.US, "%.0f cm", it) },
                    ).joinToString("/")
                    val ptDur = pt.averageRepDurationMs?.let { String.format(java.util.Locale.US, "%.2fs", it / 1000.0) }
                    val ptAccel = pt.maxPeakAccelerationMps2?.let { String.format(java.util.Locale.US, "%.2fm/s²", it) }
                    val summaryParts = listOfNotNull(
                        ptRom.takeIf { it.isNotEmpty() },
                        ptDur,
                        ptAccel,
                    ).joinToString(" · ").ifEmpty { "${pt.measuredRepCount} measured" }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        BasicText(
                            text = dateStr,
                            style = PulseTheme.typography.label.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                        BasicText(
                            text = summaryParts,
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

@Composable
private fun PersonalRecordsCard(records: List<PersonalRecordRowUiModel>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("progress_pr_card"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Key Lift Personal Records",
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
            )
            BasicText(
                text = "All-Time PRs",
                style = PulseTheme.typography.label.copy(
                    fontSize = 12.sp,
                    color = WorkoutPalette.TextMuted,
                ),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (records.isEmpty()) {
            BasicText(
                text = "No weighted personal records yet.",
                style = PulseTheme.typography.contentPrimary.copy(
                    fontSize = 13.sp,
                    color = WorkoutPalette.TextMuted,
                ),
                modifier = Modifier.testTag("progress_pr_empty"),
            )
        } else {
            records.forEachIndexed { index, pr ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_pr_row_${pr.exerciseKey}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        BasicText(
                            text = pr.exerciseName,
                            style = PulseTheme.typography.cardTitle.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        BasicText(
                            text = pr.dateLabel,
                            style = PulseTheme.typography.label.copy(
                                fontSize = 12.sp,
                                color = WorkoutPalette.TextMuted,
                            ),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        BasicText(
                            text = pr.weightLabel,
                            style = PulseTheme.typography.cardTitle.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PulseTheme.color.primary,
                            ),
                        )
                        BasicText(
                            text = pr.detailLabel,
                            style = PulseTheme.typography.label.copy(
                                fontSize = 12.sp,
                                color = WorkoutPalette.TextMuted,
                            ),
                        )
                    }
                }
                if (index < records.lastIndex) {
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
}

@Composable
private fun TrendLineIcon(color: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.12f, h * 0.72f), Offset(w * 0.38f, h * 0.45f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.38f, h * 0.45f), Offset(w * 0.56f, h * 0.62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.56f, h * 0.62f), Offset(w * 0.88f, h * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.68f, h * 0.28f), Offset(w * 0.88f, h * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.88f, h * 0.28f), Offset(w * 0.88f, h * 0.48f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}
