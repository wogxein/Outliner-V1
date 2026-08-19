package com.example.domain.tree

import com.example.domain.model.OutlineItem
import com.example.domain.model.TreeItemNode
import java.util.UUID

object TreeOperations {

    /**
     * Builds a flattened visual list of TreeItemNode for the outliner UI,
     * respecting hierarchy, sorting, collapse state, and focus/zoom mode.
     */
    fun buildFlattenedTree(
        items: List<OutlineItem>,
        focusedRootId: String? = null
    ): List<TreeItemNode> {
        if (items.isEmpty()) return emptyList()

        val itemMap = items.associateBy { it.id }
        val childrenMap = items.groupBy { it.parentId }
            .mapValues { (_, list) -> list.sortedBy { it.sortOrder } }

        fun countTotalDescendants(id: String): Int {
            val children = childrenMap[id] ?: return 0
            var count = children.size
            for (child in children) {
                count += countTotalDescendants(child.id)
            }
            return count
        }

        val result = mutableListOf<TreeItemNode>()

        fun traverse(
            item: OutlineItem,
            level: Int,
            parentChain: List<String>,
            parentCollapsed: Boolean
        ) {
            val children = childrenMap[item.id] ?: emptyList()
            val hasChildren = children.isNotEmpty()
            val isVisible = !parentCollapsed

            if (isVisible) {
                result.add(
                    TreeItemNode(
                        item = item,
                        level = level,
                        directChildrenCount = children.size,
                        totalDescendantsCount = countTotalDescendants(item.id),
                        hasChildren = hasChildren,
                        isCollapsed = item.isCollapsed,
                        isVisible = true,
                        parentIds = parentChain
                    )
                )
            }

            val childCollapsed = parentCollapsed || item.isCollapsed
            for (child in children) {
                traverse(child, level + 1, parentChain + item.id, childCollapsed)
            }
        }

        if (focusedRootId != null && itemMap.containsKey(focusedRootId)) {
            val rootItem = itemMap[focusedRootId]!!
            // Focused root itself is shown at level 0
            traverse(rootItem, 0, emptyList(), false)
        } else {
            val rootItems = childrenMap[null] ?: emptyList()
            for (rootItem in rootItems) {
                traverse(rootItem, 0, emptyList(), false)
            }
        }

        return result
    }

    /**
     * Retrieves the breadcrumb chain of outline items from the root down to the focused item.
     */
    fun getBreadcrumbs(items: List<OutlineItem>, focusedItemId: String?): List<OutlineItem> {
        if (focusedItemId == null) return emptyList()
        val itemMap = items.associateBy { it.id }
        val breadcrumbs = mutableListOf<OutlineItem>()

        var current: OutlineItem? = itemMap[focusedItemId]
        while (current != null) {
            breadcrumbs.add(0, current)
            current = current.parentId?.let { itemMap[it] }
        }
        return breadcrumbs
    }

    /**
     * Indent an item: Makes it the last child of its immediate preceding sibling.
     */
    fun indentItem(items: List<OutlineItem>, itemId: String): List<OutlineItem>? {
        val itemMap = items.associateBy { it.id }
        val target = itemMap[itemId] ?: return null

        val siblings = items.filter { it.parentId == target.parentId }
            .sortedBy { it.sortOrder }
        val targetIndex = siblings.indexOfFirst { it.id == itemId }
        if (targetIndex <= 0) return null // No preceding sibling to indent into

        val precedingSibling = siblings[targetIndex - 1]
        val existingChildrenOfPreceding = items.filter { it.parentId == precedingSibling.id }
        val newSortOrder = existingChildrenOfPreceding.size

        val updatedItems = items.toMutableList()

        // Update target item
        val targetIndexInAll = updatedItems.indexOfFirst { it.id == itemId }
        updatedItems[targetIndexInAll] = target.copy(
            parentId = precedingSibling.id,
            sortOrder = newSortOrder,
            updatedAt = System.currentTimeMillis()
        )

        // Make sure the preceding sibling is expanded so newly indented child is visible
        val precedingIndexInAll = updatedItems.indexOfFirst { it.id == precedingSibling.id }
        if (precedingIndexInAll != -1 && precedingSibling.isCollapsed) {
            updatedItems[precedingIndexInAll] = precedingSibling.copy(isCollapsed = false)
        }

        // Re-index remaining original siblings
        val remainingSiblings = siblings.filter { it.id != itemId }
        remainingSiblings.forEachIndexed { index, sibling ->
            val idx = updatedItems.indexOfFirst { it.id == sibling.id }
            if (idx != -1) {
                updatedItems[idx] = updatedItems[idx].copy(sortOrder = index)
            }
        }

        return updatedItems
    }

