package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.MainActivity
import com.ayasheta.pulsesmartgym.PulseApplication
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P11ActivityRecreationIntegrationTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var app: PulseApplication
    private lateinit var isolated: PulseDataContainer.IsolatedTestContainer

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        app = context as PulseApplication
        isolated = PulseDataContainer.createIsolatedForTesting(
            context = context,
            seedDefaultPlansOnFirstUse = true,
        )
        app.setTestDataContainerOverride(isolated.container)
    }

    @After
    fun tearDown() {
        app.setTestDataContainerOverride(null)
        isolated.close()
    }

    @Test
    fun guestAndRegisteredEntry_activityRecreation_andOwnerSwitching_preservePlansWithoutDuplication() {
        val container = isolated.container
        val guestId = runBlocking {
            val guest = (container.authRepository.continueAsGuest() as DomainResult.Success).data
            container.userProfileRepository.saveCompletedProfile(
                ownerId = guest.id,
                personalInfo = PersonalInfoDraft("Guest User", GenderIdentity.MALE, 26, 175, 74.0),
                goals = FitnessGoalsDraft(FitnessGoal.STRENGTH, ExperienceLevel.BEGINNER, 3),
            )
            container.userPreferencesRepository.setOnboardingCompleted(true)
            guest.id
        }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.waitUntil(10_000) {
                runCatching {
                    composeRule.onNodeWithContentDescription("Workouts tab").assertExists()
                    true
                }.getOrDefault(false)
            }
            composeRule.onNodeWithContentDescription("Workouts tab").performClick()
            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithText("Beginner Full Body").assertExists()
                    true
                }.getOrDefault(false)
            }
            composeRule.onNodeWithText("Upper Body Strength").assertIsDisplayed()
            composeRule.onNodeWithText("Lower Body Foundation").assertIsDisplayed()

            // Recreate MainActivity -> starter plans remain exact 3, no duplication
            scenario.recreate()
            composeRule.waitUntil(10_000) {
                runCatching {
                    composeRule.onNodeWithContentDescription("Workouts tab").assertExists()
                    true
                }.getOrDefault(false)
            }
            composeRule.onNodeWithContentDescription("Workouts tab").performClick()
            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithText("Beginner Full Body").assertExists()
                    true
                }.getOrDefault(false)
            }
            assertEquals(1, composeRule.onAllNodesWithText("Beginner Full Body").fetchSemanticsNodes().size)

            // Switch to a registered account -> gets its own 3 starter plans, edit/delete one -> switch back to Guest
            val registeredId = runBlocking {
                val account = (
                    container.authRepository.registerLocalAccount(
                        fullName = "Reg User",
                        email = "reg_switch@pulse.test",
                        password = "Password123!",
                    ) as DomainResult.Success<com.ayasheta.pulsesmartgym.core.model.profile.AuthUser>
                    ).data
                container.userProfileRepository.saveCompletedProfile(
                    ownerId = account.id,
                    personalInfo = PersonalInfoDraft("Reg User", GenderIdentity.FEMALE, 29, 166, 60.0),
                    goals = FitnessGoalsDraft(FitnessGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 4),
                )
                val regPlans = container.workoutPlanRepository.observePlans(account.id).first()
                assertEquals(3, regPlans.size)
                val toDelete = regPlans.first { it.name == "Upper Body Strength" }
                container.workoutPlanRepository.deletePlan(account.id, toDelete.id)
                assertEquals(2, container.workoutPlanRepository.observePlans(account.id).first().size)

                // Switch back to guest -> guest still has 3 plans, registered still has 2 plans
                container.authRepository.continueAsGuest()
                account.id
            }

            scenario.recreate()
            composeRule.waitUntil(10_000) {
                runCatching {
                    composeRule.onNodeWithContentDescription("Workouts tab").assertExists()
                    true
                }.getOrDefault(false)
            }
            composeRule.onNodeWithContentDescription("Workouts tab").performClick()
            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithText("Upper Body Strength").assertExists()
                    true
                }.getOrDefault(false)
            }

            runBlocking {
                assertEquals(3, container.workoutPlanRepository.observePlans(guestId).first().size)
                assertEquals(2, container.workoutPlanRepository.observePlans(registeredId).first().size)
            }
        }
    }
}
