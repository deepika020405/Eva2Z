@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.developer.ev2z.ui.screens.fastag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
// Most of these live in material-icons-extended (already on the classpath).
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
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
import com.developer.ev2z.theme.AppPageBg
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Terminal and in-flight states for a recharge.
 *
 * Two rules govern everything in this file.
 *
 * 1. Success is rendered only when the server says SUCCESS. Never from a payment SDK
 *    callback, never from an HTTP 200. A UPI collect can sit pending for 90 seconds;
 *    "the funding leg worked" is a different claim from "the toll wallet was credited".
 *
 * 2. Every state answers "did my money move?" in its first line. That is the only
 *    question a driver has when a recharge does not go cleanly.
 */
@Composable
fun FastagStatusScreen(
    txn: FastagTransaction,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    onShare: () -> Unit = {},
    onDownloadReceipt: () -> Unit = {},
    onContactSupport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // No Scaffold: this screen has no bars, so all it needs is the safe-area inset
    // applied outside the scroll viewport. Under Scaffold the hero sat under the
    // iPhone notch and the buttons under the home indicator.
    FastagResponsiveRoot(
        modifier = modifier
            .fillMaxSize()
            .background(AppPageBg)
    ) { dimens ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    // Cap before fill: fillMaxWidth pins the width to the parent's, and a
                    // widthIn after it can no longer shrink that fixed constraint.
                    .fastagContentWidth(dimens)
                    .fillMaxWidth()
                    .padding(horizontal = dimens.hPad, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (txn.status) {
                    FastagTxnStatus.SUCCESS -> SuccessBody(txn)
                    FastagTxnStatus.FAILED -> FailedBody(txn)
                    FastagTxnStatus.DEEMED,
                    FastagTxnStatus.REFUND_PENDING -> DeemedBody(txn)
                    FastagTxnStatus.REFUNDED -> RefundedBody(txn)
                    FastagTxnStatus.PENDING,
                    FastagTxnStatus.INITIATED -> ProcessingBody(txn)
                    FastagTxnStatus.ABANDONED,
                    FastagTxnStatus.EXPIRED -> AbandonedBody(txn)
                }

                Spacer(Modifier.height(22.dp))

                ReferenceCard(txn)

                Spacer(Modifier.height(20.dp))

                when (txn.status) {
                    FastagTxnStatus.SUCCESS -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GhostButton("Share", Icons.Default.Share, Modifier.weight(1f), onShare)
                            GhostButton("Receipt", Icons.Default.Download, Modifier.weight(1f), onDownloadReceipt)
                        }
                        Spacer(Modifier.height(10.dp))
                        SolidButton("Done", BrandGreen, onDone)
                    }

                    FastagTxnStatus.FAILED -> {
                        // Mints a NEW referenceId upstream. Reusing the failed one would
                        // return FAILED forever and lock the user out of retrying.
                        SolidButton("Try again", BrandGreen, onRetry)
                        Spacer(Modifier.height(10.dp))
                        GhostButton("Back to FASTag", Icons.AutoMirrored.Filled.ArrowBack, Modifier.fillMaxWidth(), onDone)
                    }

                    FastagTxnStatus.DEEMED,
                    FastagTxnStatus.REFUND_PENDING -> {
                        GhostButton("Contact support", Icons.Default.SupportAgent, Modifier.fillMaxWidth(), onContactSupport)
                        Spacer(Modifier.height(10.dp))
                        SolidButton("Done", BrandGreen, onDone)
                    }

                    FastagTxnStatus.ABANDONED,
                    FastagTxnStatus.EXPIRED -> {
                        SolidButton("Start again", BrandGreen, onRetry)
                        Spacer(Modifier.height(10.dp))
                        GhostButton("Back to FASTag", Icons.AutoMirrored.Filled.ArrowBack, Modifier.fillMaxWidth(), onDone)
                    }

                    else -> {
                        GhostButton("Done — notify me", Icons.Default.NotificationsActive, Modifier.fillMaxWidth(), onDone)
                    }
                }

                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

// ─────────────────────────── State bodies ──────────────────────────

@Composable
private fun SuccessBody(txn: FastagTransaction) {
    StatusHero(
        icon = Icons.Default.CheckCircle,
        tint = SuccessGreen,
        background = SuccessGreenLight,
        title = "Recharge successful",
        amount = formatRupees(txn.amountPaise)
    )
    if (txn.newBalancePaise != null) {
        Spacer(Modifier.height(12.dp))
        Text(
            "New balance ${formatRupees(txn.newBalancePaise)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
    }
}

@Composable
private fun FailedBody(txn: FastagTransaction) {
    StatusHero(
        icon = Icons.Default.Cancel,
        tint = DangerRed,
        background = DangerRedLight,
        title = "Payment did not go through",
        amount = null
    )
    Spacer(Modifier.height(14.dp))
    // The single most important line on a failure screen.
    MoneyStatement(
        text = "You have not been charged. No money left your account.",
        tint = SuccessGreen,
        background = SuccessGreenLight,
        icon = Icons.Default.VerifiedUser
    )
    if (!txn.failureReason.isNullOrBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(
            txn.failureReason,
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The user backed out of the UPI app, or never got as far as paying.
 *
 * This is NOT a failure and must not be dressed as one — the user made a choice.
 * It also must not reuse ProcessingBody, which says "your payment went through";
 * for an abandoned attempt that sentence is simply false.
 */
@Composable
private fun AbandonedBody(txn: FastagTransaction) {
    StatusHero(
        icon = Icons.Default.Refresh,
        tint = FastagBlue,
        background = FastagBlueLight,
        title = "Recharge not completed",
        amount = null
    )
    Spacer(Modifier.height(14.dp))
    MoneyStatement(
        text = "You have not been charged. Nothing left your account.",
        tint = SuccessGreen,
        background = SuccessGreenLight,
        icon = Icons.Default.VerifiedUser
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "The payment was cancelled before it completed. " +
                "${txn.displayIdentifier ?: "Your tag"} has not been recharged.",
        fontSize = 12.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
    )
}

@Composable
private fun DeemedBody(txn: FastagTransaction) {
    StatusHero(
        icon = Icons.Default.Schedule,
        tint = WarnAmber,
        background = WarnAmberLight,
        title = "Still processing",
        amount = null
    )
    Spacer(Modifier.height(14.dp))
    // States plainly that money left the account. The user already knows —
    // pretending otherwise is how you lose them.
    MoneyStatement(
        text = "${formatRupees(txn.totalDebitedPaise)} was debited. " +
                "The tag has not been credited yet.",
        tint = WarnAmber,
        background = WarnAmberLight,
        icon = Icons.Default.Info
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "We're checking with ${txn.billerName}. If the recharge can't be completed, " +
                "the full amount is refunded automatically to the account you paid from.",
        fontSize = 12.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
    )
    Spacer(Modifier.height(10.dp))
    Text(
        "You don't need to do anything. We'll update you.",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextHint,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun RefundedBody(txn: FastagTransaction) {
    StatusHero(
        icon = Icons.Default.Replay,
        tint = FastagBlue,
        background = FastagBlueLight,
        title = "Refunded",
        amount = formatRupees(txn.totalDebitedPaise)
    )
    Spacer(Modifier.height(14.dp))
    MoneyStatement(
        text = "The full amount has been returned to the account you paid from.",
        tint = SuccessGreen,
        background = SuccessGreenLight,
        icon = Icons.Default.VerifiedUser
    )
}

@Composable
private fun ProcessingBody(txn: FastagTransaction) {
    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(WarnAmberLight),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(38.dp),
            strokeWidth = 3.dp,
            color = WarnAmber
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "Recharge in progress",
        fontSize = 19.sp,
        fontWeight = FontWeight.ExtraBold,
        color = TextPrimary
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Your payment went through. ${txn.billerName} is crediting the tag now — " +
                "this usually takes a few minutes.",
        fontSize = 12.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
    )

    Spacer(Modifier.height(20.dp))

    // Naming the stage is what stops an anxious user tapping pay a second time.
    ProgressSteps(status = txn.status)

    Spacer(Modifier.height(16.dp))
    Text(
        "Safe to close. We'll notify you either way.",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextHint,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ProgressSteps(status: FastagTxnStatus) {
    val creditDone = status == FastagTxnStatus.SUCCESS
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepRow("Payment received", done = true, current = false)
        StepRow("Crediting FASTag", done = creditDone, current = !creditDone)
        StepRow("Balance updated", done = creditDone, current = false)
    }
}

@Composable
private fun StepRow(label: String, done: Boolean, current: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    when {
                        done -> SuccessGreen
                        current -> WarnAmber
                        else -> MutedIconBg
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (current) FontWeight.ExtraBold else FontWeight.Medium,
            color = when {
                done -> TextPrimary
                current -> WarnAmber
                else -> MutedText
            }
        )
    }
}

// ────────────────────────── Shared pieces ──────────────────────────

@Composable
private fun StatusHero(
    icon: ImageVector,
    tint: Color,
    background: Color,
    title: String,
    amount: String?
) {
    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(44.dp))
    }
    Spacer(Modifier.height(16.dp))
    Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, textAlign = TextAlign.Center)
    if (amount != null) {
        Spacer(Modifier.height(6.dp))
        Text(amount, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = tint)
    }
}

@Composable
private fun MoneyStatement(
    text: String,
    tint: Color,
    background: Color,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(9.dp))
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun ReferenceCard(txn: FastagTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, FieldBorder)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            txn.displayIdentifier?.let { RefRow("Vehicle", it) }
            RefRow("Issuer", txn.billerName)
            RefRow("Amount credited", formatRupees(txn.amountPaise))
            if (txn.convenienceFeePaise > 0L) {
                RefRow("Convenience fee", formatRupees(txn.convenienceFeePaise))
                RefRow("Total debited", formatRupees(txn.totalDebitedPaise))
            }
            txn.refundExpectedByMillis?.let {
                RefRow("Refund expected by", formatDate(it))
            }
            RefRow("Reference", txn.referenceId, mono = true)
            // Fleet operators reconcile against bank statements; support disputes
            // need the issuer reference. Both belong on every receipt.
            txn.issuerRefId?.let { RefRow("Bank reference", it, mono = true) }
            RefRow("Date", formatDateTime(txn.initiatedAtMillis))
        }
    }
}

@Composable
private fun RefRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            fontSize = if (mono) 11.sp else 12.sp,
            fontWeight = if (mono) FontWeight.Normal else FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun SolidButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun GhostButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = BrandGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
    }
}

// ──────────────────────────── Date helpers ─────────────────────────
// kotlinx-datetime rather than java.util.Calendar — this file is commonMain and
// has to compile for the iOS target too.

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

private fun formatDate(millis: Long): String {
    val date = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    return "${date.day} ${MONTHS[date.month.ordinal]} ${date.year}"
}

private fun formatDateTime(millis: Long): String {
    val dt = Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val h = dt.hour.toString().padStart(2, '0')
    val min = dt.minute.toString().padStart(2, '0')
    return "${formatDate(millis)}, $h:$min"
}