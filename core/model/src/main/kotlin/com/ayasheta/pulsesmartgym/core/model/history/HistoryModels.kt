package com.ayasheta.pulsesmartgym.core.model.history

import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession

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
}
