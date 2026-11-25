package com.ayasheta.pulsesmartgym.feature.workout.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.ExerciseRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutDurationEstimator
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlan
import com.ayasheta.pulsesmartgym.feature.workout.components.displayLabel
import com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class MyWorkoutsViewModel(
    authRepository: AuthRepository,
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val uiState: StateFlow<MyWorkoutsUiState> = authRepository.observeAuthState()
        .flatMapLatest { authState ->
            when (authState) {
                AuthState.Initializing -> flowOf(MyWorkoutsUiState.Loading)
                AuthState.Unauthenticated -> flowOf(MyWorkoutsUiState.SignedOut)
                is AuthState.Authenticated -> combine(
                    workoutPlanRepository.observePlans(authState.user.id),
                    exerciseRepository.observeAll(),
                ) { plans, catalog ->
                    val catalogById = catalog.associateBy { it.id }
                    MyWorkoutsUiState.Loaded(
                        ownerId = authState.user.id,
                        plans = plans.map { plan -> plan.toCardUiModel(catalogById) },
                    )
                }
            }
        }
        .catch { throwable ->
            emit(MyWorkoutsUiState.Error(throwable.message ?: "Unable to load your workout plans."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyWorkoutsUiState.Loading,
        )

    private fun WorkoutPlan.toCardUiModel(
        catalogById: Map<String, ExerciseDefinition>,
    ): WorkoutPlanCardUiModel {
        val sortedExercises = exercises.sortedBy { it.orderIndex }
        val resolvedDefs = sortedExercises.mapNotNull { catalogById[it.exerciseId] }
        val categories = resolvedDefs
            .map { it.muscleGroup.displayLabel() }
            .distinct()
            .take(3)
            .ifEmpty { listOf("General") }
        val firstDef = resolvedDefs.firstOrNull()
        val thumbnailRes = resolveExerciseThumbnailRes(
            imageKey = firstDef?.imageKey,
            muscleGroup = firstDef?.muscleGroup,
        )
        return WorkoutPlanCardUiModel(
            id = id,
            name = name,
            description = description,
            exerciseCount = sortedExercises.size,
            totalSetsCount = sortedExercises.sumOf { it.plannedSets.size },
            estimatedMinutes = WorkoutDurationEstimator.estimateMinutes(this),
            categoryChips = categories,
            thumbnailRes = thumbnailRes,
        )
    }
}
