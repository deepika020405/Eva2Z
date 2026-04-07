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
                                            "$fileName updated successfully ✅"
                                        )
                                    } else {
                                        // ➕ ADD FLOW
                                        viewModel.uploadDocument(
                                            fileName = fileName,
                                            imageBytes = imageBytes!!
                                        )

                                        snackbarHostState.showSnackbar(
                                            "$fileName uploaded successfully ✅"
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

// ══════════════════════════════════════════════════════════════════
// COMMENTED OUT: DUMMY UI CODE (for reference)
// ══════════════════════════════════════════════════════════════════

// 
// import androidx.compose.animation.*
// import androidx.compose.animation.core.*
// import androidx.compose.foundation.*
// import androidx.compose.foundation.layout.*
// import androidx.compose.foundation.lazy.LazyColumn
// import androidx.compose.foundation.lazy.items
// import androidx.compose.foundation.shape.CircleShape
// import androidx.compose.foundation.shape.RoundedCornerShape
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.automirrored.filled.ArrowBack
// import androidx.compose.material.icons.filled.*
// import androidx.compose.material.icons.outlined.*
// import androidx.compose.material3.*
// import androidx.compose.runtime.*
// import androidx.compose.ui.Alignment
// import androidx.compose.ui.Modifier
// import androidx.compose.ui.draw.clip
// import androidx.compose.ui.draw.shadow
// import androidx.compose.ui.graphics.Brush
// import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.graphics.vector.ImageVector
// import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.text.style.TextAlign
// import androidx.compose.ui.text.style.TextOverflow
// import androidx.compose.ui.unit.dp
// import androidx.compose.ui.unit.sp
// import androidx.compose.ui.window.Dialog
// import androidx.compose.ui.window.DialogProperties
// import com.developer.ev2z.navigation.PlatformBackHandler
// 
// // ─────────────────────────────────────────────
// //  COLOR TOKENS
// // ─────────────────────────────────────────────
// 
// private object VC {
//     val Primary        = Color(0xFF4CAF50)
//     val PrimaryDark    = Color(0xFF388E3C)
//     val PrimaryLight   = Color(0xFFE8F5E9)
//     val Surface        = Color(0xFFFFFFFF)
//     val Background     = Color(0xFFF6F6F6)
//     val TextPrimary    = Color(0xFF1E1E1E)
//     val TextSecondary  = Color(0xFF777777)
//     val TextHint       = Color(0xFFAAAAAA)
//     val Green          = Color(0xFF4CAF50)
//     val GreenBg        = Color(0xFFE8F5E9)
//     val Orange         = Color(0xFFFF9800)
//     val OrangeBg       = Color(0xFFFFF3E0)
//     val Red            = Color(0xFFE53935)
//     val RedBg          = Color(0xFFFFEBEE)
//     val Divider        = Color(0xFFEEEEEE)
//     val CardShadow     = Color(0x0D4CAF50)
//     val IconBg         = Color(0xFFE8F5E9)
// }
// 
// // ─────────────────────────────────────────────
// //  DUMMY DATA MODELS
// // ─────────────────────────────────────────────
// 
// private enum class DocStatus { ACTIVE, EXPIRING_SOON, EXPIRED }
// 
// private data class DocItem(
//     val id: String,
//     val title: String,
//     val docNumber: String,
//     val expiryDate: String,
//     val status: DocStatus,
//     val iconVector: ImageVector,
//     val iconTint: Color,
//     val iconBg: Color
// )
// 
// private val DUMMY_DOCS = listOf(
//     DocItem(
//         id = "1",
//         title = "Registration (RC)",
//         docNumber = "DL-DC-3814",
//         expiryDate = "Expires: 12 Oct 2026",
//         status = DocStatus.ACTIVE,
//         iconVector = Icons.Outlined.Article,
//         iconTint = Color(0xFF4CAF50),
//         iconBg = Color(0xFFE8F5E9)
//     ),
//     DocItem(
//         id = "2",
//         title = "Insurance",
//         docNumber = "Policy No: HDFC 9901",
//         expiryDate = "Expires: 24 Nov 2023",
//         status = DocStatus.EXPIRING_SOON,
//         iconVector = Icons.Outlined.Shield,
//         iconTint = Color(0xFF388E3C),
//         iconBg = Color(0xFFE8F5E9)
//     ),
//     DocItem(
//         id = "3",
//         title = "Driving License",
//         docNumber = "DL-120039/2023",
//         expiryDate = "Expires: 15 Jan 2030",
//         status = DocStatus.ACTIVE,
//         iconVector = Icons.Outlined.Badge,
//         iconTint = Color(0xFF00897B),
//         iconBg = Color(0xFFE0F2F1)
//     ),
//     DocItem(
//         id = "4",
//         title = "PUC Certificate",
//         docNumber = "Valid 18 months",
//         expiryDate = "Expires: 04 Feb 2024",
//         status = DocStatus.EXPIRED,
//         iconVector = Icons.Outlined.Eco,
//         iconTint = Color(0xFF43A047),
//         iconBg = Color(0xFFE8F5E9)
//     )
// )
// 
// // ─────────────────────────────────────────────
// //  SCREEN ENTRY POINT
// // ─────────────────────────────────────────────
// 
// @Composable
// fun DocumentsScreen(
//     onBack: () -> Unit = {},
//     onAddDocument: () -> Unit = {}
// ) {
//     var documents by remember { mutableStateOf(DUMMY_DOCS) }
//     var selectedDoc by remember { mutableStateOf<DocItem?>(null) }
//     var docToDelete by remember { mutableStateOf<DocItem?>(null) }
//     var showAddSheet by remember { mutableStateOf(false) }
// 
//     Box(
//         modifier = Modifier
//             .fillMaxSize()
//             .background(VC.Background)
//     ) {
//         Scaffold(
//             containerColor = VC.Background,
//             topBar = { VaultTopBar(onBack = onBack) }
//         ) { padding ->
//             if (documents.isEmpty()) {
//                 EmptyState(
//                     modifier = Modifier
//                         .fillMaxSize()
//                         .padding(padding),
//                     onAdd = { showAddSheet = true }
//                 )
//             } else {
//                 LazyColumn(
//                     modifier = Modifier
//                         .fillMaxSize()
//                         .padding(padding),
//                     contentPadding = PaddingValues(
//                         start = 16.dp,
//                         end = 16.dp,
//                         top = 12.dp,
//                         bottom = 88.dp
//                     ),
//                     verticalArrangement = Arrangement.spacedBy(12.dp)
//                 ) {
//                     item { SecureStorageBanner() }
// 
//                     items(documents, key = { it.id }) { doc ->
//                         AnimatedVisibility(
//                             visible = true,
//                             enter = fadeIn() + slideInVertically()
//                         ) {
//                             DocumentCard(
//                                 doc = doc,
//                                 onView    = { selectedDoc = doc },
//                                 onReplace = { selectedDoc = doc },
//                                 onSave    = { /* save/download action */ }
//                             )
//                         }
//                     }
// 
//                     item {
//                         Spacer(Modifier.height(4.dp))
//                         AddDocumentButton(onClick = { showAddSheet = true })
//                     }
//                 }
//             }
//         }
// 
//         // ── Document Detail Sheet ────────────────────────────────────────
//         selectedDoc?.let { doc ->
//             DocumentDetailSheet(
//                 doc       = doc,
//                 onDismiss = { selectedDoc = null },
//                 onView    = { selectedDoc = null },
//                 onEdit    = { selectedDoc = null; showAddSheet = true },
//                 onDelete  = { selectedDoc = null; docToDelete = doc }
//             )
//         }
// 
//         // ── Delete Confirmation ──────────────────────────────────────────
//         docToDelete?.let { doc ->
//             AlertDialog(
//                 onDismissRequest = { docToDelete = null },
//                 shape = RoundedCornerShape(20.dp),
//                 icon = {
//                     Box(
//                         modifier = Modifier
//                             .size(48.dp)
//                             .background(VC.RedBg, CircleShape),
//                         contentAlignment = Alignment.Center
//                     ) {
//                         Icon(
//                             Icons.Outlined.Delete,
//                             contentDescription = null,
//                             tint = VC.Red,
//                             modifier = Modifier.size(24.dp)
//                         )
//                     }
//                 },
//                 title = {
//                     Text(
//                         "Delete Document",
//                         fontWeight = FontWeight.SemiBold,
//                         color = VC.TextPrimary,
//                         textAlign = TextAlign.Center
//                     )
//                 },
//                 text = {
//                     Text(
//                         "Are you sure you want to delete \"${doc.title}\"?\nThis action cannot be undone.",
//                         color = VC.TextSecondary,
//                         fontSize = 14.sp,
//                         textAlign = TextAlign.Center,
//                         lineHeight = 20.sp
//                     )
//                 },
//                 confirmButton = {
//                     Button(
//                         onClick = {
//                             documents = documents.filter { it.id != doc.id }
//                             docToDelete = null
//                         },
//                         colors = ButtonDefaults.buttonColors(containerColor = VC.Red),
//                         shape = RoundedCornerShape(10.dp)
//                     ) { Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold) }
//                 },
//                 dismissButton = {
//                     OutlinedButton(
//                         onClick = { docToDelete = null },
//                         shape = RoundedCornerShape(10.dp),
//                         border = BorderStroke(1.dp, VC.Divider)
//                     ) { Text("Cancel", color = VC.TextSecondary) }
//                 }
//             )
//         }
// 
//         // ── Add Document Sheet ───────────────────────────────────────────
//         if (showAddSheet) {
//             AddDocumentSheet(
//                 onDismiss = { showAddSheet = false },
//                 onAdded   = { showAddSheet = false }
//             )
//         }
//     }
// }
// 
// // ─────────────────────────────────────────────
// //  TOP BAR
// // ─────────────────────────────────────────────
// 
// @Composable
// private fun VaultTopBar(onBack: () -> Unit) {
//     Surface(color = VC.Surface, shadowElevation = 1.dp) {
//         Row(
//             modifier = Modifier
//                 .fillMaxWidth()
//                 .windowInsetsPadding(WindowInsets.statusBars)
//                 .height(56.dp)
//                 .padding(horizontal = 4.dp),
//             verticalAlignment = Alignment.CenterVertically
//         ) {
//             IconButton(onClick = onBack) {
//                 Icon(
//                     Icons.AutoMirrored.Filled.ArrowBack,
//                     contentDescription = "Back",
//                     tint = VC.TextPrimary
//                 )
//             }
//             Text(
//                 text = "Document Vault",
//                 modifier = Modifier.weight(1f),
//                 textAlign = TextAlign.Center,
//                 style = MaterialTheme.typography.titleMedium.copy(
//                     fontSize = 17.sp,
//                     fontWeight = FontWeight.SemiBold,
//                     color = VC.TextPrimary
//                 )
//             )
//             BadgedBox(
//                 badge = {
//                     Badge(
//                         containerColor = VC.Red,
//                         modifier = Modifier.size(7.dp)
//                     ) {}
//                 }
//             ) {
//                 IconButton(onClick = {}) {
//                     Icon(
//                         Icons.Outlined.Notifications,
//                         contentDescription = "Alerts",
//                         tint = VC.TextPrimary
//                     )
//                 }
//             }
//         }
//     }
// }
// 
// // ─────────────────────────────────────────────
// //  SECURE STORAGE BANNER
// // ─────────────────────────────────────────────
// 
// @Composable
// private fun SecureStorageBanner() {
//     Box(
//         modifier = Modifier
//             .fillMaxWidth()
//             .clip(RoundedCornerShape(12.dp))
//             .background(
//                 Brush.horizontalGradient(
//                     listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
//                 )
//             )
//             .padding(14.dp)
//     ) {
//         Row(
//             verticalAlignment = Alignment.CenterVertically,
//             horizontalArrangement = Arrangement.spacedBy(12.dp)
//         ) {
//             Box(
//                 modifier = Modifier
//                     .size(36.dp)
//                     .background(VC.Primary.copy(alpha = 0.12f), CircleShape),
//                 contentAlignment = Alignment.Center
//             ) {
//                 Icon(
//                     Icons.Default.Lock,
//                     contentDescription = null,
//                     tint = VC.Primary,
//                     modifier = Modifier.size(18.dp)
//                 )
//             }
//             Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
//                 Text(
//                     "Secure Storage",
//                     style = MaterialTheme.typography.labelMedium.copy(
//                         fontWeight = FontWeight.Bold,
//                         color = VC.Primary,
//                         fontSize = 13.sp
//                     )
//                 )
//                 Text(
//                     "All documents are encrypted and verified for EVA2Z compliance.",
//                     style = MaterialTheme.typography.bodySmall.copy(
//                         color = VC.Primary.copy(alpha = 0.72f),
//                         fontSize = 11.5.sp,
//                         lineHeight = 15.sp
//                     )
//                 )
//             }
//         }
//     }
// }
// 
// // ─────────────────────────────────────────────
// //  DOCUMENT CARD
// // ─────────────────────────────────────────────
// 
// @Composable
// private fun DocumentCard(
//     doc: DocItem,
//     onView: () -> Unit,
//     onReplace: () -> Unit,
//     onSave: () -> Unit
// ) {
//     val (statusColor, statusBg, statusLabel) = when (doc.status) {
//         DocStatus.ACTIVE        -> Triple(VC.Green,  VC.GreenBg,  "ACTIVE")
//         DocStatus.EXPIRING_SOON -> Triple(VC.Orange, VC.OrangeBg, "EXPIRING SOON")
//         DocStatus.EXPIRED       -> Triple(VC.Red,    VC.RedBg,    "EXPIRED")
//     }
// 
//     Card(
//         modifier = Modifier
//             .fillMaxWidth()
//             .shadow(
//                 elevation = 3.dp,
//                 shape = RoundedCornerShape(14.dp),
//                 spotColor = VC.CardShadow,
//                 ambientColor = VC.CardShadow
//             ),
//         shape = RoundedCornerShape(14.dp),
//         colors = CardDefaults.cardColors(containerColor = VC.Surface),
//         elevation = CardDefaults.cardElevation(0.dp)
//     ) {
//         Column {
//             // ── Content row ──────────────────────────────────────────────
//             Row(
//                 modifier = Modifier
//                     .fillMaxWidth()
//                     .clickable { onView() }
//                     .padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
//                 verticalAlignment = Alignment.CenterVertically,
//                 horizontalArrangement = Arrangement.spacedBy(12.dp)
//             ) {
//                 // Icon
//                 Box(
//                     modifier = Modifier
//                         .size(46.dp)
//                         .background(doc.iconBg, RoundedCornerShape(10.dp)),
//                     contentAlignment = Alignment.Center
//                 ) {
//                     Icon(
//                         doc.iconVector,
//                         contentDescription = null,
//                         tint = doc.iconTint,
//                         modifier = Modifier.size(22.dp)
//                     )
//                 }
// 
//                 // Info
//                 Column(
//                     modifier = Modifier.weight(1f),
//                     verticalArrangement = Arrangement.spacedBy(4.dp)
//                 ) {
//                     Row(
//                         verticalAlignment = Alignment.CenterVertically,
//                         horizontalArrangement = Arrangement.spacedBy(8.dp)
//                     ) {
//                         Text(
//                             text = doc.title,
//                             style = MaterialTheme.typography.bodyMedium.copy(
//                                 fontWeight = FontWeight.SemiBold,
//                                 color = VC.TextPrimary,
//                                 fontSize = 14.sp
//                             ),
//                             maxLines = 1,
//                             overflow = TextOverflow.Ellipsis,
//                             modifier = Modifier.weight(1f, fill = false)
//                         )
//                         // Status badge
//                         Box(
//                             modifier = Modifier
//                                 .background(statusBg, RoundedCornerShape(4.dp))
//                                 .padding(horizontal = 6.dp, vertical = 2.dp)
//                         ) {
//                             Text(
//                                 text = statusLabel,
//                                 style = MaterialTheme.typography.labelSmall.copy(
//                                     color = statusColor,
//                                     fontWeight = FontWeight.ExtraBold,
//                                     fontSize = 9.sp,
//                                     letterSpacing = 0.3.sp
//                                 )
//                             )
//                         }
//                     }
//                     Text(
//                         doc.docNumber,
//                         style = MaterialTheme.typography.bodySmall.copy(
//                             color = VC.TextSecondary,
//                             fontSize = 12.sp
//                         )
//                     )
//                     Text(
//                         doc.expiryDate,
//                         style = MaterialTheme.typography.bodySmall.copy(
//                             color = if (doc.status != DocStatus.ACTIVE) VC.Red else VC.TextSecondary,
//                             fontSize = 11.5.sp,
//                             fontWeight = if (doc.status != DocStatus.ACTIVE) FontWeight.Medium else FontWeight.Normal
//                         )
//                     )
//                 }
// 
//                 // Chevron
//                 Icon(
//                     Icons.Default.ChevronRight,
//                     contentDescription = null,
//                     tint = VC.TextHint,
//                     modifier = Modifier.size(18.dp)
//                 )
//             }
// 
//             HorizontalDivider(
//                 color = VC.Divider,
//                 thickness = 1.dp,
//                 modifier = Modifier.padding(horizontal = 14.dp)
//             )
// 
//             // ── Action row ───────────────────────────────────────────────
//             Row(
//                 modifier = Modifier
//                     .fillMaxWidth()
//                     .padding(horizontal = 8.dp, vertical = 6.dp),
//                 horizontalArrangement = Arrangement.spacedBy(4.dp)
//             ) {
//                 CardActionBtn(
//                     icon  = Icons.Outlined.Visibility,
//                     label = "View",
//                     tint  = VC.Primary,
//                     onClick = onView,
//                     modifier = Modifier.weight(1f)
//                 )
//                 CardActionBtn(
//                     icon  = Icons.Outlined.Refresh,
//                     label = "Replace",
//                     tint  = VC.Primary,
//                     onClick = onReplace,
//                     modifier = Modifier.weight(1f)
//                 )
//                 CardActionBtn(
//                     icon  = Icons.Outlined.Download,
//                     label = "Save",
//                     tint  = VC.Primary,
//                     onClick = onSave,
//                     modifier = Modifier.weight(1f)
//                 )
//             }
//         }
//     }
// }
// 
// @Composable
// private fun CardActionBtn(
//     icon: ImageVector,
//     label: String,
//     tint: Color,
//     onClick: () -> Unit,
//     modifier: Modifier = Modifier
// ) {
//     TextButton(
//         onClick = onClick,
//         modifier = modifier.height(34.dp),
//         contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
//         colors = ButtonDefaults.textButtonColors(contentColor = tint)
//     ) {
//         Icon(icon, null, Modifier.size(15.dp), tint = tint)
//         Spacer(Modifier.width(4.dp))
//         Text(
//             label,
//             style = MaterialTheme.typography.labelSmall.copy(
//                 fontWeight = FontWeight.SemiBold,
//                 fontSize = 12.sp,
//                 color = tint
//             )
//         )
//     }
// }
// 
// // ─────────────────────────────────────────────
// //  ADD DOCUMENT BUTTON
// // ─────────────────────────────────────────────
// 
// @Composable
// private fun AddDocumentButton(onClick: () -> Unit) {
//     OutlinedButton(
//         onClick = onClick,
//         modifier = Modifier
//             .fillMaxWidth()
//             .height(50.dp),
//         shape = RoundedCornerShape(12.dp),
//         border = BorderStroke(1.5.dp, VC.Primary),
//         colors = ButtonDefaults.outlinedButtonColors(
//             containerColor = VC.PrimaryLight,
//             contentColor   = VC.Primary
//         )
//     ) {
//         Icon(Icons.Default.Add, null, Modifier.size(20.dp), tint = VC.Primary)
//         Spacer(Modifier.width(8.dp))
//         Text(
//             "Add New Document",
//             style = MaterialTheme.typography.labelLarge.copy(
//                 fontWeight = FontWeight.SemiBold,
//                 fontSize = 14.sp
//             )
//         )
//     }
// }
// 
// // ─────────────────────────────────────────────
// //  EMPTY STATE
// // ─────────────────────────────────────────────
// 
// @Composable
// private fun EmptyState(modifier: Modifier = Modifier, onAdd: () -> Unit) {
//     Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
//         Column(
//             horizontalAlignment = Alignment.CenterHorizontally,
//             verticalArrangement = Arrangement.spacedBy(12.dp)
//         ) {
//             Box(
//                 modifier = Modifier
//                     .size(80.dp)
//                     .background(VC.PrimaryLight, CircleShape),
//                 contentAlignment = Alignment.Center
//             ) {
//                 Icon(
//                     Icons.Outlined.FolderOpen,
//                     null,
//                     Modifier.size(38.dp),
//                     tint = VC.Primary
//                 )
//             }
//             Text(
//                 "No Documents Yet",
//                 style = MaterialTheme.typography.titleMedium.copy(
//                     fontWeight = FontWeight.SemiBold,
//                     color = VC.TextPrimary,
//                     fontSize = 17.sp
//                 )
//             )
//             Text(
//                 "Upload your vehicle documents to get started.\nAll files are encrypted and secure.",
//                 style = MaterialTheme.typography.bodyMedium.copy(
//                     color = VC.TextSecondary,
//                     lineHeight = 20.sp
//                 ),
//                 textAlign = TextAlign.Center
//             )
//             Spacer(Modifier.height(4.dp))
//             Button(
//                 onClick = onAdd,
//                 modifier = Modifier.height(46.dp),
//                 shape = RoundedCornerShape(12.dp),
//                 colors = ButtonDefaults.buttonColors(containerColor = VC.Primary)
//             ) {
//                 Icon(Icons.Default.Add, null, Modifier.size(18.dp))
//                 Spacer(Modifier.width(6.dp))
//                 Text("Add Document", fontWeight = FontWeight.SemiBold)
//             }
//         }
//     }
// }
// 
// // ─────────────────────────────────────────────
// //  DOCUMENT DETAIL SHEET
// // ─────────────────────────────────────────────
// 
// @Composable
// private fun DocumentDetailSheet(
//     doc: DocItem,
//     onDismiss: () -> Unit,
//     onView: () -> Unit,
//     onEdit: () -> Unit,
//     onDelete: () -> Unit
// ) {
//     Dialog(
//         onDismissRequest = onDismiss,
//         properties = DialogProperties(
//             usePlatformDefaultWidth = false,
//             dismissOnClickOutside = true
//         )
//     ) {
//         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
//             // Scrim
//             Box(
//                 modifier = Modifier
//                     .fillMaxSize()
//                     .background(Color.Black.copy(alpha = 0.45f))
//                     .clickable { onDismiss() }
//             )
// 
//             // Sheet
//             Surface(
//                 modifier = Modifier
//                     .fillMaxWidth()
//                     .fillMaxHeight(0.52f),
//                 shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
//                 color = VC.Surface
//             ) {
//                 Column(modifier = Modifier.fillMaxSize()) {
//                     // Handle
//                     Box(
//                         modifier = Modifier
//                             .fillMaxWidth()
//                             .padding(top = 10.dp, bottom = 4.dp),
//                         contentAlignment = Alignment.Center
//                     ) {
//                         Box(
//                             Modifier
//                                 .width(36.dp)
//                                 .height(4.dp)
//                                 .background(VC.Divider, RoundedCornerShape(2.dp))
//                         )
//                     }
// 
//                     // Header
//                     Row(
//                         modifier = Modifier
//                             .fillMaxWidth()
//                             .padding(horizontal = 16.dp, vertical = 10.dp),
//                         verticalAlignment = Alignment.CenterVertically,
//                         horizontalArrangement = Arrangement.spacedBy(12.dp)
//                     ) {
//                         Box(
//                             modifier = Modifier
//                                 .size(40.dp)
//                                 .background(doc.iconBg, RoundedCornerShape(10.dp)),
//                             contentAlignment = Alignment.Center
//                         ) {
//                             Icon(doc.iconVector, null, Modifier.size(20.dp), tint = doc.iconTint)
//                         }
//                         Column(modifier = Modifier.weight(1f)) {
//                             Text(
//                                 doc.title,
//                                 style = MaterialTheme.typography.titleSmall.copy(
//                                     fontWeight = FontWeight.SemiBold,
//                                     fontSize = 15.sp,
//                                     color = VC.TextPrimary
//                                 )
//                             )
//                             Text(
//                                 doc.docNumber,
//                                 style = MaterialTheme.typography.bodySmall.copy(
//                                     color = VC.TextSecondary,
//                                     fontSize = 11.5.sp
//                                 )
//                             )
//                         }
//                         IconButton(
//                             onClick = onDismiss,
//                             modifier = Modifier
//                                 .size(32.dp)
//                                 .background(VC.Background, CircleShape)
//                         ) {
//                             Icon(Icons.Default.Close, "Close", Modifier.size(16.dp), tint = VC.TextSecondary)
//                         }
//                     }
// 
//                     HorizontalDivider(color = VC.Divider)
// 
//                     // Info rows
//                     Column(
//                         modifier = Modifier
//                             .weight(1f)
//                             .padding(horizontal = 18.dp, vertical = 12.dp),
//                         verticalArrangement = Arrangement.spacedBy(10.dp)
//                     ) {
//                         SheetInfoRow("Status", doc.status.name.replace("_", " "))
//                         SheetInfoRow("Expiry", doc.expiryDate.removePrefix("Expires: "))
//                         SheetInfoRow("Document No.", doc.docNumber)
//                     }
// 
//                     HorizontalDivider(color = VC.Divider)
// 
//                     // Action icons row
//                     Row(
//                         modifier = Modifier
//                             .fillMaxWidth()
//                             .windowInsetsPadding(WindowInsets.navigationBars)
//                             .padding(horizontal = 16.dp, vertical = 12.dp),
//                         horizontalArrangement = Arrangement.SpaceAround
//                     ) {
//                         SheetIconAction(Icons.Outlined.Visibility, "View",    onClick = onView)
//                         SheetIconAction(Icons.Outlined.Edit,       "Edit",    onClick = onEdit)
//                         SheetIconAction(
//                             Icons.Outlined.Delete,
//                             "Delete",
//                             tint    = VC.Red,
//                             iconBg  = VC.RedBg,
//                             onClick = onDelete
//                         )
//                         SheetIconAction(Icons.Outlined.Download, "Save", onClick = {})
//                     }
//                 }
//             }
//         }
//     }
// }
// 
// @Composable
// private fun SheetInfoRow(label: String, value: String) {
//     Row(
//         modifier = Modifier.fillMaxWidth(),
//         horizontalArrangement = Arrangement.SpaceBetween,
//         verticalAlignment = Alignment.CenterVertically
//     ) {
//         Text(
//             label,
//             style = MaterialTheme.typography.bodySmall.copy(
//                 color = VC.TextSecondary,
//                 fontSize = 13.sp
//             )
//         )
//         Text(
//             value,
//             style = MaterialTheme.typography.bodySmall.copy(
//                 color = VC.TextPrimary,
//                 fontWeight = FontWeight.Medium,
//                 fontSize = 13.sp
//             )
//         )
//     }
// }
// 
// @Composable
// private fun SheetIconAction(
//     icon: ImageVector,
//     label: String,
//     tint: Color = VC.Primary,
//     iconBg: Color = VC.PrimaryLight,
//     onClick: () -> Unit
// ) {
//     Column(
//         horizontalAlignment = Alignment.CenterHorizontally,
//         verticalArrangement = Arrangement.spacedBy(5.dp),
//         modifier = Modifier
//             .clip(RoundedCornerShape(10.dp))
//             .clickable(onClick = onClick)
//             .padding(horizontal = 10.dp, vertical = 6.dp)
//     ) {
//         Box(
//             modifier = Modifier
//                 .size(42.dp)
//                 .background(iconBg, RoundedCornerShape(10.dp)),
//             contentAlignment = Alignment.Center
//         ) {
//             Icon(icon, label, Modifier.size(20.dp), tint = tint)
//         }
//         Text(
//             label,
//             style = MaterialTheme.typography.labelSmall.copy(
//                 fontSize = 11.sp,
//                 color = VC.TextSecondary,
//                 fontWeight = FontWeight.Medium
//             )
//         )
//     }
// }
// 
// // ─────────────────────────────────────────────
// //  ADD DOCUMENT BOTTOM SHEET
// // ─────────────────────────────────────────────
// 
// @Composable
// private fun AddDocumentSheet(
//     onDismiss: () -> Unit,
//     onAdded: () -> Unit
// ) {
//     var docName  by remember { mutableStateOf("") }
//     var docType  by remember { mutableStateOf("") }
//     var nameErr  by remember { mutableStateOf(false) }
//     var fileSelected by remember { mutableStateOf(false) }
// 
//     Dialog(
//         onDismissRequest = onDismiss,
//         properties = DialogProperties(
//             usePlatformDefaultWidth = false,
//             dismissOnClickOutside = true
//         )
//     ) {
//         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
//             Box(
//                 modifier = Modifier
//                     .fillMaxSize()
//                     .background(Color.Black.copy(alpha = 0.45f))
//                     .clickable { onDismiss() }
//             )
// 
//             Surface(
//                 modifier = Modifier
//                     .fillMaxWidth()
//                     .fillMaxHeight(0.7f),
//                 shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
//                 color = VC.Surface
//             ) {
//                 Column(
//                     modifier = Modifier
//                         .fillMaxSize()
//                         .padding(horizontal = 20.dp),
//                     verticalArrangement = Arrangement.spacedBy(14.dp)
//                 ) {
//                     // Handle
//                     Box(
//                         modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
//                         contentAlignment = Alignment.Center
//                     ) {
//                         Box(
//                             Modifier
//                                 .width(36.dp)
//                                 .height(4.dp)
//                                 .background(VC.Divider, RoundedCornerShape(2.dp))
//                         )
//                     }
// 
//                     Text(
//                         "Add New Document",
//                         style = MaterialTheme.typography.titleMedium.copy(
//                             fontWeight = FontWeight.SemiBold,
//                             fontSize = 16.sp,
//                             color = VC.TextPrimary
//                         )
//                     )
// 
//                     OutlinedTextField(
//                         value = docName,
//                         onValueChange = { docName = it; nameErr = false },
//                         label = { Text("Document Name *") },
//                         modifier = Modifier.fillMaxWidth(),
//                         isError = nameErr,
//                         singleLine = true,
//                         shape = RoundedCornerShape(12.dp),
//                         colors = OutlinedTextFieldDefaults.colors(
//                             focusedBorderColor = VC.Primary,
//                             cursorColor = VC.Primary
//                         ),
//                         supportingText = {
//                             if (nameErr) Text("Name is required", color = VC.Red, fontSize = 11.sp)
//                         }
//                     )
// 
//                     OutlinedTextField(
//                         value = docType,
//                         onValueChange = { docType = it },
//                         label = { Text("Document Type (RC, Insurance…)") },
//                         modifier = Modifier.fillMaxWidth(),
//                         singleLine = true,
//                         shape = RoundedCornerShape(12.dp),
//                         colors = OutlinedTextFieldDefaults.colors(
//                             focusedBorderColor = VC.Primary,
//                             cursorColor = VC.Primary
//                         )
//                     )
// 
//                     // File picker area
//                     Box(
//                         modifier = Modifier
//                             .fillMaxWidth()
//                             .height(110.dp)
//                             .clip(RoundedCornerShape(12.dp))
//                             .background(if (fileSelected) VC.GreenBg else VC.PrimaryLight)
//                             .border(
//                                 BorderStroke(
//                                     1.5.dp,
//                                     if (fileSelected) VC.Green else VC.Primary.copy(alpha = 0.35f)
//                                 ),
//                                 RoundedCornerShape(12.dp)
//                             )
//                             .clickable { fileSelected = !fileSelected },
//                         contentAlignment = Alignment.Center
//                     ) {
//                         Column(
//                             horizontalAlignment = Alignment.CenterHorizontally,
//                             verticalArrangement = Arrangement.spacedBy(6.dp)
//                         ) {
//                             Icon(
//                                 if (fileSelected) Icons.Default.CheckCircle else Icons.Outlined.CloudUpload,
//                                 null,
//                                 Modifier.size(28.dp),
//                                 tint = if (fileSelected) VC.Green else VC.Primary
//                             )
//                             Text(
//                                 if (fileSelected) "File selected" else "Tap to upload file",
//                                 fontSize = 13.sp,
//                                 fontWeight = FontWeight.Medium,
//                                 color = if (fileSelected) VC.Green else VC.Primary
//                             )
//                             if (!fileSelected) {
//                                 Text(
//                                     "JPG, PNG or PDF · Max 800KB",
//                                     fontSize = 11.sp,
//                                     color = VC.TextHint
//                                 )
//                             }
//                         }
//                     }
// 
//                     Spacer(Modifier.weight(1f))
// 
//                     Button(
//                         onClick = {
//                             if (docName.isBlank()) { nameErr = true; return@Button }
//                             onAdded()
//                         },
//                         modifier = Modifier
//                             .fillMaxWidth()
//                             .height(50.dp)
//                             .windowInsetsPadding(WindowInsets.navigationBars),
//                         shape = RoundedCornerShape(12.dp),
//                         colors = ButtonDefaults.buttonColors(containerColor = VC.Primary)
//                     ) {
//                         Text(
//                             "Add Document",
//                             fontWeight = FontWeight.SemiBold,
//                             fontSize = 15.sp,
//                             color = Color.White
//                         )
//                     }
// 
//                     Spacer(Modifier.height(8.dp))
//                 }
//             }
//         }
//     }
// }