package com.developer.ev2z.ui.screens.documents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    onBack: () -> Unit
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
                                        onResult = { decryptedBytes ->
                                            // ✅ YAHAN FILE OPEN KAROGE
                                            println("✅ Document decrypted, size = ${decryptedBytes.size}")

                                            // Android:
                                            // 1. temp file banao
                                            // 2. PDF/Image viewer se open karo

                                            // iOS:
                                            // UIDocumentInteractionController
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

//@Composable
//private fun DocumentItem(
//    fileName: String,
//    fileType: String
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(bottom = 12.dp)
//            .clickable {
//                // 🔜 TODO: Open / Download document
//            },
//        elevation = CardDefaults.cardElevation(2.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                imageVector = Icons.Default.Description,
//                contentDescription = null,
//                modifier = Modifier.size(28.dp)
//            )
//
//            Spacer(modifier = Modifier.width(12.dp))
//
//            Column {
//                Text(
//                    text = fileName,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Medium
//                )
//                Text(
//                    text = fileType,
//                    fontSize = 12.sp,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        }
//    }
//}

@Composable
private fun DocumentItem(
    fileName: String,
    fileType: String,
    onClick: () -> Unit
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

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = fileName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = fileType,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}