package com.ayasheta.pulsesmartgym.core.data.repository.history

import com.ayasheta.pulsesmartgym.core.data.local.database.dao.WorkoutSessionDao
import com.ayasheta.pulsesmartgym.core.data.mapper.toDomain
import com.ayasheta.pulsesmartgym.core.model.history.ExercisePersonalRecord
import com.ayasheta.pulsesmartgym.core.model.history.HistoryAggregateMetrics
import com.ayasheta.pulsesmartgym.core.model.history.SessionSummary
import com.ayasheta.pulsesmartgym.core.model.history.WorkoutAnalytics
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutHistoryRepository
import com.ayasheta.pulsesmartgym.core.model.session.SessionStatus
import com.ayasheta.pulsesmartgym.core.model.session.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWorkoutHistoryRepository(
    private val sessionDao: WorkoutSessionDao,
) : WorkoutHistoryRepository {

    override fun observeHistory(
        ownerId: String,
        statusFilter: SessionStatus?,
    ): Flow<List<SessionSummary>> =
        sessionDao.observeHistory(ownerId, statusFilter).map { sessions ->
            sessions.map { relation ->
                WorkoutAnalytics.summarizeSession(relation.toDomain())
            }
        }

    override fun observeAggregateMetrics(ownerId: String): Flow<HistoryAggregateMetrics> =
        observeHistory(ownerId = ownerId, statusFilter = null).map { summaries ->
            WorkoutAnalytics.aggregateSummaries(summaries)
        }

    override fun observeFinishedSessions(ownerId: String): Flow<List<WorkoutSession>> =
        sessionDao.observeHistory(ownerId, null).map { sessions ->
            sessions.map { it.toDomain() }
        }

    override fun observeSessionDetails(ownerId: String, sessionId: String): Flow<WorkoutSession?> =
        sessionDao.observeSession(ownerId, sessionId).map { relation ->
            relation?.toDomain()?.takeIf {
                it.status == SessionStatus.COMPLETED || it.status == SessionStatus.PARTIALLY_COMPLETED
            }
        }

    override fun observePersonalRecords(ownerId: String): Flow<List<ExercisePersonalRecord>> =
        observeFinishedSessions(ownerId).map { sessions ->
            WorkoutAnalytics.calculatePersonalRecords(sessions)
        }

    override suspend fun getSessionDetails(ownerId: String, sessionId: String): WorkoutSession? =
        sessionDao.getSession(ownerId, sessionId)?.toDomain()?.takeIf {
            it.status == SessionStatus.COMPLETED || it.status == SessionStatus.PARTIALLY_COMPLETED
        }
}
