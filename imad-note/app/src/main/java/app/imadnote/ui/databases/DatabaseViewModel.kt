package app.imadnote.ui.databases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.imadnote.AppContainer
import app.imadnote.data.db.CellEntity
import app.imadnote.data.db.CollectionEntity
import app.imadnote.data.db.PropertyEntity
import app.imadnote.data.db.PropertyType
import app.imadnote.data.db.RecordEntity
import app.imadnote.data.db.RecordRelationEntity
import app.imadnote.data.db.RecordTitle
import app.imadnote.data.db.ViewEntity
import app.imadnote.data.db.ViewType
import app.imadnote.domain.db.Cells
import app.imadnote.domain.db.DbJson
import app.imadnote.domain.db.Formula
import app.imadnote.domain.db.PropertyConfig
import app.imadnote.domain.db.Row
import app.imadnote.domain.db.Value
import app.imadnote.domain.db.ViewConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class DbState(
    val collection: CollectionEntity,
    val properties: List<PropertyEntity>,
    val rows: List<Row>,
    val views: List<ViewEntity>,
    /** Titles of records in every database of the workspace, for relations. */
    val titles: List<RecordTitle>,
    val collections: List<CollectionEntity>,
) {
    val title: PropertyEntity? = properties.firstOrNull { it.type == PropertyType.TITLE }
    fun config(p: PropertyEntity): PropertyConfig = DbJson.property(p.config)
    fun titleOf(recordId: String): String = titles.firstOrNull { it.recordId == recordId }?.title.orEmpty()
}

