package com.ayasheta.pulsesmartgym.feature.device.env

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.DeviceRepository
import com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import com.ayasheta.pulsesmartgym.feature.session.counter.SetDraftStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ExecutionEnvironmentState {
    data object Genuine : ExecutionEnvironmentState
    data class DemoInitializing(val ownerId: String) : ExecutionEnvironmentState
    data class DemoReady(
        val ownerId: String,
        val isolatedContainer: PulseDataContainer.IsolatedTestContainer,
        val demoOwnerId: String,
        val demoPlanId: String?,
    ) : ExecutionEnvironmentState

    data class DemoClosing(val ownerId: String) : ExecutionEnvironmentState
    data class DemoFailed(val ownerId: String, val message: String) : ExecutionEnvironmentState
}

class DemoEnvironmentController(
    private val appContext: Context,
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    val setDraftStore: SetDraftStore = SetDraftStore(),
    private val isolatedFactory: suspend (Context) -> PulseDataContainer.IsolatedTestContainer = { ctx ->
        PulseDataContainer.createIsolatedForTesting(ctx)
    },
) : ViewModel() {

    private val _state = MutableStateFlow<ExecutionEnvironmentState>(ExecutionEnvironmentState.Genuine)
    val state: StateFlow<ExecutionEnvironmentState> = _state.asStateFlow()

    private var activeTransitionJob: Job? = null
    private var openContainer: PulseDataContainer.IsolatedTestContainer? = null
    private var currentOwnerId: String? = null

    init {
        viewModelScope.launch {
            combine(
                authRepository.observeAuthState(),
                deviceRepository.isDemoSessionActive,
            ) { auth, isDemo ->
                val ownerId = (auth as? AuthState.Authenticated)?.user?.id
                ownerId to isDemo
            }.collect { (ownerId, isDemo) ->
                if (ownerId == null || (currentOwnerId != null && currentOwnerId != ownerId)) {
                    currentOwnerId = ownerId
                    closeDemoSync(ownerId ?: "")
                    return@collect
                }
                currentOwnerId = ownerId
                if (isDemo) {
                    val existing = _state.value
                    if (existing is ExecutionEnvironmentState.DemoReady && existing.ownerId == ownerId) {
                        return@collect
                    }
                    if (existing is ExecutionEnvironmentState.DemoInitializing && existing.ownerId == ownerId) {
                        return@collect
                    }
                    startDemo(ownerId)
                } else {
                    if (_state.value !is ExecutionEnvironmentState.Genuine) {
                        closeDemoSync(ownerId)
                    }
                }
            }
        }
    }

    private fun startDemo(ownerId: String) {
        activeTransitionJob?.cancel()
        closeOpenContainerOnly()
        _state.value = ExecutionEnvironmentState.DemoInitializing(ownerId)
        activeTransitionJob = viewModelScope.launch {
            var created: PulseDataContainer.IsolatedTestContainer? = null
            try {
                created = isolatedFactory(appContext)
                if (currentOwnerId != ownerId || !deviceRepository.isDemoSessionActive.value) {
                    created.close()
                    _state.value = ExecutionEnvironmentState.Genuine
                    return@launch
                }
                val user = (created.container.authRepository.continueAsGuest() as? DomainResult.Success)?.data
                    ?: error("Failed to initialize isolated demo profile.")
                val bench = created.container.exerciseRepository.observeAll().first()
                    .firstOrNull { it.name.contains("Bench", ignoreCase = true) }
                val planId = if (bench != null) {
                    val createdPlan = created.container.workoutPlanRepository.createPlan(
                        ownerId = user.id,
                        draft = WorkoutPlanDraft(
                            name = "Upper Body Power (Demo)",
                            description = "Isolated device simulation workout",
                            exercises = listOf(
                                PlanExerciseDraft(
                                    exerciseId = bench.id,
                                    orderIndex = 0,
                                    plannedSets = listOf(
                                        PlannedSetDraft(1, 10, WeightLoad.fromKilograms(80.0), 60),
                                        PlannedSetDraft(2, 10, WeightLoad.fromKilograms(80.0), 60),
                                    ),
                                ),
                            ),
                        ),
                    )
                    (createdPlan as? DomainResult.Success)?.data?.id
                } else {
                    null
                }
                if (currentOwnerId != ownerId || !deviceRepository.isDemoSessionActive.value) {
                    created.close()
                    _state.value = ExecutionEnvironmentState.Genuine
                    return@launch
                }
                openContainer = created
                _state.value = ExecutionEnvironmentState.DemoReady(
                    ownerId = ownerId,
                    isolatedContainer = created,
                    demoOwnerId = user.id,
                    demoPlanId = planId,
                )
            } catch (ce: CancellationException) {
                runCatching { created?.close() }
                throw ce
            } catch (e: Exception) {
                runCatching { created?.close() }
                _state.value = ExecutionEnvironmentState.DemoFailed(
                    ownerId = ownerId,
                    message = e.message ?: "Failed to start isolated demo environment.",
                )
            }
        }
    }

    private fun closeDemoSync(ownerId: String) {
        activeTransitionJob?.cancel()
        activeTransitionJob = null
        if (_state.value !is ExecutionEnvironmentState.Genuine) {
            _state.value = ExecutionEnvironmentState.DemoClosing(ownerId)
        }
        setDraftStore?.clearForEnvironment(ExecutionEnvironmentKind.DEMO)
        closeOpenContainerOnly()
        _state.value = ExecutionEnvironmentState.Genuine
    }

    private fun closeOpenContainerOnly() {
        runCatching { openContainer?.close() }
        openContainer = null
    }

    override fun onCleared() {
        activeTransitionJob?.cancel()
        activeTransitionJob = null
        closeOpenContainerOnly()
        super.onCleared()
    }
}
