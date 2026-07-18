package com.developer.ev2z.ui.screens.documents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.VaultDocumentDto
import com.developer.ev2z.response.VaultDocumentDetail
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.developer.ev2z.util.PlatformDocumentViewer
import com.developer.ev2z.util.compressStoryImage
import com.developer.ev2z.util.rememberImagePicker
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

// ════════════════════════════════════════════════════════════════════════════
//  Design tokens
// ════════════════════════════════════════════════════════════════════════════

private object VaultColors {
    val Background    = Color(0xFFF4F6F8)
    val Surface       = Color.White

    val TextPrimary   = Color(0xFF111827)
    val TextSecondary = Color(0xFF6B7280)
    val TextTertiary  = Color(0xFF9CA3AF)

    val Green         = Color(0xFF22C55E)
    val GreenDark     = Color(0xFF16A34A)
    val GreenLight    = Color(0xFFDCFCE7)

    val Red           = Color(0xFFEF4444)
    val RedLight      = Color(0xFFFEE2E2)

    val Amber         = Color(0xFFF59E0B)
    val AmberDark     = Color(0xFFB45309)
    val AmberLight    = Color(0xFFFEF3C7)
    val AmberBanner   = Color(0xFFFFF7ED)
    val AmberBorder   = Color(0xFFFED7AA)

    val Gray          = Color(0xFFCBD5E1)
    val GrayLight     = Color(0xFFF3F4F6)
    val Divider       = Color(0xFFE5E7EB)
}

// ════════════════════════════════════════════════════════════════════════════
//  Domain models
// ════════════════════════════════════════════════════════════════════════════

private enum class DocStatus { Valid, Expiring, Expired, Missing }

private data class DocumentSlot(
    val matchKeys: List<String>,
    val category: String,
    val title: String,
    val fallbackSubtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val validityYears: Int = 0,
)

private val documentSlots = listOf(
    DocumentSlot(
        matchKeys        = listOf("rc", "registration"),
        category         = "Identity & Registration",
        title            = "Registration Certificate (RC)",
        fallbackSubtitle = "Vehicle ownership document",
        icon             = Icons.Outlined.Description,
        iconTint         = VaultColors.GreenDark,
        iconBg           = VaultColors.GreenLight,
        validityYears    = 15,
    ),
    DocumentSlot(
        matchKeys        = listOf("driving", "licence", "license", "dl"),
        category         = "Identity & Registration",
        title            = "Driving Licence",
        fallbackSubtitle = "Smart card · RTO issued",
        icon             = Icons.Outlined.CreditCard,
        iconTint         = VaultColors.GreenDark,
        iconBg           = VaultColors.GreenLight,
        validityYears    = 20,
    ),
    DocumentSlot(
        matchKeys        = listOf("insurance", "policy"),
        category         = "Insurance & Compliance",
        title            = "Insurance Policy",
        fallbackSubtitle = "Comprehensive coverage",
        icon             = Icons.Outlined.Shield,
        iconTint         = VaultColors.Amber,
        iconBg           = VaultColors.AmberLight,
        validityYears    = 1,
    ),
    DocumentSlot(
        matchKeys        = listOf("puc", "pollution"),
        category         = "Insurance & Compliance",
        title            = "Pollution Certificate (PUC)",
        fallbackSubtitle = "Emission compliance certificate",
        icon             = Icons.Outlined.Eco,
        iconTint         = VaultColors.GreenDark,
        iconBg           = VaultColors.GreenLight,
        validityYears    = 1,
    ),
    DocumentSlot(
        matchKeys        = listOf("service", "maintenance"),
        category         = "Ownership & Finance",
        title            = "Service Record",
        fallbackSubtitle = "Latest service history",
        icon             = Icons.Outlined.Build,
        iconTint         = VaultColors.GreenDark,
        iconBg           = VaultColors.GreenLight,
        validityYears    = 0,
    ),
)

private data class VaultDocument(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val title: String,
)

private val documentTypes: List<VaultDocument> = documentSlots.map {
    VaultDocument(icon = it.icon, iconTint = it.iconTint, iconBg = it.iconBg, title = it.title)
}

// ════════════════════════════════════════════════════════════════════════════
//  Matching, status & summary helpers
// ════════════════════════════════════════════════════════════════════════════

private fun matchesSlot(fileName: String, slot: DocumentSlot): Boolean {
    val lower = fileName.lowercase()
    return slot.matchKeys.any { it in lower }
}

private fun matchDocumentsToSlots(
    documents: List<VaultDocumentDto>,
): LinkedHashMap<DocumentSlot, VaultDocumentDto?> {
    val out = LinkedHashMap<DocumentSlot, VaultDocumentDto?>()
    val used = mutableSetOf<String>()
    documentSlots.forEach { slot ->
        val match = documents.firstOrNull { it.id !in used && matchesSlot(it.fileName, slot) }
        if (match != null) used.add(match.id)
        out[slot] = match
    }
    return out
}

private fun unmatchedDocuments(
    documents: List<VaultDocumentDto>,
    matched: Map<DocumentSlot, VaultDocumentDto?>,
): List<VaultDocumentDto> {
    val usedIds = matched.values.mapNotNull { it?.id }.toSet()
    return documents.filterNot { it.id in usedIds }
}

