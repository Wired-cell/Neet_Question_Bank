package com.example.questionbank.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.questionbank.model.Question
import com.example.questionbank.model.Vault

@Database(
    entities = [Vault::class, Question::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(com.example.questionbank.model.Converters::class)
abstract class QuestionDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun questionDao(): QuestionDao

    companion object {
        @Volatile
        private var INSTANCE: QuestionDatabase? = null

        fun getDatabase(context: Context): QuestionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuestionDatabase::class.java,
                    "question_bank_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
