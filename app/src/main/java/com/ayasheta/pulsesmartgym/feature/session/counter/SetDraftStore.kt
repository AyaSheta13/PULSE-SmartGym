package com.ayasheta.pulsesmartgym.feature.session.counter

import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import java.util.concurrent.ConcurrentHashMap

data class SetExecutionDraft(
    val ownerId: String,
    val sessionId: String,
    val exerciseId: String,
    val setIndex: Int,
    val fullReps: Int,
    val partialReps: Int,
    val repRecords: List<RepMeasurement>,
    val actualLoad: WeightLoad?,
    val setStartedAtElapsedMs: Long,
    val hadDeviceTracking: Boolean,
)

class SetDraftStore {
    private val drafts = ConcurrentHashMap<String, SetExecutionDraft>()

    private fun key(ownerId: String, sessionId: String, exerciseId: String, setIndex: Int): String =
        "$ownerId::$sessionId::$exerciseId::$setIndex"

    fun put(draft: SetExecutionDraft) {
        drafts[key(draft.ownerId, draft.sessionId, draft.exerciseId, draft.setIndex)] = draft
    }

    fun consume(
        ownerId: String,
        sessionId: String,
        exerciseId: String,
        setIndex: Int,
    ): SetExecutionDraft? = drafts.remove(key(ownerId, sessionId, exerciseId, setIndex))

    fun clearForOtherOwners(activeOwnerId: String?) {
        if (activeOwnerId == null) {
            drafts.clear()
        } else {
            drafts.entries.removeIf { it.value.ownerId != activeOwnerId }
        }
    }

    fun clearAll() {
        drafts.clear()
    }
}
