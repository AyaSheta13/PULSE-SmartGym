package com.ayasheta.pulsesmartgym.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SplashDestination {
    data object Onboarding : SplashDestination
    data object AuthEntry : SplashDestination
    data object ProfileSetup : SplashDestination
    data object MainHome : SplashDestination
}

data class SplashUiState(
    val isResolving: Boolean = true,
    val destination: SplashDestination? = null,
)

class SplashViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val minDisplayDurationMs: Long = DEFAULT_MIN_DISPLAY_DURATION_MS,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private var hasStarted = false

    fun resolveStartupDestination() {
        if (hasStarted) return
        hasStarted = true
        viewModelScope.launch {
            val minDisplayJob = async {
                if (minDisplayDurationMs > 0L) delay(minDisplayDurationMs)
            }
            val prefs = preferencesRepository.observePreferences().first()
            val authState = authRepository.restoreSession()
            val destination = when {
                !prefs.onboardingCompleted -> SplashDestination.Onboarding
                authState is AuthState.Unauthenticated -> SplashDestination.AuthEntry
                authState is AuthState.Authenticated -> {
                    val setupDone = userProfileRepository.isProfileSetupCompleted(authState.user.id)
                    if (setupDone) SplashDestination.MainHome else SplashDestination.ProfileSetup
                }
                else -> SplashDestination.AuthEntry
            }
            minDisplayJob.await()
            _uiState.value = SplashUiState(
                isResolving = false,
                destination = destination,
            )
        }
    }

    companion object {
        const val DEFAULT_MIN_DISPLAY_DURATION_MS = 2_200L
    }
}
