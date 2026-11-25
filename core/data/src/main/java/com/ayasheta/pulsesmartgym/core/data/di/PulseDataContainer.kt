package com.ayasheta.pulsesmartgym.core.data.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase
import com.ayasheta.pulsesmartgym.core.data.local.preferences.PulsePreferencesDataSource
import com.ayasheta.pulsesmartgym.core.data.local.preferences.pulsePreferencesDataStore
import com.ayasheta.pulsesmartgym.core.data.repository.auth.FakeAuthRepository
import com.ayasheta.pulsesmartgym.core.data.repository.exercise.RoomExerciseRepository
import com.ayasheta.pulsesmartgym.core.data.repository.history.RoomWorkoutHistoryRepository
import com.ayasheta.pulsesmartgym.core.data.repository.profile.DataStoreUserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.data.repository.session.RoomWorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.data.repository.workout.RoomWorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.ExerciseRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutHistoryRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository

class PulseDataContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val isDebuggable = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    val database: PulseDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            PulseDatabase::class.java,
            "pulse_smart_gym.db",
        ).build()
    }

    private val preferencesDataSource: PulsePreferencesDataSource by lazy {
        PulsePreferencesDataSource(appContext.pulsePreferencesDataStore)
    }

    val exerciseRepository: ExerciseRepository by lazy {
        RoomExerciseRepository(database.exerciseDao())
    }

    val workoutPlanRepository: WorkoutPlanRepository by lazy {
        RoomWorkoutPlanRepository(
            workoutPlanDao = database.workoutPlanDao(),
            exerciseDao = database.exerciseDao(),
        )
    }

    val workoutSessionRepository: WorkoutSessionRepository by lazy {
        RoomWorkoutSessionRepository(
            sessionDao = database.workoutSessionDao(),
            planDao = database.workoutPlanDao(),
            exerciseDao = database.exerciseDao(),
        )
    }

    val workoutHistoryRepository: WorkoutHistoryRepository by lazy {
        RoomWorkoutHistoryRepository(database.workoutSessionDao())
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        DataStoreUserPreferencesRepository(preferencesDataSource)
    }

    val authRepository: AuthRepository by lazy {
        FakeAuthRepository(allowDemoSignIn = isDebuggable)
    }
}
