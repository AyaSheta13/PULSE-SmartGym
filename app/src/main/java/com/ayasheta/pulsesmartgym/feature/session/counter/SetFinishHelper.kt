package com.ayasheta.pulsesmartgym.feature.session.counter

import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionProgression
import com.ayasheta.pulsesmartgym.core.model.session.SessionSetTarget
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession

sealed interface SetSubmissionOutcome {
    data class Success(
        val sessionId: String,
        val hasRestStarted: Boolean,
        val nextTarget: SessionSetTarget?,
        val restErrorMessage: String?,
    ) : SetSubmissionOutcome

    data class Failure(val message: String) : SetSubmissionOutcome
    data object OwnerChanged : SetSubmissionOutcome
}

object SetFinishHelper {
    suspend fun submitSetAndStartRest(
        ownerId: String,
        request: RecordSetRequest,
        exerciseName: String,
        targetRestSeconds: Int?,
        fallbackSession: WorkoutSession?,
        workoutSessionRepository: WorkoutSessionRepository,
        currentOwnerProvider: () -> String?,
    ): SetSubmissionOutcome {
        return when (val result = workoutSessionRepository.recordPerformedSet(ownerId, request)) {
            is RecordSetResult.Recorded -> {
                if (currentOwnerProvider() != ownerId) {
                    return SetSubmissionOutcome.OwnerChanged
                }
                val refreshedSession = workoutSessionRepository.getSession(ownerId, request.sessionId)
                    ?: fallbackSession
                val nextTarget = refreshedSession?.let {
                    SessionProgression.findNextUncompletedSet(
                        session = it,
                        afterExerciseId = request.sessionExerciseId,
                        afterSetIndex = request.setIndex,
                    )
                }
                val restSeconds = targetRestSeconds ?: 0
                val shouldStartRest = restSeconds > 0 && nextTarget != null
                val (hasRestStarted, restErrorMessage) = if (shouldStartRest) {
                    when (
                        val restResult = workoutSessionRepository.startRestTimer(
                            ownerId = ownerId,
                            sessionId = request.sessionId,
                            exerciseId = request.sessionExerciseId,
                            exerciseName = exerciseName,
                            completedSetIndex = request.setIndex,
                            durationSeconds = restSeconds,
                            nextExerciseId = nextTarget?.exerciseId,
                            nextSetIndex = nextTarget?.setIndex,
                        )
                    ) {
                        is DomainResult.Success -> true to null
                        is DomainResult.Failure ->
                            false to "Set saved, but rest timer could not be started: ${restResult.error.userMessage()}"
                    }
                } else {
                    false to null
                }
                SetSubmissionOutcome.Success(
                    sessionId = request.sessionId,
                    hasRestStarted = hasRestStarted,
                    nextTarget = nextTarget,
                    restErrorMessage = restErrorMessage,
                )
            }
            is RecordSetResult.AlreadyRecorded ->
                SetSubmissionOutcome.Failure("Set ${result.setIndex} was already recorded.")
            is RecordSetResult.ValidationError ->
                SetSubmissionOutcome.Failure(result.message)
            is RecordSetResult.NotFound ->
                SetSubmissionOutcome.Failure(result.message)
            is RecordSetResult.InvalidState ->
                SetSubmissionOutcome.Failure(result.message)
        }
    }
}
