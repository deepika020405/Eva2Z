package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriminalVerificationEntryScreen(
    isLoading: Boolean,
    history: List<BgvHistoryEntry> = emptyList(),
    onSubmitNew: (CriminalForm) -> Unit,
    onSearchStatus: (candidateId: String, name: String) -> Unit,
    onDeleteHistory: (candidateId: Int) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CV_Background)
    ) {
        // Custom tab row
        Surface(
            color = CV_Surface,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // New Verification tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 0 },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == 0) CV_Primary else CV_SectionBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (selectedTab == 0) Color.White else CV_TextHint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "New Verification",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedTab == 0) Color.White else CV_TextSec
                        )
                    }
                }

                // Check Status tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = 1 },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == 1) CV_Primary else CV_SectionBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color.White else CV_TextHint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (history.isEmpty()) "Check Status"
                            else "Check Status (${history.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedTab == 1) Color.White else CV_TextSec
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> CriminalVerificationFormScreen(
                isLoading = isLoading,
                onBack = {},
                onSubmit = onSubmitNew
            )

            1 -> CheckStatusTab(
                isLoading = isLoading,
                history = history,
                onSearch = onSearchStatus,
                onDeleteHistory = onDeleteHistory
            )
        }
    }
}

@Composable
fun CheckStatusTab(
    isLoading: Boolean,
    history: List<BgvHistoryEntry> = emptyList(),
    onSearch: (candidateId: String, name: String) -> Unit,
    onDeleteHistory: (candidateId: Int) -> Unit = {}
) {
    var candidateId by rememberSaveable { mutableStateOf("") }
    val isValid = candidateId.isNotBlank()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Info card
            Card(
                colors = CardDefaults.cardColors(containerColor = CV_PrimaryBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CV_Primary.copy(alpha = 0.15f)
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Icon(
                                Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = CV_Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (history.isEmpty())
                            "Enter your Candidate ID to check verification status."
                        else
                            "Pick a verification below, or enter a Candidate ID to check its status.",
                        fontSize = 13.sp,
                        color = CV_TextSec,
                        lineHeight = 18.sp
                    )
                }
            }

            // Input
            OutlinedTextField(
                value = candidateId,
                onValueChange = { candidateId = it.filter { c -> c.isDigit() } },
                label = { Text("Candidate ID") },
                placeholder = { Text("e.g. 361880", color = CV_TextHint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CV_Primary,
                    unfocusedBorderColor = CV_Border,
                    focusedLabelColor = CV_Primary,
                    cursorColor = CV_Primary
                )
            )

            // Search button
            Button(
                onClick = { onSearch(candidateId.trim(), "") },
                enabled = isValid && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CV_Primary,
                    disabledContainerColor = CV_Primary.copy(alpha = 0.4f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Searching...", color = Color.White)
                } else {
                    Icon(
                        Icons.Default.PersonSearch,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Check Status",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (history.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                BgvHistorySection(
                    entries = history,
                    isLoading = isLoading,
                    onCheckStatus = { id -> onSearch(id.toString(), "") },
                    onDelete = onDeleteHistory
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}