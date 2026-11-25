package com.ayasheta.pulsesmartgym.core.model.repository

import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.RecordSetResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.exercise.EquipmentCategory
import com.ayasheta.pulsesmartgym.core.model.exercise.ExerciseDefinition
import com.ayasheta.pulsesmartgym.core.model.exercise.MuscleGroup
import com.ayasheta.pulsesmartgym.core.model.exercise.TrackingCategory
import com.ayasheta.pulsesmartgym.core.model.history.HistoryAggregateMetrics
import com.ayasheta.pulsesmartgym.core.model.history.SessionSummary
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.AuthUser
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.core.model.profile.UserPreferences
import com.ayasheta.pulsesmartgym.core.model.profile.UserProfile
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
    fun observePlans(ownerId: String): Flow<List<WorkoutPlan>>
    fun observePlan(ownerId: String, planId: String): Flow<WorkoutPlan?>
    suspend fun getPlan(ownerId: String, planId: String): WorkoutPlan?
    suspend fun createPlan(ownerId: String, draft: WorkoutPlanDraft): DomainResult<WorkoutPlan>
    suspend fun updatePlan(ownerId: String, planId: String, draft: WorkoutPlanDraft): DomainResult<WorkoutPlan>
    suspend fun deletePlan(ownerId: String, planId: String): DomainResult<Unit>
}

interface WorkoutSessionRepository {
    fun observeActiveSession(ownerId: String): Flow<WorkoutSession?>
    fun observeSession(ownerId: String, sessionId: String): Flow<WorkoutSession?>
    suspend fun getSession(ownerId: String, sessionId: String): WorkoutSession?
    suspend fun startSessionFromPlan(ownerId: String, planId: String): DomainResult<WorkoutSession>
    suspend fun recordPerformedSet(ownerId: String, request: RecordSetRequest): RecordSetResult
    suspend fun correctPerformedSet(ownerId: String, request: RecordSetRequest): DomainResult<PerformedSet>
    suspend fun completeSession(ownerId: String, sessionId: String): DomainResult<WorkoutSession>
    suspend fun endSessionEarly(ownerId: String, sessionId: String): DomainResult<WorkoutSession>
}

interface WorkoutHistoryRepository {
    fun observeHistory(ownerId: String, statusFilter: SessionStatus? = null): Flow<List<SessionSummary>>
    fun observeAggregateMetrics(ownerId: String): Flow<HistoryAggregateMetrics>
    suspend fun getSessionDetails(ownerId: String, sessionId: String): WorkoutSession?
}

interface UserPreferencesRepository {
    fun observePreferences(): Flow<UserPreferences>
    suspend fun updatePreferredWeightUnit(unit: WeightUnit)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun updateDefaultRestSeconds(seconds: Int)
}

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun restoreSession(): AuthState
    suspend fun continueAsGuest(): DomainResult<AuthUser>
    suspend fun registerLocalAccount(
        fullName: String,
        email: String,
        password: String,
    ): DomainResult<AuthUser>
    suspend fun signInWithEmail(
        email: String,
        password: String,
        rememberMe: Boolean,
    ): DomainResult<AuthUser>
    suspend fun resetPassword(newPassword: String): DomainResult<Unit>
    suspend fun signInDemo(displayName: String, email: String): DomainResult<AuthUser>
    suspend fun signOut()
}

interface UserProfileRepository {
    fun observeProfile(ownerId: String): Flow<UserProfile?>
    suspend fun getProfile(ownerId: String): UserProfile?
    suspend fun saveCompletedProfile(
        ownerId: String,
        personalInfo: PersonalInfoDraft,
        goals: FitnessGoalsDraft,
    ): DomainResult<UserProfile>
    suspend fun isProfileSetupCompleted(ownerId: String): Boolean
}
