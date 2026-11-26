package com.ayasheta.pulsesmartgym.feature.session.counter

import com.ayasheta.pulsesmartgym.core.model.common.WeightLoad
import com.ayasheta.pulsesmartgym.core.model.device.ExecutionEnvironmentKind
import com.ayasheta.pulsesmartgym.core.model.device.MeasurementOrigin
import com.ayasheta.pulsesmartgym.core.model.device.RepClassification
import com.ayasheta.pulsesmartgym.core.model.device.RepMeasurement
import com.ayasheta.pulsesmartgym.core.model.device.TrackingMode
import java.util.concurrent.ConcurrentHashMap

data class SetExecutionDraft(
    val ownerId: String,
    val environment: ExecutionEnvironmentKind = ExecutionEnvironmentKind.GENUINE,
    val sessionId: String,
    val exerciseId: String,
    val setIndex: Int,
    val trackingWindowId: String = "${sessionId}_${exerciseId}_${setIndex}",
    val fullReps: Int,
    val partialReps: Int,
    val repRecords: List<RepMeasurement>,
    val acceptedEventIds: Set<String> = emptySet(),
    val origin: MeasurementOrigin = MeasurementOrigin.MANUAL_INPUT,
    val actualLoad: WeightLoad?,
    val hasInitializedLoad: Boolean = true,
    val setStartedAtElapsedMs: Long,
    val trackingMode: TrackingMode = if (repRecords.isNotEmpty()) TrackingMode.DEVICE_ASSISTED else TrackingMode.MANUAL,
    val hadDeviceTracking: Boolean,
    val isManualFallback: Boolean = false,
)

class SetDraftStore {
    private val drafts = ConcurrentHashMap<String, SetExecutionDraft>()

    private fun key(
        ownerId: String,
        environment: ExecutionEnvironmentKind,
        sessionId: String,
        exerciseId: String,
        setIndex: Int,
    ): String = "$ownerId::${environment.name}::$sessionId::$exerciseId::$setIndex"

    fun put(draft: SetExecutionDraft) {
        drafts[key(draft.ownerId, draft.environment, draft.sessionId, draft.exerciseId, draft.setIndex)] = draft
    }

    fun peek(
        ownerId: String,
        environment: ExecutionEnvironmentKind,
        sessionId: String,
        exerciseId: String,
        setIndex: Int,
    ): SetExecutionDraft? = drafts[key(ownerId, environment, sessionId, exerciseId, setIndex)]

    fun consume(
        ownerId: String,
        environment: ExecutionEnvironmentKind,
        sessionId: String,
        exerciseId: String,
        setIndex: Int,
    ): SetExecutionDraft? = drafts.remove(key(ownerId, environment, sessionId, exerciseId, setIndex))

    fun clearForEnvironment(environment: ExecutionEnvironmentKind) {
        drafts.entries.removeIf { it.value.environment == environment }
    }

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

object SetDraftSavedStateCodec {
    const val MAX_SAVED_LEDGER_RECORDS = 100
    const val MAX_SAVED_EVENT_IDS = 120
    private const val VERSION = "v1"

    private fun enc(s: String): String =
        s.replace("%", "%25").replace("|", "%7C").replace(";", "%3B").replace(",", "%2C")

    private fun dec(s: String): String =
        s.replace("%2C", ",").replace("%3B", ";").replace("%7C", "|").replace("%25", "%")

    fun encode(draft: SetExecutionDraft): String {
        require(draft.repRecords.size <= MAX_SAVED_LEDGER_RECORDS) {
            "Repetition ledger size (${draft.repRecords.size}) exceeds limit ($MAX_SAVED_LEDGER_RECORDS)"
        }
        require(draft.acceptedEventIds.size <= MAX_SAVED_EVENT_IDS) {
            "Accepted event ID count (${draft.acceptedEventIds.size}) exceeds limit ($MAX_SAVED_EVENT_IDS)"
        }
        val repsEncoded = draft.repRecords.joinToString(";") { r ->
            listOf(
                r.repIndex.toString(),
                r.classification.name,
                r.totalDurationMs?.toString().orEmpty(),
                r.concentricDurationMs?.toString().orEmpty(),
                r.eccentricDurationMs?.toString().orEmpty(),
                r.angularRomDegrees?.toString().orEmpty(),
                r.linearRomCm?.toString().orEmpty(),
                r.origin.name,
            ).joinToString(",")
        }
        val eventsEncoded = draft.acceptedEventIds.joinToString(";") { enc(it) }
        return listOf(
            VERSION,
            enc(draft.ownerId),
            draft.environment.name,
            enc(draft.sessionId),
            enc(draft.exerciseId),
            draft.setIndex.toString(),
            enc(draft.trackingWindowId),
            draft.fullReps.coerceAtLeast(0).toString(),
            draft.partialReps.coerceAtLeast(0).toString(),
            draft.origin.name,
            draft.actualLoad?.grams?.toString().orEmpty(),
            draft.hasInitializedLoad.toString(),
            draft.setStartedAtElapsedMs.toString(),
            draft.trackingMode.name,
            draft.hadDeviceTracking.toString(),
            draft.isManualFallback.toString(),
            repsEncoded,
            eventsEncoded,
        ).joinToString("|")
    }

