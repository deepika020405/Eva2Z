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
import androidx.compose.material.icons.filled.MoreVert
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
import com.developer.ev2z.response.VaultDocumentDto
import com.developer.ev2z.util.PlatformDocumentViewer
import com.developer.ev2z.util.compressStoryImage
import com.developer.ev2z.util.rememberImagePicker
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

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
    var editDocument by remember { mutableStateOf<VaultDocumentDto?>(null) }

    // 🚀 Load documents once
    LaunchedEffect(Unit) {
        viewModel.loadDocuments()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("E-Vault Documents") },
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "No E-Vault documents found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Add your first E-Vault document to get started",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onOpenUpload() }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add E-Vault Document")
                        }
                    }
                }


                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(documents) { doc ->
                            DocumentItem(
                                fileName = doc.fileName,
                                fileType = "Added on ${formatIsoDateToReadable(doc.createdAt)}",
                                onView = {
                                    viewModel.openDocument(
                                        documentId = doc.id,
                                        onResult = { bytes ->
                                            onOpenViewer(bytes, doc.fileName)
                                        }
                                    )
                                },
                                onDelete = {
                                    deleteDocId = doc.id
                                },
                                onRename = {
                                    editDocument = doc   // 👈 EDIT MODE
                                }
                            )
                        }
                    }
                }

            }
            if (deleteDocId != null) {
                AlertDialog(
                    onDismissRequest = { deleteDocId = null },
                    title = { Text("Delete E-Vault Document") },
                    text = { Text("Are you sure you want to delete this E-Vault document?") },
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

    if (editDocument != null) {
        UploadDocumentScreen(
            apiService = apiService,
            storageManager = storageManager,
            editDocument = editDocument,   // 👈 PASS DOC
            onBack = {
                editDocument = null
                viewModel.loadDocuments()
            }
        )
    }
}

@OptIn(ExperimentalTime::class)
fun formatIsoDateToReadable(dateString: String?): String {
    if (dateString.isNullOrBlank()) return ""

    return try {
        val instant = Instant.parse(dateString)
        val date = instant
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

        val month = when (date.monthNumber) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> ""
        }

        "${date.dayOfMonth}-$month-${date.year}"
    } catch (e: Exception) {
        ""
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
    onView: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp) // ⬅ spacing thoda kam
            .clickable { onView() },
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp), // ⬅ compact
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = fileType,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 🔘 3 DOT MENU
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape = RoundedCornerShape(14.dp), // ✅ CURVE
                    modifier = Modifier
                        .background(Color.White)
                ) {

                    CompactMenuItem(
                        text = "View",
                        onClick = {
                            menuExpanded = false
                            onView()
                        }
                    )

                    CompactMenuItem(
                        text = "Edit",
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )

                    CompactMenuItem(
                        text = "Delete",
                        textColor = Color(0xFFD32F2F),
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun CompactMenuItem(
    text: String,
    textColor: Color = Color.Black,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                fontSize = 14.sp,
                color = textColor
            )
        },
        onClick = onClick,
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 4.dp // ⬅ spacing kam
        )
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadDocumentScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    editDocument: VaultDocumentDto? = null,
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

//    var fileName by remember { mutableStateOf("") }
//    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isUploading by remember { mutableStateOf(false) }
    var fileNameError by remember { mutableStateOf(false) }
    val isEditMode = editDocument != null

    var fileName by remember {
        mutableStateOf(editDocument?.fileName ?: "")
    }

    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }



    val imagePickerLauncher = rememberImagePicker { bytes ->
        if (bytes != null) {
            imageBytes = compressStoryImage(
                imageBytes = bytes,
                maxSizeKB = 800
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode)
                            "Edit E-Vault Document"
                        else
                            "Add E-Vault Document"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    TextButton(
                        enabled = !isUploading && fileName.isNotBlank(),
                        onClick = {
                            scope.launch {

                                // 🔎 validation
                                if (fileName.isBlank()) {
                                    fileNameError = true
                                    return@launch
                                }

                                // ❗ ADD mode me image compulsory, EDIT me optional
                                if (!isEditMode && imageBytes == null) {
                                    snackbarHostState.showSnackbar("Please select an image")
                                    return@launch
                                }

                                try {
                                    isUploading = true

                                    if (isEditMode) {
                                        // 🔁 UPDATE FLOW
                                        viewModel.updateDocument(
                                            documentId = editDocument!!.id,
                                            fileName = fileName,
                                            imageBytes = imageBytes // nullable
                                        )

                                        snackbarHostState.showSnackbar(
                                            "E-Vault Document updated successfully ✅"
                                        )
                                    } else {
                                        // ➕ ADD FLOW
                                        viewModel.uploadDocument(
                                            fileName = fileName,
                                            imageBytes = imageBytes!!
                                        )

                                        snackbarHostState.showSnackbar(
                                            "E-Vault Document uploaded successfully ✅"
                                        )
                                    }

                                    onBack() // success → exit screen

                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(
                                        "Operation failed. Please try again"
                                    )
                                    isUploading = false
                                }
                            }
                        }
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (isEditMode) "Update" else "Upload")
                        }
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

            var fileNameError by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = fileName,
                onValueChange = {
                    fileName = it
                    if (it.isNotBlank()) fileNameError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("File Name *")
                },
                isError = fileNameError,
                supportingText = {
                    if (fileNameError) {
                        Text(
                            text = "File name is required",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                singleLine = true
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