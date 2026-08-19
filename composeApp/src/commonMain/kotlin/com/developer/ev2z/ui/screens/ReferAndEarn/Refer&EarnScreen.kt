package com.developer.ev2z.ui.screens.ReferAndEarn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.developer.ev2z.getPlatformContext
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.theme.AppPageBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferAndEarnScreen(
    token: String,
    apiService: ApiService,
    onBackPressed: () -> Unit,
    onNavigateToShop: () -> Unit = {}, // ✅ Navigate to shop tab
    onNavigateToAddDevice: () -> Unit = {} // ✅ Navigate to actual add device flow
) {
    val viewModel = remember { ReferAndEarnViewModel(apiService) }
    val uiState by viewModel.uiState.collectAsState()
    val showCopySnackbar by viewModel.showCopySnackbar.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val context = getPlatformContext()

    var showBalanceDialog by remember { mutableStateOf(false) } // ✅ Changed from Coming Soon
    var showContactPicker by remember { mutableStateOf(false) } // ✅ For contacts
    var showNoDeviceDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Load referral code on first composition
    LaunchedEffect(Unit) {
        viewModel.loadReferralCode(token)
    }

    // Show copy snackbar
    LaunchedEffect(showCopySnackbar) {
        if (showCopySnackbar) {
            snackbarHostState.showSnackbar(
                message = "✅ Referral code copied!",
                duration = SnackbarDuration.Short
            )
            viewModel.resetCopySnackbar()
        }
    }

    PlatformBackHandler(enabled = true) {
        onBackPressed()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
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
        ) {
            when (val state = uiState) {
                is ReferralUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF6A9F50)
                        )
                    }
                }

                is ReferralUiState.Success -> {
                    ReferralContentScreen(
                        referralCode = state.referralCode,
                        referralAmount = state.referralAmount,
                        referralBalance = state.referralBalance,
                        onCopyCode = {
                            clipboardManager.setText(AnnotatedString(state.referralCode))
                            viewModel.showCopySuccess()
                        },
                        onShareWhatsApp = {
                            shareViaWhatsApp(context, viewModel.getWhatsAppMessage(state.referralCode, state.referralAmount))
                        },
                        onShareOther = {
                            shareViaOther(context, viewModel.getReferralMessage(state.referralCode, state.referralAmount))
                        },
                        onShowBalance = {
                            showBalanceDialog = true // ✅ Show balance dialog
                        },
                        onInviteContacts = {
                            showContactPicker = true // ✅ Open contact picker
                        }
                    )
                }

                is ReferralUiState.NoDevice -> {
                    ReferralContentScreen(
                        referralCode = "XXXXXXXX",
                        referralAmount = 150,
                        referralBalance = "0",
                        onCopyCode = {
                            showNoDeviceDialog = true
                        },
                        onShareWhatsApp = {
                            showNoDeviceDialog = true
                        },
                        onShareOther = {
                            showNoDeviceDialog = true
                        },
                        onShowBalance = {
                            showNoDeviceDialog = true
                        },
                        onInviteContacts = {
                            showNoDeviceDialog = true
                        },
                        isDisabled = false // ✅ Keep everything enabled, just show dialog
                    )
                }

                is ReferralUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("❌", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Something went wrong",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = onBackPressed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6A9F50)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }



            // ✅ Balance Dialog (shows referral balance)
            if (showBalanceDialog) {
                val state = uiState as? ReferralUiState.Success
                state?.let {
                    ReferralBalanceDialog(
                        balance = it.referralBalance,
                        onDismiss = { showBalanceDialog = false }
                    )
                }
            }

            // ✅ Contact Picker
            if (showContactPicker) {
                val state = uiState as? ReferralUiState.Success
                state?.let {
                    LaunchedEffect(Unit) {
                        openContactPicker(
                            context = context,
                            message = viewModel.getReferralMessage(it.referralCode, it.referralAmount)
                        )
                        showContactPicker = false
                    }
                }
            }

            // ✅ No Device Dialog
            if (showNoDeviceDialog) {
                NoDeviceDialog(
                    onDismiss = { showNoDeviceDialog = false },
                    onAddDevice = {
                        showNoDeviceDialog = false
                        onNavigateToAddDevice() // ✅ Open actual add device flow
                    },
                    onGoToShop = {
                        showNoDeviceDialog = false
                        onNavigateToShop() // ✅ Navigate to shop tab
                    }
                )
            }
        }
    }
}

