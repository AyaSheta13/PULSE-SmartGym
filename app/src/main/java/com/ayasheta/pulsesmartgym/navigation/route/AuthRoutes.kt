package com.ayasheta.pulsesmartgym.navigation.route

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Splash : NavKey

@Serializable
data object Onboarding : NavKey

@Serializable
data object AuthEntry : NavKey

@Serializable
data object Login : NavKey

@Serializable
data object SignUp : NavKey

@Serializable
data object ForgotPassword : NavKey

@Serializable
data object ProfileSetupPersonalInfo : NavKey

@Serializable
data object ProfileSetupFitnessGoals : NavKey
