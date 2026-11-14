package com.example.questionbank.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.questionbank.database.QuestionDao
import com.example.questionbank.database.VaultDao
import com.example.questionbank.model.Vault
import com.example.questionbank.viewmodel.VaultViewModel
import kotlinx.coroutines.flow.combine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    vaultDao: VaultDao,
    questionDao: QuestionDao,
    onNavigateToAddQuestion: (Long) -> Unit,
    onNavigateToQuestionList: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTestMode: () -> Unit,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit
) {
    val viewModel = remember { VaultViewModel(vaultDao) }
    val vaults by viewModel.vaults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Question Bank", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    val themeRotation by animateFloatAsState(
                        targetValue = if (darkTheme) 0f else 180f,
                        animationSpec = tween(durationMillis = 420),
                        label = "themeIconRotation"
                    )
                    IconButton(onClick = { onDarkThemeChange(!darkTheme) }) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = if (darkTheme) "Switch to light theme" else "Switch to dark theme",
                            modifier = Modifier.graphicsLayer {
                                rotationZ = themeRotation
                                scaleX = 0.82f + (kotlin.math.abs(themeRotation - 90f) / 500f)
                                scaleY = scaleX
                            }
                        )
                    }
                    IconButton(onClick = { onNavigateToSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = { onNavigateToAddQuestion(0) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Question")
                }
                
                FloatingActionButton(
                    onClick = onNavigateToTestMode,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Test Mode")
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading vaults...")
            }
        } else {
            VaultHierarchyList(
                vaults = vaults,
                onVaultClick = { vault ->
                    // Open the question list for leaf root vaults such as Physics,
                    // as well as sub-vaults such as Chemistry and Biology categories.
                    onNavigateToQuestionList(vault.id)
                },
                onAddQuestionClick = { vaultId ->
                    onNavigateToAddQuestion(vaultId)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun VaultHierarchyList(
    vaults: List<Vault>,
    onVaultClick: (Vault) -> Unit,
    onAddQuestionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val rootVaults = vaults.filter { it.isRoot }.sortedBy { vault -> when (vault.name.lowercase()) { "physics" -> 0; "chemistry" -> 1; "biology" -> 2; else -> 3 } }
    
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(rootVaults) { rootVault ->
            val subVaults = vaults.filter { it.parentId == rootVault.id }.sortedBy { vault -> when (vault.name.lowercase()) { "physical chemistry" -> 0; "inorganic chemistry" -> 1; "organic chemistry" -> 2; "zoology" -> 0; "botany" -> 1; else -> 3 } }
            
            VaultCard(
                vault = rootVault,
                subVaults = subVaults,
                onVaultClick = onVaultClick,
                onAddQuestionClick = onAddQuestionClick
            )
        }
    }
}

@Composable
fun VaultCard(
    vault: Vault,
    subVaults: List<Vault>,
    onVaultClick: (Vault) -> Unit,
    onAddQuestionClick: (Long) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        onClick = { 
            if (subVaults.isNotEmpty()) {
                isExpanded = !isExpanded
            } else {
                onVaultClick(vault)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier
                        .size(48.dp)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(vault.color))
                ) {
                    Text(
                        text = vault.icon.ifEmpty { vaultIconFor(vault.name) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.size(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = vault.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (vault.description.isNotEmpty()) {
                        Text(
                            text = vault.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                if (subVaults.isNotEmpty()) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (isExpanded && subVaults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subVaults.forEach { subVault ->
                        SubVaultItem(
                            vault = subVault,
                            onClick = { onVaultClick(subVault) },
                            onAddQuestionClick = { onAddQuestionClick(subVault.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubVaultItem(
    vault: Vault,
    onClick: () -> Unit,
    onAddQuestionClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier.size(40.dp),
            colors = CardDefaults.cardColors(containerColor = Color(vault.color))
        ) {
            Text(
                text = vault.icon.ifEmpty { vaultIconFor(vault.name) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.size(12.dp))
        
        Text(
            text = vault.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        
        IconButton(
            onClick = { onAddQuestionClick() },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Question",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


private fun vaultIconFor(name: String): String = when (name.lowercase()) {
    "physics" -> "⚛️"
    "chemistry" -> "🧪"
    "physical chemistry" -> "⚗️"
    "inorganic chemistry" -> "🧬"
    "organic chemistry" -> "🧫"
    "biology" -> "🌿"
    "zoology" -> "🐾"
    "botany" -> "🌱"
    else -> "📚"
}
