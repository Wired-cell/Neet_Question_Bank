package com.example.questionbank.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.vanniktech.cropper.CropImageContract
import com.vanniktech.cropper.CropImageContractOptions
import com.vanniktech.cropper.CropImageOptions
import com.vanniktech.cropper.CropImageView
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.questionbank.database.QuestionDao
import com.example.questionbank.database.VaultDao
import com.example.questionbank.model.Question
import com.example.questionbank.ui.viewmodel.AddQuestionViewModel
import com.example.questionbank.utils.ImageUtils
import com.example.questionbank.utils.OcrUtils
import com.example.questionbank.utils.PermissionUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestionScreen(
    vaultId: Long,
    questionId: Long?,
    vaultDao: VaultDao,
    questionDao: QuestionDao,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: AddQuestionViewModel = viewModel(
        factory = AddQuestionViewModel.Factory(vaultDao, questionDao, questionId)
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    val vaults by viewModel.vaults.collectAsState()
    val selectedVault by viewModel.selectedVault.collectAsState()
    val questionText by viewModel.questionText.collectAsState()
    val options by viewModel.options.collectAsState()
    val selectedOption by viewModel.selectedOption.collectAsState()
    val explanation by viewModel.explanation.collectAsState()
    val imageUri by viewModel.imageUri.collectAsState()
    val diagramUri by viewModel.diagramUri.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    
    var showVaultDialog by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }
    var showExplanationDialog by remember { mutableStateOf(false) }
    
    // Camera and gallery launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri?.let { uri ->
                viewModel.processImage(context, uri)
            }
        }
    }

    val requestCameraPermission = PermissionUtils.rememberCameraPermission {
        val uri = createImageUri(context)
        viewModel.setImageUri(uri)
        cameraLauncher.launch(uri)
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            viewModel.setImageUri(it)
            viewModel.processImage(context, it)
        }
    }
    
    // Cropper launcher for diagram cropping
    val cropperLauncher = rememberLauncherForActivityResult(
        CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                viewModel.onCropResult(uri)
            }
        } else {
            viewModel.cancelCropping()
        }
    }
    
    val isCropping by viewModel.isCropping.collectAsState()
    val cropperImageUri by viewModel.cropperImageUri.collectAsState()
    val ocrResult by viewModel.ocrResult.collectAsState()
    
    // Handle image processing
    LaunchedEffect(imageUri) {
        imageUri?.let { uri ->
            viewModel.processImage(context, uri)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (questionId == null) "Add Question" 
                        else "Edit Question",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    } else {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.saveQuestion()
                                    onBack()
                                }
                            },
                            enabled = viewModel.canSave()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Vault Selection
            VaultSelector(
                selectedVault = selectedVault,
                vaults = vaults,
                onClick = { showVaultDialog = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Question Input Section
            QuestionInputSection(
                questionText = questionText,
                onTextChange = viewModel::setQuestionText,
                onPasteClick = { 
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    val clip = clipboard?.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val text = clip.getItemAt(0).text.toString()
                        viewModel.setQuestionText(text)
                    }
                },
                onImageClick = { showImageSourceDialog = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Image Preview
            if (imageUri != null) {
                ImagePreviewCard(
                    imageUri = imageUri!!,
                    onCropClick = {
                        // Launch cropper for diagram extraction
                        val cropOptions = CropImageContractOptions(
                            uri = imageUri!!,
                            cropImageOptions = CropImageOptions(
                                guidelines = CropImageView.Guidelines.ON,
                                aspectRatioX = 1,
                                aspectRatioY = 1,
                                outputCompressFormat = Bitmap.CompressFormat.JPEG,
                                outputCompressQuality = 90,
                                fixAspectRatio = false,
                                cropShape = CropImageView.CropShape.RECTANGLE
                            )
                        )
                        cropperLauncher.launch(cropOptions)
                    },
                    onRemoveClick = { viewModel.setImageUri(null) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Diagram from cropper
            if (diagramUri != null) {
                DiagramPreviewCard(
                    diagramUri = diagramUri!!,
                    onRemoveClick = { viewModel.setDiagramUri(null) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // OCR Result
            if (ocrResult != null) {
                OcrResultCard(
                    ocrResult = ocrResult!!,
                    onUseQuestionClick = { viewModel.setQuestionText(it) },
                    onUseOptionsClick = { options ->
                        viewModel.setOptions(options)
                    },
                    onClearClick = { 
                        viewModel.clearOcrText()
                        viewModel.setOcrResult(null)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else if (viewModel.ocrText.value.isNotEmpty()) {
                OcrResultCard(
                    ocrText = viewModel.ocrText.value,
                    onUseClick = { viewModel.setQuestionText(it) },
                    onClearClick = { viewModel.clearOcrText() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Options Section
            OptionsSection(
                options = options,
                selectedOption = selectedOption,
                onAddOption = viewModel::addOption,
                onRemoveOption = viewModel::removeOption,
                onOptionChange = viewModel::updateOption,
                onSelectOption = viewModel::setSelectedOption
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Diagram Section
            if (diagramUri == null && imageUri != null) {
                DiagramSection(
                    diagramUri = null,
                    imageUri = imageUri,
                    onCropFromImage = {
                        val cropOptions = CropImageContractOptions(
                            uri = imageUri!!,
                            cropImageOptions = CropImageOptions(
                                guidelines = CropImageView.Guidelines.ON,
                                aspectRatioX = 1,
                                aspectRatioY = 1,
                                outputCompressFormat = Bitmap.CompressFormat.JPEG,
                                outputCompressQuality = 90,
                                fixAspectRatio = false,
                                cropShape = CropImageView.CropShape.RECTANGLE,
                                activityTitle = "Crop Diagram"
                            )
                        )
                        cropperLauncher.launch(cropOptions)
                    },
                    onRemoveDiagram = { viewModel.setDiagramUri(null) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Explanation Section
            ExplanationSection(
                explanation = explanation,
                onExplanationChange = viewModel::setExplanation,
                onFetchClick = { showExplanationDialog = true }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Save Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.saveQuestion()
                        onBack()
                    }
                },
                enabled = viewModel.canSave() && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Question", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        
        // Vault Selection Dialog
        if (showVaultDialog) {
            VaultSelectionDialog(
                vaults = vaults,
                selectedVault = selectedVault,
                onSelect = { vault ->
                    viewModel.setSelectedVault(vault)
                    showVaultDialog = false
                },
                onDismiss = { showVaultDialog = false }
            )
        }
        
        // Image Source Dialog
        if (showImageSourceDialog) {
            ImageSourceDialog(
                onCameraClick = {
                    showImageSourceDialog = false
                    requestCameraPermission()
                },
                onGalleryClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch("image/*")
                },
                onDismiss = { showImageSourceDialog = false }
            )
        }
        
        // Explanation Fetch Dialog
        if (showExplanationDialog) {
            ExplanationFetchDialog(
                questionText = questionText,
                onFetch = { query ->
                    viewModel.fetchExplanation(query)
                    showExplanationDialog = false
                },
                onDismiss = { showExplanationDialog = false }
            )
        }
    }
}

@Composable
fun VaultSelector(
    selectedVault: com.example.questionbank.model.Vault?,
    vaults: List<com.example.questionbank.model.Vault>,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Vault",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedVault != null) {
                    Card(
                        modifier = Modifier.size(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(selectedVault.color))
                    ) {
                        Text(
                            text = selectedVault.icon.ifEmpty { selectedVault.name.take(1) },
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = selectedVault.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = "Select a vault",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select")
            }
        }
    }
}

@Composable
fun QuestionInputSection(
    questionText: String,
    onTextChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onImageClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Question",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onPasteClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                Spacer(modifier = Modifier.size(4.dp))
                Text("Paste")
            }
            OutlinedButton(
                onClick = onImageClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Image")
                Spacer(modifier = Modifier.size(4.dp))
                Text("Image")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = questionText,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter question text or use OCR from image") },
            minLines = 3,
            maxLines = 10
        )
    }
}

@Composable
fun ImagePreviewCard(
    imageUri: Uri,
    onCropClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Question Image",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val context = LocalContext.current

                val bitmap = remember(imageUri, context) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(
                                    context.contentResolver,
                                    imageUri!!
                                )
                            )
                        } else {
                            MediaStore.Images.Media.getBitmap(
                                context.contentResolver,
                                imageUri!!
                            )
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                
                bitmap?.let { bmp ->
                    androidx.compose.foundation.Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Question Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onCropClick,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Crop")
                    }
                    IconButton(
                        onClick = onRemoveClick,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(4.dp))
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap crop to extract diagram from this image",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun OcrResultCard(
    ocrResult: com.example.questionbank.utils.OcrUtils.OcrResult,
    onUseQuestionClick: (String) -> Unit,
    onUseOptionsClick: (List<String>) -> Unit,
    onClearClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
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
                Text(
                    text = "OCR Result",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClearClick) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
            
            // Question text
            if (ocrResult.questionText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Question:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = ocrResult.questionText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Button(
                    onClick = { onUseQuestionClick(ocrResult.questionText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Use Question Text")
                }
            }
            
            // Options
            if (ocrResult.options.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Detected Options:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ocrResult.options.forEachIndexed { index, option ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                Button(
                    onClick = { onUseOptionsClick(ocrResult.options) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Use Options")
                }
            }
            
            // Diagram hint
            if (ocrResult.hasDiagram) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Diagram detected",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Diagram detected in image. Use the crop tool to extract it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OcrResultCard(
    ocrText: String,
    onUseClick: (String) -> Unit,
    onClearClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
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
                Text(
                    text = "OCR Result",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { onUseClick(ocrText) }) {
                    Text("Use")
                }
                IconButton(onClick = onClearClick) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = ocrText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun DiagramPreviewCard(
    diagramUri: Uri,
    onRemoveClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Diagram",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val context = LocalContext.current

                val bitmap = remember(diagramUri, context) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(
                                    context.contentResolver,
                                    diagramUri!!
                                )
                            )
                        } else {
                            MediaStore.Images.Media.getBitmap(
                                context.contentResolver,
                                diagramUri!!
                            )
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                
                bitmap?.let { bmp ->
                    androidx.compose.foundation.Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Diagram",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(4.dp))
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

@Composable
fun OptionsSection(
    options: List<String>,
    selectedOption: Int?,
    onAddOption: () -> Unit,
    onRemoveOption: (Int) -> Unit,
    onOptionChange: (Int, String) -> Unit,
    onSelectOption: (Int?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Options",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            FilledTonalButton(
                onClick = onAddOption,
                enabled = options.size < 4
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.size(4.dp))
                Text("Add")
            }
        }
        
        if (options.isEmpty()) {
            Text(
                text = "No options added. Tap + to add options.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEachIndexed { index, option ->
                    OptionItem(
                        index = index,
                        option = option,
                        isSelected = selectedOption == index,
                        onOptionChange = { onOptionChange(index, it) },
                        onRemove = { onRemoveOption(index) },
                        onSelect = { 
                            if (selectedOption == index) {
                                onSelectOption(null)
                            } else {
                                onSelectOption(index)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OptionItem(
    index: Int,
    option: String,
    isSelected: Boolean,
    onOptionChange: (String) -> Unit,
    onRemove: () -> Unit,
    onSelect: () -> Unit
) {
    val letter = ('A' + index).toString()
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = letter,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            BasicTextField(
                value = option,
                onValueChange = onOptionChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}

@Composable
fun DiagramSection(
    diagramUri: Uri?,
    imageUri: Uri?,
    onCropFromImage: () -> Unit,
    onRemoveDiagram: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Diagram",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        if (diagramUri != null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val context = LocalContext.current

                        val bitmap = remember(diagramUri, context) {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    ImageDecoder.decodeBitmap(
                                        ImageDecoder.createSource(
                                            context.contentResolver,
                                            diagramUri!!
                                        )
                                    )
                                } else {
                                    MediaStore.Images.Media.getBitmap(
                                        context.contentResolver,
                                        diagramUri!!
                                    )
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        bitmap?.let { bmp ->
                            androidx.compose.foundation.Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Diagram",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        IconButton(
                            onClick = onRemoveDiagram,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(4.dp))
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        } else if (imageUri != null) {
            OutlinedButton(
                onClick = onCropFromImage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Crop")
                Spacer(modifier = Modifier.size(8.dp))
                Text("Crop Diagram from Question Image")
            }
        } else {
            Text(
                text = "Add a question image first, then crop to extract diagram",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ExplanationSection(
    explanation: String,
    onExplanationChange: (String) -> Unit,
    onFetchClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Explanation",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = onFetchClick) {
                Icon(Icons.Default.Info, contentDescription = "Fetch")
                Spacer(modifier = Modifier.size(4.dp))
                Text("Fetch")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = explanation,
            onValueChange = onExplanationChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Add explanation or fetch from AI") },
            minLines = 3,
            maxLines = 8
        )
    }
}

@Composable
fun VaultSelectionDialog(
    vaults: List<com.example.questionbank.model.Vault>,
    selectedVault: com.example.questionbank.model.Vault?,
    onSelect: (com.example.questionbank.model.Vault) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Vault") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(vaults) { vault ->
                    VaultDialogItem(
                        vault = vault,
                        isSelected = vault.id == selectedVault?.id,
                        onClick = { onSelect(vault) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun VaultDialogItem(
    vault: com.example.questionbank.model.Vault,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(8.dp))
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
        Card(
            modifier = Modifier.size(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(vault.color))
        ) {
            Text(
                text = vault.icon.ifEmpty { vault.name.take(1) },
                modifier = Modifier.fillMaxSize().padding(4.dp),
                textAlign = TextAlign.Center,
                color = Color.White,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = vault.name,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ImageSourceDialog(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Image") },
        text = { Text("Choose image source") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        dismissButton = null
    )
    
    // Actually, let's use a better dialog with icons
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Question Image") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select image source for OCR:")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onCameraClick)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Camera", style = MaterialTheme.typography.labelLarge)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onGalleryClick)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Gallery", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExplanationFetchDialog(
    questionText: String,
    onFetch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf(questionText) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fetch Explanation") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Enter search query for explanation:")
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Question text or custom query") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onFetch(query) },
                enabled = query.isNotEmpty()
            ) {
                Text("Fetch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
fun createImageUri(context: Context): Uri {
    val file = File.createTempFile(
        "question_bank_${System.currentTimeMillis()}",
        ".jpg",
        context.cacheDir
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
