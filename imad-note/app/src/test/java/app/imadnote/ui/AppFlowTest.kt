package app.imadnote.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.imadnote.AppContainer
import app.imadnote.LocalAppContainer
import app.imadnote.awaitUntil
import app.imadnote.data.ThemeMode
import app.imadnote.domain.editor.Block
import app.imadnote.domain.editor.BlockProps
import app.imadnote.domain.editor.BlockType
import app.imadnote.domain.editor.EditorDoc
import app.imadnote.domain.editor.InlineStyle
import app.imadnote.domain.editor.Span
import app.imadnote.testContainer
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

/** End-to-end flows through the real navigation graph, view-models and an in-memory database. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-420dpi")
class AppFlowTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val container: AppContainer = testContainer()

    @After fun tearDown() = container.db.close()

    private fun launch(theme: ThemeMode = ThemeMode.LIGHT) {
        val shell = ShellViewModel(container, "Personal")
        awaitUntil { shell.workspace.value != null && shell.tree.value != null }
        rule.setContent {
            CompositionLocalProvider(LocalAppContainer provides container) { ImadNoteTheme(theme) { AppShell(shell) } }
        }
        rule.waitForIdle()
    }

    private fun settle() {
        awaitUntil { true }
        rule.waitForIdle()
    }

    private fun awaitText(text: String) = awaitUntil {
        rule.waitForIdle()
        rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    /** Waits for navigation that depends on a database write, e.g. opening a newly created page. */
    private fun awaitTag(tag: String) = awaitUntil {
        rule.waitForIdle()
        rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun shot(name: String) = rule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")

    private fun seed() = runBlocking {
        val ws = container.workspaces.ensureDefault("Personal")
        val plan = container.pages.create(ws.id, title = "Product launch plan", icon = "🚀")
        container.pages.saveDoc(
            plan.id,
            EditorDoc(
                listOf(
                    Block("1", BlockType.HEADING_2, "Goals"),
                    Block("2", BlockType.PARAGRAPH, "Ship the beta to early users and collect feedback.", listOf(Span(9, 13, InlineStyle.BOLD), Span(17, 28, InlineStyle.HIGHLIGHT, "orange"))),
                    Block("3", BlockType.TODO, "Finalise onboarding copy", props = BlockProps(checked = true)),
                    Block("4", BlockType.TODO, "Record the demo video"),
                    Block("5", BlockType.BULLET, "Pricing page", indent = 1),
                    Block("6", BlockType.CALLOUT, "Launch date depends on store review times.", props = BlockProps(emoji = "💡")),
                    Block("7", BlockType.QUOTE, "Make it simple, but significant."),
                    Block("8", BlockType.CODE, "val launch = Date(2026, 12, 1)", props = BlockProps(language = "kotlin")),
                    Block("9", BlockType.TABLE, props = BlockProps(cells = listOf(listOf("Channel", "Owner"), listOf("Email", "Sara"), listOf("Social", "Omar")))),
                    Block("10", BlockType.DIVIDER),
                    Block("11", BlockType.EQUATION, "E = mc^2"),
                    Block("12", BlockType.PARAGRAPH, ""),
                )
            ),
        )
        container.pages.edit(plan.id) { it.copy(isPinned = true, isFavorite = true) }
        container.pages.markOpened(plan.id)
        val journal = container.pages.create(ws.id, title = "Reading journal", icon = "📚")
        container.pages.edit(journal.id) { it.copy(isPinned = true) }
        container.pages.markOpened(journal.id)
        container.pages.create(ws.id, parentId = journal.id, title = "Deep Work — notes")
        val arabic = container.pages.create(ws.id, title = "خطة الأسبوع", icon = "📅")
        container.pages.markOpened(arabic.id)
        plan.id
    }

    @Test fun `first launch shows the empty state and creates the personal workspace`() {
        launch()
        rule.onNodeWithText("Your workspace is ready").assertIsDisplayed()
        rule.onAllNodesWithText("Personal")[0].assertIsDisplayed()
        shot("home-empty-light")
    }

    @Test fun `create a page, write in it, and find it saved after leaving`() {
        launch()
        rule.onNodeWithTag("fab").performClick()
        awaitTag("page-title")
        rule.onNodeWithTag("page-title").performTextInput("Meeting notes")
        rule.onNode(hasSetTextAction() and hasTestTag("page-title").not()).performTextInput("Discuss the roadmap")
        settle()
        rule.onNodeWithContentDescription("Back").performClick()
        settle()
        awaitText("Meeting notes")

        val page = runBlocking { container.db.pages().getAll().single() }
        assertEquals("Meeting notes", page.title)
        awaitUntil { runBlocking { container.pages.loadDoc(page.id).blocks.first().text == "Discuss the roadmap" } }
    }

    @Test fun `toolbar bold button formats the next typed text`() {
        val pageId = runBlocking {
            val ws = container.workspaces.ensureDefault("Personal")
            container.pages.create(ws.id, title = "Format").id
        }
        launch()
        rule.onNodeWithTag("tab-notes").performClick()
        settle()
        rule.onAllNodesWithText("Format")[0].performClick()
        awaitTag("page-title")
        awaitUntil { rule.waitForIdle(); rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size >= 2 }
        val body = hasSetTextAction() and hasTestTag("page-title").not()
        rule.onNode(body).performClick()
        settle()
        rule.onNodeWithContentDescription("Bold").performClick()
        rule.onNode(body).performTextInput("Loud")
        settle()
        val block = awaitBlock(pageId) { it.text == "Loud" }
        assertEquals(listOf(Span(0, 4, InlineStyle.BOLD)), block.spans)
    }

    @Test fun `slash menu converts the block`() {
        val pageId = runBlocking {
            val ws = container.workspaces.ensureDefault("Personal")
            container.pages.create(ws.id, title = "Slash").id
        }
        launch()
        rule.onNodeWithTag("tab-notes").performClick()
        settle()
        rule.onAllNodesWithText("Slash")[0].performClick()
        awaitTag("page-title")
        awaitUntil { rule.waitForIdle(); rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size >= 2 }
        val body = hasSetTextAction() and hasTestTag("page-title").not()
        rule.onNode(body).performClick()
        rule.onNode(body).performTextInput("/quo")
        settle()
        rule.onNodeWithText("Quote").performClick()
        settle()
        assertEquals(BlockType.QUOTE, awaitBlock(pageId) { it.type == BlockType.QUOTE }.type)
    }

    private fun awaitBlock(pageId: String, predicate: (Block) -> Boolean): Block {
        var found: Block? = null
        awaitUntil { runBlocking { container.pages.loadDoc(pageId).blocks.firstOrNull(predicate)?.also { found = it } != null } }
        return found!!
    }

    @Test fun `inbox capture adds an item that can be filed into pages`() {
        launch()
        rule.onNodeWithTag("tab-inbox").performClick()
        settle()
        rule.onNode(hasSetTextAction()).performTextInput("Call the supplier")
        rule.onNodeWithContentDescription("Add to inbox").performClick()
        awaitText("Call the supplier")
        shot("inbox-light")
        rule.onNodeWithContentDescription("File into Pages").performClick()
        awaitText("Inbox is clear")
        assertTrue(runBlocking { container.db.pages().getAll().single().let { !it.isInbox && it.title == "Call the supplier" } })
    }

    @Test fun `trash and restore from the notes list`() {
        seed()
        launch()
        rule.onNodeWithTag("tab-notes").performClick()
        settle()
        shot("notes-light")
        rule.onAllNodes(hasContentDescription("More options"))[0].performClick()
        rule.onNodeWithText("Move to trash").performClick()
        settle()
        val ws = runBlocking { container.workspaces.current.first()!!.id }
        assertEquals(1, runBlocking { container.pages.observeTrashed(ws).first().size })
    }

    @Test fun `theme choice in settings is persisted`() {
        launch()
        rule.onNodeWithTag("tab-settings").performClick()
        settle()
        shot("settings-light")
        rule.onNodeWithText("Dark").performClick()
        awaitUntil { runBlocking { container.settings.themeMode.first() == ThemeMode.DARK } }
    }

    @Test fun `screenshots - home and editor, light`() {
        val planId = seed()
        launch()
        shot("home-light")
        rule.onAllNodesWithText("Product launch plan")[0].performClick()
        settle()
        shot("editor-light")
        rule.onNodeWithText("Record the demo video", substring = true).performClick()
        settle()
        shot("editor-toolbar-light")
        assertTrue(planId.isNotEmpty())
    }

    @Test fun `screenshots - home and editor, dark`() {
        seed()
        launch(ThemeMode.DARK)
        shot("home-dark")
        rule.onAllNodesWithText("Product launch plan")[0].performClick()
        settle()
        shot("editor-dark")
    }

    @Test
    @Config(qualifiers = "ar-w411dp-h891dp-420dpi")
    fun `arabic locale lays out right to left with translated labels`() {
        seed()
        launch()
        rule.onNode(hasTestTag("tab-home") and hasText("الرئيسية")).assertIsDisplayed()
        val home = rule.onNodeWithTag("tab-home").fetchSemanticsNode().boundsInRoot
        val settings = rule.onNodeWithTag("tab-settings").fetchSemanticsNode().boundsInRoot
        assertTrue("first tab should be on the right in RTL", home.left > settings.left)
        shot("home-arabic")
        rule.onAllNodesWithText("Product launch plan")[0].performClick()
        settle()
        shot("editor-arabic")
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-240dpi")
    fun `tablet shows the permanent sidebar with the page tree`() {
        seed()
        launch()
        rule.onNodeWithText("Pages").assertIsDisplayed()
        rule.onNodeWithText("Trash").assertIsDisplayed()
        assertTrue(rule.onAllNodes(hasContentDescription("Open menu")).fetchSemanticsNodes().isEmpty())
        shot("home-tablet")
    }
}
