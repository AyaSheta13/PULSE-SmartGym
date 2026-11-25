package com.ayasheta.pulsesmartgym.core.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PlanExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PlannedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.WorkoutPlanEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.relation.WorkoutPlanWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {

    @Transaction
    @Query("SELECT * FROM workout_plans ORDER BY updatedAtEpochMs DESC")
    fun observeAllPlans(): Flow<List<WorkoutPlanWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_plans WHERE id = :planId")
    fun observePlan(planId: String): Flow<WorkoutPlanWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_plans WHERE id = :planId")
    suspend fun getPlan(planId: String): WorkoutPlanWithExercises?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlan(plan: WorkoutPlanEntity)

    @Update
    suspend fun updatePlan(plan: WorkoutPlanEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlanExercises(exercises: List<PlanExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlannedSets(sets: List<PlannedSetEntity>)

    @Query("DELETE FROM plan_exercises WHERE planId = :planId")
    suspend fun deletePlanExercisesByPlanId(planId: String)

    @Query("DELETE FROM workout_plans WHERE id = :planId")
    suspend fun deletePlanById(planId: String): Int

    @Transaction
    suspend fun createPlanGraph(
        plan: WorkoutPlanEntity,
        exercises: List<PlanExerciseEntity>,
        sets: List<PlannedSetEntity>,
    ) {
        insertPlan(plan)
        if (exercises.isNotEmpty()) insertPlanExercises(exercises)
        if (sets.isNotEmpty()) insertPlannedSets(sets)
    }

    @Transaction
    suspend fun replacePlanGraph(
        plan: WorkoutPlanEntity,
        exercises: List<PlanExerciseEntity>,
        sets: List<PlannedSetEntity>,
    ) {
        updatePlan(plan)
        deletePlanExercisesByPlanId(plan.id)
        if (exercises.isNotEmpty()) insertPlanExercises(exercises)
        if (sets.isNotEmpty()) insertPlannedSets(sets)
    }
}
