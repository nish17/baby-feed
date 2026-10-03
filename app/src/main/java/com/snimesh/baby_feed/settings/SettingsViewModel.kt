package com.snimesh.baby_feed.settings

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.snimesh.baby_feed.widget.FeedWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Any change immediately triggers a widget redraw (via onIntervalChanged) so the countdown
 * recomputes against the existing last-feed timestamp right away -- it applies live, not just to
 * feeds logged after the change (see Key Technical Decisions).
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val onIntervalChanged: suspend () -> Unit = {},
) : ViewModel() {

    private val _intervalMillis = MutableStateFlow(DEFAULT_FEEDING_INTERVAL_MILLIS)
    val intervalMillis: StateFlow<Long> = _intervalMillis.asStateFlow()

    init {
        viewModelScope.launch {
            _intervalMillis.value = repository.getFeedingIntervalMillis()
        }
    }

    fun setIntervalMillis(intervalMillis: Long) {
        viewModelScope.launch {
            repository.setFeedingIntervalMillis(intervalMillis)
            _intervalMillis.value = intervalMillis
            onIntervalChanged()
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                return SettingsViewModel(
                    repository = SettingsRepository.getInstance(appContext),
                    onIntervalChanged = { FeedWidget().updateAll(appContext) },
                ) as T
            }
        }
    }
}
