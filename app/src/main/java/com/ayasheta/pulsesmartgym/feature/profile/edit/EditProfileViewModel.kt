package com.ayasheta.pulsesmartgym.feature.profile.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.EditProfileDraft
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
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

    @Volatile
    private var activeAuthOwnerId: String? = null

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collectLatest { authState ->
                when (authState) {
                    AuthState.Initializing -> {
                        activeAuthOwnerId = null
                        _uiState.update { it.copy(isLoading = true, hasExistingProfile = false) }
                    }
                    AuthState.Unauthenticated -> {
                        activeAuthOwnerId = null
                        _uiState.value = EditProfileUiState(
                            isLoading = false,
                            hasExistingProfile = false,
                            missingProfileError = "Your session has ended. Please sign in again.",
                        )
                    }
                    is AuthState.Authenticated -> {
                        val user = authState.user
                        activeAuthOwnerId = user.id
                        val emailText = when {
                            user.isGuest -> "Offline Guest Profile (No Email)"
                            user.email.isNotBlank() -> user.email
                            else -> "Local Account"
                        }
                        val profile = userProfileRepository.getProfile(user.id)
                        if (activeAuthOwnerId != user.id) return@collectLatest
                        if (profile == null || !profile.setupCompleted) {
                            _uiState.value = EditProfileUiState(
                                isLoading = false,
                                hasExistingProfile = false,
                                missingProfileError = "No completed profile was found for this account. Complete Profile Setup before editing.",
                                ownerId = user.id,
                                isGuest = user.isGuest,
                                emailDisplay = emailText,
                            )
                        } else {
                            val weightStr = String.format(Locale.US, "%.1f", profile.weightKg)
                            val heightStr = profile.heightCm.toString()
                            _uiState.value = EditProfileUiState(
                                isLoading = false,
                                hasExistingProfile = true,
                                missingProfileError = null,
                                ownerId = user.id,
                                isGuest = user.isGuest,
                                emailDisplay = emailText,
                                fullName = profile.fullName,
                                weightInput = weightStr,
                                heightInput = heightStr,
                                gender = profile.gender,
                                fitnessGoal = profile.fitnessGoal,
                                experienceLevel = profile.experienceLevel,
                                initialFullName = profile.fullName,
                                initialWeightInput = weightStr,
                                initialHeightInput = heightStr,
                                initialGender = profile.gender,
                                initialFitnessGoal = profile.fitnessGoal,
                                initialExperienceLevel = profile.experienceLevel,
                            )
                        }
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
        if (!state.canSave) {
            if (!state.isLoading && !state.hasExistingProfile) {
                _uiState.update {
                    it.copy(
                        saveError = it.missingProfileError
                            ?: "Cannot save changes without an existing completed profile.",
                    )
                }
            }
            return
        }
        if (state.ownerId.isBlank() || state.ownerId != activeAuthOwnerId) {
            _uiState.update {
                it.copy(saveError = "Active account changed while editing. Changes were not saved.")
            }
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

        val targetOwnerId = state.ownerId
        _uiState.update { it.copy(isSaving = true, validationError = null, saveError = null) }

        viewModelScope.launch {
            if (targetOwnerId != activeAuthOwnerId) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = "Active account changed while editing. Changes were not saved.",
                    )
                }
                return@launch
            }
            val result = userProfileRepository.updateExistingProfile(
                ownerId = targetOwnerId,
                draft = EditProfileDraft(
                    fullName = trimmedName,
                    gender = state.gender,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    fitnessGoal = state.fitnessGoal,
                    experienceLevel = state.experienceLevel,
                ),
            )
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    if (targetOwnerId == activeAuthOwnerId) {
                        onSuccess()
                    }
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
