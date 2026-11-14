package com.example.questionbank.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.questionbank.database.QuestionDao

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolutionScreen(
    questionId: Long,
    questionDao: QuestionDao,
    darkTheme: Boolean,
    darcula: Boolean,
    onBack: () -> Unit
) {
    val question = questionDao.getQuestionById(questionId).collectAsState(initial = null).value
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solution") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (question == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val image = question.diagramUri?.takeIf { it.isNotBlank() }
                ?: question.imageUri?.takeIf { it.isNotBlank() }
            Column(
                modifier = Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = question.questionText,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (image != null) {
                    AsyncImage(
                        model = Uri.parse(image),
                        contentDescription = "Question diagram",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface),
                        contentScale = ContentScale.Fit,
                        colorFilter = if (darkTheme || darcula) {
                            androidx.compose.ui.graphics.ColorFilter.colorMatrix(invertImageColorMatrix())
                        } else null
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF1B5E20)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Correct answer", color = androidx.compose.ui.graphics.Color(0xFFC8E6C9),
                            style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        val answer = question.correctOptionIndex?.let { question.options.getOrNull(it) }
                        Text(
                            text = answer ?: "Correct answer not saved",
                            color = androidx.compose.ui.graphics.Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Text("Solution", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text(
                    text = question.explanation.ifBlank { "No saved solution is available for this question yet." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
