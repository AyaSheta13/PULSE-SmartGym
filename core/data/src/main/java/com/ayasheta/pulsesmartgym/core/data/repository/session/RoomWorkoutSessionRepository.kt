package com.ayasheta.pulsesmartgym.core.data.repository.session

import com.ayasheta.pulsesmartgym.core.data.local.database.dao.ExerciseDao
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.WorkoutPlanDao
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.WorkoutSessionDao
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PerformedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.RepRecordEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionPlannedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.WorkoutSessionEntity
import com.ayasheta.pulsesmartgym.core.data.mapper.toDomain
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.PulseValidators
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomWorkoutSessionRepository(
    private val sessionDao: WorkoutSessionDao,
    private val planDao: WorkoutPlanDao,
    private val exerciseDao: ExerciseDao,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) : WorkoutSessionRepository {

    override fun observeActiveSession(): Flow<WorkoutSession?> =
        sessionDao.observeActiveSession().map { it?.toDomain() }

    override fun observeSession(sessionId: String): Flow<WorkoutSession?> =
        sessionDao.observeSession(sessionId).map { it?.toDomain() }

    override suspend fun getSession(sessionId: String): WorkoutSession? =
        sessionDao.getSession(sessionId)?.toDomain()

    override suspend fun startSessionFromPlan(planId: String): DomainResult<WorkoutSession> {
        val planWithExercises = planDao.getPlan(planId)
            ?: return DomainResult.Failure(DataError.NotFound("Workout plan '$planId' not found"))

        val sessionId = UUID.randomUUID().toString()
        val sessionEntity = WorkoutSessionEntity(
            id = sessionId,
            ownerProfileId = planWithExercises.plan.ownerProfileId,
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
                ?: return DomainResult.Failure(
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

        sessionDao.startSessionGraph(sessionEntity, sessionExercises, sessionPlannedSets)
        val created = sessionDao.getSession(sessionId)?.toDomain()
            ?: return DomainResult.Failure(DataError.StorageError("Failed to read started session"))
        return DomainResult.Success(created)
    }

    override suspend fun recordPerformedSet(request: RecordSetRequest): RecordSetResult {
        PulseValidators.validateSetIndex(request.setIndex)?.let {
            return RecordSetResult.ValidationError(it)
        }
        PulseValidators.validatePerformedReps(request.fullReps, request.partialReps)?.let {
            return RecordSetResult.ValidationError(it)
        }

        val session = sessionDao.getSession(request.sessionId)
            ?: return RecordSetResult.NotFound("Session '${request.sessionId}' not found")

        if (session.session.status != SessionStatus.ACTIVE) {
            return RecordSetResult.InvalidState("Cannot record set on session in state ${session.session.status}")
        }

        val exercise = session.exercises.firstOrNull { it.sessionExercise.id == request.sessionExerciseId }
            ?: return RecordSetResult.NotFound("Session exercise '${request.sessionExerciseId}' not found in session")

        val hasPlannedSet = exercise.plannedSets.any { it.setIndex == request.setIndex }
        if (!hasPlannedSet) {
            return RecordSetResult.NotFound(
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

        val inserted = sessionDao.insertPerformedSetWithRepsIfAbsent(setEntity, repEntities)
        return if (inserted) {
            RecordSetResult.Recorded(performedSetId)
        } else {
            RecordSetResult.AlreadyRecorded(
                sessionId = request.sessionId,
                sessionExerciseId = request.sessionExerciseId,
                setIndex = request.setIndex,
            )
        }
    }

    override suspend fun correctPerformedSet(request: RecordSetRequest): DomainResult<PerformedSet> {
        PulseValidators.validateSetIndex(request.setIndex)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }
        PulseValidators.validatePerformedReps(request.fullReps, request.partialReps)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }

        val session = sessionDao.getSession(request.sessionId)
            ?: return DomainResult.Failure(DataError.NotFound("Session '${request.sessionId}' not found"))
        if (session.session.status != SessionStatus.ACTIVE) {
            return DomainResult.Failure(
                DataError.InvalidSessionState("Cannot correct set on session in state ${session.session.status}"),
            )
        }

        val existing = sessionDao.findPerformedSet(
            sessionId = request.sessionId,
            sessionExerciseId = request.sessionExerciseId,
            setIndex = request.setIndex,
        ) ?: return DomainResult.Failure(
            DataError.NotFound("No recorded set found for (${request.sessionId}, ${request.sessionExerciseId}, ${request.setIndex})"),
        )

        val updatedSet = existing.copy(
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

        sessionDao.replacePerformedSetCorrection(updatedSet, repEntities)
        return DomainResult.Success(updatedSet.toDomain(request.repRecords))
    }

    override suspend fun completeSession(sessionId: String): DomainResult<WorkoutSession> {
        val session = sessionDao.getSession(sessionId)
            ?: return DomainResult.Failure(DataError.NotFound("Session '$sessionId' not found"))

        if (session.session.status != SessionStatus.ACTIVE) {
            return DomainResult.Failure(
                DataError.InvalidSessionState("Session '$sessionId' is already ${session.session.status}"),
            )
        }

        val plannedCount = session.exercises.sumOf { it.plannedSets.size }
        val performedCount = session.exercises.sumOf { it.performedSets.size }
        if (performedCount < plannedCount) {
            return DomainResult.Failure(
                DataError.InvalidSessionState(
                    "Cannot mark session COMPLETED with $performedCount/$plannedCount sets done; use endSessionEarly instead.",
                ),
            )
        }

        sessionDao.updateSessionStatus(
            sessionId = sessionId,
            status = SessionStatus.COMPLETED,
            endedAtEpochMs = clockMs(),
        )
        val completed = sessionDao.getSession(sessionId)?.toDomain()
            ?: return DomainResult.Failure(DataError.StorageError("Failed to load completed session"))
        return DomainResult.Success(completed)
    }

    override suspend fun endSessionEarly(sessionId: String): DomainResult<WorkoutSession> {
        val session = sessionDao.getSession(sessionId)
            ?: return DomainResult.Failure(DataError.NotFound("Session '$sessionId' not found"))

        if (session.session.status != SessionStatus.ACTIVE) {
            return DomainResult.Failure(
                DataError.InvalidSessionState("Session '$sessionId' is already ${session.session.status}"),
            )
        }

        val performedCount = session.exercises.sumOf { it.performedSets.size }
        val targetStatus = if (performedCount > 0) {
            SessionStatus.PARTIALLY_COMPLETED
        } else {
            SessionStatus.ABANDONED
        }

        sessionDao.updateSessionStatus(
            sessionId = sessionId,
            status = targetStatus,
            endedAtEpochMs = clockMs(),
        )
        val updated = sessionDao.getSession(sessionId)?.toDomain()
            ?: return DomainResult.Failure(DataError.StorageError("Failed to load ended session"))
        return DomainResult.Success(updated)
    }
}
