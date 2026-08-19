package com.developer.ev2z.ui.screens.AccountActivity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.util.AlertPrefs    // SMS toggle still uses this
//import com.developer.ev2z.util.saveAlertPrefs // SMS toggle still uses this
import kotlinx.coroutines.delay
import com.developer.ev2z.theme.AppPageBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountActivityScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    onBackPressed: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val viewModel = remember { AccountActivityViewModel(apiService, storageManager) }
    val state by viewModel.state.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    // ── ALARM DISABLED ─────────────────────────────────────────────
    // var alarmEnabled by remember { mutableStateOf(AlertPrefs.alarmEnabled) }
    var smsEnabled by remember { mutableStateOf(AlertPrefs.smsEnabled) }

    // ✅ UNCHANGED — Watch for delete success and trigger logout
    LaunchedEffect(state.deleteSuccess) {
        if (state.deleteSuccess) {
            println("✅ AccountActivityScreen: Delete success detected!")
            delay(1500)
            println("🚪 AccountActivityScreen: Closing dialogs...")
            showPasswordDialog = false
            showDeleteDialog = false
            password = ""
            println("🚪 AccountActivityScreen: Calling onAccountDeleted()...")
            onAccountDeleted()
        }
    }

    PlatformBackHandler(enabled = true) {
        onBackPressed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Account Activity",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppPageBg
                )
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppPageBg)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppPageBg)
            ) {
                item {

                    // ─────────────────────────────────────
                    // ✅ NEW — GeoFence Alert Settings
                    // ─────────────────────────────────────
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "GeoFence Alert Settings",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {

                            // ── ALARM DISABLED ─────────────────────────────
                            // AlertToggleItem(
                            //     icon = Icons.Default.Notifications,
                            //     title = "Alarm",
                            //     subtitle = if (alarmEnabled) "Alarm will ring on breach"
                            //                else "Alarm is disabled",
                            //     checked = alarmEnabled,
                            //     onCheckedChange = {
                            //         alarmEnabled = it
                            //         AlertPrefs.alarmEnabled = it
                            //         saveAlertPrefs(AlertPrefs.alarmEnabled, AlertPrefs.smsEnabled)
                            //     }
                            // )
                            // HorizontalDivider(
                            //     modifier = Modifier.padding(horizontal = 16.dp),
                            //     color = Color(0xFFE0E0E0)
                            // )
                            // ───────────────────────────────────────────────

                            // 📩 SMS Toggle
                            AlertToggleItem(
                                icon = Icons.Default.Sms,
                                title = "SMS Alert",
                                subtitle = if (smsEnabled)
                                    "SMS will be sent on breach"
                                else
                                    "SMS alert is disabled",
                                checked = smsEnabled,
                                onCheckedChange = {
                                    smsEnabled = it
                                    AlertPrefs.smsEnabled = it
//                                    saveAlertPrefs(
//                                        false, // alarmEnabled placeholder (alarm disabled)
//                                        AlertPrefs.smsEnabled
//                                    )
                                }
                            )
                        }
                    }

                    // ─────────────────────────────────────
                    // ✅ UNCHANGED — Delete Account
                    // ─────────────────────────────────────
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Delete Account",
                        fontSize = 14.sp,
                        color = Color(0xFFDC3545),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        SettingNavigationItem(
                            icon = Icons.Default.DeleteForever,
                            title = "Delete Account",
                            subtitle = "Permanently delete your account",
                            onClick = { showDeleteDialog = true },
                            iconTint = Color(0xFFDC3545),
                            titleColor = Color(0xFFDC3545)
                        )
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // ✅ UNCHANGED
            FloatingContactButton(
                phoneNumber = "919755273822",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 40.dp)
            )
        }
    }

    // ✅ UNCHANGED — First Dialog - Confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isDeleting) showDeleteDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC3545),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Delete Account", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Are you sure you want to delete your account?")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This will permanently delete:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("• Your profile", fontSize = 13.sp, color = Color.Gray)
                    Text("• All your devices", fontSize = 13.sp, color = Color.Gray)
                    Text("• All your data", fontSize = 13.sp, color = Color.Gray)
                    Text("• Account settings", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This action cannot be undone.",
                        color = Color(0xFFDC3545),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        showPasswordDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC3545)
                    )
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ✅ UNCHANGED — Second Dialog - Password Confirmation
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isDeleting && !state.deleteSuccess) {
                    showPasswordDialog = false
                    password = ""
                    viewModel.clearError()
                }
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC3545),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Confirm Password", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "To delete your account, please confirm your password:",
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            viewModel.clearError()
                        },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isDeleting,
                        singleLine = true,
                        isError = state.errorMessage != null
                    )

                    if (state.deleteSuccess) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "User deleted successfully",
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LaunchedEffect(Unit) {
                            delay(1000)
                            onAccountDeleted()
                        }
                    }

                    state.errorMessage?.let { error ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = Color(0xFFDC3545),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (password.isEmpty()) return@Button
                        println("🗑️ AccountActivityScreen: Delete button clicked")
                        viewModel.deleteAccount(password)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC3545)
                    ),
                    enabled = password.isNotEmpty() && !state.isDeleting && !state.deleteSuccess
                ) {
                    if (state.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.isDeleting) "Deleting..." else "Delete Account")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        password = ""
                        viewModel.clearError()
                    },
                    enabled = !state.isDeleting && !state.deleteSuccess
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ✅ NEW — Alert Toggle Item composable
@Composable
fun AlertToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (checked) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = if (checked) Color(0xFF4CAF50) else Color.Gray
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}

// ✅ UNCHANGED — SettingNavigationItem
@Composable
fun SettingNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = Color(0xFF4CAF50),
    titleColor: Color = Color(0xFF333333)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = Color(0xFF999999)
        )
    }
}