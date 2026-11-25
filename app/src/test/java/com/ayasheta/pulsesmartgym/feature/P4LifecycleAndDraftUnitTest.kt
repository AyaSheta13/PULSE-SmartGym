package com.ayasheta.pulsesmartgym.feature

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.UserPreferences
import com.ayasheta.pulsesmartgym.core.model.repository.UserPreferencesRepository
import com.ayasheta.pulsesmartgym.feature.onboarding.OnboardingViewModel
import com.ayasheta.pulsesmartgym.feature.profilesetup.ProfileSetupDraftHolder
import com.ayasheta.pulsesmartgym.navigation.NavEntryViewModelStoreRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class P4LifecycleAndDraftUnitTest {

    @Test
    fun onboardingViewModel_restoresCurrentPageFromSavedStateHandle() {
        val handle = SavedStateHandle()
        val fakePrefs = object : UserPreferencesRepository {
            private val flow = MutableStateFlow(UserPreferences())
            override fun observePreferences(): Flow<UserPreferences> = flow
            override suspend fun updatePreferredWeightUnit(unit: WeightUnit) {}
            override suspend fun setOnboardingCompleted(completed: Boolean) {}
            override suspend fun updateDefaultRestSeconds(seconds: Int) {}
        }

        val vm1 = OnboardingViewModel(handle, fakePrefs)
        assertEquals(0, vm1.uiState.value.currentPage)
        vm1.nextPage()
        assertEquals(1, vm1.uiState.value.currentPage)

        // Recreate ViewModel with the same SavedStateHandle (simulates Activity recreation / process restore)
        val vm2 = OnboardingViewModel(handle, fakePrefs)
        assertEquals(1, vm2.uiState.value.currentPage)
    }

    @Test
    fun profileSetupDraftHolder_isolatesDraftsPerOwner_andResetsOnAccountSwitch() {
        val handle = SavedStateHandle()
        val holder = ProfileSetupDraftHolder(handle)

        holder.ensureOwner("owner_a")
        holder.updatePersonalInfo(
            ownerId = "owner_a",
            fullName = "Alice",
            gender = GenderIdentity.FEMALE,
            ageText = "26",
            heightText = "168",
            weightText = "62",
            initializedFromProfile = true,
        )
        holder.updateGoals(
            ownerId = "owner_a",
            fitnessGoal = FitnessGoal.STRENGTH,
            experienceLevel = ExperienceLevel.ADVANCED,
            weeklyWorkouts = 5,
        )

        // Switching to owner_b clears owner_a's incomplete draft and never leaks Alice's data
        val ownerBDraft = holder.ensureOwner("owner_b")
        assertEquals("owner_b", ownerBDraft.ownerId)
        assertEquals("", ownerBDraft.fullName)
        assertEquals("", ownerBDraft.ageText)
        assertNull(holder.toPersonalInfoDraftOrNull("owner_a"))

        // Updating owner_b persists in SavedStateHandle for owner_b only
        holder.updatePersonalInfo(
            ownerId = "owner_b",
            fullName = "Bob",
            ageText = "31",
            heightText = "180",
            weightText = "82",
        )
        val restoredHolder = ProfileSetupDraftHolder(handle)
        assertEquals("owner_b", restoredHolder.state.value.ownerId)
        assertEquals("Bob", restoredHolder.state.value.fullName)
        assertEquals("82", restoredHolder.toPersonalInfoDraftOrNull("owner_b")?.weightKg?.toInt()?.toString())
    }

    private class SampleEntryVm(
        val onClearCallback: () -> Unit = {},
    ) : ViewModel() {
        override fun onCleared() {
            onClearCallback()
        }
    }

    @Test
    fun navEntryViewModelStoreRegistry_retainsAndClearsOnPop() {
        var cleared = false

        val registry = NavEntryViewModelStoreRegistry()
        val store = registry.getOrCreateStore("Login")
        val owner = object : androidx.lifecycle.ViewModelStoreOwner {
            override val viewModelStore = store
        }
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SampleEntryVm { cleared = true } as T
        }
        val vm = ViewModelProvider(owner, factory)[SampleEntryVm::class.java]

        // Re-fetching same key returns same ViewModel instance
        val sameOwner = object : androidx.lifecycle.ViewModelStoreOwner {
            override val viewModelStore = registry.getOrCreateStore("Login")
        }
        val sameVm = ViewModelProvider(sameOwner, factory)[SampleEntryVm::class.java]
        assertTrue(vm === sameVm)

        // Popping entry clears its ViewModelStore and triggers onCleared()
        registry.clearStore("Login")
        assertTrue(cleared)
    }
}
