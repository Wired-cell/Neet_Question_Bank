package com.example.questionbank.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.questionbank.model.Vault
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vault: Vault): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vaults: List<Vault>)

    @Update
    suspend fun update(vault: Vault)

    @Query("DELETE FROM vaults WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM vaults WHERE parentId IS NULL ORDER BY name ASC")
    fun getAllRootVaults(): Flow<List<Vault>>

    @Query("SELECT * FROM vaults WHERE parentId = :parentId ORDER BY name ASC")
    fun getSubVaults(parentId: Long): Flow<List<Vault>>

    @Query("SELECT * FROM vaults WHERE id = :id")
    fun getVaultById(id: Long): Flow<Vault?>

    @Query("SELECT * FROM vaults ORDER BY name ASC")
    fun getAllVaults(): Flow<List<Vault>>

    @Query("SELECT COUNT(*) FROM vaults WHERE parentId = :parentId")
    suspend fun getSubVaultCount(parentId: Long): Int

    @Query("SELECT * FROM vaults WHERE name = :name AND parentId = :parentId")
    suspend fun getVaultByNameAndParent(name: String, parentId: Long?): Vault?
}
