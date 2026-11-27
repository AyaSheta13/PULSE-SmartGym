package com.ayasheta.pulsesmartgym.feature

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ayasheta.pulsesmartgym.R
import com.ayasheta.pulsesmartgym.core.data.local.database.ExerciseCatalogSeed
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.feature.workout.components.displayLabel
import com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes
import com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseDetailsScreenContent
import com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseDetailsUiState
import com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseGuideCatalog
import com.ayasheta.pulsesmartgym.feature.workout.picker.ExercisePickerItemUiModel
import com.ayasheta.pulsesmartgym.feature.workout.picker.ExercisePickerScreenContent
import com.ayasheta.pulsesmartgym.feature.workout.picker.ExercisePickerUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExerciseDetailsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exerciseDetailsScreen_displaysPhoto_category_bodyPart_andInstructions() {
        val benchEntity = ExerciseCatalogSeed.defaultExercises.first()
        val bench = ExerciseDefinition(
            id = benchEntity.id,
            name = benchEntity.name,
            muscleGroup = benchEntity.muscleGroup,
            equipment = benchEntity.equipment,
            trackingCategory = benchEntity.trackingCategory,
            description = benchEntity.description,
            imageKey = benchEntity.imageKey,
        )
        val guide = ExerciseGuideCatalog.forExercise(bench)

        composeRule.setContent {
            PulseTheme {
                ExerciseDetailsScreenContent(
                    uiState = ExerciseDetailsUiState.Loaded(
                        exerciseId = bench.id,
                        name = bench.name,
                        bodyPartLabel = bench.muscleGroup.displayLabel(),
                        equipmentLabel = bench.equipment.displayLabel(),
                        categoryLabel = bench.trackingCategory.displayLabel(),
                        imageRes = resolveExerciseThumbnailRes(bench.imageKey, bench.muscleGroup),
                        guide = guide,
                    ),
                    onBackClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("exercise_details_image").assertIsDisplayed()
        composeRule.onNodeWithText("Barbell Bench Press").assertIsDisplayed()
        composeRule.onNodeWithText("CHEST").assertIsDisplayed()
        composeRule.onNodeWithText("BARBELL").assertIsDisplayed()
        composeRule.onNodeWithText("LINEAR").assertIsDisplayed()
        composeRule.onNodeWithTag("exercise_details_primary_muscles").assertIsDisplayed()
        composeRule.onNodeWithTag("exercise_details_instructions").assertIsDisplayed()
    }

    @Test
    fun exercisePicker_infoButton_triggersDetailsWithoutTogglingSelection() {
        var toggledId: String? = null
        var detailsId: String? = null

        composeRule.setContent {
            PulseTheme {
                ExercisePickerScreenContent(
                    uiState = ExercisePickerUiState(
                        isLoading = false,
                        exercises = listOf(
                            ExercisePickerItemUiModel(
                                id = "ex_bench_press",
                                name = "Barbell Bench Press",
                                muscleGroup = MuscleGroup.CHEST,
                                muscleSubtitle = "Chest",
                                equipmentLabel = "Barbell",
                                thumbnailRes = R.drawable.ex_thumb_bench_press,
                                isSelected = false,
                            ),
                        ),
                    ),
                    onQueryChanged = {},
                    onFilterSelected = {},
                    onToggleExercise = { toggledId = it },
                    onBackClick = {},
                    onConfirmClick = {},
                    onExerciseDetailsClick = { detailsId = it },
                )
            }
        }

        composeRule.onNodeWithTag("picker_details_ex_bench_press").performClick()
        assertEquals("ex_bench_press", detailsId)
        assertEquals(null, toggledId)
        composeRule.onNodeWithTag("picker_toggle_ex_bench_press").performClick()
        assertEquals("ex_bench_press", toggledId)
    }
}
