package com.ayasheta.pulsesmartgym.feature.session.execution

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ayasheta.pulsesmartgym.core.designsystem.component.card.Card
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Minus
import com.ayasheta.pulsesmartgym.core.designsystem.vectors.Plus
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionUiMapper
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette
import java.util.Locale
import kotlin.math.max
import kotlin.math.sin

@Composable
internal fun WorkoutInfoCard(
    workoutElapsedSec: Long,
    restSeconds: Int?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("set_execution_info_card"),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TimerOutlineIcon(tint = PulseTheme.color.primary)
                    BasicText(
                        text = "Workout Time",
                        style = PulseTheme.typography.label.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                }
                BasicText(
                    text = SessionUiMapper.formatElapsedMmSs(workoutElapsedSec),
                    style = PulseTheme.typography.numericMetric.copy(
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                    modifier = Modifier.testTag("set_execution_workout_elapsed"),
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(34.dp)
                    .background(Color(0xFFF6AFA0)),
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HourglassOutlineIcon(tint = PulseTheme.color.primary)
                    BasicText(
                        text = "Rest",
                        style = PulseTheme.typography.label.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA63A24),
                        ),
                    )
                }
                BasicText(
                    text = if (restSeconds != null && restSeconds > 0) "${restSeconds}s" else "0s",
                    style = PulseTheme.typography.numericMetric.copy(
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                    modifier = Modifier.testTag("set_execution_rest_info"),
                )
            }
        }
    }
}

