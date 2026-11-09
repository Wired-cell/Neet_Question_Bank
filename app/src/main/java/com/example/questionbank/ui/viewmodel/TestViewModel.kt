package com.example.questionbank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questionbank.database.QuestionDao
import com.example.questionbank.database.VaultDao
import com.example.questionbank.model.Question
import com.example.questionbank.model.Vault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.random.Random

class TestViewModel(
    private val vaultDao: VaultDao,
    private val questionDao: QuestionDao
) : ViewModel() {
    
    private val _vaults = MutableStateFlow<List<Vault>>(emptyList())
    val vaults: StateFlow<List<Vault>> = _vaults.asStateFlow()
    
    private val _selectedVaults = MutableStateFlow<Set<Long>>(emptySet())
    val selectedVaults: StateFlow<Set<Long>> = _selectedVaults.asStateFlow()
    
    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()
    
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()
    
    private val _currentQuestion = MutableStateFlow<Question?>(null)
    val currentQuestion: StateFlow<Question?> = _currentQuestion.asStateFlow()
    
    private val _selectedOption = MutableStateFlow<Int?>(null)
    val selectedOption: StateFlow<Int?> = _selectedOption.asStateFlow()
    
    private val _isAnswered = MutableStateFlow(false)
    val isAnswered: StateFlow<Boolean> = _isAnswered.asStateFlow()
    
    private val _isCorrect = MutableStateFlow<Boolean?>(null)
    val isCorrect: StateFlow<Boolean?> = _isCorrect.asStateFlow()
    
    private val _testStarted = MutableStateFlow(false)
    val testStarted: StateFlow<Boolean> = _testStarted.asStateFlow()
    
    private val _testCompleted = MutableStateFlow(false)
    val testCompleted: StateFlow<Boolean> = _testCompleted.asStateFlow()
    
    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()
    
    private val _totalQuestions = MutableStateFlow(0)
    val totalQuestions: StateFlow<Int> = _totalQuestions.asStateFlow()
    
    private val _timeRemaining = MutableStateFlow(180 * 60 * 1000L) // 180 minutes in milliseconds
    val timeRemaining: StateFlow<Long> = _timeRemaining.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning.asStateFlow()
    
    private val _timerFinished = MutableStateFlow(false)
    val timerFinished: StateFlow<Boolean> = _timerFinished.asStateFlow()
    
    // Test configuration
    private var questionsPerVault = 45
    private var testDuration: Long = 180 * 60 * 1000L // 180 minutes in milliseconds
    
    private var timerJob: kotlinx.coroutines.Job? = null
    
    init {
        loadVaults()
    }
    
    private fun loadVaults() {
        viewModelScope.launch {
            vaultDao.getAllVaults().collect { vaults ->
                _vaults.value = vaults
            }
        }
    }
    
    /**
     * Toggle vault selection
     */
    fun toggleVault(vaultId: Long) {
        _selectedVaults.value = if (_selectedVaults.value.contains(vaultId)) {
            _selectedVaults.value - vaultId
        } else {
            _selectedVaults.value + vaultId
        }
    }
    
    /**
     * Get sub-vaults (Botany, Zoology, Physical, Inorganic, Organic)
     */
    fun getSubVaults(): List<Vault> {
        return _vaults.value.filter { it.parentId != null }
    }
    
    /**
     * Get all leaf vaults (sub-vaults only)
     */
    fun getLeafVaults(): List<Vault> {
        return _vaults.value.filter { it.isSubSubject }
    }
    
    /**
     * Start test with selected vaults
     */
    fun startTest() {
        viewModelScope.launch {
            _isLoading.value = true
            _testStarted.value = true
            _testCompleted.value = false
            _timerFinished.value = false
            _score.value = 0
            _currentQuestionIndex.value = 0
            _selectedOption.value = null
            _isAnswered.value = false
            _isCorrect.value = null
            _timeRemaining.value = testDuration
            
            // Start timer
            startTimer()
            
            // Get all questions from selected vaults
            val allQuestions = mutableListOf<Question>()
            
            if (_selectedVaults.value.isEmpty()) {
                // If no vaults selected, use all leaf vaults
                getLeafVaults().forEach { vault ->
                    questionDao.getQuestionsByVault(vault.id).collect { questions ->
                        allQuestions.addAll(questions)
                    }
                }
            } else {
                _selectedVaults.value.forEach { vaultId ->
                    questionDao.getQuestionsByVault(vaultId).collect { questions ->
                        allQuestions.addAll(questions)
                    }
                }
            }
            
            // Wait for all questions to load
            // This is a simplified approach - in production, use proper async handling
            _questions.value = allQuestions.distinctBy { it.id }
            _totalQuestions.value = _questions.value.size
            
            // Select questions: 45 from each vault
            val selectedQuestions = selectQuestionsByVault(allQuestions)
            _questions.value = selectedQuestions
            _totalQuestions.value = selectedQuestions.size
            
            if (_questions.value.isNotEmpty()) {
                _currentQuestion.value = _questions.value[0]
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Start the countdown timer
     */
    private fun startTimer() {
        timerJob?.cancel()
        _timerRunning.value = true
        _timeRemaining.value = testDuration
        
        timerJob = viewModelScope.launch {
            while (_timeRemaining.value > 0 && !_testCompleted.value) {
                delay(1000L) // Update every second
                _timeRemaining.value -= 1000L
                
                // Check if timer finished
                if (_timeRemaining.value <= 0) {
                    _timerFinished.value = true
                    _testCompleted.value = true
                }
            }
            _timerRunning.value = false
        }
    }
    
    /**
     * Pause the timer
     */
    fun pauseTimer() {
        timerJob?.cancel()
        _timerRunning.value = false
    }
    
    /**
     * Resume the timer
     */
    fun resumeTimer() {
        if (_timeRemaining.value > 0 && !_timerRunning.value) {
            startTimer()
        }
    }
    
    /**
     * Stop the timer
     */
    fun stopTimer() {
        timerJob?.cancel()
        _timerRunning.value = false
    }
    
    /**
     * Reset the timer
     */
    fun resetTimer() {
        _timeRemaining.value = testDuration
        _timerRunning.value = false
        _timerFinished.value = false
    }
    
    /**
     * Get formatted time remaining (MM:SS)
     */
    fun getFormattedTime(): String {
        val totalSeconds = _timeRemaining.value / 1000L
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Get formatted time remaining (HH:MM:SS)
     */
    fun getFormattedTimeLong(): String {
        val totalSeconds = _timeRemaining.value / 1000L
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    
    /**
     * Select 45 questions from each sub-vault
     */
    private fun selectQuestionsByVault(allQuestions: List<Question>): List<Question> {
        // Group questions by vault
        val questionsByVault = allQuestions.groupBy { it.vaultId }
        
        val selectedQuestions = mutableListOf<Question>()
        
        // For each vault, select up to 45 questions
        questionsByVault.forEach { (vaultId, questions) ->
            val shuffled = questions.shuffled(Random(System.currentTimeMillis()))
            val selected = shuffled.take(45)
            selectedQuestions.addAll(selected)
        }
        
        return selectedQuestions.shuffled(Random(System.currentTimeMillis() + 1))
    }
    
    /**
     * Select specific number of questions from each vault
     */
    fun setQuestionsPerVault(count: Int) {
        questionsPerVault = count
    }
    
    /**
     * Move to next question
     */
    fun nextQuestion() {
        if (_currentQuestionIndex.value < _questions.value.size - 1) {
            _currentQuestionIndex.value++
            _currentQuestion.value = _questions.value[_currentQuestionIndex.value]
            _selectedOption.value = null
            _isAnswered.value = false
            _isCorrect.value = null
        } else {
            _testCompleted.value = true
        }
    }
    
    /**
     * Move to previous question
     */
    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value--
            _currentQuestion.value = _questions.value[_currentQuestionIndex.value]
            _selectedOption.value = null
            _isAnswered.value = false
            _isCorrect.value = null
        }
    }
    
    /**
     * Select an option
     */
    fun selectOption(index: Int) {
        if (_isAnswered.value) return
        
        _selectedOption.value = index
        
        // Check if correct
        val question = _currentQuestion.value
        val isCorrect = question?.correctOptionIndex == index
        _isCorrect.value = isCorrect
        _isAnswered.value = true
        
        if (isCorrect) {
            _score.value = _score.value + 1
        }
    }
    
    /**
     * Skip question
     */
    fun skipQuestion() {
        _selectedOption.value = null
        _isAnswered.value = true
        _isCorrect.value = false
    }
    
    /**
     * Reset test
     */
    fun resetTest() {
        stopTimer()
        resetTimer()
        _testStarted.value = false
        _testCompleted.value = false
        _timerFinished.value = false
        _score.value = 0
        _currentQuestionIndex.value = 0
        _questions.value = emptyList()
        _currentQuestion.value = null
        _selectedOption.value = null
        _isAnswered.value = false
        _isCorrect.value = null
    }
    
    /**
     * Get progress percentage
     */
    fun getProgress(): Float {
        if (_totalQuestions.value == 0) return 0f
        return (_currentQuestionIndex.value + 1).toFloat() / _totalQuestions.value * 100
    }
    
    /**
     * Get current question number
     */
    fun getCurrentQuestionNumber(): String {
        return "${_currentQuestionIndex.value + 1}/${_totalQuestions.value}"
    }
    
    /**
     * Get score percentage
     */
    fun getScorePercentage(): Float {
        if (_currentQuestionIndex.value == 0) return 0f
        return _score.value.toFloat() / _currentQuestionIndex.value * 100
    }
    
    /**
     * Select all leaf vaults (Botany, Zoology, Physical, Inorganic, Organic, Physics)
     */
    fun selectAllLeafVaults() {
        val leafVaults = getLeafVaults()
        _selectedVaults.value = leafVaults.map { it.id }.toSet()
    }
    
    /**
     * Deselect all vaults
     */
    fun deselectAllVaults() {
        _selectedVaults.value = emptySet()
    }
}
