package com.ayasheta.pulsesmartgym.feature.workout.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.ExerciseRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutPlanRepository
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutDurationEstimator
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlan
import com.ayasheta.pulsesmartgym.feature.workout.components.displayLabel
import com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutDetailsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val planId: String,
    authRepository: AuthRepository,
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val expandedOccurrenceIdFlow = MutableStateFlow(
        savedStateHandle.get<String?>(KEY_EXPANDED_ID),
    )
    private var hasInitializedDefaultExpansion = savedStateHandle.contains(KEY_EXPANDED_INIT)

    val uiState: StateFlow<WorkoutDetailsUiState> = authRepository.observeAuthState()
        .flatMapLatest { authState ->
            when (authState) {
                AuthState.Initializing -> flowOf(WorkoutDetailsUiState.Loading)
                AuthState.Unauthenticated -> flowOf(WorkoutDetailsUiState.SignedOut)
                is AuthState.Authenticated -> combine(
                    workoutPlanRepository.observePlan(authState.user.id, planId),
                    exerciseRepository.observeAll(),
                    userPreferencesRepository.observePreferences(),
                    expandedOccurrenceIdFlow,
                ) { plan, catalog, prefs, expandedId ->
                    if (plan == null) {
                        WorkoutDetailsUiState.NotFound("Workout plan not found or no longer available.")
                    } else {
                        val catalogById = catalog.associateBy { it.id }
                        val uiExercises = plan.toDetailExercises(catalogById, prefs.preferredWeightUnit)
                        val resolvedExpandedId = if (!hasInitializedDefaultExpansion) {
                            hasInitializedDefaultExpansion = true
                            savedStateHandle[KEY_EXPANDED_INIT] = true
                            val firstId = uiExercises.firstOrNull()?.occurrenceId
                            expandedOccurrenceIdFlow.value = firstId
                            savedStateHandle[KEY_EXPANDED_ID] = firstId
                            firstId
                        } else {
                            expandedId?.takeIf { id -> uiExercises.any { it.occurrenceId == id } }
                        }
                        val muscleTags = plan.exercises
                            .sortedBy { it.orderIndex }
                            .mapNotNull { catalogById[it.exerciseId]?.muscleGroup?.displayLabel() }
                            .distinct()
                        val categoryTag = when {
                            muscleTags.isEmpty() -> "WORKOUT PLAN"
                            muscleTags.size == 1 -> "${muscleTags.first().uppercase()} FOCUS"
                            else -> "${muscleTags.take(2).joinToString(" & ").uppercase()} ROUTINE"
                        }
                        WorkoutDetailsUiState.Loaded(
                            planId = plan.id,
                            categoryTag = categoryTag,
                            planName = plan.name,
                            description = plan.description,
                            exerciseCount = uiExercises.size,
                            totalSetsCount = uiExercises.sumOf { it.setsCount },
                            estimatedMinutes = WorkoutDurationEstimator.estimateMinutes(plan),
                            exercises = uiExercises,
                            expandedOccurrenceId = resolvedExpandedId,
                        )
                    }
                }
            }
        }
        .catch { throwable ->
            emit(WorkoutDetailsUiState.NotFound(throwable.message ?: "Unable to load workout plan."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WorkoutDetailsUiState.Loading,
        )

    fun toggleExercise(occurrenceId: String) {
        val next = if (expandedOccurrenceIdFlow.value == occurrenceId) null else occurrenceId
        hasInitializedDefaultExpansion = true
        savedStateHandle[KEY_EXPANDED_INIT] = true
        expandedOccurrenceIdFlow.value = next
        savedStateHandle[KEY_EXPANDED_ID] = next
    }

    private fun WorkoutPlan.toDetailExercises(
        catalogById: Map<String, ExerciseDefinition>,
        preferredUnit: WeightUnit,
    ): List<WorkoutDetailExerciseUiModel> = exercises
        .sortedBy { it.orderIndex }
        .mapIndexed { index, planExercise ->
            val def = catalogById[planExercise.exerciseId]
            val sets = planExercise.plannedSets.sortedBy { it.setIndex }
            val distinctReps = sets.map { it.targetReps }.distinct()
            val distinctRests = sets.map { it.targetRestSeconds ?: 90 }.distinct()
            val targetRepsLabel = when {
                sets.isEmpty() -> "0"
                distinctReps.size == 1 -> distinctReps.first().toString()
                else -> "${distinctReps.min()}–${distinctReps.max()}"
            }
            val restLabel = when {
                sets.isEmpty() -> "—"
                distinctRests.size == 1 -> "${distinctRests.first()}s"
                else -> "Mixed"
            }
            val perSetRows = sets.map { set ->
                val loadLabel = set.targetLoad?.let { load ->
                    val raw = if (preferredUnit == WeightUnit.KG) load.toKilograms() else load.toPounds()
                    val formatted = if (raw % 1.0 == 0.0) raw.roundToInt().toString() else String.format("%.1f", raw)
                    val unit = if (preferredUnit == WeightUnit.KG) "kg" else "lbs"
                    "$formatted $unit"
                }
                WorkoutDetailSetRowUiModel(
                    setIndex = set.setIndex,
                    targetReps = set.targetReps,
                    restSeconds = set.targetRestSeconds,
                    loadLabel = loadLabel,
                )
            }
            val hasLoads = perSetRows.any { it.loadLabel != null }
            WorkoutDetailExerciseUiModel(
                occurrenceId = planExercise.id,
                orderNumber = index + 1,
                exerciseId = planExercise.exerciseId,
                name = def?.name ?: "Exercise",
                muscleSubtitle = def?.let { "${it.muscleGroup.displayLabel()} • ${it.equipment.displayLabel()}" } ?: "Planned Movement",
                photoRes = resolveExerciseThumbnailRes(def?.imageKey, def?.muscleGroup),
                setsCount = sets.size,
                targetRepsLabel = targetRepsLabel,
                restLabel = restLabel,
                collapsedSummary = "${sets.size} × $targetRepsLabel",
                perSetRows = perSetRows,
                hasMixedOrLoadDetails = distinctReps.size > 1 || distinctRests.size > 1 || hasLoads,
            )
        }

    private companion object {
        const val KEY_EXPANDED_ID = "details_expanded_occurrence_id"
        const val KEY_EXPANDED_INIT = "details_expanded_initialized"
    }
}
