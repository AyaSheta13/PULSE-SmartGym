package com.ayasheta.pulsesmartgym.feature

import com.ayasheta.pulsesmartgym.core.data.local.database.ExerciseCatalogSeed
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.feature.workout.components.resolveExerciseThumbnailRes
import com.ayasheta.pulsesmartgym.feature.workout.exercise.ExerciseGuideCatalog
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseGuideCatalogTest {

    @Test
    fun allSeededExercises_haveDedicatedGuide_muscles_instructions_andThumbnail() {
        val seeded = ExerciseCatalogSeed.defaultExercises.map { entity ->
            ExerciseDefinition(
                id = entity.id,
                name = entity.name,
                muscleGroup = entity.muscleGroup,
                equipment = entity.equipment,
                trackingCategory = entity.trackingCategory,
                description = entity.description,
                imageKey = entity.imageKey,
            )
        }
        assertTrue(seeded.size >= 18)

        seeded.forEach { exercise ->
            val guide = ExerciseGuideCatalog.forExercise(exercise)
            assertTrue("Missing summary for ${exercise.id}", guide.summary.isNotBlank())
            assertTrue("Missing primary muscles for ${exercise.id}", guide.primaryMuscles.isNotBlank())
            assertTrue("Missing secondary muscles for ${exercise.id}", guide.secondaryMuscles.isNotBlank())
            assertTrue("Expected >= 4 steps for ${exercise.id}", guide.instructions.size >= 4)
            assertTrue("Expected >= 2 form cues for ${exercise.id}", guide.formCues.size >= 2)
            assertNotEquals(
                "Custom fallback used unexpectedly for ${exercise.id}",
                "Core & joint stabilizers",
                guide.secondaryMuscles,
            )
            assertTrue(resolveExerciseThumbnailRes(exercise.imageKey, exercise.muscleGroup) != 0)
        }
    }

    @Test
    fun customExercise_fallsBackGracefully() {
        val custom = ExerciseDefinition(
            id = "custom_1",
            name = "Custom Goblet Squat",
            muscleGroup = MuscleGroup.LEGS,
            equipment = EquipmentCategory.DUMBBELL,
            trackingCategory = TrackingCategory.COMPOUND,
            description = null,
            imageKey = null,
        )
        val guide = ExerciseGuideCatalog.forExercise(custom)
        assertTrue(guide.summary.contains("Custom Goblet Squat"))
        assertTrue(guide.instructions.size >= 3)
    }
}
