package com.developer.ev2z.ui.screens.serviceWarranty

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ══════════════════════════ SCAFFOLD + CARDS ══════════════════════════════ */

/**
 * Shared page chrome for the flow — matches MyAccountScreen / SupportWarrantyScreen:
 * AppPageBg behind everything, a flat top bar in the same colour, and the
 * chevron-left back affordance the rest of the app uses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (topPadding: androidx.compose.ui.unit.Dp, bottomPadding: androidx.compose.ui.unit.Dp) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = ServiceColors.PageBg,
        snackbarHost = snackbarHost,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ServiceColors.TextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = ServiceColors.TextPrimary,
                        )
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ServiceColors.PageBg,
                ),
            )
        },
    ) { inner ->
        content(inner.calculateTopPadding(), inner.calculateBottomPadding())
    }
}

/** White rounded card — the app's standard content container. */
@Composable
fun ServiceCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ServiceColors.CardBg),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null || trailing != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (title != null) {
                        Text(
                            title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ServiceColors.TextPrimary,
                        )
                    } else {
                        Spacer(Modifier.width(0.dp))
                    }
                    trailing?.invoke()
                }
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun LabelValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = ServiceColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = ServiceColors.TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f),
        )
    }
}

/* ══════════════════════════════ CHIPS ═════════════════════════════════════ */

@Composable
private fun Pill(text: String, fg: Color, bg: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
    }
}

@Composable
fun TicketStatusChip(status: TicketStatus) {
    val (fg, bg) = when (status) {
        TicketStatus.SUBMITTED -> ServiceColors.Info to ServiceColors.InfoSoft
        TicketStatus.OPEN -> ServiceColors.Info to ServiceColors.InfoSoft
        TicketStatus.IN_PROGRESS -> ServiceColors.Warning to ServiceColors.WarningSoft
        TicketStatus.COMPLETED -> ServiceColors.Brand to ServiceColors.BrandSoft
    }
    Pill(status.label, fg, bg)
}

/* ═════════════════════════════ BANNERS ════════════════════════════════════ */

enum class BannerTone { INFO, SUCCESS, WARNING, DANGER }

@Composable
fun InlineBanner(
    text: String,
    tone: BannerTone,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Info,
) {
    val (fg, bg) = when (tone) {
        BannerTone.INFO -> ServiceColors.Info to ServiceColors.InfoSoft
        BannerTone.SUCCESS -> ServiceColors.Brand to ServiceColors.BrandSoft
        BannerTone.WARNING -> ServiceColors.Warning to ServiceColors.WarningSoft
        BannerTone.DANGER -> ServiceColors.Danger to ServiceColors.DangerSoft
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = fg, lineHeight = 17.sp)
    }
}

/* ═══════════════════════════ ATTACHMENTS ══════════════════════════════════ */

@Composable
fun AttachmentRow(
    attachment: ServiceAttachment,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
) {
    val icon = when (attachment.kind) {
        AttachmentKind.INVOICE -> Icons.Filled.Description
        AttachmentKind.PHOTO -> Icons.Filled.Image
        AttachmentKind.VIDEO -> Icons.Filled.Videocam
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .let { if (onOpen != null) it.clickable { onOpen() } else it }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(ServiceColors.BrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = ServiceColors.Brand, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                attachment.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ServiceColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                // A file fetched back from the server has no byte count, so the
                // kind of file stands in rather than printing "0 B".
                if (attachment.sizeBytes > 0) attachment.sizeBytes.asFileSize()
                else attachment.name.substringAfterLast('.', "").uppercase().ifBlank { "File" },
                fontSize = 11.sp,
                color = ServiceColors.TextMuted,
            )
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove ${attachment.name}",
                    tint = ServiceColors.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/* ═══════════════════════════ TILES + CARDS ════════════════════════════════ */

@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ServiceColors.CardBg),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                    .background(ServiceColors.BrandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = ServiceColors.Brand,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            // Both texts reserve a fixed number of lines so every tile in a row is
            // exactly as tall as its neighbour — otherwise a title that wraps onto a
            // second line ("Raise New Complaint") makes its card taller than a
            // single-line one ("My Complaints").
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ServiceColors.TextPrimary,
                lineHeight = 18.sp,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 11.sp,
                color = ServiceColors.TextSecondary,
                lineHeight = 15.sp,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ServiceColors.CardBg),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ServiceColors.TextPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            // Single line keeps all four counters the same height even when a label
            // is too wide for a narrow column.
            Text(
                label,
                fontSize = 10.sp,
                color = ServiceColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TicketCard(
    ticket: ServiceTicket,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ServiceColors.CardBg),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    ticket.ticketId,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ServiceColors.TextSecondary,
                )
                TicketStatusChip(ticket.status)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                ticket.component.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ServiceColors.TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                ticket.issueDescription,
                fontSize = 12.sp,
                color = ServiceColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = ServiceColors.BorderSoft)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(
                    ticket.vehicle.registrationNumber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ServiceColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Updated ${ticket.lastUpdatedMillis.asServiceDate()}",
                    fontSize = 10.sp,
                    color = ServiceColors.TextMuted,
                )
            }
        }
    }
}

