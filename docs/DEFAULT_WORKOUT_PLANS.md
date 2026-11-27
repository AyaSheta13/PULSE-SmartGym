# PULSE Smart Gym — Default Starter Workout Plans

> **Disclaimer:** These starter routines are general educational templates adapted from public strength-training guidelines for healthy adults. They are **not** personalized medical, rehabilitation, or clinical training advice. Users with injuries, chronic conditions, or medical concerns should consult a qualified healthcare or exercise professional before starting resistance training.

---

## Primary Evidence Sources

1. **ACSM 2026 Resistance Training Position Stand / Evidence Synthesis**  
   - URL: https://pmc.ncbi.nlm.nih.gov/articles/PMC12965823/  
   - Key Takeaways Applied: Novices achieve meaningful strength and hypertrophy adaptations with **2–3 sets** per exercise (often starting at the lower end of weekly volume, e.g., 2 sets per movement), **≥ 2 sessions/week**, controlled full range of motion, **1–2+ minutes rest** between compound sets, and **without needing to train to momentary muscular failure**.
2. **Mayo Clinic — Weight Training: Proper Technique**  
   - URL: https://www.mayoclinic.org/healthy-lifestyle/fitness/in-depth/weight-training/art-20045842  
   - Key Takeaways Applied: Warm up for **5–10 minutes** with light aerobic activity before lifting; prioritize slow, controlled form over heavy weight; breathe steadily (never hold breath); allow **at least 48 hours (1 full day)** of recovery before training the same muscle group again.
3. **Selecting Resistance Training Exercises for Novices — Expert Consensus**  
   - URL: https://pmc.ncbi.nlm.nih.gov/articles/PMC11873903/  
   - Key Takeaways Applied: Exercise selection for novices should balance movement-pattern coverage with manageable technical/coordination demands and rely on foundational compound and supported machine/cable/dumbbell movements that can be learned safely.

---

## Starter Routine Configurations (Adapted to Existing Catalog)

> **Adaptation Note:** None of the three routines are prescribed verbatim by a single publication; each routine is an adaptation of ACSM/Mayo Clinic/Novice-Consensus programming principles mapped onto PULSE's existing **18-exercise catalog** (`ExerciseCatalogSeed.kt`). No mandatory starting weights are assigned (`targetLoad = null`) so each user selects a manageable load.

### 1. Beginner Full Body
- **Intended Experience Level:** Beginner / First-Time User
- **Suggested Weekly Frequency:** 2–3 non-consecutive days/week (at least 48h between sessions)
- **Total Working Volume:** 5 exercises, **10 working sets** (reduced from 13 sets to keep initial fatigue manageable for inexperienced lifters)
- **Movement Patterns & Exercise Order** (large multi-joint compounds first, overhead press last):
  1. **Barbell Back Squat** (`ex_back_squat`) — *Squat* — Quadriceps, Glutes, Core — **2 sets × 10 reps**, **120s rest**
  2. **DB Flat Press** (`ex_db_flat_press`) — *Horizontal Push* — Pectorals, Anterior Deltoids, Triceps — **2 sets × 10 reps**, **90s rest**
  3. **Cable Row** (`ex_cable_row`) — *Horizontal Pull* — Lats, Rhomboids, Mid-Back, Biceps — **2 sets × 10 reps**, **90s rest**
  4. **Romanian Deadlift** (`ex_romanian_deadlift`) — *Hip Hinge* — Hamstrings, Glutes, Spinal Erectors — **2 sets × 10 reps**, **120s rest**
  5. **DB Shoulder Press** (`ex_db_shoulder_press`) — *Vertical Push* — Anterior/Lateral Deltoids, Triceps — **2 sets × 12 reps**, **60s rest**
- **Rationale:** Covers all major lower- and upper-body movement patterns in a single ~18–20 minute session while keeping total set count (10 sets) approachable for beginners and providing 120s rest on the highest-demand barbell lifts.

### 2. Upper Body Strength
- **Intended Experience Level:** Beginner to Early Intermediate (Upper/Lower split option)
- **Suggested Weekly Frequency:** 1–2×/week as part of an Upper/Lower routine (≥ 48h between upper sessions)
- **Total Working Volume:** 5 exercises, **13 working sets**
- **Movement Patterns & Exercise Order** (multi-joint push/pull first, single-joint arm accessories last):
  1. **Barbell Bench Press** (`ex_bench_press`) — *Horizontal Push* — Chest, Front Delts, Triceps — **3 sets × 8 reps**, **90s rest**
  2. **Lat Pulldown** (`ex_lat_pulldown`) — *Vertical Pull* — Latissimus Dorsi, Biceps — **3 sets × 10 reps**, **90s rest**
  3. **DB Shoulder Press** (`ex_db_shoulder_press`) — *Vertical Push* — Shoulders, Triceps — **3 sets × 10 reps**, **60s rest**
  4. **Dumbbell Bicep Curl** (`ex_bicep_curl`) — *Elbow Flexion* — Biceps Brachii — **2 sets × 12 reps**, **60s rest**
  5. **Cable Tricep Pushdown** (`ex_tricep_pushdown`) — *Elbow Extension* — Triceps Brachii — **2 sets × 12 reps**, **60s rest**
- **Rationale:** Balances horizontal/vertical pushing and pulling before adding low-fatigue arm isolation work.

### 3. Lower Body Foundation
- **Intended Experience Level:** Beginner to Early Intermediate (Upper/Lower split option)
- **Suggested Weekly Frequency:** 1–2×/week as part of an Upper/Lower routine (≥ 48h between lower sessions)
- **Total Working Volume:** 4 exercises, **11 working sets**
- **Movement Patterns & Exercise Order** (compound knee-dominant and hip-hinge lifts first, machine/calf isolation last):
  1. **Leg Press** (`ex_leg_press`) — *Knee-Dominant Compound* — Quadriceps, Glutes — **3 sets × 12 reps**, **90s rest**
  2. **Romanian Deadlift** (`ex_romanian_deadlift`) — *Hip Hinge* — Hamstrings, Glutes — **3 sets × 10 reps**, **120s rest**
  3. **Leg Extension** (`ex_leg_extension`) — *Knee Extension Isolation* — Quadriceps — **2 sets × 12 reps**, **60s rest**
  4. **Standing Calf Raises** (`ex_calf_raise`) — *Ankle Plantarflexion* — Gastrocnemius, Soleus — **3 sets × 15 reps**, **60s rest**
- **Rationale:** Uses supported machine leg press alongside a foundational hip hinge to train quads, hamstrings, glutes, and calves with lower technical barrier for novices.

---

## General Beginner Training & Safety Guidance
- **Warm-Up:** Perform 5–10 minutes of light aerobic movement and 1–2 light warm-up sets before your working sets.
- **Starting Load:** Choose a light, manageable weight that lets you complete all target reps with crisp form and **2–3 reps in reserve** (avoiding unnecessary training to failure).
- **Technique First:** Move through a full, pain-free range of motion with controlled tempo; stop or reduce weight if form breaks down.
- **Rest & Recovery:** Take longer rest (90–120s+) on compound lifts whenever needed, and allow at least **48 hours** before training the same muscle group again.
- **Progression:** Only increase weight slightly (e.g., 2.5–5%) once you can comfortably complete the target sets and reps across two consecutive workouts with solid technique.
