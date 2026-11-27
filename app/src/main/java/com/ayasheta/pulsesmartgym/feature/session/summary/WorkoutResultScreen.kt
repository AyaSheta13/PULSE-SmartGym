package com.ayasheta.pulsesmartgym.feature.session.summary

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
import com.ayasheta.pulsesmartgym.core.designsystem.component.progress.PulseLoadingIndicator
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Back
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.CheckMark
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.HistoryClock
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette
import com.ayasheta.pulsesmartgym.feature.workout.editor.LocalRenderModalsInWindow

@Composable
fun WorkoutResultScreen(
    viewModel: WorkoutResultViewModel,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
    onViewHistory: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutResultScreenContent(
        uiState = uiState,
        onBackToHome = onBackToHome,
        onViewHistory = onViewHistory,
        modifier = modifier,
    )
}

@Composable
fun WorkoutResultScreenContent(
    uiState: WorkoutResultUiState,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
    onViewHistory: (() -> Unit)? = null,
) {
    val renderInWindow = LocalRenderModalsInWindow.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WorkoutPalette.Background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WorkoutPalette.Background)
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBackToHome)
                    .testTag("summary_back_button"),
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
                text = "Workout Summary",
                style = PulseTheme.typography.cardTitle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkoutPalette.TextPrimary,
                ),
            )
        }

        when (uiState) {
            WorkoutResultUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PulseLoadingIndicator(modifier = Modifier.testTag("summary_loading"))
                }
            }
            WorkoutResultUiState.SignedOut -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    BasicText(
                        text = "Sign in to view your workout summary.",
                        style = PulseTheme.typography.contentPrimary.copy(fontSize = 15.sp, color = WorkoutPalette.TextMuted),
                        modifier = Modifier.testTag("summary_signed_out"),
                    )
                }
            }
            is WorkoutResultUiState.Missing -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    BasicText(
                        text = uiState.message,
                        style = PulseTheme.typography.contentPrimary.copy(fontSize = 15.sp, color = WorkoutPalette.TextMuted),
                        modifier = Modifier.testTag("summary_missing"),
                    )
                }
            }
            is WorkoutResultUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    BasicText(
                        text = uiState.message,
                        style = PulseTheme.typography.contentPrimary.copy(fontSize = 15.sp, color = WorkoutPalette.ErrorText),
                        modifier = Modifier.testTag("summary_error"),
                    )
                }
            }
            is WorkoutResultUiState.Loaded -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .let { if (renderInWindow) it else it.navigationBarsPadding() }
                        .testTag("summary_content"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "status_card") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                                .testTag("summary_status_card"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val badgeColor = if (uiState.status == SessionStatus.ABANDONED) {
                                Color(0xFF8D8781)
                            } else {
                                Color(0xFF00B87C)
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (uiState.status != SessionStatus.ABANDONED) {
                                    Image(
                                        imageVector = CheckMark,
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(Color.White),
                                        modifier = Modifier.size(16.dp),
                                    )
                                } else {
                                    BasicText(
                                        text = "—",
                                        style = PulseTheme.typography.ctaPrimary.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        ),
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                BasicText(
                                    text = uiState.statusHeadline,
                                    style = PulseTheme.typography.cardTitle.copy(
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WorkoutPalette.TextPrimary,
                                    ),
                                    modifier = Modifier.testTag("summary_headline"),
                                )
                                BasicText(
                                    text = uiState.statusSubtitle,
                                    style = PulseTheme.typography.contentPrimary.copy(
                                        fontSize = 12.sp,
                                        color = Color(0xFF7A7570),
                                    ),
                                    modifier = Modifier.testTag("summary_subtitle"),
                                )
                            }
                        }
                    }

                    item(key = "hero_metrics") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                                .testTag("summary_hero_metrics"),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    BasicText(
                                        text = "TOTAL REPETITIONS",
                                        style = PulseTheme.typography.contentPrimary.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF7A7570),
                                        ),
                                    )
                                    if (uiState.totalPartialReps > 0) {
                                        BasicText(
                                            text = "+ ${uiState.totalPartialReps} partial reps",
                                            style = PulseTheme.typography.contentPrimary.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF7A7570),
                                            ),
                                        )
                                    }
                                }
                                BasicText(
                                    text = uiState.totalFullReps.toString(),
                                    style = PulseTheme.typography.cardTitle.copy(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WorkoutPalette.PrimaryCoral,
                                    ),
                                    modifier = Modifier.testTag("summary_total_reps"),
                                )
                            }

                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SummaryMetricChip(
                                    text = uiState.durationText,
                                    showClock = true,
                                )
                                SummaryMetricChip(
                                    text = uiState.setsChipText,
                                    showClock = false,
                                )
                                SummaryMetricChip(
                                    text = uiState.volumeChipText,
                                    showClock = false,
                                )
                            }
                        }
                    }

                    item(key = "exercise_breakdown") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, Color(0xFFF1EEEA)), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                                .testTag("summary_breakdown_card"),
                        ) {
                            uiState.performanceSummary?.let { perf ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFAF8F6))
                                        .border(BorderStroke(1.dp, Color(0xFFECE8E3)), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                        .testTag("result_performance_summary"),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    BasicText(
                                        text = "Performance Summary (${perf.measuredRepCount}/${perf.totalRecordedReps} measured reps)",
                                        style = PulseTheme.typography.label.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WorkoutPalette.TextPrimary,
                                        ),
                                    )
                                    val metricsLine = listOfNotNull(
                                        perf.averageRepDurationMs?.let {
                                            String.format(java.util.Locale.US, "Avg Rep: %.2fs", it / 1000.0)
                                        },
                                        perf.averageAngularRomDegrees?.let {
                                            String.format(java.util.Locale.US, "Avg Angular ROM: %.1f°", it)
                                        },
                                        perf.averageLinearRomCm?.let {
                                            String.format(java.util.Locale.US, "Avg Linear ROM: %.1f cm", it)
                                        },
                                        perf.maxPeakAccelerationMps2?.let {
                                            String.format(java.util.Locale.US, "Peak Accel: %.2f m/s²", it)
                                        },
                                    ).joinToString("  •  ")
                                    if (metricsLine.isNotEmpty()) {
                                        BasicText(
                                            text = metricsLine,
                                            style = PulseTheme.typography.label.copy(
                                                fontSize = 11.sp,
                                                color = WorkoutPalette.TextMuted,
                                            ),
                                        )
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
                                    style = PulseTheme.typography.contentPrimary.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF7A7570),
                                    ),
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            uiState.exercises.forEachIndexed { index, item ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color(0xFFF3F0EC)),
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        BasicText(
                                            text = item.exerciseName,
                                            style = PulseTheme.typography.contentPrimary.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WorkoutPalette.TextPrimary,
                                            ),
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            val repLabel = if (item.isSkipped) {
                                                "0 sets • 0 reps (Skipped)"
                                            } else {
                                                val setWord = if (item.recordedSetsCount == 1) "set" else "sets"
                                                val base = "${item.recordedSetsCount} $setWord • ${item.totalFullReps} reps"
                                                if (item.totalPartialReps > 0) "$base (+${item.totalPartialReps} partial)" else base
                                            }
                                            BasicText(
                                                text = repLabel,
                                                style = PulseTheme.typography.contentPrimary.copy(
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF7A7570),
                                                ),
                                            )
                                            if (item.loadBadgeText != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFFE8F8F1))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                ) {
                                                    BasicText(
                                                        text = item.loadBadgeText,
                                                        style = PulseTheme.typography.contentPrimary.copy(
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF008A5B),
                                                        ),
                                                    )
                                                }
                                            } else if (item.isSkipped) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFFF1F7F4))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                ) {
                                                    BasicText(
                                                        text = "—",
                                                        style = PulseTheme.typography.contentPrimary.copy(
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF6E7D76),
                                                        ),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF6F3F2))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        BasicText(
                                            text = item.modeBadgeText,
                                            style = PulseTheme.typography.contentPrimary.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF5A5754),
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "back_to_home_cta") {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (uiState.status != SessionStatus.ABANDONED && onViewHistory != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WorkoutPalette.PrimaryCoral)
                                    .clickable(onClick = onViewHistory)
                                    .testTag("button_summary_view_history"),
                                contentAlignment = Alignment.Center,
                            ) {
                                BasicText(
                                    text = "View History",
                                    style = PulseTheme.typography.ctaPrimary.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    ),
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onBackToHome)
                                    .padding(vertical = 10.dp)
                                    .testTag("button_summary_back_home"),
                                contentAlignment = Alignment.Center,
                            ) {
                                BasicText(
                                    text = "Back to Home →",
                                    style = PulseTheme.typography.contentPrimary.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = WorkoutPalette.TextMuted,
                                    ),
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WorkoutPalette.PrimaryCoral)
                                    .clickable(onClick = onBackToHome)
                                    .testTag("button_summary_back_home"),
                                contentAlignment = Alignment.Center,
                            ) {
                                BasicText(
                                    text = "Back to Home",
                                    style = PulseTheme.typography.ctaPrimary.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
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

@Composable
private fun SummaryMetricChip(
    text: String,
    showClock: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF6F3F2))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showClock) {
            Image(
                imageVector = HistoryClock,
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFF5A5754)),
                modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.size(4.dp))
        }
        BasicText(
            text = text,
            style = PulseTheme.typography.contentPrimary.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = WorkoutPalette.TextPrimary,
            ),
            maxLines = 2,
        )
    }
}