@OptIn(ExperimentalTime::class)
private fun statusForUploaded(doc: VaultDocumentDto, slot: DocumentSlot): Pair<DocStatus, String> {
    if (slot.validityYears == 0) return DocStatus.Valid to ""
    val createdAtIso = doc.createdAt ?: return DocStatus.Valid to ""
    return try {
        val createdMs: Long = Instant.parse(createdAtIso).toEpochMilliseconds()
        val nowMs: Long = Clock.System.now().toEpochMilliseconds()
        val totalDays: Long = slot.validityYears * 365L
        val daysSince: Long = (nowMs - createdMs) / 86_400_000L
        val daysLeft: Long = totalDays - daysSince
        when {
            daysLeft < 0L  -> DocStatus.Expired  to "Expired"
            daysLeft <= 30 -> DocStatus.Expiring to "$daysLeft days left"
            daysLeft < 365 -> DocStatus.Valid    to "Expires in $daysLeft days"
            else           -> DocStatus.Valid    to "Expires in ${daysLeft / 365} years"
        }
    } catch (_: Exception) {
        DocStatus.Valid to ""
    }
}

private data class VaultSummary(
    val uploadedCount: Int,
    val totalSlots: Int,
    val valid: Int,
    val expired: Int,
    val expiring: Int,
    val missing: Int,
    val firstExpiringSlot: DocumentSlot? = null,
    val firstExpiringDaysLeft: Int? = null,
)

private fun buildSummary(matched: Map<DocumentSlot, VaultDocumentDto?>): VaultSummary {
    var valid = 0; var expired = 0; var expiring = 0; var missing = 0
    var firstExpiringSlot: DocumentSlot? = null
    var firstExpiringDays: Int? = null
    matched.forEach { (slot, doc) ->
        if (doc == null) { missing++; return@forEach }
//        val (status, detail) = statusForUploaded(doc, slot)
//        when (status) {
//            DocStatus.Valid    -> valid++
//            DocStatus.Expiring -> {
//                expiring++
//                val days = detail.substringBefore(' ').toIntOrNull()
//                if (firstExpiringDays == null || (days != null && days < firstExpiringDays!!)) {
//                    firstExpiringSlot = slot
//                    firstExpiringDays = days
//                }
//            }
//            DocStatus.Expired  -> expired++
//            DocStatus.Missing  -> missing++
//        }
    }
    return VaultSummary(
        uploadedCount         = matched.values.count { it != null },
        totalSlots            = matched.size,
        valid                 = valid,
        expired               = expired,
        expiring              = expiring,
        missing               = missing,
        firstExpiringSlot     = firstExpiringSlot,
        firstExpiringDaysLeft = firstExpiringDays,
    )
}

