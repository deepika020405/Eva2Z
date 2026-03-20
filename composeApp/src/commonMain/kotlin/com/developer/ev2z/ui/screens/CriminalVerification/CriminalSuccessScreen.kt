package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable
fun CriminalSuccessScreen(
    form: CriminalForm?,
    candidateId: Int?,
    status: BgvStatus,
    reportUrl: String?,
    candidateName: String?,
    onDone: () -> Unit,
    onNewVerification: () -> Unit
) {
    val uriHandler  = LocalUriHandler.current
    val displayName = form?.name ?: candidateName

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("Verification Submitted", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Your background verification has been submitted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            StatusBadge(status)
            Spacer(Modifier.height(20.dp))
            VerificationTimeline(status)
            Spacer(Modifier.height(20.dp))

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(20.dp)) {

                    // ── Candidate Details ─────────────────────
                    SectionTitle("Candidate Details")
                    candidateId?.let { InfoRow("Candidate ID", it.toString()) }
                    displayName?.let { InfoRow("Name", it) }
                    Spacer(Modifier.height(16.dp))

                    // ── Personal Info — only own verification ─
                    form?.let {
                        SectionTitle("Personal Information")
                        InfoRow("Email", it.email)
                        InfoRow("Phone", it.phone)
                        InfoRow("Employee ID", it.employeeId)

                        Spacer(Modifier.height(16.dp))

                        SectionTitle("Address Details")
                        InfoRow("House No.", it.houseNo)
                        InfoRow("Colony", it.colony)
                        InfoRow("City", it.city)
                        InfoRow("State", it.state)
                        InfoRow("Pincode", it.pincode)
                        if (it.landmark.isNotBlank()) InfoRow("Landmark", it.landmark)
                        InfoRow("Staying Period", it.stayingPeriod)

                        Spacer(Modifier.height(24.dp))
                    }

                    // ── View Report — only when completed ─────
                    if (status == BgvStatus.COMPLETED && !reportUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick  = { uriHandler.openUri(reportUrl) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("View Full Report")
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // ── Back to Dashboard ─────────────────────
                    Button(
                        onClick  = onDone,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(14.dp)
                    ) { Text("Back to Dashboard") }

                    // ── Start New — only terminal state ───────
                    if (status == BgvStatus.COMPLETED || status == BgvStatus.FAILED) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick  = onNewVerification,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start New Verification")
                        }
                    }
                }
            }
        }
    }
}

// ─── Supporting Composables ───────────────────────────────

@Composable
fun StatusBadge(status: BgvStatus) {
    val (text, color) = when (status) {
        BgvStatus.COMPLETED            -> "✅ Verification Completed" to Color(0xFF16A34A)
        BgvStatus.FAILED               -> "❌ Verification Failed"    to Color(0xFFDC2626)
        BgvStatus.VERIFICATION_STARTED -> "🔄 In Progress"            to Color(0xFF2563EB)
        BgvStatus.CANDIDATE_CREATED    -> "👤 Candidate Created"      to Color(0xFF7C3AED)
        BgvStatus.PAYMENT_DONE         -> "💳 Payment Completed"      to Color(0xFF16A34A)
        else                           -> "📋 Submitted"              to Color(0xFF6B7280)
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(text, color = color,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun VerificationTimeline(status: BgvStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TimelineItem("Form Submitted",         status >= BgvStatus.FORM_SUBMITTED)
        TimelineItem("Payment Completed",      status >= BgvStatus.PAYMENT_DONE)
        TimelineItem("Candidate Created",      status >= BgvStatus.CANDIDATE_CREATED)
        TimelineItem("Verification Started",   status >= BgvStatus.VERIFICATION_STARTED)
        TimelineItem("Verification Completed", status == BgvStatus.COMPLETED)
    }
}

@Composable
fun TimelineItem(title: String, completed: Boolean) {
    val color = if (completed) Color(0xFF16A34A) else Color(0xFF9CA3AF)
    val icon  = if (completed) Icons.Default.CheckCircle else Icons.Default.HourglassTop
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium,
            color = if (completed) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}