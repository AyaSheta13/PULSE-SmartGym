package com.ayasheta.pulsesmartgym.core.model.exercise

enum class TrackingCategory {
    LINEAR,
    ANGULAR,
    COMPOUND,
}

enum class EquipmentCategory {
    BARBELL,
    DUMBBELL,
    CABLE,
    MACHINE,
    BODYWEIGHT,
    OTHER,
}

enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    LEGS,
    CORE,
    FULL_BODY,
}

data class ExerciseDefinition(
    val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: EquipmentCategory,
    val trackingCategory: TrackingCategory,
    val description: String? = null,
    val imageKey: String? = null,
    val isHardwareTrackingSupported: Boolean = false,
)
