package com.example.domain.model

data class Folder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val sortOrder: Int = 0,
    val color: String? = null,
    val isExpanded: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val subfolders: List<Folder> = emptyList(),
    val noteCount: Int = 0
)

data class FolderTreeNode(
    val folder: Folder,
    val level: Int,
    val isExpanded: Boolean,
    val children: List<FolderTreeNode> = emptyList()
)
