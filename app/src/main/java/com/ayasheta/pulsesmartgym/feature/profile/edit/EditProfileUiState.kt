package com.ayasheta.pulsesmartgym.feature.profile.edit

import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val ownerId: String = "",
    val isGuest: Boolean = false,
    val emailDisplay: String = "",
    val fullName: String = "",
    val weightInput: String = "",
    val heightInput: String = "",
    val gender: GenderIdentity = GenderIdentity.MALE,
    val fitnessGoal: FitnessGoal = FitnessGoal.HYPERTROPHY,
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val preservedAge: Int = 25,
    val preservedWeeklyWorkouts: Int = 4,
    val preservedUnit: WeightUnit = WeightUnit.KG,
    val initialFullName: String = "",
    val initialWeightInput: String = "",
    val initialHeightInput: String = "",
    val initialGender: GenderIdentity = GenderIdentity.MALE,
    val initialFitnessGoal: FitnessGoal = FitnessGoal.HYPERTROPHY,
    val initialExperienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val avatarUnavailableNotice: Boolean = false,
    val validationError: String? = null,
    val saveError: String? = null,
    val isSaving: Boolean = false,
) {
    val initials: String
        get() {
            val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            return when {
                parts.isEmpty() -> "P"
                parts.size == 1 -> parts[0].first().uppercaseChar().toString()
                else -> "${parts.first().first().uppercaseChar()}${parts.last().first().uppercaseChar()}"
            }
        }

    val hasUnsavedChanges: Boolean
        get() = !isLoading && (
            fullName != initialFullName ||
                weightInput != initialWeightInput ||
                heightInput != initialHeightInput ||
                gender != initialGender ||
                fitnessGoal != initialFitnessGoal ||
                experienceLevel != initialExperienceLevel
            )
}
