package com.ayasheta.pulsesmartgym.feature.profile.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class EditProfileViewModel(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collectLatest { authState ->
                when (authState) {
                    AuthState.Initializing -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    AuthState.Unauthenticated -> {
                        _uiState.value = EditProfileUiState(
                            isLoading = false,
                            saveError = "Session expired. Please sign in again.",
                        )
                    }
                    is AuthState.Authenticated -> {
                        val user = authState.user
                        val profile = userProfileRepository.getProfile(user.id)
                        val name = profile?.fullName ?: user.displayName
                        val weightStr = profile?.let { String.format(Locale.US, "%.1f", it.weightKg) } ?: ""
                        val heightStr = profile?.heightCm?.toString() ?: ""
                        val gender = profile?.gender ?: GenderIdentity.MALE
                        val goal = profile?.fitnessGoal ?: FitnessGoal.HYPERTROPHY
                        val level = profile?.experienceLevel ?: ExperienceLevel.INTERMEDIATE
                        val emailText = when {
                            user.isGuest -> "Offline Guest Profile (No Email)"
                            user.email.isNotBlank() -> user.email
                            else -> "Local Account"
                        }
                        _uiState.value = EditProfileUiState(
                            isLoading = false,
                            ownerId = user.id,
                            isGuest = user.isGuest,
                            emailDisplay = emailText,
                            fullName = name,
                            weightInput = weightStr,
                            heightInput = heightStr,
                            gender = gender,
                            fitnessGoal = goal,
                            experienceLevel = level,
                            preservedAge = profile?.age ?: 25,
                            preservedWeeklyWorkouts = profile?.weeklyWorkouts ?: 4,
                            preservedUnit = profile?.measurementUnit ?: WeightUnit.KG,
                            initialFullName = name,
                            initialWeightInput = weightStr,
                            initialHeightInput = heightStr,
                            initialGender = gender,
                            initialFitnessGoal = goal,
                            initialExperienceLevel = level,
                        )
                    }
                }
            }
        }
    }

    fun onFullNameChanged(value: String) {
        _uiState.update { it.copy(fullName = value, validationError = null, saveError = null) }
    }

    fun onWeightChanged(value: String) {
        _uiState.update { it.copy(weightInput = value, validationError = null, saveError = null) }
    }

    fun onHeightChanged(value: String) {
        _uiState.update { it.copy(heightInput = value, validationError = null, saveError = null) }
    }

    fun onGenderSelected(gender: GenderIdentity) {
        _uiState.update { it.copy(gender = gender, validationError = null, saveError = null) }
    }

    fun onFitnessGoalSelected(goal: FitnessGoal) {
        _uiState.update { it.copy(fitnessGoal = goal, validationError = null, saveError = null) }
    }

    fun onExperienceLevelSelected(level: ExperienceLevel) {
        _uiState.update { it.copy(experienceLevel = level, validationError = null, saveError = null) }
    }

    fun onChangeAvatarClick() {
        _uiState.update { it.copy(avatarUnavailableNotice = true) }
    }

    fun dismissAvatarNotice() {
        _uiState.update { it.copy(avatarUnavailableNotice = false) }
    }

    fun saveChanges(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isLoading || state.isSaving) return
        if (state.ownerId.isBlank()) {
            _uiState.update { it.copy(saveError = "Unable to save without an authenticated profile.") }
            return
        }

        val trimmedName = state.fullName.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(validationError = "Full name is required.") }
            return
        }

        val weightKg = state.weightInput.trim().toDoubleOrNull()
        if (weightKg == null || weightKg.isNaN() || weightKg.isInfinite() || weightKg <= 0.0 || weightKg > 500.0) {
            _uiState.update { it.copy(validationError = "Enter a valid weight between 1 and 500 kg.") }
            return
        }

        val heightCm = state.heightInput.trim().toIntOrNull()
        if (heightCm == null || heightCm !in 80..260) {
            _uiState.update { it.copy(validationError = "Enter a valid height between 80 and 260 cm.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, validationError = null, saveError = null) }

        viewModelScope.launch {
            val result = userProfileRepository.saveCompletedProfile(
                ownerId = state.ownerId,
                personalInfo = PersonalInfoDraft(
                    fullName = trimmedName,
                    gender = state.gender,
                    age = state.preservedAge,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    measurementUnit = state.preservedUnit,
                ),
                goals = FitnessGoalsDraft(
                    fitnessGoal = state.fitnessGoal,
                    experienceLevel = state.experienceLevel,
                    weeklyWorkouts = state.preservedWeeklyWorkouts,
                ),
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess()
                }
                is DomainResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveError = result.error.userMessage(),
                        )
                    }
                }
            }
        }
    }
}
