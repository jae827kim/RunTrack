package com.runtrack.data.repository

import com.runtrack.data.local.*
import java.time.LocalDateTime
import java.util.UUID

class LocalAdminTrackRepository(private val store: TrackSnapshotStore) : TrackRepository {
    private var activeSession: ActiveRunSession? = null

    override suspend fun load() = store.load()

    override suspend fun startSession(shoeId: String?): ActiveRunSession = activeSession ?: ActiveRunSession(
        UUID.randomUUID().toString(), LocalDateTime.now().withSecond(0).withNano(0).toString(), shoeId
    ).also { activeSession = it }

    override suspend fun finishSession(
        session: ActiveRunSession,
        distanceKm: Double,
        durationMinutes: Int,
        notes: String?
    ) {
        require(activeSession?.sessionId == session.sessionId) { "진행 중인 러닝 세션이 없습니다." }
        saveRun(RunDraft(
            startTime = session.startTime,
            duration = durationMinutes.toString(),
            distance = distanceKm.toString(),
            shoeId = session.shoeId.orEmpty(),
            notes = notes.orEmpty()
        ), null)
        activeSession = null
    }

    override suspend fun saveShoe(draft: ShoeDraft, id: String?) {
        val data = store.load()
        val shoe = if (id == null) draft.toShoe() else draft.toShoe(id)
        store.save(data.saveShoe(shoe))
    }

    override suspend fun deleteShoe(id: String) { store.save(store.load().deleteShoe(id)) }

    override suspend fun saveRun(draft: RunDraft, id: String?) {
        val data = store.load()
        val run = if (id == null) draft.toRun(data.shoes) else draft.toRun(data.shoes, id)
        store.save(data.saveRun(run))
    }

    override suspend fun deleteRun(id: String) { store.save(store.load().deleteRun(id)) }

    companion object { const val PREFERENCES_NAME = "runtrack_admin_records_v1" }
}
