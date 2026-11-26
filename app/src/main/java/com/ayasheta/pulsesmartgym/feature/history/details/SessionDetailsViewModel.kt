package com.ayasheta.pulsesmartgym.feature.history.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import com.ayasheta.pulsesmartgym.core.model.history.VolumeCoverage
import com.ayasheta.pulsesmartgym.core.model.history.WorkoutAnalytics
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutHistoryRepository
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.feature.history.HistoryUiMapper
import com.ayasheta.pulsesmartgym.feature.session.dashboard.SessionUiMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailsViewModel(
    private val sessionId: String,
    private val authRepository: AuthRepository,
    private val workoutHistoryRepository: WorkoutHistoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionDetailsUiState>(SessionDetailsUiState.Loading)
    val uiState: StateFlow<SessionDetailsUiState> = _uiState.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    init {
        observeDetails()
    }

    fun retry() {
        _uiState.value = SessionDetailsUiState.Loading
        refreshTrigger.value += 1
    }

    private fun observeDetails() {
        viewModelScope.launch {
            combine(
                authRepository.observeAuthState(),
                refreshTrigger,
            ) { auth, _ -> auth }
                .flatMapLatest { authState ->
                    when (authState) {
                        AuthState.Initializing -> flowOf(SessionDetailsUiState.Loading)
                        AuthState.Unauthenticated -> flowOf(SessionDetailsUiState.SignedOut)
                        is AuthState.Authenticated -> {
                            combine(
                                workoutHistoryRepository.observeSessionDetails(authState.user.id, sessionId),
                                userPreferencesRepository.observePreferences(),
                            ) { session, prefs ->
                                if (session == null) {
                                    SessionDetailsUiState.Unavailable(
                                        "This historical workout is unavailable or has not been saved to History.",
                                    )
                                } else {
                                    mapToLoaded(
                                        session = session,
                                        unit = prefs.preferredWeightUnit,
                                        today = todayProvider(),
                                        zoneId = zoneProvider(),
                                    )
                                }
                            }.catch { throwable ->
                                emit(
                                    SessionDetailsUiState.Error(
                                        throwable.message ?: "Unable to load session details.",
                                    ),
                                )
                            }
                        }
                    }
                }
                .collectLatest { state ->
                    _uiState.value = state
                }
        }
    }

    companion object {
        private val monthDayFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
        private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

        fun mapToLoaded(
            session: WorkoutSession,
            unit: WeightUnit,
            today: LocalDate = LocalDate.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): SessionDetailsUiState.Loaded {
            val summary = WorkoutAnalytics.summarizeSession(session)
            val orderedExercises = session.exercises.sortedBy { it.orderIndex }
            val allPerformed = orderedExercises.flatMap { it.performedSets }
            val coverage = WorkoutAnalytics.calculateVolumeCoverage(allPerformed)

            val zoned = Instant.ofEpochMilli(session.startedAtEpochMs).atZone(zoneId)
            val datePart = when (val localDate = zoned.toLocalDate()) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> if (localDate.year == today.year) {
                    localDate.format(monthDayFormatter)
                } else {
                    "${localDate.format(monthDayFormatter)}, ${localDate.year}"
                }
            }
            val isPartial = session.status == SessionStatus.PARTIALLY_COMPLETED
            val overviewSubtitle = if (isPartial) {
                "$datePart • Ended Early • Saved on device (Local)"
            } else {
                "$datePart • ${zoned.format(timeFormatter)} • Saved on device (Local)"
            }

            val durationText = summary.durationMs?.let { dur ->
                if (isPartial) HistoryUiMapper.formatMmSs(dur) else HistoryUiMapper.formatDurationCompact(dur)
            } ?: "—"

            val setsChipText = if (isPartial) {
                "${summary.completedSetsCount} / ${summary.totalPlannedSetsCount} sets"
            } else {
                val word = if (summary.completedSetsCount == 1) "set" else "sets"
                "${summary.completedSetsCount} $word"
            }

            val repWord = if (summary.totalFullReps == 1) "repetition" else "repetitions"
            val repsChipText = if (summary.totalPartialReps > 0) {
                "${summary.totalFullReps} $repWord (+${summary.totalPartialReps} partial)"
            } else {
                "${summary.totalFullReps} $repWord"
            }

            val unitLabel = if (unit == WeightUnit.KG) "kg" else "lbs"
            val volumeChipText = if (coverage == VolumeCoverage.NO_MEASURED_VOLUME) {
                "— volume"
            } else {
                val volValue = when (unit) {
                    WeightUnit.KG -> summary.totalVolumeKg
                    WeightUnit.LBS -> summary.totalVolumeKg * 2.20462262185
                }
                val formatted = NumberFormat.getIntegerInstance(Locale.US).format(volValue.roundToInt())
                if (coverage == VolumeCoverage.PARTIALLY_MEASURED) {
                    "$formatted $unitLabel vol (partial)"
                } else {
                    "$formatted $unitLabel volume"
                }
            }

            val recordedExercisesCount = orderedExercises.count { it.performedSets.isNotEmpty() }
            val breakdownHeaderRight = if (isPartial) {
                "$recordedExercisesCount of ${orderedExercises.size} Exercises Recorded"
            } else {
                val exWord = if (orderedExercises.size == 1) "Exercise" else "Exercises"
                "${orderedExercises.size} $exWord"
            }

            val exercises = orderedExercises.map { ex ->
                val performed = ex.performedSets.sortedBy { it.setIndex }
                val isSkipped = performed.isEmpty()
                val fullReps = performed.sumOf { it.fullReps }
                val partialReps = performed.sumOf { it.partialReps }

                val summaryLine = if (isSkipped) {
                    "0 sets • 0 reps (Skipped)"
                } else {
                    val sWord = if (performed.size == 1) "set" else "sets"
                    val rWord = if (fullReps == 1) "rep" else "reps"
                    val pSuffix = if (partialReps > 0) " (+$partialReps partial)" else ""
                    "${performed.size} $sWord • $fullReps $rWord$pSuffix"
                }

                val distinctLoads = performed.mapNotNull { it.actualLoad }.distinctBy { it.grams }
                val loadBadgeText = when {
                    isSkipped || distinctLoads.isEmpty() -> "—"
                    distinctLoads.size == 1 -> SessionUiMapper.formatWeightLoad(distinctLoads.first(), unit) ?: "—"
                    else -> SessionUiMapper.formatWeightLoad(distinctLoads.maxBy { it.grams }, unit)
                        ?.let { "up to $it" } ?: "—"
                }

                val modeBadgeText = when {
                    isSkipped -> "Skipped"
                    performed.any { it.trackingMode == TrackingMode.DEVICE_ASSISTED } -> "Device"
                    else -> "Manual"
                }

                val setDetails = performed.map { set ->
                    val rText = if (set.partialReps > 0) {
                        "${set.fullReps} reps (+${set.partialReps} partial)"
                    } else {
                        "${set.fullReps} reps"
                    }
                    val wText = SessionUiMapper.formatWeightLoad(set.actualLoad, unit) ?: "—"
                    val dText = set.durationMs?.let { HistoryUiMapper.formatDurationCompact(it) }
                    val mLabel = if (set.trackingMode == TrackingMode.DEVICE_ASSISTED) "Device" else "Manual"
                    RecordedSetDetailUiModel(
                        setIndex = set.setIndex,
                        repsText = rText,
                        weightText = wText,
                        durationText = dText,
                        modeLabel = mLabel,
                    )
                }

                SessionDetailExerciseUiModel(
                    sessionExerciseId = ex.id,
                    exerciseName = ex.exerciseNameSnapshot,
                    summaryLine = summaryLine,
                    loadBadgeText = loadBadgeText,
                    modeBadgeText = modeBadgeText,
                    isSkipped = isSkipped,
                    recordedSets = setDetails,
                )
            }

            return SessionDetailsUiState.Loaded(
                sessionId = session.id,
                planName = session.planNameSnapshot,
                overviewSubtitle = overviewSubtitle,
                status = session.status,
                durationText = durationText,
                setsChipText = setsChipText,
                repsChipText = repsChipText,
                volumeChipText = volumeChipText,
                breakdownHeaderRight = breakdownHeaderRight,
                exercises = exercises,
            )
        }
    }
}
