package com.imadnote.app.ui

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.imadnote.app.MainActivity
import com.imadnote.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real app (Robolectric) through each main screen, so a crash on opening any of
 * them fails CI with a full stack trace.
 */
@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun s(id: Int) = rule.activity.getString(id)

    private fun clickText(text: String) {
        rule.onAllNodes(hasText(text) and hasClickAction())[0].performClick()
        rule.waitForIdle()
    }

    private fun clickDescription(text: String) {
        rule.onAllNodes(hasContentDescription(text) and hasClickAction())[0].performClick()
        rule.waitForIdle()
    }

    private fun assertShown(text: String) {
        rule.waitUntil(5_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun addNoteFromNotesEmptyStateOpensEditor() {
        clickText(s(R.string.tab_notes))
        clickText(s(R.string.add_note))
        assertShown(s(R.string.new_note))
    }

    @Test
    fun quickCaptureTilesOpenEditors() {
        clickDescription(s(R.string.quick_capture))
        clickText(s(R.string.capture_checklist))
        assertShown(s(R.string.new_note))
    }

    @Test
    fun addTaskOpensTaskEditor() {
        clickText(s(R.string.tab_tasks))
        clickText(s(R.string.add_task))
        assertShown(s(R.string.new_task))
    }

    @Test
    fun moreScreenEntriesOpen() {
        clickText(s(R.string.tab_more))
        clickText(s(R.string.calendar))
        assertShown(s(R.string.date_today))
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        clickText(s(R.string.tab_projects))
        assertShown(s(R.string.projects_empty_title))
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        clickText(s(R.string.settings))
        assertShown(s(R.string.settings_language))
    }

    @Test
    fun homeShowsDashboard() {
        assertShown(s(R.string.stat_today_tasks))
        clickText(s(R.string.stat_today_tasks).let { it })
    }
}
