package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ayasheta.pulsesmartgym.app.PulseApp
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseBottomNavigationBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.history.WeeklyProgressSummary
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.feature.home.HomeLastSessionUiModel
import com.ayasheta.pulsesmartgym.feature.home.HomePlanItemUiModel
import com.ayasheta.pulsesmartgym.feature.home.HomeScreenContent
import com.ayasheta.pulsesmartgym.feature.home.HomeUiState
import com.ayasheta.pulsesmartgym.feature.profile.ProfileModalSheet
import com.ayasheta.pulsesmartgym.feature.profile.ProfileScreenContent
import com.ayasheta.pulsesmartgym.feature.profile.ProfileUiState
import com.ayasheta.pulsesmartgym.feature.profile.edit.EditProfileScreenContent
import com.ayasheta.pulsesmartgym.feature.profile.edit.EditProfileUiState
import com.ayasheta.pulsesmartgym.navigation.AppEntryFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class P5ScreenshotCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureBaseAndResponsiveP5Screens() {
        val samplePlans = listOf(
            HomePlanItemUiModel("p1", "Upper Body Power", 4, 16),
            HomePlanItemUiModel("p2", "Lower Body Strength", 4, 16),
            HomePlanItemUiModel("p3", "Full Body Functional", 4, 12),
        )

        val populatedHome = HomeUiState.Loaded(
            ownerId = "qa_owner",
            displayName = "Alex Miller",
            avatarInitial = "A",
            welcomeOverline = "Welcome back",
            greetingTitle = "Good morning, Alex",
            currentDate = LocalDate.of(2026, 11, 25),
            featuredPlan = samplePlans.first(),
            activeSession = null,
            plans = samplePlans,
            weeklyProgress = WeeklyProgressSummary(
                completedWorkoutsCount = 3,
                partialWorkoutsCount = 0,
                weeklyTarget = 4,
                totalVolumeKg = 18450.0,
                dailyVolumeKg = listOf(2200.0, 3400.0, 0.0, 4100.0, 2850.0, 5900.0, 0.0),
                dayOfWeekNumber = 6,
                progressFraction = 0.75f,
            ),
            lastSession = HomeLastSessionUiModel(
                sessionId = "s_last",
                planName = "Upper Body Power",
                dateLabel = "OCT 24",
                totalFullReps = 142,
                durationLabel = "42m 15s",
                completedSetsCount = 16,
                isPartial = false,
            ),
            hasCompletedHistory = true,
        )

        val scenarios: List<Pair<String, @Composable () -> Unit>> = listOf(
            "01_home_no_plans.png" to {
                Box(modifier = Modifier.size(390.dp, 884.dp)) {
                    ShellPreview(selectedTab = PulseTab.HOME) {
                        HomeScreenContent(
                            uiState = HomeUiState.Loaded(
                                ownerId = "qa_owner",
                                displayName = "Alex Miller",
                                avatarInitial = "A",
                                welcomeOverline = "Welcome",
                                greetingTitle = "Good morning, Alex",
                                currentDate = LocalDate.of(2026, 11, 25),
                                featuredPlan = null,
                                activeSession = null,
                                plans = emptyList(),
                                weeklyProgress = WeeklyProgressSummary(0, 0, 4, 0.0, List(7) { 0.0 }, 1, 0f),
                                lastSession = null,
                                hasCompletedHistory = false,
                            ),
                            onAvatarClick = {},
                            onDeviceStatusClick = {},
                            onSeeAllPlansClick = {},
                            onPlanClick = {},
                            onProgressClick = {},
                            onLastSessionClick = {},
                        )
                    }
                }
            },
            "02_home_first_workout.png" to {
                Box(modifier = Modifier.size(390.dp, 884.dp)) {
                    ShellPreview(selectedTab = PulseTab.HOME) {
                        HomeScreenContent(
                            uiState = HomeUiState.Loaded(
                                ownerId = "qa_owner",
                                displayName = "Alex Miller",
                                avatarInitial = "A",
                                welcomeOverline = "Welcome",
                                greetingTitle = "Good morning, Alex",
                                currentDate = LocalDate.of(2026, 11, 25),
                                featuredPlan = samplePlans.first(),
                                activeSession = null,
                                plans = samplePlans,
                                weeklyProgress = WeeklyProgressSummary(0, 0, 4, 0.0, List(7) { 0.0 }, 1, 0f),
                                lastSession = null,
                                hasCompletedHistory = false,
                            ),
                            onAvatarClick = {},
                            onDeviceStatusClick = {},
                            onSeeAllPlansClick = {},
                            onPlanClick = {},
                            onProgressClick = {},
                            onLastSessionClick = {},
                        )
                    }
                }
            },
            "03_home_populated.png" to {
                Box(modifier = Modifier.size(390.dp, 884.dp)) {
                    ShellPreview(selectedTab = PulseTab.HOME) {
                        HomeScreenContent(
                            uiState = populatedHome,
                            onAvatarClick = {},
                            onDeviceStatusClick = {},
                            onSeeAllPlansClick = {},
                            onPlanClick = {},
                            onProgressClick = {},
                            onLastSessionClick = {},
                        )
                    }
                }
            },
            "04_profile_settings.png" to {
                Box(modifier = Modifier.size(390.dp, 884.dp)) {
                    ShellPreview(selectedTab = PulseTab.PROFILE) {
                        ProfileScreenContent(
                            uiState = sampleProfileState(activeSheet = null),
                            onEditProfileClick = {},
                            onDeviceClick = {},
                            onOpenSheet = {},
                            onDismissSheet = {},
                            onConfirmMetricUnits = {},
                            onLogoutClick = {},
                        )
                    }
                }
            },
            "05_edit_profile.png" to {
                Box(modifier = Modifier.size(390.dp, 884.dp)) {
                    EditProfileScreenContent(
                        uiState = EditProfileUiState(
                            isLoading = false,
                            hasExistingProfile = true,
                            ownerId = "qa_owner",
                            isGuest = false,
                            emailDisplay = "alex.miller@example.com",
                            fullName = "Alex Miller",
                            weightInput = "78.0",
                            heightInput = "182",
                            gender = GenderIdentity.MALE,
                            fitnessGoal = FitnessGoal.HYPERTROPHY,
                            experienceLevel = ExperienceLevel.INTERMEDIATE,
                        ),
                        onBackClick = {},
                        onFullNameChanged = {},
                        onWeightChanged = {},
                        onHeightChanged = {},
                        onGenderSelected = {},
                        onFitnessGoalSelected = {},
                        onExperienceLevelSelected = {},
                        onChangeAvatarClick = {},
                        onDismissAvatarNotice = {},
                        onSaveClick = {},
                        onDiscardClick = {},
                    )
                }
            },
            "09_home_360dp.png" to {
                Box(modifier = Modifier.size(360.dp, 800.dp)) {
                    ShellPreview(selectedTab = PulseTab.HOME) {
                        HomeScreenContent(
                            uiState = populatedHome,
                            onAvatarClick = {},
                            onDeviceStatusClick = {},
                            onSeeAllPlansClick = {},
                            onPlanClick = {},
                            onProgressClick = {},
                            onLastSessionClick = {},
                        )
                    }
                }
            },
            "10_home_412dp_font130.png" to {
                val baseDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(baseDensity.density, fontScale = 1.3f),
                ) {
                    Box(modifier = Modifier.size(412.dp, 915.dp)) {
                        ShellPreview(selectedTab = PulseTab.HOME) {
                            HomeScreenContent(
                                uiState = populatedHome,
                                onAvatarClick = {},
                                onDeviceStatusClick = {},
                                onSeeAllPlansClick = {},
                                onPlanClick = {},
                                onProgressClick = {},
                                onLastSessionClick = {},
                            )
                        }
                    }
                }
            },
            "11_profile_360dp.png" to {
                Box(modifier = Modifier.size(360.dp, 800.dp)) {
                    ShellPreview(selectedTab = PulseTab.PROFILE) {
                        ProfileScreenContent(
                            uiState = sampleProfileState(activeSheet = null),
                            onEditProfileClick = {},
                            onDeviceClick = {},
                            onOpenSheet = {},
                            onDismissSheet = {},
                            onConfirmMetricUnits = {},
                            onLogoutClick = {},
                        )
                    }
                }
            },
            "12_profile_412dp_font130.png" to {
                val baseDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(baseDensity.density, fontScale = 1.3f),
                ) {
                    Box(modifier = Modifier.size(412.dp, 915.dp)) {
                        ShellPreview(selectedTab = PulseTab.PROFILE) {
                            ProfileScreenContent(
                                uiState = sampleProfileState(activeSheet = null),
                                onEditProfileClick = {},
                                onDeviceClick = {},
                                onOpenSheet = {},
                                onDismissSheet = {},
                                onConfirmMetricUnits = {},
                                onLogoutClick = {},
                            )
                        }
                    }
                }
            },
        )

        var screenIndex by mutableIntStateOf(0)
        composeRule.setContent {
            PulseTheme {
                scenarios[screenIndex].second()
            }
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val outDir = File(context.filesDir, "p5_screenshots").apply { mkdirs() }

        scenarios.forEachIndexed { idx, (filename, _) ->
            composeRule.runOnIdle { screenIndex = idx }
            composeRule.waitForIdle()
            val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
            FileOutputStream(File(outDir, filename)).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    }

    @Test
    fun captureRealProfileSheetsWithNavigation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = PulseDataContainer(context)
        runBlocking {
            container.userPreferencesRepository.setOnboardingCompleted(true)
            val user = (container.authRepository.registerLocalAccount(
                fullName = "Alex Miller",
                email = "qa_sheets_${System.nanoTime()}@pulse.test",
                password = "Password!1",
            ) as DomainResult.Success).data
            container.userProfileRepository.saveCompletedProfile(
                ownerId = user.id,
                personalInfo = PersonalInfoDraft("Alex Miller", GenderIdentity.MALE, 28, 182, 78.0),
                goals = FitnessGoalsDraft(FitnessGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 4),
            )
        }

        composeRule.setContent {
            PulseApp(
                isDebugEntryEnabled = true,
                initialFlow = AppEntryFlow.MAIN,
                initialTab = PulseTab.PROFILE,
                dataContainer = container,
            )
        }

        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag("settings_row_privacy").assertExists()
                true
            }.getOrDefault(false)
        }

        val outDir = File(context.filesDir, "p5_screenshots").apply { mkdirs() }
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

        val sheetFlows = listOf(
            Triple("settings_row_privacy", "sheet_privacy", "06_sheet_privacy.png"),
            Triple("settings_row_units", "sheet_units", "07_sheet_units.png"),
            Triple("settings_row_account", "sheet_account_preferences", "08_sheet_account_preferences.png"),
        )

        sheetFlows.forEach { (rowTag, sheetTag, filename) ->
            composeRule.onNodeWithTag(rowTag).performClick()
            composeRule.onNodeWithTag(sheetTag).assertIsDisplayed()
            composeRule.waitForIdle()
            Thread.sleep(300)
            val bitmap = requireNotNull(uiAutomation.takeScreenshot()) {
                "uiAutomation.takeScreenshot() returned null for $filename"
            }
            val outFile = File(outDir, filename)
            FileOutputStream(outFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            bitmap.recycle()
            require(outFile.exists() && outFile.length() > 0L) {
                "Screenshot file was not written or is empty: ${outFile.absolutePath}"
            }
            composeRule.onNodeWithText(
                if (sheetTag == "sheet_units") "Confirm" else "Got It",
            ).performClick()
            composeRule.waitForIdle()
        }
    }

    @Composable
    private fun ShellPreview(
        selectedTab: PulseTab,
        content: @Composable () -> Unit,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                content()
            }
            PulseBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = {},
            )
        }
    }

    private fun sampleProfileState(activeSheet: ProfileModalSheet?) = ProfileUiState.Loaded(
        ownerId = "qa_owner",
        fullName = "Alex Miller",
        initials = "AM",
        subtitleIdentity = "alex.miller@example.com",
        membershipLabel = "Member since Oct 2026",
        isGuest = false,
        ageYears = 28,
        heightCm = 182,
        weightKg = 78.0,
        fitnessGoal = FitnessGoal.HYPERTROPHY,
        preferredWeightUnit = WeightUnit.KG,
        activeSheet = activeSheet,
    )
}
