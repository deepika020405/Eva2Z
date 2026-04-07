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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CriminalSuccessScreen(
    form: CriminalForm?,
    candidateId: Int?,
    status: BgvStatus,
    reportUrl: String?,
    candidateName: String?,
    courtResult: CourtCheckResult?,
    onDone: () -> Unit,
    onNewVerification: () -> Unit
) {
    val displayName = form?.name ?: candidateName

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CV_Background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "Verification Result",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CV_TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Background verification report",
                fontSize = 14.sp,
                color = CV_TextSec
            )

            Spacer(Modifier.height(16.dp))
            StatusBadge(status)
            Spacer(Modifier.height(18.dp))
            VerificationTimeline(status)
            Spacer(Modifier.height(18.dp))

            // Criminal Check Result Card
            if (status == BgvStatus.COMPLETED && courtResult != null) {
                CriminalCheckCard(courtResult = courtResult, candidateName = displayName)
                Spacer(Modifier.height(14.dp))
            }

            // Candidate Details
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CV_Surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    SectionTitle("Candidate Details")
                    candidateId?.let { InfoRow("Candidate ID", it.toString()) }
                    displayName?.let { InfoRow("Name", it) }

                    form?.let {
                        Spacer(Modifier.height(14.dp))
                        SectionTitle("Personal Information")
                        InfoRow("Email", it.email)
                        InfoRow("Phone", it.phone)
                        InfoRow("Employee ID", it.employeeId)

                        Spacer(Modifier.height(14.dp))
                        SectionTitle("Address Details")
                        InfoRow("House No.", it.houseNo)
                        InfoRow("Colony", it.colony)
                        InfoRow("City", it.city)
                        InfoRow("State", it.state)
                        InfoRow("Pincode", it.pincode)
                        if (it.landmark.isNotBlank()) InfoRow("Landmark", it.landmark)
                        InfoRow("Staying Period", it.stayingPeriod)
                        Spacer(Modifier.height(20.dp))
                    }

                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CV_Primary)
                    ) {
                        Text(
                            "Back to Dashboard",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (status == BgvStatus.COMPLETED || status == BgvStatus.FAILED) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onNewVerification,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CV_Primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Start New Verification",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Criminal Check Result Card ──────────────────────────────────
@Composable
fun CriminalCheckCard(courtResult: CourtCheckResult, candidateName: String?) {
    val isClean = courtResult.checkStatusCode == 1
    val mainColor = if (isClean) CV_Success else CV_Error
    val bgColor = mainColor.copy(alpha = 0.06f)
    val verdict = if (isClean) "No Criminal Record Found" else "Criminal Record Detected"
    val subtext = if (isClean)
        "This individual has no criminal history on record."
    else
        "A criminal or court record has been found for this individual."

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CV_Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Verdict icon
            Surface(
                shape = CircleShape,
                color = mainColor.copy(alpha = 0.12f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        if (isClean) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = mainColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = verdict,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = mainColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtext,
                fontSize = 13.sp,
                color = CV_TextSec,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = mainColor.copy(alpha = 0.15f))
            Spacer(Modifier.height(14.dp))

            // Details
            candidateName?.let {
                ResultDetailRow(label = "Candidate", value = it)
            }
            ResultDetailRow(label = "Verification Type", value = "Court Record Check")
            ResultDetailRow(
                label = "Address Verified",
                value = "${courtResult.city}, ${courtResult.state}"
            )
            if (courtResult.completionDate.isNotBlank()) {
                ResultDetailRow(label = "Verified On", value = courtResult.completionDate)
            }
            ResultDetailRow(
                label = "Status",
                value = courtResult.checkStatus,
                valueColor = mainColor
            )
        }
    }
}

@Composable
fun ResultDetailRow(
    label: String,
    value: String,
    valueColor: Color = CV_TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = CV_TextHint,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            modifier = Modifier.weight(1.5f),
            textAlign = TextAlign.End
        )
    }
}

// ── Supporting Composables ──────────────────────────────────────

@Composable
fun StatusBadge(status: BgvStatus) {
    val (text, color) = when (status) {
        BgvStatus.COMPLETED -> "Verification Completed" to CV_Success
        BgvStatus.FAILED -> "Verification Failed" to CV_Error
        BgvStatus.VERIFICATION_STARTED -> "In Progress" to CV_Info
        BgvStatus.CANDIDATE_CREATED -> "Candidate Created" to Color(0xFF7C3AED)
        BgvStatus.PAYMENT_DONE -> "Payment Completed" to CV_Success
        else -> "Submitted" to CV_TextHint
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = CV_TextPrimary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            label,
            fontSize = 11.sp,
            color = CV_TextHint
        )
        Text(
            value,
            fontSize = 15.sp,
            color = CV_TextPrimary
        )
    }
}

@Composable
fun VerificationTimeline(status: BgvStatus) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CV_Surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Progress",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CV_TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            TimelineItem("Form Submitted", status >= BgvStatus.FORM_SUBMITTED)
            TimelineItem("Payment Completed", status >= BgvStatus.PAYMENT_DONE)
            TimelineItem("Candidate Created", status >= BgvStatus.CANDIDATE_CREATED)
            TimelineItem("Verification Started", status >= BgvStatus.VERIFICATION_STARTED)
            TimelineItem("Verification Completed", status == BgvStatus.COMPLETED)
        }
    }
}

@Composable
fun TimelineItem(title: String, completed: Boolean) {
    val color = if (completed) CV_Success else CV_TextHint
    val icon = if (completed) Icons.Default.CheckCircle else Icons.Default.HourglassTop

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.size(30.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = if (completed) FontWeight.Medium else FontWeight.Normal,
            color = if (completed) CV_TextPrimary else CV_TextHint
        )
    }
}