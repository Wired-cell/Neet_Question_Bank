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
    version = 2,
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

        @Volatile
        private var INSTANCE: QuestionDatabase? = null

        fun getDatabase(context: Context): QuestionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuestionDatabase::class.java,
                    "question_bank_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
