package com.ayasheta.pulsesmartgym.core.model.common

import kotlin.math.roundToLong

enum class WeightUnit {
    KG,
    LBS,
}

@JvmInline
value class WeightLoad(val grams: Long) {
    init {
        require(grams >= 0L) { "WeightLoad cannot be negative: $grams" }
    }

    fun toKilograms(): Double = grams / 1000.0

    fun toPounds(): Double = grams / GRAMS_PER_POUND

    companion object {
        private const val GRAMS_PER_POUND = 453.59237

        fun fromGrams(grams: Long): WeightLoad = WeightLoad(grams)

        fun fromKilograms(kg: Double): WeightLoad {
            require(kg >= 0.0) { "Kilograms cannot be negative: $kg" }
            return WeightLoad((kg * 1000.0).roundToLong())
        }

        fun fromPounds(lbs: Double): WeightLoad {
            require(lbs >= 0.0) { "Pounds cannot be negative: $lbs" }
            return WeightLoad((lbs * GRAMS_PER_POUND).roundToLong())
        }
    }
}

sealed interface DataError {
    data class ValidationError(val message: String) : DataError
    data class NotFound(val message: String) : DataError
    data class DuplicateSet(val sessionId: String, val sessionExerciseId: String, val setIndex: Int) : DataError
    data class InvalidSessionState(val message: String) : DataError
    data class StorageError(val message: String) : DataError
    data class Unauthorized(val message: String) : DataError

    fun userMessage(): String = when (this) {
        is ValidationError -> message
        is NotFound -> message
        is DuplicateSet -> "Set $setIndex has already been recorded."
        is InvalidSessionState -> message
        is StorageError -> message
        is Unauthorized -> message
    }
}

sealed interface DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>
    data class Failure(val error: DataError) : DomainResult<Nothing>
}

sealed interface RecordSetResult {
    data class Recorded(val performedSetId: String) : RecordSetResult
    data class AlreadyRecorded(val sessionId: String, val sessionExerciseId: String, val setIndex: Int) : RecordSetResult
    data class InvalidState(val message: String) : RecordSetResult
    data class NotFound(val message: String) : RecordSetResult
    data class ValidationError(val message: String) : RecordSetResult
}

object PulseValidators {
    fun validateOwnerId(ownerId: String): String? =
        if (ownerId.isBlank()) "Owner ID cannot be blank" else null

    fun validatePlanName(name: String): String? =
        if (name.isBlank()) "Workout plan name cannot be empty" else null

    fun validateSetIndex(setIndex: Int): String? =
        if (setIndex <= 0) "Set index must be >= 1 (1-based policy), got $setIndex" else null

    fun validateTargetReps(targetReps: Int): String? =
        if (targetReps <= 0) "Target repetitions must be > 0, got $targetReps" else null

    fun validatePerformedReps(fullReps: Int, partialReps: Int): String? = when {
        fullReps < 0 -> "Full repetitions cannot be negative: $fullReps"
        partialReps < 0 -> "Partial repetitions cannot be negative: $partialReps"
        fullReps + partialReps <= 0 -> "Performed set must record at least 1 full or partial repetition"
        else -> null
    }
}