@Composable
internal fun UnifiedRepAndWeightCard(
    currentReps: Int,
    targetReps: Int,
    weightNumberText: String,
    unitLabel: String,
    overlineLabel: String,
    canAdjustReps: Boolean,
    canAdjustWeight: Boolean,
    onDecrementRep: () -> Unit,
    onIncrementRep: () -> Unit,
    onDecrementWeight: () -> Unit,
    onIncrementWeight: () -> Unit,
    onEditWeightClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetProgress = if (targetReps <= 0) 0f else (currentReps.toFloat() / targetReps.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "unified_rep_progress",
    )
    val trackColor = Color(0xFFE0DFDD)
    val arcColor = PulseTheme.color.primary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("set_execution_counter_card"),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Decorative halos behind interactive content
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .testTag("rep_counter_card_decorations"),
            ) {
                val radius = 78.dp.toPx()
                drawCircle(
                    color = Color(0x1CFF623E),
                    radius = radius,
                    center = Offset(x = size.width - 28.dp.toPx(), y = 26.dp.toPx()),
                )
                drawCircle(
                    color = Color(0x1E39B8FD),
                    radius = radius,
                    center = Offset(x = 28.dp.toPx(), y = size.height - 28.dp.toPx()),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3F2F0))
                            .clickable(enabled = canAdjustReps, onClick = onDecrementRep),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            imageVector = Minus,
                            contentDescription = "Subtract one rep",
                            colorFilter = ColorFilter.tint(
                                if (canAdjustReps) WorkoutPalette.TextPrimary else WorkoutPalette.TextMuted,
                            ),
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 172.dp, height = 148.dp)
                            .testTag("rep_counter_gauge_arc")
                            .semantics {
                                progressBarRangeInfo = ProgressBarRangeInfo(
                                    current = animatedProgress,
                                    range = 0f..1f,
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(152.dp)
                                .padding(6.dp),
                        ) {
                            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            drawArc(
                                color = trackColor,
                                startAngle = 145f,
                                sweepAngle = 250f,
                                useCenter = false,
                                style = stroke,
                            )
                            if (animatedProgress > 0f) {
                                drawArc(
                                    color = arcColor,
                                    startAngle = 145f,
                                    sweepAngle = 250f * animatedProgress,
                                    useCenter = false,
                                    style = stroke,
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            BasicText(
                                text = overlineLabel,
                                style = PulseTheme.typography.label.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = WorkoutPalette.TextMuted,
                                ),
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                BasicText(
                                    text = currentReps.toString(),
                                    style = PulseTheme.typography.numericMetric.copy(
                                        fontSize = 48.sp,
                                        lineHeight = 52.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WorkoutPalette.TextPrimary,
                                    ),
                                    modifier = Modifier.testTag("text_rep_counter_value"),
                                )
                                BasicText(
                                    text = "/ $targetReps",
                                    style = PulseTheme.typography.numericMetric.copy(
                                        fontSize = 20.sp,
                                        lineHeight = 28.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WorkoutPalette.TextMuted,
                                    ),
                                    modifier = Modifier.padding(bottom = 6.dp),
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3F2F0))
                            .clickable(enabled = canAdjustReps, onClick = onIncrementRep),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            imageVector = Plus,
                            contentDescription = "Add one rep",
                            colorFilter = ColorFilter.tint(
                                if (canAdjustReps) WorkoutPalette.TextPrimary else WorkoutPalette.TextMuted,
                            ),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Compact inline weight control capsule inside the unified card (~250-260dp wide)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFF4F3F1))
                        .border(BorderStroke(1.dp, Color(0xFFE9E8E6)), RoundedCornerShape(999.dp))
                        .padding(start = 18.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                        .testTag("set_execution_weight_card"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                enabled = canAdjustWeight,
                                role = Role.Button,
                                onClick = onEditWeightClick,
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .semantics {
                                contentDescription = "Edit load weight: $weightNumberText $unitLabel. Tap to enter exact weight."
                            }
                            .testTag("button_edit_weight_direct"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        DumbbellVectorIcon(tint = WorkoutPalette.TextPrimary)
                        BasicText(
                            text = "Load:",
                            style = PulseTheme.typography.contentPrimary.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = WorkoutPalette.TextMuted,
                            ),
                        )
                        BasicText(
                            text = "$weightNumberText $unitLabel",
                            style = PulseTheme.typography.numericMetric.copy(
                                fontSize = 19.sp,
                                lineHeight = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = PulseTheme.color.primary,
                            ),
                            modifier = Modifier.testTag("text_current_weight_value"),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color(0xFFF3A798)),
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE7E6E4))
                                .clickable(enabled = canAdjustWeight, onClick = onDecrementWeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                imageVector = Minus,
                                contentDescription = "Decrease weight",
                                colorFilter = ColorFilter.tint(WorkoutPalette.TextPrimary),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE7E6E4))
                                .clickable(enabled = canAdjustWeight, onClick = onIncrementWeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                imageVector = Plus,
                                contentDescription = "Increase weight",
                                colorFilter = ColorFilter.tint(WorkoutPalette.TextPrimary),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class PlottedSignalKind(val unit: String, val label: String) {
    LINEAR_ROM("cm", "Linear ROM (cm)"),
    ANGULAR_ROM("°", "Angular ROM (°)"),
    PEAK_ACCEL("m/s²", "Peak Acceleration (m/s²)"),
    REP_DURATION("s", "Rep Duration (s)"),
}

@Composable
internal fun LivePerformanceWaveformCard(
    repRecords: List<RepMeasurement>,
    targetReps: Int,
    isStarted: Boolean,
    isDeviceAssisted: Boolean,
    isFallbackManual: Boolean,
    isSimulated: Boolean,
    modifier: Modifier = Modifier,
) {
    var showSignalInfo by rememberSaveable { mutableStateOf(false) }

    val signalKind = remember(repRecords) {
        when {
            repRecords.any { it.linearRomCm != null && it.linearRomCm!! >= 0.0 } -> PlottedSignalKind.LINEAR_ROM
            repRecords.any { it.angularRomDegrees != null && it.angularRomDegrees!! >= 0.0 } -> PlottedSignalKind.ANGULAR_ROM
            repRecords.any { it.peakAccelerationMetersPerSecondSquared != null && it.peakAccelerationMetersPerSecondSquared!! >= 0.0 } ->
                PlottedSignalKind.PEAK_ACCEL
            repRecords.any { it.totalDurationMs != null && it.totalDurationMs!! >= 0L } -> PlottedSignalKind.REP_DURATION
            else -> null
        }
    }

    val seriesValues: List<Double?> = remember(repRecords, signalKind) {
        when (signalKind) {
            PlottedSignalKind.LINEAR_ROM -> repRecords.map { it.linearRomCm?.takeIf { v -> v.isFinite() && v >= 0.0 } }
            PlottedSignalKind.ANGULAR_ROM -> repRecords.map { it.angularRomDegrees?.takeIf { v -> v.isFinite() && v >= 0.0 } }
            PlottedSignalKind.PEAK_ACCEL -> repRecords.map {
                it.peakAccelerationMetersPerSecondSquared?.takeIf { v -> v.isFinite() && v >= 0.0 }
            }
            PlottedSignalKind.REP_DURATION -> repRecords.map {
                it.totalDurationMs?.takeIf { v -> v >= 0L }?.div(1000.0)
            }
            null -> emptyList()
        }
    }

    val validValues = remember(seriesValues) { seriesValues.filterNotNull() }
    val hasMeasuredData = validValues.isNotEmpty()
    val statusBadgeText = when {
        hasMeasuredData && isSimulated -> "Rep #${repRecords.size} • Sim"
        hasMeasuredData -> "Rep #${repRecords.size}"
        isFallbackManual || (!isDeviceAssisted && isStarted) -> "Manual • No telemetry"
        else -> "No measured reps"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("set_execution_live_performance"),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PerformanceBarsIcon(tint = PulseTheme.color.primary)
                    BasicText(
                        text = "Live Performance",
                        style = PulseTheme.typography.cardTitle.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFF3F2F0))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("live_performance_status_pill"),
                ) {
                    BasicText(
                        text = statusBadgeText,
                        style = PulseTheme.typography.label.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasMeasuredData) WorkoutPalette.TextPrimary else WorkoutPalette.TextMuted,
                        ),
                    )
                }
            }

            // Integrated chart directly inside Live Performance Card (no nested rounded box)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = if (signalKind != null) "Motion Trend (${signalKind.label})" else "Motion Trend (This Set)",
                    style = PulseTheme.typography.label.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WorkoutPalette.TextMuted,
                    ),
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { showSignalInfo = !showSignalInfo }
                        .semantics { contentDescription = "Motion trend chart info" }
                        .testTag("button_motion_trend_info"),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "ⓘ",
                        style = PulseTheme.typography.label.copy(
                            fontSize = 13.sp,
                            color = WorkoutPalette.TextMuted,
                        ),
                    )
                }
            }

            if (showSignalInfo) {
                BasicText(
                    text = if (signalKind != null) {
                        "Plots recorded ${signalKind.label} across measured reps in this set."
                    } else {
                        "Illustrative placeholder until compatible sensor telemetry is recorded."
                    },
                    style = PulseTheme.typography.label.copy(
                        fontSize = 11.sp,
                        color = WorkoutPalette.TextMuted,
                    ),
                )
            }

            val maxVal = validValues.maxOrNull() ?: 1.0
            val minVal = validValues.minOrNull() ?: 0.0
            val midVal = (maxVal + minVal) / 2.0
            val xCount = max(targetReps.coerceIn(4, 12), seriesValues.size)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Column(
                    modifier = Modifier
                        .width(34.dp)
                        .height(70.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    if (hasMeasuredData && signalKind != null) {
                        BasicText(
                            text = formatAxisNumber(maxVal, ""),
                            style = PulseTheme.typography.label.copy(fontSize = 9.sp, color = WorkoutPalette.TextMuted),
                        )
                        BasicText(
                            text = formatAxisNumber(midVal, ""),
                            style = PulseTheme.typography.label.copy(fontSize = 9.sp, color = WorkoutPalette.TextMuted),
                        )
                        BasicText(
                            text = signalKind.unit,
                            style = PulseTheme.typography.label.copy(fontSize = 9.sp, color = WorkoutPalette.TextMuted),
                        )
                    } else {
                        BasicText(
                            text = "—",
                            style = PulseTheme.typography.label.copy(fontSize = 9.sp, color = WorkoutPalette.TextMuted),
                        )
                        BasicText(
                            text = "—",
                            style = PulseTheme.typography.label.copy(fontSize = 9.sp, color = WorkoutPalette.TextMuted),
                        )
                        BasicText(
                            text = "0",
                            style = PulseTheme.typography.label.copy(fontSize = 9.sp, color = WorkoutPalette.TextMuted),
                        )
                    }
                }

                val waveColor = if (hasMeasuredData) PulseTheme.color.primary else Color(0xFFC5C4C2)
                val fillColor = if (hasMeasuredData) Color(0x28FF5D37) else Color(0x1E9E9D9B)

                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .testTag(if (hasMeasuredData) "motion_trend_measured_canvas" else "motion_trend_placeholder_canvas"),
                ) {
                    val w = size.width
                    val h = size.height
                    val padTop = 6.dp.toPx()
                    val padBottom = 4.dp.toPx()
                    val plotH = (h - padTop - padBottom).coerceAtLeast(1f)

                    // Minimal left vertical axis & bottom horizontal baseline
                    drawLine(
                        color = Color(0xFFE0DFDD),
                        start = Offset(0f, 0f),
                        end = Offset(0f, h - padBottom),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawLine(
                        color = Color(0xFFE0DFDD),
                        start = Offset(0f, h - padBottom),
                        end = Offset(w, h - padBottom),
                        strokeWidth = 1.dp.toPx(),
                    )

                    if (hasMeasuredData) {
                        val span = (maxVal - minVal).takeIf { it > 0.0001 } ?: 1.0
                        val totalIndices = seriesValues.size.coerceAtLeast(1)
                        val segments = mutableListOf<MutableList<Offset>>()
                        var currentSegment: MutableList<Offset>? = null

                        seriesValues.forEachIndexed { idx, v ->
                            if (v == null) {
                                currentSegment = null
                            } else {
                                val x = if (totalIndices == 1) {
                                    w * 0.5f
                                } else {
                                    w * (idx.toFloat() / (totalIndices - 1).toFloat())
                                }
                                val norm = ((v - minVal) / span).toFloat().coerceIn(0f, 1f)
                                val y = if (totalIndices == 1) {
                                    padTop + plotH * 0.35f
                                } else {
                                    h - padBottom - norm * plotH
                                }
                                val seg = currentSegment ?: mutableListOf<Offset>().also {
                                    segments.add(it)
                                    currentSegment = it
                                }
                                seg.add(Offset(x, y))
                            }
                        }

                        segments.forEach { seg ->
                            if (seg.size == 1) {
                                val pt = seg.first()
                                if (totalIndices == 1) {
                                    val expanded = listOf(Offset(0f, h - padBottom), pt, Offset(w, pt.y))
                                    val strokePath = Path().apply {
                                        moveTo(expanded.first().x, expanded.first().y)
                                        for (i in 0 until expanded.lastIndex) {
                                            val p0 = expanded[i]
                                            val p1 = expanded[i + 1]
                                            val midX = (p0.x + p1.x) / 2f
                                            cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                        }
                                    }
                                    val fillPath = Path().apply {
                                        addPath(strokePath)
                                        lineTo(expanded.last().x, h - padBottom)
                                        lineTo(expanded.first().x, h - padBottom)
                                        close()
                                    }
                                    drawPath(fillPath, brush = Brush.verticalGradient(listOf(fillColor, Color.Transparent)))
                                    drawPath(strokePath, color = waveColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                                } else {
                                    drawCircle(color = waveColor, radius = 3.5.dp.toPx(), center = pt)
                                }
                            } else if (seg.size >= 2) {
                                val strokePath = Path().apply {
                                    moveTo(seg.first().x, seg.first().y)
                                    for (i in 0 until seg.lastIndex) {
                                        val p0 = seg[i]
                                        val p1 = seg[i + 1]
                                        val midX = (p0.x + p1.x) / 2f
                                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                    }
                                }
                                val fillPath = Path().apply {
                                    addPath(strokePath)
                                    lineTo(seg.last().x, h - padBottom)
                                    lineTo(seg.first().x, h - padBottom)
                                    close()
                                }
                                drawPath(fillPath, brush = Brush.verticalGradient(listOf(fillColor, Color.Transparent)))
                                drawPath(strokePath, color = waveColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                        }
                    } else {
                        // Smooth continuous illustrative wave with rounded peaks & rounded valleys
                        val periods = 5
                        val halfPeriods = periods * 2
                        val points = (0..halfPeriods).map { idx ->
                            val x = w * (idx.toFloat() / halfPeriods.toFloat())
                            val isPeak = idx % 2 == 1
                            val y = if (isPeak) padTop + plotH * 0.22f else h - padBottom - plotH * 0.16f
                            Offset(x, y)
                        }
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.lastIndex) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val midX = (p0.x + p1.x) / 2f
                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            }
                        }
                        val fillPath = Path().apply {
                            addPath(strokePath)
                            lineTo(points.last().x, h - padBottom)
                            lineTo(points.first().x, h - padBottom)
                            close()
                        }
                        drawPath(fillPath, brush = Brush.verticalGradient(listOf(fillColor, Color.Transparent)))
                        drawPath(strokePath, color = waveColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val count = if (hasMeasuredData) seriesValues.size else xCount
                for (i in 1..count.coerceAtLeast(2)) {
                    BasicText(
                        text = i.toString(),
                        style = PulseTheme.typography.label.copy(fontSize = 10.sp, color = WorkoutPalette.TextMuted),
                    )
                }
            }
        }
    }
}

