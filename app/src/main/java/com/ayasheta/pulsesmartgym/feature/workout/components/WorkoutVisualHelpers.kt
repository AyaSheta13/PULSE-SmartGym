package com.ayasheta.pulsesmartgym.feature.workout.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ayasheta.pulsesmartgym.R
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory

internal object WorkoutPalette {
    val Background = Color(0xFFFCF9F8)
    val CreateBackground = Color(0xFFFAFAFA)
    val CardSurface = Color.White
    val CardBorder = Color(0xFFF1EEEA)
    val EditorCardBorder = Color(0xFFE5E7EB)
    val Divider = Color(0xFFEDE8E3)
    val InputBackground = Color(0xFFF0EDED)
    val StepperTileBackground = Color(0xFFF2F2F5)
    val MetricTileBackground = Color(0xFFF9FAFB)
    val TextPrimary = Color(0xFF1C1B1B)
    val TextEditorPrimary = Color(0xFF1A1C24)
    val TextBrownMuted = Color(0xFF5A413B)
    val TextMuted = Color(0xFF7A7570)
    val TextSubtle = Color(0xFF9CA3B0)
    val TextPlaceholder = Color(0xFF8C8C8C)
    val PrimaryCoral = Color(0xFFFF623E)
    val PrimaryCoralDark = Color(0xFFB22C0B)
    val PrimaryCoralTint = Color(0xFFFFF1EE)
    val CountBadgeBg = Color(0xFFFFDAD2)
    val CountBadgeText = Color(0xFF3D0600)
    val EquipmentMintBg = Color(0xFF6FFBBE)
    val EquipmentMintText = Color(0xFF002113)
    val NeutralButtonBg = Color(0xFFEAE7E7)
    val InlineCreateBg = Color(0xFFF6F3F2)
    val ReadyBannerBg = Color(0x336FFBBE)
    val ReadyIconBg = Color(0xFF006C49)
    val ReadyTextPrimary = Color(0xFF003623)
    val ReadyTextSecondary = Color(0xFF005236)
    val ErrorBg = Color(0xFFFFEDEA)
    val ErrorText = Color(0xFFBA1A1A)
}

@DrawableRes
fun resolveExerciseThumbnailRes(
    imageKey: String?,
    muscleGroup: MuscleGroup? = null,
): Int = when (imageKey?.removePrefix("ex_")) {
    "bench_press" -> R.drawable.ex_thumb_bench_press
    "db_flat_press" -> R.drawable.ex_thumb_db_flat_press
    "cable_row" -> R.drawable.ex_thumb_cable_row
    "cable_fly" -> R.drawable.ex_thumb_cable_fly
    "bicep_curl" -> R.drawable.ex_thumb_bicep_curl
    "db_preacher_curl" -> R.drawable.ex_thumb_preacher_curl
    "lateral_raise" -> R.drawable.ex_thumb_lateral_raise
    "db_shoulder_press" -> R.drawable.ex_thumb_shoulder_press
    "back_squat" -> R.drawable.ex_thumb_back_squat
    "smith_squat" -> R.drawable.ex_thumb_smith_squat
    "leg_press" -> R.drawable.ex_thumb_leg_press
    "leg_extension" -> R.drawable.ex_thumb_leg_extension
    "romanian_deadlift" -> R.drawable.ex_thumb_romanian_deadlift
    "db_incline_press" -> R.drawable.ex_thumb_incline_press
    "barbell_row" -> R.drawable.ex_thumb_barbell_row
    "lat_pulldown" -> R.drawable.ex_thumb_lat_pulldown
    "calf_raise" -> R.drawable.ex_thumb_calf_raise
    "tricep_pushdown" -> R.drawable.ex_thumb_tricep_pushdown
    else -> when (muscleGroup) {
        MuscleGroup.LEGS -> R.drawable.plan_thumb_lower_body
        MuscleGroup.FULL_BODY, MuscleGroup.CORE -> R.drawable.plan_thumb_full_body
        else -> R.drawable.plan_thumb_upper_body
    }
}

fun MuscleGroup.displayLabel(): String = when (this) {
    MuscleGroup.CHEST -> "Chest"
    MuscleGroup.BACK -> "Back"
    MuscleGroup.SHOULDERS -> "Shoulders"
    MuscleGroup.BICEPS -> "Biceps"
    MuscleGroup.TRICEPS -> "Triceps"
    MuscleGroup.LEGS -> "Legs"
    MuscleGroup.CORE -> "Core"
    MuscleGroup.FULL_BODY -> "Full Body"
}

fun EquipmentCategory.displayLabel(): String = when (this) {
    EquipmentCategory.BARBELL -> "Barbell"
    EquipmentCategory.DUMBBELL -> "Dumbbells"
    EquipmentCategory.CABLE -> "Cables"
    EquipmentCategory.MACHINE -> "Machine"
    EquipmentCategory.BODYWEIGHT -> "Bodyweight"
    EquipmentCategory.OTHER -> "Other"
}

fun TrackingCategory.displayLabel(): String = when (this) {
    TrackingCategory.LINEAR -> "Linear"
    TrackingCategory.ANGULAR -> "Angular"
    TrackingCategory.COMPOUND -> "Compound"
}

fun ExerciseDefinition.trackingSupportBadge(): String =
    if (isHardwareTrackingSupported) "HARDWARE TRACKED" else "MANUAL ONLY"

/**
 * Honest P7 boundary notice when Start / Start Workout is tapped during P6.
 * Never fabricates a session or calls startSessionFromPlan().
 */
@Composable
fun StartWorkoutDeferredDialog(
    planName: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color.White)
                    .clickable(enabled = false, onClick = {})
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .testTag("dialog_start_workout_deferred"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BasicText(
                    text = "Live Workout Execution Coming in P7",
                    style = PulseTheme.typography.cardTitle.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WorkoutPalette.TextPrimary,
                    ),
                )
                BasicText(
                    text = "\"$planName\" is saved and ready. Guided set tracking, rep counting, and rest timers will be enabled in the live workout session phase.",
                    style = PulseTheme.typography.contentPrimary.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = WorkoutPalette.TextBrownMuted,
                    ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WorkoutPalette.PrimaryCoral)
                        .clickable(onClick = onDismiss)
                        .testTag("button_dismiss_p7_notice"),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Got It",
                        style = PulseTheme.typography.ctaPrimary.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
