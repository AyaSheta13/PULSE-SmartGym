package com.ayasheta.pulsesmartgym.feature.profilesetup.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import com.ayasheta.pulsesmartgym.feature.profilesetup.ProfileSetupDraftHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FitnessGoalsUiState(
    val selectedGoal: FitnessGoal = FitnessGoal.HYPERTROPHY,
    val selectedExperience: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val selectedWeeklyWorkouts: Int = 4,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val setupCompleted: Boolean = false,
)

class FitnessGoalsViewModel(
    private val draftHolder: ProfileSetupDraftHolder,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private var activeOwnerId: String = ""

    private val _uiState = MutableStateFlow(FitnessGoalsUiState())
    val uiState: StateFlow<FitnessGoalsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collectLatest { authState ->
                when (authState) {
                    AuthState.Initializing -> Unit
                    AuthState.Unauthenticated -> {
                        activeOwnerId = ""
                        draftHolder.reset()
                        _uiState.value = FitnessGoalsUiState()
                    }
                    is AuthState.Authenticated -> {
                        activeOwnerId = authState.user.id
                        val draft = draftHolder.ensureOwner(activeOwnerId)
                        _uiState.update {
                            it.copy(
                                selectedGoal = draft.fitnessGoal,
                                selectedExperience = draft.experienceLevel,
                                selectedWeeklyWorkouts = draft.weeklyWorkouts,
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectGoal(goal: FitnessGoal) {
        if (activeOwnerId.isNotBlank()) {
            draftHolder.updateGoals(ownerId = activeOwnerId, fitnessGoal = goal)
        }
        _uiState.update { it.copy(selectedGoal = goal, errorMessage = null) }
    }

    fun selectExperience(level: ExperienceLevel) {
        if (activeOwnerId.isNotBlank()) {
            draftHolder.updateGoals(ownerId = activeOwnerId, experienceLevel = level)
        }
        _uiState.update { it.copy(selectedExperience = level, errorMessage = null) }
    }

    fun selectWeeklyWorkouts(days: Int) {
        if (activeOwnerId.isNotBlank()) {
            draftHolder.updateGoals(ownerId = activeOwnerId, weeklyWorkouts = days)
        }
        _uiState.update { it.copy(selectedWeeklyWorkouts = days, errorMessage = null) }
    }

    fun completeSetup() {
        val current = _uiState.value
        if (current.isSaving || current.setupCompleted) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val authState = authRepository.observeAuthState().first()
            if (authState !is AuthState.Authenticated) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "No active session found. Please sign in or continue as guest.",
                    )
                }
                return@launch
            }

            val ownerId = authState.user.id
            val personalInfo = draftHolder.toPersonalInfoDraftOrNull(ownerId)
            if (personalInfo == null) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Personal info is incomplete. Please go back and verify step 1.",
                    )
                }
                return@launch
            }

            val goalsDraft = FitnessGoalsDraft(
                fitnessGoal = current.selectedGoal,
                experienceLevel = current.selectedExperience,
                weeklyWorkouts = current.selectedWeeklyWorkouts,
            )

            when (
                val result = userProfileRepository.saveCompletedProfile(
                    ownerId = ownerId,
                    personalInfo = personalInfo,
                    goals = goalsDraft,
                )
            ) {
                is DomainResult.Success -> {
                    draftHolder.reset(ownerId)
                    _uiState.update { it.copy(isSaving = false, setupCompleted = true) }
                }
                is DomainResult.Failure -> {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = result.error.userMessage())
                    }
                }
            }
        }
    }

    fun consumeCompletion() {
        _uiState.update { it.copy(setupCompleted = false) }
    }
}
