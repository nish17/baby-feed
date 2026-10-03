package com.snimesh.baby_feed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Redirects Dispatchers.Main to a test dispatcher so viewModelScope.launch { ... } (which uses
 * Dispatchers.Main.immediate) actually runs inside plain JVM ViewModel tests. UnconfinedTestDispatcher
 * runs launched coroutines eagerly, so assertions right after a ViewModel call see the result
 * without needing advanceUntilIdle().
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