/* ════════════════════════════ STATES ══════════════════════════════════════ */

@Composable
fun LoadingState(label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = ServiceColors.Brand, strokeWidth = 3.dp)
        Spacer(Modifier.height(14.dp))
        Text(label, fontSize = 13.sp, color = ServiceColors.TextSecondary)
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(ServiceColors.BrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = ServiceColors.Brand, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ServiceColors.TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            fontSize = 13.sp,
            color = ServiceColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
        )
        if (action != null) {
            Spacer(Modifier.height(14.dp))
            action()
        }
    }
}

/* ═══════════════════════ STEPPER + TIMELINE ═══════════════════════════════ */

/** Horizontal lifecycle stepper across the four backend statuses. */
@Composable
fun StatusStepper(current: TicketStatus, modifier: Modifier = Modifier) {
    val stages = TicketStatus.stepperStages
    val currentIndex = current.stepIndex

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            stages.forEachIndexed { index, _ ->
                val done = index <= currentIndex
                Box(
                    Modifier
                        .size(if (index == currentIndex) 14.dp else 10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (done) ServiceColors.Brand else ServiceColors.BorderSoft),
                )
                if (index != stages.lastIndex) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (index < currentIndex) ServiceColors.Brand else ServiceColors.BorderSoft),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            current.label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ServiceColors.Brand,
        )
        val next = stages.getOrNull(currentIndex + 1)
        if (next != null) {
            Text(
                "Next: ${next.label}",
                fontSize = 11.sp,
                color = ServiceColors.TextMuted,
            )
        }
    }
}

/** Vertical history rail (FRD §7). Entries arrive newest-first. */
@Composable
fun TicketTimeline(entries: List<TimelineEntry>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        entries.forEachIndexed { index, entry ->
            Row(Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (index == 0) ServiceColors.Brand else ServiceColors.BorderSoft),
                    )
                    if (index != entries.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(44.dp)
                                .background(ServiceColors.BorderSoft),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).padding(bottom = if (index == entries.lastIndex) 0.dp else 12.dp)) {
                    Text(
                        entry.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ServiceColors.TextPrimary,
                    )
                    entry.note?.let {
                        Text(it, fontSize = 12.sp, color = ServiceColors.TextSecondary, lineHeight = 17.sp)
                    }
                    Text(
                        entry.timestampMillis.asServiceDateTime(),
                        fontSize = 10.sp,
                        color = ServiceColors.TextMuted,
                    )
                }
            }
        }
    }
}

/** Outlined container used by the upload drop zones. */
@Composable
fun DashedDropZone(
    label: String,
    hint: String,
    icon: ImageVector,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, if (isError) ServiceColors.Danger else ServiceColors.BorderSoft),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = ServiceColors.Brand, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ServiceColors.TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(hint, fontSize = 11.sp, color = ServiceColors.TextMuted, textAlign = TextAlign.Center)
        }
    }
}