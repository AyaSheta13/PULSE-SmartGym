package com.ayasheta.pulsesmartgym.feature

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ayasheta.pulsesmartgym.R
import com.ayasheta.pulsesmartgym.app.PulseApp
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseBottomNavigationBar
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlanExerciseDraft
import com.ayasheta.pulsesmartgym.core.model.workout.PlannedSetDraft
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import com.ayasheta.pulsesmartgym.feature.workout.editor.LocalRenderModalsInWindow
import com.ayasheta.pulsesmartgym.navigation.AppEntryFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.ayasheta.pulsesmartgym.feature.workout.details.WorkoutDetailExerciseUiModel
import com.ayasheta.pulsesmartgym.feature.workout.details.WorkoutDetailsScreenContent
import com.ayasheta.pulsesmartgym.feature.workout.details.WorkoutDetailsUiState
import com.ayasheta.pulsesmartgym.feature.workout.editor.EditorExerciseDraftItem
import com.ayasheta.pulsesmartgym.feature.workout.editor.WorkoutPlanDraftState
import com.ayasheta.pulsesmartgym.feature.workout.editor.WorkoutPlanEditorMode
import com.ayasheta.pulsesmartgym.feature.workout.editor.WorkoutPlanEditorScreenContent
import com.ayasheta.pulsesmartgym.feature.workout.editor.WorkoutPlanEditorUiState
import com.ayasheta.pulsesmartgym.feature.workout.list.MyWorkoutsScreenContent
import com.ayasheta.pulsesmartgym.feature.workout.list.MyWorkoutsUiState
import com.ayasheta.pulsesmartgym.feature.workout.list.WorkoutPlanCardUiModel
import com.ayasheta.pulsesmartgym.feature.workout.picker.ExercisePickerItemUiModel
import com.ayasheta.pulsesmartgym.feature.workout.picker.ExercisePickerScreenContent
import com.ayasheta.pulsesmartgym.feature.workout.picker.ExercisePickerUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class P6ScreenshotCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureAllP6ReferenceStates() {
        val samplePlans = listOf(
            WorkoutPlanCardUiModel(
                id = "p1",
                name = "Upper Body Power",
                description = "Chest, back, and arms strength routine",
                exerciseCount = 4,
                totalSetsCount = 16,
                estimatedMinutes = 45,
                categoryChips = listOf("Chest", "Back", "Arms"),
                thumbnailRes = R.drawable.ex_thumb_bench_press,
            ),
            WorkoutPlanCardUiModel(
                id = "p2",
                name = "Lower Body Strength",
                description = "Quads, hamstrings, and calves",
                exerciseCount = 4,
                totalSetsCount = 16,
                estimatedMinutes = 50,
                categoryChips = listOf("Quads", "Glutes", "Hamstrings"),
                thumbnailRes = R.drawable.ex_thumb_back_squat,
            ),
            WorkoutPlanCardUiModel(
                id = "p3",
                name = "Full Body Functional",
                description = "Total body compound strength",
                exerciseCount = 4,
                totalSetsCount = 12,
                estimatedMinutes = 40,
                categoryChips = listOf("Full Body", "Core", "Stamina"),
                thumbnailRes = R.drawable.ex_thumb_romanian_deadlift,
            ),
        )

        val editorExercises = listOf(
            EditorExerciseDraftItem(
                occurrenceKey = "occ_1",
                exerciseId = "ex_bench_press",
                exercise = ExerciseDefinition("ex_bench_press", "Barbell Bench Press", MuscleGroup.CHEST, EquipmentCategory.BARBELL, TrackingCategory.LINEAR, imageKey = "bench_press"),
                sets = (1..4).map { PlannedSetDraft(it, 10, null, 90) },
            ),
            EditorExerciseDraftItem(
                occurrenceKey = "occ_2",
                exerciseId = "ex_db_incline_press",
                exercise = ExerciseDefinition("ex_db_incline_press", "Incline Dumbbell Press", MuscleGroup.CHEST, EquipmentCategory.DUMBBELL, TrackingCategory.LINEAR, imageKey = "db_incline_press"),
                sets = (1..4).map { PlannedSetDraft(it, 9, null, 75) },
            ),
            EditorExerciseDraftItem(
                occurrenceKey = "occ_3",
                exerciseId = "ex_barbell_row",
                exercise = ExerciseDefinition("ex_barbell_row", "Bent-Over Barbell Row", MuscleGroup.BACK, EquipmentCategory.BARBELL, TrackingCategory.COMPOUND, imageKey = "barbell_row"),
                sets = (1..4).map { PlannedSetDraft(it, 8, null, 90) },
            ),
            EditorExerciseDraftItem(
                occurrenceKey = "occ_4",
                exerciseId = "ex_db_shoulder_press",
                exercise = ExerciseDefinition("ex_db_shoulder_press", "DB Shoulder Press", MuscleGroup.SHOULDERS, EquipmentCategory.DUMBBELL, TrackingCategory.LINEAR, imageKey = "db_shoulder_press"),
                sets = (1..4).map { PlannedSetDraft(it, 10, null, 60) },
            ),
        )

        val pickerItems = listOf(
            ExercisePickerItemUiModel("ex_cable_row", "Cable Row", MuscleGroup.BACK, "Back", "CABLES", R.drawable.ex_thumb_cable_row, false),
            ExercisePickerItemUiModel("ex_cable_fly", "Cable Fly", MuscleGroup.CHEST, "Chest", "CABLES", R.drawable.ex_thumb_cable_fly, false),
            ExercisePickerItemUiModel("ex_db_flat_press", "DB Flat Press", MuscleGroup.CHEST, "Chest", "DUMBBELLS", R.drawable.ex_thumb_bench_press, false),
            ExercisePickerItemUiModel("ex_leg_press", "Leg Press", MuscleGroup.LEGS, "Legs", "MACHINE", R.drawable.ex_thumb_leg_press, false),
            ExercisePickerItemUiModel("ex_smith_squat", "Smith Squat", MuscleGroup.LEGS, "Legs", "MACHINE", R.drawable.ex_thumb_back_squat, false),
            ExercisePickerItemUiModel("ex_bicep_curl", "Dumbbell Bicep Curl", MuscleGroup.BICEPS, "Biceps", "DUMBBELLS", R.drawable.ex_thumb_bicep_curl, false),
        )

        val detailExercises = listOf(
            WorkoutDetailExerciseUiModel("d1", 1, "ex_back_squat", "Barbell Squat", "Quads & Glutes", R.drawable.ex_thumb_back_squat, 4, "8", "120s", "4 × 8", emptyList(), false),
            WorkoutDetailExerciseUiModel("d2", 2, "ex_leg_press", "Leg Press", "Quads & Hamstrings", R.drawable.ex_thumb_leg_press, 4, "10", "90s", "4 × 10", emptyList(), false),
            WorkoutDetailExerciseUiModel("d3", 3, "ex_romanian_deadlift", "Romanian Deadlift", "Hamstrings & Glutes", R.drawable.ex_thumb_romanian_deadlift, 4, "10", "90s", "4 × 10", emptyList(), false),
            WorkoutDetailExerciseUiModel("d4", 4, "ex_calf_raise", "Standing Calf Raises", "Calves", R.drawable.ex_thumb_calf_raise, 4, "15", "60s", "4 × 15", emptyList(), false),
        )

        var currentScreen by mutableStateOf<@Composable () -> Unit>({})
        composeRule.setContent {
            PulseTheme {
                CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1f)) {
                    Box(
                        modifier = Modifier
                            .size(width = 390.dp, height = 884.dp)
                            .consumeWindowInsets(WindowInsets.systemBars),
                    ) {
                        currentScreen()
                    }
                }
            }
        }

        fun capture(fileName: String, content: @Composable () -> Unit) {
            composeRule.runOnUiThread { currentScreen = content }
            composeRule.waitForIdle()
            val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
            saveBitmap(bitmap, fileName)
        }

        capture("01_my_workouts_populated.png") {
            Column(modifier = Modifier.fillMaxSize()) {
                MyWorkoutsScreenContent(
                    uiState = MyWorkoutsUiState.Loaded("qa", samplePlans),
                    onNewPlanClick = {},
                    onPlanClick = {},
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                PulseBottomNavigationBar(selectedTab = PulseTab.WORKOUTS, onTabSelected = {})
            }
        }

        capture("02_my_workouts_empty.png") {
            Column(modifier = Modifier.fillMaxSize()) {
                MyWorkoutsScreenContent(
                    uiState = MyWorkoutsUiState.Loaded("qa", emptyList()),
                    onNewPlanClick = {},
                    onPlanClick = {},
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                PulseBottomNavigationBar(selectedTab = PulseTab.WORKOUTS, onTabSelected = {})
            }
        }

        capture("03_my_workouts_deleted.png") {
            Column(modifier = Modifier.fillMaxSize()) {
                MyWorkoutsScreenContent(
                    uiState = MyWorkoutsUiState.Loaded("qa", samplePlans.drop(1)),
                    onNewPlanClick = {},
                    onPlanClick = {},
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                PulseBottomNavigationBar(selectedTab = PulseTab.WORKOUTS, onTabSelected = {})
            }
        }

        capture("04_create_plan_empty.png") {
            WorkoutPlanEditorScreenContent(
                uiState = WorkoutPlanEditorUiState(mode = WorkoutPlanEditorMode.CREATE),
                onBackClick = {}, onDeleteClick = {}, onNameChanged = {}, onDescriptionChanged = {},
                onAddExerciseClick = {}, onRemoveExercise = {}, onAdjustSets = { _, _ -> },
                onAdjustReps = { _, _ -> }, onAdjustRest = { _, _ -> }, onToggleSetDetails = {},
                onUpdateSingleSet = { _, _, _, _, _, _ -> }, onSaveClick = {}, onCancelClick = {},
                onDismissDelete = {}, onConfirmDelete = {}, onDismissDiscard = {}, onConfirmDiscard = {},
            )
        }

        capture("05_create_plan_selected.png") {
            WorkoutPlanEditorScreenContent(
                uiState = WorkoutPlanEditorUiState(
                    mode = WorkoutPlanEditorMode.CREATE,
                    draft = WorkoutPlanDraftState("Upper Body Push", "Chest, shoulders & triceps", editorExercises.take(2)),
                ),
                onBackClick = {}, onDeleteClick = {}, onNameChanged = {}, onDescriptionChanged = {},
                onAddExerciseClick = {}, onRemoveExercise = {}, onAdjustSets = { _, _ -> },
                onAdjustReps = { _, _ -> }, onAdjustRest = { _, _ -> }, onToggleSetDetails = {},
                onUpdateSingleSet = { _, _, _, _, _, _ -> }, onSaveClick = {}, onCancelClick = {},
                onDismissDelete = {}, onConfirmDelete = {}, onDismissDiscard = {}, onConfirmDiscard = {},
            )
        }

        capture("06_exercise_picker_unselected.png") {
            ExercisePickerScreenContent(
                uiState = ExercisePickerUiState(exercises = pickerItems, selectedIds = emptyList(), isLoading = false),
                onQueryChanged = {}, onFilterSelected = {}, onToggleExercise = {}, onBackClick = {}, onConfirmClick = {},
                bottomInsetOverride = 0.dp,
            )
        }

        capture("07_exercise_picker_3_selected.png") {
            val threeSelected = pickerItems.mapIndexed { idx, item -> item.copy(isSelected = idx < 3) }
            ExercisePickerScreenContent(
                uiState = ExercisePickerUiState(
                    exercises = threeSelected,
                    selectedIds = listOf("ex_cable_row", "ex_cable_fly", "ex_db_flat_press"),
                    isLoading = false,
                ),
                onQueryChanged = {}, onFilterSelected = {}, onToggleExercise = {}, onBackClick = {}, onConfirmClick = {},
                bottomInsetOverride = 0.dp,
            )
        }

        capture("08_edit_plan_populated.png") {
            WorkoutPlanEditorScreenContent(
                uiState = WorkoutPlanEditorUiState(
                    mode = WorkoutPlanEditorMode.EDIT,
                    planId = "p1",
                    draft = WorkoutPlanDraftState("Upper Body Power", "Chest, back, and arms strength routine", editorExercises),
                ),
                onBackClick = {}, onDeleteClick = {}, onNameChanged = {}, onDescriptionChanged = {},
                onAddExerciseClick = {}, onRemoveExercise = {}, onAdjustSets = { _, _ -> },
                onAdjustReps = { _, _ -> }, onAdjustRest = { _, _ -> }, onToggleSetDetails = {},
                onUpdateSingleSet = { _, _, _, _, _, _ -> }, onSaveClick = {}, onCancelClick = {},
                onDismissDelete = {}, onConfirmDelete = {}, onDismissDiscard = {}, onConfirmDiscard = {},
            )
        }

        fun detailsState(expandedId: String?) = WorkoutDetailsUiState.Loaded(
            planId = "p2",
            categoryTag = "LOWER BODY STRENGTH",
            planName = "Lower Body Strength",
            description = null,
            exerciseCount = 4,
            totalSetsCount = 16,
            estimatedMinutes = 50,
            exercises = detailExercises,
            expandedOccurrenceId = expandedId,
        )

        capture("09_workout_details_all_collapsed.png") {
            WorkoutDetailsScreenContent(detailsState(null), onBackClick = {}, onToggleExercise = {}, onEditPlanClick = {})
        }
        capture("10_workout_details_ex1_expanded.png") {
            WorkoutDetailsScreenContent(detailsState("d1"), onBackClick = {}, onToggleExercise = {}, onEditPlanClick = {})
        }
        capture("11_workout_details_ex2_expanded.png") {
            WorkoutDetailsScreenContent(detailsState("d2"), onBackClick = {}, onToggleExercise = {}, onEditPlanClick = {})
        }
        capture("12_workout_details_ex3_expanded.png") {
            WorkoutDetailsScreenContent(detailsState("d3"), onBackClick = {}, onToggleExercise = {}, onEditPlanClick = {})
        }
        capture("13_workout_details_ex4_expanded.png") {
            WorkoutDetailsScreenContent(detailsState("d4"), onBackClick = {}, onToggleExercise = {}, onEditPlanClick = {})
        }
    }

    @Test
    fun captureRealDeleteModalOverEditPlan() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        withIsolatedScreenshotContainer(context) { container ->
            val createdPlanId = seedIsolatedDeleteModalFixture(container)

            composeRule.setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(density = 1f, fontScale = 1f),
                    LocalRenderModalsInWindow provides true,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 390.dp, height = 884.dp)
                            .consumeWindowInsets(WindowInsets.systemBars),
                    ) {
                        PulseApp(
                            isDebugEntryEnabled = true,
                            initialFlow = AppEntryFlow.MAIN,
                            initialTab = PulseTab.WORKOUTS,
                            dataContainer = container,
                        )
                    }
                }
            }

            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithTag("workout_plan_card_$createdPlanId").assertExists()
                    true
                }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("workout_plan_card_$createdPlanId").performClick()

            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithTag("details_edit_plan_button").assertExists()
                    true
                }.getOrDefault(false)
            }
            composeRule.onNodeWithTag("details_edit_plan_button").performClick()

            composeRule.waitUntil(5_000) {
                runCatching {
                    composeRule.onNodeWithText("Edit Workout Plan").assertExists()
                    true
                }.getOrDefault(false)
            }
            composeRule.onNodeWithContentDescription("Delete").performClick()
            composeRule.onNodeWithTag("modal_delete_plan").assertIsDisplayed()
            composeRule.waitForIdle()

            val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
            saveBitmap(bitmap, "14_delete_plan_confirmation.png")
        }
    }

    @Test
    fun screenshotSetupAndTeardown_preservesExistingTargetAppData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val targetContainer = PulseDataContainer(context)
        val uniqueEmail = "qa_preserve_${System.currentTimeMillis()}@pulse.local"

        val (targetOwnerId, baselinePlanId, baselineAuth) = runBlocking {
            targetContainer.userPreferencesRepository.setOnboardingCompleted(true)
            val account = (targetContainer.authRepository.registerLocalAccount(
                fullName = "Target User",
                email = uniqueEmail,
                password = "Password123!",
            ) as DomainResult.Success).data
            targetContainer.userProfileRepository.saveCompletedProfile(
                ownerId = account.id,
                personalInfo = PersonalInfoDraft("Target User", GenderIdentity.FEMALE, 29, 168, 62.0),
                goals = FitnessGoalsDraft(FitnessGoal.HYPERTROPHY, ExperienceLevel.BEGINNER, 3),
            )
            targetContainer.exerciseRepository.observeAll().first()
            val plan = (targetContainer.workoutPlanRepository.createPlan(
                account.id,
                WorkoutPlanDraft(
                    name = "Target Existing Plan",
                    description = "Must remain untouched",
                    exercises = listOf(
                        PlanExerciseDraft(
                            exerciseId = "ex_bench_press",
                            orderIndex = 0,
                            plannedSets = (1..3).map { PlannedSetDraft(it, 8, null, 90) },
                        ),
                    ),
                ),
            ) as DomainResult.Success).data
            Triple(account.id, plan.id, targetContainer.authRepository.restoreSession())
        }

        try {
            withIsolatedScreenshotContainer(context) { isolated ->
                seedIsolatedDeleteModalFixture(isolated)
                runBlocking {
                    isolated.workoutPlanRepository.observePlans(
                        (isolated.authRepository.restoreSession() as AuthState.Authenticated).user.id,
                    ).first().forEach {
                        isolated.workoutPlanRepository.deletePlan(
                            (isolated.authRepository.restoreSession() as AuthState.Authenticated).user.id,
                            it.id,
                        )
                    }
                }
            }

            runBlocking {
                val afterAuth = targetContainer.authRepository.restoreSession()
                val afterProfile = targetContainer.userProfileRepository.observeProfile(targetOwnerId).first()
                val afterPlans = targetContainer.workoutPlanRepository.observePlans(targetOwnerId).first()

                org.junit.Assert.assertEquals(baselineAuth, afterAuth)
                org.junit.Assert.assertEquals("Target User", afterProfile?.fullName)
                org.junit.Assert.assertEquals(listOf(baselinePlanId), afterPlans.map { it.id })
                org.junit.Assert.assertEquals("Target Existing Plan", afterPlans.single().name)
            }
        } finally {
            runBlocking {
                targetContainer.workoutPlanRepository.deletePlan(targetOwnerId, baselinePlanId)
                targetContainer.authRepository.signOut()
            }
        }
    }

    private fun seedIsolatedDeleteModalFixture(container: PulseDataContainer): String = runBlocking {
        container.userPreferencesRepository.setOnboardingCompleted(true)
        val guest = (container.authRepository.continueAsGuest() as DomainResult.Success).data
        container.userProfileRepository.saveCompletedProfile(
            ownerId = guest.id,
            personalInfo = PersonalInfoDraft("Alex", GenderIdentity.MALE, 26, 178, 76.0),
            goals = FitnessGoalsDraft(FitnessGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 4),
        )
        container.exerciseRepository.observeAll().first()
        val created = container.workoutPlanRepository.createPlan(
            guest.id,
            WorkoutPlanDraft(
                name = "Upper Body Power",
                description = "Chest, back, and arms strength routine",
                exercises = listOf(
                    PlanExerciseDraft(
                        exerciseId = "ex_bench_press",
                        orderIndex = 0,
                        plannedSets = (1..4).map { PlannedSetDraft(it, 10, null, 90) },
                    ),
                ),
            ),
        ) as DomainResult.Success
        created.data.id
    }

    private inline fun withIsolatedScreenshotContainer(
        context: Context,
        block: (PulseDataContainer) -> Unit,
    ) {
        PulseDataContainer.createIsolatedForTesting(context).use { isolated ->
            block(isolated.container)
        }
    }

    private fun saveBitmap(bitmap: Bitmap, fileName: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = File(context.filesDir, "p6_screenshots").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        require(file.exists() && file.length() > 0L) {
            "Captured screenshot $fileName was missing or empty"
        }
    }
}
