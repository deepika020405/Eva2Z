package com.developer.ev2z.ui.screens.Profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService

@Composable
fun ProfileScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val viewModel = remember { ProfileViewModel(apiService, storageManager) }
    val state by viewModel.state.collectAsState()

    var showImageZoom by remember { mutableStateOf(false) }

    // Handle logout navigation
    LaunchedEffect(state.logoutSuccess) {
        viewModel.loadUserData()
        if (state.logoutSuccess) {
            onLogout()
        }
    }

    
    ProfileScreenContent(
        state = state,
        onRefresh = { viewModel.fetchProfilePicture() },
        onProfileImageClick = { showImageZoom = true },
        onEditProfile = onEditProfile,
        onGeneralSettings = { /* Navigate to SettingActivity */ },
        onAccountActivity = { /* Navigate to account activity */ },
        onReferAndEarn = { /* Navigate to ReferAndEarnActivity */ },
        onChangePassword = { /* Navigate to PasswordChangeActivity */ },
        onManageDevice = { /* Navigate to ManageDevicesActivity */ },
        onAddFence = { /* Navigate to FenceActivity */ },
        onHistoryPlayback = { /* Navigate to HistoryPlaybackActivity */ },
        onRateApp = { /* Open Play Store */ },
        onFeedback = { /* Open feedback */ },
        onPrivacyPolicy = { /* Navigate to TermsConditionActivity with PrivacyPolicy */ },
        onTermsConditions = { /* Navigate to TermsConditionActivity */ },
        onContactUs = { /* WhatsApp support */ },
        onLogout = { viewModel.logout() },
        onDismissError = { viewModel.clearError() }
    )

    // ✅ Image Zoom Dialog
    if (showImageZoom && !state.userImage.isNullOrEmpty()) {
        ProfileImageZoomDialog(
            imageUrl = state.userImage!!,
            userName = state.userName,
            onDismiss = { showImageZoom = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    state: ProfileState,
    onRefresh: () -> Unit = {},
    onProfileImageClick: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onGeneralSettings: () -> Unit = {},
    onAccountActivity: () -> Unit = {},
    onReferAndEarn: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onManageDevice: () -> Unit = {},
    onAddFence: () -> Unit = {},
    onHistoryPlayback: () -> Unit = {},
    onRateApp: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onTermsConditions: () -> Unit = {},
    onContactUs: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Header
                item {
                    ProfileHeader(
                        userName = state.userName,
                        userImage = state.userImage,
                        onImageClick = onProfileImageClick
                    )
                }

                // Profile Settings Section
                item {
                    Text(
                        text = "Profile Settings",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }

                item {
                    ProfileMenuCard(
                        items = listOf(
                            ProfileMenuItem("Edit Profile", "edit_profile", onEditProfile),
                            ProfileMenuItem("General Settings", "settings", onGeneralSettings),
                            ProfileMenuItem("Account Activity", "activity", onAccountActivity),
                            ProfileMenuItem("Refer And Earn", "refer", onReferAndEarn),
                            ProfileMenuItem("Change Password", "password", onChangePassword)
                        )
                    )
                }

                // Device Settings Section
                item {
                    Text(
                        text = "Device Settings",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }

                item {
                    ProfileMenuCard(
                        items = listOf(
                            ProfileMenuItem("Manage Device", "device", onManageDevice),
                            ProfileMenuItem("Add Fence", "fence", onAddFence),
                            ProfileMenuItem("History Playback", "history", onHistoryPlayback)
                        )
                    )
                }

                // About A2Z Section
                item {
                    Text(
                        text = "About A2Z",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                }

                item {
                    AboutSection(
                        onRateApp = onRateApp,
                        onFeedback = onFeedback,
                        onPrivacyPolicy = onPrivacyPolicy,
                        onTermsConditions = onTermsConditions,
                        onContactUs = onContactUs
                    )
                }

                // Logout Button
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text(
                            text = "Log Out",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // Loading indicator
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // Error Snackbar
    state.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or toast
            onDismissError()
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Yes", color = Color(0xFF4CAF50))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}

// ✅ Profile Header with clickable image
@Composable
fun ProfileHeader(
    userName: String,
    userImage: String?,
    onImageClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ✅ Profile Image (Clickable to zoom)
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0E0E0))
                .clickable { onImageClick() },
            contentAlignment = Alignment.Center
        ) {
            if (!userImage.isNullOrEmpty()) {
                AsyncImage(
                    model = userImage,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = userName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
    }
}

// ✅ Profile Image Zoom Dialog
@Composable
fun ProfileImageZoomDialog(
    imageUrl: String,
    userName: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoomed Image
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Profile Picture Zoomed",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                // User Name
                Text(
                    text = userName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

data class ProfileMenuItem(
    val title: String,
    val icon: String,
    val onClick: () -> Unit
)

@Composable
fun ProfileMenuCard(
    items: List<ProfileMenuItem>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            items.forEachIndexed { index, item ->
                ProfileMenuItemRow(
                    title = item.title,
                    onClick = item.onClick
                )

                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color(0xFFE0E0E0)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileMenuItemRow(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF666666))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            color = Color(0xFF333333),
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Arrow",
            tint = Color(0xFF999999)
        )
    }
}

@Composable
fun AboutSection(
    onRateApp: () -> Unit,
    onFeedback: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onTermsConditions: () -> Unit,
    onContactUs: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AboutMenuItem("Rate EVA2Z On Play Store", onRateApp)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFE0E0E0)
            )
            AboutMenuItem("Feedback", onFeedback)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFE0E0E0)
            )
            AboutMenuItem("Privacy Policy", onPrivacyPolicy)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFE0E0E0)
            )
            AboutMenuItem("Terms & Conditions", onTermsConditions)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFE0E0E0)
            )
            AboutMenuItem("Contact Us", onContactUs)
        }
    }
}

@Composable
fun AboutMenuItem(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Arrow",
            tint = Color(0xFF999999),
            modifier = Modifier.size(20.dp)
        )
    }
}