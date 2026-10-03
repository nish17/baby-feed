package com.snimesh.baby_feed.history

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.snimesh.baby_feed.data.FeedEntry
import com.snimesh.baby_feed.data.FeedRepository
import com.snimesh.baby_feed.widget.FeedWidget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lists entries newest-first; edit reuses the same "no future timestamp" validation as Unit 1.
 * Delete relies on FeedRepository's existing max-timestamp logic to determine the new "last
 * feed" -- no special-case code needed here (see Key Technical Decisions). Both edit and delete
 * trigger a widget redraw via the injected onEntriesChanged, same split as BackdateViewModel.
 */
class HistoryViewModel(
    private val repository: FeedRepository,
    private val onEntriesChanged: suspend () -> Unit = {},
) : ViewModel() {

    val entries: StateFlow<List<FeedEntry>> = repository.observeAll()
        .map { list -> list.sortedByDescending { it.timestampMillis } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateEntry(entry: FeedEntry) {
        viewModelScope.launch {
            try {
                repository.updateFeed(entry, nowMillis = System.currentTimeMillis())
                onEntriesChanged()
            } catch (rejected: IllegalArgumentException) {
                // Future timestamp -- reject silently, same validation as Unit 1. A real app
                // would surface this to the user; weekend scope treats it as a no-op.
            }
        }
    }

    fun deleteEntry(entry: FeedEntry) {
        viewModelScope.launch {
            repository.deleteFeed(entry)
            onEntriesChanged()
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                return HistoryViewModel(
                    repository = FeedRepository.getInstance(appContext),
                    onEntriesChanged = { FeedWidget().updateAll(appContext) },
                ) as T
            }
        }
    }
}
