package com.example.questionbank.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darcula: Boolean,
    onDarculaChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var apiKey by remember {
        mutableStateOf(prefs.getString("google_ai_key", "") ?: "")
    }
    var isTestingGemini by remember { mutableStateOf(false) }
    var geminiTestResult by remember { mutableStateOf<String?>(null) }
    var geminiTestSucceeded by remember { mutableStateOf<Boolean?>(null) }
    var groqKey by remember { mutableStateOf(prefs.getString("groq_api_key", "") ?: "") }
    var nvidiaKey by remember { mutableStateOf(prefs.getString("nvidia_api_key", "") ?: "") }
    var groqModel by remember { mutableStateOf(prefs.getString("groq_model", "qwen/qwen3.8-27b") ?: "qwen/qwen3.8-27b") }
    var nvidiaModel by remember { mutableStateOf(prefs.getString("nvidia_model", "nvidia/nemotron-3.5-super-vl-120b-a12b") ?: "nvidia/nemotron-3.5-super-vl-120b-a12b") }
    var isTestingGroq by remember { mutableStateOf(false) }
    var isTestingNvidia by remember { mutableStateOf(false) }
    var groqTestResult by remember { mutableStateOf<String?>(null) }
    var nvidiaTestResult by remember { mutableStateOf<String?>(null) }
    var groqTestSucceeded by remember { mutableStateOf<Boolean?>(null) }
    var nvidiaTestSucceeded by remember { mutableStateOf<Boolean?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Turn Dark to Darcula", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Use near-black AMOLED surfaces while dark mode is enabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Switch(checked = darcula, onCheckedChange = onDarculaChange)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Google AI Studio API Key",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            prefs.edit().putString("google_ai_key", it).apply()
                            geminiTestResult = null
                            geminiTestSucceeded = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter your Google AI Studio API key") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This key will be used to fetch explanations for questions. Get your key from Google AI Studio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val keyToTest = apiKey.trim()
                            if (keyToTest.isNotEmpty() && !isTestingGemini) {
                                prefs.edit().putString("google_ai_key", keyToTest).apply()
                                isTestingGemini = true
                                geminiTestResult = null
                                geminiTestSucceeded = null
                                scope.launch {
                                    try {
                                        val response = withContext(Dispatchers.IO) {
                                            runGeminiConnectionTest(keyToTest)
                                        }
                                        geminiTestSucceeded = true
                                        geminiTestResult = "Gemini connection successful!\n\n$response"
                                    } catch (e: Exception) {
                                        geminiTestSucceeded = false
                                        geminiTestResult = "Gemini test failed: ${e.message ?: "Unknown error"}"
                                    } finally {
                                        isTestingGemini = false
                                    }
                                }
                            }
                        },
                        enabled = apiKey.isNotBlank() && !isTestingGemini,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTestingGemini) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text("  Testing Gemini…")
                        } else {
                            Text("Test Gemini")
                        }
                    }
                    geminiTestResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (geminiTestSucceeded) {
                                true -> MaterialTheme.colorScheme.primary
                                false -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ProviderApiCard(
                title = "Groq API",
                description = "Vision-capable model for explanations using question text and the cropped diagram.",
                keyValue = groqKey,
                keyPlaceholder = "Enter your Groq API key",
                onKeyChange = {
                    groqKey = it
                    prefs.edit().putString("groq_api_key", it).apply()
                    groqTestResult = null
                    groqTestSucceeded = null
                },
                model = groqModel,
                models = listOf("qwen/qwen3.8-27b", "meta-models/muse-spark-open"),
                onModelChange = {
                    groqModel = it
                    prefs.edit().putString("groq_model", it).apply()
                },
                isTesting = isTestingGroq,
                result = groqTestResult,
                succeeded = groqTestSucceeded,
                onTest = {
                    val key = groqKey.trim()
                    if (key.isNotEmpty() && !isTestingGroq) {
                        prefs.edit().putString("groq_api_key", key).apply()
                        isTestingGroq = true
                        groqTestResult = null
                        groqTestSucceeded = null
                        scope.launch {
                            try {
                                val answer = withContext(Dispatchers.IO) {
                                    runOpenAiCompatibleTest(key, groqModel, "https://api.groq.com/openai/v1/chat/completions")
                                }
                                groqTestSucceeded = true
                                groqTestResult = "Groq connection successful!\n\n$answer"
                            } catch (e: Exception) {
                                groqTestSucceeded = false
                                groqTestResult = "Groq test failed: ${e.message ?: "Unknown error"}"
                            } finally { isTestingGroq = false }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProviderApiCard(
                title = "NVIDIA Build API",
                description = "Vision-capable NVIDIA NIM model for explanations using question text and the cropped diagram.",
                keyValue = nvidiaKey,
                keyPlaceholder = "Enter your NVIDIA Build API key",
                onKeyChange = {
                    nvidiaKey = it
                    prefs.edit().putString("nvidia_api_key", it).apply()
                    nvidiaTestResult = null
                    nvidiaTestSucceeded = null
                },
                model = nvidiaModel,
                models = listOf("nvidia/nemotron-3.5-super-vl-120b-a12b", "stepfun-ai/step-5-preview"),
                onModelChange = {
                    nvidiaModel = it
                    prefs.edit().putString("nvidia_model", it).apply()
                },
                isTesting = isTestingNvidia,
                result = nvidiaTestResult,
                succeeded = nvidiaTestSucceeded,
                onTest = {
                    val key = nvidiaKey.trim()
                    if (key.isNotEmpty() && !isTestingNvidia) {
                        prefs.edit().putString("nvidia_api_key", key).apply()
                        isTestingNvidia = true
                        nvidiaTestResult = null
                        nvidiaTestSucceeded = null
                        scope.launch {
                            try {
                                val answer = withContext(Dispatchers.IO) {
                                    runOpenAiCompatibleTest(key, nvidiaModel, "https://integrate.api.nvidia.com/v1/chat/completions")
                                }
                                nvidiaTestSucceeded = true
                                nvidiaTestResult = "NVIDIA Build connection successful!\n\n$answer"
                            } catch (e: Exception) {
                                nvidiaTestSucceeded = false
                                nvidiaTestResult = "NVIDIA Build test failed: ${e.message ?: "Unknown error"}"
                            } finally { isTestingNvidia = false }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Question Bank App",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A comprehensive question bank app for students with OCR, diagram support, and AI-powered explanations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun runGeminiConnectionTest(apiKey: String): String {
    val prompt = "Introduce yourself briefly as the AI assistant in this app. Then answer this exact question: how many letter r's are in the word strawberry? Count the letters carefully."
    val body = JSONObject()
        .put("model", "gemini-3.8-flash")
        .put("input", prompt)
        .toString()

    val connection = (URL("https://generativelanguage.googleapis.com/v1beta/interactions")
        .openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 10000
        readTimeout = 25000
        doOutput = true
        useCaches = false
        setRequestProperty("Connection", "close")
        setRequestProperty("x-goog-api-key", apiKey)
        setRequestProperty("Api-Revision", "2026-05-20")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
    }

    try {
        val bytes = body.toByteArray(Charsets.UTF_8)
        connection.setFixedLengthStreamingMode(bytes.size)
        connection.outputStream.use { it.write(bytes) }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            val message = runCatching {
                JSONObject(responseText).optJSONObject("error")?.optString("message")
            }.getOrNull()?.takeIf { it.isNotBlank() }
            throw IllegalStateException(message ?: "Google AI Studio returned HTTP $status.")
        }

        val response = JSONObject(responseText)
        if (response.optString("status") != "completed") {
            throw IllegalStateException(
                "Google accepted the test but did not complete it immediately. Try again in a moment."
            )
        }

        val steps = response.optJSONArray("steps")
            ?: throw IllegalStateException("Gemini returned no output steps.")
        val answer = buildString {
            for (i in 0 until steps.length()) {
                val step = steps.optJSONObject(i) ?: continue
                if (step.optString("type") != "model_output") continue
                val content = step.optJSONArray("content") ?: continue
                for (j in 0 until content.length()) {
                    val item = content.optJSONObject(j) ?: continue
                    val text = item.optString("text")
                    if (item.optString("type") == "text" && text.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(text)
                    }
                }
            }
        }.trim()
        if (answer.isBlank()) throw IllegalStateException("Gemini returned an empty answer.")
        return answer
    } finally {
        connection.disconnect()
    }
}


@Composable
private fun ProviderApiCard(
    title: String,
    description: String,
    keyValue: String,
    keyPlaceholder: String,
    onKeyChange: (String) -> Unit,
    model: String,
    models: List<String>,
    onModelChange: (String) -> Unit,
    isTesting: Boolean,
    result: String?,
    succeeded: Boolean?,
    onTest: () -> Unit
) {
    var modelMenuExpanded by remember { mutableStateOf(false) }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = keyValue,
                onValueChange = onKeyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(keyPlaceholder) },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("Model to use", style = MaterialTheme.typography.labelLarge)
            androidx.compose.foundation.layout.Box {
                OutlinedButton(onClick = { modelMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(model)
                }
                DropdownMenu(expanded = modelMenuExpanded, onDismissRequest = { modelMenuExpanded = false }) {
                    models.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { onModelChange(option); modelMenuExpanded = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = onTest, enabled = keyValue.isNotBlank() && !isTesting, modifier = Modifier.fillMaxWidth()) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Text("  Testing…")
                } else Text("Test $title")
            }
            result?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = if (succeeded == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun runOpenAiCompatibleTest(apiKey: String, model: String, endpoint: String): String {
    val prompt = "Introduce yourself briefly as the AI assistant in this app. Then answer this exact question: how many letter r's are in the word strawberry? Count the letters carefully."
    val body = JSONObject()
        .put("model", model)
        .put("messages", org.json.JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        .put("temperature", 0.2)
        .toString()
    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15000
        readTimeout = 90000
        doOutput = true
        useCaches = false
        setRequestProperty("Connection", "close")
        setRequestProperty("Authorization", "Bearer $apiKey")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
    }
    try {
        val bytes = body.toByteArray(Charsets.UTF_8)
        connection.setFixedLengthStreamingMode(bytes.size)
        connection.outputStream.use { it.write(bytes) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            val message = runCatching { JSONObject(responseText).optJSONObject("error")?.optString("message") }.getOrNull()
                ?.takeIf { it.isNotBlank() }
            throw IllegalStateException(message ?: "Provider returned HTTP $status.")
        }
        val response = JSONObject(responseText)
        val answer = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.opt("content")
        val text = when (answer) {
            is String -> answer
            is org.json.JSONArray -> (0 until answer.length()).mapNotNull { answer.optJSONObject(it)?.optString("text") }.filter { it.isNotBlank() }.joinToString("\\n")
            else -> ""
        }.trim()
        if (text.isBlank()) throw IllegalStateException("Provider returned an empty answer.")
        return text
    } finally {
        connection.disconnect()
    }
}
