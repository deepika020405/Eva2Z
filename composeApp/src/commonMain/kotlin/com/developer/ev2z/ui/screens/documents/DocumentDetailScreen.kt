
package com.developer.ev2z.ui.screens.documents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.developer.ev2z.response.VaultDocumentDetail
import com.developer.ev2z.theme.AppPageBg
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// ════════════════════════════════════════════════════════════════════════════
//  Design tokens
// ════════════════════════════════════════════════════════════════════════════

private object DetailColors {
    val Background    = AppPageBg
    val Surface       = Color.White
    val PreviewBg     = Color(0xFF1B1B1F)
    val PreviewMuted  = Color(0xFF9CA3AF)
    val Paper         = Color(0xFFEEEEEE)
    val PaperFold     = Color(0xFFD4D4D4)
    val PaperLine     = Color(0xFFB0B0B0)
    val TextPrimary   = Color(0xFF111827)
    val TextSecondary = Color(0xFF6B7280)
    val Green         = Color(0xFF22C55E)
    val GreenDark     = Color(0xFF16A34A)
    val GreenLight    = Color(0xFFDCFCE7)
    val Red           = Color(0xFFEF4444)
    val RedLight      = Color(0xFFFEE2E2)
    val Amber         = Color(0xFFF59E0B)
    val AmberDark     = Color(0xFFB45309)
    val AmberLight    = Color(0xFFFEF3C7)
    val GrayLight     = Color(0xFFF3F4F6)
}

enum class DocumentBadgeStatus { Valid, Expiring, Expired, Missing }

// ════════════════════════════════════════════════════════════════════════════
//  Document detail screen
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun DocumentDetailScreen(
    fileName: String,
    uploadedDate: String,
    onClose: () -> Unit,
    onReupload: () -> Unit,
    onDelete: () -> Unit,
    detail: VaultDocumentDetail? = null,
    detailLoading: Boolean = false,
    fallbackDocumentType: String? = null,
    fallbackExpiryDate: String? = null,
    preview: @Composable BoxScope.() -> Unit = { PdfPreviewPlaceholder() },
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailColors.PreviewBg)
            .statusBarsPadding(),
    ) {
        DetailTopBar(title = fileName, onClose = onClose)

        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 180.dp)
                .background(DetailColors.PreviewBg),
            contentAlignment = Alignment.Center,
            content          = preview,
        )

        DocumentInfoCard(
            fileName             = fileName,
            uploadedDate         = uploadedDate,
            detail               = detail,
            detailLoading        = detailLoading,
            fallbackDocumentType = fallbackDocumentType,
            fallbackExpiryDate   = fallbackExpiryDate,
            onReupload           = onReupload,
            onDelete             = onDelete,
        )
    }
}

