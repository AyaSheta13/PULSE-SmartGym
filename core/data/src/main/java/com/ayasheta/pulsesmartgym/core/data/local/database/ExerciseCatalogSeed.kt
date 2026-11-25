package com.ayasheta.pulsesmartgym.core.data.local.database

import com.ayasheta.pulsesmartgym.core.data.local.database.entity.ExerciseEntity
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory

object ExerciseCatalogSeed {
    val defaultExercises: List<ExerciseEntity> = listOf(
        ExerciseEntity(
            id = "ex_bench_press",
            name = "Barbell Bench Press",
            muscleGroup = MuscleGroup.CHEST,
            equipment = EquipmentCategory.BARBELL,
            trackingCategory = TrackingCategory.LINEAR,
            description = "Flat barbell chest press.",
            imageKey = "bench_press",
            isHardwareTrackingSupported = false,
        ),
        ExerciseEntity(
            id = "ex_cable_row",
            name = "Cable Row",
            muscleGroup = MuscleGroup.BACK,
            equipment = EquipmentCategory.CABLE,
            trackingCategory = TrackingCategory.LINEAR,
            description = "Seated horizontal cable row for lats and mid-back.",
            imageKey = "cable_row",
            isHardwareTrackingSupported = false,
        ),
        ExerciseEntity(
            id = "ex_bicep_curl",
            name = "Dumbbell Bicep Curl",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = EquipmentCategory.DUMBBELL,
            trackingCategory = TrackingCategory.ANGULAR,
            description = "Standing alternating or bilateral dumbbell curl.",
            imageKey = "bicep_curl",
            isHardwareTrackingSupported = false,
        ),
        ExerciseEntity(
            id = "ex_lateral_raise",
            name = "Dumbbell Lateral Raise",
            muscleGroup = MuscleGroup.SHOULDERS,
            equipment = EquipmentCategory.DUMBBELL,
            trackingCategory = TrackingCategory.ANGULAR,
            description = "Lateral deltoid raise.",
            imageKey = "lateral_raise",
            isHardwareTrackingSupported = false,
        ),
        ExerciseEntity(
            id = "ex_back_squat",
            name = "Barbell Back Squat",
            muscleGroup = MuscleGroup.LEGS,
            equipment = EquipmentCategory.BARBELL,
            trackingCategory = TrackingCategory.COMPOUND,
            description = "Compound lower-body barbell squat.",
            imageKey = "back_squat",
            isHardwareTrackingSupported = false,
        ),
        ExerciseEntity(
            id = "ex_romanian_deadlift",
            name = "Romanian Deadlift",
            muscleGroup = MuscleGroup.LEGS,
            equipment = EquipmentCategory.BARBELL,
            trackingCategory = TrackingCategory.COMPOUND,
            description = "Hip-hinge posterior chain movement.",
            imageKey = "romanian_deadlift",
            isHardwareTrackingSupported = false,
        ),
        ExerciseEntity(
            id = "ex_lat_pulldown",
            name = "Lat Pulldown",
            muscleGroup = MuscleGroup.BACK,
            equipment = EquipmentCategory.CABLE,
            trackingCategory = TrackingCategory.LINEAR,
            description = "Vertical cable pulldown targeting latissimus dorsi.",
            imageKey = "lat_pulldown",
            isHardwareTrackingSupported = false,
        ),
        ExerciseEntity(
            id = "ex_tricep_pushdown",
            name = "Cable Tricep Pushdown",
            muscleGroup = MuscleGroup.TRICEPS,
            equipment = EquipmentCategory.CABLE,
            trackingCategory = TrackingCategory.ANGULAR,
            description = "Standing cable tricep extension.",
            imageKey = "tricep_pushdown",
            isHardwareTrackingSupported = false,
        ),
    )
}