// ════════════════════════════════════════════════════════════════════════════
//  Main screen
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    onBack: () -> Unit,
    onOpenViewer: (ByteArray, String) -> Unit,
    onOpenUpload: () -> Unit,
) {
    val viewModel = remember {
        DocumentsViewModel(apiService = apiService, storageManager = storageManager)
    }

    val documents by viewModel.documents
    val loading   by viewModel.loading
    val error     by viewModel.error

    var deleteDocId         by remember { mutableStateOf<String?>(null) }
    var editDocument        by remember { mutableStateOf<VaultDocumentDto?>(null) }
    var showDocTypeSheet    by remember { mutableStateOf(false) }
    var newDocPrefilledName by remember { mutableStateOf<String?>(null) }
    var alertDismissed      by remember { mutableStateOf(false) }
    var selectedDoc         by remember { mutableStateOf<Pair<VaultDocumentDto, DocumentSlot?>?>(null) }

    // Disable the screen-level back handler while detail overlay is open so it
    // can handle its own close action instead of popping the whole screen.
    PlatformBackHandler(enabled = selectedDoc == null) { onBack() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val dismissSheet: () -> Unit = {
        scope.launch { sheetState.hide() }
            .invokeOnCompletion { if (!sheetState.isVisible) showDocTypeSheet = false }
    }

    LaunchedEffect(Unit) { viewModel.loadDocuments() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.Background),
    ) {
        when {
            loading -> Box(
                modifier         = Modifier.fillMaxSize().statusBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = VaultColors.Green)
            }

            error != null -> Box(
                modifier         = Modifier.fillMaxSize().statusBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text      = error ?: "Something went wrong",
                    color     = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(24.dp),
                )
            }

            else -> {
                val matched = remember(documents) { matchDocumentsToSlots(documents) }
                val extras  = remember(documents, matched) { unmatchedDocuments(documents, matched) }
                val summary = remember(matched) { buildSummary(matched) }
                val grouped = remember(matched) { matched.entries.groupBy { it.key.category } }

                DocumentsContent(
                    summary               = summary,
                    grouped               = grouped,
                    extras                = extras,
                    showExpiringAlert     = !alertDismissed && summary.firstExpiringSlot != null,
                    onDismissAlert        = { alertDismissed = true },
                    onCardTap             = { doc, slot -> selectedDoc = doc to slot },
                    onView                = { doc ->
                        viewModel.openDocument(
                            documentId = doc.id,
                            onResult   = { bytes -> onOpenViewer(bytes, doc.fileName) },
                        )
                    },
                    onEdit                = { editDocument = it },
                    onDeleteRequest       = { deleteDocId = it.id },
                    onUploadForSlot       = { slot -> newDocPrefilledName = slot.title },
                    onAddDocumentTapped   = { showDocTypeSheet = true },
                    onBack                = onBack,
                )
            }
        }

        if (deleteDocId != null) {
            AlertDialog(
                onDismissRequest = { deleteDocId = null },
                title            = { Text("Delete Document") },
                text             = { Text("Are you sure you want to delete this document?") },
                confirmButton    = {
                    TextButton(onClick = {
                        viewModel.deleteDocument(deleteDocId!!)
                        deleteDocId = null
                    }) { Text("Delete", color = VaultColors.Red) }
                },
                dismissButton    = {
                    TextButton(onClick = { deleteDocId = null }) { Text("Cancel") }
                },
            )
        }

        // Detail overlay is the last child of the Box so it sits on top of
        // the vault list and dialogs with correct z-order and touch dispatch.
        if (selectedDoc != null) {
            val (doc, slot) = selectedDoc!!
            var previewBytes by remember(doc.id) { mutableStateOf<ByteArray?>(null) }
            var detail by remember(doc.id) { mutableStateOf<VaultDocumentDetail?>(null) }

            LaunchedEffect(doc.id) {
                viewModel.openDocument(
                    documentId = doc.id,
                    onResult   = { bytes -> previewBytes = bytes },
                )
            }

            // The new backend serves the image via `image_url` (a plain URL),
            // not the encrypted `vault/{id}` endpoint (which 404s), so fetch the
            // detail to get that URL and fall back to loading it directly.
            LaunchedEffect(doc.id) {
                viewModel.loadDocumentDetail(doc.id) { detail = it }
            }

            PlatformBackHandler(enabled = true) { selectedDoc = null }

            DocumentDetailScreen(
                fileName     = slot?.title ?: doc.fileName,
                uploadedDate = formatIsoDateToReadable(doc.createdAt),
                onClose      = { selectedDoc = null },
                onReupload   = { selectedDoc = null; editDocument = doc },
                onDelete     = { selectedDoc = null; deleteDocId = doc.id },
                preview      = {
                    val imageUrl = ApiService.absoluteImageUrl(detail?.imageUrl) ?: doc.imageUrl
                    when {
                        previewBytes != null -> PlatformDocumentViewer(
                            bytes    = previewBytes!!,
                            modifier = Modifier.fillMaxSize(),
                        )
                        imageUrl != null -> AsyncImage(
                            model              = imageUrl,
                            contentDescription = doc.fileName,
                            contentScale       = ContentScale.Fit,
                            modifier           = Modifier.fillMaxSize(),
                        )
                        else -> PdfPreviewPlaceholder()
                    }
                },
            )
        }
    }

    if (editDocument != null) {
        UploadDocumentScreen(
            apiService     = apiService,
            storageManager = storageManager,
            editDocument   = editDocument,
            onBack         = {
                editDocument = null
                viewModel.loadDocuments()
            },
        )
    }

    if (newDocPrefilledName != null) {
        UploadDocumentScreen(
            apiService     = apiService,
            storageManager = storageManager,
            editDocument   = null,
            prefilledName  = newDocPrefilledName!!,
            onBack         = {
                newDocPrefilledName = null
                viewModel.loadDocuments()
            },
        )
    }

    if (showDocTypeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDocTypeSheet = false },
            sheetState       = sheetState,
            containerColor   = VaultColors.Surface,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle       = null,
        ) {
            DocumentTypeSheet(
                documents = documentTypes,
                onSelect  = { doc ->
                    newDocPrefilledName = doc.title
                    dismissSheet()
                },
                onClose   = { dismissSheet() },
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Content scaffolding
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun DocumentsContent(
    summary: VaultSummary,
    grouped: Map<String, List<Map.Entry<DocumentSlot, VaultDocumentDto?>>>,
    extras: List<VaultDocumentDto>,
    showExpiringAlert: Boolean,
    onDismissAlert: () -> Unit,
    onCardTap: (VaultDocumentDto, DocumentSlot?) -> Unit,
    onView: (VaultDocumentDto) -> Unit,
    onEdit: (VaultDocumentDto) -> Unit,
    onDeleteRequest: (VaultDocumentDto) -> Unit,
    onUploadForSlot: (DocumentSlot) -> Unit,
    onAddDocumentTapped: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier            = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { VaultHeader(onBack = onBack, onAdd = onAddDocumentTapped) }

        item { UploadProgress(uploaded = summary.uploadedCount, total = summary.totalSlots) }

//        item {
//            StatsRow(
//                valid    = summary.valid,
//                expired  = summary.expired,
//                expiring = summary.expiring,
//                missing  = summary.missing,
//            )
//        }

        item {
            AnimatedVisibility(
                visible = showExpiringAlert && summary.firstExpiringSlot != null,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically(),
            ) {
                val slot = summary.firstExpiringSlot
                if (slot != null) {
                    ExpiringAlert(
                        title     = "${slot.title.substringBefore(' ')} expiring in ${summary.firstExpiringDaysLeft ?: 0} days",
                        subtitle  = "Renew soon to avoid lapse in coverage.",
                        ctaText   = "Renew Now",
                        onRenew   = { onUploadForSlot(slot) },
                        onDismiss = onDismissAlert,
                    )
                }
            }
        }

        grouped.forEach { (category, entries) ->
            item { SectionHeader(title = category, count = entries.size) }

            entries.forEach { (slot, doc) ->
                item {
                    if (doc != null) {
                        DocumentCard(
                            doc      = doc,
                            slot     = slot,
                            onTap    = { onCardTap(doc, slot) },
                            onView   = { onView(doc) },
                            onEdit   = { onEdit(doc) },
                            onDelete = { onDeleteRequest(doc) },
                        )
                    } else {
                        MissingSlotCard(slot = slot, onUpload = { onUploadForSlot(slot) })
                    }
                }
            }
        }

        if (extras.isNotEmpty()) {
            item { SectionHeader(title = "Other Documents", count = extras.size) }
            items(extras) { doc ->
                DocumentCard(
                    doc      = doc,
                    slot     = null,
                    onTap    = { onCardTap(doc, null) },
                    onView   = { onView(doc) },
                    onEdit   = { onEdit(doc) },
                    onDelete = { onDeleteRequest(doc) },
                )
            }
        }

        item { AddDocumentButton(onClick = onAddDocumentTapped) }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Header
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun VaultHeader(onBack: () -> Unit, onAdd: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(
                icon       = Icons.AutoMirrored.Filled.ArrowBack,
                background = VaultColors.GrayLight,
                tint       = VaultColors.TextPrimary,
                onClick    = onBack,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "My Vault",
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color      = VaultColors.TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "My City · MH12AB3456",
                    fontSize = 13.sp,
                    color    = VaultColors.TextSecondary,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                icon       = Icons.Outlined.Notifications,
                background = VaultColors.GrayLight,
                tint       = VaultColors.TextSecondary,
                onClick    = { },
            )
            CircleIconButton(
                icon       = Icons.Filled.Add,
                background = VaultColors.Green,
                tint       = Color.White,
                onClick    = onAdd,
            )
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    background: Color,
    tint: Color,
    onClick: () -> Unit,
    size: Dp = 40.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Upload progress
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun UploadProgress(uploaded: Int, total: Int) {
    val pct = if (total == 0) 0f else uploaded.toFloat() / total.toFloat()
    val pctInt = (pct * 100).toInt()
    Column {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                "$uploaded of $total documents uploaded",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = VaultColors.TextPrimary,
            )
            Text(
                "$pctInt%",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (pct >= 1f) VaultColors.GreenDark else VaultColors.Amber,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress   = { pct },
            color      = if (pct >= 1f) VaultColors.Green else VaultColors.Amber,
            trackColor = VaultColors.GreenLight,
            modifier   = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text     = progressMessage(uploaded, total),
            fontSize = 13.sp,
            color    = VaultColors.TextSecondary,
        )
    }
}

private fun progressMessage(uploaded: Int, total: Int): String {
    val missing = (total - uploaded).coerceAtLeast(0)
    return when {
        total == 0          -> "Add your first document to get started."
        uploaded == 0       -> "Get started — upload your first document."
        missing == 0        -> "All documents up to date ✓"
        missing == 1        -> "Almost there! Upload 1 more to complete your vault"
        uploaded >= total/2 -> "Great progress — $missing more to go"
        else                -> "Keep going — $missing more to upload"
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Stats row
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun StatsRow(valid: Int, expired: Int, expiring: Int, missing: Int) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard("Valid",    valid,    VaultColors.Green, Modifier.weight(1f))
        StatCard("Expired",  expired,  VaultColors.Red,   Modifier.weight(1f))
        StatCard("Expiring", expiring, VaultColors.Amber, Modifier.weight(1f))
        StatCard("Missing",  missing,  VaultColors.Gray,  Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(VaultColors.Surface)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BookGlyph(color = color)
        Spacer(Modifier.height(10.dp))
        Text(
            count.toString(),
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = VaultColors.TextPrimary,
        )
        Text(label, fontSize = 12.sp, color = VaultColors.TextSecondary)
    }
}

@Composable
private fun BookGlyph(color: Color) {
    Row(modifier = Modifier.size(width = 22.dp, height = 26.dp)) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp))
                .background(color.copy(alpha = 0.55f)),
        )
        Box(
            modifier = Modifier
                .width(17.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                .background(color),
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Expiring alert banner
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun ExpiringAlert(
    title: String,
    subtitle: String,
    ctaText: String,
    onRenew: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VaultColors.AmberBanner)
            .border(1.dp, VaultColors.AmberBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Column {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.WarningAmber, null,
                        tint = VaultColors.Amber,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        title,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = VaultColors.AmberDark,
                    )
                }
                Icon(
                    Icons.Outlined.Close, "Dismiss",
                    tint = VaultColors.TextTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onDismiss),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color    = VaultColors.AmberDark.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 26.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier          = Modifier
                    .padding(start = 26.dp)
                    .clickable(onClick = onRenew),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    ctaText,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = VaultColors.GreenDark,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward, null,
                    tint = VaultColors.GreenDark,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Section header
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = VaultColors.TextPrimary,
        )
        Spacer(Modifier.width(6.dp))
        Text("($count)", fontSize = 14.sp, color = VaultColors.TextTertiary)
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Document card (uploaded state)
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun DocumentCard(
    doc: VaultDocumentDto,
    slot: DocumentSlot?,
    onTap: () -> Unit,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val displayTitle    = slot?.title ?: doc.fileName
    val displaySubtitle = slot?.fallbackSubtitle ?: "Added ${formatIsoDateToReadable(doc.createdAt)}"
    val icon            = slot?.icon ?: iconFor(doc.fileName)
    val iconTint        = slot?.iconTint ?: iconTintFor(doc.fileName)
    val iconBg          = slot?.iconBg ?: iconBgFor(doc.fileName)

//    val (status, detail) = slot?.let { statusForUploaded(doc, it) } ?: (DocStatus.Valid to "")
//    val statusLabel = when (status) {
//        DocStatus.Valid    -> "VALID"
//        DocStatus.Expiring -> "EXPIRING"
//        DocStatus.Expired  -> "EXPIRED"
//        DocStatus.Missing  -> "MISSING"
//    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VaultColors.Surface)
            .clickable(onClick = onTap)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                displayTitle,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = VaultColors.TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                displaySubtitle,
                fontSize = 13.sp,
                color    = VaultColors.TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
//            StatusBadge(label = statusLabel, detail = detail, status = status)
        }

        Spacer(Modifier.width(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                icon       = Icons.Outlined.Visibility,
                background = VaultColors.GrayLight,
                tint       = VaultColors.TextSecondary,
                onClick    = onView,
                size       = 36.dp,
            )
            Box {
                CircleIconButton(
                    icon       = Icons.Default.MoreVert,
                    background = VaultColors.GrayLight,
                    tint       = VaultColors.TextSecondary,
                    onClick    = { menuExpanded = true },
                    size       = 36.dp,
                )
                DropdownMenu(
                    expanded         = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape            = RoundedCornerShape(14.dp),
                    modifier         = Modifier.background(Color.White),
                ) {
                    CompactMenuItem(text = "Edit") {
                        menuExpanded = false; onEdit()
                    }
                    CompactMenuItem(text = "Delete", textColor = VaultColors.Red) {
                        menuExpanded = false; onDelete()
                    }
                }
            }
        }
    }
}




// ════════════════════════════════════════════════════════════════════════════
//  Missing slot card
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun MissingSlotCard(slot: DocumentSlot, onUpload: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VaultColors.Surface)
            .border(
                width = 1.dp,
                color = VaultColors.Divider,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onUpload)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(slot.iconBg.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                slot.icon, null,
                tint     = slot.iconTint.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                slot.title,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = VaultColors.TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                slot.fallbackSubtitle,
                fontSize = 13.sp,
                color    = VaultColors.TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
//            StatusBadge(label = "MISSING", detail = "", status = DocStatus.Missing)
        }

        Spacer(Modifier.width(8.dp))

        CircleIconButton(
            icon       = Icons.Outlined.Upload,
            background = VaultColors.GreenLight,
            tint       = VaultColors.GreenDark,
            onClick    = onUpload,
            size       = 36.dp,
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Status badge
// ════════════════════════════════════════════════════════════════════════════

//@Composable
//private fun StatusBadge(label: String, detail: String, status: DocStatus) {
//    val (bg, fg) = when (status) {
//        DocStatus.Valid    -> VaultColors.GreenLight to VaultColors.GreenDark
//        DocStatus.Expiring -> VaultColors.AmberLight to VaultColors.AmberDark
//        DocStatus.Expired  -> VaultColors.RedLight   to VaultColors.Red
//        DocStatus.Missing  -> VaultColors.GrayLight  to VaultColors.TextSecondary
//    }
//    Box(
//        modifier = Modifier
//            .clip(RoundedCornerShape(6.dp))
//            .background(bg)
//            .padding(horizontal = 8.dp, vertical = 4.dp),
//    ) {
//        val text = if (detail.isNotEmpty()) "$label · $detail" else label
//        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
//    }
//}

// ════════════════════════════════════════════════════════════════════════════
//  Add document button (inline)
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun AddDocumentButton(onClick: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Add, null,
            tint = VaultColors.TextSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "Add Document",
            fontSize   = 15.sp,
            fontWeight = FontWeight.Medium,
            color      = VaultColors.TextSecondary,
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  "Which document?" picker sheet
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun DocumentTypeSheet(
    documents: List<VaultDocument>,
    onSelect: (VaultDocument) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 32.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                "Which document?",
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color      = VaultColors.TextPrimary,
            )
            Box(
                modifier         = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, VaultColors.Green, CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close, "Close",
                    tint = VaultColors.Green,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            documents.forEach { doc ->
                DocumentTypeRow(doc = doc, onClick = { onSelect(doc) })
            }
        }
    }
}

@Composable
private fun DocumentTypeRow(doc: VaultDocument, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VaultColors.Background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(doc.iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(doc.icon, null, tint = doc.iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                doc.title,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = VaultColors.TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text("Tap to upload", fontSize = 12.sp, color = VaultColors.TextTertiary)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Icon / colour heuristic (fallback for unmatched docs)
// ════════════════════════════════════════════════════════════════════════════

private fun iconFor(fileName: String): ImageVector {
    val l = fileName.lowercase()
    return when {
        "rc" in l || "registration" in l                                       -> Icons.Outlined.Description
        "license" in l || "driving" in l || "licence" in l || "dl" in l        -> Icons.Outlined.CreditCard
        "insurance" in l || "policy" in l                                      -> Icons.Outlined.Shield
        "puc" in l || "pollution" in l                                         -> Icons.Outlined.Eco
        "service" in l || "maintenance" in l                                   -> Icons.Outlined.Build
        else                                                                   -> Icons.Outlined.Description
    }
}

private fun iconTintFor(fileName: String): Color {
    val l = fileName.lowercase()
    return if ("insurance" in l || "policy" in l) VaultColors.Amber else VaultColors.GreenDark
}

private fun iconBgFor(fileName: String): Color {
    val l = fileName.lowercase()
    return if ("insurance" in l || "policy" in l) VaultColors.AmberLight else VaultColors.GreenLight
}

// ════════════════════════════════════════════════════════════════════════════
//  Date formatting
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalTime::class)
private fun computeExpiryDate(doc: VaultDocumentDto, slot: DocumentSlot?): String? {
    if (slot == null || slot.validityYears == 0) return null
    val createdAt = doc.createdAt ?: return null
    return try {
        val date = Instant.parse(createdAt)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val expiryYear = date.year + slot.validityYears
        val month = when (date.monthNumber) {
            1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
            5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
            9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
            else -> ""
        }
        "${date.dayOfMonth}-$month-$expiryYear"
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalTime::class)
fun formatIsoDateToReadable(dateString: String?): String {
    if (dateString.isNullOrBlank()) return ""
    return try {
        val instant = Instant.parse(dateString)
        val date    = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val month   = when (date.monthNumber) {
            1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
            5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
            9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
            else -> ""
        }
        "${date.dayOfMonth}-$month-${date.year}"
    } catch (_: Exception) {
        ""
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Compact menu item
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun CompactMenuItem(
    text: String,
    textColor: Color = Color.Black,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text           = { Text(text = text, fontSize = 14.sp, color = textColor) },
        onClick        = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    )
}

// ════════════════════════════════════════════════════════════════════════════
//  Document viewer screen
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    fileName: String,
    bytes: ByteArray,
    onBack: () -> Unit,
    onDownload: () -> Unit,
) {
    PlatformBackHandler(enabled = true) { onBack() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title          = { Text(fileName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions        = {
                    IconButton(onClick = {
                        onDownload()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message  = "Download successful. Please check in Downloads Folder",
                                duration = SnackbarDuration.Short,
                            )
                        }
                    }) {
                        Icon(Icons.Default.Download, null)
                    }
                },
            )
        },
    ) { padding ->
        PlatformDocumentViewer(
            bytes    = bytes,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Upload / edit screen
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)
@Composable
fun UploadDocumentScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    editDocument: VaultDocumentDto? = null,
    prefilledName: String = "",
    onBack: () -> Unit,
) {
    PlatformBackHandler(enabled = true) { onBack() }

    val viewModel = remember {
        DocumentsViewModel(apiService = apiService, storageManager = storageManager)
    }

    val scope             = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isUploading       by remember { mutableStateOf(false) }
    val isEditMode        = editDocument != null

    var fileName      by remember { mutableStateOf(editDocument?.fileName ?: prefilledName) }
    var imageBytes    by remember { mutableStateOf<ByteArray?>(null) }
    var fileNameError by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberImagePicker { bytes ->
        if (bytes != null) {
            imageBytes = compressStoryImage(imageBytes = bytes, maxSizeKB = 800)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title          = { Text(if (isEditMode) "Edit Document" else "Add Document") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions        = {
                    TextButton(
                        enabled = !isUploading && fileName.isNotBlank(),
                        onClick = {
                            scope.launch {
                                if (fileName.isBlank()) {
                                    fileNameError = true
                                    return@launch
                                }
                                if (!isEditMode && imageBytes == null) {
                                    snackbarHostState.showSnackbar("Please select an image")
                                    return@launch
                                }
                                try {
                                    isUploading = true

                                    val result = if (isEditMode) {
                                        viewModel.updateDocument(
                                            documentId = editDocument!!.id,
                                            fileName   = fileName,
                                            imageBytes = imageBytes,
                                        )
                                    } else {
                                        viewModel.uploadDocument(
                                            fileName   = fileName,
                                            imageBytes = imageBytes!!,
                                        )
                                    }

                                    if (result.success) {
                                        // The vault list endpoint may not return a
                                        // freshly uploaded doc yet, so surface it right
                                        // away from the upload response (real id, type,
                                        // expiry) and cache the picked image for preview.
                                        val uploadedId =
                                            if (isEditMode) editDocument!!.id
                                            else result.id?.toString()
                                        if (!uploadedId.isNullOrBlank()) {
                                            viewModel.addOrUpdateLocal(
                                                VaultDocumentDto(
                                                    id           = uploadedId,
                                                    fileName     = fileName,
                                                    fileType     = "image/jpeg",
                                                    createdAt    = Clock.System.now().toString(),
                                                    documentType = result.documentType,
                                                    expiryDate   = result.expiryDate,
                                                    imageUrl     = ApiService.absoluteImageUrl(result.imageUrl),
                                                ),
                                                imageBytes,
                                            )
                                        }
                                        val action = if (isEditMode) "updated" else "uploaded"
                                        snackbarHostState.showSnackbar(
                                            uploadSuccessMessage(fileName, action, result),
                                        )
                                        onBack()
                                    } else {
                                        // Rejected by server validation (e.g. wrong
                                        // document type). Keep the screen open so the
                                        // user can pick the correct file.
                                        snackbarHostState.showSnackbar(rejectionMessage(result))
                                        isUploading = false
                                    }
                                } catch (_: Exception) {
                                    snackbarHostState.showSnackbar("Operation failed. Please check your connection and try again.")
                                    isUploading = false
                                }
                            }
                        },
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(if (isEditMode) "Update" else "Upload")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value          = fileName,
                onValueChange  = {
                    fileName = it
                    if (it.isNotBlank()) fileNameError = false
                },
                modifier       = Modifier.fillMaxWidth(),
                label          = { Text("File Name *") },
                isError        = fileNameError,
                supportingText = {
                    if (fileNameError) {
                        Text("File name is required", color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine     = true,
            )

            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE0E0E0))
                    .clickable { imagePickerLauncher.launch() },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (imageBytes == null) "Tap to select image" else "Image selected ✅")
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  COMMENTED OUT: previous active UI code
// ════════════════════════════════════════════════════════════════════════════

// @OptIn(ExperimentalMaterial3Api::class)
// @Composable
// fun DocumentsScreen(
//     apiService: ApiService,
//     storageManager: StorageManager,
//     onBack: () -> Unit,
//     onOpenViewer: (ByteArray, String) -> Unit,
//     onOpenUpload: () -> Unit
// ) {
//     PlatformBackHandler(enabled = true) { onBack() }
//     val viewModel = remember {
//         DocumentsViewModel(apiService = apiService, storageManager = storageManager)
//     }
//     val documents by viewModel.documents
//     val loading by viewModel.loading
//     val error by viewModel.error
//     var deleteDocId by remember { mutableStateOf<String?>(null) }
//     var editDocument by remember { mutableStateOf<VaultDocumentDto?>(null) }
//     LaunchedEffect(Unit) { viewModel.loadDocuments() }
//     Scaffold(
//         topBar = {
//             TopAppBar(
//                 title = { Text("E-Vault Documents") },
//                 navigationIcon = {
//                     IconButton(onClick = onBack) {
//                         Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
//                     }
//                 },
//                 actions = {
//                     IconButton(onClick = { onOpenUpload() }) {
//                         Icon(Icons.Default.Add, contentDescription = "Add Document")
//                     }
//                 }
//             )
//         }
//     ) { padding ->
//         Box(modifier = Modifier.fillMaxSize().padding(padding)) {
//             when {
//                 loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
//                 error != null -> Text(
//                     text = error ?: "Something went wrong",
//                     color = MaterialTheme.colorScheme.error,
//                     modifier = Modifier.align(Alignment.Center)
//                 )
//                 documents.isEmpty() -> Column(
//                     modifier = Modifier.fillMaxSize().padding(24.dp),
//                     verticalArrangement = Arrangement.Center,
//                     horizontalAlignment = Alignment.CenterHorizontally
//                 ) {
//                     Icon(Icons.Default.Description, null, modifier = Modifier.size(64.dp),
//                         tint = MaterialTheme.colorScheme.onSurfaceVariant)
//                     Spacer(Modifier.height(12.dp))
//                     Text("No E-Vault documents found", fontSize = 18.sp, fontWeight = FontWeight.Medium)
//                     Spacer(Modifier.height(6.dp))
//                     Text("Add your first E-Vault document to get started", fontSize = 14.sp,
//                         color = MaterialTheme.colorScheme.onSurfaceVariant)
//                     Spacer(Modifier.height(20.dp))
//                     Button(onClick = { onOpenUpload() }) {
//                         Icon(Icons.Default.Add, null)
//                         Spacer(Modifier.width(8.dp))
//                         Text("Add E-Vault Document")
//                     }
//                 }
//                 else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
//                     items(documents) { doc ->
//                         DocumentItem(
//                             fileName = doc.fileName,
//                             fileType = "Added on ${formatIsoDateToReadable(doc.createdAt)}",
//                             onView = { viewModel.openDocument(doc.id) { bytes -> onOpenViewer(bytes, doc.fileName) } },
//                             onDelete = { deleteDocId = doc.id },
//                             onRename = { editDocument = doc }
//                         )
//                     }
//                 }
//             }
//             if (deleteDocId != null) {
//                 AlertDialog(
//                     onDismissRequest = { deleteDocId = null },
//                     title = { Text("Delete E-Vault Document") },
//                     text = { Text("Are you sure you want to delete this E-Vault document?") },
//                     confirmButton = {
//                         TextButton(onClick = { viewModel.deleteDocument(deleteDocId!!); deleteDocId = null }) {
//                             Text("Yes", color = Color.Red)
//                         }
//                     },
//                     dismissButton = { TextButton(onClick = { deleteDocId = null }) { Text("Cancel") } }
//                 )
//             }
//         }
//     }
//     if (editDocument != null) {
//         UploadDocumentScreen(
//             apiService = apiService, storageManager = storageManager,
//             editDocument = editDocument,
//             onBack = { editDocument = null; viewModel.loadDocuments() }
//         )
//     }
// }
//
// @Composable
// private fun DocumentItem(
//     fileName: String, fileType: String,
//     onView: () -> Unit, onDelete: () -> Unit, onRename: () -> Unit = {}
// ) {
//     var menuExpanded by remember { mutableStateOf(false) }
//     Card(
//         modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onView() },
//         elevation = CardDefaults.cardElevation(2.dp),
//         shape = RoundedCornerShape(12.dp)
//     ) {
//         Row(
//             modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
//             verticalAlignment = Alignment.CenterVertically
//         ) {
//             Icon(Icons.Default.Description, null, modifier = Modifier.size(26.dp))
//             Spacer(Modifier.width(10.dp))
//             Column(modifier = Modifier.weight(1f)) {
//                 Text(fileName, fontSize = 15.sp, fontWeight = FontWeight.Medium)
//                 Text(fileType, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
//             }
//             Box {
//                 IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
//                     Icon(Icons.Default.MoreVert, "More options", modifier = Modifier.size(20.dp))
//                 }
//                 DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false },
//                     shape = RoundedCornerShape(14.dp), modifier = Modifier.background(Color.White)) {
//                     CompactMenuItem("View") { menuExpanded = false; onView() }
//                     CompactMenuItem("Edit") { menuExpanded = false; onRename() }
//                     CompactMenuItem("Delete", Color(0xFFD32F2F)) { menuExpanded = false; onDelete() }
//                 }
//             }
//         }
//     }
// }
//
// @OptIn(ExperimentalMaterial3Api::class)
// @Composable
// fun UploadDocumentScreen(
//     apiService: ApiService, storageManager: StorageManager,
//     editDocument: VaultDocumentDto? = null, onBack: () -> Unit
// ) {
//     PlatformBackHandler(enabled = true) { onBack() }
//     val viewModel = remember { DocumentsViewModel(apiService = apiService, storageManager = storageManager) }
//     val scope = rememberCoroutineScope()
//     val snackbarHostState = remember { SnackbarHostState() }
//     var isUploading by remember { mutableStateOf(false) }
//     var fileNameError by remember { mutableStateOf(false) }
//     val isEditMode = editDocument != null
//     var fileName by remember { mutableStateOf(editDocument?.fileName ?: "") }
//     var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
//     val imagePickerLauncher = rememberImagePicker { bytes ->
//         if (bytes != null) imageBytes = compressStoryImage(imageBytes = bytes, maxSizeKB = 800)
//     }
//     Scaffold(
//         snackbarHost = { SnackbarHost(snackbarHostState) },
//         topBar = {
//             TopAppBar(
//                 title = { Text(if (isEditMode) "Edit E-Vault Document" else "Add E-Vault Document") },
//                 navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
//                 actions = {
//                     TextButton(enabled = !isUploading && fileName.isNotBlank(), onClick = {
//                         scope.launch {
//                             if (fileName.isBlank()) { fileNameError = true; return@launch }
//                             if (!isEditMode && imageBytes == null) { snackbarHostState.showSnackbar("Please select an image"); return@launch }
//                             try {
//                                 isUploading = true
//                                 if (isEditMode) {
//                                     viewModel.updateDocument(editDocument!!.id, fileName, imageBytes)
//                                     snackbarHostState.showSnackbar("$fileName updated successfully ✅")
//                                 } else {
//                                     viewModel.uploadDocument(fileName, imageBytes!!)
//                                     snackbarHostState.showSnackbar("$fileName uploaded successfully ✅")
//                                 }
//                                 onBack()
//                             } catch (e: Exception) {
//                                 snackbarHostState.showSnackbar("Operation failed. Please try again")
//                                 isUploading = false
//                             }
//                         }
//                     }) {
//                         if (isUploading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
//                         else Text(if (isEditMode) "Update" else "Upload")
//                     }
//                 }
//             )
//         }
//     ) { padding ->
//         Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
//             var fileNameError by remember { mutableStateOf(false) }
//             OutlinedTextField(value = fileName, onValueChange = { fileName = it; if (it.isNotBlank()) fileNameError = false },
//                 modifier = Modifier.fillMaxWidth(), label = { Text("File Name *") }, isError = fileNameError,
//                 supportingText = { if (fileNameError) Text("File name is required", color = MaterialTheme.colorScheme.error) },
//                 singleLine = true)
//             Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))
//                 .background(Color(0xFFE0E0E0)).clickable { imagePickerLauncher.launch() }, contentAlignment = Alignment.Center) {
//                 Text(if (imageBytes == null) "Tap to select image" else "Image selected ✅")
//             }
//         }
//     }
// }

// ════════════════════════════════════════════════════════════════════════════
//  COMMENTED OUT: original dummy UI code (kept for reference)
// ════════════════════════════════════════════════════════════════════════════

// private object VC {
//     val Primary        = Color(0xFF4CAF50)
//     ...
// }
// (full dummy UI omitted for brevity — was never connected to real data)

// ════════════════════════════════════════════════════════════════════════════
//  Vault upload result helpers
// ════════════════════════════════════════════════════════════════════════════

/** Maps a document-type code from the API to a human-readable label. */
private fun readableDocType(code: String?): String = when (code?.lowercase()) {
    "dl"                       -> "Driving Licence"
    "rc"                       -> "Registration Certificate (RC)"
    "puc"                      -> "Pollution Certificate (PUC)"
    "insurance", "policy"      -> "Insurance Policy"
    "service", "maintenance"   -> "Service Record"
    null, ""                   -> "document"
    else                       -> code.uppercase()
}

/** Builds a clear message when the server rejects an uploaded document. */
private fun rejectionMessage(result: com.developer.ev2z.response.VaultUploadResponse): String {
    if (result.isWrongDocumentType) {
        val expected = readableDocType(result.expectedType)
        val detected = readableDocType(result.detectedType)
        return "This looks like a $detected, but a $expected was expected. " +
                "Please upload the correct document."
    }
    return when (result.reason) {
        "could_not_validate_try_again" ->
            "We couldn't read this document clearly. Please upload a clearer, well-lit photo and try again."
        "not_a_document" ->
            "This image doesn't look like a document. Please upload a clear photo of the document."
        else -> result.message?.takeIf { it.isNotBlank() }
            ?: "We couldn't validate this document. Please upload a clear image of the correct document."
    }
}

/** Builds a success message that surfaces the detected type and expiry, when present. */
private fun uploadSuccessMessage(
    fileName: String,
    action: String,
    result: com.developer.ev2z.response.VaultUploadResponse,
): String {
    val builder = StringBuilder("$fileName $action successfully ✅")
    result.documentType?.let { builder.append(" · ${readableDocType(it)}") }
    formatPlainDate(result.expiryDate)?.let { builder.append(" · Valid till $it") }
    return builder.toString()
}

/** Formats a plain "yyyy-MM-dd" date (no time component) to "dd-MMM-yyyy". */
private fun formatPlainDate(date: String?): String? {
    if (date.isNullOrBlank()) return null
    val parts = date.split("-")
    if (parts.size != 3) return null
    val year  = parts[0]
    val month = parts[1].toIntOrNull() ?: return null
    val day   = parts[2].toIntOrNull() ?: return null
    val monthName = when (month) {
        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
        5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
        9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
        else -> return null
    }
    return "$day-$monthName-$year"
}