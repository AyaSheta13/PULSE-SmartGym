package com.ayasheta.pulsesmartgym.navigation.route

import androidx.navigation3.runtime.NavKey
import com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutSession(
    val planId: String,
    val sessionId: String? = null,
    val environment: ExecutionEnvironmentKind = ExecutionEnvironmentKind.GENUINE,
) : NavKey

@Serializable
data class ReadyStartSet(
    val sessionId: String,
    val exerciseId: String,
    val setIndex: Int,
    val environment: ExecutionEnvironmentKind = ExecutionEnvironmentKind.GENUINE,
) : NavKey

@Serializable
data class ManualRepCounter(
    val sessionId: String,
    val exerciseId: String,
    val setIndex: Int,
    val environment: ExecutionEnvironmentKind = ExecutionEnvironmentKind.GENUINE,
) : NavKey

@Serializable
data class DeviceAssistedRepCounter(
    val sessionId: String,
    val exerciseId: String,
    val setIndex: Int,
    val environment: ExecutionEnvironmentKind = ExecutionEnvironmentKind.GENUINE,
) : NavKey

@Serializable
data class WorkoutComplete(
    val sessionId: String,
    val environment: ExecutionEnvironmentKind = ExecutionEnvironmentKind.GENUINE,
) : NavKey

@Serializable
data class PartialWorkoutSummary(
    val sessionId: String,
    val environment: ExecutionEnvironmentKind = ExecutionEnvironmentKind.GENUINE,
) : NavKey

@Serializable
data class SessionDetails(val sessionId: String) : NavKey

@Serializable
data class SessionExerciseDetails(
    val sessionId: String,
    val sessionExerciseId: String,
    val environment: ExecutionEnvironmentKind = ExecutionEnvironmentKind.GENUINE,
) : NavKey
