package com.ayasheta.pulsesmartgym.core.model.repository

import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.history.HistoryAggregateMetrics
import com.ayasheta.pulsesmartgym.core.model.history.SessionSummary
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
import com.ayasheta.pulsesmartgym.core.model.profile.UserPreferences
import com.ayasheta.pulsesmartgym.core.model.session.PerformedSet
import com.ayasheta.pulsesmartgym.core.model.session.RecordSetRequest
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlan
import com.ayasheta.pulsesmartgym.core.model.workout.WorkoutPlanDraft
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeAll(): Flow<List<ExerciseDefinition>>
    fun search(
        query: String = "",
        muscleGroup: MuscleGroup? = null,
        equipment: EquipmentCategory? = null,
        trackingCategory: TrackingCategory? = null,
    ): Flow<List<ExerciseDefinition>>
    suspend fun getById(id: String): ExerciseDefinition?
}

interface WorkoutPlanRepository {
    fun observePlans(): Flow<List<WorkoutPlan>>
    fun observePlan(planId: String): Flow<WorkoutPlan?>
    suspend fun getPlan(planId: String): WorkoutPlan?
    suspend fun createPlan(draft: WorkoutPlanDraft): DomainResult<WorkoutPlan>
    suspend fun updatePlan(planId: String, draft: WorkoutPlanDraft): DomainResult<WorkoutPlan>
    suspend fun deletePlan(planId: String): DomainResult<Unit>
}

interface WorkoutSessionRepository {
    fun observeActiveSession(): Flow<WorkoutSession?>
    fun observeSession(sessionId: String): Flow<WorkoutSession?>
    suspend fun getSession(sessionId: String): WorkoutSession?
    suspend fun startSessionFromPlan(planId: String): DomainResult<WorkoutSession>
    suspend fun recordPerformedSet(request: RecordSetRequest): com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
    suspend fun correctPerformedSet(request: RecordSetRequest): DomainResult<PerformedSet>
    suspend fun completeSession(sessionId: String): DomainResult<WorkoutSession>
    suspend fun endSessionEarly(sessionId: String): DomainResult<WorkoutSession>
}

interface WorkoutHistoryRepository {
    fun observeHistory(statusFilter: SessionStatus? = null): Flow<List<SessionSummary>>
    fun observeAggregateMetrics(): Flow<HistoryAggregateMetrics>
    suspend fun getSessionDetails(sessionId: String): WorkoutSession?
}

interface UserPreferencesRepository {
    fun observePreferences(): Flow<UserPreferences>
    suspend fun updatePreferredWeightUnit(unit: WeightUnit)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun updateDefaultRestSeconds(seconds: Int)
}

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun signInDemo(displayName: String, email: String): DomainResult<AuthUser>
    suspend fun signOut()
}
