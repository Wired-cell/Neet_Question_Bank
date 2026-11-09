package com.example.questionbank.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = Vault::class,
            parentColumns = ["id"],
            childColumns = ["vaultId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(Converters::class)
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vaultId: Long,
    val questionText: String,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int? = null,
    val explanation: String = "",
    val imageUri: String? = null,
    val diagramUri: String? = null,
    val isFavorite: Boolean = false,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class Difficulty {
    EASY, MEDIUM, HARD
}

class Converters {
    @androidx.room.TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(",")

    @androidx.room.TypeConverter
    fun toStringList(data: String): List<String> = if (data.isEmpty()) emptyList() else data.split(",")

    @androidx.room.TypeConverter
    fun fromDifficulty(difficulty: Difficulty): String = difficulty.name

    @androidx.room.TypeConverter
    fun toDifficulty(data: String): Difficulty = Difficulty.valueOf(data)
}
