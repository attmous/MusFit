package com.musfit.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRuleTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun installsSharedSchedulerForMainDispatcher() = runTest {
        assertSame(mainDispatcherRule.dispatcher.scheduler, testScheduler)
    }
}
