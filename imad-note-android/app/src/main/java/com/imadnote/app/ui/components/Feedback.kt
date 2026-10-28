package com.imadnote.app.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.imadnote.app.R
import com.imadnote.app.data.TaskEntity
import com.imadnote.app.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** App-wide success feedback: one snackbar at a time, at most one action (usually Undo). */
class Feedback(val host: SnackbarHostState, private val scope: CoroutineScope) {
    fun show(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val result = host.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }
}

val LocalFeedback = staticCompositionLocalOf<Feedback> { error("Feedback not provided") }

/** Shared completion behaviour for every task list: toggle, then offer Undo after completing. */
class TaskActions(private val tasks: TaskRepository, private val scope: CoroutineScope) {
    fun toggle(task: TaskEntity, onCompleted: (TaskRepository.Completion) -> Unit) {
        scope.launch { tasks.toggleComplete(task)?.let(onCompleted) }
    }

    fun undo(completion: TaskRepository.Completion) {
        scope.launch { tasks.undoComplete(completion) }
    }
}

@Composable
fun rememberTaskToggle(actions: TaskActions): (TaskEntity) -> Unit {
    val feedback = LocalFeedback.current
    val message = stringResource(R.string.task_completed)
    val undo = stringResource(R.string.action_undo)
    return remember(actions, feedback, message, undo) {
        { task -> actions.toggle(task) { completion -> feedback.show(message, undo) { actions.undo(completion) } } }
    }
}
