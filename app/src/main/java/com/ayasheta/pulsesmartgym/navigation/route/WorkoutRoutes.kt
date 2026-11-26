package com.ayasheta.pulsesmartgym.navigation.route

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutDetails(val planId: String) : NavKey

@Serializable
data object CreateWorkoutPlan : NavKey

@Serializable
data class EditWorkoutPlan(val planId: String) : NavKey

@Serializable
data class ExercisePicker(
    val planId: String? = null,
    val editorSessionId: String? = null,
) : NavKey

@Serializable
data class ExerciseDetails(val exerciseId: String) : NavKey
