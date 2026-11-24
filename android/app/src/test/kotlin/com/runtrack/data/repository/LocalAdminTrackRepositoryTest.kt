package com.runtrack.data.repository

import com.runtrack.data.local.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class LocalAdminTrackRepositoryTest {
    private class MemoryStore : TrackSnapshotStore {
        var data = TrackSnapshot()
        override suspend fun load() = data
        override suspend fun save(snapshot: TrackSnapshot) { data = snapshot }
    }

    @Test fun offlineRecordsPersistAcrossRepositoriesAndUpdateShoeTotals() = runBlocking {
        val store = MemoryStore()
        val repository = LocalAdminTrackRepository(store)
        repository.saveShoe(ShoeDraft(brand = "Local", model = "Shoe", distance = "10", count = "2"), null)
        val shoe = repository.load().shoes.single()
        val draft = RunDraft(duration = "32.5", distance = "5", shoeId = shoe.id, calories = "210")
        repository.saveRun(draft, null)
        val restored = LocalAdminTrackRepository(store).load()
        assertEquals(15.0, restored.shoeDistance(shoe), 0.0)
        assertEquals(3, restored.shoeRunCount(shoe))
        assertEquals(210.0, restored.runs.single().calories!!, 0.0)
        repository.saveRun(draft.copy(distance = "6"), restored.runs.single().id)
        assertEquals(1, repository.load().runs.size)
        assertEquals(16.0, repository.load().shoeDistance(shoe), 0.0)
        repository.deleteRun(restored.runs.single().id)
        assertEquals(10.0, repository.load().shoeDistance(shoe), 0.0)
        assertEquals(2, repository.load().shoeRunCount(shoe))
    }

    @Test fun removingShoePreservesHistoricalRunAndShoeName() = runBlocking {
        val repository = LocalAdminTrackRepository(MemoryStore())
        repository.saveShoe(ShoeDraft(brand = "Local", model = "History"), null)
        val shoe = repository.load().shoes.single()
        repository.saveRun(RunDraft(duration = "30", distance = "5", shoeId = shoe.id), null)
        repository.deleteShoe(shoe.id)
        val data = repository.load()
        assertTrue(data.shoes.isEmpty())
        assertNull(data.runs.single().shoeId)
        assertEquals(shoe.displayName, data.runs.single().shoeName)
    }
}
