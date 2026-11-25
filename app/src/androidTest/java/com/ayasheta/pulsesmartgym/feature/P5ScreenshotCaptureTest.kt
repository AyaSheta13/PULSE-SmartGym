package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseBottomNavigationBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.history.WeeklyProgressSummary
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.feature.home.HomeLastSessionUiModel
import com.ayasheta.pulsesmartgym.feature.home.HomePlanItemUiModel
import com.ayasheta.pulsesmartgym.feature.home.HomeScreenContent
import com.ayasheta.pulsesmartgym.feature.home.HomeUiState
import com.ayasheta.pulsesmartgym.feature.profile.ProfileModalHost
import com.ayasheta.pulsesmartgym.feature.profile.ProfileModalSheet
import com.ayasheta.pulsesmartgym.feature.profile.ProfileScreenContent
import com.ayasheta.pulsesmartgym.feature.profile.ProfileUiState
import com.ayasheta.pulsesmartgym.feature.profile.edit.EditProfileScreenContent
import com.ayasheta.pulsesmartgym.feature.profile.edit.EditProfileUiState
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
    fun captureAllP5Screens() {
        val samplePlans = listOf(
            HomePlanItemUiModel("p1", "Upper Body Power", 4, 16),
            HomePlanItemUiModel("p2", "Lower Body Strength", 4, 16),
            HomePlanItemUiModel("p3", "Full Body Functional", 4, 12),
        )

        val scenarios: List<Pair<String, @Composable () -> Unit>> = listOf(
            "01_home_no_plans.png" to {
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
            },
            "02_home_first_workout.png" to {
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
            },
            "03_home_populated.png" to {
                ShellPreview(selectedTab = PulseTab.HOME) {
                    HomeScreenContent(
                        uiState = HomeUiState.Loaded(
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
                        ),
                        onAvatarClick = {},
                        onDeviceStatusClick = {},
                        onSeeAllPlansClick = {},
                        onPlanClick = {},
                        onProgressClick = {},
                        onLastSessionClick = {},
                    )
                }
            },
            "04_profile_settings.png" to {
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
            },
            "05_edit_profile.png" to {
                EditProfileScreenContent(
                    uiState = EditProfileUiState(
                        isLoading = false,
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
            },
            "06_sheet_privacy.png" to {
                ProfileModalHost(
                    activeSheet = ProfileModalSheet.PRIVACY_AND_LOCAL_DATA,
                    isGuest = false,
                    preferredUnit = WeightUnit.KG,
                    onDismiss = {},
                    onConfirmUnits = {},
                    inWindow = true,
                )
            },
            "07_sheet_units.png" to {
                ProfileModalHost(
                    activeSheet = ProfileModalSheet.UNITS_AND_MEASUREMENTS,
                    isGuest = false,
                    preferredUnit = WeightUnit.KG,
                    onDismiss = {},
                    onConfirmUnits = {},
                    inWindow = true,
                )
            },
            "08_sheet_account_preferences.png" to {
                ProfileModalHost(
                    activeSheet = ProfileModalSheet.ACCOUNT_PREFERENCES,
                    isGuest = false,
                    preferredUnit = WeightUnit.KG,
                    onDismiss = {},
                    onConfirmUnits = {},
                    inWindow = true,
                )
            },
        )

        var screenIndex by mutableIntStateOf(0)
        composeRule.setContent {
            PulseTheme {
                Box(modifier = Modifier.size(390.dp, 884.dp)) {
                    scenarios[screenIndex].second()
                }
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
