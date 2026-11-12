package com.runtrack.data.repository

import com.runtrack.data.local.*

class LocalAdminTrackRepository(private val store: TrackSnapshotStore) : TrackRepository {
    override suspend fun load() = store.load()

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
