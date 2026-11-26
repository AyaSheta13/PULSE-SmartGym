package com.ayasheta.pulsesmartgym.feature.session.counter

import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionProgression
import com.ayasheta.pulsesmartgym.core.model.session.SessionSetTarget
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import kotlinx.coroutines.CancellationException

sealed interface SetSubmissionOutcome {
    data class Success(
        val sessionId: String,
        val hasRestStarted: Boolean,
        val nextTarget: SessionSetTarget?,
        val restErrorMessage: String?,
    ) : SetSubmissionOutcome

    data class AlreadyCompleted(val setIndex: Int) : SetSubmissionOutcome
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
        if (currentOwnerProvider() != ownerId) {
            return SetSubmissionOutcome.OwnerChanged
        }
        val recordResult = try {
            workoutSessionRepository.recordPerformedSet(ownerId, request)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            return SetSubmissionOutcome.Failure(e.message ?: "Failed to save set to local storage.")
        }

        return when (recordResult) {
            is RecordSetResult.Recorded -> {
                if (currentOwnerProvider() != ownerId) {
                    return SetSubmissionOutcome.OwnerChanged
                }
                val refreshedSession = try {
                    workoutSessionRepository.getSession(ownerId, request.sessionId) ?: fallbackSession
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Exception) {
                    fallbackSession
                }
                if (currentOwnerProvider() != ownerId) {
                    return SetSubmissionOutcome.OwnerChanged
                }
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
                    val restResult = try {
                        workoutSessionRepository.startRestTimer(
                            ownerId = ownerId,
                            sessionId = request.sessionId,
                            exerciseId = request.sessionExerciseId,
                            exerciseName = exerciseName,
                            completedSetIndex = request.setIndex,
                            durationSeconds = restSeconds,
                            nextExerciseId = nextTarget?.exerciseId,
                            nextSetIndex = nextTarget?.setIndex,
                        )
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (e: Exception) {
                        null
                    }
                    when (restResult) {
                        is DomainResult.Success -> true to null
                        is DomainResult.Failure ->
                            false to "Set saved, but rest timer could not be started: ${restResult.error.userMessage()}"
                        null ->
                            false to "Set saved, but rest timer could not be started."
                    }
                } else {
                    false to null
                }
                if (currentOwnerProvider() != ownerId) {
                    return SetSubmissionOutcome.OwnerChanged
                }
                SetSubmissionOutcome.Success(
                    sessionId = request.sessionId,
                    hasRestStarted = hasRestStarted,
                    nextTarget = nextTarget,
                    restErrorMessage = restErrorMessage,
                )
            }
            is RecordSetResult.AlreadyRecorded ->
                SetSubmissionOutcome.AlreadyCompleted(recordResult.setIndex)
            is RecordSetResult.ValidationError ->
                SetSubmissionOutcome.Failure(recordResult.message)
            is RecordSetResult.NotFound ->
                SetSubmissionOutcome.Failure(recordResult.message)
            is RecordSetResult.InvalidState ->
                SetSubmissionOutcome.Failure(recordResult.message)
        }
    }
}
