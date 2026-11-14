package com.example.questionbank.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.questionbank.database.QuestionDao
import com.example.questionbank.database.VaultDao
import com.example.questionbank.model.Question
import com.example.questionbank.model.Vault
import com.example.questionbank.viewmodel.QuestionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionListScreen(
    vaultId: Long,
    questionDao: QuestionDao,
    vaultDao: VaultDao,
    darkTheme: Boolean,
    darcula: Boolean,
    onNavigateToAddQuestion: () -> Unit,
    onNavigateToEditQuestion: (Long) -> Unit,
    onNavigateToSolution: (Long) -> Unit,
    onBack: () -> Unit
) {
    val viewModel = remember { QuestionViewModel(questionDao) }
    val questions by viewModel.questions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var vault by remember { mutableStateOf<Vault?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(vaultId) {
        vaultDao.getVaultById(vaultId).collect { v ->
            vault = v
            if (v != null) viewModel.getQuestionsByVaultFlow(v.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vault?.name ?: "Questions", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Search") }, onClick = { showMenu = false })
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddQuestion,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Question")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search questions...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )

            val visibleQuestions = questions.filter {
                searchQuery.isBlank() ||
                    it.questionText.contains(searchQuery, ignoreCase = true) ||
                    it.options.any { option -> option.contains(searchQuery, ignoreCase = true) }
            }

            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading questions...")
                }
            } else if (visibleQuestions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (searchQuery.isBlank()) "No questions yet" else "No matching questions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (searchQuery.isBlank()) "Tap + to add your first question" else "Try a different search",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(visibleQuestions, key = { _, question -> question.id }) { index, question ->
                        QuestionCard(
                            question = question,
                            questionNumber = index + 1,
                            darkTheme = darkTheme || darcula,
                            onEditClick = { onNavigateToEditQuestion(question.id) },
                            onFavoriteClick = {
                                scope.launch { questionDao.updateFavorite(question.id, !question.isFavorite) }
                            },
                            onSolutionClick = { onNavigateToSolution(question.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionCard(
    question: Question,
    questionNumber: Int,
    darkTheme: Boolean,
    onEditClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onSolutionClick: () -> Unit
) {
    var selectedOption by remember(question.id) { mutableStateOf<Int?>(null) }
    val correctIndex = question.correctOptionIndex
    val image = question.diagramUri?.takeIf { it.isNotBlank() }
        ?: question.imageUri?.takeIf { it.isNotBlank() }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = questionNumber.toString() + ".",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
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
                    colorFilter = if (darkTheme) ColorFilter.colorMatrix(invertImageColorMatrix()) else null
                )
            }

            if (question.options.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    question.options.forEachIndexed { index, option ->
                        val isCorrect = selectedOption != null && correctIndex == index
                        val isWrongSelection = selectedOption == index &&
                            correctIndex != null && selectedOption != correctIndex
                        val background = when {
                            isCorrect -> Color(0xFF1B5E20)
                            isWrongSelection -> Color(0xFFB71C1C)
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val foreground = if (isCorrect || isWrongSelection) Color.White
                            else MaterialTheme.colorScheme.onSurface

                        Card(
                            onClick = { selectedOption = index },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = background),
                            border = if (selectedOption == index || isCorrect) {
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isWrongSelection) Color(0xFFFF8A80) else Color(0xFF81C784)
                                )
                            } else null
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = foreground
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Solution",
                    modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onSolutionClick).padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit question")
                }
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        if (question.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (question.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

internal fun invertImageColorMatrix(): ColorMatrix = ColorMatrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    )
)
