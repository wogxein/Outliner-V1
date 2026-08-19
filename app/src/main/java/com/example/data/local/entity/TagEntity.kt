package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "item_tag_cross_ref",
    primaryKeys = ["itemId", "tagId"],
    indices = [
        Index(value = ["tagId"]),
        Index(value = ["itemId"])
    ]
)
data class ItemTagCrossRef(
    val itemId: String,
    val tagId: String
)
