package app.imadnote.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.imadnote.R
import app.imadnote.data.db.PageEntity
import app.imadnote.data.db.PageKind
import app.imadnote.data.db.WorkspaceEntity
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.EmojiTile
import app.imadnote.ui.components.SectionHeader
import app.imadnote.ui.components.TextInputDialog
import app.imadnote.ui.theme.ImadTheme

@Composable
fun Sidebar(
    workspaces: List<WorkspaceEntity>,
    current: WorkspaceEntity?,
    tree: PageTree?,
    currentRoute: String?,
    actions: PageActions,
    onNavigate: (Destination) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSearch: () -> Unit,
    onSelectWorkspace: (String) -> Unit,
    onCreateWorkspace: (String) -> Unit,
    onNewPage: () -> Unit,
    onNewFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ImadTheme.colors
    var expanded by rememberSaveable { mutableStateOf(setOf<String>()) }
    var namingFolder by remember { mutableStateOf(false) }

    Column(modifier.fillMaxHeight().background(colors.surface).padding(horizontal = 10.dp)) {
        WorkspaceSwitcher(workspaces, current, onSelectWorkspace, onCreateWorkspace)
        Spacer(Modifier.height(4.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SidebarItem(Icons.Rounded.Search, stringResource(R.string.nav_search), currentRoute == Routes.SEARCH, onClick = onOpenSearch)
            for (destination in Destination.entries) {
                SidebarItem(destination.icon, stringResource(destination.label), currentRoute == destination.route, Modifier.testTag("nav-${destination.route}")) { onNavigate(destination) }
            }
            SidebarItem(Icons.Rounded.DeleteOutline, stringResource(R.string.nav_trash), currentRoute == Routes.TRASH, onClick = onOpenTrash)

            if (tree != null && tree.favorites.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionHeader(stringResource(R.string.sidebar_favorites), Modifier.padding(horizontal = 8.dp))
                for (page in tree.favorites) PageTreeRow(page, 0, false, null, tree, actions, showMenu = false)
            }

            Spacer(Modifier.height(12.dp))
            SectionHeader(stringResource(R.string.sidebar_pages), Modifier.padding(start = 8.dp)) {
                AppIconButton(Icons.Rounded.CreateNewFolder, stringResource(R.string.folder_new), { namingFolder = true }, Modifier.size(36.dp))
                AppIconButton(Icons.Rounded.Add, stringResource(R.string.page_new), onNewPage, Modifier.size(36.dp))
            }
            if (tree != null) {
                @Composable
                fun branch(nodes: List<PageEntity>, depth: Int) {
                    for (node in nodes) {
                        val children = tree.children(node.id)
                        val open = node.id in expanded
                        PageTreeRow(node, depth, open, { expanded = if (open) expanded - node.id else expanded + node.id }.takeIf { children.isNotEmpty() }, tree, actions)
                        if (open) branch(children, depth + 1)
                    }
                }
                branch(tree.roots, 0)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (namingFolder) {
        TextInputDialog(
            title = stringResource(R.string.folder_new),
            confirmLabel = stringResource(R.string.action_create),
            dismissLabel = stringResource(R.string.action_cancel),
            placeholder = stringResource(R.string.folder_name_hint),
            onConfirm = { name -> namingFolder = false; onNewFolder(name) },
            onDismiss = { namingFolder = false },
        )
    }
}

@Composable
private fun WorkspaceSwitcher(workspaces: List<WorkspaceEntity>, current: WorkspaceEntity?, onSelect: (String) -> Unit, onCreate: (String) -> Unit) {
    val colors = ImadTheme.colors
    var open by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    Box(Modifier.padding(top = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.workspace_switch)) { open = true }
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmojiTile(current?.icon ?: "🏠", size = 34.dp, background = colors.primarySoft)
            Spacer(Modifier.width(10.dp))
            Text(
                current?.name.orEmpty(), style = MaterialTheme.typography.titleMedium, color = colors.text,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Icon(Icons.Rounded.UnfoldMore, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(open, { open = false }, containerColor = colors.surface, shape = MaterialTheme.shapes.medium) {
            for (workspace in workspaces) {
                DropdownMenuItem(
                    text = { Text(workspace.name, fontWeight = if (workspace.id == current?.id) FontWeight.SemiBold else FontWeight.Normal) },
                    leadingIcon = { Text(workspace.icon, fontSize = 18.sp) },
                    onClick = { open = false; onSelect(workspace.id) },
                )
            }
            HorizontalDivider(color = colors.border)
            DropdownMenuItem(
                text = { Text(stringResource(R.string.workspace_new)) },
                leadingIcon = { Icon(Icons.Rounded.Add, null, tint = colors.textSecondary) },
                onClick = { open = false; creating = true },
            )
        }
    }
    if (creating) {
        TextInputDialog(
            title = stringResource(R.string.workspace_new),
            confirmLabel = stringResource(R.string.action_create),
            dismissLabel = stringResource(R.string.action_cancel),
            placeholder = stringResource(R.string.workspace_name_hint),
            onConfirm = { name -> creating = false; onCreate(name) },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun SidebarItem(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = ImadTheme.colors
    Row(
        modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.primarySoft else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick).semantics { this.selected = selected }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) colors.primary else colors.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) colors.primary else colors.text,
        )
    }
}

@Composable
private fun PageTreeRow(page: PageEntity, depth: Int, open: Boolean, onToggle: (() -> Unit)?, tree: PageTree, actions: PageActions, showMenu: Boolean = true) {
    val colors = ImadTheme.colors
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button) { if (page.kind == PageKind.FOLDER) onToggle?.invoke() else actions.onOpen(page) }
            .padding(start = (depth * 14).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onToggle != null) {
            val label = stringResource(if (open) R.string.page_collapse else R.string.page_expand)
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClickLabel = label, onClick = onToggle), contentAlignment = Alignment.Center) {
                Icon(if (open) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight, label, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
            }
        } else {
            Spacer(Modifier.width(28.dp))
        }
        Text(page.displayIcon, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            page.displayTitle(), style = MaterialTheme.typography.bodyMedium, color = colors.text,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        if (showMenu) PageMenuButton(page, tree, actions, Modifier.size(40.dp))
    }
}
