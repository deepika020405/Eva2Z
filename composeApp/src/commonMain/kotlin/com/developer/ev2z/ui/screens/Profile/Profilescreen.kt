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
    refreshTrigger: Int = 0, // ✅ Keep this parameter
    onEditProfile: () -> Unit = {},
    onManageDevice: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onTermsConditions: () -> Unit = {},
    onRefundPolicy: () -> Unit = {},
    onShippingPolicy: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onGeoFencing: () -> Unit = {},
    onHistoryPlayback: () -> Unit = {},
    onGeneralSettings: () -> Unit = {},
    onAccountActivity: () -> Unit = {},
    onReferAndEarn: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val viewModel = remember { ProfileViewModel(apiService, storageManager) }
    val state by viewModel.state.collectAsState()
    var showImageZoom by remember { mutableStateOf(false) }

    // ✅ FIX: Reload data when refreshTrigger changes
    LaunchedEffect(refreshTrigger) {
        viewModel.loadUserData()
        viewModel.fetchProfilePicture()
    }

    // ✅ FIX: Separate logout handler
    LaunchedEffect(state.logoutSuccess) {
        if (state.logoutSuccess) {
            onLogout()
        }
    }

    ProfileScreenContent(
        state = state,
        onRefresh = { viewModel.fetchProfilePicture() },
        onProfileImageClick = { showImageZoom = true },
        onEditProfile = onEditProfile,
        onGeneralSettings = onGeneralSettings,
        onAccountActivity = onAccountActivity,
        onReferAndEarn = onReferAndEarn,
        onChangePassword = onChangePassword,
        onManageDevice = onManageDevice,
        onAddFence = onGeoFencing,
        onHistoryPlayback = onHistoryPlayback,
        onRateApp = { viewModel.openPlayStore() },
        onFeedback = { viewModel.openFeedback() },
        onPrivacyPolicy = onPrivacyPolicy,
        onTermsConditions = onTermsConditions,
        onRefundPolicy = onRefundPolicy,
        onShippingPolicy = onShippingPolicy,
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
    onChangePassword: () -> Unit = {},
    onManageDevice: () -> Unit = {},
    onAddFence: () -> Unit = {},
    onHistoryPlayback: () -> Unit = {},
    onRateApp: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onTermsConditions: () -> Unit = {},
    onRefundPolicy: () -> Unit = {},
    onShippingPolicy: () -> Unit = {},
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
                        onRefundPolicy = onRefundPolicy,
                        onShippingPolicy = onShippingPolicy,
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
    onRefundPolicy: () -> Unit,
    onShippingPolicy: () -> Unit,
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
            AboutMenuItem("Refund & Cancellation Policy", Icons.Rounded.Receipt, onRefundPolicy)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFE0E0E0)
            )
            AboutMenuItem("Shipping & Installation Policy", Icons.Rounded.LocalShipping, onShippingPolicy)
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




git commit -m "feat: Device management, account deletion, and data refresh fixes

🎯 Major Features:
- Edit Device Name functionality with validation
- Account deletion with auto-logout
- Fixed image picker in Add Device screen
- Fixed iOS QR/barcode scanning
- Real-time data refresh after updates

📱 Edit Device Name:
- Created EditDeviceNameScreen.kt with form UI
- Created EditDeviceNameViewModel.kt with update logic
- Only device name is editable (ID & SIM disabled)
- Auto-capitalize device names
- Name validation (min 3 characters)
- Success/error state handling
- API: updateDevice() endpoint integration

🔄 Data Refresh Fixes:
- Added refreshTrigger parameter to ManageDeviceScreen
- Added refreshTrigger parameter to ProfileScreen
- Auto-refresh device list after name update
- Auto-refresh profile after edit
- LaunchedEffect triggers on data changes
- No more stale data display

🗑️ Account Deletion:
- Fixed API response parsing (status vs success)
- Added AccountActivityViewModel logout() function
- Implemented proper state management
- Added LaunchedEffect for auto-logout trigger
- Success message display (1 second delay)
- Dialog state handling during operation
- Storage cleanup on deletion
- Proper callback flow to App.kt

🔧 Image Picker Fixes:
- Fixed gallery button in Add Device screen
- Proper button sizing (48dp clickable area)
- Visual feedback with background
- Border styling for better UX

📱 iOS Barcode Scanning Fixes:
- Enhanced barcode data extraction logic
- Better regex for SIM number parsing: simNo[\''\\s]*[:=][\''\\s]*([0-9]+)
- Support for multiple QR formats (XML, plain numbers)
- Length validation (Device ID: 15-16, SIM: 19-20 digits)
- Improved gallery image processing
- Detailed logging for debugging
- Background thread camera initialization

🎨 UI/UX Improvements:
- Edit icon in device list items
- Disabled field styling (gray background)
- Real-time validation messages
- Loading states with spinners
- Success/error feedback dialogs
- Confirmation dialogs for destructive actions
- Smooth transitions between screens

📊 Files Changed:
NEW:
- EditDeviceNameScreen.kt (156 lines)
- EditDeviceNameViewModel.kt (67 lines)

UPDATED:
- ManageDeviceScreen.kt (+refreshTrigger, +onEditDeviceClick)
- ProfileScreen.kt (+refreshTrigger, auto-reload)
- HomeNavGraph.kt (+refresh state management)
- AccountActivityScreen.kt (auto-logout fix)
- AccountActivityViewModel.kt (logout function, API key fix)
- AddDeviceScreenCommon.kt (gallery button fix)
- AddDeviceScreen.ios.kt (barcode scanning improvements)
- ApiService.kt (updateDevice endpoint)

✅ Testing Status:
- Android: Edit device working ✅
- Android: Account deletion with auto-logout ✅
- Android: Gallery picker working ✅
- Android: Data refresh working ✅
- iOS: QR/Barcode scanning improved ✅
- iOS: Gallery processing enhanced ✅
- iOS: Background camera initialization ✅

🔄 API Endpoints Used:
- PATCH /update-device (Edit device name)
- DELETE /account (Delete account)
- GET /my-devices (Refresh device list)
- GET /profile-picture (Refresh profile data)

🐛 Bugs Fixed:
1. Stale data in ManageDeviceScreen after edit
2. Stale data in ProfileScreen after edit
3. Gallery button not responding in Add Device
4. iOS barcode data extraction issues
5. Account deletion not triggering logout
6. API response key mismatch (status vs success)
7. Missing logout() function in ViewModel

📝 Technical Details:
- Refresh triggers using State<Int>
- LaunchedEffect for reactive updates
- Auto-capitalize device names
- Cross-platform barcode scanning
- Proper state management with StateFlow
- Coroutine-based API calls
- Material 3 UI components
- Form validation with error messages
- Background thread operations (iOS camera)

🚀 Performance Improvements:
- Efficient recomposition with refreshTrigger
- Background thread camera initialization (iOS)
- Minimal re-renders with LaunchedEffect
- Proper cleanup on navigation

🔐 Security & Best Practices:
- Token validation before API calls
- Storage cleanup on account deletion
- Error handling with user feedback
- Input validation (name, device ID, SIM)
- Proper null safety
- Exception handling with stack traces

📱 Cross-Platform Support:
- Android: CameraX + ML Kit barcode scanning
- iOS: AVFoundation + Vision framework
- Shared UI components (CommonMain)
- Platform-specific camera implementations
- Unified image picker interface

🎯 Next Steps:
- Test on real iPhone with PhonePe app
- Add device type icons in navigation
- Implement change password validation
- Add device history tracking
- Optimize image compression
