package com.ayasheta.pulsesmartgym.core.model.exercise

import com.ayasheta.pulsesmartgym.core.model.device.DeviceCapabilities
import com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind

enum class TrackingMovementStrategy {
    LINEAR,
    ANGULAR,
    COMPOUND,
    MANUAL_ONLY,
}

enum class CalibrationRequirementStatus {
    UNCONFIGURED_PENDING_HARDWARE_SPEC,
    CONFIGURED,
}

data class ExerciseTrackingConfiguration(
    val exerciseId: String,
    val strategy: TrackingMovementStrategy,
    val supportsAngularRom: Boolean,
    val supportsLinearRom: Boolean,
    val supportsPeakAcceleration: Boolean,
    val supportsAutomaticRepDetection: Boolean,
    val calibrationStatus: CalibrationRequirementStatus = CalibrationRequirementStatus.UNCONFIGURED_PENDING_HARDWARE_SPEC,
    val calibrationNotes: String = "Hardware sensor placement and ROM thresholds pending firmware agreement.",
)

object ExerciseTrackingCatalog {

    private val configsByExerciseId: Map<String, ExerciseTrackingConfiguration> = mapOf(
        "ex_bench_press" to linear("ex_bench_press"),
        "ex_db_flat_press" to linear("ex_db_flat_press"),
        "ex_db_incline_press" to linear("ex_db_incline_press"),
        "ex_db_shoulder_press" to linear("ex_db_shoulder_press"),
        "ex_back_squat" to linear("ex_back_squat"),
        "ex_smith_squat" to linear("ex_smith_squat"),
        "ex_leg_press" to linear("ex_leg_press"),
        "ex_romanian_deadlift" to compound("ex_romanian_deadlift"),
        "ex_barbell_row" to compound("ex_barbell_row"),
        "ex_cable_row" to linear("ex_cable_row"),
        "ex_lat_pulldown" to linear("ex_lat_pulldown"),
        "ex_bicep_curl" to angular("ex_bicep_curl"),
        "ex_db_preacher_curl" to angular("ex_db_preacher_curl"),
        "ex_lateral_raise" to angular("ex_lateral_raise"),
        "ex_tricep_pushdown" to angular("ex_tricep_pushdown"),
        "ex_cable_fly" to angular("ex_cable_fly"),
        "ex_leg_extension" to angular("ex_leg_extension"),
        "ex_calf_raise" to linear("ex_calf_raise"),
    )

    fun forExercise(
        exerciseId: String?,
        trackingCategory: TrackingCategory,
    ): ExerciseTrackingConfiguration {
        if (exerciseId != null) {
            configsByExerciseId[exerciseId]?.let { return it }
        }
        return when (trackingCategory) {
            TrackingCategory.LINEAR -> linear(exerciseId ?: "custom")
            TrackingCategory.ANGULAR -> angular(exerciseId ?: "custom")
            TrackingCategory.COMPOUND -> compound(exerciseId ?: "custom")
        }
    }

    fun resolveEffectiveCapabilities(
        deviceCapabilities: DeviceCapabilities,
        exerciseConfig: ExerciseTrackingConfiguration,
        environment: ExecutionEnvironmentKind,
        hasCalibration: Boolean = false,
    ): DeviceCapabilities {
        val allowAutomatic = deviceCapabilities.automaticRepEvents &&
            exerciseConfig.supportsAutomaticRepDetection &&
            (environment == ExecutionEnvironmentKind.DEMO ||
                (exerciseConfig.strategy != TrackingMovementStrategy.MANUAL_ONLY && hasCalibration))
        return deviceCapabilities.copy(
            automaticRepEvents = allowAutomatic,
            fullPartialClassification = deviceCapabilities.fullPartialClassification && allowAutomatic,
            angularRom = deviceCapabilities.angularRom && exerciseConfig.supportsAngularRom,
            linearRom = deviceCapabilities.linearRom && exerciseConfig.supportsLinearRom,
            peakAcceleration = deviceCapabilities.peakAcceleration && exerciseConfig.supportsPeakAcceleration,
        )
    }

    private fun linear(id: String) = ExerciseTrackingConfiguration(
        exerciseId = id,
        strategy = TrackingMovementStrategy.LINEAR,
        supportsAngularRom = false,
        supportsLinearRom = true,
        supportsPeakAcceleration = true,
        supportsAutomaticRepDetection = true,
    )

    private fun angular(id: String) = ExerciseTrackingConfiguration(
        exerciseId = id,
        strategy = TrackingMovementStrategy.ANGULAR,
        supportsAngularRom = true,
        supportsLinearRom = false,
        supportsPeakAcceleration = true,
        supportsAutomaticRepDetection = true,
    )

    private fun compound(id: String) = ExerciseTrackingConfiguration(
        exerciseId = id,
        strategy = TrackingMovementStrategy.COMPOUND,
        supportsAngularRom = true,
        supportsLinearRom = true,
        supportsPeakAcceleration = true,
        supportsAutomaticRepDetection = true,
    )
}
