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
    @Query("SELECT * FROM workout_plans WHERE ownerProfileId = :ownerId ORDER BY updatedAtEpochMs DESC")
    fun observeAllPlans(ownerId: String): Flow<List<WorkoutPlanWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_plans WHERE id = :planId AND ownerProfileId = :ownerId")
    fun observePlan(ownerId: String, planId: String): Flow<WorkoutPlanWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_plans WHERE id = :planId AND ownerProfileId = :ownerId")
    suspend fun getPlan(ownerId: String, planId: String): WorkoutPlanWithExercises?

    @Query("SELECT COUNT(*) FROM workout_plans WHERE ownerProfileId = :ownerId")
    suspend fun countPlans(ownerId: String): Int

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

    @Query("DELETE FROM workout_plans WHERE id = :planId AND ownerProfileId = :ownerId")
    suspend fun deletePlanById(ownerId: String, planId: String): Int
}
