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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
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
    private val _isFetchingExplanation = MutableStateFlow(false)
    val isFetchingExplanation: StateFlow<Boolean> = _isFetchingExplanation.asStateFlow()
    
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
    
    fun fetchExplanation(query: String, context: Context, sourceImageUri: Uri? = null) {
        viewModelScope.launch {
            _isFetchingExplanation.value = true
            try {
                val apiKey = context.applicationContext
                    .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .getString("google_ai_key", "")
                    ?.trim()
                    .orEmpty()
                if (apiKey.isBlank()) {
                    _explanation.value = "Please add your Google AI Studio API key in Settings, then try again."
                    return@launch
                }
                if (query.isBlank()) {
                    _explanation.value = "Please enter a question before requesting an explanation."
                    return@launch
                }
                _explanation.value = withContext(Dispatchers.IO) {
                    requestGeminiExplanation(apiKey, query, context, sourceImageUri)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AddQuestionViewModel", "Failed to fetch explanation", e)
                _explanation.value = when (e) {
                    is java.net.SocketTimeoutException ->
                        "Gemini took too long to respond. Your question may still be processing; please try Fetch Solution again."
                    is java.net.SocketException, is java.io.IOException ->
                        "The connection to Google's AI service was interrupted. This is not necessarily a Wi-Fi or API-key problem. Please try again."
                    else ->
                        "Could not fetch an explanation: ${e.message ?: "Unknown error"}"
                }
            } finally {
                _isFetchingExplanation.value = false
            }
        }
    }

    private fun requestGeminiExplanation(
        apiKey: String,
        question: String,
        context: Context,
        sourceImageUri: Uri?
    ): String {
        val imageInstruction = if (sourceImageUri == null) {
            "No source image is attached. Solve using the question text below.\nQuestion text:\n$question"
        } else {
            "Use the attached original question photo or gallery image as the source of truth. It contains the full printed question, answer choices, mathematical notation and diagram."
        }
        val prompt = """
            You are a careful physics and science tutor. Read the supplied question image yourself,
            including every line of text, all answer choices, labels, units, mathematical notation,
            and the complete diagram. Treat the image as the primary source. Do not rely on OCR
            text or assume missing values from a textbook or familiar question. If a symbol, value,
            or diagram detail is genuinely unreadable, say exactly what is unclear rather than guessing.
            Solve the question step by step, define variables, show substitutions and calculations,
            and clearly identify the correct option when possible.

            IMPORTANT OUTPUT FORMATTING:
            The answer is displayed in a plain text field, not a LaTeX renderer. Do NOT use LaTeX,
            dollar signs for math, Markdown, or commands such as \\( \\), \\[ \\], \\frac,
            \\sqrt, \\times, or \\text. Write equations as readable plain text using Unicode
            symbols: ×, ÷, √, =, ≈, ≤, ≥, Δ, θ, π, →, and superscripts/subscripts such as m²,
            x₁, v₀, and 10⁻³. Put each equation on its own line. Use clear headings and numbered
            steps without Markdown decoration.

            $imageInstruction
        """.trimIndent()

        // Attach the original image so Gemini can inspect diagrams and notation directly.
        val input = org.json.JSONArray()
            .put(JSONObject().put("type", "text").put("text", prompt))
        if (sourceImageUri != null) {
            val resolver = context.contentResolver
            val imageBytes = resolver.openInputStream(sourceImageUri)?.use { it.readBytes() }
                ?: throw java.io.IOException("Could not read the original question image.")
            if (imageBytes.size > 14 * 1024 * 1024) {
                throw IllegalStateException("The question image is too large to send. Please crop or resize it and try again.")
            }
            val mimeType = resolver.getType(sourceImageUri)
                ?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
            val encodedImage = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            input.put(
                JSONObject()
                    .put("type", "image")
                    .put("mime_type", mimeType)
                    .put("data", encodedImage)
            )
        }

        // Background mode avoids holding one long HTTP connection open during generation.
        val createBody = JSONObject()
            .put("model", "gemini-3.8-flash")
            .put("input", input)
            .put("background", true)
        val created = executeGeminiRequest(
            url = URL("https://generativelanguage.googleapis.com/v1beta/interactions"),
            apiKey = apiKey,
            method = "POST",
            body = createBody.toString()
        )
        val interactionId = created.optString("id")
        val initialStatus = created.optString("status")

        if (initialStatus == "completed") return extractGeminiText(created)
        if (initialStatus == "failed" || initialStatus == "cancelled" || initialStatus == "incomplete") {
            throw IllegalStateException("Gemini ended the request with status '$initialStatus'.")
        }
        if (interactionId.isBlank()) {
            throw IllegalStateException("Google accepted the request but did not return an interaction ID.")
        }

        // Poll with short independent requests. A brief socket reset no longer
        // destroys the whole generation; the model continues on Google's servers.
        repeat(30) {
            Thread.sleep(2000)
            val current = try {
                executeGeminiRequest(
                    url = URL("https://generativelanguage.googleapis.com/v1beta/interactions/$interactionId"),
                    apiKey = apiKey,
                    method = "GET"
                )
            } catch (e: java.io.IOException) {
                Log.w("AddQuestionViewModel", "Temporary connection issue while checking Gemini; will retry", e)
                return@repeat
            }

            when (val status = current.optString("status")) {
                "completed" -> return extractGeminiText(current)
                "failed", "cancelled", "incomplete", "budget_exceeded" ->
                    throw IllegalStateException("Gemini finished with status '$status'.")
            }
        }

        throw java.net.SocketTimeoutException(
            "Gemini is still processing this question after 60 seconds. Please try again in a moment."
        )
    }

    private fun executeGeminiRequest(
        url: URL,
        apiKey: String,
        method: String,
        body: String? = null
    ): JSONObject {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8000
            readTimeout = 12000
            useCaches = false
            setRequestProperty("Connection", "close")
            setRequestProperty("x-goog-api-key", apiKey)
            setRequestProperty("Api-Revision", "2026-05-20")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            if (body != null) {
                val bytes = body.toByteArray(Charsets.UTF_8)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseText.isBlank()) {
                throw java.io.IOException("Google returned an empty response (HTTP $status).")
            }
            val response = JSONObject(responseText)
            if (status !in 200..299) {
                val message = response.optJSONObject("error")?.optString("message")
                    ?.takeIf { it.isNotBlank() }
                    ?: "Google AI Studio returned HTTP $status"
                throw IllegalStateException(message)
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun extractGeminiText(interaction: JSONObject): String {
        val steps = interaction.optJSONArray("steps")
            ?: throw IllegalStateException("Gemini completed without returning any output steps.")
        val answer = buildString {
            for (stepIndex in 0 until steps.length()) {
                val step = steps.optJSONObject(stepIndex) ?: continue
                if (step.optString("type") != "model_output") continue
                val content = step.optJSONArray("content") ?: continue
                for (contentIndex in 0 until content.length()) {
                    val item = content.optJSONObject(contentIndex) ?: continue
                    val text = item.optString("text")
                    if (item.optString("type") == "text" && text.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(text)
                    }
                }
            }
        }.trim()
        if (answer.isBlank()) {
            throw IllegalStateException("Gemini completed the request but returned no text explanation.")
        }
        return answer
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
