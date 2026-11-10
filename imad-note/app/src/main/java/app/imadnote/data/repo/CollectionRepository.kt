package app.imadnote.data.repo

import androidx.room.withTransaction
import app.imadnote.core.Clock
import app.imadnote.core.newId
import app.imadnote.data.db.AppDatabase
import app.imadnote.data.db.CellEntity
import app.imadnote.data.db.CollectionEntity
import app.imadnote.data.db.PropertyEntity
import app.imadnote.data.db.PropertyType
import app.imadnote.data.db.RecordEntity
import app.imadnote.data.db.RecordRelationEntity
import app.imadnote.data.db.ViewEntity
import app.imadnote.data.db.ViewType
import app.imadnote.domain.db.Cells
import app.imadnote.domain.db.DbJson
import app.imadnote.domain.db.PropertyConfig
import app.imadnote.domain.db.SelectOption
import app.imadnote.domain.db.ViewConfig
import kotlinx.coroutines.flow.Flow

/** User-defined databases: schema (properties), records, cell values, relations and saved views. */
class CollectionRepository(
    private val db: AppDatabase,
    private val indexer: SearchIndexer,
    private val clock: Clock,
) {
    private val dao = db.collections()

    fun observeAll(workspaceId: String): Flow<List<CollectionEntity>> = dao.observeAll(workspaceId)
    fun observe(id: String) = dao.observe(id)
    fun observeProperties(id: String) = dao.observeProperties(id)
    fun observeRecords(id: String) = dao.observeRecords(id)
    fun observeCells(id: String) = dao.observeCells(id)
    fun observeRelations(id: String) = dao.observeRelations(id)
    fun observeViews(id: String) = dao.observeViews(id)
    fun observeRecordTitles(workspaceId: String) = dao.observeRecordTitles(workspaceId)
    fun observeRecordCount(id: String) = dao.observeRecordCount(id)

    /** A new database starts with Name, Status and Date properties and a table view. */
    suspend fun create(workspaceId: String, name: String, icon: String = "🗂️"): CollectionEntity = db.withTransaction {
        val now = clock.now()
        val collection = CollectionEntity(newId(), workspaceId, name.trim().ifEmpty { "Database" }, icon, sortIndex = 0, createdAt = now, updatedAt = now)
        dao.upsert(collection)
        dao.upsertProperty(PropertyEntity(newId(), collection.id, "Name", PropertyType.TITLE, sortIndex = 0))
        dao.upsertProperty(PropertyEntity(newId(), collection.id, "Status", PropertyType.STATUS, DbJson.encode(PropertyConfig(options = Cells.defaultStatus)), 1))
        dao.upsertProperty(PropertyEntity(newId(), collection.id, "Date", PropertyType.DATE, sortIndex = 2))
        dao.upsertView(ViewEntity(newId(), collection.id, "Table", ViewType.TABLE, sortIndex = 0))
        collection
    }

    suspend fun update(collection: CollectionEntity) = dao.upsert(collection.copy(name = collection.name.trim().ifEmpty { "Database" }, updatedAt = clock.now()))

    suspend fun delete(id: String) = db.withTransaction {
        indexer.removeChildren(id) // record entries use the collection as their parent
        dao.delete(id)
    }

    // ---- properties ----

    suspend fun addProperty(collectionId: String, name: String, type: PropertyType): PropertyEntity = db.withTransaction {
        require(type != PropertyType.TITLE) { "A database has exactly one title property" }
        val config = when (type) {
            PropertyType.STATUS -> PropertyConfig(options = Cells.defaultStatus)
            else -> PropertyConfig()
        }
        val property = PropertyEntity(newId(), collectionId, name.trim().ifEmpty { "Property" }, type, DbJson.encode(config), (dao.properties(collectionId).maxOfOrNull { it.sortIndex } ?: 0) + 1)
        dao.upsertProperty(property)
        property
    }

    suspend fun renameProperty(property: PropertyEntity, name: String) = dao.upsertProperty(property.copy(name = name.trim().ifEmpty { property.name }))

    suspend fun updatePropertyConfig(property: PropertyEntity, config: PropertyConfig) = db.withTransaction {
        dao.upsertProperty(property.copy(config = DbJson.encode(config)))
        // Values pointing at removed options are cleared so they cannot reappear.
        if (property.type == PropertyType.SELECT || property.type == PropertyType.STATUS || property.type == PropertyType.MULTI_SELECT) {
            val valid = config.options.mapTo(HashSet()) { it.id }
            for (record in dao.records(property.collectionId)) {
                val cell = dao.cellsForRecord(record.id).firstOrNull { it.propertyId == property.id } ?: continue
                if (property.type == PropertyType.MULTI_SELECT) {
                    val kept = DbJson.ids(cell.value).filter { it in valid }
                    if (kept.isEmpty()) dao.deleteCell(record.id, property.id) else dao.upsertCell(cell.copy(value = DbJson.encodeIds(kept)))
                } else if (cell.value !in valid) dao.deleteCell(record.id, property.id)
            }
        }
    }

    /**
     * Changes a property's type. Values are converted where it is meaningful (text ⇄ number, text → select
     * creates options) and dropped otherwise. The title property cannot change type.
     */
    suspend fun changePropertyType(property: PropertyEntity, type: PropertyType) = db.withTransaction {
        if (property.type == PropertyType.TITLE || type == PropertyType.TITLE || property.type == type) return@withTransaction
        val old = DbJson.property(property.config)
        val cells = dao.allCells().filter { it.propertyId == property.id }
        dao.clearCells(property.id)
        if (property.type == PropertyType.RELATION) {
            dao.relationsForProperty(property.id).forEach { dao.unlink(it) }
        }
        var config = PropertyConfig()
        val textual = setOf(PropertyType.TEXT, PropertyType.URL, PropertyType.EMAIL, PropertyType.PHONE, PropertyType.PERSON)
        val converted = ArrayList<CellEntity>()
        when {
            type == PropertyType.SELECT || type == PropertyType.STATUS || type == PropertyType.MULTI_SELECT -> {
                // Text and select values become options with the same names.
                val names = cells.flatMap { cell ->
                    when (property.type) {
                        PropertyType.SELECT, PropertyType.STATUS -> listOfNotNull(old.options.firstOrNull { it.id == cell.value }?.name)
                        PropertyType.MULTI_SELECT -> DbJson.ids(cell.value).mapNotNull { id -> old.options.firstOrNull { it.id == id }?.name }
                        in textual -> listOf(cell.value)
                        else -> emptyList()
                    }
                }.map(String::trim).filter(String::isNotEmpty).distinct()
                val options = (if (type == PropertyType.STATUS && names.isEmpty()) Cells.defaultStatus else names.mapIndexed { i, n -> SelectOption(newId(), n, OPTION_COLORS[i % OPTION_COLORS.size]) })
                config = PropertyConfig(options = options)
                for (cell in cells) {
                    val value = Cells.decode(property.type, cell.value, old).asText()
                    val ids = value.split(", ").mapNotNull { name -> options.firstOrNull { it.name == name.trim() }?.id }
                    if (ids.isEmpty()) continue
                    converted += cell.copy(value = if (type == PropertyType.MULTI_SELECT) DbJson.encodeIds(ids) else ids.first())
                }
            }
            type == PropertyType.NUMBER -> cells.forEach { c -> Cells.decode(property.type, c.value, old).asText().toDoubleOrNull()?.let { converted += c.copy(value = it.toString()) } }
            type in textual -> cells.forEach { c -> Cells.decode(property.type, c.value, old).asText().takeIf { it.isNotEmpty() }?.let { converted += c.copy(value = it) } }
            type == PropertyType.CHECKBOX -> cells.forEach { c -> if (c.value == "true") converted += c }
            type == PropertyType.DATE -> cells.forEach { c -> if (Cells.decode(PropertyType.DATE, c.value, old) != app.imadnote.domain.db.Value.Empty) converted += c }
        }
        dao.upsertProperty(property.copy(type = type, config = DbJson.encode(config)))
        dao.upsertCells(converted)
        cells.map { it.recordId }.distinct().forEach { indexer.putRecord(db, it) }
    }

    suspend fun deleteProperty(property: PropertyEntity) = db.withTransaction {
        if (property.type == PropertyType.TITLE) return@withTransaction
        dao.deleteProperty(property.id) // cells and relations cascade
        dao.records(property.collectionId).forEach { indexer.putRecord(db, it.id) }
    }

    // ---- records ----

    suspend fun addRecord(collectionId: String, initial: Map<String, String> = emptyMap()): RecordEntity = db.withTransaction {
        val now = clock.now()
        val record = RecordEntity(newId(), collectionId, (dao.records(collectionId).maxOfOrNull { it.sortIndex } ?: -1) + 1, now, now)
        dao.upsertRecord(record)
        dao.upsertCells(initial.filterValues { it.isNotEmpty() }.map { (p, v) -> CellEntity(record.id, p, v) })
        indexer.putRecord(db, record.id)
        touch(collectionId)
        record
    }

    /** Stores a cell value; an empty string clears it. */
    suspend fun setCell(recordId: String, propertyId: String, value: String) = db.withTransaction {
        if (value.isEmpty()) dao.deleteCell(recordId, propertyId) else dao.upsertCell(CellEntity(recordId, propertyId, value))
        dao.touchRecord(recordId, clock.now())
        indexer.putRecord(db, recordId)
    }

    suspend fun setRelations(recordId: String, propertyId: String, targets: List<String>) = db.withTransaction {
        dao.clearRelations(recordId, propertyId)
        dao.linkAll(targets.distinct().filter { it != recordId }.map { RecordRelationEntity(recordId, propertyId, it) })
        dao.touchRecord(recordId, clock.now())
    }

    suspend fun duplicateRecord(recordId: String): RecordEntity? = db.withTransaction {
        val source = dao.record(recordId) ?: return@withTransaction null
        val copy = addRecord(source.collectionId, dao.cellsForRecord(recordId).associate { it.propertyId to it.value })
        dao.linkAll(dao.relationsFrom(recordId).map { it.copy(fromRecordId = copy.id) })
        copy
    }

    suspend fun deleteRecord(recordId: String) = db.withTransaction {
        indexer.remove(recordId)
        dao.deleteRecord(recordId)
    }

    private suspend fun touch(collectionId: String) {
        dao.get(collectionId)?.let { dao.upsert(it.copy(updatedAt = clock.now())) }
    }

    // ---- views ----

    suspend fun addView(collectionId: String, name: String, type: ViewType, config: ViewConfig = ViewConfig()): ViewEntity = db.withTransaction {
        val view = ViewEntity(newId(), collectionId, name.trim().ifEmpty { type.name.lowercase().replaceFirstChar(Char::uppercase) }, type, DbJson.encode(config), (dao.views(collectionId).maxOfOrNull { it.sortIndex } ?: -1) + 1)
        dao.upsertView(view)
        view
    }

    suspend fun updateView(view: ViewEntity, config: ViewConfig = DbJson.view(view.config)) = dao.upsertView(view.copy(config = DbJson.encode(config)))

    /** The last view cannot be deleted. */
    suspend fun deleteView(view: ViewEntity): Boolean = db.withTransaction {
        if (dao.views(view.collectionId).size <= 1) return@withTransaction false
        dao.deleteView(view.id)
        true
    }

    companion object {
        val OPTION_COLORS = listOf("blue", "green", "orange", "purple", "red", "gray")
    }
}
