package com.ayasheta.pulsesmartgym.feature.profile.edit

import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val hasExistingProfile: Boolean = false,
    val missingProfileError: String? = null,
    val ownerId: String = "",
    val isGuest: Boolean = false,
    val emailDisplay: String = "",
    val fullName: String = "",
    val weightInput: String = "",
    val heightInput: String = "",
    val gender: GenderIdentity = GenderIdentity.MALE,
    val fitnessGoal: FitnessGoal = FitnessGoal.HYPERTROPHY,
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
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
    val canSave: Boolean
        get() = hasExistingProfile && !isLoading && !isSaving

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
        get() = hasExistingProfile && !isLoading && (
            fullName != initialFullName ||
                weightInput != initialWeightInput ||
                heightInput != initialHeightInput ||
                gender != initialGender ||
                fitnessGoal != initialFitnessGoal ||
                experienceLevel != initialExperienceLevel
            )
}
