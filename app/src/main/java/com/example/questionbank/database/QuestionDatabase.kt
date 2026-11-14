package com.example.questionbank.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.example.questionbank.model.Question
import com.example.questionbank.model.Vault

@Database(
    entities = [Vault::class, Question::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(com.example.questionbank.model.Converters::class)
abstract class QuestionDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun questionDao(): QuestionDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_questions_vaultId ON questions(vaultId)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE vaults SET name = 'Physical Chemistry' WHERE name = 'Physical'")
                db.execSQL("UPDATE vaults SET name = 'Inorganic Chemistry' WHERE name = 'Inorganic'")
                db.execSQL("UPDATE vaults SET name = 'Organic Chemistry' WHERE name = 'Organic'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE questions ADD COLUMN solutionImageUri TEXT")
                db.execSQL("UPDATE vaults SET icon = '⚛️' WHERE name = 'Physics'")
                db.execSQL("UPDATE vaults SET icon = '🧪' WHERE name = 'Chemistry'")
                db.execSQL("UPDATE vaults SET icon = '⚗️' WHERE name = 'Physical Chemistry'")
                db.execSQL("UPDATE vaults SET icon = '🧬' WHERE name = 'Inorganic Chemistry'")
                db.execSQL("UPDATE vaults SET icon = '🧫' WHERE name = 'Organic Chemistry'")
                db.execSQL("UPDATE vaults SET icon = '🌿' WHERE name = 'Biology'")
                db.execSQL("UPDATE vaults SET icon = '🌱' WHERE name = 'Botany'")
                db.execSQL("UPDATE vaults SET icon = '🐾' WHERE name = 'Zoology'")
            }
        }

        @Volatile
        private var INSTANCE: QuestionDatabase? = null

        fun getDatabase(context: Context): QuestionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuestionDatabase::class.java,
                    "question_bank_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
