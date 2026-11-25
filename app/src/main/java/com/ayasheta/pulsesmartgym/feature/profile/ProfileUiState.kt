package com.ayasheta.pulsesmartgym.feature.profile

import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal

enum class ProfileModalSheet {
    ACCOUNT_PREFERENCES,
    UNITS_AND_MEASUREMENTS,
    PRIVACY_AND_LOCAL_DATA,
    WORKOUT_REMINDERS_COMING_SOON,
}

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data object SignedOut : ProfileUiState

    data class Error(val message: String) : ProfileUiState

    data class Loaded(
        val ownerId: String,
        val fullName: String,
        val initials: String,
        val subtitleIdentity: String,
        val membershipLabel: String,
        val isGuest: Boolean,
        val ageYears: Int?,
        val heightCm: Int?,
        val weightKg: Double?,
        val fitnessGoal: FitnessGoal?,
        val preferredWeightUnit: WeightUnit,
        val activeSheet: ProfileModalSheet? = null,
    ) : ProfileUiState
}
