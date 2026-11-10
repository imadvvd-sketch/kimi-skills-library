package app.imadnote.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.imadnote.AppContainer
import app.imadnote.data.db.LabelEntity
import app.imadnote.data.db.ProjectEntity
import app.imadnote.data.db.TaskEntity
import app.imadnote.data.db.TaskLabelCrossRef
import app.imadnote.data.db.TaskStatus
import app.imadnote.domain.tasks.TaskQuickParser
import app.imadnote.ui.workspaceId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Everything the task screens need, for the current workspace. */
data class TaskData(
    val workspaceId: String,
    val tasks: List<TaskEntity>,
    val labels: List<LabelEntity>,
    val links: List<TaskLabelCrossRef>,
    val projects: List<ProjectEntity>,
) {
    private val labelById = labels.associateBy { it.id }
    private val labelsByTask = links.groupBy({ it.taskId }) { labelById[it.labelId] }
    val projectById = projects.associateBy { it.id }
    private val subtasks = tasks.filter { it.parentId != null }.groupBy { it.parentId }

    val topLevel: List<TaskEntity> = tasks.filter { it.parentId == null }
    fun labelsOf(taskId: String): List<LabelEntity> = labelsByTask[taskId].orEmpty().filterNotNull()
    fun subtasksOf(taskId: String): List<TaskEntity> = subtasks[taskId].orEmpty()
}

val TaskEntity.isDone get() = status == TaskStatus.DONE
val TaskEntity.due: LocalDate? get() = dueDate?.let(LocalDate::ofEpochDay)

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.tasks

    val data: StateFlow<TaskData?> = container.workspaceId().flatMapLatest { ws ->
        if (ws == null) flowOf(null)
        else combine(repo.observeAll(ws), repo.observeLabels(ws), repo.observeTaskLabels(), container.projects.observeAll(ws)) { t, l, r, p ->
            TaskData(ws, t, l, r, p)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Parses quick-add shortcuts. [defaultDue] applies when the text names no date (e.g. adding from "Today"). */
    fun add(text: String, defaultDue: LocalDate? = null, projectId: String? = null, parentId: String? = null) {
        val ws = data.value?.workspaceId ?: return
        if (text.isBlank()) return
        val parsed = TaskQuickParser.parse(text, LocalDate.now())
        viewModelScope.launch {
            repo.create(ws, parsed.title, projectId, parentId, parsed.dueDate ?: defaultDue, parsed.priority.level, parsed.labels)
        }
    }

    fun setDone(id: String, done: Boolean) { viewModelScope.launch { repo.setCompleted(id, done) } }
    fun edit(id: String, transform: (TaskEntity) -> TaskEntity) { viewModelScope.launch { repo.edit(id, transform) } }
    fun delete(id: String) { viewModelScope.launch { repo.delete(id) } }
    fun setLabels(taskId: String, ids: Set<String>) { viewModelScope.launch { repo.setLabels(taskId, ids) } }

    fun createLabel(name: String, attachTo: String?) {
        val ws = data.value?.workspaceId ?: return
        viewModelScope.launch {
            val label = repo.createLabel(ws, name)
            if (attachTo != null) {
                val current = data.value?.labelsOf(attachTo)?.map { it.id }?.toSet().orEmpty()
                repo.setLabels(attachTo, current + label.id)
            }
        }
    }
}
