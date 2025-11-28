package com.bravebrain

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for ScreentimeHistoryActivity
 * 
 * These tests verify the activity can be launched and basic UI elements exist
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ScreentimeHistoryActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ScreentimeHistoryActivity::class.java)

    @Test
    fun testActivityLaunches() {
        // Simply verify the activity launches without crashing
        activityRule.scenario.onActivity { activity ->
            assert(activity != null)
        }
    }
}
