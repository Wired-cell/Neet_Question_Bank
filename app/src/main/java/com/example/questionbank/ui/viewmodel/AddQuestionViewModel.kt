package com.example.questionbank.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.questionbank.database.QuestionDao
import com.example.questionbank.database.VaultDao
import com.example.questionbank.model.Difficulty
import com.example.questionbank.model.Question
import com.example.questionbank.utils.OcrUtils
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class AddQuestionViewModel(
    private val vaultDao: VaultDao,
    private val questionDao: QuestionDao,
    private val questionId: Long?
) : ViewModel() {
    
    private val _vaults = MutableStateFlow<List<com.example.questionbank.model.Vault>>(emptyList())
    val vaults: StateFlow<List<com.example.questionbank.model.Vault>> = _vaults.asStateFlow()
    
    private val _selectedVault = MutableStateFlow<com.example.questionbank.model.Vault?>(null)
    val selectedVault: StateFlow<com.example.questionbank.model.Vault?> = _selectedVault.asStateFlow()
    
    private val _questionText = MutableStateFlow("")
    val questionText: StateFlow<String> = _questionText.asStateFlow()
    
    private val _options = MutableStateFlow<List<String>>(emptyList())
    val options: StateFlow<List<String>> = _options.asStateFlow()
    
    private val _selectedOption = MutableStateFlow<Int?>(null)
    val selectedOption: StateFlow<Int?> = _selectedOption.asStateFlow()
    
    private val _explanation = MutableStateFlow("")
    val explanation: StateFlow<String> = _explanation.asStateFlow()
    
    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri.asStateFlow()
    
    private val _diagramUri = MutableStateFlow<Uri?>(null)
    val diagramUri: StateFlow<Uri?> = _diagramUri.asStateFlow()
    
    private val _ocrText = MutableStateFlow("")
    val ocrText: StateFlow<String> = _ocrText.asStateFlow()
    
    private val _ocrResult = MutableStateFlow<com.example.questionbank.utils.OcrUtils.OcrResult?>(null)
    val ocrResult: StateFlow<com.example.questionbank.utils.OcrUtils.OcrResult?> = _ocrResult.asStateFlow()
    
    private val _showCropper = MutableStateFlow(false)
    val showCropper: StateFlow<Boolean> = _showCropper.asStateFlow()
    
    private val _cropperImageUri = MutableStateFlow<Uri?>(null)
    val cropperImageUri: StateFlow<Uri?> = _cropperImageUri.asStateFlow()
    
    private val _isCropping = MutableStateFlow(false)
    val isCropping: StateFlow<Boolean> = _isCropping.asStateFlow()
    
    // Cropper state
    private var cropperOutputUri: Uri? = null
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    private var currentQuestion: Question? = null
    
    init {
        loadVaults()
        loadQuestion()
    }
    
    private fun loadVaults() {
        viewModelScope.launch {
            vaultDao.getAllVaults().collect { vaults ->
                _vaults.value = vaults
                // Select first vault by default if none selected
                if (_selectedVault.value == null && vaults.isNotEmpty()) {
                    _selectedVault.value = vaults.firstOrNull()
                }
            }
        }
    }
    
    private fun loadQuestion() {
        if (questionId != null) {
            viewModelScope.launch {
                questionDao.getQuestionById(questionId).collect { question ->
                    currentQuestion = question
                    question?.let { q ->
                        _selectedVault.value = _vaults.value.find { it.id == q.vaultId }
                        _questionText.value = q.questionText
                        _options.value = q.options
                        _selectedOption.value = q.correctOptionIndex
                        _explanation.value = q.explanation
                        _imageUri.value = q.imageUri?.let { Uri.parse(it) }
                        _diagramUri.value = q.diagramUri?.let { Uri.parse(it) }
                    }
                }
            }
        }
    }
    
    fun setSelectedVault(vault: com.example.questionbank.model.Vault) {
        _selectedVault.value = vault
    }
    
    fun setQuestionText(text: String) {
        _questionText.value = text
    }
    
    fun addOption() {
        _options.value = _options.value + ""
    }
    
    fun removeOption(index: Int) {
        val newOptions = _options.value.toMutableList()
        newOptions.removeAt(index)
        _options.value = newOptions
        // Adjust selected option if needed
        if (_selectedOption.value != null && _selectedOption.value!! >= index) {
            _selectedOption.value = if (_selectedOption.value!! > index) _selectedOption.value!! - 1 else null
        }
    }
    
    fun updateOption(index: Int, text: String) {
        val newOptions = _options.value.toMutableList()
        if (index < newOptions.size) {
            newOptions[index] = text
            _options.value = newOptions
        }
    }
    
    fun setSelectedOption(index: Int?) {
        _selectedOption.value = index
    }
    
    fun setExplanation(text: String) {
        _explanation.value = text
    }
    
    fun setImageUri(uri: Uri?) {
        _imageUri.value = uri
    }
    
    fun setDiagramUri(uri: Uri?) {
        _diagramUri.value = uri
    }
    
    fun clearOcrText() {
        _ocrText.value = ""
    }
    
    fun setOcrResult(result: com.example.questionbank.utils.OcrUtils.OcrResult?) {
        _ocrResult.value = result
    }
    
    fun setOptions(options: List<String>) {
        _options.value = options
    }
    
    fun processImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val rawText = OcrUtils.recognizeTextFromUri(context, recognizer, uri)
                _ocrText.value = rawText
                
                // Process OCR text to extract question and options
                val ocrResult = OcrUtils.processOcrText(rawText)
                _ocrResult.value = ocrResult
                
                // Auto-fill question text if not already set
                if (_questionText.value.isBlank() && ocrResult.questionText.isNotBlank()) {
                    _questionText.value = ocrResult.questionText
                }
                
                // Auto-detect options from OCR
                if (_options.value.isEmpty() && ocrResult.options.isNotEmpty()) {
                    _options.value = ocrResult.options
                }
                
                // Check if diagram is likely present
                if (ocrResult.hasDiagram && _diagramUri.value == null) {
                    // Suggest cropping diagram
                    _cropperImageUri.value = uri
                }
            } catch (e: Exception) {
                Log.e("AddQuestionViewModel", "OCR failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Start cropping an image for diagram
     */
    fun startCropping(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isCropping.value = true
            _cropperImageUri.value = uri
        }
    }
    
    /**
     * Handle cropper result
     */
    fun onCropResult(uri: Uri?) {
        viewModelScope.launch {
            _isCropping.value = false
            _cropperImageUri.value = null
            
            uri?.let { croppedUri ->
                _diagramUri.value = croppedUri
            }
        }
    }
    
    /**
     * Cancel cropping
     */
    fun cancelCropping() {
        viewModelScope.launch {
            _isCropping.value = false
            _cropperImageUri.value = null
        }
    }
    
    private fun autoDetectOptions(text: String) {
        // Pattern to detect options like A) ..., B) ..., etc.
        val optionPatterns = listOf(
            Regex("""([A-Da-d])\)\s+(.+?)(?=\s*[A-Da-d]|\)|$)"""),
            Regex("""([A-Da-d])\s*\.\s+(.+?)(?=\s*[A-Da-d]|\.|$)"""),
            Regex("""([A-Da-d])\s*-\s+(.+?)(?=\s*[A-Da-d]|-|$)""")
        )
        
        val detectedOptions = mutableListOf<String>()
        for (pattern in optionPatterns) {
            val matches = pattern.findAll(text)
            matches.forEach { match ->
                val optionText = match.groupValues.getOrNull(2)?.trim() ?: ""
                if (optionText.isNotEmpty()) {
                    detectedOptions.add(optionText)
                }
            }
            if (detectedOptions.isNotEmpty()) break
        }
        
        // Only set options if we detected some and user hasn't added any yet
        if (detectedOptions.isNotEmpty() && _options.value.isEmpty()) {
            _options.value = detectedOptions.take(4)
        }
    }
    
    fun cropDiagramFromImage(context: Context) {
        viewModelScope.launch {
            _imageUri.value?.let { uri ->
                try {
                    // For now, just copy the image as diagram
                    // In a real implementation, this would open a cropper
                    val file = File.createTempFile(
                        "diagram_${System.currentTimeMillis()}",
                        ".jpg",
                        context.cacheDir
                    )
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val outputStream = FileOutputStream(file)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    
                    _diagramUri.value = Uri.fromFile(file)
                } catch (e: Exception) {
                    Log.e("AddQuestionViewModel", "Failed to crop diagram", e)
                }
            }
        }
    }
    
    fun fetchExplanation(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // This would call Google AI Studio API
                // For now, we'll just set a placeholder
                _explanation.value = "Explanation for: $query\n\nThis would be fetched from Google AI Studio using the API key from settings."
            } catch (e: Exception) {
                Log.e("AddQuestionViewModel", "Failed to fetch explanation", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun canSave(): Boolean {
        return _questionText.value.isNotBlank() && 
               _selectedVault.value != null &&
               !_isSaving.value
    }
    
    suspend fun saveQuestion() {
        if (!canSave()) return
        
        _isSaving.value = true
        try {
            val vault = _selectedVault.value ?: return
            val question = Question(
                id = currentQuestion?.id ?: 0,
                vaultId = vault.id,
                questionText = _questionText.value,
                options = _options.value.filter { it.isNotBlank() },
                correctOptionIndex = _selectedOption.value,
                explanation = _explanation.value,
                imageUri = _imageUri.value?.toString(),
                diagramUri = _diagramUri.value?.toString(),
                isFavorite = currentQuestion?.isFavorite ?: false,
                difficulty = currentQuestion?.difficulty ?: Difficulty.MEDIUM,
                tags = currentQuestion?.tags ?: emptyList(),
                createdAt = currentQuestion?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            if (question.id == 0L) {
                questionDao.insert(question)
            } else {
                questionDao.update(question)
            }
        } catch (e: Exception) {
            Log.e("AddQuestionViewModel", "Failed to save question", e)
            throw e
        } finally {
            _isSaving.value = false
        }
    }
    
    class Factory(
        private val vaultDao: VaultDao,
        private val questionDao: QuestionDao,
        private val questionId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddQuestionViewModel(vaultDao, questionDao, questionId) as T
        }
    }
}
