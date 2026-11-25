package com.ayasheta.pulsesmartgym.core.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.PerformedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.RepRecordEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionExerciseEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.SessionPlannedSetEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.WorkoutSessionEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.relation.WorkoutSessionWithExercises
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    @Transaction
    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE ownerProfileId = :ownerId AND status = 'ACTIVE'
        ORDER BY startedAtEpochMs DESC
        LIMIT 1
        """,
    )
    fun observeActiveSession(ownerId: String): Flow<WorkoutSessionWithExercises?>

    @Transaction
    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE ownerProfileId = :ownerId AND status = 'ACTIVE'
        ORDER BY startedAtEpochMs DESC
        LIMIT 1
        """,
    )
    suspend fun getActiveSession(ownerId: String): WorkoutSessionWithExercises?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId AND ownerProfileId = :ownerId")
    fun observeSession(ownerId: String, sessionId: String): Flow<WorkoutSessionWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId AND ownerProfileId = :ownerId")
    suspend fun getSession(ownerId: String, sessionId: String): WorkoutSessionWithExercises?

    @Transaction
    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE ownerProfileId = :ownerId
          AND status IN ('COMPLETED', 'PARTIALLY_COMPLETED')
          AND (:statusFilter IS NULL OR status = :statusFilter)
        ORDER BY startedAtEpochMs DESC
        """,
    )
    fun observeHistory(
        ownerId: String,
        statusFilter: SessionStatus?,
    ): Flow<List<WorkoutSessionWithExercises>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSessionExercises(exercises: List<SessionExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSessionPlannedSets(sets: List<SessionPlannedSetEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPerformedSetIgnore(set: PerformedSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRepRecords(records: List<RepRecordEntity>)

    @Query(
        """
        SELECT * FROM performed_sets
        WHERE sessionId = :sessionId
          AND sessionExerciseId = :sessionExerciseId
          AND setIndex = :setIndex
        LIMIT 1
        """,
    )
    suspend fun findPerformedSet(
        sessionId: String,
        sessionExerciseId: String,
        setIndex: Int,
    ): PerformedSetEntity?

    @Update
    suspend fun updatePerformedSet(set: PerformedSetEntity)

    @Query("DELETE FROM rep_records WHERE performedSetId = :performedSetId")
    suspend fun deleteRepRecordsForSet(performedSetId: String)

    @Query(
        """
        UPDATE workout_sessions
        SET status = :status, endedAtEpochMs = :endedAtEpochMs
        WHERE id = :sessionId
          AND ownerProfileId = :ownerId
          AND status = 'ACTIVE'
        """,
    )
    suspend fun updateSessionStatusIfActive(
        ownerId: String,
        sessionId: String,
        status: SessionStatus,
        endedAtEpochMs: Long,
    ): Int
}