    /**
     * Unindent an item: Moves it out of its current parent to become a sibling of the parent.
     */
    fun unindentItem(items: List<OutlineItem>, itemId: String): List<OutlineItem>? {
        val itemMap = items.associateBy { it.id }
        val target = itemMap[itemId] ?: return null
        val parent = target.parentId?.let { itemMap[it] } ?: return null // Already at root

        val grandparent = parent.parentId
        val parentSiblings = items.filter { it.parentId == grandparent }
            .sortedBy { it.sortOrder }

        val newParentId = grandparent
        val insertSortOrder = parent.sortOrder + 1

        val updatedItems = items.toMutableList()

        // Shift siblings of parent that come after parent by +1
        parentSiblings.filter { it.sortOrder >= insertSortOrder }.forEach { sibling ->
            val idx = updatedItems.indexOfFirst { it.id == sibling.id }
            if (idx != -1) {
                updatedItems[idx] = updatedItems[idx].copy(sortOrder = sibling.sortOrder + 1)
            }
        }

        // Update target item
        val targetIndexInAll = updatedItems.indexOfFirst { it.id == itemId }
        updatedItems[targetIndexInAll] = target.copy(
            parentId = newParentId,
            sortOrder = insertSortOrder,
            updatedAt = System.currentTimeMillis()
        )

        // Re-index former siblings of target
        val formerSiblings = items.filter { it.parentId == parent.id && it.id != itemId }
            .sortedBy { it.sortOrder }
        formerSiblings.forEachIndexed { index, sibling ->
            val idx = updatedItems.indexOfFirst { it.id == sibling.id }
            if (idx != -1) {
                updatedItems[idx] = updatedItems[idx].copy(sortOrder = index)
            }
        }

        return updatedItems
    }

    /**
     * Move an item UP within its siblings.
     */
    fun moveItemUp(items: List<OutlineItem>, itemId: String): List<OutlineItem>? {
        val itemMap = items.associateBy { it.id }
        val target = itemMap[itemId] ?: return null

        val siblings = items.filter { it.parentId == target.parentId }
            .sortedBy { it.sortOrder }
        val index = siblings.indexOfFirst { it.id == itemId }
        if (index <= 0) return null // Already topmost sibling

        val previousSibling = siblings[index - 1]
        val updatedItems = items.toMutableList()

        val targetIdx = updatedItems.indexOfFirst { it.id == target.id }
        val prevIdx = updatedItems.indexOfFirst { it.id == previousSibling.id }

        updatedItems[targetIdx] = target.copy(sortOrder = previousSibling.sortOrder, updatedAt = System.currentTimeMillis())
        updatedItems[prevIdx] = previousSibling.copy(sortOrder = target.sortOrder, updatedAt = System.currentTimeMillis())

        return updatedItems
    }

    /**
     * Move an item DOWN within its siblings.
     */
    fun moveItemDown(items: List<OutlineItem>, itemId: String): List<OutlineItem>? {
        val itemMap = items.associateBy { it.id }
        val target = itemMap[itemId] ?: return null

        val siblings = items.filter { it.parentId == target.parentId }
            .sortedBy { it.sortOrder }
        val index = siblings.indexOfFirst { it.id == itemId }
        if (index < 0 || index >= siblings.size - 1) return null // Already bottommost sibling

        val nextSibling = siblings[index + 1]
        val updatedItems = items.toMutableList()

        val targetIdx = updatedItems.indexOfFirst { it.id == target.id }
        val nextIdx = updatedItems.indexOfFirst { it.id == nextSibling.id }

        updatedItems[targetIdx] = target.copy(sortOrder = nextSibling.sortOrder, updatedAt = System.currentTimeMillis())
        updatedItems[nextIdx] = nextSibling.copy(sortOrder = target.sortOrder, updatedAt = System.currentTimeMillis())

        return updatedItems
    }

    /**
     * Collects all descendant items recursively for a given item ID.
     */
    fun getDescendantIds(items: List<OutlineItem>, itemId: String): Set<String> {
        val childrenMap = items.groupBy { it.parentId }
        val result = mutableSetOf<String>()

        fun collect(id: String) {
            val children = childrenMap[id] ?: return
            for (child in children) {
                result.add(child.id)
                collect(child.id)
            }
        }

        collect(itemId)
        return result
    }

