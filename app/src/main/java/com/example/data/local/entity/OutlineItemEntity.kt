package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "outline_items",
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["parentId"]),
        Index(value = ["noteId", "parentId", "sortOrder"])
    ]
)
data class OutlineItemEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val parentId: String? = null,
    val sortOrder: Int = 0,
    val text: String = "",
    val isCollapsed: Boolean = false,
    val hasCheckbox: Boolean = false,
    val isChecked: Boolean = false,
    val headingLevel: Int = 0, // 0 = normal, 1 = H1, 2 = H2, 3 = H3
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isCode: Boolean = false,
    val textColor: String? = null,
    val backgroundColor: String? = null,
    val url: String? = null,
    val mediaType: String? = null, // "youtube_video", "youtube_playlist", "audio", "file"
    val mediaUri: String? = null,
    val mediaTitle: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
