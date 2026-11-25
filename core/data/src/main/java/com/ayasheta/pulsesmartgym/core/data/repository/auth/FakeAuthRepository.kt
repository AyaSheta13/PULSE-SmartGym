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
 * Fake local authentication implementation for unit tests and debug-only demo fallback.
 * Never performs network calls, never stores passwords or tokens, and never auto-authenticates.
 */
class FakeAuthRepository(
    private val allowDemoSignIn: Boolean,
) : AuthRepository {

    private val authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)

    override fun observeAuthState(): Flow<AuthState> = authState.asStateFlow()

    override suspend fun restoreSession(): AuthState = authState.value

    override suspend fun continueAsGuest(): DomainResult<AuthUser> {
        val guest = AuthUser(
            id = "guest_test_local",
            displayName = "Guest",
            email = "",
            isDemoUser = false,
            isGuest = true,
        )
        authState.value = AuthState.Authenticated(guest)
        return DomainResult.Success(guest)
    }

    override suspend fun registerLocalAccount(
        fullName: String,
        email: String,
        password: String,
    ): DomainResult<AuthUser> = DomainResult.Failure(
        DataError.Unauthorized("Local account registration requires LocalAuthRepository"),
    )

    override suspend fun signInWithEmail(
        email: String,
        password: String,
        rememberMe: Boolean,
    ): DomainResult<AuthUser> = DomainResult.Failure(
        DataError.Unauthorized("Email sign-in requires LocalAuthRepository"),
    )

    override suspend fun resetPassword(newPassword: String): DomainResult<Unit> = DomainResult.Failure(
        DataError.Unauthorized("Password reset is unavailable while unauthenticated"),
    )

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
            isGuest = false,
        )
        authState.value = AuthState.Authenticated(demoUser)
        return DomainResult.Success(demoUser)
    }

    override suspend fun signOut() {
        authState.value = AuthState.Unauthenticated
    }
}
