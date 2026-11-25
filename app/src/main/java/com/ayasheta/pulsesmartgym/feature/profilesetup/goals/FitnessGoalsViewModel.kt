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

    private val _uiState = MutableStateFlow(
        FitnessGoalsUiState(
            selectedGoal = draftHolder.state.value.fitnessGoal,
            selectedExperience = draftHolder.state.value.experienceLevel,
            selectedWeeklyWorkouts = draftHolder.state.value.weeklyWorkouts,
        ),
    )
    val uiState: StateFlow<FitnessGoalsUiState> = _uiState.asStateFlow()

    fun selectGoal(goal: FitnessGoal) {
        draftHolder.updateGoals(fitnessGoal = goal)
        _uiState.update { it.copy(selectedGoal = goal, errorMessage = null) }
    }

    fun selectExperience(level: ExperienceLevel) {
        draftHolder.updateGoals(experienceLevel = level)
        _uiState.update { it.copy(selectedExperience = level, errorMessage = null) }
    }

    fun selectWeeklyWorkouts(days: Int) {
        draftHolder.updateGoals(weeklyWorkouts = days)
        _uiState.update { it.copy(selectedWeeklyWorkouts = days, errorMessage = null) }
    }

    fun completeSetup() {
        val current = _uiState.value
        if (current.isSaving || current.setupCompleted) return

        val personalInfo = draftHolder.toPersonalInfoDraftOrNull()
        if (personalInfo == null) {
            _uiState.update {
                it.copy(errorMessage = "Personal info is incomplete. Please go back and verify step 1.")
            }
            return
        }

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

            val goalsDraft = FitnessGoalsDraft(
                fitnessGoal = current.selectedGoal,
                experienceLevel = current.selectedExperience,
                weeklyWorkouts = current.selectedWeeklyWorkouts,
            )

            when (
                val result = userProfileRepository.saveCompletedProfile(
                    ownerId = authState.user.id,
                    personalInfo = personalInfo,
                    goals = goalsDraft,
                )
            ) {
                is DomainResult.Success -> {
                    draftHolder.reset()
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
