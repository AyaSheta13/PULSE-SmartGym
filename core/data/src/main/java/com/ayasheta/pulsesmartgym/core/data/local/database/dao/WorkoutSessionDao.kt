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
    @Query("SELECT * FROM workout_sessions WHERE status = 'ACTIVE' ORDER BY startedAtEpochMs DESC LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun observeSession(sessionId: String): Flow<WorkoutSessionWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: String): WorkoutSessionWithExercises?

    @Transaction
    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE status IN ('COMPLETED', 'PARTIALLY_COMPLETED')
          AND (:statusFilter IS NULL OR status = :statusFilter)
        ORDER BY startedAtEpochMs DESC
        """,
    )
    fun observeHistory(statusFilter: SessionStatus?): Flow<List<WorkoutSessionWithExercises>>

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

    @Query("UPDATE workout_sessions SET status = :status, endedAtEpochMs = :endedAtEpochMs WHERE id = :sessionId")
    suspend fun updateSessionStatus(
        sessionId: String,
        status: SessionStatus,
        endedAtEpochMs: Long,
    )

    @Transaction
    suspend fun startSessionGraph(
        session: WorkoutSessionEntity,
        exercises: List<SessionExerciseEntity>,
        plannedSets: List<SessionPlannedSetEntity>,
    ) {
        insertSession(session)
        if (exercises.isNotEmpty()) insertSessionExercises(exercises)
        if (plannedSets.isNotEmpty()) insertSessionPlannedSets(plannedSets)
    }

    @Transaction
    suspend fun insertPerformedSetWithRepsIfAbsent(
        set: PerformedSetEntity,
        repRecords: List<RepRecordEntity>,
    ): Boolean {
        val rowId = insertPerformedSetIgnore(set)
        if (rowId == -1L) return false
        if (repRecords.isNotEmpty()) {
            insertRepRecords(repRecords)
        }
        return true
    }

    @Transaction
    suspend fun replacePerformedSetCorrection(
        updatedSet: PerformedSetEntity,
        repRecords: List<RepRecordEntity>,
    ) {
        updatePerformedSet(updatedSet)
        deleteRepRecordsForSet(updatedSet.id)
        if (repRecords.isNotEmpty()) {
            insertRepRecords(repRecords)
        }
    }
}
