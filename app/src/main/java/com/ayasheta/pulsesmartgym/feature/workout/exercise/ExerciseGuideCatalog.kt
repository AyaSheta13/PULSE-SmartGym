package com.ayasheta.pulsesmartgym.feature.workout.exercise

import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.feature.workout.components.displayLabel

data class ExerciseInstructionStep(
    val title: String,
    val detail: String,
)

data class ExerciseGuide(
    val exerciseId: String,
    val summary: String,
    val primaryMuscles: String,
    val secondaryMuscles: String,
    val instructions: List<ExerciseInstructionStep>,
    val formCues: List<String>,
)

object ExerciseGuideCatalog {

    private val guidesById: Map<String, ExerciseGuide> = listOf(
        ExerciseGuide(
            exerciseId = "ex_bench_press",
            summary = "Flat barbell chest press building horizontal pushing strength across the chest, front delts, and triceps.",
            primaryMuscles = "Pectoralis major (sternal), Anterior deltoids, Triceps brachii",
            secondaryMuscles = "Serratus anterior, Forearm flexors, Core stabilizers",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Lie flat on the bench with your eyes directly under the barbell, feet planted flat on the floor, and glutes firmly on the pad."),
                ExerciseInstructionStep("Grip & Unrack", "Grip slightly wider than shoulder-width with straight wrists. Pinch your shoulder blades back and down into the bench before unracking over your chest."),
                ExerciseInstructionStep("Lower with Control", "Inhale and lower the bar smoothly toward your lower sternum while keeping your elbows tucked at roughly a 45° angle."),
                ExerciseInstructionStep("Press Upward", "Lightly touch the chest without bouncing, drive your feet into the floor, and press vertically back to lockout."),
            ),
            formCues = listOf(
                "Keep shoulder blades retracted throughout the entire set.",
                "Avoid flaring elbows out to 90° to protect the shoulder joint.",
                "Keep glutes and heels planted for stable leg drive.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_cable_row",
            summary = "Seated horizontal cable pull that targets the mid-back, lats, and scapular retractors with constant cable tension.",
            primaryMuscles = "Latissimus dorsi, Rhomboids, Middle trapezius",
            secondaryMuscles = "Biceps brachii, Posterior deltoids, Erector spinae",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Attach a V-bar or close-grip handle to the low pulley, sit upright, and plant your feet firmly on the footplates with a soft knee bend."),
                ExerciseInstructionStep("Starting Posture", "Grasp the handle and sit tall with a proud chest, neutral spine, and braced core."),
                ExerciseInstructionStep("Pull & Squeeze", "Pull the handle toward your lower abdomen by driving your elbows straight back and squeezing your shoulder blades together."),
                ExerciseInstructionStep("Controlled Return", "Slowly extend your arms forward for a controlled lat stretch without rounding your lower back or rocking your torso."),
            ),
            formCues = listOf(
                "Lead with the elbows rather than curling with the biceps.",
                "Keep torso upright and avoid excessive backward lean.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_bicep_curl",
            summary = "Classic standing dumbbell curl isolating elbow flexion to build biceps brachii strength and arm size.",
            primaryMuscles = "Biceps brachii",
            secondaryMuscles = "Brachialis, Brachioradialis, Forearm flexors, Core stabilizers",
            instructions = listOf(
                ExerciseInstructionStep("Starting Position", "Stand tall with feet shoulder-width apart and hold a dumbbell in each hand at your sides with palms facing forward."),
                ExerciseInstructionStep("Pin Elbows", "Keep your elbows tucked close to your torso so your upper arms remain stationary."),
                ExerciseInstructionStep("Curl & Squeeze", "Exhale and curl the dumbbells upward toward shoulder height, contracting the biceps firmly at the top."),
                ExerciseInstructionStep("Controlled Descent", "Inhale and lower the weights smoothly back to full extension without swinging your hips."),
            ),
            formCues = listOf(
                "Keep shoulders relaxed and down away from your ears.",
                "Control the lowering phase for 2–3 seconds to prevent momentum.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_lateral_raise",
            summary = "Isolation shoulder raise targeting the lateral deltoids to build upper-body width and shoulder stability.",
            primaryMuscles = "Lateral deltoids",
            secondaryMuscles = "Anterior deltoids, Upper trapezius, Supraspinatus",
            instructions = listOf(
                ExerciseInstructionStep("Starting Position", "Stand upright with core braced and hold dumbbells at your sides with palms facing inward and a slight, fixed bend in the elbows."),
                ExerciseInstructionStep("Raise Outward", "Lead with your elbows and lift both arms out to the sides in a wide arc until your arms reach shoulder height."),
                ExerciseInstructionStep("Pause at Parallel", "Hold briefly when your arms are parallel to the floor without shrugging your traps."),
                ExerciseInstructionStep("Lower Smoothly", "Lower the dumbbells slowly back to your sides, keeping continuous tension on the deltoids."),
            ),
            formCues = listOf(
                "Stop at shoulder height rather than swinging above parallel.",
                "Keep a slight forward torso lean if comfortable for scapular plane alignment.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_back_squat",
            summary = "Foundational compound lower-body lift developing quadriceps, glute, and posterior-chain strength.",
            primaryMuscles = "Quadriceps, Gluteus maximus, Hamstrings",
            secondaryMuscles = "Spinal erectors, Rectus abdominis, Adductors, Upper back",
            instructions = listOf(
                ExerciseInstructionStep("Bar Placement & Grip", "Set the barbell at upper-chest height. Step under it, rest it across your upper traps, and pull your shoulder blades tight."),
                ExerciseInstructionStep("Unrack & Stance", "Brace your core, stand up to unrack, step back, and set feet slightly wider than shoulder-width with toes angled slightly out."),
                ExerciseInstructionStep("Controlled Descent", "Inhale, brace, and sit your hips back and down while keeping your chest lifted and knees tracking over your toes."),
                ExerciseInstructionStep("Drive Upward", "Reach parallel or slightly below, then drive evenly through mid-foot and heel to stand tall without locking knees aggressively."),
            ),
            formCues = listOf(
                "Maintain a neutral spine and braced core from start to finish.",
                "Keep heels flat on the floor and prevent knees from caving inward.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_romanian_deadlift",
            summary = "Hip-hinge barbell movement emphasizing hamstring lengthening, glute power, and spinal stability.",
            primaryMuscles = "Hamstrings, Gluteus maximus",
            secondaryMuscles = "Spinal erectors, Adductor magnus, Forearms, Upper back",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Stand with feet hip-width apart holding the barbell with an overhand grip at thigh level and a soft, fixed bend in your knees."),
                ExerciseInstructionStep("Brace & Align", "Pull your shoulders back, lift your chest, and brace your core to lock in a flat spine."),
                ExerciseInstructionStep("Hip Hinge", "Push your hips straight back behind you while sliding the bar close along your thighs and shins until you feel a deep hamstring stretch."),
                ExerciseInstructionStep("Drive Hips Forward", "Stop before your lower back rounds (typically mid-shin), then drive your hips forward and squeeze your glutes to stand upright."),
            ),
            formCues = listOf(
                "Keep the barbell grazing your legs throughout the hinge.",
                "Maintain the same slight knee angle—do not turn it into a squat.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_lat_pulldown",
            summary = "Vertical cable pull targeting the latissimus dorsi and upper back with customizable grip width.",
            primaryMuscles = "Latissimus dorsi",
            secondaryMuscles = "Biceps brachii, Teres major, Rhomboids, Posterior deltoids",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Adjust the thigh pad snugly over your legs and grasp the pulldown bar with an overhand grip slightly wider than shoulder-width."),
                ExerciseInstructionStep("Starting Posture", "Sit tall with arms extended overhead and lean back slightly (10–15°) while keeping your chest lifted."),
                ExerciseInstructionStep("Pull to Upper Chest", "Drive your elbows down and toward your sides until the bar reaches your upper chest/collarbone."),
                ExerciseInstructionStep("Controlled Return", "Pause briefly at the bottom, then control the bar smoothly back to full overhead extension."),
            ),
            formCues = listOf(
                "Pull through your elbows rather than shrugging your shoulders.",
                "Avoid leaning way back to yank the weight down.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_tricep_pushdown",
            summary = "Cable elbow-extension exercise isolating all three heads of the triceps brachii.",
            primaryMuscles = "Triceps brachii (lateral, long, and medial heads)",
            secondaryMuscles = "Forearm stabilizers, Core",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Attach a straight bar, V-bar, or rope to a high pulley and stand facing the stack with a slight forward hip hinge."),
                ExerciseInstructionStep("Lock Elbows at Sides", "Tuck your elbows tightly against your ribs; keep your upper arms motionless throughout."),
                ExerciseInstructionStep("Extend Downward", "Exhale and push the attachment downward by extending your elbows until your arms are straight."),
                ExerciseInstructionStep("Controlled Return", "Squeeze the triceps at lockout, then slowly allow forearms to rise to ~90° without letting elbows drift forward."),
            ),
            formCues = listOf(
                "Keep shoulders down and avoid leaning over the bar with body weight.",
                "Control the upward return so the weight stack never slams.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_cable_fly",
            summary = "Dual-cable chest isolation movement delivering constant tension through horizontal adduction.",
            primaryMuscles = "Pectoralis major (sternal & clavicular fibers)",
            secondaryMuscles = "Anterior deltoids, Biceps brachii (stabilizer), Core",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Set both pulleys near shoulder or upper-chest height, grab the stirrup handles, and step forward into a staggered stance."),
                ExerciseInstructionStep("Arm Arc", "Hinge slightly forward with a proud chest and open your arms wide with a slight, fixed bend in your elbows."),
                ExerciseInstructionStep("Bring Hands Together", "Exhale and sweep both handles forward in a hugging arc until your hands meet in front of your chest."),
                ExerciseInstructionStep("Stretch Under Control", "Squeeze your chest for a beat, then inhale and reverse the arc slowly until you feel a comfortable pectoral stretch."),
            ),
            formCues = listOf(
                "Keep the elbow angle constant—do not turn the fly into a press.",
                "Maintain a stable torso without swaying forward and back.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_db_flat_press",
            summary = "Flat bench dumbbell press allowing an independent, joint-friendly range of motion for chest and triceps.",
            primaryMuscles = "Pectoralis major, Anterior deltoids, Triceps brachii",
            secondaryMuscles = "Serratus anterior, Rotator cuff stabilizers, Core",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Sit on a flat bench with dumbbells resting on your thighs, then kick them up as you lie back with weights beside your chest."),
                ExerciseInstructionStep("Align & Anchor", "Retract your shoulder blades into the bench, plant your feet flat, and keep your elbows angled ~45° from your torso."),
                ExerciseInstructionStep("Press Upward", "Exhale and press the dumbbells straight up over your chest until arms are extended without clanging the weights together."),
                ExerciseInstructionStep("Lower Smoothly", "Inhale and lower the dumbbells slowly back to chest level for a deep, controlled stretch."),
            ),
            formCues = listOf(
                "Keep wrists stacked directly over your elbows.",
                "Lower with control to protect the anterior shoulder capsule.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_leg_press",
            summary = "Seated 45-degree machine press targeting the quadriceps and glutes with strong back support.",
            primaryMuscles = "Quadriceps, Gluteus maximus",
            secondaryMuscles = "Hamstrings, Adductors, Calves",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Sit with your lower back and glutes pressed flat against the backrest and place feet shoulder-width apart on the sled."),
                ExerciseInstructionStep("Release Safeties", "Press slightly to unlatch the safety handles while keeping core braced and knees aligned with toes."),
                ExerciseInstructionStep("Lower the Sled", "Bend your knees smoothly to bring the platform down toward ~90° without letting your lower back curl off the pad."),
                ExerciseInstructionStep("Press Through Mid-Foot", "Drive evenly through heels and mid-foot to extend your legs, stopping just short of a hard knee lockout."),
            ),
            formCues = listOf(
                "Never let your hips or lower back lift off the seat pad at the bottom.",
                "Keep knees tracking in line with your second and third toes.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_smith_squat",
            summary = "Guided-track squat providing vertical bar stability for focused quadriceps and glute loading.",
            primaryMuscles = "Quadriceps, Gluteus maximus",
            secondaryMuscles = "Hamstrings, Core, Spinal erectors",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Set the bar across your upper traps and place safety stops below your target squat depth."),
                ExerciseInstructionStep("Foot Placement", "Position your feet shoulder-width apart and slightly forward (3–6 inches ahead of the bar path) to align with the fixed track."),
                ExerciseInstructionStep("Unrack & Descend", "Rotate the wrists to unhook the bar, brace your core, and lower your hips until thighs reach parallel."),
                ExerciseInstructionStep("Drive & Re-Rack", "Press through your feet to stand back up smoothly, then rotate wrists to lock the bar onto the catches when finished."),
            ),
            formCues = listOf(
                "Keep torso upright and heels planted flat throughout the rep.",
                "Always set the mechanical safety catches before starting.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_db_preacher_curl",
            summary = "Supported biceps curl on an angled preacher pad that eliminates shoulder sway and maximizes elbow-flexor isolation.",
            primaryMuscles = "Biceps brachii (short head emphasis)",
            secondaryMuscles = "Brachialis, Brachioradialis",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Adjust the preacher seat so your armpits rest comfortably over the top edge of the pad and press the back of your upper arm flat."),
                ExerciseInstructionStep("Starting Grip", "Hold the dumbbell with an underhand grip and begin with the arm extended just short of hyperextension."),
                ExerciseInstructionStep("Curl Upward", "Keep your upper arm glued to the pad and curl the dumbbell up until your forearm is nearly vertical."),
                ExerciseInstructionStep("Slow Descent", "Squeeze at the top, then lower slowly under full control back to the bottom stretch without dropping abruptly."),
            ),
            formCues = listOf(
                "Do not bounce or hyperextend the elbow at the bottom.",
                "Keep the back of your upper arm pressed into the pad the entire time.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_leg_extension",
            summary = "Seated single-joint machine exercise isolating all four heads of the quadriceps.",
            primaryMuscles = "Quadriceps (rectus femoris, vastus lateralis, vastus medialis, vastus intermedius)",
            secondaryMuscles = "Tibialis anterior (minor stabilizer)",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Adjust the backrest so your knee joint aligns with the machine pivot and place the ankle pad just above your shins/ankles."),
                ExerciseInstructionStep("Anchor Torso", "Grip the side handles firmly to keep your hips and lower back pressed into the seat."),
                ExerciseInstructionStep("Extend Smoothly", "Exhale and extend your knees in a controlled arc until your legs are fully straight."),
                ExerciseInstructionStep("Pause & Lower", "Squeeze your quads briefly at the top, then lower smoothly back to ~90° without letting the stack slam."),
            ),
            formCues = listOf(
                "Avoid kicking or swinging the weight up with momentum.",
                "Keep the ankle pad resting above the foot, not on the ankle joint.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_db_incline_press",
            summary = "Incline dumbbell press emphasizing the upper (clavicular) pectorals and anterior deltoids.",
            primaryMuscles = "Pectoralis major (clavicular/upper head), Anterior deltoids",
            secondaryMuscles = "Triceps brachii, Serratus anterior",
            instructions = listOf(
                ExerciseInstructionStep("Bench Angle & Setup", "Set an incline bench to 30–45°. Sit back with head, shoulders, and glutes pressed firmly against the pad."),
                ExerciseInstructionStep("Position Weights", "Hold the dumbbells at upper-chest level with elbows tucked roughly 45° from your torso."),
                ExerciseInstructionStep("Press Overhead", "Exhale and press the dumbbells upward and slightly inward until your arms extend directly above your upper chest."),
                ExerciseInstructionStep("Lower Under Control", "Inhale and lower the weights smoothly until handles align with your upper chest/collarbone."),
            ),
            formCues = listOf(
                "Keep the bench at 30–45°—steeper angles shift load toward the shoulders.",
                "Maintain retracted shoulder blades and planted feet.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_db_shoulder_press",
            summary = "Seated vertical dumbbell press targeting the anterior and lateral deltoids with back support.",
            primaryMuscles = "Anterior deltoids, Lateral deltoids",
            secondaryMuscles = "Triceps brachii, Upper trapezius, Upper chest",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Sit upright against a supported bench with feet planted flat and lower back pressed comfortably against the pad."),
                ExerciseInstructionStep("Start at Shoulders", "Bring the dumbbells to shoulder height with palms facing forward and elbows slightly forward in the scapular plane."),
                ExerciseInstructionStep("Press Upward", "Exhale and press the weights overhead in a smooth arc until arms are extended without locking out aggressively."),
                ExerciseInstructionStep("Lower Smoothly", "Inhale and lower the dumbbells with control back to shoulder level."),
            ),
            formCues = listOf(
                "Brace your core so your lower back does not excessively arch.",
                "Avoid clanking the dumbbells together at the top.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_barbell_row",
            summary = "Compound bent-over barbell pull developing mid-back thickness, lats, and posterior-chain bracing.",
            primaryMuscles = "Latissimus dorsi, Rhomboids, Middle trapezius",
            secondaryMuscles = "Erector spinae, Biceps brachii, Posterior deltoids, Forearms",
            instructions = listOf(
                ExerciseInstructionStep("Setup & Hinge", "Stand hip-width apart over the barbell, grip slightly wider than shoulders, and hinge forward to ~45° with a flat back and soft knees."),
                ExerciseInstructionStep("Brace Core", "Engage your abs and lats to keep your torso angle locked and spine neutral."),
                ExerciseInstructionStep("Row to Lower Chest", "Drive your elbows up and back, pulling the barbell smoothly toward your lower chest or upper abdomen."),
                ExerciseInstructionStep("Lower Under Control", "Squeeze your shoulder blades at the top, then lower the bar back to arm's length without jerking your torso."),
            ),
            formCues = listOf(
                "Keep your spine flat and avoid rounding your lower back.",
                "Maintain a consistent torso angle—do not heave with your hips.",
            ),
        ),
        ExerciseGuide(
            exerciseId = "ex_calf_raise",
            summary = "Standing machine calf raise targeting the gastrocnemius through full ankle plantarflexion.",
            primaryMuscles = "Gastrocnemius",
            secondaryMuscles = "Soleus, Ankle & foot stabilizers",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Stand with the balls of your feet on the footplate and heels hanging free. Position shoulder pads snugly with knees straight (soft micro-bend)."),
                ExerciseInstructionStep("Deep Bottom Stretch", "Lower your heels slowly below the platform until you feel a thorough stretch through your calves."),
                ExerciseInstructionStep("Press Up High", "Exhale and push forcefully through the balls of your feet to rise as high onto your toes as possible."),
                ExerciseInstructionStep("Pause & Lower", "Hold peak contraction for 1–2 seconds, then lower slowly back to the bottom stretch without bouncing."),
            ),
            formCues = listOf(
                "Use a full range of motion and pause at the top and bottom.",
                "Keep knees nearly straight without bouncing with hips or knees.",
            ),
        ),
    ).associateBy { it.exerciseId }

    fun forExercise(exercise: ExerciseDefinition): ExerciseGuide {
        return guidesById[exercise.id] ?: ExerciseGuide(
            exerciseId = exercise.id,
            summary = exercise.description?.takeIf { it.isNotBlank() }
                ?: "${exercise.name} targeting ${exercise.muscleGroup.displayLabel().lowercase()} using ${exercise.equipment.displayLabel().lowercase()}.",
            primaryMuscles = exercise.muscleGroup.displayLabel(),
            secondaryMuscles = "Core & joint stabilizers",
            instructions = listOf(
                ExerciseInstructionStep("Setup", "Prepare the ${exercise.equipment.displayLabel().lowercase()} and establish a balanced, stable posture with your core braced."),
                ExerciseInstructionStep("Controlled Execution", "Perform the primary ${exercise.muscleGroup.displayLabel().lowercase()} movement smoothly through a comfortable, full range of motion."),
                ExerciseInstructionStep("Controlled Return", "Return to the starting position slowly under control while maintaining joint alignment."),
            ),
            formCues = listOf(
                "Keep your spine neutral and breathe steadily throughout each repetition.",
                "Prioritize controlled tempo over momentum.",
            ),
        )
    }
}
