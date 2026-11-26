package com.ayasheta.pulsesmartgym.core.model.history

import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.SessionExercise
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
    val leadExerciseSourceId: String? = null,
    val leadExerciseName: String? = null,
    val leadMuscleGroup: MuscleGroup? = null,
    val measuredSetsCount: Int = 0,
)

data class HistoryAggregateMetrics(
    val totalSessions: Int,
    val completedSessions: Int,
    val partialSessions: Int,
    val totalFullReps: Int,
    val totalPartialReps: Int,
    val totalVolumeKg: Double,
    val totalDurationMs: Long = 0L,
    val totalCompletedSets: Int = 0,
)

enum class VolumeCoverage {
    NO_MEASURED_VOLUME,
    KNOWN_ZERO_VOLUME,
    PARTIALLY_MEASURED,
    FULLY_MEASURED,
}

data class PeriodProgressMetrics(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val completedWorkoutsCount: Int,
    val partialWorkoutsCount: Int,
    val completedWorkoutsDelta: Int?,
    val weeklyTarget: Int?,
    val targetCompletionPercent: Int?,
    val totalVolumeKg: Double,
    val previousWeekVolumeKg: Double,
    val volumeChangePercent: Int?,
    val volumeCoverage: VolumeCoverage,
    val setsLoggedCount: Int,
    val setsLoggedDelta: Int?,
    val avgDurationMinutes: Int?,
    val paceMinutesPerSet: Double?,
    val dailyVolumeKg: List<Double>,
    val dailyMeasuredFlags: List<Boolean>,
    val highlightDayIndex: Int?,
    val previousWeekVolumeCoverage: VolumeCoverage = VolumeCoverage.NO_MEASURED_VOLUME,
    val previousWeekSetsLoggedCount: Int = 0,
)

