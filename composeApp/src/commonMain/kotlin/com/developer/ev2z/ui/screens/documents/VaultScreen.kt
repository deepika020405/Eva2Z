//package com.developer.ev2z.ui.screens.documents
//
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material3.FloatingActionButton
//import androidx.compose.material3.Icon
//import androidx.compose.material3.Scaffold
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import com.developer.ev2z.data.local.model.VaultDocument
//import com.developer.ev2z.ui.components.DocumentCard
//import com.developer.ev2z.ui.components.VaultHeader
//
//@Composable
//fun VaultScreen(
//    viewModel: DocumentsViewModel,
//    onUpload: () -> Unit,
//    onView: (VaultDocument) -> Unit
//) {
//
//    val documents by viewModel.documents
//
//    LaunchedEffect(Unit) {
//        viewModel.loadDocuments()
//    }
//
//    Scaffold(
//
//        floatingActionButton = {
//
//            FloatingActionButton(
//                onClick = onUpload
//            ) {
//                Icon(Icons.Default.Add, null)
//            }
//
//        }
//
//    ) { padding ->
//
//        LazyColumn(
//            modifier = Modifier
//                .padding(padding)
//                .padding(16.dp)
//        ) {
//
//            item {
//                VaultHeader()
//                Spacer(Modifier.height(16.dp))
//            }
//
//            items(documents) { doc ->
//
//                DocumentCard(
//                    doc = doc,
//                    onView = { onView(doc) },
//                    onDelete = {
//                        viewModel.deleteDocument(doc.id)
//                    }
//                )
//
//            }
//
//        }
//
//    }
//
//}