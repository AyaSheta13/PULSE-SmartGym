package com.ayasheta.pulsesmartgym.core.model.session

object SessionProgression {

    /**
     * Finds the next uncompleted set in plan order after ([afterExerciseId], [afterSetIndex]),
     * wrapping around to earlier exercises/sets if the user performed sets out of order.
     * Returns null if every planned set in [session] has already been recorded.
     */
    fun findNextUncompletedSet(
        session: WorkoutSession,
        afterExerciseId: String? = null,
        afterSetIndex: Int? = null,
    ): SessionSetTarget? {
        val orderedSets = session.exercises
            .sortedBy { it.orderIndex }
            .flatMap { exercise ->
                val performedIndices = exercise.performedSets.map { it.setIndex }.toSet()
                exercise.plannedSets
                    .sortedBy { it.setIndex }
                    .map { planned ->
                        Triple(
                            SessionSetTarget(
                                exerciseId = exercise.id,
                                exerciseName = exercise.exerciseNameSnapshot,
                                setIndex = planned.setIndex,
                            ),
                            planned.setIndex in performedIndices,
                            exercise.id == afterExerciseId && planned.setIndex == afterSetIndex,
                        )
                    }
            }

        if (orderedSets.isEmpty()) return null

        val anchorIdx = if (afterExerciseId != null && afterSetIndex != null) {
            orderedSets.indexOfFirst { it.third }
        } else {
            -1
        }

        if (anchorIdx >= 0) {
            for (i in (anchorIdx + 1) until orderedSets.size) {
                if (!orderedSets[i].second) return orderedSets[i].first
            }
            for (i in 0 until anchorIdx) {
                if (!orderedSets[i].second) return orderedSets[i].first
            }
            return null
        }

        return orderedSets.firstOrNull { !it.second }?.first
    }

    fun computeRemainingRestSeconds(deadlineEpochMs: Long, nowEpochMs: Long): Int {
        val remainingMs = deadlineEpochMs - nowEpochMs
        if (remainingMs <= 0L) return 0
        return ((remainingMs + 999L) / 1000L).toInt()
    }

    fun computeRestProgress(
        durationSeconds: Int,
        deadlineEpochMs: Long,
        nowEpochMs: Long,
    ): Float {
        if (durationSeconds <= 0) return 0f
        val remainingMs = (deadlineEpochMs - nowEpochMs).coerceAtLeast(0L)
        val totalMs = durationSeconds.toLong() * 1000L
        return (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    }

    fun formatMmSs(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val minutes = safe / 60L
        val seconds = safe % 60L
        return "%02d:%02d".format(minutes, seconds)
    }
}