@Composable
private fun DetailTopBar(title: String, onClose: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(DetailColors.Surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close",
                tint               = DetailColors.TextPrimary,
                modifier           = Modifier.size(22.dp),
            )
        }

        Text(
            text       = title,
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = DetailColors.TextPrimary,
            textAlign  = TextAlign.Center,
            maxLines   = 1,
            modifier   = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )

        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(1.5.dp, DetailColors.Green, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Description,
                contentDescription = null,
                tint               = DetailColors.GreenDark,
                modifier           = Modifier.size(17.dp),
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  PDF / preview placeholder
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun PdfPreviewPlaceholder(message: String = "Loading document…") {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(modifier = Modifier.size(width = 120.dp, height = 140.dp)) {
            val foldSize   = 26.dp.toPx()
            val corner     = 4.dp.toPx()
            val w          = size.width
            val h          = size.height
            val lineGap    = 8.dp.toPx()
            val lineHeight = 1.5.dp.toPx()
            val pad        = 14.dp.toPx()

            val paperPath = Path().apply {
                moveTo(corner, 0f)
                lineTo(w - foldSize, 0f)
                lineTo(w, foldSize)
                lineTo(w, h - corner)
                quadraticTo(w, h, w - corner, h)
                lineTo(corner, h)
                quadraticTo(0f, h, 0f, h - corner)
                lineTo(0f, corner)
                quadraticTo(0f, 0f, corner, 0f)
                close()
            }
            drawPath(paperPath, DetailColors.Paper)

            val foldPath = Path().apply {
                moveTo(w - foldSize, 0f)
                lineTo(w, foldSize)
                lineTo(w - foldSize, foldSize)
                close()
            }
            drawPath(foldPath, DetailColors.PaperFold)

            var y   = pad + lineGap
            var idx = 0
            while (y < h - pad) {
                val maxW    = w - pad * 2
                val isShort = idx % 4 == 3
                val lineW   = if (isShort) maxW * 0.55f else maxW * 0.92f
                val startX  = pad
                val endX    = (startX + lineW).coerceAtMost(
                    if (y < foldSize) w - foldSize - 4.dp.toPx() else w - pad
                )
                if (endX > startX) {
                    drawLine(
                        color       = DetailColors.PaperLine,
                        start       = Offset(startX, y),
                        end         = Offset(endX, y),
                        strokeWidth = lineHeight,
                    )
                }
                y += lineGap
                idx++
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text  = message,
            color = DetailColors.PreviewMuted,
            fontSize = 13.sp,
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Info card (bottom sheet of the detail screen)
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun DocumentInfoCard(
    fileName: String,
    uploadedDate: String,
    detail: VaultDocumentDetail?,
    detailLoading: Boolean,
    fallbackDocumentType: String?,
    fallbackExpiryDate: String?,
    onReupload: () -> Unit,
    onDelete: () -> Unit,
) {
    val fields = remember(detail, fallbackDocumentType, fallbackExpiryDate) {
        extractedFields(detail, fallbackDocumentType, fallbackExpiryDate)
    }
    val badge = remember(detail, fallbackExpiryDate) {
        expiryBadge(detail?.expiryDate ?: fallbackExpiryDate)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DetailColors.Surface)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text       = fileName,
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color      = DetailColors.TextPrimary,
                maxLines   = 1,
                modifier   = Modifier.weight(1f, fill = false),
            )
            if (badge != null) {
                Spacer(Modifier.width(8.dp))
                DetailStatusBadge(label = badge.second, status = badge.first)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Server-side OCR fields read off the document image itself.
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                fields.isNotEmpty() -> fields.forEach { (label, value) ->
                    InfoColumn(label = label, value = value)
                }

                detailLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color       = DetailColors.Green,
                        strokeWidth = 2.dp,
                        modifier    = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Reading details from the document…",
                        fontSize = 13.sp,
                        color    = DetailColors.TextSecondary,
                    )
                }

                else -> Text(
                    text     = "No details could be read from this document yet.",
                    fontSize = 13.sp,
                    color    = DetailColors.TextSecondary,
                )
            }

            InfoColumn(
                label = "Uploaded",
                value = uploadedDate.ifBlank { "—" },
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick  = onReupload,
                shape    = RoundedCornerShape(14.dp),
                border   = BorderStroke(1.5.dp, DetailColors.Green),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = DetailColors.GreenDark),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                Text("Re-upload", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            OutlinedButton(
                onClick  = onDelete,
                shape    = RoundedCornerShape(14.dp),
                border   = BorderStroke(1.5.dp, DetailColors.Red),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = DetailColors.Red),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                Text("Delete", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = DetailColors.TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DetailColors.TextPrimary)
    }
}

@Composable
private fun DetailStatusBadge(label: String, status: DocumentBadgeStatus, detail: String = "") {
    val (bg, fg) = when (status) {
        DocumentBadgeStatus.Valid    -> DetailColors.GreenLight to DetailColors.GreenDark
        DocumentBadgeStatus.Expiring -> DetailColors.AmberLight to DetailColors.AmberDark
        DocumentBadgeStatus.Expired  -> DetailColors.RedLight   to DetailColors.Red
        DocumentBadgeStatus.Missing  -> DetailColors.GrayLight  to DetailColors.TextSecondary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        val text = if (detail.isNotEmpty()) "$label · $detail" else label
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Extracted-detail helpers
// ════════════════════════════════════════════════════════════════════════════

private val monthNames = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** True for OCR values the backend sends as filler rather than real data. */
private fun String?.isMeaningful(): Boolean {
    val v = this?.trim().orEmpty()
    return v.isNotEmpty() &&
        !v.equals("null", ignoreCase = true) &&
        !v.equals("n/a", ignoreCase = true) &&
        !v.equals("na", ignoreCase = true) &&
        !v.equals("-", ignoreCase = true)
}

/** Parses the date shapes the vault returns: ISO instant, `yyyy-MM-dd`, `dd-MM-yyyy`, `dd/MM/yyyy`. */
private fun parseVaultDate(raw: String?): LocalDate? {
    val trimmed = raw?.trim().orEmpty()
    if (!trimmed.isMeaningful()) return null

    runCatching { return LocalDate.parse(trimmed) }
    @OptIn(ExperimentalTime::class)
    runCatching {
        return Instant.parse(trimmed).toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    val parts = trimmed.split('-', '/', '.')
    if (parts.size == 3) {
        val day   = parts[0].toIntOrNull()
        val month = parts[1].toIntOrNull()
        val year  = parts[2].toIntOrNull()
        if (day != null && month != null && year != null && month in 1..12 && day in 1..31) {
            return runCatching { LocalDate(year, month, day) }.getOrNull()
        }
    }
    return null
}

/** Human-readable date; falls back to the raw string when the shape is unknown. */
private fun formatVaultDate(raw: String?): String {
    if (!raw.isMeaningful()) return ""
    val date = parseVaultDate(raw) ?: return raw!!.trim()
    return "${date.day}-${monthNames[date.month.ordinal]}-${date.year}"
}

private fun documentTypeLabel(raw: String?): String? {
    if (!raw.isMeaningful()) return null
    return when (raw!!.trim().lowercase()) {
        "dl", "driving_licence", "driving_license" -> "Driving Licence"
        "rc", "registration_certificate"           -> "Registration Certificate"
        "puc", "pucc"                              -> "PUC Certificate"
        "insurance"                                -> "Insurance"
        "fitness"                                  -> "Fitness Certificate"
        "permit"                                   -> "Permit"
        "aadhaar", "aadhar"                        -> "Aadhaar"
        "pan"                                      -> "PAN Card"
        else -> raw.trim().replace('_', ' ')
            .split(' ')
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
    }
}


private fun extractedFields(
    detail: VaultDocumentDetail?,
    fallbackDocumentType: String?,
    fallbackExpiryDate: String?,
): List<Pair<String, String>> {
    val out = mutableListOf<Pair<String, String>>()
    fun add(label: String, value: String?) {
        if (value.isMeaningful()) out += label to value!!.trim()
    }

    val identifier = detail?.primaryIdentifier

    add("Document type", documentTypeLabel(detail?.documentType ?: fallbackDocumentType))
    add("Document number", identifier)
    add("Name", detail?.holderName)
    add("Father's name", detail?.fatherName)
    add("Date of birth", formatVaultDate(detail?.dob))
    // The vehicle number often *is* the identifier on an RC — don't repeat it.
    if (!detail?.vehicleNumber.equals(identifier, ignoreCase = true)) {
        add("Vehicle number", detail?.vehicleNumber)
    }
    add("Vehicle class", detail?.vehicleClass)
    add("Issue date", formatVaultDate(detail?.issueDate))
    add("Expiry date", formatVaultDate(detail?.expiryDate ?: fallbackExpiryDate))
    add("Issuing authority", detail?.issuingAuthority)
    add("Address", detail?.address)

    return out
}

/** Validity badge derived from the extracted expiry date, or null when unknown. */
@OptIn(ExperimentalTime::class)
private fun expiryBadge(expiry: String?): Pair<DocumentBadgeStatus, String>? {
    val expiryDate = parseVaultDate(expiry) ?: return null
    val today      = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val daysLeft   = expiryDate.toEpochDays() - today.toEpochDays()
    return when {
        daysLeft < 0   -> DocumentBadgeStatus.Expired  to "EXPIRED"
        daysLeft <= 30 -> DocumentBadgeStatus.Expiring to "$daysLeft days left"
        else           -> DocumentBadgeStatus.Valid    to "VALID"
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  Upload source bottom sheet
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadSourceBottomSheet(
    visible: Boolean,
    documentName: String,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onFiles: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope      = rememberCoroutineScope()

    fun dismissThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }
            .invokeOnCompletion {
                if (!sheetState.isVisible) { onDismiss(); action() }
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = DetailColors.Surface,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle       = null,
    ) {
        UploadSourceSheetContent(
            documentName = documentName,
            onCamera     = { dismissThen(onCamera) },
            onGallery    = { dismissThen(onGallery) },
            onFiles      = { dismissThen(onFiles) },
            onClose      = { dismissThen {} },
        )
    }
}

@Composable
private fun UploadSourceSheetContent(
    documentName: String,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onFiles: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text       = "Upload $documentName",
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color      = DetailColors.TextPrimary,
                modifier   = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            )
            Box(
                modifier         = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, DetailColors.Green, CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint               = DetailColors.Green,
                    modifier           = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            UploadSourceRow(
                icon     = Icons.Outlined.PhotoCamera,
                title    = "Camera",
                subtitle = "Take a new photo",
                onClick  = onCamera,
            )
            UploadSourceRow(
                icon     = Icons.Outlined.Image,
                title    = "Gallery",
                subtitle = "Choose from device",
                onClick  = onGallery,
            )
            UploadSourceRow(
                icon     = Icons.Outlined.Folder,
                title    = "Browse files",
                subtitle = "Browse files",
                onClick  = onFiles,
            )
        }
    }
}

@Composable
private fun UploadSourceRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DetailColors.Background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DetailColors.GreenLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = DetailColors.GreenDark, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DetailColors.TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = DetailColors.TextSecondary)
        }
    }
}
