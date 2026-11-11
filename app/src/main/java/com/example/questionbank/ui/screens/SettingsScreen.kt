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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