/** Resolves stored cells, relations, timestamps and formulas into typed [Row]s. */
object RowBuilder {
    fun build(
        properties: List<PropertyEntity>,
        records: List<RecordEntity>,
        cells: List<CellEntity>,
        relations: List<RecordRelationEntity>,
        titles: Map<String, String>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Row> {
        val cellsByRecord = cells.groupBy { it.recordId }
        val linksByRecord = relations.groupBy { it.fromRecordId }
        val configs = properties.associate { it.id to DbJson.property(it.config) }
        val byName = properties.associateBy { it.name.lowercase() }
        return records.map { record ->
            val raw = cellsByRecord[record.id].orEmpty().associate { it.propertyId to it.value }
            val links = linksByRecord[record.id].orEmpty().groupBy({ it.propertyId }) { it.toRecordId }
            val values = HashMap<String, Value>()
            for (p in properties) {
                values[p.id] = when (p.type) {
                    PropertyType.RELATION -> Value.Many(links[p.id].orEmpty().map { titles[it].orEmpty().ifEmpty { "…" } })
                    PropertyType.CREATED_TIME -> Cells.timestamp(record.createdAt, zone)
                    PropertyType.LAST_EDITED_TIME -> Cells.timestamp(record.updatedAt, zone)
                    PropertyType.FORMULA -> continue
                    else -> Cells.decode(p.type, raw[p.id], configs.getValue(p.id))
                }
            }
            val errors = HashMap<String, String>()
            val visiting = HashSet<String>()
            fun formula(p: PropertyEntity): Value {
                values[p.id]?.let { return it }
                if (!visiting.add(p.id)) throw IllegalStateException("cycle")
                val result = Formula.evaluate(configs.getValue(p.id).formula.orEmpty(), today) { name ->
                    val target = byName[name.lowercase()] ?: return@evaluate null
                    if (target.type == PropertyType.FORMULA) {
                        val v = try { formula(target) } catch (_: IllegalStateException) { throw Formula.FormulaException("Circular reference") }
                        // An error in a referenced formula makes this one an error too.
                        errors[target.id]?.let { throw Formula.FormulaException(it) }
                        v
                    } else values[target.id] ?: Value.Empty
                }
                visiting.remove(p.id)
                val value = when (result) {
                    is Formula.Result.Ok -> result.value
                    is Formula.Result.Error -> { errors[p.id] = result.message; Value.Empty }
                }
                values[p.id] = value
                return value
            }
            for (p in properties.filter { it.type == PropertyType.FORMULA }) {
                try { formula(p) } catch (_: IllegalStateException) { errors[p.id] = "Circular reference"; values[p.id] = Value.Empty }
            }
            Row(record.id, record.sortIndex, values, raw, links, errors)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseViewModel(private val collectionId: String, private val container: AppContainer) : ViewModel() {
    private val repo = container.collections

    /** `null` while loading; a missing collection is reported through [missing]. */
    val state: StateFlow<DbState?> = repo.observe(collectionId).flatMapLatest { collection ->
        if (collection == null) flowOf(null)
        else {
            val content = combine(repo.observeProperties(collectionId), repo.observeRecords(collectionId), repo.observeCells(collectionId), repo.observeRelations(collectionId), repo.observeViews(collectionId)) { p, r, c, rel, v ->
                Content(p, r, c, rel, v)
            }
            combine(content, repo.observeRecordTitles(collection.workspaceId), repo.observeAll(collection.workspaceId)) { c, titles, all ->
                val titleMap = titles.associate { it.recordId to it.title.orEmpty() }
                DbState(collection, c.properties, RowBuilder.build(c.properties, c.records, c.cells, c.relations, titleMap), c.views, titles, all)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val missing: StateFlow<Boolean> = repo.observe(collectionId).flatMapLatest { flowOf(it == null) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private data class Content(val properties: List<PropertyEntity>, val records: List<RecordEntity>, val cells: List<CellEntity>, val relations: List<RecordRelationEntity>, val views: List<ViewEntity>)

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }

    fun rename(name: String) = launch { state.value?.collection?.let { repo.update(it.copy(name = name)) } }
    fun setIcon(icon: String) = launch { state.value?.collection?.let { repo.update(it.copy(icon = icon)) } }
    fun delete() = launch { repo.delete(collectionId) }

    fun addRecord(initial: Map<String, String> = emptyMap(), onCreated: (String) -> Unit = {}) = launch { onCreated(repo.addRecord(collectionId, initial).id) }
    fun setCell(recordId: String, propertyId: String, value: String) = launch { repo.setCell(recordId, propertyId, value) }
    fun setRelations(recordId: String, propertyId: String, targets: List<String>) = launch { repo.setRelations(recordId, propertyId, targets) }
    fun duplicateRecord(recordId: String) = launch { repo.duplicateRecord(recordId) }
    fun deleteRecord(recordId: String) = launch { repo.deleteRecord(recordId) }

    fun addProperty(name: String, type: PropertyType, onCreated: (PropertyEntity) -> Unit = {}) = launch { onCreated(repo.addProperty(collectionId, name, type)) }
    fun renameProperty(p: PropertyEntity, name: String) = launch { repo.renameProperty(p, name) }
    fun changeType(p: PropertyEntity, type: PropertyType) = launch { repo.changePropertyType(p, type) }
    fun updateConfig(p: PropertyEntity, config: PropertyConfig) = launch { repo.updatePropertyConfig(p, config) }
    fun deleteProperty(p: PropertyEntity) = launch { repo.deleteProperty(p) }

    fun addView(name: String, type: ViewType, onCreated: (String) -> Unit) = launch {
        val props = state.value?.properties.orEmpty()
        val config = ViewConfig(
            groupBy = if (type == ViewType.BOARD) props.firstOrNull { it.type == PropertyType.STATUS || it.type == PropertyType.SELECT }?.id else null,
            dateProperty = if (type == ViewType.CALENDAR) props.firstOrNull { it.type == PropertyType.DATE }?.id else null,
        )
        onCreated(repo.addView(collectionId, name, type, config).id)
    }
    fun renameView(view: ViewEntity, name: String) = launch { repo.updateView(view.copy(name = name.trim().ifEmpty { view.name })) }
    fun updateView(view: ViewEntity, config: ViewConfig) = launch { repo.updateView(view, config) }
    fun deleteView(view: ViewEntity) = launch { repo.deleteView(view) }
}
