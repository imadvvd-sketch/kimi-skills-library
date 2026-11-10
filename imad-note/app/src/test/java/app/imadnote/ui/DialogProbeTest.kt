package app.imadnote.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.imadnote.ui.components.TextInputDialog
import app.imadnote.ui.theme.ImadNoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * The naming dialog, tested at Robolectric's default screen size. At phone densities
 * (e.g. 420dpi) Robolectric never reports Compose idle while any text field is shown inside a
 * Material AlertDialog, even a bare one with no app code; flow tests therefore create named
 * items through repositories and exercise the dialog here.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DialogProbeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun naming_dialog_trims_input_and_blocks_blank_names() {
        var confirmed: String? = null
        rule.setContent { ImadNoteTheme { TextInputDialog("New project", "Create", "Cancel", onConfirm = { confirmed = it }, onDismiss = {}, placeholder = "Project name") } }
        rule.onNodeWithText("Create").performClick()
        assertEquals(null, confirmed)
        rule.onNode(hasSetTextAction()).performTextInput("  Garden  ")
        rule.onNodeWithText("Create").performClick()
        assertEquals("Garden", confirmed)
    }
}
