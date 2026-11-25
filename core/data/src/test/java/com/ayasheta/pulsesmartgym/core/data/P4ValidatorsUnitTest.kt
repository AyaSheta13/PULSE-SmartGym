package com.ayasheta.pulsesmartgym.core.data

import com.ayasheta.pulsesmartgym.core.model.profile.ProfileAndAuthValidators
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class P4ValidatorsUnitTest {

    @Test
    fun emailValidator_acceptsValidEmails_andRejectsMalformed() {
        assertTrue(ProfileAndAuthValidators.isValidEmail("alex.miller@example.com"))
        assertTrue(ProfileAndAuthValidators.isValidEmail("user+gym@pulse.fit"))
        assertFalse(ProfileAndAuthValidators.isValidEmail(""))
        assertFalse(ProfileAndAuthValidators.isValidEmail("invalid-email-format"))
        assertFalse(ProfileAndAuthValidators.isValidEmail("invalid.email@"))
        assertFalse(ProfileAndAuthValidators.isValidEmail("@example.com"))
    }

    @Test
    fun strongPasswordValidator_enforcesLengthNumberAndSymbol() {
        assertTrue(ProfileAndAuthValidators.isStrongPassword("Pulse@2026!"))
        assertTrue(ProfileAndAuthValidators.isStrongPassword("Abcdef1!"))
        assertFalse(ProfileAndAuthValidators.isStrongPassword("12345"))
        assertFalse(ProfileAndAuthValidators.isStrongPassword("onlyletters"))
        assertFalse(ProfileAndAuthValidators.isStrongPassword("Letters123"))
        assertFalse(ProfileAndAuthValidators.isStrongPassword("Letters!@#"))
        assertFalse(ProfileAndAuthValidators.isStrongPassword("Ab1!"))
    }
}
