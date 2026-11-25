package com.ayasheta.pulsesmartgym.core.data.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.local.database.RoomTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.local.preferences.PulsePreferencesDataSource
import com.ayasheta.pulsesmartgym.core.data.local.preferences.pulsePreferencesDataStore
import com.ayasheta.pulsesmartgym.core.data.repository.auth.LocalAuthRepository
import com.ayasheta.pulsesmartgym.core.data.repository.exercise.RoomExerciseRepository
import com.ayasheta.pulsesmartgym.core.data.repository.history.RoomWorkoutHistoryRepository
import com.ayasheta.pulsesmartgym.core.data.repository.profile.DataStoreUserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.data.repository.profile.RoomUserProfileRepository
import com.ayasheta.pulsesmartgym.core.data.repository.session.RoomWorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.data.repository.workout.RoomWorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.ExerciseRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutHistoryRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository

open class PulseDataContainer(
    context: Context,
    private val customDatabase: PulseDatabase? = null,
    private val customPreferencesDataSource: PulsePreferencesDataSource? = null,
) {
    private val appContext = context.applicationContext
    private val isDebuggable = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    val database: PulseDatabase by lazy {
        customDatabase ?: Room.databaseBuilder(
            appContext,
            PulseDatabase::class.java,
            "pulse_smart_gym.db",
        ).addMigrations(PulseDatabase.MIGRATION_1_2)
            .build()
    }

    private val transactionRunner: PulseTransactionRunner by lazy {
        RoomTransactionRunner(database)
    }

    private val preferencesDataSource: PulsePreferencesDataSource by lazy {
        customPreferencesDataSource ?: PulsePreferencesDataSource(appContext.pulsePreferencesDataStore)
    }

    val exerciseRepository: ExerciseRepository by lazy {
        RoomExerciseRepository(database.exerciseDao())
    }

    val workoutPlanRepository: WorkoutPlanRepository by lazy {
        RoomWorkoutPlanRepository(
            workoutPlanDao = database.workoutPlanDao(),
            exerciseDao = database.exerciseDao(),
            transactionRunner = transactionRunner,
        )
    }

    val workoutSessionRepository: WorkoutSessionRepository by lazy {
        RoomWorkoutSessionRepository(
            sessionDao = database.workoutSessionDao(),
            planDao = database.workoutPlanDao(),
            exerciseDao = database.exerciseDao(),
            transactionRunner = transactionRunner,
        )
    }

    val workoutHistoryRepository: WorkoutHistoryRepository by lazy {
        RoomWorkoutHistoryRepository(database.workoutSessionDao())
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        DataStoreUserPreferencesRepository(preferencesDataSource)
    }

    open val userProfileRepository: UserProfileRepository by lazy {
        RoomUserProfileRepository(
            userProfileDao = database.userProfileDao(),
            transactionRunner = transactionRunner,
        )
    }

    open val authRepository: AuthRepository by lazy {
        LocalAuthRepository(
            accountDao = database.localAccountDao(),
            preferencesDataSource = preferencesDataSource,
            transactionRunner = transactionRunner,
            allowDemoSignIn = isDebuggable,
        )
    }

    class IsolatedTestContainer(
        val container: PulseDataContainer,
        private val closeAction: () -> Unit,
    ) : AutoCloseable {
        override fun close() {
            closeAction()
        }
    }

    companion object {
        fun createIsolatedForTesting(context: Context): IsolatedTestContainer {
            val appContext = context.applicationContext
            val tempDir = File(appContext.cacheDir, "pulse_isolated_test_${System.nanoTime()}").apply {
                mkdirs()
            }
            val isolatedDb = Room.inMemoryDatabaseBuilder(appContext, PulseDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val isolatedDataStore = PreferenceDataStoreFactory.create(
                scope = dataStoreScope,
                produceFile = { File(tempDir, "isolated_prefs.preferences_pb") },
            )
            val container = PulseDataContainer(
                context = appContext,
                customDatabase = isolatedDb,
                customPreferencesDataSource = PulsePreferencesDataSource(isolatedDataStore),
            )
            return IsolatedTestContainer(container) {
                isolatedDb.close()
                dataStoreScope.cancel()
                tempDir.deleteRecursively()
            }
        }
    }
}
