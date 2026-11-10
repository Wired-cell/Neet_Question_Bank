package com.example.questionbank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questionbank.database.VaultDao
import com.example.questionbank.model.Vault
import com.example.questionbank.model.defaultVaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class VaultViewModel(private val vaultDao: VaultDao) : ViewModel() {
    
    private val _vaults = MutableStateFlow<List<Vault>>(emptyList())
    val vaults: StateFlow<List<Vault>> = _vaults.asStateFlow()
    
    private val _selectedVault = MutableStateFlow<Vault?>(null)
    val selectedVault: StateFlow<Vault?> = _selectedVault.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _expandedVaults = MutableStateFlow<Set<Long>>(emptySet())
    val expandedVaults: StateFlow<Set<Long>> = _expandedVaults.asStateFlow()
    
    init {
        loadVaults()
    }
    
    private fun loadVaults() {
        viewModelScope.launch {
            _isLoading.value = true
            vaultDao.getAllVaults().collect { vaults ->
                if (vaults.isEmpty()) {
                    // Initialize default vaults once; the Room Flow emits the inserted rows.
                    defaultVaults.forEach { vault -> vaultDao.insert(vault) }
                } else {
                    _vaults.value = vaults
                    _isLoading.value = false
                }
            }
        }
    }

    fun getRootVaults(): Flow<List<Vault>> = vaultDao.getAllRootVaults()
    
    fun getSubVaults(parentId: Long): Flow<List<Vault>> = vaultDao.getSubVaults(parentId)
    
    fun selectVault(vault: Vault) {
        _selectedVault.value = vault
    }
    
    fun toggleExpand(vaultId: Long) {
        _expandedVaults.value = if (_expandedVaults.value.contains(vaultId)) {
            _expandedVaults.value - vaultId
        } else {
            _expandedVaults.value + vaultId
        }
    }
    
    fun isExpanded(vaultId: Long): Boolean = _expandedVaults.value.contains(vaultId)
    
    suspend fun addVault(vault: Vault): Long {
        return vaultDao.insert(vault)
    }
    
    suspend fun updateVault(vault: Vault) {
        vaultDao.update(vault)
        loadVaults()
    }
    
    suspend fun deleteVault(vault: Vault) {
        vaultDao.delete(vault.id)
        loadVaults()
    }
    
    fun getVaultHierarchy(): List<VaultNode> {
        val rootVaults = _vaults.value.filter { it.isRoot }
        return rootVaults.map { root ->
            val subVaults = _vaults.value.filter { it.parentId == root.id }
            VaultNode(root, subVaults.map { VaultNode(it, emptyList()) })
        }
    }
    
    data class VaultNode(
        val vault: Vault,
        val children: List<VaultNode>
    )
}
