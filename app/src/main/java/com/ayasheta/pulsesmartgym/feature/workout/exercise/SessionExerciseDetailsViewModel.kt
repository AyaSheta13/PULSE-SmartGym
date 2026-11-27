package com.ayasheta.pulsesmartgym.feature.workout.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.ExerciseRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.feature.workout.components.displayLabel
import com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Resolves exercise instructions for a specific session exercise occurrence
 * while keeping the active workout session untouched.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionExerciseDetailsViewModel(
    private val sessionId: String,
    private val sessionExerciseId: String,
    authRepository: AuthRepository,
    workoutSessionRepository: WorkoutSessionRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    val uiState: StateFlow<ExerciseDetailsUiState> = authRepository.observeAuthState()
        .flatMapLatest { auth ->
            val ownerId = (auth as? AuthState.Authenticated)?.user?.id
            if (ownerId == null) {
                flowOf(ExerciseDetailsUiState.NotFound(sessionExerciseId))
            } else {
                combine(
                    workoutSessionRepository.observeSession(ownerId = ownerId, sessionId = sessionId),
                    exerciseRepository.observeAll(),
                ) { session, exercises ->
                    val occurrence = session?.exercises?.firstOrNull { it.id == sessionExerciseId }
                        ?: return@combine ExerciseDetailsUiState.NotFound(sessionExerciseId)

                    val resolved = occurrence.sourceExerciseId?.let { sourceId ->
                        exercises.firstOrNull { it.id == sourceId }
                    } ?: exercises.firstOrNull {
                        it.name.equals(occurrence.exerciseNameSnapshot, ignoreCase = true)
                    } ?: ExerciseDefinition(
                        id = occurrence.sourceExerciseId ?: occurrence.id,
                        name = occurrence.exerciseNameSnapshot,
                        muscleGroup = occurrence.muscleGroupSnapshot,
                        equipment = occurrence.equipmentSnapshot,
                        trackingCategory = occurrence.trackingCategorySnapshot,
                        description = null,
                        imageKey = null,
                    )

                    ExerciseDetailsUiState.Loaded(
                        exerciseId = resolved.id,
                        name = resolved.name,
                        bodyPartLabel = resolved.muscleGroup.displayLabel(),
                        equipmentLabel = resolved.equipment.displayLabel(),
                        categoryLabel = resolved.trackingCategory.displayLabel(),
                        imageRes = resolveExerciseThumbnailRes(resolved.imageKey, resolved.muscleGroup),
                        guide = ExerciseGuideCatalog.forExercise(resolved),
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ExerciseDetailsUiState.Loading,
        )
}
