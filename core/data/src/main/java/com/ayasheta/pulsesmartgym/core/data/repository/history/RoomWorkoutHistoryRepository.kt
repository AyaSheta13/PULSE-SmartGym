package com.ayasheta.pulsesmartgym.core.data.repository.history

import com.ayasheta.pulsesmartgym.core.data.local.database.dao.WorkoutSessionDao
import com.ayasheta.pulsesmartgym.core.data.mapper.toDomain
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

    override suspend fun getSessionDetails(ownerId: String, sessionId: String): WorkoutSession? =
        sessionDao.getSession(ownerId, sessionId)?.toDomain()
}
