package com.ayasheta.pulsesmartgym.core.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ayasheta.pulsesmartgym.core.data.local.database.converter.PulseTypeConverters
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.ExerciseDao
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.WorkoutPlanDao
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.WorkoutSessionDao
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.ExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PerformedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PlanExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PlannedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.RepRecordEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionPlannedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.WorkoutPlanEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutPlanEntity::class,
        PlanExerciseEntity::class,
        PlannedSetEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SessionPlannedSetEntity::class,
        PerformedSetEntity::class,
        RepRecordEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(PulseTypeConverters::class)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
}
