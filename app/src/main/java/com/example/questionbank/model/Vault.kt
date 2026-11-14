package com.example.questionbank.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vaults")
data class Vault(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val description: String = "",
    val color: Int = 0xFF6200EE.toInt(),
    val icon: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isRoot: Boolean get() = parentId == null
    val isSubject: Boolean get() = parentId == null
    val isSubSubject: Boolean get() = parentId != null
}

// Default vault structure
val defaultVaults = listOf(
    Vault(name = "Physics", parentId = null, color = 0xFF2196F3.toInt(), icon = "📚"),
    Vault(name = "Chemistry", parentId = null, color = 0xFF4CAF50.toInt(), icon = "🧪"),
    Vault(name = "Physical Chemistry", parentId = 2, color = 0xFF9C27B0.toInt()),
    Vault(name = "Inorganic Chemistry", parentId = 2, color = 0xFFFF9800.toInt()),
    Vault(name = "Organic Chemistry", parentId = 2, color = 0xFFF44336.toInt()),
    Vault(name = "Biology", parentId = null, color = 0xFF8BC34A.toInt(), icon = "🌱"),
    Vault(name = "Botany", parentId = 6, color = 0xFF4CAF50.toInt()),
    Vault(name = "Zoology", parentId = 6, color = 0xFFCDDC39.toInt())
)
