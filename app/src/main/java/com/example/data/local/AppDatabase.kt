package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AiChatDao
import com.example.data.local.dao.FolderDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.OutlineItemDao
import com.example.data.local.dao.TagDao
import com.example.data.local.entity.AIConversationEntity
import com.example.data.local.entity.AIMessageEntity
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.ItemTagCrossRef
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.OutlineItemEntity
import com.example.data.local.entity.TagEntity

@Database(
    entities = [
        FolderEntity::class,
        NoteEntity::class,
        OutlineItemEntity::class,
        TagEntity::class,
        ItemTagCrossRef::class,
        AIConversationEntity::class,
        AIMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun noteDao(): NoteDao
    abstract fun outlineItemDao(): OutlineItemDao
    abstract fun tagDao(): TagDao
    abstract fun aiChatDao(): AiChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "outliner_database.db"
                ).fallbackToDestructiveMigration(true)
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
