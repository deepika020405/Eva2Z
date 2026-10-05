package com.developer.ev2z.ui.screens.serviceWarranty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.ui.screens.social_media.openUrl

/**
 * SCREEN 4 — Complaint Detail (FRD §5 / §7).
 *
 * Only the complaint itself: what the user filed, where it has reached, what
 * they attached and the status history. Vehicle and warranty details are the
 * dashboard's job — repeating them here just buried the status.
 */
@Composable
fun ComplaintDetailScreen(
    viewModel: ComplaintDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val ticket = state.ticket

    PlatformBackHandler(true) { onBack()
    }

    ServiceScaffold(
        title = ticket?.ticketId ?: "Complaint",
        onBack = onBack,
    ) { topPadding, bottomPadding ->

        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(top = topPadding),
                contentAlignment = Alignment.Center,
            ) { LoadingState(label = "Loading ticket…") }

            ticket == null -> Box(
                Modifier.fillMaxSize().padding(top = topPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = "Ticket not found",
                    subtitle = state.errorMessage ?: "This service request is no longer available.",
                    icon = Icons.Filled.ErrorOutline,
                    action = {
                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ServiceColors.Brand,
                                contentColor = Color.White,
                            ),
                        ) { Text("Go back", fontSize = 14.sp) }
                    },
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = topPadding + 8.dp,
                    bottom = bottomPadding + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {

                /* ──────────────────────── header ──────────────────────── */
                item(key = "header") { TicketHeader(ticket) }

                /* ──────────────────────── stepper ─────────────────────── */
                item(key = "stepper") {
                    ServiceCard(title = "Progress") { StatusStepper(current = ticket.status) }
                }

                /* ─────────────────── what was submitted ───────────────── */
                item(key = "issue") {
                    ServiceCard(title = "Your complaint") {
                        Text(
                            ticket.issueDescription,
                            fontSize = 13.sp,
                            color = ServiceColors.TextPrimary,
                            lineHeight = 19.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        LabelValueRow("Problem", ticket.problemLabel)
                        ticket.vehicleType?.let { LabelValueRow("Vehicle", it.label) }
                        ticket.errorCode?.let { LabelValueRow("Error code", it) }
                        // Only the device the complaint was filed against — the rest
                        // of the vehicle's details belong on the dashboard.
                        ticket.vehicle.vehicleId.takeIf { it.isNotEmpty() }?.let {
                            LabelValueRow("Device", ticket.vehicle.registrationNumber)
                        }
                        LabelValueRow("Raised on", ticket.createdAtMillis.asServiceDateTime())

                        // The charge, right under the date it was raised. Paid is the
                        // common case now — the complaint cannot be completed without
                        // it — so the reference and the time are shown too: they are
                        // what support asks for.
                        //
                        // These read the ticket's derived accessors rather than its
                        // receipt: a paid row usually comes back with a null
                        // merchant_order_id, so going via `payment` left the time and
                        // the reference blank on exactly the tickets that had them.
                        val charge = ticket.chargeRupees
                        when {
                            ticket.chargePaid -> {
                                LabelValueRow(
                                    "Service charge",
                                    charge?.let { "₹$it paid" } ?: "Paid",
                                )
                                ticket.chargePaidAtMillis?.let {
                                    LabelValueRow("Paid on", it.asServiceDateTime())
                                }
                                ticket.chargeReference?.let {
                                    LabelValueRow("Payment ref", it)
                                }
                            }
                            ticket.chargePending -> {
                                LabelValueRow(
                                    "Service charge",
                                    charge?.let { "₹$it · payment pending" }
                                        ?: "Payment pending",
                                )
                                ticket.chargeReference?.let {
                                    LabelValueRow("Payment ref", it)
                                }
                            }
                            // Said out loud rather than left blank: an EVA2Z Connect
                            // customer pays nothing, and an empty space where the
                            // charge should be reads as missing data.
                            ticket.chargeNotRequired ->
                                LabelValueRow("Service charge", "Not applicable")

                            charge != null ->
                                LabelValueRow("Service charge", "₹$charge")
                        }
                    }
                }

                /* ───────────────────────── paid banner ────────────────────── */
                // Stated once in full, because it is the receipt: the app no longer
                // keeps a payment record of its own, so this screen is where a user
                // comes back to check what they were charged.
                if (ticket.chargePaid) {
                    item(key = "charge") {
                        InlineBanner(
                            text = ticket.chargeRupees
                                ?.let { "Service charge of ₹$it has been paid for this request." }
                                ?: "The service charge for this request has been paid.",
                            tone = BannerTone.SUCCESS,
                            icon = Icons.Filled.Verified,
                        )
                    }
                } else if (ticket.chargePending) {
                    item(key = "charge") {
                        InlineBanner(
                            text = ticket.chargeRupees
                                ?.let {
                                    "₹$it service charge is still outstanding on this request. " +
                                        "It will be marked paid once the payment is confirmed."
                                }
                                ?: "The service charge on this request is not confirmed yet.",
                            tone = BannerTone.WARNING,
                        )
                    }
                }

                /* ────────────────────── attachments ───────────────────── */
                if (ticket.attachments.isNotEmpty()) {
                    item(key = "attachments") {
                        ServiceCard(title = "Invoice & media") {
                            ticket.attachments.forEachIndexed { index, attachment ->
                                AttachmentRow(
                                    attachment = attachment,
                                    // Only the server-stored copies have somewhere to
                                    // go; a not-yet-uploaded pick stays inert.
                                    onOpen = attachment.url?.let { url -> { openUrl(url) } },
                                    modifier = Modifier.padding(
                                        bottom = if (index == ticket.attachments.lastIndex) 0.dp else 8.dp,
                                    ),
                                )
                            }
                        }
                    }
                }

                /* ──────────────────────── timeline ────────────────────── */
                item(key = "timeline") {
                    ServiceCard(title = "Status timeline") {
                        if (ticket.timeline.isEmpty()) {
                            Text(
                                "No updates yet.",
                                fontSize = 13.sp,
                                color = ServiceColors.TextSecondary,
                            )
                        } else {
                            TicketTimeline(entries = ticket.timeline)
                        }
                    }
                }
            }
        }
    }
}

/* ───────────────────────────── private pieces ──────────────────────────── */

@Composable
private fun TicketHeader(ticket: ServiceTicket) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    ticket.problemLabel,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ServiceColors.TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    ticket.issueTitle,
                    fontSize = 13.sp,
                    color = ServiceColors.TextSecondary,
                    lineHeight = 18.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            TicketStatusChip(ticket.status)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Last updated ${ticket.lastUpdatedMillis.asServiceDateTime()}",
            fontSize = 11.sp,
            color = ServiceColors.TextMuted,
        )
    }
}