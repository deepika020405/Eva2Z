package com.developer.ev2z.ui.screens.serviceWarranty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler

/**
 * SCREEN 1 — Service Dashboard (FRD §3).
 * Quick actions, counters, active allocation and recent complaints.
 */
@Composable
fun ServiceDashboardScreen(
    viewModel: ServiceDashboardViewModel,
    onRaiseComplaint: (vehicleId: String?) -> Unit,
    onOpenComplaints: (ComplaintFilter) -> Unit,
    onOpenTicket: (ticketId: String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    PlatformBackHandler(true) { onBack() }

    ServiceScaffold(
        title = "Service & Warranty",
        onBack = onBack,
        actions = {
            IconButton(onClick = viewModel::refresh, enabled = !state.isRefreshing) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ServiceColors.Brand,
                    )
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = ServiceColors.TextPrimary)
                }
            }
        },
    ) { topPadding, bottomPadding ->

        if (state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(top = topPadding),
                contentAlignment = Alignment.Center,
            ) { LoadingState(label = "Loading your service data…") }
            return@ServiceScaffold
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {

            // Same breakpoints the rest of the app uses (see HomeScreen).
            val isTablet = maxWidth >= 600.dp
            val isCompact = maxWidth < 360.dp
            val hPad = when { isTablet -> 28.dp; isCompact -> 12.dp; else -> 16.dp }
            val gap = if (isCompact) 8.dp else 10.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = hPad,
                    end = hPad,
                    top = topPadding + 8.dp,
                    bottom = bottomPadding + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                /* ───────────────────── active allocation ────────────────────── */
                state.activeAllocation?.let { ticket ->
                    item(key = "allocation") {
                        ActiveAllocationCard(ticket) { onOpenTicket(ticket.ticketId) }
                    }
                }

                /* ─────────────────────── quick actions ──────────────────────── */
                item(key = "actions") {
                    val actions = listOf(
                        ActionSpec(
                            "Raise New Complaint",
                            "Report a component issue",
                            Icons.Filled.AddCircleOutline,
                        ) { onRaiseComplaint(state.selectedVehicle?.vehicleId) },
                        ActionSpec(
                            "My Complaints",
                            "${state.summary.active} open request(s)",
                            Icons.AutoMirrored.Filled.ListAlt,
                        ) { onOpenComplaints(ComplaintFilter.ALL) },
                        ActionSpec(
                            "Service Status",
                            "${state.summary.inProgress} in progress",
                            Icons.Filled.VerifiedUser,
                        ) { onOpenComplaints(ComplaintFilter.IN_PROGRESS) },
                    )
                    // All four across on a tablet, 2×2 on a phone.
                    EqualTileGrid(
                        items = actions,
                        perRow = if (isTablet) 4 else 2,
                        gap = gap,
                    ) { spec, tileModifier ->
                        ActionTile(
                            title = spec.title,
                            subtitle = spec.subtitle,
                            icon = spec.icon,
                            onClick = spec.onClick,
                            modifier = tileModifier,
                        )
                    }
                }



                /* ──────────────────────── recent tickets ────────────────────── */
                item(key = "recent-header") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Recent complaints",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ServiceColors.TextPrimary,
                        )
                        if (state.recentTickets.isNotEmpty()) {
                            TextButton(onClick = { onOpenComplaints(ComplaintFilter.ALL) }) {
                                Text("View all", fontSize = 13.sp, color = ServiceColors.Brand)
                            }
                        }
                    }
                }

                if (state.recentTickets.isEmpty()) {
                    item(key = "recent-empty") {
                        ServiceCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.History,
                                    contentDescription = null,
                                    tint = ServiceColors.TextMuted,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "No service requests yet. Raise a complaint and we'll track it here.",
                                    fontSize = 13.sp,
                                    color = ServiceColors.TextSecondary,
                                    lineHeight = 18.sp,
                                )
                            }
                        }
                    }
                } else {
                    items(state.recentTickets, key = { it.ticketId }) { ticket ->
                        TicketCard(ticket = ticket, onClick = { onOpenTicket(ticket.ticketId) })
                    }
                }

                state.errorMessage?.let { message ->
                    item(key = "error") { InlineBanner(text = message, tone = BannerTone.DANGER) }
                }
            }
        }
    }
}

/* ───────────────────────────── private pieces ──────────────────────────── */

private data class ActionSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * Lays [items] out [perRow] to a row, every tile the same width.
 *
 * A short last row is padded with empty weighted spacers so its tiles keep the
 * width they'd have in a full row instead of stretching to fill it.
 */
@Composable
private fun <T> EqualTileGrid(
    items: List<T>,
    perRow: Int,
    gap: Dp,
    tile: @Composable RowScope.(item: T, modifier: Modifier) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        items.chunked(perRow).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                rowItems.forEach { item -> tile(item, Modifier.weight(1f)) }
                repeat(perRow - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ActiveAllocationCard(ticket: ServiceTicket, onClick: () -> Unit) {
    val allocation = ticket.allocation ?: return
    ServiceCard(
        title = "Assigned service",
        trailing = {
            TextButton(onClick = onClick) {
                Text("Details", fontSize = 13.sp, color = ServiceColors.Brand)
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(ServiceColors.BrandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Build,
                    contentDescription = null,
                    tint = ServiceColors.Brand,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    allocation.technicianName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ServiceColors.TextPrimary,
                )
                Text(
                    allocation.serviceCenterName,
                    fontSize = 12.sp,
                    color = ServiceColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        InlineBanner(
            text = allocation.expectedServiceDateMillis
                ?.let { "Expected service date: ${it.asServiceDate()} · Ticket ${ticket.ticketId}" }
                ?: "Ticket ${ticket.ticketId} — service date will be confirmed shortly.",
            tone = BannerTone.INFO,
        )
    }
}