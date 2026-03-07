//package com.developer.ev2z.ui.screens.documents
//
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Button
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.material3.TopAppBar
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//
//
//
//@Composable
//fun UploadDocumentScreen(
//    viewModel: DocumentsViewModel,
//    onBack: () -> Unit
//) {
//
//    var fileName by remember { mutableStateOf("") }
//    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
//
//    Scaffold(
//
//        topBar = {
//
//            TopAppBar(
//                title = { Text("Upload Document") }
//            )
//
//        }
//
//    ) { padding ->
//
//        Column(
//            modifier = Modifier
//                .padding(padding)
//                .padding(16.dp)
//        ) {
//
//            OutlinedTextField(
//                value = fileName,
//                onValueChange = { fileName = it },
//                label = { Text("File Name") }
//            )
//
//            Spacer(Modifier.height(20.dp))
//
//            Button(
//                onClick = {
//
//                    imageBytes?.let {
//                        viewModel.uploadDocument(fileName, it)
//                        onBack()
//                    }
//
//                }
//            ) {
//                Text("Upload & Scan Document")
//            }
//
//        }
//
//    }
//
//}