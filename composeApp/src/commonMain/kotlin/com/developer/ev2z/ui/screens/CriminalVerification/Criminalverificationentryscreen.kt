package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriminalVerificationEntryScreen(
    isLoading: Boolean,
    onSubmitNew: (CriminalForm) -> Unit,
    onSearchStatus: (candidateId: String, name: String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick  = { selectedTab = 0 },
                text     = { Text("New Verification", fontWeight = FontWeight.Medium) },
                icon     = { Icon(Icons.Default.VerifiedUser, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick  = { selectedTab = 1 },
                text     = { Text("Check Status", fontWeight = FontWeight.Medium) },
                icon     = { Icon(Icons.Default.PersonSearch, contentDescription = null) }
            )
        }

        when (selectedTab) {
            0 -> CriminalVerificationFormScreen(
                isLoading = isLoading,
                onBack    = {},
                onSubmit  = onSubmitNew
            )
            1 -> CheckStatusTab(
                isLoading = isLoading,
                onSearch  = onSearchStatus
            )
        }
    }
}

@Composable
fun CheckStatusTab(
    isLoading: Boolean,
    onSearch: (candidateId: String, name: String) -> Unit
) {
    var candidateId by rememberSaveable { mutableStateOf("") }

    val isValid = candidateId.isNotBlank()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Enter your Candidate ID to check verification status.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            OutlinedTextField(
                value         = candidateId,
                onValueChange = { candidateId = it.filter { c -> c.isDigit() } },
                label         = { Text("Candidate ID") },
                placeholder   = { Text("e.g. 361880") },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape         = RoundedCornerShape(12.dp),
                modifier      = Modifier.fillMaxWidth()
            )

            Button(
                onClick  = { onSearch(candidateId.trim(), "") },
                enabled  = isValid && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Searching...")
                } else {
                    Icon(Icons.Default.PersonSearch, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Check Status")
                }
            }
        }
    }
}