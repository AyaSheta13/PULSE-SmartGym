package com.ayasheta.pulsesmartgym.core.data.repository.session

import com.ayasheta.pulsesmartgym.core.data.local.database.PulseTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.ExerciseDao
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.WorkoutPlanDao
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.WorkoutSessionDao
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PerformedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.RepRecordEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionPlannedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionRestStateEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.WorkoutSessionEntity
import com.ayasheta.pulsesmartgym.core.data.mapper.toDomain
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.PulseValidators
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionRestState
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomWorkoutSessionRepository(
    private val sessionDao: WorkoutSessionDao,
    private val planDao: WorkoutPlanDao,
    private val exerciseDao: ExerciseDao,
    private val transactionRunner: PulseTransactionRunner,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) : WorkoutSessionRepository {

    override fun observeActiveSession(ownerId: String): Flow<WorkoutSession?> =
        sessionDao.observeActiveSession(ownerId).map { it?.toDomain() }

    override fun observeSession(ownerId: String, sessionId: String): Flow<WorkoutSession?> =
        sessionDao.observeSession(ownerId, sessionId).map { it?.toDomain() }

    override suspend fun getSession(ownerId: String, sessionId: String): WorkoutSession? =
        sessionDao.getSession(ownerId, sessionId)?.toDomain()

    override suspend fun startSessionFromPlan(
        ownerId: String,
        planId: String,
    ): DomainResult<WorkoutSession> {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }
        return transactionRunner.runInTransaction {
            val existingActive = sessionDao.getActiveSession(ownerId)
            if (existingActive != null) {
                return@runInTransaction if (existingActive.session.sourcePlanId == planId) {
                    DomainResult.Success(existingActive.toDomain())
                } else {
                    DomainResult.Failure(
                        DataError.ActiveSessionConflict(
                            activeSessionId = existingActive.session.id,
                            activePlanId = existingActive.session.sourcePlanId,
                            activePlanName = existingActive.session.planNameSnapshot,
                        ),
                    )
                }
            }

            val planWithExercises = planDao.getPlan(ownerId, planId)
                ?: return@runInTransaction DomainResult.Failure(DataError.NotFound("Workout plan '$planId' not found"))

            val sessionId = UUID.randomUUID().toString()
            val sessionEntity = WorkoutSessionEntity(
                id = sessionId,
                ownerProfileId = ownerId,
                sourcePlanId = planWithExercises.plan.id,
                planNameSnapshot = planWithExercises.plan.name,
                startedAtEpochMs = clockMs(),
                endedAtEpochMs = null,
                status = SessionStatus.ACTIVE,
            )

            val sessionExercises = mutableListOf<SessionExerciseEntity>()
            val sessionPlannedSets = mutableListOf<SessionPlannedSetEntity>()

            for (exWithSets in planWithExercises.exercises.sortedBy { it.planExercise.orderIndex }) {
                val catalogExercise = exerciseDao.getById(exWithSets.planExercise.exerciseId)
                    ?: return@runInTransaction DomainResult.Failure(
                        DataError.NotFound("Exercise '${exWithSets.planExercise.exerciseId}' not found in catalog"),
                    )
                val sessionExerciseId = UUID.randomUUID().toString()
                sessionExercises += SessionExerciseEntity(
                    id = sessionExerciseId,
                    sessionId = sessionId,
                    sourceExerciseId = catalogExercise.id,
                    exerciseNameSnapshot = catalogExercise.name,
                    muscleGroupSnapshot = catalogExercise.muscleGroup,
                    equipmentSnapshot = catalogExercise.equipment,
                    trackingCategorySnapshot = catalogExercise.trackingCategory,
                    orderIndex = exWithSets.planExercise.orderIndex,
                )
                exWithSets.plannedSets.sortedBy { it.setIndex }.forEach { planned ->
                    sessionPlannedSets += SessionPlannedSetEntity(
                        id = UUID.randomUUID().toString(),
                        sessionExerciseId = sessionExerciseId,
                        setIndex = planned.setIndex,
                        targetReps = planned.targetReps,
                        targetLoadGrams = planned.targetLoadGrams,
                        targetRestSeconds = planned.targetRestSeconds,
                    )
                }
            }

            sessionDao.insertSession(sessionEntity)
            if (sessionExercises.isNotEmpty()) sessionDao.insertSessionExercises(sessionExercises)
            if (sessionPlannedSets.isNotEmpty()) sessionDao.insertSessionPlannedSets(sessionPlannedSets)

            val created = sessionDao.getSession(ownerId, sessionId)?.toDomain()
                ?: return@runInTransaction DomainResult.Failure(DataError.StorageError("Failed to read started session"))
            DomainResult.Success(created)
        }
    }

    override suspend fun startSessionFromHistory(
        ownerId: String,
        historicalSessionId: String,
    ): DomainResult<WorkoutSession> {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }
        if (historicalSessionId.isBlank()) {
            return DomainResult.Failure(DataError.ValidationError("Historical session ID cannot be blank"))
        }
        return try {
            transactionRunner.runInTransaction {
                val existingActive = sessionDao.getActiveSession(ownerId)
                if (existingActive != null) {
                    return@runInTransaction DomainResult.Failure(
                        DataError.ActiveSessionConflict(
                            activeSessionId = existingActive.session.id,
                            activePlanId = existingActive.session.sourcePlanId,
                            activePlanName = existingActive.session.planNameSnapshot,
                        ),
                    )
                }

                val historical = sessionDao.getSession(ownerId, historicalSessionId)
                    ?: return@runInTransaction DomainResult.Failure(
                        DataError.NotFound("Historical workout session '$historicalSessionId' not found"),
                    )

                val status = historical.session.status
                if (status != SessionStatus.COMPLETED && status != SessionStatus.PARTIALLY_COMPLETED) {
                    return@runInTransaction DomainResult.Failure(
                        DataError.InvalidSessionState("Cannot repeat session in state $status"),
                    )
                }
                if (historical.session.planNameSnapshot.isBlank()) {
                    return@runInTransaction DomainResult.Failure(
                        DataError.ValidationError("Historical workout snapshot is missing a plan name"),
                    )
                }
                if (historical.exercises.isEmpty()) {
                    return@runInTransaction DomainResult.Failure(
                        DataError.ValidationError("Historical workout has no exercises to repeat"),
                    )
                }
                val hasInvalidSnapshot = historical.exercises.any { ex ->
                    ex.sessionExercise.exerciseNameSnapshot.isBlank() ||
                        ex.sessionExercise.orderIndex < 0 ||
                        ex.plannedSets.any { p ->
                            p.setIndex < 1 || p.targetReps < 1 || (p.targetRestSeconds != null && p.targetRestSeconds < 0)
                        }
                }
                if (hasInvalidSnapshot) {
                    return@runInTransaction DomainResult.Failure(
                        DataError.ValidationError("Historical workout snapshot contains invalid exercise or set data"),
                    )
                }

                val newSessionId = UUID.randomUUID().toString()
                val validSourcePlanId = historical.session.sourcePlanId?.takeIf { planId ->
                    planDao.getPlan(ownerId, planId) != null
                }
                val newSessionEntity = WorkoutSessionEntity(
                    id = newSessionId,
                    ownerProfileId = ownerId,
                    sourcePlanId = validSourcePlanId,
                    planNameSnapshot = historical.session.planNameSnapshot,
                    startedAtEpochMs = clockMs(),
                    endedAtEpochMs = null,
                    status = SessionStatus.ACTIVE,
                )

                val newExercises = mutableListOf<SessionExerciseEntity>()
                val newPlannedSets = mutableListOf<SessionPlannedSetEntity>()

                for (exWithSets in historical.exercises.sortedBy { it.sessionExercise.orderIndex }) {
                    val src = exWithSets.sessionExercise
                    val validSourceExerciseId = src.sourceExerciseId?.takeIf { exId ->
                        exerciseDao.getById(exId) != null
                    }
                    val newExerciseId = UUID.randomUUID().toString()
                    newExercises += SessionExerciseEntity(
                        id = newExerciseId,
                        sessionId = newSessionId,
                        sourceExerciseId = validSourceExerciseId,
                        exerciseNameSnapshot = src.exerciseNameSnapshot,
                        muscleGroupSnapshot = src.muscleGroupSnapshot,
                        equipmentSnapshot = src.equipmentSnapshot,
                        trackingCategorySnapshot = src.trackingCategorySnapshot,
                        orderIndex = src.orderIndex,
                    )
                    exWithSets.plannedSets.sortedBy { it.setIndex }.forEach { planned ->
                        newPlannedSets += SessionPlannedSetEntity(
                            id = UUID.randomUUID().toString(),
                            sessionExerciseId = newExerciseId,
                            setIndex = planned.setIndex,
                            targetReps = planned.targetReps,
                            targetLoadGrams = planned.targetLoadGrams,
                            targetRestSeconds = planned.targetRestSeconds,
                        )
                    }
                }

                sessionDao.insertSession(newSessionEntity)
                if (newExercises.isNotEmpty()) sessionDao.insertSessionExercises(newExercises)
                if (newPlannedSets.isNotEmpty()) sessionDao.insertSessionPlannedSets(newPlannedSets)

                val created = sessionDao.getSession(ownerId, newSessionId)?.toDomain()
                    ?: return@runInTransaction DomainResult.Failure(DataError.StorageError("Failed to read repeated session"))
                DomainResult.Success(created)
            }
        } catch (ce: kotlin.coroutines.cancellation.CancellationException) {
            throw ce
        } catch (t: Throwable) {
            DomainResult.Failure(DataError.StorageError(t.message ?: "Failed to repeat workout session"))
        }
    }

    override suspend fun recordPerformedSet(
        ownerId: String,
        request: RecordSetRequest,
    ): RecordSetResult {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return RecordSetResult.ValidationError(it)
        }
        PulseValidators.validateSetIndex(request.setIndex)?.let {
            return RecordSetResult.ValidationError(it)
        }
        PulseValidators.validatePerformedReps(request.fullReps, request.partialReps)?.let {
            return RecordSetResult.ValidationError(it)
        }

        return transactionRunner.runInTransaction {
            val session = sessionDao.getSession(ownerId, request.sessionId)
                ?: return@runInTransaction RecordSetResult.NotFound("Session '${request.sessionId}' not found")

            if (session.session.status != SessionStatus.ACTIVE) {
                return@runInTransaction RecordSetResult.InvalidState(
                    "Cannot record set on session in state ${session.session.status}",
                )
            }

            val exercise = session.exercises.firstOrNull { it.sessionExercise.id == request.sessionExerciseId }
                ?: return@runInTransaction RecordSetResult.NotFound(
                    "Session exercise '${request.sessionExerciseId}' not found in session",
                )

            val hasPlannedSet = exercise.plannedSets.any { it.setIndex == request.setIndex }
            if (!hasPlannedSet) {
                return@runInTransaction RecordSetResult.NotFound(
                    "Set index ${request.setIndex} does not exist in snapshot for '${request.sessionExerciseId}'",
                )
            }

            val performedSetId = UUID.randomUUID().toString()
            val setEntity = PerformedSetEntity(
                id = performedSetId,
                sessionId = request.sessionId,
                sessionExerciseId = request.sessionExerciseId,
                setIndex = request.setIndex,
                fullReps = request.fullReps,
                partialReps = request.partialReps,
                actualLoadGrams = request.actualLoad?.grams,
                completedAtEpochMs = clockMs(),
                trackingMode = request.trackingMode,
                origin = request.origin,
                durationMs = request.durationMs,
            )
            val repEntities = request.repRecords.map { rep ->
                RepRecordEntity(
                    id = UUID.randomUUID().toString(),
                    performedSetId = performedSetId,
                    repIndex = rep.repIndex,
                    classification = rep.classification,
                    totalDurationMs = rep.totalDurationMs,
                    concentricDurationMs = rep.concentricDurationMs,
                    eccentricDurationMs = rep.eccentricDurationMs,
                    angularRomDegrees = rep.angularRomDegrees,
                    linearRomCm = rep.linearRomCm,
                    origin = rep.origin,
                )
            }

            val rowId = sessionDao.insertPerformedSetIgnore(setEntity)
            if (rowId == -1L) {
                return@runInTransaction RecordSetResult.AlreadyRecorded(
                    sessionId = request.sessionId,
                    sessionExerciseId = request.sessionExerciseId,
                    setIndex = request.setIndex,
                )
            }
            if (repEntities.isNotEmpty()) {
                sessionDao.insertRepRecords(repEntities)
            }
            RecordSetResult.Recorded(performedSetId)
        }
    }

    override suspend fun correctPerformedSet(
        ownerId: String,
        request: RecordSetRequest,
    ): DomainResult<PerformedSet> {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }
        PulseValidators.validateSetIndex(request.setIndex)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }
        PulseValidators.validatePerformedReps(request.fullReps, request.partialReps)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }

        return transactionRunner.runInTransaction {
            val session = sessionDao.getSession(ownerId, request.sessionId)
                ?: return@runInTransaction DomainResult.Failure(DataError.NotFound("Session '${request.sessionId}' not found"))
            if (session.session.status != SessionStatus.ACTIVE) {
                return@runInTransaction DomainResult.Failure(
                    DataError.InvalidSessionState("Cannot correct set on session in state ${session.session.status}"),
                )
            }

            val existing = sessionDao.findPerformedSet(
                sessionId = request.sessionId,
                sessionExerciseId = request.sessionExerciseId,
                setIndex = request.setIndex,
            ) ?: return@runInTransaction DomainResult.Failure(
                DataError.NotFound("No recorded set found for (${request.sessionId}, ${request.sessionExerciseId}, ${request.setIndex})"),
            )

            val updatedSet = existing.copy(
                fullReps = request.fullReps,
                partialReps = request.partialReps,
                actualLoadGrams = request.actualLoad?.grams,
                completedAtEpochMs = existing.completedAtEpochMs,
                trackingMode = request.trackingMode,
                origin = request.origin,
                durationMs = request.durationMs ?: existing.durationMs,
            )
            sessionDao.updatePerformedSet(updatedSet)
            val resultRepRecords = if (request.repRecords.isNotEmpty()) {
                val repEntities = request.repRecords.map { rep ->
                    RepRecordEntity(
                        id = UUID.randomUUID().toString(),
                        performedSetId = existing.id,
                        repIndex = rep.repIndex,
                        classification = rep.classification,
                        totalDurationMs = rep.totalDurationMs,
                        concentricDurationMs = rep.concentricDurationMs,
                        eccentricDurationMs = rep.eccentricDurationMs,
                        angularRomDegrees = rep.angularRomDegrees,
                        linearRomCm = rep.linearRomCm,
                        origin = rep.origin,
                    )
                }
                sessionDao.deleteRepRecordsForSet(updatedSet.id)
                sessionDao.insertRepRecords(repEntities)
                request.repRecords
            } else {
                emptyList()
            }
            DomainResult.Success(updatedSet.toDomain(resultRepRecords))
        }
    }

    override suspend fun completeSession(ownerId: String, sessionId: String): DomainResult<WorkoutSession> {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }
        return transactionRunner.runInTransaction {
            val session = sessionDao.getSession(ownerId, sessionId)
                ?: return@runInTransaction DomainResult.Failure(DataError.NotFound("Session '$sessionId' not found"))

            if (session.session.status != SessionStatus.ACTIVE) {
                return@runInTransaction DomainResult.Failure(
                    DataError.InvalidSessionState("Session '$sessionId' is already ${session.session.status}"),
                )
            }

            val plannedCount = session.exercises.sumOf { it.plannedSets.size }
            val performedCount = session.exercises.sumOf { it.performedSets.size }
            if (performedCount < plannedCount) {
                return@runInTransaction DomainResult.Failure(
                    DataError.InvalidSessionState(
                        "Cannot mark session COMPLETED with $performedCount/$plannedCount sets done; use endSessionEarly instead.",
                    ),
                )
            }

            val updatedRows = sessionDao.updateSessionStatusIfActive(
                ownerId = ownerId,
                sessionId = sessionId,
                status = SessionStatus.COMPLETED,
                endedAtEpochMs = clockMs(),
            )
            if (updatedRows == 0) {
                return@runInTransaction DomainResult.Failure(
                    DataError.InvalidSessionState("Session '$sessionId' is no longer ACTIVE"),
                )
            }
            sessionDao.deleteRestState(sessionId)
            val completed = sessionDao.getSession(ownerId, sessionId)?.toDomain()
                ?: return@runInTransaction DomainResult.Failure(DataError.StorageError("Failed to load completed session"))
            DomainResult.Success(completed)
        }
    }

    override suspend fun endSessionEarly(ownerId: String, sessionId: String): DomainResult<WorkoutSession> {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }
        return transactionRunner.runInTransaction {
            val session = sessionDao.getSession(ownerId, sessionId)
                ?: return@runInTransaction DomainResult.Failure(DataError.NotFound("Session '$sessionId' not found"))

            if (session.session.status != SessionStatus.ACTIVE) {
                return@runInTransaction DomainResult.Failure(
                    DataError.InvalidSessionState("Session '$sessionId' is already ${session.session.status}"),
                )
            }

            val performedCount = session.exercises.sumOf { it.performedSets.size }
            val targetStatus = if (performedCount > 0) {
                SessionStatus.PARTIALLY_COMPLETED
            } else {
                SessionStatus.ABANDONED
            }

            val updatedRows = sessionDao.updateSessionStatusIfActive(
                ownerId = ownerId,
                sessionId = sessionId,
                status = targetStatus,
                endedAtEpochMs = clockMs(),
            )
            if (updatedRows == 0) {
                return@runInTransaction DomainResult.Failure(
                    DataError.InvalidSessionState("Session '$sessionId' is no longer ACTIVE"),
                )
            }
            sessionDao.deleteRestState(sessionId)
            val updated = sessionDao.getSession(ownerId, sessionId)?.toDomain()
                ?: return@runInTransaction DomainResult.Failure(DataError.StorageError("Failed to load ended session"))
            DomainResult.Success(updated)
        }
    }

    override fun observeRestState(ownerId: String, sessionId: String): Flow<SessionRestState?> =
        sessionDao.observeRestState(ownerId, sessionId).map { it?.toDomain() }

    override suspend fun startRestTimer(
        ownerId: String,
        sessionId: String,
        exerciseId: String,
        exerciseName: String,
        completedSetIndex: Int,
        durationSeconds: Int,
        nextExerciseId: String?,
        nextSetIndex: Int?,
    ): DomainResult<SessionRestState> {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }
        if (durationSeconds <= 0) {
            return DomainResult.Failure(DataError.ValidationError("Rest duration must be positive"))
        }
        return transactionRunner.runInTransaction {
            val session = sessionDao.getSession(ownerId, sessionId)
                ?: return@runInTransaction DomainResult.Failure(DataError.NotFound("Session '$sessionId' not found"))
            if (session.session.status != SessionStatus.ACTIVE) {
                return@runInTransaction DomainResult.Failure(
                    DataError.InvalidSessionState("Session '$sessionId' is not ACTIVE"),
                )
            }
            val deadlineEpochMs = clockMs() + durationSeconds.toLong() * 1000L
            val entity = SessionRestStateEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                completedSetIndex = completedSetIndex,
                nextExerciseId = nextExerciseId,
                nextSetIndex = nextSetIndex,
                durationSeconds = durationSeconds,
                deadlineEpochMs = deadlineEpochMs,
            )
            sessionDao.upsertRestState(entity)
            DomainResult.Success(entity.toDomain())
        }
    }

    override suspend fun extendRestTimer(
        ownerId: String,
        sessionId: String,
        additionalSeconds: Int,
    ): DomainResult<SessionRestState> {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }
        if (additionalSeconds <= 0) {
            return DomainResult.Failure(DataError.ValidationError("Extension seconds must be positive"))
        }
        return transactionRunner.runInTransaction {
            val existing = sessionDao.getRestState(ownerId, sessionId)
                ?: return@runInTransaction DomainResult.Failure(DataError.NotFound("No active rest timer for session"))
            val nowMs = clockMs()
            val baseMs = maxOf(existing.deadlineEpochMs, nowMs)
            val newDeadlineMs = baseMs + additionalSeconds.toLong() * 1000L
            val newRemainingSec = ((newDeadlineMs - nowMs + 999L) / 1000L).toInt().coerceAtLeast(1)
            val updated = existing.copy(
                durationSeconds = maxOf(existing.durationSeconds + additionalSeconds, newRemainingSec),
                deadlineEpochMs = newDeadlineMs,
            )
            sessionDao.upsertRestState(updated)
            DomainResult.Success(updated.toDomain())
        }
    }

    override suspend fun clearRestTimer(ownerId: String, sessionId: String): DomainResult<Unit> {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }
        return transactionRunner.runInTransaction {
            val session = sessionDao.getSession(ownerId, sessionId)
                ?: return@runInTransaction DomainResult.Failure(DataError.NotFound("Session '$sessionId' not found"))
            sessionDao.deleteRestState(session.session.id)
            DomainResult.Success(Unit)
        }
    }
}
