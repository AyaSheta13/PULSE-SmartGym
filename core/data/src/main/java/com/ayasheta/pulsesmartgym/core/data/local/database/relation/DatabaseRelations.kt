package com.ayasheta.pulsesmartgym.core.data.local.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PerformedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PlanExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PlannedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.RepRecordEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionPlannedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.WorkoutPlanEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.WorkoutSessionEntity

data class PlanExerciseWithSets(
    @Embedded val planExercise: PlanExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "planExerciseId",
    )
    val plannedSets: List<PlannedSetEntity>,
)

data class WorkoutPlanWithExercises(
    @Embedded val plan: WorkoutPlanEntity,
    @Relation(
        entity = PlanExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "planId",
    )
    val exercises: List<PlanExerciseWithSets>,
)

data class PerformedSetWithReps(
    @Embedded val performedSet: PerformedSetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "performedSetId",
    )
    val repRecords: List<RepRecordEntity>,
)

data class SessionExerciseWithSets(
    @Embedded val sessionExercise: SessionExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionExerciseId",
    )
    val plannedSets: List<SessionPlannedSetEntity>,
    @Relation(
        entity = PerformedSetEntity::class,
        parentColumn = "id",
        entityColumn = "sessionExerciseId",
    )
    val performedSets: List<PerformedSetWithReps>,
)

data class WorkoutSessionWithExercises(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = SessionExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val exercises: List<SessionExerciseWithSets>,
)
