package com.runtrack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtrack.data.local.*
import com.runtrack.data.api.ApiFailure
import com.runtrack.data.api.userMessage
import com.runtrack.data.repository.RemoteTrackRepository
import com.runtrack.data.repository.TrackRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TrackState(
    val data: TrackSnapshot = TrackSnapshot(), val loading: Boolean = true,
    val saving: Boolean = false, val error: String? = null, val loadFailed: Boolean = false
)

class TrackViewModel(private val repository: TrackRepository = RemoteTrackRepository()) : ViewModel() {
    private val mutation = Mutex()
    private val mutable = MutableStateFlow(TrackState())
    val state = mutable.asStateFlow()
    init { reload() }

    fun reload() = viewModelScope.launch {
        if (mutable.value.saving) return@launch
        mutation.withLock {
        mutable.value = mutable.value.copy(loading = true, error = null)
        try { mutable.value = TrackState(data = repository.load(), loading = false) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { mutable.value = mutable.value.copy(loading = false, loadFailed = true, error = error.userMessage()) }
        }
    }

    fun clearError() { mutable.value = mutable.value.copy(error = null) }
    fun saveShoe(draft: ShoeDraft, id: String?, done: () -> Unit) = change(done) { repository.saveShoe(draft, id) }
    fun saveRun(draft: RunDraft, id: String?, done: () -> Unit) = change(done) { repository.saveRun(draft, id) }
    fun deleteShoe(id: String) = change { repository.deleteShoe(id) }
    fun deleteRun(id: String) = change { repository.deleteRun(id) }

    private fun change(done: () -> Unit = {}, request: suspend () -> Unit) {
        if (mutable.value.loading || mutable.value.loadFailed || mutable.value.saving) return
        mutable.value = mutable.value.copy(saving = true, error = null)
        viewModelScope.launch {
            mutation.withLock {
                var stored = false
                try {
                    request()
                    stored = true
                    done()
                    mutable.value = mutable.value.copy(saving = false, loading = true)
                    mutable.value = TrackState(data = repository.load(), loading = false)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    if (com.runtrack.BuildConfig.DEBUG) android.util.Log.e("RunTrackApi",
                        error.javaClass.name + "\n" + error.stackTrace.take(8).joinToString("\n"))
                    val uncertain = error !is ApiFailure
                    if (!stored && uncertain) done()
                    mutable.value = mutable.value.copy(saving = false, loading = false, loadFailed = stored || uncertain,
                        error = when {
                            stored -> "저장은 완료됐지만 목록을 불러오지 못했어요. 다시 불러와 주세요."
                            uncertain -> "요청 결과를 확인하지 못했어요. 목록을 다시 불러와 저장 여부를 확인해 주세요."
                            else -> error.userMessage()
                        })
                }
            }
        }
    }
}
