// ProfileScreen.kt - COMPLETE FIXED VERSION
package com.developer.ev2z.ui.screens.Profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    onManageDevice: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onTermsConditions: () -> Unit = {},
    onChangePassword: () -> Unit = {}, // ✅ Parameter exists
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
        onChangePassword = onChangePassword, // ✅ PASS IT TO CONTENT
        onManageDevice = onManageDevice,
        onAddFence = { /* Navigate to FenceActivity */ },
        onHistoryPlayback = { /* Navigate to HistoryPlaybackActivity */ },
        onRateApp = { viewModel.openPlayStore() },
        onFeedback = { viewModel.openFeedback() },
        onPrivacyPolicy = onPrivacyPolicy,
        onTermsConditions = onTermsConditions,
        onContactUs = { viewModel.openWhatsApp() },
        onLogout = { viewModel.logout() },
        onDismissError = { viewModel.clearError() }
    )

    // Image Zoom Dialog
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
    onChangePassword: () -> Unit = {}, // ✅ RECEIVE CALLBACK
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
                item {
                    ProfileHeader(
                        userName = state.userName,
                        userImage = state.userImage,
                        onImageClick = onProfileImageClick
                    )
                }

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
                            ProfileMenuItem("Edit Profile", Icons.Rounded.Edit, onEditProfile),
                            ProfileMenuItem("General Settings", Icons.Rounded.Settings, onGeneralSettings),
                            ProfileMenuItem("Account Activity", Icons.Rounded.ManageAccounts, onAccountActivity),
                            ProfileMenuItem("Refer And Earn", Icons.Rounded.CardGiftcard, onReferAndEarn),
                            ProfileMenuItem("Change Password", Icons.Rounded.Password, onChangePassword) // ✅ WORKING NOW
                        )
                    )
                }

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
                            ProfileMenuItem("Manage Device", Icons.Rounded.Devices, onManageDevice),
                            ProfileMenuItem("Manage Geo Fence", Icons.Rounded.LocationOn, onAddFence),
                            ProfileMenuItem("History Playback", Icons.Rounded.History, onHistoryPlayback)
                        )
                    )
                }

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

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    state.error?.let { error ->
        LaunchedEffect(error) {
            onDismissError()
        }
    }

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

                Text(
                    text = userName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

data class ProfileMenuItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun ProfileMenuCard(items: List<ProfileMenuItem>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            items.forEachIndexed { index, item ->
                ProfileMenuItemRow(
                    title = item.title,
                    icon = item.icon,
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
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AboutMenuItem("Rate EVA2Z On Play Store", Icons.Rounded.Star, onRateApp)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFE0E0E0)
            )
            AboutMenuItem("Feedback", Icons.Rounded.Feedback, onFeedback)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFE0E0E0)
            )
            AboutMenuItem("Privacy Policy", Icons.Rounded.PrivacyTip, onPrivacyPolicy)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFE0E0E0)
            )
            AboutMenuItem("Terms & Conditions", Icons.Rounded.Description, onTermsConditions)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFE0E0E0)
            )
            AboutMenuItem("Contact Us", Icons.Rounded.Support, onContactUs)
        }
    }
}

@Composable
fun AboutMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

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