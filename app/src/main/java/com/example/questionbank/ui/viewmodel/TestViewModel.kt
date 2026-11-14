package com.example.questionbank.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questionbank.database.QuestionDao
import com.example.questionbank.database.VaultDao
import com.example.questionbank.model.Question
import com.example.questionbank.model.Vault
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val _timeRemaining = MutableStateFlow(180 * 60 * 1000L)
    val timeRemaining: StateFlow<Long> = _timeRemaining.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning.asStateFlow()
    private val _timerFinished = MutableStateFlow(false)
    val timerFinished: StateFlow<Boolean> = _timerFinished.asStateFlow()

    private val _chemistryWeights = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val chemistryWeights: StateFlow<Map<Long, Int>> = _chemistryWeights.asStateFlow()
    private val _correctQuestions = MutableStateFlow<List<Question>>(emptyList())
    val correctQuestions: StateFlow<List<Question>> = _correctQuestions.asStateFlow()
    private val _incorrectQuestions = MutableStateFlow<List<Question>>(emptyList())
    val incorrectQuestions: StateFlow<List<Question>> = _incorrectQuestions.asStateFlow()
    private val _unattendedQuestions = MutableStateFlow<List<Question>>(emptyList())
    val unattendedQuestions: StateFlow<List<Question>> = _unattendedQuestions.asStateFlow()

    private val answers = mutableMapOf<Long, Int>()
    private val testDuration = 180 * 60 * 1000L
    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            vaultDao.getAllVaults().collect { all ->
                _vaults.value = all
                val chemistry = all.filter { child -> all.firstOrNull { it.id == child.parentId }?.name.equals("Chemistry", true) }
                if (_chemistryWeights.value.isEmpty() && chemistry.isNotEmpty()) {
                    _chemistryWeights.value = chemistry.associate { v ->
                        v.id to when {
                            v.name.startsWith("Physical", true) -> 34
                            v.name.startsWith("Inorganic", true) -> 33
                            else -> 33
                        }
                    }
                }
            }
        }
    }

    fun getLeafVaults(): List<Vault> = _vaults.value.filter { it.parentId != null }
    fun getChemistryVaults(): List<Vault> = _vaults.value.filter { v ->
        _vaults.value.firstOrNull { it.id == v.parentId }?.name.equals("Chemistry", true)
    }
    fun getSelectableVaults(): List<Vault> = (_vaults.value.filter {
        it.parentId != null || it.name.equals("Physics", true)
    }).sortedWith(compareBy<Vault> { if (it.name.equals("Physics", true)) 0 else 1 }.thenBy { it.name })

    fun toggleVault(vaultId: Long) {
        _selectedVaults.value = if (vaultId in _selectedVaults.value) _selectedVaults.value - vaultId
        else _selectedVaults.value + vaultId
    }

    fun setChemistryWeight(vaultId: Long, weight: Int) {
        _chemistryWeights.value = _chemistryWeights.value.toMutableMap().apply {
            this[vaultId] = weight.coerceIn(0, 100)
        }
    }

    fun startTest() {
        viewModelScope.launch {
            _isLoading.value = true
            _testStarted.value = true
            _testCompleted.value = false
            _timerFinished.value = false
            _currentQuestionIndex.value = 0
            _score.value = 0
            answers.clear()
            _correctQuestions.value = emptyList()
            _incorrectQuestions.value = emptyList()
            _unattendedQuestions.value = emptyList()
            _timeRemaining.value = testDuration

            val selected = getSelectableVaults().filter { it.id in _selectedVaults.value }
            val allByVault = selected.associateWith { questionDao.getQuestionsByVault(it.id).first() }
            val chemistrySelected = selected.filter { it in getChemistryVaults() }
            val otherSelected = selected.filterNot { it in chemistrySelected }
            val picked = mutableListOf<Question>()

            otherSelected.forEach { vault ->
                picked += allByVault[vault].orEmpty().shuffled().take(45)
            }
            if (chemistrySelected.isNotEmpty()) {
                val weights = chemistrySelected.associate { it.id to (_chemistryWeights.value[it.id] ?: 33).coerceAtLeast(0) }
                val totalWeight = weights.values.sum().takeIf { it > 0 } ?: chemistrySelected.size
                val exact = chemistrySelected.associate { v ->
                    v.id to 45.0 * ((weights[v.id] ?: 1).takeIf { totalWeight > 0 } ?: 1) / totalWeight
                }
                val counts = exact.mapValues { it.value.toInt() }.toMutableMap()
                var remaining = 45 - counts.values.sum()
                exact.entries.sortedByDescending { it.value - it.value.toInt() }.forEach { entry ->
                    if (remaining > 0) { counts[entry.key] = (counts[entry.key] ?: 0) + 1; remaining-- }
                }
                chemistrySelected.forEach { vault ->
                    picked += allByVault[vault].orEmpty().shuffled().take(counts[vault.id] ?: 0)
                }
            }
            _questions.value = picked.shuffled()
            _totalQuestions.value = picked.size
            _currentQuestion.value = _questions.value.firstOrNull()
            syncCurrentAnswer()
            _isLoading.value = false
            if (_questions.value.isEmpty()) {
                _testStarted.value = false
                return@launch
            }
            startTimer()
        }
    }

    private fun syncCurrentAnswer() {
        val q = _currentQuestion.value
        _selectedOption.value = q?.let { answers[it.id] }
        _isAnswered.value = _selectedOption.value != null
        _isCorrect.value = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        _timerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_timeRemaining.value > 0 && !_testCompleted.value) {
                delay(1000)
                _timeRemaining.value -= 1000
            }
            if (_timeRemaining.value <= 0) {
                _timerFinished.value = true
                finishTest()
            }
            _timerRunning.value = false
        }
    }

    fun pauseTimer() { timerJob?.cancel(); _timerRunning.value = false }
    fun resumeTimer() { if (_timeRemaining.value > 0 && !_timerRunning.value) startTimer() }
    fun stopTimer() { timerJob?.cancel(); _timerRunning.value = false }
    fun resetTimer() { _timeRemaining.value = testDuration; _timerRunning.value = false; _timerFinished.value = false }
    fun getFormattedTime(): String = String.format("%02d:%02d", _timeRemaining.value / 60000, (_timeRemaining.value / 1000) % 60)
    fun getFormattedTimeLong(): String {
        val seconds = _timeRemaining.value / 1000
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    }
    fun getCurrentQuestionNumber(): String = "${_currentQuestionIndex.value + 1}/${_totalQuestions.value}"
    fun getProgress(): Float = if (_totalQuestions.value == 0) 0f else (_currentQuestionIndex.value + 1).toFloat() / _totalQuestions.value

    fun selectOption(index: Int) {
        val q = _currentQuestion.value ?: return
        answers[q.id] = index
        _selectedOption.value = index
        _isAnswered.value = true
    }
    fun skipQuestion() = nextQuestion()
    fun nextQuestion() {
        if (_currentQuestionIndex.value < _questions.value.lastIndex) {
            _currentQuestionIndex.value++
            _currentQuestion.value = _questions.value[_currentQuestionIndex.value]
            syncCurrentAnswer()
        } else finishTest()
    }
    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value--
            _currentQuestion.value = _questions.value[_currentQuestionIndex.value]
            syncCurrentAnswer()
        }
    }

    private fun finishTest() {
        stopTimer()
        val all = _questions.value
        _correctQuestions.value = all.filter { q -> answers[q.id] != null && answers[q.id] == q.correctOptionIndex }
        _incorrectQuestions.value = all.filter { q -> answers[q.id] != null && answers[q.id] != q.correctOptionIndex }
        _unattendedQuestions.value = all.filter { q -> answers[q.id] == null }
        _score.value = _correctQuestions.value.size
        _testCompleted.value = true
    }

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
        answers.clear()
    }

    fun selectAllLeafVaults() { _selectedVaults.value = getSelectableVaults().map { it.id }.toSet() }
    fun deselectAllVaults() { _selectedVaults.value = emptySet() }
}
