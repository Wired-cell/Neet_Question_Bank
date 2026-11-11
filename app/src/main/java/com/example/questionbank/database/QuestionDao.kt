package com.example.questionbank.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.questionbank.model.Question
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: Question): Long

    @Update
    suspend fun update(question: Question)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM questions WHERE vaultId = :vaultId ORDER BY createdAt DESC")
    fun getQuestionsByVault(vaultId: Long): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE id = :id")
    fun getQuestionById(id: Long): Flow<Question?>

    @Query("SELECT * FROM questions ORDER BY createdAt DESC")
    fun getAllQuestions(): Flow<List<Question>>

    @Query("SELECT * FROM questions")
    suspend fun getAllQuestionsOnce(): List<Question>

    @Query("UPDATE questions SET imageUri = NULL WHERE imageUri IS NOT NULL")
    suspend fun clearOriginalImageUris()

    @Query("SELECT * FROM questions WHERE vaultId IN (SELECT id FROM vaults WHERE parentId = :parentId) ORDER BY createdAt DESC")
    fun getQuestionsByParentVault(parentId: Long): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteQuestions(): Flow<List<Question>>

    @Query("SELECT COUNT(*) FROM questions WHERE vaultId = :vaultId")
    suspend fun getQuestionCount(vaultId: Long): Int

    @Query("SELECT * FROM questions WHERE questionText LIKE '%' || :query || '%' OR options LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchQuestions(query: String): Flow<List<Question>>
}
