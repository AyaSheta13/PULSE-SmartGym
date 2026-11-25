package com.ayasheta.pulsesmartgym.core.data.repository.workout

import com.ayasheta.pulsesmartgym.core.data.local.database.PulseTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.ExerciseDao
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.WorkoutPlanDao
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PlanExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PlannedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.WorkoutPlanEntity
import com.ayasheta.pulsesmartgym.core.data.mapper.toDomain
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.PulseValidators
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlan
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomWorkoutPlanRepository(
    private val workoutPlanDao: WorkoutPlanDao,
    private val exerciseDao: ExerciseDao,
    private val transactionRunner: PulseTransactionRunner,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) : WorkoutPlanRepository {

    override fun observePlans(ownerId: String): Flow<List<WorkoutPlan>> =
        workoutPlanDao.observeAllPlans(ownerId).map { list -> list.map { it.toDomain() } }

    override fun observePlan(ownerId: String, planId: String): Flow<WorkoutPlan?> =
        workoutPlanDao.observePlan(ownerId, planId).map { it?.toDomain() }

    override suspend fun getPlan(ownerId: String, planId: String): WorkoutPlan? =
        workoutPlanDao.getPlan(ownerId, planId)?.toDomain()

    override suspend fun createPlan(ownerId: String, draft: WorkoutPlanDraft): DomainResult<WorkoutPlan> {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }
        return transactionRunner.runInTransaction {
            validateDraft(draft)?.let { return@runInTransaction DomainResult.Failure(it) }

            val now = clockMs()
            val planId = UUID.randomUUID().toString()
            val planEntity = WorkoutPlanEntity(
                id = planId,
                ownerProfileId = ownerId,
                name = draft.name.trim(),
                description = draft.description?.trim(),
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            )
            val (exerciseEntities, setEntities) = buildChildEntities(planId, draft)
            workoutPlanDao.insertPlan(planEntity)
            if (exerciseEntities.isNotEmpty()) workoutPlanDao.insertPlanExercises(exerciseEntities)
            if (setEntities.isNotEmpty()) workoutPlanDao.insertPlannedSets(setEntities)

            val created = workoutPlanDao.getPlan(ownerId, planId)?.toDomain()
                ?: return@runInTransaction DomainResult.Failure(DataError.StorageError("Failed to read created workout plan"))
            DomainResult.Success(created)
        }
    }

    override suspend fun updatePlan(
        ownerId: String,
        planId: String,
        draft: WorkoutPlanDraft,
    ): DomainResult<WorkoutPlan> {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }
        return transactionRunner.runInTransaction {
            val existing = workoutPlanDao.getPlan(ownerId, planId)
                ?: return@runInTransaction DomainResult.Failure(DataError.NotFound("Workout plan '$planId' not found"))
            validateDraft(draft)?.let { return@runInTransaction DomainResult.Failure(it) }

            val updatedEntity = existing.plan.copy(
                name = draft.name.trim(),
                description = draft.description?.trim(),
                updatedAtEpochMs = clockMs(),
            )
            val (exerciseEntities, setEntities) = buildChildEntities(planId, draft)
            workoutPlanDao.updatePlan(updatedEntity)
            workoutPlanDao.deletePlanExercisesByPlanId(planId)
            if (exerciseEntities.isNotEmpty()) workoutPlanDao.insertPlanExercises(exerciseEntities)
            if (setEntities.isNotEmpty()) workoutPlanDao.insertPlannedSets(setEntities)

            val updated = workoutPlanDao.getPlan(ownerId, planId)?.toDomain()
                ?: return@runInTransaction DomainResult.Failure(DataError.StorageError("Failed to read updated workout plan"))
            DomainResult.Success(updated)
        }
    }

    override suspend fun deletePlan(ownerId: String, planId: String): DomainResult<Unit> {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }
        return transactionRunner.runInTransaction {
            val deletedRows = workoutPlanDao.deletePlanById(ownerId, planId)
            if (deletedRows > 0) {
                DomainResult.Success(Unit)
            } else {
                DomainResult.Failure(DataError.NotFound("Workout plan '$planId' not found"))
            }
        }
    }

    private suspend fun validateDraft(draft: WorkoutPlanDraft): DataError? {
        PulseValidators.validatePlanName(draft.name)?.let {
            return DataError.ValidationError(it)
        }
        for (exercise in draft.exercises) {
            if (exerciseDao.getById(exercise.exerciseId) == null) {
                return DataError.ValidationError("Unknown exerciseId '${exercise.exerciseId}'")
            }
            val seenIndices = mutableSetOf<Int>()
            for (set in exercise.plannedSets) {
                PulseValidators.validateSetIndex(set.setIndex)?.let {
                    return DataError.ValidationError(it)
                }
                PulseValidators.validateTargetReps(set.targetReps)?.let {
                    return DataError.ValidationError(it)
                }
                if (!seenIndices.add(set.setIndex)) {
                    return DataError.ValidationError("Duplicate planned setIndex ${set.setIndex} in exercise ${exercise.exerciseId}")
                }
            }
        }
        return null
    }

    private fun buildChildEntities(
        planId: String,
        draft: WorkoutPlanDraft,
    ): Pair<List<PlanExerciseEntity>, List<PlannedSetEntity>> {
        val exerciseEntities = mutableListOf<PlanExerciseEntity>()
        val setEntities = mutableListOf<PlannedSetEntity>()

        draft.exercises.forEach { ex ->
            val planExerciseId = UUID.randomUUID().toString()
            exerciseEntities += PlanExerciseEntity(
                id = planExerciseId,
                planId = planId,
                exerciseId = ex.exerciseId,
                orderIndex = ex.orderIndex,
                notes = ex.notes?.trim(),
            )
            ex.plannedSets.forEach { set ->
                setEntities += PlannedSetEntity(
                    id = UUID.randomUUID().toString(),
                    planExerciseId = planExerciseId,
                    setIndex = set.setIndex,
                    targetReps = set.targetReps,
                    targetLoadGrams = set.targetLoad?.grams,
                    targetRestSeconds = set.targetRestSeconds,
                )
            }
        }
        return exerciseEntities to setEntities
    }
}
