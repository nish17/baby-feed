package com.snimesh.baby_feed.settings

import com.snimesh.baby_feed.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun settingAnInterval_persistsAndIsReadBackCorrectly() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        val twoHours = TimeUnit.HOURS.toMillis(2)

        viewModel.setIntervalMillis(twoHours)

        assertEquals(twoHours, repository.getFeedingIntervalMillis())
        assertEquals(twoHours, viewModel.intervalMillis.value)
    }

    @Test
    fun changingTheInterval_updatesExposedValueImmediately() = runTest {
        val repository = FakeSettingsRepository(intervalMillis = DEFAULT_FEEDING_INTERVAL_MILLIS)
        val viewModel = SettingsViewModel(repository)
        val fourHours = TimeUnit.HOURS.toMillis(4)

        viewModel.setIntervalMillis(fourHours)

        assertEquals(fourHours, viewModel.intervalMillis.value)
    }

    @Test
    fun construction_hydratesFromAPreExistingStoredInterval() = runTest {
        val fourHours = TimeUnit.HOURS.toMillis(4)
        val repository = FakeSettingsRepository(intervalMillis = fourHours)

        val viewModel = SettingsViewModel(repository)

        assertEquals(fourHours, viewModel.intervalMillis.value)
    }

    @Test
    fun changingInterval_triggersWidgetRedraw() = runTest {
        var redrawTriggered = false
        val viewModel = SettingsViewModel(
            repository = FakeSettingsRepository(),
            onIntervalChanged = { redrawTriggered = true },
        )

        viewModel.setIntervalMillis(TimeUnit.HOURS.toMillis(2))

        assertTrue(redrawTriggered)
    }
}
