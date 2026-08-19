package com.example.export

import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.ItemTagCrossRef
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.OutlineItemEntity
import com.example.data.local.entity.TagEntity
import com.example.domain.repository.OutlinerRepository
import org.json.JSONArray
import org.json.JSONObject

object DatabaseBackupManager {

    const val BACKUP_SCHEMA_VERSION = 1

    data class BackupData(
        val version: Int,
        val appName: String,
        val exportedAt: Long,
        val folders: List<FolderEntity>,
        val notes: List<NoteEntity>,
        val items: List<OutlineItemEntity>,
        val tags: List<TagEntity>,
        val crossRefs: List<ItemTagCrossRef>
    )

    suspend fun createBackupJson(repository: OutlinerRepository): String {
        val folders = repository.getAllFoldersForBackup()
        val notes = repository.getAllNotesForBackup()
        val items = repository.getAllItemsForBackup()
        val tags = repository.getAllTagsForBackup()
        val crossRefs = repository.getAllCrossRefsForBackup()

        val root = JSONObject()
        root.put("version", BACKUP_SCHEMA_VERSION)
        root.put("app", "Outliner")
        root.put("exportedAt", System.currentTimeMillis())

        val foldersArray = JSONArray()
        for (f in folders) {
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("name", f.name)
            obj.put("parentId", f.parentId ?: JSONObject.NULL)
            obj.put("sortOrder", f.sortOrder)
            obj.put("color", f.color ?: JSONObject.NULL)
            obj.put("isExpanded", f.isExpanded)
            obj.put("isDeleted", f.isDeleted)
            obj.put("deletedAt", f.deletedAt ?: JSONObject.NULL)
            obj.put("createdAt", f.createdAt)
            obj.put("updatedAt", f.updatedAt)
            foldersArray.put(obj)
        }
        root.put("folders", foldersArray)

        val notesArray = JSONArray()
        for (n in notes) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("folderId", n.folderId ?: JSONObject.NULL)
            obj.put("title", n.title)
            obj.put("isFavorite", n.isFavorite)
            obj.put("isDeleted", n.isDeleted)
            obj.put("deletedAt", n.deletedAt ?: JSONObject.NULL)
            obj.put("sortOrder", n.sortOrder)
            obj.put("createdAt", n.createdAt)
            obj.put("updatedAt", n.updatedAt)
            obj.put("lastAccessedAt", n.lastAccessedAt)
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        val itemsArray = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("noteId", item.noteId)
            obj.put("parentId", item.parentId ?: JSONObject.NULL)
            obj.put("sortOrder", item.sortOrder)
            obj.put("text", item.text)
            obj.put("isCollapsed", item.isCollapsed)
            obj.put("hasCheckbox", item.hasCheckbox)
            obj.put("isChecked", item.isChecked)
            obj.put("headingLevel", item.headingLevel)
            obj.put("isBold", item.isBold)
            obj.put("isItalic", item.isItalic)
            obj.put("isStrikethrough", item.isStrikethrough)
            obj.put("isCode", item.isCode)
            obj.put("textColor", item.textColor ?: JSONObject.NULL)
            obj.put("backgroundColor", item.backgroundColor ?: JSONObject.NULL)
            obj.put("url", item.url ?: JSONObject.NULL)
            obj.put("mediaType", item.mediaType ?: JSONObject.NULL)
            obj.put("mediaUri", item.mediaUri ?: JSONObject.NULL)
            obj.put("mediaTitle", item.mediaTitle ?: JSONObject.NULL)
            obj.put("createdAt", item.createdAt)
            obj.put("updatedAt", item.updatedAt)
            itemsArray.put(obj)
        }
        root.put("outline_items", itemsArray)

