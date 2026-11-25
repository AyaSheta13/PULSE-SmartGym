package com.ayasheta.pulsesmartgym.core.data.repository.exercise

import com.ayasheta.pulsesmartgym.core.data.local.database.ExerciseCatalogSeed
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.ExerciseDao
import com.ayasheta.pulsesmartgym.core.data.mapper.toDomain
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomExerciseRepository(
    private val exerciseDao: ExerciseDao,
) : ExerciseRepository {

    private val seedMutex = Mutex()
    @Volatile
    private var hasSeeded = false

    private suspend fun ensureSeeded() {
        if (hasSeeded) return
        seedMutex.withLock {
            if (!hasSeeded) {
                exerciseDao.insertAllIgnore(ExerciseCatalogSeed.defaultExercises)
                hasSeeded = true
            }
        }
    }

    override fun observeAll(): Flow<List<ExerciseDefinition>> = flow {
        ensureSeeded()
        emitAll(
            exerciseDao.observeAll().map { entities -> entities.map { it.toDomain() } },
        )
    }

    override fun search(
        query: String,
        muscleGroup: MuscleGroup?,
        equipment: EquipmentCategory?,
        trackingCategory: TrackingCategory?,
    ): Flow<List<ExerciseDefinition>> = flow {
        ensureSeeded()
        emitAll(
            exerciseDao.search(
                query = query.trim(),
                muscleGroup = muscleGroup,
                equipment = equipment,
                trackingCategory = trackingCategory,
            ).map { entities -> entities.map { it.toDomain() } },
        )
    }

    override suspend fun getById(id: String): ExerciseDefinition? {
        ensureSeeded()
        return exerciseDao.getById(id)?.toDomain()
    }
}