    fun decodeAndValidate(
        raw: String?,
        expectedOwnerId: String,
        expectedEnvironment: ExecutionEnvironmentKind,
        expectedSessionId: String,
        expectedExerciseId: String,
        expectedSetIndex: Int,
        currentElapsedMs: Long,
    ): SetExecutionDraft? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split("|")
        if (parts.size != 18 || parts[0] != VERSION) return null
        val ownerId = dec(parts[1])
        if (ownerId != expectedOwnerId) return null
        val env = runCatching { ExecutionEnvironmentKind.valueOf(parts[2]) }.getOrNull() ?: return null
        if (env != expectedEnvironment) return null
        val sessionId = dec(parts[3])
        val exerciseId = dec(parts[4])
        val setIndex = parts[5].toIntOrNull() ?: return null
        if (sessionId != expectedSessionId || exerciseId != expectedExerciseId || setIndex != expectedSetIndex) {
            return null
        }
        val windowId = dec(parts[6])
        val expectedWindow = "${expectedSessionId}_${expectedExerciseId}_${expectedSetIndex}"
        if (windowId != expectedWindow) return null
        val fullReps = parts[7].toIntOrNull() ?: return null
        val partialReps = parts[8].toIntOrNull() ?: return null
        if (fullReps < 0 || partialReps < 0) return null
        val origin = runCatching { MeasurementOrigin.valueOf(parts[9]) }.getOrNull() ?: return null
        val load = if (parts[10].isEmpty()) {
            null
        } else {
            val g = parts[10].toLongOrNull() ?: return null
            if (g < 0L) return null
            WeightLoad(g)
        }
        val loadInit = parts[11].toBooleanStrictOrNull() ?: return null
        val startElapsedMs = parts[12].toLongOrNull() ?: return null
        val mode = runCatching { TrackingMode.valueOf(parts[13]) }.getOrNull() ?: return null
        val hadDevice = parts[14].toBooleanStrictOrNull() ?: return null
        val fallback = parts[15].toBooleanStrictOrNull() ?: return null

        val parsedReps = ArrayList<RepMeasurement>()
        if (parts[16].isNotEmpty()) {
            val repTokens = parts[16].split(";")
            if (repTokens.size > MAX_SAVED_LEDGER_RECORDS) return null
            for (token in repTokens) {
                val f = token.split(",")
                if (f.size != 8) return null
                val cls = runCatching { RepClassification.valueOf(f[1]) }.getOrNull() ?: return null
                val dur = if (f[2].isEmpty()) null else f[2].toLongOrNull() ?: return null
                val con = if (f[3].isEmpty()) null else f[3].toLongOrNull() ?: return null
                val ecc = if (f[4].isEmpty()) null else f[4].toLongOrNull() ?: return null
                val ang = if (f[5].isEmpty()) null else f[5].toDoubleOrNull() ?: return null
                val lin = if (f[6].isEmpty()) null else f[6].toDoubleOrNull() ?: return null
                val org = runCatching { MeasurementOrigin.valueOf(f[7]) }.getOrNull() ?: return null
                if (dur != null && dur < 0L) return null
                if (con != null && con < 0L) return null
                if (ecc != null && ecc < 0L) return null
                if (ang != null && (!ang.isFinite() || ang < 0.0)) return null
                if (lin != null && (!lin.isFinite() || lin < 0.0)) return null
                parsedReps.add(
                    RepMeasurement(
                        repIndex = parsedReps.size + 1,
                        classification = cls,
                        totalDurationMs = dur,
                        concentricDurationMs = con,
                        eccentricDurationMs = ecc,
                        angularRomDegrees = ang,
                        linearRomCm = lin,
                        origin = org,
                    ),
                )
            }
        }

        val eventIds = if (parts[17].isEmpty()) {
            emptySet()
        } else {
            val rawIds = parts[17].split(";").map { dec(it) }.filter { it.isNotBlank() }
            if (rawIds.size > MAX_SAVED_EVENT_IDS) return null
            rawIds.toSet()
        }

        val safeStartElapsedMs = if (startElapsedMs in 0L..currentElapsedMs) {
            startElapsedMs
        } else {
            currentElapsedMs
        }

        val (derivedFull, derivedPartial) = if (hadDevice) {
            parsedReps.count { it.classification == RepClassification.FULL } to
                parsedReps.count { it.classification == RepClassification.PARTIAL }
        } else {
            fullReps to partialReps
        }

        return SetExecutionDraft(
            ownerId = ownerId,
            environment = env,
            sessionId = sessionId,
            exerciseId = exerciseId,
            setIndex = setIndex,
            trackingWindowId = windowId,
            fullReps = derivedFull,
            partialReps = derivedPartial,
            repRecords = parsedReps,
            acceptedEventIds = eventIds,
            origin = origin,
            actualLoad = load,
            hasInitializedLoad = loadInit,
            setStartedAtElapsedMs = safeStartElapsedMs,
            trackingMode = mode,
            hadDeviceTracking = hadDevice,
            isManualFallback = fallback,
        )
    }
}
