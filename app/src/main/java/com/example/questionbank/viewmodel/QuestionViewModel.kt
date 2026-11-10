package com.example.questionbank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questionbank.database.QuestionDao
import com.example.questionbank.model.Question
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuestionViewModel(private val questionDao: QuestionDao) : ViewModel() {
    
    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()
    
    private val _selectedQuestion = MutableStateFlow<Question?>(null)
    val selectedQuestion: StateFlow<Question?> = _selectedQuestion.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    fun getQuestionsByVault(vaultId: Long): Flow<List<Question>> = questionDao.getQuestionsByVault(vaultId)
    
    fun getAllQuestions(): Flow<List<Question>> = questionDao.getAllQuestions()
    
    fun searchQuestions(query: String): Flow<List<Question>> = questionDao.searchQuestions(query)
    
    fun selectQuestion(question: Question) {
        _selectedQuestion.value = question
    }
    
    suspend fun addQuestion(question: Question): Long {
        return questionDao.insert(question)
    }
    
    suspend fun updateQuestion(question: Question) {
        questionDao.update(question)
    }
    
    suspend fun deleteQuestion(question: Question) {
        questionDao.delete(question.id)
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun getQuestionsByVaultFlow(vaultId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            questionDao.getQuestionsByVault(vaultId).collect { questions ->
                _questions.value = questions
                _isLoading.value = false
            }
        }
    }
    
    suspend fun toggleFavorite(question: Question) {
        questionDao.update(question.copy(isFavorite = !question.isFavorite))
    }
}