@Composable
internal fun SetLoadBottomSheet(
    visible: Boolean,
    initialWeightText: String,
    unitLabel: String,
    inWindow: Boolean,
    onDismiss: () -> Unit,
    onApply: (String) -> String?,
) {
    if (!visible) return

    var inputText by remember(visible, initialWeightText) { mutableStateOf(initialWeightText) }
    var errorMessage by remember(visible) { mutableStateOf<String?>(null) }

    val content: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(Color.White)
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .testTag("sheet_direct_weight_input"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BasicText(
                    text = "Set Load",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF4F3F1))
                        .border(
                            BorderStroke(
                                1.5.dp,
                                if (errorMessage != null) WorkoutPalette.ErrorText else PulseTheme.color.primary,
                            ),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            errorMessage = null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        cursorBrush = SolidColor(PulseTheme.color.primary),
                        textStyle = PulseTheme.typography.numericMetric.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_direct_weight"),
                    )
                    BasicText(
                        text = unitLabel,
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WorkoutPalette.TextMuted,
                        ),
                    )
                }

                if (errorMessage != null) {
                    BasicText(
                        text = errorMessage!!,
                        style = PulseTheme.typography.label.copy(
                            fontSize = 12.sp,
                            color = WorkoutPalette.ErrorText,
                        ),
                        modifier = Modifier.testTag("error_direct_weight"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF4F3F1))
                            .clickable(onClick = onDismiss)
                            .testTag("button_cancel_direct_weight"),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "Cancel",
                            style = PulseTheme.typography.contentPrimary.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PulseTheme.color.primary)
                            .clickable {
                                val err = onApply(inputText)
                                if (err != null) {
                                    errorMessage = err
                                } else {
                                    onDismiss()
                                }
                            }
                            .testTag("button_apply_direct_weight"),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "Apply",
                            style = PulseTheme.typography.contentPrimary.copy(
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

    if (inWindow) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            content()
        }
    } else {
        content()
    }
}

