package com.ayasheta.pulsesmartgym.feature.profilesetup

import androidx.lifecycle.SavedStateHandle
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
    val ownerId: String = "",
    val fullName: String = "",
    val gender: GenderIdentity = GenderIdentity.MALE,
    val ageText: String = "",
    val heightText: String = "",
    val weightText: String = "",
    val measurementUnit: WeightUnit = WeightUnit.KG,
    val fitnessGoal: FitnessGoal = FitnessGoal.HYPERTROPHY,
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val weeklyWorkouts: Int = 4,
    val initializedFromProfile: Boolean = false,
)

/**
 * Owner-isolated profile setup draft holder.
 * Guarantees that switching accounts or signing out never leaks draft data
 * from a previous owner into another identity's setup flow.
 */
class ProfileSetupDraftHolder(
    private val savedStateHandle: SavedStateHandle? = null,
) {
    private val _state = MutableStateFlow(restoreFromSavedState())
    val state: StateFlow<ProfileSetupDraftState> = _state.asStateFlow()

    fun ensureOwner(ownerId: String): ProfileSetupDraftState {
        if (ownerId.isBlank()) return ProfileSetupDraftState()
        val current = _state.value
        if (current.ownerId == ownerId) return current
        val fresh = ProfileSetupDraftState(ownerId = ownerId)
        _state.value = fresh
        persistToSavedState(fresh)
        return fresh
    }

    fun updatePersonalInfo(
        ownerId: String,
        fullName: String? = null,
        gender: GenderIdentity? = null,
        ageText: String? = null,
        heightText: String? = null,
        weightText: String? = null,
        initializedFromProfile: Boolean? = null,
    ) {
        if (ownerId.isBlank()) return
        _state.update { current ->
            val base = if (current.ownerId == ownerId) current else ProfileSetupDraftState(ownerId = ownerId)
            base.copy(
                fullName = fullName ?: base.fullName,
                gender = gender ?: base.gender,
                ageText = ageText ?: base.ageText,
                heightText = heightText ?: base.heightText,
                weightText = weightText ?: base.weightText,
                initializedFromProfile = initializedFromProfile ?: base.initializedFromProfile,
            ).also(::persistToSavedState)
        }
    }

    fun updateGoals(
        ownerId: String,
        fitnessGoal: FitnessGoal? = null,
        experienceLevel: ExperienceLevel? = null,
        weeklyWorkouts: Int? = null,
    ) {
        if (ownerId.isBlank()) return
        _state.update { current ->
            val base = if (current.ownerId == ownerId) current else ProfileSetupDraftState(ownerId = ownerId)
            base.copy(
                fitnessGoal = fitnessGoal ?: base.fitnessGoal,
                experienceLevel = experienceLevel ?: base.experienceLevel,
                weeklyWorkouts = weeklyWorkouts ?: base.weeklyWorkouts,
            ).also(::persistToSavedState)
        }
    }

    fun toPersonalInfoDraftOrNull(ownerId: String): PersonalInfoDraft? {
        val current = _state.value
        if (current.ownerId != ownerId) return null
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

    fun reset(ownerId: String? = null) {
        val current = _state.value
        if (ownerId == null || current.ownerId == ownerId) {
            val empty = ProfileSetupDraftState()
            _state.value = empty
            persistToSavedState(empty)
        }
    }

    private fun persistToSavedState(draft: ProfileSetupDraftState) {
        savedStateHandle?.set(KEY_OWNER_ID, draft.ownerId)
        savedStateHandle?.set(KEY_FULL_NAME, draft.fullName)
        savedStateHandle?.set(KEY_GENDER, draft.gender.name)
        savedStateHandle?.set(KEY_AGE, draft.ageText)
        savedStateHandle?.set(KEY_HEIGHT, draft.heightText)
        savedStateHandle?.set(KEY_WEIGHT, draft.weightText)
        savedStateHandle?.set(KEY_GOAL, draft.fitnessGoal.name)
        savedStateHandle?.set(KEY_EXPERIENCE, draft.experienceLevel.name)
        savedStateHandle?.set(KEY_WEEKLY, draft.weeklyWorkouts)
        savedStateHandle?.set(KEY_INITIALIZED, draft.initializedFromProfile)
    }

    private fun restoreFromSavedState(): ProfileSetupDraftState {
        val handle = savedStateHandle ?: return ProfileSetupDraftState()
        val ownerId = handle.get<String>(KEY_OWNER_ID).orEmpty()
        if (ownerId.isBlank()) return ProfileSetupDraftState()
        return ProfileSetupDraftState(
            ownerId = ownerId,
            fullName = handle.get<String>(KEY_FULL_NAME).orEmpty(),
            gender = handle.get<String>(KEY_GENDER)?.let {
                runCatching { GenderIdentity.valueOf(it) }.getOrNull()
            } ?: GenderIdentity.MALE,
            ageText = handle.get<String>(KEY_AGE).orEmpty(),
            heightText = handle.get<String>(KEY_HEIGHT).orEmpty(),
            weightText = handle.get<String>(KEY_WEIGHT).orEmpty(),
            fitnessGoal = handle.get<String>(KEY_GOAL)?.let {
                runCatching { FitnessGoal.valueOf(it) }.getOrNull()
            } ?: FitnessGoal.HYPERTROPHY,
            experienceLevel = handle.get<String>(KEY_EXPERIENCE)?.let {
                runCatching { ExperienceLevel.valueOf(it) }.getOrNull()
            } ?: ExperienceLevel.INTERMEDIATE,
            weeklyWorkouts = handle.get<Int>(KEY_WEEKLY) ?: 4,
            initializedFromProfile = handle.get<Boolean>(KEY_INITIALIZED) ?: false,
        )
    }

    private companion object {
        const val KEY_OWNER_ID = "profile_draft_owner_id"
        const val KEY_FULL_NAME = "profile_draft_full_name"
        const val KEY_GENDER = "profile_draft_gender"
        const val KEY_AGE = "profile_draft_age"
        const val KEY_HEIGHT = "profile_draft_height"
        const val KEY_WEIGHT = "profile_draft_weight"
        const val KEY_GOAL = "profile_draft_goal"
        const val KEY_EXPERIENCE = "profile_draft_experience"
        const val KEY_WEEKLY = "profile_draft_weekly"
        const val KEY_INITIALIZED = "profile_draft_initialized"
    }
}
