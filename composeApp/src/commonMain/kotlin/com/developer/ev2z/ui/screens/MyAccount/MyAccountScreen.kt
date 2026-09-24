package com.developer.ev2z.ui.screens.MyAccount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.ui.screens.Profile.ProfileMenuItemRow
import kotlinx.coroutines.launch
import com.developer.ev2z.theme.AppPageBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAccountScreen(
    selectedDeviceId: String,
    onBack: () -> Unit,
    onChangePlan: () -> Unit,
    onBillDetails: () -> Unit,
    onWarranty: (String) -> Unit,
    onServiceComplaints: () -> Unit = {}
) {
    PlatformBackHandler(true) {
        onBack()
    }

    // 🔹 Snackbar for user feedback
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Account", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppPageBg
                )
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppPageBg)
        ) {

            // 🔹 Plan & Billing Section
            item {
                AccountCard(
                    "Plan & Billing",
                    listOf(
                        "Plans & Subscriptions" to onChangePlan,
                        "Bill Details" to onBillDetails
                    )
                )
            }

            // 🔹 Device & Warranty Section
            item {
                AccountCard(
                    "Device & Warranty",
                    listOf(
                        "Warranty Information" to {
                            if (selectedDeviceId.isNotBlank()) {
                                // ✅ Device exists → open warranty
                                onWarranty(selectedDeviceId)
                            } else {
                                // ❌ No device → show message only
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "No device added. Please add a device first."
                                    )
                                }
                            }
                        },
                        // Service & Warranty complaint flow — raise a component
                        // complaint and track it through warranty + allocation.
                        "Service & Complaints" to onServiceComplaints
                    )
                )
            }
        }
    }
}

@Composable
fun AccountCard(
    title: String,
    items: List<Pair<String, () -> Unit>>
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    ProfileMenuItemRow(
                        title = item.first,
                        icon = Icons.Rounded.ChevronRight,
                        onClick = item.second
                    )
                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }
            }
        }
    }
}