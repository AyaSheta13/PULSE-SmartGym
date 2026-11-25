package com.ayasheta.pulsesmartgym.feature.profilesetup.personalinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import com.ayasheta.pulsesmartgym.feature.profilesetup.ProfileSetupDraftHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonalInfoUiState(
    val fullName: String = "",
    val gender: GenderIdentity = GenderIdentity.MALE,
    val ageText: String = "",
    val heightText: String = "",
    val weightText: String = "",
    val nameError: String? = null,
    val metricsError: String? = null,
    val imperialNotice: String? = null,
    val navigateToGoals: Boolean = false,
)

class PersonalInfoViewModel(
    private val draftHolder: ProfileSetupDraftHolder,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private var activeOwnerId: String = ""

    private val _uiState = MutableStateFlow(PersonalInfoUiState())
    val uiState: StateFlow<PersonalInfoUiState> = _uiState.asStateFlow()

    init {
        observeActiveOwner()
    }

    private fun observeActiveOwner() {
        viewModelScope.launch {
            authRepository.observeAuthState().collectLatest { authState ->
                when (authState) {
                    AuthState.Initializing -> Unit
                    AuthState.Unauthenticated -> {
                        activeOwnerId = ""
                        draftHolder.reset()
                        _uiState.value = PersonalInfoUiState()
                    }
                    is AuthState.Authenticated -> {
                        val ownerId = authState.user.id
                        activeOwnerId = ownerId
                        val draft = draftHolder.ensureOwner(ownerId)
                        if (draft.initializedFromProfile) {
                            _uiState.update {
                                it.copy(
                                    fullName = draft.fullName,
                                    gender = draft.gender,
                                    ageText = draft.ageText,
                                    heightText = draft.heightText,
                                    weightText = draft.weightText,
                                    nameError = null,
                                    metricsError = null,
                                )
                            }
                            return@collectLatest
                        }

                        val existingProfile = userProfileRepository.getProfile(ownerId)
                        if (existingProfile != null) {
                            val formattedWeight = if (existingProfile.weightKg % 1.0 == 0.0) {
                                existingProfile.weightKg.toInt().toString()
                            } else {
                                existingProfile.weightKg.toString()
                            }
                            draftHolder.updatePersonalInfo(
                                ownerId = ownerId,
                                fullName = existingProfile.fullName,
                                gender = existingProfile.gender,
                                ageText = existingProfile.age.toString(),
                                heightText = existingProfile.heightCm.toString(),
                                weightText = formattedWeight,
                                initializedFromProfile = true,
                            )
                            draftHolder.updateGoals(
                                ownerId = ownerId,
                                fitnessGoal = existingProfile.fitnessGoal,
                                experienceLevel = existingProfile.experienceLevel,
                                weeklyWorkouts = existingProfile.weeklyWorkouts,
                            )
                            _uiState.update {
                                it.copy(
                                    fullName = existingProfile.fullName,
                                    gender = existingProfile.gender,
                                    ageText = existingProfile.age.toString(),
                                    heightText = existingProfile.heightCm.toString(),
                                    weightText = formattedWeight,
                                    nameError = null,
                                    metricsError = null,
                                )
                            }
                        } else {
                            val accountName = if (!authState.user.isGuest && draft.fullName.isBlank()) {
                                authState.user.displayName
                            } else {
                                draft.fullName
                            }
                            draftHolder.updatePersonalInfo(
                                ownerId = ownerId,
                                fullName = accountName,
                                initializedFromProfile = true,
                            )
                            _uiState.update {
                                it.copy(
                                    fullName = accountName,
                                    gender = draft.gender,
                                    ageText = draft.ageText,
                                    heightText = draft.heightText,
                                    weightText = draft.weightText,
                                    nameError = null,
                                    metricsError = null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) {
        if (activeOwnerId.isNotBlank()) {
            draftHolder.updatePersonalInfo(ownerId = activeOwnerId, fullName = value)
        }
        _uiState.update { it.copy(fullName = value, nameError = null) }
    }

    fun onGenderSelect(gender: GenderIdentity) {
        if (activeOwnerId.isNotBlank()) {
            draftHolder.updatePersonalInfo(ownerId = activeOwnerId, gender = gender)
        }
        _uiState.update { it.copy(gender = gender) }
    }

    fun onAgeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(3)
        if (activeOwnerId.isNotBlank()) {
            draftHolder.updatePersonalInfo(ownerId = activeOwnerId, ageText = digits)
        }
        _uiState.update { it.copy(ageText = digits, metricsError = null) }
    }

    fun onHeightChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(3)
        if (activeOwnerId.isNotBlank()) {
            draftHolder.updatePersonalInfo(ownerId = activeOwnerId, heightText = digits)
        }
        _uiState.update { it.copy(heightText = digits, metricsError = null) }
    }

    fun onWeightChange(value: String) {
        val filtered = buildString {
            var dotSeen = false
            for (ch in value) {
                if (ch.isDigit()) {
                    append(ch)
                } else if (ch == '.' && !dotSeen) {
                    dotSeen = true
                    append(ch)
                }
            }
        }.take(5)
        if (activeOwnerId.isNotBlank()) {
            draftHolder.updatePersonalInfo(ownerId = activeOwnerId, weightText = filtered)
        }
        _uiState.update { it.copy(weightText = filtered, metricsError = null) }
    }

    fun onImperialClicked() {
        _uiState.update {
            it.copy(imperialNotice = "Imperial units (lbs, ft/in) are coming soon. Metric is active.")
        }
    }

    fun continueToGoals() {
        val current = _uiState.value
        val nameError = if (current.fullName.trim().isEmpty()) {
            "Your name is required"
        } else {
            null
        }
        val age = current.ageText.toIntOrNull()
        val height = current.heightText.toIntOrNull()
        val weight = current.weightText.toDoubleOrNull()
        val metricsError = when {
            age == null || age !in 13..120 -> "Enter a valid age (13–120 yrs)"
            height == null || height !in 80..260 -> "Enter a valid height (80–260 cm)"
            weight == null || weight <= 0.0 || weight > 500.0 -> "Enter a valid weight (> 0 kg)"
            else -> null
        }

        if (nameError != null || metricsError != null) {
            _uiState.update { it.copy(nameError = nameError, metricsError = metricsError) }
            return
        }

        _uiState.update {
            it.copy(
                nameError = null,
                metricsError = null,
                navigateToGoals = true,
            )
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(navigateToGoals = false) }
    }
}
