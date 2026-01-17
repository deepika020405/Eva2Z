package com.developer.ev2z.ui.screens.documents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.util.PlatformDocumentViewer
import com.developer.ev2z.util.compressStoryImage
import com.developer.ev2z.util.rememberImagePicker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    onBack: () -> Unit,
    onOpenViewer: (ByteArray, String) -> Unit, // 👈 ADD THIS
    onOpenUpload: () -> Unit
) {
    // 🔙 System back handling
    PlatformBackHandler(enabled = true) {
        onBack()
    }

    val viewModel = remember {
        DocumentsViewModel(
            apiService = apiService,
            storageManager = storageManager
        )
    }

    val documents by viewModel.documents
    val loading by viewModel.loading
    val error by viewModel.error
    var deleteDocId by remember { mutableStateOf<String?>(null) }


    // 🚀 Load documents once
    LaunchedEffect(Unit) {
        viewModel.loadDocuments()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Documents") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // open upload screen
                        onOpenUpload()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Document")
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {
                    Text(
                        text = error ?: "Something went wrong",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                documents.isEmpty() -> {
                    Text(
                        text = "No documents found",
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(documents) { doc ->
                            DocumentItem(
                                fileName = doc.fileName,
                                fileType = doc.fileType,
                                onClick = {
                                    viewModel.openDocument(
                                        documentId = doc.id,
                                        onResult = { bytes ->
                                            onOpenViewer(bytes, doc.fileName)
                                        }
                                    )
                                },
                                onDelete = {deleteDocId = doc.id}
                            )
                        }
                    }
                }

            }
            if (deleteDocId != null) {
                AlertDialog(
                    onDismissRequest = { deleteDocId = null },
                    title = { Text("Delete Document") },
                    text = { Text("Are you sure you want to delete this document?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteDocument(deleteDocId!!)
                                deleteDocId = null
                            }
                        ) {
                            Text("Yes", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteDocId = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    fileName: String,
    bytes: ByteArray,
    onBack: () -> Unit,
    onDownload: () -> Unit
) {
    // ✅ SYSTEM BACK HANDLING
    PlatformBackHandler(enabled = true) {
        onBack()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = { Text(fileName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onDownload()

                            // ✅ SHOW SNACKBAR
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Download successful. Please check in Downloads Folder",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Default.Download, null)
                    }
                }
            )
        }
    ) { padding ->
        PlatformDocumentViewer(
            bytes = bytes,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}






@Composable
private fun DocumentItem(
    fileName: String,
    fileType: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(fileName, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    fileType,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDocumentScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    onBack: () -> Unit
) {
    // ✅ SYSTEM BACK HANDLING
    PlatformBackHandler(enabled = true) {
        onBack()
    }

    val viewModel = remember {
        DocumentsViewModel(
            apiService = apiService,
            storageManager = storageManager
        )
    }

    var fileName by remember { mutableStateOf("") }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberImagePicker { bytes ->
        if (bytes != null) {
            imageBytes = compressStoryImage(
                imageBytes = bytes,
                maxSizeKB = 800
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Document") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    TextButton(
                        enabled = fileName.isNotBlank() && imageBytes != null,
                        onClick = {
                            scope.launch {
                                viewModel.uploadDocument(
                                    fileName = fileName,
                                    imageBytes = imageBytes!!
                                )
                                onBack()
                            }
                        }
                    ) {
                        Text("Upload")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text("File name") },
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE0E0E0))
                    .clickable { imagePickerLauncher.launch() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (imageBytes == null)
                        "Tap to select image"
                    else
                        "Image selected ✅"
                )
            }
        }
    }
}