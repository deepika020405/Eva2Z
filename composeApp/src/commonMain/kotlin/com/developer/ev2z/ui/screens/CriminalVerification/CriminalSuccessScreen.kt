package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriminalSuccessScreen(
    form: CriminalForm?,
    candidateId: Int?,
    status: BgvStatus,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp) // ✅ Tablet friendly
                .fillMaxWidth()
                .padding(20.dp)
        ) {

            // 🔹 HEADER
            Text(
                text = "Verification Submitted",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Your background verification is in progress.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // 🔹 STATUS BADGE
            StatusBadge(status)

            Spacer(Modifier.height(20.dp))

            // 🔹 TIMELINE
            VerificationTimeline(status)

            Spacer(Modifier.height(20.dp))

            // 🔹 DETAILS CARD
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(20.dp)) {

                    candidateId?.let {
                        SectionTitle("Candidate Details")
                        InfoRow("Candidate ID", it.toString())
                        Spacer(Modifier.height(16.dp))
                    }

                    form?.let {
                        SectionTitle("Personal Information")
                        InfoRow("Name", it.name)
                        InfoRow("Email", it.email)
                        InfoRow("Phone", it.phone)

                        Spacer(Modifier.height(16.dp))

                        SectionTitle("Address Details")
                        InfoRow("House No.", it.houseNo)
                        InfoRow("Colony", it.colony)
                        InfoRow("City", it.city)
                        InfoRow("State", it.state)
                        InfoRow("Pincode", it.pincode)
                        InfoRow("Landmark", it.landmark)
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Back to Dashboard")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: BgvStatus) {
    val (text, color) = when (status) {
        BgvStatus.COMPLETED -> "Verification Completed" to Color(0xFF16A34A)
        BgvStatus.FAILED -> "Verification Failed" to Color(0xFFDC2626)
        BgvStatus.VERIFICATION_STARTED -> "Verification in Progress" to Color(0xFF2563EB)
        BgvStatus.CANDIDATE_CREATED -> "Candidate Created" to Color(0xFF7C3AED)
        BgvStatus.PAYMENT_DONE -> "Payment Completed" to Color(0xFF16A34A)
        else -> "Submitted" to Color(0xFF6B7280)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            color = color,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun VerificationTimeline(status: BgvStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TimelineItem("Form Submitted", status >= BgvStatus.FORM_SUBMITTED)
        TimelineItem("Payment Completed", status >= BgvStatus.PAYMENT_DONE)
        TimelineItem("Candidate Created", status >= BgvStatus.CANDIDATE_CREATED)
        TimelineItem("Verification Started", status >= BgvStatus.VERIFICATION_STARTED)
    }
}

@Composable
fun TimelineItem(title: String, completed: Boolean) {

    val color = if (completed) Color(0xFF16A34A) else Color(0xFF9CA3AF)
    val icon = if (completed) Icons.Default.CheckCircle else Icons.Default.HourglassTop

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

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (completed) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}