    /**
     * Delete an item and all its descendants.
     */
    fun deleteSubtree(items: List<OutlineItem>, itemId: String): List<OutlineItem> {
        val target = items.find { it.id == itemId } ?: return items
        val toDelete = getDescendantIds(items, itemId) + itemId

        val remaining = items.filter { it.id !in toDelete }.toMutableList()

        // Re-index siblings of target
        val siblings = remaining.filter { it.parentId == target.parentId }
            .sortedBy { it.sortOrder }
        siblings.forEachIndexed { index, sibling ->
            val idx = remaining.indexOfFirst { it.id == sibling.id }
            if (idx != -1) {
                remaining[idx] = remaining[idx].copy(sortOrder = index)
            }
        }

        return remaining
    }

    /**
     * Duplicate an item and its entire subtree, inserting it immediately after the original item.
     */
    fun duplicateSubtree(items: List<OutlineItem>, itemId: String): List<OutlineItem> {
        val target = items.find { it.id == itemId } ?: return items
        val itemMap = items.associateBy { it.id }
        val childrenMap = items.groupBy { it.parentId }

        val idMapping = mutableMapOf<String, String>()
        val newItems = mutableListOf<OutlineItem>()

        fun duplicateRecursive(originalItem: OutlineItem, newParentId: String?, sortOrder: Int): OutlineItem {
            val newId = UUID.randomUUID().toString()
            idMapping[originalItem.id] = newId
            val duplicated = originalItem.copy(
                id = newId,
                parentId = newParentId,
                sortOrder = sortOrder,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            newItems.add(duplicated)

            val children = childrenMap[originalItem.id]?.sortedBy { it.sortOrder } ?: emptyList()
            children.forEachIndexed { index, child ->
                duplicateRecursive(child, newId, index)
            }
            return duplicated
        }

        val updatedItems = items.toMutableList()

        // Shift siblings that come after original target
        val siblings = items.filter { it.parentId == target.parentId }.sortedBy { it.sortOrder }
        val targetSiblingIdx = siblings.indexOfFirst { it.id == itemId }
        val insertOrder = target.sortOrder + 1

        siblings.filter { it.sortOrder >= insertOrder }.forEach { sibling ->
            val idx = updatedItems.indexOfFirst { it.id == sibling.id }
            if (idx != -1) {
                updatedItems[idx] = updatedItems[idx].copy(sortOrder = sibling.sortOrder + 1)
            }
        }

        duplicateRecursive(target, target.parentId, insertOrder)
        updatedItems.addAll(newItems)

        return updatedItems
    }

    /**
     * Adds a new sibling immediately after the given target item.
     */
    fun addSiblingAfter(items: List<OutlineItem>, afterItemId: String?, noteId: String): Pair<List<OutlineItem>, OutlineItem> {
        val newItemId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        if (afterItemId == null) {
            // Add as first item in note
            val roots = items.filter { it.parentId == null }.sortedBy { it.sortOrder }
            val updatedItems = items.map {
                if (it.parentId == null) it.copy(sortOrder = it.sortOrder + 1) else it
            }.toMutableList()

            val newItem = OutlineItem(
                id = newItemId,
                noteId = noteId,
                parentId = null,
                sortOrder = 0,
                text = "",
                createdAt = now,
                updatedAt = now
            )
            updatedItems.add(newItem)
            return Pair(updatedItems, newItem)
        }

        val target = items.find { it.id == afterItemId }
        val parentId = target?.parentId
        val insertOrder = (target?.sortOrder ?: -1) + 1

        val updatedItems = items.map {
            if (it.parentId == parentId && it.sortOrder >= insertOrder) {
                it.copy(sortOrder = it.sortOrder + 1)
            } else {
                it
            }
        }.toMutableList()

        val newItem = OutlineItem(
            id = newItemId,
            noteId = noteId,
            parentId = parentId,
            sortOrder = insertOrder,
            text = "",
            createdAt = now,
            updatedAt = now
        )
        updatedItems.add(newItem)
        return Pair(updatedItems, newItem)
    }

    /**
     * Adds a new child to a parent item.
     */
    fun addChildItem(items: List<OutlineItem>, parentItemId: String, noteId: String): Pair<List<OutlineItem>, OutlineItem> {
        val newItemId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val existingChildren = items.filter { it.parentId == parentItemId }
        val newSortOrder = existingChildren.size

        val updatedItems = items.map {
            if (it.id == parentItemId && it.isCollapsed) {
                it.copy(isCollapsed = false)
            } else {
                it
            }
        }.toMutableList()

        val newItem = OutlineItem(
            id = newItemId,
            noteId = noteId,
            parentId = parentItemId,
            sortOrder = newSortOrder,
            text = "",
            createdAt = now,
            updatedAt = now
        )
        updatedItems.add(newItem)
        return Pair(updatedItems, newItem)
    }
}
