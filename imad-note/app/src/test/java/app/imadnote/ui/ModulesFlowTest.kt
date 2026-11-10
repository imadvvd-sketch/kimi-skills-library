package app.imadnote.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.imadnote.AppContainer
import app.imadnote.LocalAppContainer
import app.imadnote.awaitUntil
import app.imadnote.data.ThemeMode
import app.imadnote.data.db.EventEntity
import app.imadnote.data.db.PropertyType
import app.imadnote.data.db.TaskStatus
import app.imadnote.data.db.ViewType
import app.imadnote.domain.db.DbJson
import app.imadnote.domain.tasks.Recurrence
import app.imadnote.testContainer
import app.imadnote.testContext
import app.imadnote.ui.shell.AppShell
import app.imadnote.ui.shell.ShellViewModel
import app.imadnote.ui.theme.ImadNoteTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId

/** End-to-end flows for tasks, projects, databases, calendar, search and analytics. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-420dpi")
class ModulesFlowTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    // Real clock so completion dates fall in the charts' date range.
    private val container: AppContainer = testContainer(clock = app.imadnote.core.Clock.System)
    private val ws: String = runBlocking { container.workspaces.ensureDefault("Personal").id }

    @After fun tearDown() = container.db.close()

    private fun launch(theme: ThemeMode = ThemeMode.LIGHT) {
        val shell = ShellViewModel(container, "Personal")
        awaitUntil { shell.workspace.value != null && shell.tree.value != null }
        rule.setContent { CompositionLocalProvider(LocalAppContainer provides container) { ImadNoteTheme(theme) { AppShell(shell) } } }
        rule.waitForIdle()
    }

    private fun settle() { awaitUntil { true }; rule.waitForIdle() }
    /** Advances Compose's clock so debounced work (delay in effects) runs, then waits for idle. */
    private fun pump() { rule.mainClock.advanceTimeBy(100); rule.waitForIdle() }
    private fun awaitExact(text: String) = awaitUntil { pump(); rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun awaitText(text: String) = awaitUntil { pump(); rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    private fun route(label: String) = app.imadnote.ui.shell.Destination.entries.first { testContext().getString(it.label) == label }.route
    private fun shot(name: String) = rule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    private fun openFromMenu(label: String) {
        rule.onNodeWithTag("tab-more").performClick(); settle()
        rule.onNodeWithTag("nav-" + route(label)).performClick(); settle()
    }

    /** Realistic sample content for screenshots. */
    private fun seed() = runBlocking {
        val today = LocalDate.now()
        val launch = container.projects.create(ws, "Product launch", "🚀", 0xFF527DAA, "Ship the beta to early users.")
        container.projects.edit(launch.id) { it.copy(startDate = today.minusDays(10).toEpochDay(), deadline = today.plusDays(20).toEpochDay()) }
        val home = container.projects.create(ws, "Move house", "🏠", 0xFF2E9D73)
        val t = container.tasks
        t.create(ws, "Record the demo video", launch.id, dueDate = today, priority = 3, labelNames = listOf("video"))
        t.create(ws, "Write release notes", launch.id, dueDate = today, priority = 2)
        t.create(ws, "Pay electricity bill", dueDate = today.minusDays(1), priority = 1)
        val weekly = t.create(ws, "Team sync", dueDate = today.plusDays(1))
        t.edit(weekly.id) { it.copy(recurrence = Recurrence(Recurrence.Unit.WEEK).encode(), dueTime = 10 * 60) }
        t.create(ws, "Book movers", home.id, dueDate = today.plusDays(4))
        listOf("Draft pricing page", "Collect beta feedback", "Fix login bug").forEachIndexed { i, title ->
            val done = t.create(ws, title, launch.id, dueDate = today.minusDays(i.toLong() + 1))
            t.setCompleted(done.id, true)
        }
        val zone = ZoneId.systemDefault()
        val start = today.atStartOfDay(zone).plusHours(14)
        container.events.save(EventEntity("", ws, "Design review", startAt = start.toInstant().toEpochMilli(), endAt = start.plusHours(1).toInstant().toEpochMilli(), color = 0xFF7B55C7, createdAt = 0, updatedAt = 0))

        val books = container.collections.create(ws, "Reading list", "📚")
        val props = container.collections.observeProperties(books.id).first()
        val title = props.first { it.type == PropertyType.TITLE }
        val status = props.first { it.type == PropertyType.STATUS }
        val date = props.first { it.type == PropertyType.DATE }
        val rating = container.collections.addProperty(books.id, "Rating", PropertyType.NUMBER)
        val options = DbJson.property(status.config).options
        listOf(Triple("Deep Work", 2, 5), Triple("Atomic Habits", 1, 4), Triple("The Design of Everyday Things", 0, 0), Triple("Thinking, Fast and Slow", 1, 0)).forEachIndexed { i, (name, s, r) ->
            container.collections.addRecord(books.id, buildMap {
                put(title.id, name); put(status.id, options[s].id); put(date.id, today.plusDays(i * 3L).toString())
                if (r > 0) put(rating.id, r.toString())
            })
        }
        container.collections.addView(books.id, "Board", ViewType.BOARD, app.imadnote.domain.db.ViewConfig(groupBy = status.id))
        books.id
    }

    @Test fun `quick-add a task with shortcuts, complete it and see it under Completed`() {
        launch()
        rule.onNodeWithTag("tab-tasks").performClick(); settle()
        awaitUntil { pump(); rule.onAllNodes(hasTestTag("task-add"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("task-add", useUnmergedTree = true).performTextInput("Buy milk !3 #home")
        rule.onNodeWithTag("task-add", useUnmergedTree = true).performImeAction()
        awaitText("Buy milk")
        val task = runBlocking { container.db.tasks().getAll().single() }
        assertEquals("Buy milk", task.title)
        assertEquals(3, task.priority)
        assertEquals(LocalDate.now().toEpochDay(), task.dueDate) // added from the Today view
        rule.onAllNodesWithContentDescription("Mark as done")[0].performClick()
        awaitUntil { runBlocking { container.db.tasks().get(task.id)?.status == TaskStatus.DONE } }
        rule.onNodeWithText("Completed").performScrollTo().performClick(); settle()
        awaitText("Buy milk")
    }

    @Test fun `task detail edits priority and adds a subtask`() {
        val id = runBlocking { container.tasks.create(ws, "Plan trip").id }
        launch()
        rule.onNodeWithTag("tab-tasks").performClick(); settle()
        awaitExact("All")
        rule.onNodeWithText("All").performClick(); settle()
        awaitExact("Plan trip")
        rule.onNodeWithText("Plan trip").performClick(); settle()
        awaitUntil { pump(); rule.onAllNodes(hasTestTag("task-title")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("No priority").performClick(); settle()
        rule.onNodeWithText("High").performClick(); settle()
        rule.onNode(hasSetTextAction() and hasText("Add a subtask")).performTextInput("Book flights")
        rule.onNodeWithContentDescription("Add").performClick()
        awaitUntil { runBlocking { container.db.tasks().subtasks(id).any { it.title == "Book flights" } } }
        awaitUntil { runBlocking { container.tasks.get(id)?.priority == 3 } }
        shot("task-detail-light")
    }

    @Test fun `screenshots of tasks, projects, calendar, analytics and databases`() {
        val dbId = seed()
        launch()
        rule.onNodeWithTag("tab-tasks").performClick(); settle()
        awaitText("Record the demo video")
        shot("tasks-light")
        openFromMenu("Projects")
        awaitExact("Product launch")
        shot("projects-light")
        rule.onNodeWithText("Product launch").performClick(); settle()
        awaitText("Ship the beta")
        shot("project-detail-light")
        rule.onNodeWithContentDescription("Back").performClick(); settle()
        rule.onNodeWithTag("tab-calendar").performClick(); settle()
        awaitText("Design review")
        shot("calendar-month-light")
        rule.onNodeWithText("Day").performClick(); settle()
        shot("calendar-day-light")
        openFromMenu("Analytics")
        awaitText("Completion rate")
        shot("analytics-light")
        openFromMenu("Databases")
        awaitExact("Reading list")
        rule.onNodeWithText("Reading list").performClick(); settle()
        awaitText("Deep Work")
        shot("database-table-light")
        assertTrue(dbId.isNotEmpty())
    }

    @Test fun `dark theme screenshots`() {
        seed()
        launch(ThemeMode.DARK)
        rule.onNodeWithTag("tab-tasks").performClick(); settle()
        awaitText("Record the demo video")
        shot("tasks-dark")
        openFromMenu("Analytics")
        awaitText("Completion rate")
        shot("analytics-dark")
    }

    @Test
    @Config(qualifiers = "ar-w411dp-h891dp-420dpi")
    fun `arabic screenshots of tasks and calendar`() {
        seed()
        launch()
        rule.onNodeWithTag("tab-tasks").performClick(); settle()
        awaitText("Record the demo video")
        shot("tasks-arabic")
        rule.onNodeWithTag("tab-calendar").performClick(); settle()
        awaitText("Design review")
        shot("calendar-arabic")
    }

    @Test fun `board view groups records by status`() {
        val id = runBlocking {
            val c = container.collections.create(ws, "B", "📚")
            container.collections.update(c.copy(name = "B"))
            val status = container.collections.observeProperties(c.id).first().first { it.type == PropertyType.STATUS }
            val title = container.collections.observeProperties(c.id).first().first { it.type == PropertyType.TITLE }
            val options = DbJson.property(status.config).options
            listOf("Deep Work" to 2, "Atomic Habits" to 1, "Thinking, Fast and Slow" to 1, "The Design of Everyday Things" to 0).forEach { (name, i) ->
                container.collections.addRecord(c.id, mapOf(title.id to name, status.id to options[i].id))
            }
            container.collections.observeViews(c.id).first().single().let { container.db.collections().deleteView(it.id) }
            container.collections.addView(c.id, "Board", ViewType.BOARD, app.imadnote.domain.db.ViewConfig(groupBy = status.id))
            c.id
        }
        launch()
        openFromMenu("Databases")
        awaitExact("B")
        rule.onNodeWithText("B").performClick()
        rule.mainClock.autoAdvance = false
        repeat(20) { rule.mainClock.advanceTimeByFrame() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        awaitText("Not started")
        shot("database-board-light")
    }

    @Test fun `open a database, add a record, name it and add a property`() {
        val id = runBlocking { container.collections.create(ws, "Contacts", "👥").id }
        launch()
        openFromMenu("Databases")
        awaitExact("Contacts")
        rule.onNodeWithText("Contacts").performClick(); settle()
        awaitUntil { pump(); rule.onAllNodes(hasTestTag("db-table")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("New").performClick(); settle()
        awaitUntil { runBlocking { container.db.collections().allRecords().size == 1 } }
        awaitUntil { pump(); rule.onAllNodes(hasTestTag("record-title")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("record-title").performTextInput("Sara")
        awaitUntil { pump(); runBlocking { container.db.collections().allCells().any { it.value == "Sara" } } }
        assertTrue(runBlocking { container.collections.observeRecordCount(id).first() } == 1)
    }

    @Test fun `search finds a task and opens it`() {
        runBlocking { container.tasks.create(ws, "Renew passport") }
        launch()
        rule.onNodeWithContentDescription("Search").performClick(); settle()
        rule.onNodeWithTag("search-field").performTextInput("passp")
        awaitText("Renew passport")
        shot("search-light")
        rule.onNodeWithText("Renew passport").performClick(); settle()
        awaitUntil { pump(); rule.onAllNodes(hasTestTag("task-title")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, runBlocking { container.db.search().observeHistory().first().size })
    }

    @Test fun `quick capture as a task and as a checklist`() {
        launch()
        openFromMenu("Inbox")
        rule.onNodeWithText("Task").performClick(); settle()
        rule.onNodeWithTag("capture-field").performTextInput("Call the bank tomorrow")
        rule.onNodeWithContentDescription("Add to inbox").performClick()
        awaitUntil { runBlocking { container.db.tasks().getAll().any { it.title == "Call the bank" && it.dueDate == LocalDate.now().plusDays(1).toEpochDay() } } }
        rule.onNodeWithText("Checklist").performClick(); settle()
        rule.onNodeWithTag("capture-field").performTextInput("Eggs\nBread")
        rule.onNodeWithContentDescription("Add to inbox").performClick()
        awaitUntil { runBlocking { container.db.blocks().getAll().count { it.type == "todo" } == 2 } }
        shot("inbox-capture-light")
    }
}
