package com.ayasheta.pulsesmartgym.feature.history

import com.ayasheta.pulsesmartgym.R
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.history.HistoryAggregateMetrics
import com.ayasheta.pulsesmartgym.core.model.history.SessionSummary
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object HistoryUiMapper {

    private val monthDayFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val monthHeaderFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.US)
    private val monthYearHeaderFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

    fun mapToUiState(
        summaries: List<SessionSummary>,
        aggregates: HistoryAggregateMetrics,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): WorkoutHistoryUiState {
        val eligible = summaries
            .filter { it.status == SessionStatus.COMPLETED || it.status == SessionStatus.PARTIALLY_COMPLETED }
            .sortedWith(
                compareByDescending<SessionSummary> { it.startedAtEpochMs }
                    .thenBy { it.sessionId },
            )
        if (eligible.isEmpty()) return WorkoutHistoryUiState.Empty

        val sessionWord = if (aggregates.totalSessions == 1) "saved session" else "saved sessions"
        val savedSessionsHeadline = "${aggregates.totalSessions} $sessionWord"

        val repsWord = if (aggregates.totalFullReps == 1) "rep" else "reps"
        val setsWord = if (aggregates.totalCompletedSets == 1) "set" else "sets"
        val partialSuffix = if (aggregates.totalPartialReps > 0) " (+${aggregates.totalPartialReps} partial)" else ""
        val durationPart = if (aggregates.totalDurationMs > 0L) {
            " • ${formatDurationCompact(aggregates.totalDurationMs)}"
        } else {
            ""
        }
        val aggregateMetricsLine =
            "${aggregates.totalFullReps} $repsWord$partialSuffix • ${aggregates.totalCompletedSets} $setsWord$durationPart"

        val hasPartial = eligible.any { it.status == SessionStatus.PARTIALLY_COMPLETED }
        val currentYear = today.year
        val grouped = eligible.groupBy { summary ->
            val date = Instant.ofEpochMilli(summary.startedAtEpochMs).atZone(zoneId).toLocalDate()
            if (hasPartial && date.year == currentYear && date.month == today.month) {
                "RECENT" to "RECENT"
            } else {
                val key = "${date.year}-${date.monthValue}"
                val label = if (date.year == currentYear) {
                    date.format(monthHeaderFormatter).uppercase(Locale.US)
                } else {
                    date.format(monthYearHeaderFormatter).uppercase(Locale.US)
                }
                key to label
            }
        }

        val sections = grouped.entries.map { (keyPair, items) ->
            HistorySectionUiModel(
                key = keyPair.first,
                title = keyPair.second,
                items = items.map { mapRow(it, today, zoneId) },
            )
        }

        return WorkoutHistoryUiState.Populated(
            savedSessionsHeadline = savedSessionsHeadline,
            aggregateMetricsLine = aggregateMetricsLine,
            sections = sections,
        )
    }

    private fun mapRow(
        summary: SessionSummary,
        today: LocalDate,
        zoneId: ZoneId,
    ): HistorySessionItemUiModel {
        val instant = Instant.ofEpochMilli(summary.startedAtEpochMs).atZone(zoneId)
        val localDate = instant.toLocalDate()
        val dateLabel = when (localDate) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> if (localDate.year == today.year) {
                localDate.format(monthDayFormatter)
            } else {
                "${localDate.format(monthDayFormatter)}, ${localDate.year}"
            }
        }
        val timeLabel = instant.format(timeFormatter)
        val isPartial = summary.status == SessionStatus.PARTIALLY_COMPLETED

        val subtitleLine = if (isPartial) {
            "$dateLabel • Ended Early"
        } else {
            "$dateLabel • $timeLabel"
        }

        val durLabel = summary.durationMs?.let { durMs ->
            if (isPartial) formatMmSs(durMs) else formatDurationCompact(durMs)
        }
        val setsLabel = if (isPartial) {
            "${summary.completedSetsCount} / ${summary.totalPlannedSetsCount} sets"
        } else {
            val word = if (summary.completedSetsCount == 1) "set" else "sets"
            "${summary.completedSetsCount} $word"
        }
        val repsWord = if (summary.totalFullReps == 1) "rep" else "reps"
        val repsLabel = if (summary.totalPartialReps > 0) {
            "${summary.totalFullReps} $repsWord (+${summary.totalPartialReps}p)"
        } else {
            "${summary.totalFullReps} $repsWord"
        }
        val metricsLine = listOfNotNull(durLabel, setsLabel, repsLabel).joinToString(" • ")

        val statusBadgeLine = if (isPartial) {
            "Partial • Saved on device"
        } else {
            "✓ Saved on device"
        }

        val thumbRes = resolveHistoryThumbnail(
            sourceExerciseId = summary.leadExerciseSourceId,
            exerciseName = summary.leadExerciseName,
            muscleGroup = summary.leadMuscleGroup,
            planName = summary.planNameSnapshot,
        )

        return HistorySessionItemUiModel(
            sessionId = summary.sessionId,
            planName = summary.planNameSnapshot,
            subtitleLine = subtitleLine,
            metricsLine = metricsLine,
            statusBadgeLine = statusBadgeLine,
            isPartial = isPartial,
            status = summary.status,
            thumbnailRes = thumbRes,
        )
    }

    fun resolveHistoryThumbnail(
        sourceExerciseId: String?,
        exerciseName: String?,
        muscleGroup: MuscleGroup?,
        planName: String,
    ): Int {
        if (!sourceExerciseId.isNullOrBlank()) {
            return resolveExerciseThumbnailRes(
                imageKey = sourceExerciseId,
                muscleGroup = muscleGroup,
            )
        }
        val lowerName = (exerciseName ?: planName).lowercase(Locale.US)
        return when {
            "bench" in lowerName -> R.drawable.ex_thumb_bench_press
            "squat" in lowerName -> R.drawable.ex_thumb_back_squat
            "row" in lowerName -> R.drawable.ex_thumb_barbell_row
            "incline" in lowerName -> R.drawable.ex_thumb_incline_press
            "curl" in lowerName -> R.drawable.ex_thumb_bicep_curl
            "lower" in lowerName || "leg" in lowerName -> R.drawable.plan_thumb_lower_body
            "full" in lowerName -> R.drawable.plan_thumb_full_body
            else -> R.drawable.plan_thumb_upper_body
        }
    }

    fun formatDurationCompact(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return when {
            hours > 0L -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0L -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun formatMmSs(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%02d:%02d".format(minutes, seconds)
    }
}
