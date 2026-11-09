package com.example.questionbank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.questionbank.database.QuestionDao
import com.example.questionbank.database.VaultDao

class AppViewModelFactory(
    private val vaultDao: VaultDao,
    private val questionDao: QuestionDao
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(VaultViewModel::class.java) -> {
                VaultViewModel(vaultDao) as T
            }
            modelClass.isAssignableFrom(QuestionViewModel::class.java) -> {
                QuestionViewModel(questionDao) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
