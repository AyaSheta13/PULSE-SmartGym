package com.ayasheta.pulsesmartgym.core.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ayasheta.pulsesmartgym.core.data.local.database.converter.PulseTypeConverters
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.ExerciseDao
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.LocalAccountDao
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.UserProfileDao
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.WorkoutPlanDao
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.WorkoutSessionDao
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.ExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.LocalAccountEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PerformedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PlanExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PlannedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.RepRecordEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionPlannedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionRestStateEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.UserProfileEntity
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
        SessionRestStateEntity::class,
        PerformedSetEntity::class,
        RepRecordEntity::class,
        LocalAccountEntity::class,
        UserProfileEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(PulseTypeConverters::class)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun localAccountDao(): LocalAccountDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_accounts` (
                        `id` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `passwordSalt` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_local_accounts_email` ON `local_accounts` (`email`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_profiles` (
                        `ownerId` TEXT NOT NULL,
                        `fullName` TEXT NOT NULL,
                        `gender` TEXT NOT NULL,
                        `age` INTEGER NOT NULL,
                        `heightCm` INTEGER NOT NULL,
                        `weightKg` REAL NOT NULL,
                        `measurementUnit` TEXT NOT NULL,
                        `fitnessGoal` TEXT NOT NULL,
                        `experienceLevel` TEXT NOT NULL,
                        `weeklyWorkouts` INTEGER NOT NULL,
                        `setupCompleted` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`ownerId`)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `session_rest_states` (
                        `sessionId` TEXT NOT NULL,
                        `exerciseId` TEXT NOT NULL,
                        `exerciseName` TEXT NOT NULL,
                        `completedSetIndex` INTEGER NOT NULL,
                        `nextExerciseId` TEXT,
                        `nextSetIndex` INTEGER,
                        `durationSeconds` INTEGER NOT NULL,
                        `deadlineEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`sessionId`),
                        FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `rep_records` ADD COLUMN `peakAccelerationMetersPerSecondSquared` REAL DEFAULT NULL",
                )
            }
        }
    }
}
