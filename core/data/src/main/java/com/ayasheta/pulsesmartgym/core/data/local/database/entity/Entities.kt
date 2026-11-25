package com.ayasheta.pulsesmartgym.core.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: EquipmentCategory,
    val trackingCategory: TrackingCategory,
    val description: String?,
    val imageKey: String?,
    val isHardwareTrackingSupported: Boolean,
)

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey val id: String,
    val ownerProfileId: String,
    val name: String,
    val description: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["planId"]),
        Index(value = ["exerciseId"]),
    ],
)
data class PlanExerciseEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val notes: String?,
)

@Entity(
    tableName = "planned_sets",
    foreignKeys = [
        ForeignKey(
            entity = PlanExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["planExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["planExerciseId"]),
        Index(value = ["planExerciseId", "setIndex"], unique = true),
    ],
)
data class PlannedSetEntity(
    @PrimaryKey val id: String,
    val planExerciseId: String,
    val setIndex: Int,
    val targetReps: Int,
    val targetLoadGrams: Long?,
    val targetRestSeconds: Int?,
)

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourcePlanId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["sourcePlanId"]),
        Index(value = ["status"]),
        Index(value = ["startedAtEpochMs"]),
    ],
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val ownerProfileId: String,
    val sourcePlanId: String?,
    val planNameSnapshot: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val status: SessionStatus,
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceExerciseId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sourceExerciseId"]),
    ],
)
data class SessionExerciseEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val sourceExerciseId: String?,
    val exerciseNameSnapshot: String,
    val muscleGroupSnapshot: MuscleGroup,
    val equipmentSnapshot: EquipmentCategory,
    val trackingCategorySnapshot: TrackingCategory,
    val orderIndex: Int,
)

@Entity(
    tableName = "session_planned_sets",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionExerciseId"]),
        Index(value = ["sessionExerciseId", "setIndex"], unique = true),
    ],
)
data class SessionPlannedSetEntity(
    @PrimaryKey val id: String,
    val sessionExerciseId: String,
    val setIndex: Int,
    val targetReps: Int,
    val targetLoadGrams: Long?,
    val targetRestSeconds: Int?,
)

@Entity(
    tableName = "performed_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionExerciseId"]),
        Index(value = ["sessionId", "sessionExerciseId", "setIndex"], unique = true),
    ],
)
data class PerformedSetEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val sessionExerciseId: String,
    val setIndex: Int,
    val fullReps: Int,
    val partialReps: Int,
    val actualLoadGrams: Long?,
    val completedAtEpochMs: Long,
    val trackingMode: TrackingMode,
    val origin: MeasurementOrigin,
    val durationMs: Long?,
)

@Entity(
    tableName = "rep_records",
    foreignKeys = [
        ForeignKey(
            entity = PerformedSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["performedSetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["performedSetId"]),
        Index(value = ["performedSetId", "repIndex"], unique = true),
    ],
)
data class RepRecordEntity(
    @PrimaryKey val id: String,
    val performedSetId: String,
    val repIndex: Int,
    val classification: RepClassification,
    val totalDurationMs: Long?,
    val concentricDurationMs: Long?,
    val eccentricDurationMs: Long?,
    val angularRomDegrees: Double?,
    val linearRomCm: Double?,
    val origin: MeasurementOrigin,
)
