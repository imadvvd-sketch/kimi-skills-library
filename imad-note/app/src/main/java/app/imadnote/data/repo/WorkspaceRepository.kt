package app.imadnote.data.repo

import app.imadnote.core.Clock
import app.imadnote.core.newId
import app.imadnote.data.SettingsStore
import app.imadnote.data.db.AppDatabase
import app.imadnote.data.db.WorkspaceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class WorkspaceRepository(
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val clock: Clock,
) {
    private val dao = db.workspaces()

    val workspaces: Flow<List<WorkspaceEntity>> = dao.observeAll()

    /** The selected workspace; falls back to the first one if the stored id no longer exists. */
    val current: Flow<WorkspaceEntity?> = combine(workspaces, settings.currentWorkspaceId) { all, id ->
        all.firstOrNull { it.id == id } ?: all.firstOrNull()
    }

    /** Creates the personal workspace on first launch. Safe to call repeatedly. */
    suspend fun ensureDefault(personalName: String): WorkspaceEntity {
        dao.getAll().firstOrNull()?.let { existing ->
            val selected = settings.currentWorkspaceId.first()
            if (selected == null || dao.get(selected) == null) settings.setCurrentWorkspace(existing.id)
            return existing
        }
        return create(personalName, "🏠")
    }

    suspend fun create(name: String, icon: String): WorkspaceEntity {
        val workspace = WorkspaceEntity(newId(), name.trim().ifEmpty { "Workspace" }, icon, dao.count(), clock.now())
        dao.upsert(workspace)
        settings.setCurrentWorkspace(workspace.id)
        return workspace
    }

    suspend fun update(workspace: WorkspaceEntity) = dao.upsert(workspace.copy(name = workspace.name.trim().ifEmpty { "Workspace" }))

    suspend fun select(id: String) = settings.setCurrentWorkspace(id)

    /** Deletes a workspace and everything in it. The last workspace cannot be deleted. */
    suspend fun delete(id: String): Boolean {
        val all = dao.getAll()
        if (all.size <= 1) return false
        if (all.none { it.id == id }) return false
        dao.delete(id)
        db.openHelper.writableDatabase.execSQL("DELETE FROM search_index WHERE workspaceId = ?", arrayOf(id))
        if (settings.currentWorkspaceId.first() == id) settings.setCurrentWorkspace(all.first { it.id != id }.id)
        return true
    }
}
