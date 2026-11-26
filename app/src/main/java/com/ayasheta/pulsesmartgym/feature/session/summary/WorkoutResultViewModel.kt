package com.ayasheta.pulsesmartgym.feature.session.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.history.VolumeCoverage
import com.ayasheta.pulsesmartgym.core.model.history.WorkoutAnalytics
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionUiMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutResultViewModel(
    private val sessionId: String,
    authRepository: AuthRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<WorkoutResultUiState> = authRepository.observeAuthState()
        .flatMapLatest { authState ->
            when (authState) {
                AuthState.Initializing -> flowOf(WorkoutResultUiState.Loading)
                AuthState.Unauthenticated -> flowOf(WorkoutResultUiState.SignedOut)
                is AuthState.Authenticated -> combine(
                    workoutSessionRepository.observeSession(authState.user.id, sessionId),
                    userPreferencesRepository.observePreferences(),
                ) { session, prefs ->
                    if (session == null) {
                        WorkoutResultUiState.Missing("Workout summary not found.")
                    } else {
                        mapSessionToResult(session, prefs.preferredWeightUnit)
                    }
                }
            }
        }
        .catch { throwable ->
            emit(WorkoutResultUiState.Error(throwable.message ?: "Unable to load workout summary."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WorkoutResultUiState.Loading,
        )

    companion object {
        fun mapSessionToResult(
            session: WorkoutSession,
            unit: WeightUnit,
        ): WorkoutResultUiState.Loaded {
            val summary = WorkoutAnalytics.summarizeSession(session)
            val orderedExercises = session.exercises.sortedBy { it.orderIndex }

            val (headline, subtitle) = when (session.status) {
                SessionStatus.COMPLETED -> "Workout Complete!" to "Your workout has been recorded."
                SessionStatus.PARTIALLY_COMPLETED -> "Workout Ended Early" to
                    "${summary.completedSetsCount} of ${summary.totalPlannedSetsCount} sets completed • Progress saved to History."
                SessionStatus.ABANDONED -> "Workout Abandoned" to
                    "0 of ${summary.totalPlannedSetsCount} sets recorded • Not saved to workout history."
                SessionStatus.ACTIVE -> "Workout In Progress" to
                    "${summary.completedSetsCount} of ${summary.totalPlannedSetsCount} sets completed."
            }

            val setsChip = if (session.status == SessionStatus.COMPLETED) {
                "${summary.completedSetsCount} completed sets"
            } else {
                "${summary.completedSetsCount} / ${summary.totalPlannedSetsCount} sets"
            }

            val allPerformed = orderedExercises.flatMap { it.performedSets }
            val coverage = WorkoutAnalytics.calculateVolumeCoverage(allPerformed)
            val volumeChip = when {
                coverage == VolumeCoverage.NO_MEASURED_VOLUME && summary.completedSetsCount > 0 -> "— volume"
                coverage == VolumeCoverage.PARTIALLY_MEASURED -> {
                    val formatted = SessionUiMapper.formatVolumeNumber(summary.totalVolumeKg, unit)
                    val unitLabel = if (unit == WeightUnit.KG) "kg" else "lbs"
                    "$formatted $unitLabel vol (partial)"
                }
                else -> "${SessionUiMapper.formatVolumeWithUnit(summary.totalVolumeKg, unit)} volume"
            }

            val recordedExercisesCount = orderedExercises.count { it.performedSets.isNotEmpty() }
            val breakdownRight = if (session.status == SessionStatus.COMPLETED) {
                "${orderedExercises.size} Exercises"
            } else {
                "$recordedExercisesCount of ${orderedExercises.size} Exercises Recorded"
            }

            val breakdownItems = orderedExercises.map { ex ->
                val performed = ex.performedSets.sortedBy { it.setIndex }
                val setsCount = performed.size
                val fullReps = performed.sumOf { it.fullReps }
                val partialReps = performed.sumOf { it.partialReps }
                val distinctLoads = performed.mapNotNull { it.actualLoad }.distinctBy { it.grams }
                val loadBadge = when {
                    distinctLoads.size == 1 -> SessionUiMapper.formatWeightLoad(distinctLoads.first(), unit)
                    distinctLoads.size > 1 -> SessionUiMapper.formatWeightLoad(distinctLoads.maxBy { it.grams }, unit)?.let { "up to $it" }
                    else -> null
                }
                val modeBadge = when {
                    performed.isEmpty() -> "Skipped"
                    performed.any { it.trackingMode == TrackingMode.DEVICE_ASSISTED } -> "Device"
                    else -> "Manual"
                }
                ResultExerciseBreakdownUiModel(
                    sessionExerciseId = ex.id,
                    exerciseName = ex.exerciseNameSnapshot,
                    recordedSetsCount = setsCount,
                    totalFullReps = fullReps,
                    totalPartialReps = partialReps,
                    loadBadgeText = loadBadge,
                    modeBadgeText = modeBadge,
                    isSkipped = performed.isEmpty(),
                )
            }

            return WorkoutResultUiState.Loaded(
                sessionId = session.id,
                planName = session.planNameSnapshot,
                status = session.status,
                statusHeadline = headline,
                statusSubtitle = subtitle,
                totalFullReps = summary.totalFullReps,
                totalPartialReps = summary.totalPartialReps,
                durationText = SessionUiMapper.formatDurationReadable(summary.durationMs),
                setsChipText = setsChip,
                volumeChipText = volumeChip,
                breakdownHeaderRight = breakdownRight,
                preferredWeightUnit = unit,
                exercises = breakdownItems,
            )
        }
    }
}
