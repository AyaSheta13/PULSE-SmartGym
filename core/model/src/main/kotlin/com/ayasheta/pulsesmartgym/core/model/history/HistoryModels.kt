package com.ayasheta.pulsesmartgym.core.model.history

import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlan
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class SessionSummary(
    val sessionId: String,
    val planNameSnapshot: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val status: SessionStatus,
    val completedSetsCount: Int,
    val totalPlannedSetsCount: Int,
    val totalFullReps: Int,
    val totalPartialReps: Int,
    val totalVolumeKg: Double,
    val durationMs: Long?,
)

data class HistoryAggregateMetrics(
    val totalSessions: Int,
    val completedSessions: Int,
    val partialSessions: Int,
    val totalFullReps: Int,
    val totalPartialReps: Int,
    val totalVolumeKg: Double,
)

data class WeeklyProgressSummary(
    val completedWorkoutsCount: Int,
    val partialWorkoutsCount: Int,
    val weeklyTarget: Int?,
    val totalVolumeKg: Double,
    val dailyVolumeKg: List<Double>,
    val dayOfWeekNumber: Int,
    val progressFraction: Float,
)

object WorkoutAnalytics {

    /**
     * Weighted volume uses actual logged full repetitions multiplied by actual recorded load.
     * Partial repetitions are tracked separately and excluded from weighted volume.
     * Sets with unknown/null load are excluded rather than treated as 0 kg.
     */
    fun calculateVolumeKg(performedSets: List<PerformedSet>): Double =
        performedSets.sumOf { set ->
            val load = set.actualLoad ?: return@sumOf 0.0
            set.fullReps * load.toKilograms()
        }

    fun summarizeSession(session: WorkoutSession): SessionSummary {
        val allPerformed = session.exercises.flatMap { it.performedSets }
        val totalPlanned = session.exercises.sumOf { it.plannedSets.size }
        val durationMs = session.endedAtEpochMs?.let { end ->
            (end - session.startedAtEpochMs).coerceAtLeast(0L)
        }
        return SessionSummary(
            sessionId = session.id,
            planNameSnapshot = session.planNameSnapshot,
            startedAtEpochMs = session.startedAtEpochMs,
            endedAtEpochMs = session.endedAtEpochMs,
            status = session.status,
            completedSetsCount = allPerformed.size,
            totalPlannedSetsCount = totalPlanned,
            totalFullReps = allPerformed.sumOf { it.fullReps },
            totalPartialReps = allPerformed.sumOf { it.partialReps },
            totalVolumeKg = calculateVolumeKg(allPerformed),
            durationMs = durationMs,
        )
    }

    fun aggregateSummaries(summaries: List<SessionSummary>): HistoryAggregateMetrics =
        HistoryAggregateMetrics(
            totalSessions = summaries.size,
            completedSessions = summaries.count { it.status == SessionStatus.COMPLETED },
            partialSessions = summaries.count { it.status == SessionStatus.PARTIALLY_COMPLETED },
            totalFullReps = summaries.sumOf { it.totalFullReps },
            totalPartialReps = summaries.sumOf { it.totalPartialReps },
            totalVolumeKg = summaries.sumOf { it.totalVolumeKg },
        )

    /**
     * Computes weekly training progress for the Monday-through-Sunday local calendar week
     * containing [referenceDate] in [zoneId].
     *
     * Policy:
     * - UTC [SessionSummary.startedAtEpochMs] instants are mapped to [LocalDate] in [zoneId].
     * - [WeeklyProgressSummary.completedWorkoutsCount] counts [SessionStatus.COMPLETED] only;
     *   [SessionStatus.PARTIALLY_COMPLETED] is tracked separately in [WeeklyProgressSummary.partialWorkoutsCount].
     * - [WeeklyProgressSummary.totalVolumeKg] and [WeeklyProgressSummary.dailyVolumeKg] sum actual
     *   performed weighted volume across finished sessions (COMPLETED + PARTIALLY_COMPLETED).
     * - Active and cancelled sessions are excluded.
     */
    fun calculateWeeklySummary(
        summaries: List<SessionSummary>,
        weeklyTarget: Int?,
        referenceDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): WeeklyProgressSummary {
        val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val dailyVolumes = DoubleArray(7) { 0.0 }

        var completedCount = 0
        var partialCount = 0
        var totalVolumeKg = 0.0

        for (summary in summaries) {
            val isFinished = summary.status == SessionStatus.COMPLETED ||
                summary.status == SessionStatus.PARTIALLY_COMPLETED
            if (!isFinished) continue

            val sessionDate = Instant.ofEpochMilli(summary.startedAtEpochMs)
                .atZone(zoneId)
                .toLocalDate()
            if (sessionDate.isBefore(weekStart) || sessionDate.isAfter(weekEnd)) continue

            when (summary.status) {
                SessionStatus.COMPLETED -> completedCount += 1
                SessionStatus.PARTIALLY_COMPLETED -> partialCount += 1
                else -> Unit
            }

            val dayIndex = (sessionDate.dayOfWeek.value - 1).coerceIn(0, 6)
            dailyVolumes[dayIndex] += summary.totalVolumeKg
            totalVolumeKg += summary.totalVolumeKg
        }

        val fraction = if (weeklyTarget != null && weeklyTarget > 0) {
            (completedCount.toFloat() / weeklyTarget.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        return WeeklyProgressSummary(
            completedWorkoutsCount = completedCount,
            partialWorkoutsCount = partialCount,
            weeklyTarget = weeklyTarget,
            totalVolumeKg = totalVolumeKg,
            dailyVolumeKg = dailyVolumes.toList(),
            dayOfWeekNumber = referenceDate.dayOfWeek.value,
            progressFraction = fraction,
        )
    }

    /**
     * Deterministic featured plan selection rule:
     * 1. If an active session exists, prefer its source plan if still available.
     * 2. Otherwise select the most recently updated plan.
     */
    fun selectFeaturedPlan(
        plans: List<WorkoutPlan>,
        activeSession: WorkoutSession?,
    ): WorkoutPlan? {
        if (plans.isEmpty()) return null
        if (activeSession != null) {
            plans.firstOrNull { it.id == activeSession.sourcePlanId }?.let { return it }
        }
        return plans.maxByOrNull { it.updatedAtEpochMs }
    }

    fun selectLastFinishedSession(summaries: List<SessionSummary>): SessionSummary? =
        summaries
            .filter {
                it.status == SessionStatus.COMPLETED ||
                    it.status == SessionStatus.PARTIALLY_COMPLETED
            }
            .maxByOrNull { it.endedAtEpochMs ?: it.startedAtEpochMs }
}
