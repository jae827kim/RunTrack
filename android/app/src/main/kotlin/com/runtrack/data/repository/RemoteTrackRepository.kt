package com.runtrack.data.repository

import com.runtrack.data.api.*
import com.runtrack.data.local.*
import com.runtrack.data.models.*
import java.time.*

interface TrackRepository {
    suspend fun load(): TrackSnapshot
    suspend fun startSession(shoeId: String?): ActiveRunSession
    suspend fun finishSession(session: ActiveRunSession, distanceKm: Double, durationMinutes: Int, notes: String?)
    suspend fun saveShoe(draft: ShoeDraft, id: String?)
    suspend fun deleteShoe(id: String)
    suspend fun saveRun(draft: RunDraft, id: String?)
    suspend fun deleteRun(id: String)
}

class RemoteTrackRepository(
    private val shoes: ShoeService = RetrofitClient.shoeService,
    private val runs: RunningService = RetrofitClient.runningService,
    private val zone: ZoneId = ZoneId.systemDefault()
) : TrackRepository {
    override suspend fun startSession(shoeId: String?): ActiveRunSession {
        val parsedShoeId = shoeId?.toIntOrNull()
        require(shoeId == null || parsedShoeId != null) { "선택한 신발을 확인할 수 없습니다." }
        val session = runs.startRunning(RunningSessionStartRequest(parsedShoeId)).requiredBody()
        return ActiveRunSession(session.sessionId, localDateTime(session.startTime, zone).toString(), session.shoeId?.toString())
    }

    override suspend fun finishSession(
        session: ActiveRunSession,
        distanceKm: Double,
        durationMinutes: Int,
        notes: String?
    ) {
        runs.endRunning(session.sessionId, RunningSessionEndRequest(
            distanceKm = distanceKm.toFloat(), durationMinutes = durationMinutes, notes = notes
        )).requiredBody()
    }

    override suspend fun load(): TrackSnapshot {
        val serverShoes = shoes.getShoes().requiredBody()
        val records = mutableListOf<RunningRecord>()
        do {
            val page = runs.getRunningRecords(records.size, 100).requiredBody()
            if (page.any { next -> records.any { it.id == next.id } }) throw ApiFailure(0, "기록 목록이 변경됐어요. 다시 불러와 주세요.")
            records.addAll(page)
        } while (page.size == 100)
        val names = serverShoes.associate { it.id to "${it.brand} ${it.model}" }
        return TrackSnapshot(shoes = serverShoes.map { shoe ->
            TrackedShoe(shoe.id.toString(), shoe.brand, shoe.model, shoe.purchaseDate?.take(10).orEmpty(),
                shoe.size?.blank(), shoe.color?.blank(), localCondition(shoe.condition), notes = shoe.notes?.blank(),
                serverDistanceKm = shoe.cumulativeKm.toDouble(), serverRunCount = shoe.runCount)
        }, runs = records.map { record ->
            TrackedRun(record.id.toString(), localDateTime(record.startTime, zone).toString(),
                record.durationMinutes.toDouble(), record.distanceKm.toDouble(), record.avgPaceMinPerKm?.toDouble(),
                record.avgHeartRate, record.caloriesBurned?.toDouble(), record.shoeId?.toString(), names[record.shoeId],
                record.avgSpeedKmh?.toDouble(), record.feeling, record.temperatureC?.toDouble(), record.notes)
        })
    }

    override suspend fun saveShoe(draft: ShoeDraft, id: String?) {
        require(draft.errors().isEmpty())
        if (id == null) shoes.createShoe(ShoeCreateRequest(draft.brand.trim(), draft.model.trim(),
            draft.size.blank(), draft.color.blank(), "${draft.purchaseDate}T00:00:00", draft.notes.blank(),
            remoteCondition(draft.condition))).requiredBody()
        else shoes.updateShoe(id.toInt(), ShoeUpdateRequest(draft.brand.trim(), draft.model.trim(), draft.size,
            draft.color, remoteCondition(draft.condition), draft.notes)).requiredBody()
    }

    override suspend fun deleteShoe(id: String) { shoes.deleteShoe(id.toInt()).requireSuccess() }

    override suspend fun saveRun(draft: RunDraft, id: String?) {
        require(draft.apiErrors(editing = id != null).isEmpty())
        val duration = draft.duration.toInt()
        val start = LocalDateTime.parse(draft.startTime).atZone(zone)
        val startText = start.toOffsetDateTime().toString()
        val endText = start.plusMinutes(duration.toLong()).toOffsetDateTime().toString()
        if (id == null) runs.createRecord(RunningRecordCreateRequest(
            distanceKm = draft.distance.toFloat(), durationMinutes = duration, startTime = startText, endTime = endText,
            shoeId = draft.shoeId.toIntOrNull(), avgHeartRate = draft.heartRate.toIntOrNull(),
            temperatureC = draft.temperature.toFloatOrNull(), feeling = draft.rating.blank(), notes = draft.notes.blank()
        )).requiredBody()
        else runs.updateRecord(id.toInt(), RunningRecordUpdateRequest(draft.distance.toDouble(), duration,
            startText, endText, draft.shoeId.toIntOrNull(), draft.rating.blank(), draft.notes.blank())).requiredBody()
    }

    override suspend fun deleteRun(id: String) { runs.deleteRecord(id.toInt()).requireSuccess() }
}

/** The current server returns timezone-naive UTC strings as well as ISO offset timestamps. */
internal fun localDateTime(value: String, zone: ZoneId): LocalDateTime =
    runCatching { OffsetDateTime.parse(value).atZoneSameInstant(zone).toLocalDateTime() }
        .getOrElse { LocalDateTime.parse(value).atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDateTime() }

private fun String.blank() = trim().takeIf { it.isNotEmpty() }
private fun remoteCondition(value: String) = when (value) {
    "", "양호" -> "good"; "사용 중" -> "fair"; "교체 권장" -> "worn"; else -> value
}
private fun localCondition(value: String) = when (value) {
    "good" -> "양호"; "fair" -> "사용 중"; "worn" -> "교체 권장"; else -> value
}
