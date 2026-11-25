package com.ayasheta.pulsesmartgym.core.data

import com.ayasheta.pulsesmartgym.core.data.repository.auth.FakeAuthRepository
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAuthRepositoryTest {

    @Test
    fun fakeAuthRepository_startsUnauthenticatedAndRejectsDemoSignInInReleaseMode() = runTest {
        val releaseAuth = FakeAuthRepository(allowDemoSignIn = false)
        assertEquals(AuthState.Unauthenticated, releaseAuth.observeAuthState().first())

        val result = releaseAuth.signInDemo("Alex", "alex@pulse.fit")
        assertTrue(result is DomainResult.Failure)
        assertTrue((result as DomainResult.Failure).error is DataError.Unauthorized)
        assertEquals(AuthState.Unauthenticated, releaseAuth.observeAuthState().first())
    }

    @Test
    fun fakeAuthRepository_allowsControlledDemoSignInAndSignOutInDebugMode() = runTest {
        val debugAuth = FakeAuthRepository(allowDemoSignIn = true)
        val result = debugAuth.signInDemo("Alex", "alex@pulse.fit")
        assertTrue(result is DomainResult.Success)
        val state = debugAuth.observeAuthState().first()
        assertTrue(state is AuthState.Authenticated)
        assertTrue((state as AuthState.Authenticated).user.isDemoUser)

        debugAuth.signOut()
        assertEquals(AuthState.Unauthenticated, debugAuth.observeAuthState().first())
    }
}
