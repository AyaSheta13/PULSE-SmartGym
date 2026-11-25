package com.ayasheta.pulsesmartgym.core.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.ExerciseEntity
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT * FROM exercises
        WHERE (:query = '' OR LOWER(name) LIKE '%' || LOWER(:query) || '%')
          AND (:muscleGroup IS NULL OR muscleGroup = :muscleGroup)
          AND (:equipment IS NULL OR equipment = :equipment)
          AND (:trackingCategory IS NULL OR trackingCategory = :trackingCategory)
        ORDER BY name ASC
        """,
    )
    fun search(
        query: String,
        muscleGroup: MuscleGroup?,
        equipment: EquipmentCategory?,
        trackingCategory: TrackingCategory?,
    ): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(exercises: List<ExerciseEntity>)
}