@Composable
private fun ReferralContentScreen(
    referralCode: String,
    referralAmount: Int,
    referralBalance: String,
    onCopyCode: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareOther: () -> Unit,
    onShowBalance: () -> Unit, // ✅ Changed from onShowComingSoon
    onInviteContacts: () -> Unit, // ✅ New callback
    isDisabled: Boolean = false
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 88.dp)
                .background(AppPageBg)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Refer & Earn\nRewards",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Illustration
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF9E6)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎁", fontSize = 80.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Offer Text
            Text(
                text = "Get ₹$referralAmount OFF",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
//            Text(
//                text = "refer your friends and get ₹$referralAmount off coupon\nfor each successful referral.",
//                style = MaterialTheme.typography.bodyMedium,
//                color = Color.Gray,
//                fontSize = 14.sp,
//                textAlign = TextAlign.Center,
//                lineHeight = 20.sp
//            )


            Text(
                text = if (isDisabled)
                    "Refer your friends and get ₹$referralAmount off coupon\nfor each successful referral."
                else
                    "Please Purchase a device first than you will be eligible for referral",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDisabled) Color(0xFF6A9F50) else Color.Black,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                fontWeight = if (isDisabled) FontWeight.Medium else FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Referral Code Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Code Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isDisabled) Color(0xFFEEEEEE) else Color(0xFFF5F5F5),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = referralCode,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDisabled) Color.Gray else Color.Black,
                        fontSize = 18.sp
                    )
                }

                // Copy Button
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(
                            if (isDisabled) Color(0xFFEEEEEE) else Color(0xFFF5F5F5),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = !isDisabled, onClick = onCopyCode)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = if (isDisabled) Color.Gray else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDisabled) Color.Gray else Color.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ✅ Invite from contacts - Opens contact picker
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isDisabled) Color(0xFFEEEEEE) else Color(0xFFF5F5F5),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(
                            enabled = !isDisabled,
                            onClick = onInviteContacts
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Invite from\ncontacts",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDisabled) Color.Gray else Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.NavigateNext,
                            contentDescription = null,
                            tint = if (isDisabled) Color.Gray else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("👥", fontSize = 60.sp)
                }

                // ✅ Show referral balance - Opens balance dialog
//                Column(
//                    modifier = Modifier
//                        .weight(1f)
//                        .background(
//                            if (isDisabled) Color(0xFFEEEEEE) else Color(0xFFF5F5F5),
//                            RoundedCornerShape(12.dp)
//                        )
//                        .clickable(
//                            enabled = !isDisabled,
//                            onClick = onShowBalance
//                        )
//                        .padding(16.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text(
//                            text = "Show referral\nbalance",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = if (isDisabled) Color.Gray else Color.Black,
//                            fontSize = 13.sp,
//                            fontWeight = FontWeight.Medium,
//                            lineHeight = 18.sp,
//                            modifier = Modifier.weight(1f)
//                        )
//                        Icon(
//                            Icons.Default.NavigateNext,
//                            contentDescription = null,
//                            tint = if (isDisabled) Color.Gray else Color.Black,
//                            modifier = Modifier.size(20.dp)
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    Text("💸", fontSize = 60.sp)
//                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // How it Works Section
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "How it Works",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                StepItem(number = "1", text = "Invite a friend")
                Spacer(modifier = Modifier.height(12.dp))
                StepItem(number = "2", text = "They sign up")
                Spacer(modifier = Modifier.height(12.dp))
                StepItem(number = "3", text = "First order placed")
                Spacer(modifier = Modifier.height(12.dp))
                StepItem(number = "4", text = "Both get ₹$referralAmount reward")

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⭐", fontSize = 16.sp)
                    Text(
                        text = "You must have at least 1 Device to share",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6A9F50),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom Buttons - Fixed at bottom
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Share via WhatsApp Button
                Button(
                    onClick = onShareWhatsApp,
                    enabled = !isDisabled,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        disabledContainerColor = Color(0xFFCCCCCC)
                    ),
                    shape = RoundedCornerShape(36.dp)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Share via WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WhatsApp",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Share Icon Button
                IconButton(
                    onClick = onShareOther,
                    enabled = !isDisabled,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (isDisabled) Color(0xFFEEEEEE) else Color(0xFFF5F5F5),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = if (isDisabled) Color.Gray else Color(0xFF6A9F50),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

        }
    }
}

@Composable
private fun StepItem(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFE3F1DC)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 16.sp
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
            fontSize = 14.sp
        )
    }
}

// ✅ NEW: Referral Balance Dialog
@Composable
private fun ReferralBalanceDialog(
    balance: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F1DC)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💰", fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Your Referral Balance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Balance Amount
                Text(
                    text = "₹$balance",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6A9F50),
                    fontSize = 36.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Keep referring friends to earn more rewards!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A9F50)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Close",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ✅ UPDATED: No Device Dialog with Shop navigation
@Composable
private fun NoDeviceDialog(
    onDismiss: () -> Unit,
    onAddDevice: () -> Unit,
    onGoToShop: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Referral Eligibility Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Please Purchase a device first than you will be eligible for referral.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.DarkGray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

//                Spacer(modifier = Modifier.height(24.dp))

//                Text(
//                    text = "To refer friends, you must have at least 1 active device on your account.",
//                    style = MaterialTheme.typography.bodyLarge,
//                    color = Color.DarkGray,
//                    fontSize = 15.sp,
//                    textAlign = TextAlign.Center,
//                    lineHeight = 22.sp
//                )

//                Spacer(modifier = Modifier.height(12.dp))
//
//                Text(
//                    text = "Referrals are available only after the minimum device requirement is met.",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = Color.Gray,
//                    fontSize = 14.sp,
//                    textAlign = TextAlign.Center,
//                    lineHeight = 20.sp
//                )
//
                Spacer(modifier = Modifier.height(32.dp))

                // Add Device Button
                Button(
                    onClick = onGoToShop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A9F50)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Go to Shop",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

//                TextButton(onClick = onAddDevice) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        Text(
//                            text = "Add Device",
//                            color = Color(0xFF6A9F50),
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Medium
//                        )
//                    }
//                }
            }
        }
    }
}

// Platform-specific sharing functions
expect fun shareViaWhatsApp(context: Any?, message: String)
expect fun shareViaOther(context: Any?, message: String)
expect fun openContactPicker(context: Any?, message: String) // ✅ New function