private fun formatAxisNumber(value: Double, unit: String): String {
    val formatted = if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
    return "$formatted $unit"
}

@Composable
private fun TimerOutlineIcon(tint: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(color = tint, radius = size.minDimension * 0.38f, style = stroke)
        drawLine(
            color = tint,
            start = Offset(size.width * 0.5f, size.height * 0.5f),
            end = Offset(size.width * 0.5f, size.height * 0.28f),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun HourglassOutlineIcon(tint: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.15f)
            lineTo(w * 0.8f, h * 0.15f)
            lineTo(w * 0.68f, h * 0.85f)
            lineTo(w * 0.32f, h * 0.85f)
            close()
        }
        drawPath(path, color = tint, style = Stroke(width = 1.4.dp.toPx(), join = StrokeJoin.Round))
    }
}

@Composable
private fun DumbbellVectorIcon(tint: Color) {
    Canvas(modifier = Modifier.size(width = 18.dp, height = 14.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.05f, h * 0.2f),
            size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.22f, h * 0.08f),
            size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.84f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.36f, h * 0.5f),
            end = Offset(w * 0.64f, h * 0.5f),
            strokeWidth = 2.dp.toPx(),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.64f, h * 0.08f),
            size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.84f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.81f, h * 0.2f),
            size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
        )
    }
}

@Composable
private fun PerformanceBarsIcon(tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val barW = w * 0.2f
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.55f),
            size = androidx.compose.ui.geometry.Size(barW, h * 0.4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.4f, h * 0.32f),
            size = androidx.compose.ui.geometry.Size(barW, h * 0.63f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.72f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(barW, h * 0.85f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()),
        )
    }
}
