package com.ayasheta.pulsesmartgym.core.data.repository.auth

import com.ayasheta.pulsesmartgym.core.data.local.database.PulseTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.LocalAccountDao
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.LocalAccountEntity
import com.ayasheta.pulsesmartgym.core.data.local.preferences.PersistedSessionKind
import com.ayasheta.pulsesmartgym.core.data.local.preferences.PulsePreferencesDataSource
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
import com.ayasheta.pulsesmartgym.core.model.profile.ProfileAndAuthValidators
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

/**
 * Local offline-first authentication repository:
 * - Guest mode: stable persisted unique ownerId (`guest_<uuid>`), usable in Release and Debug.
 * - Local accounts: stored in Room with salted PBKDF2-HMAC-SHA256 hashes (never plaintext).
 * - Demo mode: strictly restricted to [allowDemoSignIn] (Debug builds only).
 */
class LocalAuthRepository(
    private val accountDao: LocalAccountDao,
    private val preferencesDataSource: PulsePreferencesDataSource,
    private val transactionRunner: PulseTransactionRunner,
    private val allowDemoSignIn: Boolean,
) : AuthRepository {

    private val restoreMutex = Mutex()
    private val authState = MutableStateFlow<AuthState>(AuthState.Initializing)

    override fun observeAuthState(): Flow<AuthState> = authState.asStateFlow()

    override suspend fun restoreSession(): AuthState = restoreMutex.withLock {
        val current = authState.value
        if (current !is AuthState.Initializing) {
            return@withLock current
        }
        val persisted = preferencesDataSource.readPersistedSession()
        val resolved = when (persisted.kind) {
            PersistedSessionKind.NONE -> AuthState.Unauthenticated
            PersistedSessionKind.GUEST -> {
                val guestId = persisted.ownerId?.takeIf { it.isNotBlank() }
                    ?: preferencesDataSource.getOrCreateGuestOwnerId()
                AuthState.Authenticated(
                    AuthUser(
                        id = guestId,
                        displayName = "Guest",
                        email = "",
                        isDemoUser = false,
                        isGuest = true,
                    ),
                )
            }
            PersistedSessionKind.LOCAL_ACCOUNT -> {
                val accountId = persisted.ownerId
                if (!persisted.rememberMe || accountId.isNullOrBlank()) {
                    preferencesDataSource.clearActiveSession()
                    AuthState.Unauthenticated
                } else {
                    val account = accountDao.getById(accountId)
                    if (account == null) {
                        preferencesDataSource.clearActiveSession()
                        AuthState.Unauthenticated
                    } else {
                        AuthState.Authenticated(
                            AuthUser(
                                id = account.id,
                                displayName = account.displayName,
                                email = account.email,
                                isDemoUser = false,
                                isGuest = false,
                                createdAtEpochMs = account.createdAtEpochMs,
                            ),
                        )
                    }
                }
            }
        }
        authState.value = resolved
        return@withLock resolved
    }

    override suspend fun continueAsGuest(): DomainResult<AuthUser> {
        val guestOwnerId = preferencesDataSource.getOrCreateGuestOwnerId()
        val guestUser = AuthUser(
            id = guestOwnerId,
            displayName = "Guest",
            email = "",
            isDemoUser = false,
            isGuest = true,
        )
        preferencesDataSource.setActiveSession(
            kind = PersistedSessionKind.GUEST,
            ownerId = guestOwnerId,
            rememberMe = true,
        )
        authState.value = AuthState.Authenticated(guestUser)
        return DomainResult.Success(guestUser)
    }

    override suspend fun registerLocalAccount(
        fullName: String,
        email: String,
        password: String,
    ): DomainResult<AuthUser> {
        val trimmedName = fullName.trim()
        val normalizedEmail = email.trim().lowercase(Locale.US)
        if (trimmedName.isEmpty()) {
            return DomainResult.Failure(
                DataError.ValidationError("Full name is required"),
            )
        }
        if (!ProfileAndAuthValidators.isValidEmail(normalizedEmail)) {
            return DomainResult.Failure(
                DataError.ValidationError("Invalid email format"),
            )
        }
        if (!ProfileAndAuthValidators.isStrongPassword(password)) {
            return DomainResult.Failure(
                DataError.ValidationError("Weak password: Minimum 8 characters required with symbol & number."),
            )
        }

        val hashed = withContext(Dispatchers.Default) {
            PasswordHasher.hash(password)
        }
        val accountId = "user_${UUID.randomUUID()}"
        val entity = LocalAccountEntity(
            id = accountId,
            displayName = trimmedName,
            email = normalizedEmail,
            passwordSalt = hashed.saltBase64,
            passwordHash = hashed.hashBase64,
            createdAtEpochMs = System.currentTimeMillis(),
        )

        val inserted = transactionRunner.runInTransaction {
            if (accountDao.getByEmail(normalizedEmail) != null) {
                return@runInTransaction false
            }
            accountDao.insertAccount(entity)
            true
        }
        if (!inserted) {
            return DomainResult.Failure(
                DataError.ValidationError("An account with this email already exists on this device"),
            )
        }

        val user = AuthUser(
            id = accountId,
            displayName = trimmedName,
            email = normalizedEmail,
            isDemoUser = false,
            isGuest = false,
            createdAtEpochMs = entity.createdAtEpochMs,
        )
        preferencesDataSource.setActiveSession(
            kind = PersistedSessionKind.LOCAL_ACCOUNT,
            ownerId = accountId,
            rememberMe = true,
        )
        authState.value = AuthState.Authenticated(user)
        return DomainResult.Success(user)
    }

    override suspend fun signInWithEmail(
        email: String,
        password: String,
        rememberMe: Boolean,
    ): DomainResult<AuthUser> {
        val normalizedEmail = email.trim().lowercase(Locale.US)
        if (!ProfileAndAuthValidators.isValidEmail(normalizedEmail) || password.length < 8) {
            return DomainResult.Failure(
                DataError.ValidationError("Invalid email or password format"),
            )
        }

        val account = accountDao.getByEmail(normalizedEmail)
            ?: return DomainResult.Failure(
                DataError.Unauthorized("The email or password does not match any local profile on this device."),
            )

        val valid = withContext(Dispatchers.Default) {
            PasswordHasher.verify(password, account.passwordSalt, account.passwordHash)
        }
        if (!valid) {
            return DomainResult.Failure(
                DataError.Unauthorized("The email or password does not match any local profile on this device."),
            )
        }

        val user = AuthUser(
            id = account.id,
            displayName = account.displayName,
            email = account.email,
            isDemoUser = false,
            isGuest = false,
            createdAtEpochMs = account.createdAtEpochMs,
        )
        preferencesDataSource.setActiveSession(
            kind = PersistedSessionKind.LOCAL_ACCOUNT,
            ownerId = account.id,
            rememberMe = rememberMe,
        )
        authState.value = AuthState.Authenticated(user)
        return DomainResult.Success(user)
    }

    override suspend fun resetPassword(newPassword: String): DomainResult<Unit> {
        val current = authState.value
        if (current !is AuthState.Authenticated || current.user.isGuest || current.user.isDemoUser) {
            return DomainResult.Failure(
                DataError.Unauthorized(
                    "Unauthenticated password reset is disabled to protect local accounts. Remote email recovery requires a future backend.",
                ),
            )
        }
        if (!ProfileAndAuthValidators.isStrongPassword(newPassword)) {
            return DomainResult.Failure(
                DataError.ValidationError("Weak password: Minimum 8 characters required with symbol & number."),
            )
        }
        val hashed = withContext(Dispatchers.Default) {
            PasswordHasher.hash(newPassword)
        }
        val updated = accountDao.updatePasswordHash(
            accountId = current.user.id,
            salt = hashed.saltBase64,
            hash = hashed.hashBase64,
        )
        if (updated == 0) {
            return DomainResult.Failure(
                DataError.NotFound("Local account not found"),
            )
        }
        return DomainResult.Success(Unit)
    }

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
        preferencesDataSource.clearActiveSession()
        authState.value = AuthState.Unauthenticated
    }
}
