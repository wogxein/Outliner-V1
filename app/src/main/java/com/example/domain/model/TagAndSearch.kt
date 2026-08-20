package com.example.domain.model

data class Tag(
    val id: String,
    val name: String,
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class SearchResult(
    val noteId: String,
    val noteTitle: String,
    val folderName: String?,
    val matchedItemId: String?,
    val matchedText: String,
    val isTitleMatch: Boolean,
    val updatedAt: Long
)
