package com.ayasheta.pulsesmartgym.core.data.repository.auth

import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake local authentication implementation for development and testing.
 * Never performs network calls, never stores passwords or tokens, and never auto-authenticates.
 */
class FakeAuthRepository(
    private val allowDemoSignIn: Boolean,
) : AuthRepository {

    private val authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)

    override fun observeAuthState(): Flow<AuthState> = authState.asStateFlow()

    override suspend fun signInDemo(displayName: String, email: String): DomainResult<AuthUser> {
        if (!allowDemoSignIn) {
            return DomainResult.Failure(
                DataError.Unauthorized("Demo sign-in is disabled in release builds"),
            )
        }
        if (displayName.isBlank() || email.isBlank()) {
            return DomainResult.Failure(
                DataError.ValidationError("Display name and email must not be blank"),
            )
        }
        val demoUser = AuthUser(
            id = "demo_local_user",
            displayName = displayName.trim(),
            email = email.trim(),
            isDemoUser = true,
        )
        authState.value = AuthState.Authenticated(demoUser)
        return DomainResult.Success(demoUser)
    }

    override suspend fun signOut() {
        authState.value = AuthState.Unauthenticated
    }
}
