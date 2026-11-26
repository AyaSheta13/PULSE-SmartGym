package com.ayasheta.pulsesmartgym.feature.progress

import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.history.ExercisePersonalRecord
import com.ayasheta.pulsesmartgym.core.model.history.PeriodProgressMetrics
import com.ayasheta.pulsesmartgym.core.model.history.VolumeCoverage
import com.ayasheta.pulsesmartgym.core.model.history.WorkoutAnalytics
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionUiMapper
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object ProgressUiMapper {

    private val shortMonthDay = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    private val dayOnly = DateTimeFormatter.ofPattern("d", Locale.US)
    private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    private val fullDayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    fun mapToUiState(
        sessions: List<WorkoutSession>,
        weeklyTarget: Int?,
        unit: WeightUnit,
        selectedWeekOffset: Int,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): TrainingProgressUiState {
        val finished = sessions.filter {
            it.status == SessionStatus.COMPLETED || it.status == SessionStatus.PARTIALLY_COMPLETED
        }
        if (finished.isEmpty()) {
            return TrainingProgressUiState.Empty
        }

        val referenceDate = today.minusWeeks(selectedWeekOffset.coerceAtLeast(0).toLong())
        val metrics = WorkoutAnalytics.calculatePeriodMetrics(
            sessions = finished,
            weeklyTarget = weeklyTarget,
            referenceDate = referenceDate,
            currentToday = today,
            zoneId = zoneId,
        )
        val prs = WorkoutAnalytics.calculatePersonalRecords(finished, limit = 3)

        val weekOptions = (0..4).map { offset ->
            val ref = today.minusWeeks(offset.toLong())
            val start = ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = start.plusDays(6)
            val prefix = if (offset == 0) "This Week • " else ""
            WeekOptionUiModel(
                weekOffset = offset,
                label = "$prefix${formatWeekRange(start, end)}",
            )
        }

        return TrainingProgressUiState.Populated(
            selectedWeekOffset = selectedWeekOffset.coerceAtLeast(0),
            weekBadgeLabel = formatWeekRange(metrics.weekStart, metrics.weekEnd),
            weekOptions = weekOptions,
            workoutsCard = buildWorkoutsCard(metrics),
            volumeCard = buildVolumeCard(metrics, unit),
            setsCard = buildSetsCard(metrics),
            avgDurationCard = buildAvgDurationCard(metrics),
            chartHeaderVolumeText = buildChartHeaderVolume(metrics, unit, selectedWeekOffset.coerceAtLeast(0)),
            bars = buildBars(metrics, unit),
            personalRecords = prs.map { mapPrRow(it, unit, today, zoneId) },
        )
    }

    private fun buildWorkoutsCard(metrics: PeriodProgressMetrics): ProgressMetricCardUiModel {
        val delta = metrics.completedWorkoutsDelta
        val (deltaText, tone) = when {
            delta == null -> "No prior week" to DeltaTone.NEUTRAL
            delta > 0 -> "+$delta vs last wk" to DeltaTone.POSITIVE
            delta < 0 -> "$delta vs last wk" to DeltaTone.NEGATIVE
            else -> "Same as last wk" to DeltaTone.NEUTRAL
        }
        return ProgressMetricCardUiModel(
            title = "WORKOUTS",
            primaryValue = metrics.completedWorkoutsCount.toString(),
            primaryUnit = "done",
            deltaText = deltaText,
            deltaTone = tone,
        )
    }

    private fun buildVolumeCard(
        metrics: PeriodProgressMetrics,
        unit: WeightUnit,
    ): ProgressMetricCardUiModel {
        val isUnknownWithActivity = metrics.volumeCoverage == VolumeCoverage.NO_MEASURED_VOLUME &&
            metrics.setsLoggedCount > 0
        val (primaryVal, primaryUnit) = if (isUnknownWithActivity) {
            "—" to "unmeasured"
        } else {
            when (unit) {
                WeightUnit.KG -> {
                    if (metrics.totalVolumeKg >= 1000.0) {
                        val tonnes = ((metrics.totalVolumeKg / 1000.0) * 10.0).roundToLong() / 10.0
                        val exactKg = SessionUiMapper.formatVolumeNumber(metrics.totalVolumeKg, WeightUnit.KG)
                        val suffix = if (metrics.volumeCoverage == VolumeCoverage.PARTIALLY_MEASURED) " • partial" else ""
                        "$tonnes" to "tonnes ($exactKg kg$suffix)"
                    } else {
                        val suffix = if (metrics.volumeCoverage == VolumeCoverage.PARTIALLY_MEASURED) "kg (partial)" else "kg"
                        SessionUiMapper.formatVolumeNumber(metrics.totalVolumeKg, WeightUnit.KG) to suffix
                    }
                }
                WeightUnit.LBS -> {
                    val suffix = if (metrics.volumeCoverage == VolumeCoverage.PARTIALLY_MEASURED) "lbs (partial)" else "lbs"
                    SessionUiMapper.formatVolumeNumber(metrics.totalVolumeKg, WeightUnit.LBS) to suffix
                }
            }
        }

        val pct = metrics.volumeChangePercent
        val changePart = when {
            isUnknownWithActivity -> "Unmeasured loads"
            metrics.volumeCoverage == VolumeCoverage.PARTIALLY_MEASURED -> "Partial coverage"
            metrics.previousWeekVolumeCoverage == VolumeCoverage.PARTIALLY_MEASURED -> "Prior wk partial"
            metrics.previousWeekVolumeCoverage == VolumeCoverage.NO_MEASURED_VOLUME &&
                metrics.previousWeekSetsLoggedCount > 0 -> "Prior wk unmeasured"
            pct == null -> "No prior volume"
            pct > 0 -> "+$pct%"
            else -> "$pct%"
        }
        val targetPart = metrics.targetCompletionPercent?.let { " • $it% weekly target" } ?: ""
        val deltaText = "$changePart$targetPart"
        val tone = when {
            pct == null -> DeltaTone.NEUTRAL
            pct > 0 -> DeltaTone.POSITIVE
            pct < 0 -> DeltaTone.NEGATIVE
            else -> DeltaTone.NEUTRAL
        }

        return ProgressMetricCardUiModel(
            title = "WEEKLY VOLUME",
            primaryValue = primaryVal,
            primaryUnit = primaryUnit,
            deltaText = deltaText,
            deltaTone = tone,
        )
    }

    private fun buildSetsCard(metrics: PeriodProgressMetrics): ProgressMetricCardUiModel {
        val delta = metrics.setsLoggedDelta
        val (deltaText, tone) = when {
            delta == null -> "No prior week" to DeltaTone.NEUTRAL
            delta > 0 -> "+$delta vs last week" to DeltaTone.POSITIVE
            delta < 0 -> "$delta vs last week" to DeltaTone.NEGATIVE
            else -> "Same as last week" to DeltaTone.NEUTRAL
        }
        return ProgressMetricCardUiModel(
            title = "SETS LOGGED",
            primaryValue = metrics.setsLoggedCount.toString(),
            primaryUnit = if (metrics.setsLoggedCount == 1) "set" else "sets",
            deltaText = deltaText,
            deltaTone = tone,
        )
    }

    private fun buildAvgDurationCard(metrics: PeriodProgressMetrics): ProgressMetricCardUiModel {
        val avgMin = metrics.avgDurationMinutes
        val pace = metrics.paceMinutesPerSet
        val primaryVal = avgMin?.toString() ?: "—"
        val primaryUnit = if (avgMin != null) "min" else ""
        val deltaText = if (pace != null) {
            val roundedPace = ((pace * 10.0).roundToInt() / 10.0)
            "Pace: $roundedPace min/set"
        } else {
            "Pace: —"
        }
        return ProgressMetricCardUiModel(
            title = "AVG DURATION",
            primaryValue = primaryVal,
            primaryUnit = primaryUnit,
            deltaText = deltaText,
            deltaTone = DeltaTone.NEUTRAL,
        )
    }

    private fun buildChartHeaderVolume(
        metrics: PeriodProgressMetrics,
        unit: WeightUnit,
        selectedWeekOffset: Int,
    ): String {
        val periodSuffix = if (selectedWeekOffset == 0) {
            "This Week"
        } else {
            formatWeekRange(metrics.weekStart, metrics.weekEnd)
        }
        if (metrics.volumeCoverage == VolumeCoverage.NO_MEASURED_VOLUME && metrics.setsLoggedCount > 0) {
            return "Unmeasured ($periodSuffix)"
        }
        val formattedWithUnit = SessionUiMapper.formatVolumeWithUnit(metrics.totalVolumeKg, unit)
        return if (metrics.volumeCoverage == VolumeCoverage.PARTIALLY_MEASURED) {
            "$formattedWithUnit (partial) • $periodSuffix"
        } else {
            "$formattedWithUnit $periodSuffix"
        }
    }

    private fun buildBars(
        metrics: PeriodProgressMetrics,
        unit: WeightUnit,
    ): List<WeeklyVolumeBarUiModel> {
        val maxVol = metrics.dailyVolumeKg.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0

        return metrics.dailyVolumeKg.mapIndexed { idx, kg ->
            val heightFraction = if (kg <= 0.0) 0.06f else (kg / maxVol).toFloat().coerceIn(0.12f, 1f)
            val measured = metrics.dailyMeasuredFlags.getOrElse(idx) { false }
            val a11y = if (kg > 0.0) {
                "${fullDayNames[idx]}: ${SessionUiMapper.formatVolumeWithUnit(kg, unit)}"
            } else if (measured) {
                "${fullDayNames[idx]}: ${SessionUiMapper.formatVolumeWithUnit(0.0, unit)}"
            } else {
                "${fullDayNames[idx]}: No volume logged"
            }
            WeeklyVolumeBarUiModel(
                dayIndex = idx,
                dayLabel = dayLabels[idx],
                fullDayName = fullDayNames[idx],
                volumeKg = kg,
                heightFraction = heightFraction,
                isHighlighted = metrics.highlightDayIndex == idx,
                isMeasured = measured,
                accessibilityText = a11y,
            )
        }
    }

    private fun mapPrRow(
        pr: ExercisePersonalRecord,
        unit: WeightUnit,
        today: LocalDate,
        zoneId: ZoneId,
    ): PersonalRecordRowUiModel {
        val date = Instant.ofEpochMilli(pr.achievedAtEpochMs).atZone(zoneId).toLocalDate()
        val dateLabel = if (date.year == today.year) {
            date.format(shortMonthDay)
        } else {
            "${date.format(shortMonthDay)}, ${date.year}"
        }
        val weightLabel = SessionUiMapper.formatWeightLoad(pr.maxLoad, unit) ?: "—"
        val repWord = if (pr.repsAtMaxLoad == 1) "rep" else "reps"
        val improvementText = when {
            pr.previousBestLoad == null -> "First PR"
            (pr.improvementKg ?: 0.0) > 0.0 -> {
                val kg = pr.improvementKg!!
                val valInUnit = when (unit) {
                    WeightUnit.KG -> kg
                    WeightUnit.LBS -> kg * 2.20462262185
                }
                val rounded = ((valInUnit * 10.0).roundToInt() / 10.0)
                val formatted = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
                val u = if (unit == WeightUnit.KG) "kg" else "lbs"
                "+$formatted $u PR"
            }
            else -> "Matched PR"
        }

        return PersonalRecordRowUiModel(
            exerciseKey = pr.exerciseKey,
            exerciseName = pr.exerciseName,
            dateLabel = dateLabel,
            weightLabel = weightLabel,
            detailLabel = "${pr.repsAtMaxLoad} $repWord • $improvementText",
        )
    }

    fun formatWeekRange(start: LocalDate, end: LocalDate): String {
        return if (start.month == end.month && start.year == end.year) {
            "${start.format(shortMonthDay)}–${end.format(dayOnly)}"
        } else {
            "${start.format(shortMonthDay)}–${end.format(shortMonthDay)}"
        }
    }
}
