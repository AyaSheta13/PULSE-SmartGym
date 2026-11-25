package com.ayasheta.pulsesmartgym.core.model.profile

import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit

data class UserPreferences(
    val preferredWeightUnit: WeightUnit = WeightUnit.KG,
    val onboardingCompleted: Boolean = false,
    val defaultRestSeconds: Int = 90,
)

data class AuthUser(
    val id: String,
    val displayName: String,
    val email: String,
    val isDemoUser: Boolean,
)

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
}
