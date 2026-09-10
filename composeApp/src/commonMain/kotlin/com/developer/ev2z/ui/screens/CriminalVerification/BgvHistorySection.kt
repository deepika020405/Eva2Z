package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Candidate ids the user already submitted. Tapping one re-checks its status with
 * the server, so nobody has to write the id down on paper.
 */
@Composable
fun BgvHistorySection(
    entries: List<BgvHistoryEntry>,
    isLoading: Boolean,
    onCheckStatus: (candidateId: Int) -> Unit,
    onDelete: (candidateId: Int) -> Unit
) {
    if (entries.isEmpty()) return

    val clipboard = LocalClipboardManager.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = CV_TextSec,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Your Verifications",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CV_TextPrimary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "(${entries.size})",
                fontSize = 13.sp,
                color = CV_TextHint
            )
        }

        entries.forEach { entry ->
            BgvHistoryCard(
                entry = entry,
                isLoading = isLoading,
                onCheckStatus = { onCheckStatus(entry.candidateId) },
                onCopy = { clipboard.setText(AnnotatedString(entry.candidateId.toString())) },
                onDelete = { onDelete(entry.candidateId) }
            )
        }
    }
}

@Composable
private fun BgvHistoryCard(
    entry: BgvHistoryEntry,
    isLoading: Boolean,
    onCheckStatus: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val status = parseHistoryStatus(entry.status)
    val (statusText, statusColor) = historyStatusStyle(status)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CV_Surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onCheckStatus() }
    ) {
        Column(Modifier.padding(14.dp)) {

            // Name + status chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.name.ifBlank { "Candidate" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CV_TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Candidate ID + copy
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CV_SectionBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ID",
                            fontSize = 10.sp,
                            color = CV_TextHint
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            entry.candidateId.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CV_TextPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Candidate ID",
                            tint = CV_Primary,
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { onCopy() }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                if (entry.createdAtMs > 0) {
                    Text(
                        formatHistoryDate(entry.createdAtMs),
                        fontSize = 11.sp,
                        color = CV_TextHint
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = CV_Border)
            Spacer(Modifier.height(10.dp))

            // Payment + action
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.paymentDone) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CV_Success,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        buildString {
                            append("Payment Done")
                            if (entry.amount > 0) append(" · ₹${entry.amount}")
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CV_Success
                    )
                } else {
                    Text(
                        "Payment status unavailable",
                        fontSize = 12.sp,
                        color = CV_TextHint
                    )
                }

                Spacer(Modifier.weight(1f))

                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Remove from history",
                    tint = CV_TextHint,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onDelete() }
                )

                Spacer(Modifier.width(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = !isLoading) { onCheckStatus() }
                ) {
                    Text(
                        "Check Status",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CV_Primary
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = CV_Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (!entry.merchantOrderId.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Order: ${entry.merchantOrderId}",
                    fontSize = 10.sp,
                    color = CV_TextHint
                )
            }
        }
    }
}

internal fun parseHistoryStatus(raw: String) = when (raw) {
    "COMPLETED"            -> BgvStatus.COMPLETED
    "FAILED"               -> BgvStatus.FAILED
    "CANDIDATE_CREATED"    -> BgvStatus.CANDIDATE_CREATED
    "PAYMENT_DONE"         -> BgvStatus.PAYMENT_DONE
    "FORM_SUBMITTED"       -> BgvStatus.FORM_SUBMITTED
    else                   -> BgvStatus.VERIFICATION_STARTED
}

internal fun historyStatusStyle(status: BgvStatus): Pair<String, Color> = when (status) {
    BgvStatus.COMPLETED            -> "Completed" to CV_Success
    BgvStatus.FAILED               -> "Failed" to CV_Error
    BgvStatus.VERIFICATION_STARTED -> "In Progress" to CV_Info
    BgvStatus.CANDIDATE_CREATED    -> "Candidate Created" to Color(0xFF7C3AED)
    BgvStatus.PAYMENT_DONE         -> "Payment Done" to CV_Success
    BgvStatus.FORM_SUBMITTED       -> "Submitted" to CV_TextHint
}

private val historyMonths = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

@OptIn(kotlin.time.ExperimentalTime::class)
internal fun formatHistoryDate(epochMs: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val hour12 = when (dt.hour % 12) { 0 -> 12; else -> dt.hour % 12 }
    val amPm = if (dt.hour < 12) "AM" else "PM"
    val minute = dt.minute.toString().padStart(2, '0')
    return "${dt.day} ${historyMonths[dt.month.ordinal]} ${dt.year}, $hour12:$minute $amPm"
}