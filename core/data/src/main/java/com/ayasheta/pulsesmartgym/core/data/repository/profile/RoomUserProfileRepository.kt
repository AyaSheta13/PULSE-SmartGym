package com.ayasheta.pulsesmartgym.core.data.repository.profile

import com.ayasheta.pulsesmartgym.core.data.local.database.PulseTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.local.database.dao.UserProfileDao
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.UserProfileEntity
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.PulseValidators
import com.ayasheta.pulsesmartgym.core.model.profile.EditProfileDraft
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import com.ayasheta.pulsesmartgym.core.model.profile.UserProfile
import com.ayasheta.pulsesmartgym.core.model.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomUserProfileRepository(
    private val userProfileDao: UserProfileDao,
    private val transactionRunner: PulseTransactionRunner,
) : UserProfileRepository {

    override fun observeProfile(ownerId: String): Flow<UserProfile?> =
        userProfileDao.observeByOwner(ownerId).map { it?.toDomain() }

    override suspend fun getProfile(ownerId: String): UserProfile? =
        userProfileDao.getByOwner(ownerId)?.toDomain()

    override suspend fun saveCompletedProfile(
        ownerId: String,
        personalInfo: PersonalInfoDraft,
        goals: FitnessGoalsDraft,
    ): DomainResult<UserProfile> {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }

        val trimmedName = personalInfo.fullName.trim()
        if (trimmedName.isEmpty()) {
            return DomainResult.Failure(DataError.ValidationError("Your name is required"))
        }
        if (personalInfo.age !in 13..120) {
            return DomainResult.Failure(DataError.ValidationError("Age must be between 13 and 120"))
        }
        if (personalInfo.heightCm !in 80..260) {
            return DomainResult.Failure(DataError.ValidationError("Height must be between 80 and 260 cm"))
        }
        if (personalInfo.weightKg <= 0.0 || personalInfo.weightKg > 500.0) {
            return DomainResult.Failure(DataError.ValidationError("Weight must be greater than 0 kg"))
        }
        if (goals.weeklyWorkouts !in 3..6) {
            return DomainResult.Failure(DataError.ValidationError("Weekly workouts must be between 3 and 6 days"))
        }

        val now = System.currentTimeMillis()
        val entity = UserProfileEntity(
            ownerId = ownerId,
            fullName = trimmedName,
            gender = personalInfo.gender,
            age = personalInfo.age,
            heightCm = personalInfo.heightCm,
            weightKg = personalInfo.weightKg,
            measurementUnit = personalInfo.measurementUnit,
            fitnessGoal = goals.fitnessGoal,
            experienceLevel = goals.experienceLevel,
            weeklyWorkouts = goals.weeklyWorkouts,
            setupCompleted = true,
            updatedAtEpochMs = now,
        )

        transactionRunner.runInTransaction {
            userProfileDao.upsertProfile(entity)
        }
        return DomainResult.Success(entity.toDomain())
    }

    override suspend fun updateExistingProfile(
        ownerId: String,
        draft: EditProfileDraft,
    ): DomainResult<UserProfile> {
        PulseValidators.validateOwnerId(ownerId)?.let {
            return DomainResult.Failure(DataError.ValidationError(it))
        }

        val trimmedName = draft.fullName.trim()
        if (trimmedName.isEmpty()) {
            return DomainResult.Failure(DataError.ValidationError("Your name is required"))
        }
        if (draft.heightCm !in 80..260) {
            return DomainResult.Failure(DataError.ValidationError("Height must be between 80 and 260 cm"))
        }
        if (!draft.weightKg.isFinite() || draft.weightKg <= 0.0 || draft.weightKg > 500.0) {
            return DomainResult.Failure(DataError.ValidationError("Weight must be greater than 0 kg"))
        }

        return transactionRunner.runInTransaction {
            val existing = userProfileDao.getByOwner(ownerId)
            if (existing == null || !existing.setupCompleted) {
                return@runInTransaction DomainResult.Failure(
                    DataError.NotFound("No completed profile exists for this account. Complete Profile Setup first."),
                )
            }
            val updated = existing.copy(
                fullName = trimmedName,
                gender = draft.gender,
                heightCm = draft.heightCm,
                weightKg = draft.weightKg,
                fitnessGoal = draft.fitnessGoal,
                experienceLevel = draft.experienceLevel,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
            userProfileDao.upsertProfile(updated)
            DomainResult.Success(updated.toDomain())
        }
    }

    override suspend fun isProfileSetupCompleted(ownerId: String): Boolean {
        if (ownerId.isBlank()) return false
        return userProfileDao.getByOwner(ownerId)?.setupCompleted == true
    }

    private fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
        ownerId = ownerId,
        fullName = fullName,
        gender = gender,
        age = age,
        heightCm = heightCm,
        weightKg = weightKg,
        measurementUnit = measurementUnit,
        fitnessGoal = fitnessGoal,
        experienceLevel = experienceLevel,
        weeklyWorkouts = weeklyWorkouts,
        setupCompleted = setupCompleted,
        updatedAtEpochMs = updatedAtEpochMs,
    )
}