        val tagsArray = JSONArray()
        for (t in tags) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("color", t.color ?: JSONObject.NULL)
            obj.put("createdAt", t.createdAt)
            tagsArray.put(obj)
        }
        root.put("tags", tagsArray)

        val crossRefsArray = JSONArray()
        for (c in crossRefs) {
            val obj = JSONObject()
            obj.put("itemId", c.itemId)
            obj.put("tagId", c.tagId)
            crossRefsArray.put(obj)
        }
        root.put("item_tag_cross_refs", crossRefsArray)

        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): Result<BackupData> {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val appName = root.optString("app", "Outliner")
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

            val foldersList = mutableListOf<FolderEntity>()
            val foldersArray = root.optJSONArray("folders") ?: JSONArray()
            for (i in 0 until foldersArray.length()) {
                val obj = foldersArray.getJSONObject(i)
                foldersList.add(
                    FolderEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        parentId = if (obj.isNull("parentId")) null else obj.getString("parentId"),
                        sortOrder = obj.optInt("sortOrder", 0),
                        color = if (obj.isNull("color")) null else obj.getString("color"),
                        isExpanded = obj.optBoolean("isExpanded", true),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        deletedAt = if (obj.isNull("deletedAt")) null else obj.getLong("deletedAt"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val notesList = mutableListOf<NoteEntity>()
            val notesArray = root.optJSONArray("notes") ?: JSONArray()
            for (i in 0 until notesArray.length()) {
                val obj = notesArray.getJSONObject(i)
                notesList.add(
                    NoteEntity(
                        id = obj.getString("id"),
                        folderId = if (obj.isNull("folderId")) null else obj.getString("folderId"),
                        title = obj.getString("title"),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        deletedAt = if (obj.isNull("deletedAt")) null else obj.getLong("deletedAt"),
                        sortOrder = obj.optInt("sortOrder", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        lastAccessedAt = obj.optLong("lastAccessedAt", System.currentTimeMillis())
                    )
                )
            }

            val itemsList = mutableListOf<OutlineItemEntity>()
            val itemsArray = root.optJSONArray("outline_items") ?: JSONArray()
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.getJSONObject(i)
                itemsList.add(
                    OutlineItemEntity(
                        id = obj.getString("id"),
                        noteId = obj.getString("noteId"),
                        parentId = if (obj.isNull("parentId")) null else obj.getString("parentId"),
                        sortOrder = obj.optInt("sortOrder", 0),
                        text = obj.optString("text", ""),
                        isCollapsed = obj.optBoolean("isCollapsed", false),
                        hasCheckbox = obj.optBoolean("hasCheckbox", false),
                        isChecked = obj.optBoolean("isChecked", false),
                        headingLevel = obj.optInt("headingLevel", 0),
                        isBold = obj.optBoolean("isBold", false),
                        isItalic = obj.optBoolean("isItalic", false),
                        isStrikethrough = obj.optBoolean("isStrikethrough", false),
                        isCode = obj.optBoolean("isCode", false),
                        textColor = if (obj.isNull("textColor")) null else obj.getString("textColor"),
                        backgroundColor = if (obj.isNull("backgroundColor")) null else obj.getString("backgroundColor"),
                        url = if (obj.isNull("url")) null else obj.getString("url"),
                        mediaType = if (obj.isNull("mediaType")) null else obj.getString("mediaType"),
                        mediaUri = if (obj.isNull("mediaUri")) null else obj.getString("mediaUri"),
                        mediaTitle = if (obj.isNull("mediaTitle")) null else obj.getString("mediaTitle"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val tagsList = mutableListOf<TagEntity>()
            val tagsArray = root.optJSONArray("tags") ?: JSONArray()
            for (i in 0 until tagsArray.length()) {
                val obj = tagsArray.getJSONObject(i)
                tagsList.add(
                    TagEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        color = if (obj.isNull("color")) null else obj.getString("color"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val crossRefsList = mutableListOf<ItemTagCrossRef>()
            val crossRefsArray = root.optJSONArray("item_tag_cross_refs") ?: JSONArray()
            for (i in 0 until crossRefsArray.length()) {
                val obj = crossRefsArray.getJSONObject(i)
                crossRefsList.add(
                    ItemTagCrossRef(
                        itemId = obj.getString("itemId"),
                        tagId = obj.getString("tagId")
                    )
                )
            }

            Result.success(
                BackupData(
                    version = version,
                    appName = appName,
                    exportedAt = exportedAt,
                    folders = foldersList,
                    notes = notesList,
                    items = itemsList,
                    tags = tagsList,
                    crossRefs = crossRefsList
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
