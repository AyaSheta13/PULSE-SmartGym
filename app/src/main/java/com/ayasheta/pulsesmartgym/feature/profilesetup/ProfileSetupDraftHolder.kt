package com.ayasheta.pulsesmartgym.feature.profilesetup

import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProfileSetupDraftState(
    val fullName: String = "",
    val gender: GenderIdentity = GenderIdentity.MALE,
    val ageText: String = "",
    val heightText: String = "",
    val weightText: String = "",
    val measurementUnit: WeightUnit = WeightUnit.KG,
    val fitnessGoal: FitnessGoal = FitnessGoal.HYPERTROPHY,
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val weeklyWorkouts: Int = 4,
    val loadedForOwnerId: String? = null,
)

class ProfileSetupDraftHolder {
    private val _state = MutableStateFlow(ProfileSetupDraftState())
    val state: StateFlow<ProfileSetupDraftState> = _state.asStateFlow()

    fun updatePersonalInfo(
        fullName: String? = null,
        gender: GenderIdentity? = null,
        ageText: String? = null,
        heightText: String? = null,
        weightText: String? = null,
        loadedForOwnerId: String? = null,
    ) {
        _state.update { current ->
            current.copy(
                fullName = fullName ?: current.fullName,
                gender = gender ?: current.gender,
                ageText = ageText ?: current.ageText,
                heightText = heightText ?: current.heightText,
                weightText = weightText ?: current.weightText,
                loadedForOwnerId = loadedForOwnerId ?: current.loadedForOwnerId,
            )
        }
    }

    fun updateGoals(
        fitnessGoal: FitnessGoal? = null,
        experienceLevel: ExperienceLevel? = null,
        weeklyWorkouts: Int? = null,
    ) {
        _state.update { current ->
            current.copy(
                fitnessGoal = fitnessGoal ?: current.fitnessGoal,
                experienceLevel = experienceLevel ?: current.experienceLevel,
                weeklyWorkouts = weeklyWorkouts ?: current.weeklyWorkouts,
            )
        }
    }

    fun toPersonalInfoDraftOrNull(): PersonalInfoDraft? {
        val current = _state.value
        val age = current.ageText.trim().toIntOrNull() ?: return null
        val heightCm = current.heightText.trim().toIntOrNull() ?: return null
        val weightKg = current.weightText.trim().toDoubleOrNull() ?: return null
        return PersonalInfoDraft(
            fullName = current.fullName.trim(),
            gender = current.gender,
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            measurementUnit = current.measurementUnit,
        )
    }

    fun reset() {
        _state.value = ProfileSetupDraftState()
    }
}
