package com.example.questionbank.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.questionbank.database.QuestionDao
import com.example.questionbank.database.VaultDao
import com.example.questionbank.ui.viewmodel.TestViewModel
import com.example.questionbank.viewmodel.AppViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    vaultDao: VaultDao,
    questionDao: QuestionDao,
    onBack: () -> Unit
) {
    val viewModel: TestViewModel = viewModel(
        factory = TestViewModelFactory(vaultDao, questionDao)
    )
    
    val vaults by viewModel.vaults.collectAsState()
    val selectedVaults by viewModel.selectedVaults.collectAsState()
    val testStarted by viewModel.testStarted.collectAsState()
    val testCompleted by viewModel.testCompleted.collectAsState()
    val timerFinished by viewModel.timerFinished.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val totalQuestions by viewModel.totalQuestions.collectAsState()
    val selectedOption by viewModel.selectedOption.collectAsState()
    val isAnswered by viewModel.isAnswered.collectAsState()
    val isCorrect by viewModel.isCorrect.collectAsState()
    val score by viewModel.score.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val timerRunning by viewModel.timerRunning.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (testStarted) "Test Mode" else "Start Test",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (testStarted) {
                            viewModel.resetTest()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (testStarted) {
            if (testCompleted || timerFinished) {
                viewModel.stopTimer()
                
                TestResultScreen(
                    score = score,
                    totalQuestions = totalQuestions,
                    onRestart = { viewModel.resetTest() },
                    onBack = onBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } else if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading questions...")
                }
            } else {
                TestQuestionScreen(
                    viewModel = viewModel,
                    currentQuestion = currentQuestion,
                    currentQuestionIndex = currentQuestionIndex,
                    totalQuestions = totalQuestions,
                    selectedOption = selectedOption,
                    isAnswered = isAnswered,
                    isCorrect = isCorrect,
                    score = score,
                    onOptionSelect = viewModel::selectOption,
                    onNext = viewModel::nextQuestion,
                    onPrevious = viewModel::previousQuestion,
                    onSkip = viewModel::skipQuestion,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        } else {
            TestSetupScreen(
                viewModel = viewModel,
                vaults = vaults,
                selectedVaults = selectedVaults,
                onStartTest = viewModel::startTest,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
fun TestSetupScreen(
    viewModel: TestViewModel,
    vaults: List<com.example.questionbank.model.Vault>,
    selectedVaults: Set<Long>,
    onStartTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val leafVaults = viewModel.getLeafVaults()
    
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Test Mode",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Select the subjects you want to test on. Each subject will have 45 questions.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Subjects",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = viewModel::selectAllLeafVaults,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Select All")
                    }
                    FilledTonalButton(
                        onClick = viewModel::deselectAllVaults,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Display leaf vaults (sub-vaults only)
                leafVaults.forEach { vault ->
                    VaultSelectionItem(
                        vault = vault,
                        isSelected = selectedVaults.contains(vault.id),
                        onClick = { viewModel.toggleVault(vault.id) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onStartTest,
            enabled = selectedVaults.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Start Test", style = MaterialTheme.typography.labelLarge)
        }
        
        if (selectedVaults.isEmpty()) {
            Text(
                text = "Please select at least one subject",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun VaultSelectionItem(
    vault: com.example.questionbank.model.Vault,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Card(
            modifier = Modifier.size(32.dp),
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer
            else
                Color(vault.color)
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            } else {
                Text(
                    text = vault.icon.ifEmpty { vault.name.take(1) },
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = vault.name,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun TestQuestionScreen(
    viewModel: TestViewModel,
    currentQuestion: com.example.questionbank.model.Question?,
    currentQuestionIndex: Int,
    totalQuestions: Int,
    selectedOption: Int?,
    isAnswered: Boolean,
    isCorrect: Boolean?,
    score: Int,
    onOptionSelect: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress, score, and timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = viewModel.getCurrentQuestionNumber(),
                style = MaterialTheme.typography.labelLarge
            )
            
            LinearProgressIndicator(
                progress = { viewModel.getProgress() / 100f },
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            )
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = viewModel.getFormattedTimeLong(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (timeRemaining <= 60 * 1000) MaterialTheme.colorScheme.error 
                            else if (timeRemaining <= 10 * 60 * 1000) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // Question
        currentQuestion?.let { question ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Q${currentQuestionIndex + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = question.questionText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    // Image if available
                    if (question.imageUri != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Question Image",
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                    
                    // Diagram if available
                    if (question.diagramUri != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Diagram",
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
            
            // Options
            if (question.options.isNotEmpty()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        question.options.forEachIndexed { index, option ->
                            val isSelected = selectedOption == index
                            val showResult = isAnswered && question.correctOptionIndex == index
                            
                            OptionItem(
                                letter = ('A' + index).toString(),
                                text = option,
                                isSelected = isSelected,
                                isCorrect = showResult && isCorrect == true,
                                isIncorrect = showResult && isCorrect == false,
                                enabled = !isAnswered,
                                onClick = { onOptionSelect(index) }
                            )
                        }
                    }
                }
            }
            
            // Explanation (if answered)
            if (isAnswered && question.explanation.isNotBlank()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Explanation:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Answer feedback
            if (isAnswered) {
                val feedbackText = if (isCorrect == true) {
                    "Correct! ✓"
                } else if (isCorrect == false) {
                    "Incorrect ✗"
                } else {
                    "Skipped"
                }
                val feedbackColor = if (isCorrect == true) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else if (isCorrect == false) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                val feedbackBg = if (isCorrect == true) {
                    MaterialTheme.colorScheme.primaryContainer
                } else if (isCorrect == false) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = feedbackBg
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(
                            if (isCorrect == true) Icons.Default.CheckCircle
                            else if (isCorrect == false) Icons.Default.Close
                            else Icons.Default.HelpOutline,
                            contentDescription = feedbackText,
                            tint = feedbackColor
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = feedbackText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = feedbackColor
                        )
                        
                        if (question.correctOptionIndex != null && isCorrect == false) {
                            Spacer(modifier = Modifier.size(16.dp))
                            Text(
                                text = "Correct: ${('A' + (question.correctOptionIndex)).toString()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = feedbackColor
                            )
                        }
                    }
                }
            }
            
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = currentQuestionIndex > 0 && !isAnswered,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Previous")
                }
                
                if (!isAnswered) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = "Skip")
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Skip")
                    }
                } else {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (currentQuestionIndex < totalQuestions - 1) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next")
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Next")
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Finish")
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Finish")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OptionItem(
    letter: String,
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isIncorrect: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCorrect -> MaterialTheme.colorScheme.primaryContainer
        isIncorrect -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    
    val borderColor = when {
        isCorrect -> MaterialTheme.colorScheme.primary
        isIncorrect -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    
    ElevatedCard(
        onClick = { if (enabled) onClick() },
        modifier = Modifier.fillMaxWidth(),
        containerColor = backgroundColor,
        border = if (isSelected || isCorrect || isIncorrect) {
            androidx.compose.material3.CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(borderColor),
                width = 2.dp
            )
        } else {
            null
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected || isCorrect) 
                            MaterialTheme.colorScheme.primary
                        else if (isIncorrect)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected || isCorrect) 
                            MaterialTheme.colorScheme.onPrimary
                        else if (isIncorrect)
                            MaterialTheme.colorScheme.onError
                        else
                            MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected || isCorrect) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                } else if (isIncorrect) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Incorrect",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.size(12.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TestResultScreen(
    score: Int,
    totalQuestions: Int,
    timeRemaining: String = "00:00:00",
    timerFinished: Boolean = false,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percentage = if (totalQuestions > 0) (score.toFloat() / totalQuestions * 100) else 0f
    
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        // Score circle
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    when {
                        percentage >= 80 -> MaterialTheme.colorScheme.primaryContainer
                        percentage >= 60 -> MaterialTheme.colorScheme.tertiaryContainer
                        percentage >= 40 -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "of $totalQuestions",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Percentage
        Text(
            text = "${String.format("%.1f", percentage)}%",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                percentage >= 80 -> MaterialTheme.colorScheme.primary
                percentage >= 60 -> MaterialTheme.colorScheme.tertiary
                percentage >= 40 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Timer info
        if (timerFinished) {
            Text(
                text = "Time's up!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        Text(
            text = "Time Remaining: $timeRemaining",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Grade
        val grade = when {
            percentage >= 90 -> "A+ (Excellent!)"
            percentage >= 80 -> "A (Great!)"
            percentage >= 70 -> "B (Good)"
            percentage >= 60 -> "C (Average)"
            percentage >= 50 -> "D (Pass)"
            else -> "F (Fail)"
        }
        Text(
            text = grade,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Summary
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Correct Answers:")
                    Text("$score")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Questions:")
                    Text("$totalQuestions")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Percentage:")
                    Text("${String.format("%.1f", percentage)}%")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Home, contentDescription = "Home")
                Spacer(modifier = Modifier.size(4.dp))
                Text("Home")
            }
            
            Button(
                onClick = onRestart,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Replay, contentDescription = "Restart")
                Spacer(modifier = Modifier.size(4.dp))
                Text("Restart Test")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Factory for TestViewModel
class TestViewModelFactory(
    private val vaultDao: VaultDao,
    private val questionDao: QuestionDao
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return TestViewModel(vaultDao, questionDao) as T
    }
}
