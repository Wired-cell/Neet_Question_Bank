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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.questionbank.database.QuestionDao
import com.example.questionbank.database.VaultDao
import com.example.questionbank.model.Question
import com.example.questionbank.model.Vault
import com.example.questionbank.ui.viewmodel.TestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    vaultDao: VaultDao,
    questionDao: QuestionDao,
    onBack: () -> Unit,
    onNavigateToSolution: (Long) -> Unit
) {
    val vm: TestViewModel = viewModel(factory = TestViewModelFactory(vaultDao, questionDao))
    val vaults by vm.vaults.collectAsState()
    val selectedVaults by vm.selectedVaults.collectAsState()
    val testStarted by vm.testStarted.collectAsState()
    val testCompleted by vm.testCompleted.collectAsState()
    val currentQuestion by vm.currentQuestion.collectAsState()
    val currentIndex by vm.currentQuestionIndex.collectAsState()
    val total by vm.totalQuestions.collectAsState()
    val selectedOption by vm.selectedOption.collectAsState()
    val loading by vm.isLoading.collectAsState()
    val timeRemaining by vm.timeRemaining.collectAsState()
    val score by vm.score.collectAsState()
    val chemistryWeights by vm.chemistryWeights.collectAsState()
    val answerMap by vm.answerMap.collectAsState()
    val correct by vm.correctQuestions.collectAsState()
    val incorrect by vm.incorrectQuestions.collectAsState()
    val unattended by vm.unattendedQuestions.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (testStarted) "Test" else "Set up test") },
            navigationIcon = {
                IconButton(onClick = { if (testStarted) vm.resetTest() else onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { padding ->
        when {
            !testStarted -> TestSetupScreen(
                vm, selectedVaults, chemistryWeights,
                Modifier.fillMaxSize().padding(padding)
            )
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            testCompleted -> TestResultScreen(
                score = score,
                totalQuestions = total,
                correct = correct,
                answerMap = answerMap,
                incorrect = incorrect,
                unattended = unattended,
                onSolution = onNavigateToSolution,
                onRestart = vm::resetTest,
                onBack = onBack,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            else -> TestQuestionScreen(
                question = currentQuestion,
                questionNumber = currentIndex + 1,
                total = total,
                selectedOption = selectedOption,
                timeRemaining = timeRemaining,
                progress = vm.getProgress(),
                onSelect = vm::selectOption,
                onPrevious = vm::previousQuestion,
                onNext = vm::nextQuestion,
                onFinish = vm::nextQuestion,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}

@Composable
private fun TestSetupScreen(
    vm: TestViewModel,
    selectedVaults: Set<Long>,
    weights: Map<Long, Int>,
    modifier: Modifier = Modifier
) {
    val selectable = vm.getSelectableVaults()
    val chemistry = vm.getChemistryVaults().filter { it.id in selectedVaults }
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Choose question banks", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Physics and individual Chemistry sub-vaults are selectable. Biology, Botany, Zoology, and the Chemistry parent are excluded. Chemistry selections share 45 questions.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = vm::selectAllLeafVaults, modifier = Modifier.weight(1f)) { Text("Select all") }
            OutlinedButton(onClick = vm::deselectAllVaults, modifier = Modifier.weight(1f)) { Text("Clear") }
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                selectable.forEach { vault ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.toggleVault(vault.id) }.padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = vault.id in selectedVaults,
                            onCheckedChange = { vm.toggleVault(vault.id) }
                        )
                        Text(vault.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (chemistry.size > 1) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Chemistry question distribution", style = MaterialTheme.typography.titleMedium)
                    Text("Choose the relative focus for the 45 Chemistry questions. The selected weights are automatically scaled to 100%.")
                    chemistry.forEach { vault ->
                        val weight = weights[vault.id] ?: 33
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(vault.name)
                            Text("$weight%")
                        }
                        Slider(
                            value = weight.toFloat(),
                            onValueChange = { vm.setChemistryWeight(vault.id, it.toInt()) },
                            valueRange = 0f..100f
                        )
                    }
                }
            }
        }
        Button(
            onClick = vm::startTest,
            enabled = selectedVaults.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { Text("Start test") }
        if (selectedVaults.isEmpty()) Text("Select at least one question bank.", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun TestQuestionScreen(
    question: Question?,
    questionNumber: Int,
    total: Int,
    selectedOption: Int?,
    timeRemaining: Long,
    progress: Float,
    onSelect: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$questionNumber / $total", fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Text(String.format("%02d:%02d", timeRemaining / 60000, (timeRemaining / 1000) % 60))
        }
        question?.let { q ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(q.questionText, style = MaterialTheme.typography.bodyLarge)
                    val image = q.diagramUri ?: q.imageUri
                    if (!image.isNullOrBlank()) {
                        AsyncImage(
                            model = Uri.parse(image),
                            contentDescription = "Question diagram",
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            q.options.forEachIndexed { index, option ->
                Card(
                    onClick = { onSelect(index) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedOption == index) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = if (selectedOption == index) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(option, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        if (selectedOption == index) Icon(Icons.Default.Check, contentDescription = "Selected")
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onPrevious, enabled = questionNumber > 1, modifier = Modifier.weight(1f)) { Text("Previous") }
            Button(onClick = if (questionNumber == total) onFinish else onNext, modifier = Modifier.weight(1f)) {
                Text(if (questionNumber == total) "Submit test" else "Next")
            }
        }
        Text("You can change your selection before submitting. Answers and explanations are revealed only after the test.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TestResultScreen(
    score: Int,
    totalQuestions: Int,
    correct: List<Question>,
    answerMap: Map<Long, Int>,
    incorrect: List<Question>,
    unattended: List<Question>,
    onSolution: (Long) -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf("Correct") }
    val percentage = if (totalQuestions == 0) 0f else score * 100f / totalQuestions
    val categories = listOf("Correct" to correct, "Incorrect" to incorrect, "Not attended" to unattended)
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Test results", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("$score / $totalQuestions correct  •  ${"%.1f".format(percentage)}%")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { (name, list) ->
                Card(
                    modifier = Modifier.weight(1f).clickable { expanded = if (expanded == name) "" else name },
                    colors = CardDefaults.cardColors(containerColor = if (expanded == name) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(list.size.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(name, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        val visible = categories.firstOrNull { it.first == expanded }?.second.orEmpty()
        if (expanded.isNotEmpty()) {
            Text("$expanded questions", style = MaterialTheme.typography.titleMedium)
            visible.forEachIndexed { index, question ->
                ResultQuestionCard(index + 1, question, answerMap[question.id], onSolution)
            }
            if (visible.isEmpty()) Text("No questions in this category.")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Home, contentDescription = null); Text("Home")
            }
            Button(onClick = onRestart, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Replay, contentDescription = null); Text("Restart")
            }
        }
    }
}

@Composable
private fun ResultQuestionCard(number: Int, question: Question, selectedAnswer: Int?, onSolution: (Long) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$number. ${question.questionText}", style = MaterialTheme.typography.bodyLarge)
            val image = question.diagramUri ?: question.imageUri
            if (!image.isNullOrBlank()) {
                AsyncImage(Uri.parse(image), contentDescription = "Question diagram", modifier = Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Fit)
            }
            question.options.forEachIndexed { index, option ->
                val isCorrect = index == question.correctOptionIndex
                val isWrongSelection = selectedAnswer == index && selectedAnswer != question.correctOptionIndex
                val container = when { isCorrect -> Color(0xFF1B5E20); isWrongSelection -> Color(0xFFB71C1C); else -> MaterialTheme.colorScheme.surfaceVariant }
                val textColor = if (isCorrect || isWrongSelection) Color.White else MaterialTheme.colorScheme.onSurface
                Text(option, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(container).padding(10.dp), color = textColor)
            }
            Text(
                "Solution",
                modifier = Modifier.align(Alignment.Start).clickable { onSolution(question.id) }.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

class TestViewModelFactory(
    private val vaultDao: VaultDao,
    private val questionDao: QuestionDao
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        TestViewModel(vaultDao, questionDao) as T
}
