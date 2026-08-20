package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.BuildConfig
import com.example.data.local.dao.AiBackendDao
import com.example.data.local.dao.AiChatDao
import com.example.data.local.dao.FolderDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.OutlineItemDao
import com.example.data.local.dao.TagDao
import com.example.data.local.entity.AIConversationEntity
import com.example.data.local.entity.AIMessageEntity
import com.example.data.local.entity.AiBackendEntity
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
        AIMessageEntity::class,
        AiBackendEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun noteDao(): NoteDao
    abstract fun outlineItemDao(): OutlineItemDao
    abstract fun tagDao(): TagDao
    abstract fun aiChatDao(): AiChatDao
    abstract fun aiBackendDao(): AiBackendDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create ai_backends table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_backends` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `apiKey` TEXT NOT NULL,
                        `models` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // Recreate ai_messages table without sourcesJson and searchQueriesJson
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_messages_new` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `conversationId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`conversationId`) REFERENCES `ai_conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_messages_conversationId` ON `ai_messages_new` (`conversationId`)")
                db.execSQL(
                    """
                    INSERT INTO `ai_messages_new` (`id`, `conversationId`, `role`, `content`, `createdAt`)
                    SELECT `id`, `conversationId`, `role`, `content`, `createdAt` FROM `ai_messages`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `ai_messages`")
                db.execSQL("ALTER TABLE `ai_messages_new` RENAME TO `ai_messages`")

                // Insert default Gemini backend
                val defaultKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `ai_backends` (`id`, `name`, `url`, `apiKey`, `models`, `isDefault`, `createdAt`, `updatedAt`)
                    VALUES ('default-gemini', 'Gemini', 'https://generativelanguage.googleapis.com/v1beta/openai/', '$defaultKey', 'gemini-3.5-flash-lite', 1, $now, $now)
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "outliner_database.db"
                ).addMigrations(MIGRATION_2_3)
                 .fallbackToDestructiveMigration(true)
                 .addCallback(object : RoomDatabase.Callback() {
                     override fun onCreate(db: SupportSQLiteDatabase) {
                         super.onCreate(db)
                         val defaultKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
                         val now = System.currentTimeMillis()
                         db.execSQL(
                             """
                             INSERT OR IGNORE INTO `ai_backends` (`id`, `name`, `url`, `apiKey`, `models`, `isDefault`, `createdAt`, `updatedAt`)
                             VALUES ('default-gemini', 'Gemini', 'https://generativelanguage.googleapis.com/v1beta/openai/', '$defaultKey', 'gemini-3.5-flash-lite', 1, $now, $now)
                             """.trimIndent()
                         )
                     }
                 })
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