data class ExercisePersonalRecord(
    val exerciseKey: String,
    val exerciseName: String,
    val achievedAtEpochMs: Long,
    val maxLoad: WeightLoad,
    val repsAtMaxLoad: Int,
    val previousBestLoad: WeightLoad?,
    val improvementKg: Double?,
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
        val orderedExercises = session.exercises.sortedBy { it.orderIndex }
        val leadExercise = orderedExercises.firstOrNull { it.performedSets.isNotEmpty() }
            ?: orderedExercises.firstOrNull()
        val allPerformed = orderedExercises.flatMap { it.performedSets }
        val totalPlanned = orderedExercises.sumOf { it.plannedSets.size }
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
            leadExerciseSourceId = leadExercise?.sourceExerciseId,
            leadExerciseName = leadExercise?.exerciseNameSnapshot,
            leadMuscleGroup = leadExercise?.muscleGroupSnapshot,
            measuredSetsCount = allPerformed.count { it.actualLoad != null },
        )
    }

    fun aggregateSummaries(summaries: List<SessionSummary>): HistoryAggregateMetrics {
        val finished = summaries.filter {
            it.status == SessionStatus.COMPLETED || it.status == SessionStatus.PARTIALLY_COMPLETED
        }
        return HistoryAggregateMetrics(
            totalSessions = finished.size,
            completedSessions = finished.count { it.status == SessionStatus.COMPLETED },
            partialSessions = finished.count { it.status == SessionStatus.PARTIALLY_COMPLETED },
            totalFullReps = finished.sumOf { it.totalFullReps },
            totalPartialReps = finished.sumOf { it.totalPartialReps },
            totalVolumeKg = finished.sumOf { it.totalVolumeKg },
            totalDurationMs = finished.sumOf { it.durationMs ?: 0L },
            totalCompletedSets = finished.sumOf { it.completedSetsCount },
        )
    }

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

    fun calculateVolumeCoverage(performedSets: List<PerformedSet>): VolumeCoverage {
        if (performedSets.isEmpty()) return VolumeCoverage.NO_MEASURED_VOLUME
        val measured = performedSets.filter { it.actualLoad != null }
        if (measured.isEmpty()) return VolumeCoverage.NO_MEASURED_VOLUME
        val volumeKg = calculateVolumeKg(measured)
        if (measured.size == performedSets.size) {
            return if (volumeKg == 0.0) VolumeCoverage.KNOWN_ZERO_VOLUME else VolumeCoverage.FULLY_MEASURED
        }
        return VolumeCoverage.PARTIALLY_MEASURED
    }

    fun calculatePeriodMetrics(
        sessions: List<WorkoutSession>,
        weeklyTarget: Int?,
        referenceDate: LocalDate = LocalDate.now(),
        currentToday: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): PeriodProgressMetrics {
        val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val prevWeekStart = weekStart.minusWeeks(1)
        val prevWeekEnd = weekStart.minusDays(1)

        val finishedSessions = sessions.filter {
            it.status == SessionStatus.COMPLETED || it.status == SessionStatus.PARTIALLY_COMPLETED
        }

        val currentWeekSessions = mutableListOf<WorkoutSession>()
        val prevWeekSessions = mutableListOf<WorkoutSession>()
        for (session in finishedSessions) {
            val localDate = Instant.ofEpochMilli(session.startedAtEpochMs).atZone(zoneId).toLocalDate()
            when {
                !localDate.isBefore(weekStart) && !localDate.isAfter(weekEnd) -> currentWeekSessions += session
                !localDate.isBefore(prevWeekStart) && !localDate.isAfter(prevWeekEnd) -> prevWeekSessions += session
            }
        }

        val completedCount = currentWeekSessions.count { it.status == SessionStatus.COMPLETED }
        val partialCount = currentWeekSessions.count { it.status == SessionStatus.PARTIALLY_COMPLETED }
        val prevCompletedCount = prevWeekSessions.count { it.status == SessionStatus.COMPLETED }
        val completedDelta = if (prevWeekSessions.isNotEmpty() || completedCount > 0) {
            completedCount - prevCompletedCount
        } else {
            null
        }

        val targetCompletionPercent = if (weeklyTarget != null && weeklyTarget > 0) {
            ((completedCount.toDouble() / weeklyTarget.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else {
            null
        }

        val dailyVolumes = DoubleArray(7) { 0.0 }
        val dailyMeasured = BooleanArray(7) { false }
        val allCurrentPerformed = mutableListOf<PerformedSet>()
        for (session in currentWeekSessions) {
            val localDate = Instant.ofEpochMilli(session.startedAtEpochMs).atZone(zoneId).toLocalDate()
            val dayIdx = (localDate.dayOfWeek.value - 1).coerceIn(0, 6)
            val performed = session.exercises.flatMap { it.performedSets }
            allCurrentPerformed += performed
            val sessionVol = calculateVolumeKg(performed)
            dailyVolumes[dayIdx] += sessionVol
            if (performed.any { it.actualLoad != null }) {
                dailyMeasured[dayIdx] = true
            }
        }

        val currentVolumeKg = dailyVolumes.sum()
        val currentCoverage = calculateVolumeCoverage(allCurrentPerformed)
        val prevAllPerformed = prevWeekSessions.flatMap { s -> s.exercises.flatMap { it.performedSets } }
        val prevVolumeKg = calculateVolumeKg(prevAllPerformed)
        val prevCoverage = calculateVolumeCoverage(prevAllPerformed)
        val isCurrentCompleteForComparison = currentCoverage == VolumeCoverage.FULLY_MEASURED ||
            currentCoverage == VolumeCoverage.KNOWN_ZERO_VOLUME ||
            (currentCoverage == VolumeCoverage.NO_MEASURED_VOLUME && allCurrentPerformed.isEmpty())
        val isPrevCompleteForComparison = prevCoverage == VolumeCoverage.FULLY_MEASURED
        val volumeChangePercent = if (
            prevVolumeKg > 0.0 &&
            isCurrentCompleteForComparison &&
            isPrevCompleteForComparison
        ) {
            kotlin.math.round(((currentVolumeKg - prevVolumeKg) / prevVolumeKg) * 100.0).toInt()
        } else {
            null
        }

        val setsLogged = allCurrentPerformed.size
        val prevSetsLogged = prevAllPerformed.size
        val setsDelta = if (prevWeekSessions.isNotEmpty() || setsLogged > 0) {
            setsLogged - prevSetsLogged
        } else {
            null
        }

        val durationsWithSets = currentWeekSessions.mapNotNull { session ->
            val end = session.endedAtEpochMs ?: return@mapNotNull null
            val durMs = (end - session.startedAtEpochMs).coerceAtLeast(0L)
            val setCount = session.exercises.sumOf { it.performedSets.size }
            durMs to setCount
        }
        val avgDurationMinutes = if (durationsWithSets.isNotEmpty()) {
            val totalMs = durationsWithSets.sumOf { it.first }
            kotlin.math.round((totalMs.toDouble() / durationsWithSets.size) / 60_000.0).toInt()
        } else {
            null
        }
        val totalDurationSets = durationsWithSets.sumOf { it.second }
        val paceMinutesPerSet = if (durationsWithSets.isNotEmpty() && totalDurationSets > 0) {
            val totalMs = durationsWithSets.sumOf { it.first }
            (totalMs.toDouble() / 60_000.0) / totalDurationSets.toDouble()
        } else {
            null
        }

        val highlightDayIndex = if (!currentToday.isBefore(weekStart) && !currentToday.isAfter(weekEnd)) {
            (currentToday.dayOfWeek.value - 1).coerceIn(0, 6)
        } else {
            null
        }

        return PeriodProgressMetrics(
            weekStart = weekStart,
            weekEnd = weekEnd,
            completedWorkoutsCount = completedCount,
            partialWorkoutsCount = partialCount,
            completedWorkoutsDelta = completedDelta,
            weeklyTarget = weeklyTarget,
            targetCompletionPercent = targetCompletionPercent,
            totalVolumeKg = currentVolumeKg,
            previousWeekVolumeKg = prevVolumeKg,
            volumeChangePercent = volumeChangePercent,
            volumeCoverage = currentCoverage,
            setsLoggedCount = setsLogged,
            setsLoggedDelta = setsDelta,
            avgDurationMinutes = avgDurationMinutes,
            paceMinutesPerSet = paceMinutesPerSet,
            dailyVolumeKg = dailyVolumes.toList(),
            dailyMeasuredFlags = dailyMeasured.toList(),
            highlightDayIndex = highlightDayIndex,
            previousWeekVolumeCoverage = prevCoverage,
            previousWeekSetsLoggedCount = prevSetsLogged,
        )
    }

    private data class PrCandidate(
        val exerciseKey: String,
        val exerciseName: String,
        val achievedAtEpochMs: Long,
        val load: WeightLoad,
        val fullReps: Int,
        val performedSetId: String,
    )

    /**
     * Weight PR policy:
     * - Eligible sessions: COMPLETED or PARTIALLY_COMPLETED only.
     * - Eligible sets: fullReps >= 1 and actualLoad != null with grams > 0.
     * - Identity: stable sourceExerciseId, falling back to normalized name + muscle + equipment.
     * - Tie-breaker: higher load grams -> higher full reps -> most recent timestamp -> performedSetId.
     * - Improvement: difference against highest eligible load achieved strictly before the PR timestamp.
     */
    fun calculatePersonalRecords(
        sessions: List<WorkoutSession>,
        limit: Int = 3,
    ): List<ExercisePersonalRecord> {
        val finished = sessions.filter {
            it.status == SessionStatus.COMPLETED || it.status == SessionStatus.PARTIALLY_COMPLETED
        }
        val candidatesByKey = mutableMapOf<String, MutableList<PrCandidate>>()

        for (session in finished) {
            for (exercise in session.exercises) {
                val key = stableExerciseKey(exercise)
                for (set in exercise.performedSets) {
                    val load = set.actualLoad ?: continue
                    if (set.fullReps < 1 || load.grams <= 0L) continue
                    candidatesByKey.getOrPut(key) { mutableListOf() }.add(
                        PrCandidate(
                            exerciseKey = key,
                            exerciseName = exercise.exerciseNameSnapshot,
                            achievedAtEpochMs = set.completedAtEpochMs,
                            load = load,
                            fullReps = set.fullReps,
                            performedSetId = set.id,
                        ),
                    )
                }
            }
        }

        val comparator = compareByDescending<PrCandidate> { it.load.grams }
            .thenByDescending { it.fullReps }
            .thenByDescending { it.achievedAtEpochMs }
            .thenBy { it.performedSetId }

        val records = candidatesByKey.values.mapNotNull { list ->
            val best = list.minWithOrNull(comparator) ?: return@mapNotNull null
            val previousBest = list
                .filter { it.achievedAtEpochMs < best.achievedAtEpochMs }
                .maxByOrNull { it.load.grams }
                ?.load
            val improvementKg = when {
                previousBest == null -> null
                best.load.grams > previousBest.grams -> (best.load.grams - previousBest.grams) / 1000.0
                else -> 0.0
            }
            ExercisePersonalRecord(
                exerciseKey = best.exerciseKey,
                exerciseName = best.exerciseName,
                achievedAtEpochMs = best.achievedAtEpochMs,
                maxLoad = best.load,
                repsAtMaxLoad = best.fullReps,
                previousBestLoad = previousBest,
                improvementKg = improvementKg,
            )
        }

        return records
            .sortedWith(
                compareByDescending<ExercisePersonalRecord> { it.maxLoad.grams }
                    .thenByDescending { it.achievedAtEpochMs }
                    .thenBy { it.exerciseName },
            )
            .take(limit)
    }

    private fun stableExerciseKey(exercise: SessionExercise): String =
        exercise.sourceExerciseId
            ?: "snapshot:${exercise.exerciseNameSnapshot.trim().lowercase()}:${exercise.muscleGroupSnapshot.name}:${exercise.equipmentSnapshot.name}"
}
