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
    val isGuest: Boolean = false,
    val createdAtEpochMs: Long? = null,
)

sealed interface AuthState {
    data object Initializing : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
}

enum class GenderIdentity(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
}

enum class FitnessGoal(
    val title: String,
    val subtitle: String,
) {
    HYPERTROPHY(
        title = "Build Muscle & Hypertrophy",
        subtitle = "Focus on volume & muscle growth",
    ),
    STRENGTH(
        title = "Increase Strength & Power",
        subtitle = "Heavy compound lifts & progressive PRs",
    ),
    FITNESS(
        title = "Fitness & Consistency",
        subtitle = "Maintain health & consistent workout momentum",
    ),
}

enum class ExperienceLevel(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
}

data class UserProfile(
    val ownerId: String,
    val fullName: String,
    val gender: GenderIdentity,
    val age: Int,
    val heightCm: Int,
    val weightKg: Double,
    val measurementUnit: WeightUnit = WeightUnit.KG,
    val fitnessGoal: FitnessGoal,
    val experienceLevel: ExperienceLevel,
    val weeklyWorkouts: Int,
    val setupCompleted: Boolean,
    val updatedAtEpochMs: Long,
)

data class PersonalInfoDraft(
    val fullName: String,
    val gender: GenderIdentity,
    val age: Int,
    val heightCm: Int,
    val weightKg: Double,
    val measurementUnit: WeightUnit = WeightUnit.KG,
)

data class FitnessGoalsDraft(
    val fitnessGoal: FitnessGoal,
    val experienceLevel: ExperienceLevel,
    val weeklyWorkouts: Int,
)

object ProfileAndAuthValidators {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && EMAIL_REGEX.matches(trimmed)
    }

    /**
     * Enforces the password rule displayed in the Sign Up UI:
     * Minimum 8 characters with at least one number and one symbol.
     */
    fun isStrongPassword(password: String): Boolean {
        if (password.length < 8) return false
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
        return hasDigit && hasSymbol
    }
}
