package com.developer.ev2z.ui.screens.Profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
// import androidx.compose.material.icons.filled.DarkMode   // NEW UI
// import androidx.compose.material.icons.filled.LightMode   // NEW UI
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.developer.ev2z.data.local.StorageKeys
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.ui.screens.MyAccount.MyAccountScreen
import com.developer.ev2z.ui.screens.PrivacyPolicy_TandC.PrivacyPolicy_TandCScreen
import com.developer.ev2z.ui.screens.PrivacyPolicy_TandC.WebViewType
// import com.developer.ev2z.ui.screens.homeScreen.LocalHomeTheme  // NEW UI
import com.developer.ev2z.ui.screens.social_media.openUrl
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.ic_facebook
import eva2z_connect_multiplatform.composeapp.generated.resources.ic_instagram
import eva2z_connect_multiplatform.composeapp.generated.resources.ic_linkedin
import eva2z_connect_multiplatform.composeapp.generated.resources.ic_twitter
import eva2z_connect_multiplatform.composeapp.generated.resources.ic_youtube
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.developer.ev2z.theme.AppPageBg

@Composable
fun ProfileScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    refreshTrigger: Int = 0,
    onEditProfile: () -> Unit = {},
    onManageDevice: () -> Unit = {},
    onGpsData: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onTermsConditions: () -> Unit = {},
    onRefundPolicy: () -> Unit = {},
    onShippingPolicy: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onNavigateMyAccount: () -> Unit,
    onGeoFencing: () -> Unit = {},
    onHistoryPlayback: () -> Unit = {},
    onGeneralSettings: () -> Unit = {},
    onAccountActivity: () -> Unit = {},
    onReferAndEarn: () -> Unit = {},
    onAboutApp: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenDocumentsVault: () -> Unit = {},
) {
    val viewModel = remember { ProfileViewModel(apiService, storageManager) }
    var showVaultPasswordDialog by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()
    var showImageZoom by remember { mutableStateOf(false) }
    var webViewType by remember { mutableStateOf<WebViewType?>(null) }

    LaunchedEffect(refreshTrigger) {
        viewModel.loadUserData()
        viewModel.fetchProfilePicture()
    }

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
        onGPSData = onGpsData,
        onAddFence = onGeoFencing,
        onNavigateMyAccount = onNavigateMyAccount,
        onHistoryPlayback = onHistoryPlayback,
        onRateApp = { viewModel.openPlayStore() },
        onFeedback = { viewModel.openFeedback() },
        onPrivacyPolicy = onPrivacyPolicy,
        onTermsConditions = onTermsConditions,
        onRefundPolicy = onRefundPolicy,
        onShippingPolicy = onShippingPolicy,
        onContactUs = { viewModel.openWhatsApp() },
        onAboutApp = onAboutApp,
        onLogout = { viewModel.logout() },
        onDismissError = { viewModel.clearError() },
        onFacebook = { webViewType = WebViewType.FACEBOOK },
        onInstagram = { webViewType = WebViewType.INSTAGRAM },
        onLinkedIn = { webViewType = WebViewType.LINKEDIN },
        onYouTube = { webViewType = WebViewType.YOUTUBE },
        onUnlockDocuments = { password ->
            viewModel.unlockDocumentsVault(
                password = password,
                onSuccess = {
                    showVaultPasswordDialog = false
                    onOpenDocumentsVault()
                }
            )
        },
        onOpenDocumentsVault = {
            onOpenDocumentsVault()
        },
        storageManager = storageManager,
        showVaultPasswordDialog = showVaultPasswordDialog,
        onShowVaultPasswordDialog = { showVaultPasswordDialog = it },
    )

    if (showImageZoom && !state.userImage.isNullOrEmpty()) {
        ProfileImageZoomDialog(
            imageUrl = state.userImage!!,
            userName = "",
            onDismiss = { showImageZoom = false }
        )
    }
    webViewType?.let { type ->
        PrivacyPolicy_TandCScreen(
            type = type,
            onBackPressed = { webViewType = null }
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
    onGPSData: () -> Unit = {},
    onAddFence: () -> Unit = {},
    onHistoryPlayback: () -> Unit = {},
    onNavigateMyAccount: () -> Unit,
    onRateApp: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onTermsConditions: () -> Unit = {},
    onRefundPolicy: () -> Unit = {},
    onShippingPolicy: () -> Unit = {},
    onContactUs: () -> Unit = {},
    onAboutApp: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onFacebook: () -> Unit,
    onInstagram: () -> Unit,
    onLinkedIn: () -> Unit,
    onYouTube: () -> Unit,
    onUnlockDocuments: (String) -> Unit,
    onOpenDocumentsVault: () -> Unit,
    showVaultPasswordDialog: Boolean,
    onShowVaultPasswordDialog: (Boolean) -> Unit,
    storageManager: StorageManager,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.error) {
        if (state.error == "Invalid password") {
            passwordError = state.error
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppPageBg
                )
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isCompact = screenHeight < 700.dp

            // Responsive sizes
            val horizontalPad = (screenWidth * 0.05f).coerceIn(16.dp, 28.dp)
            val cardCorner = 18.dp
            val sectionLabelSize = if (isCompact) 12.sp else 13.sp
            val menuTitleSize = if (isCompact) 14.sp else 15.sp
            val menuIconSize = if (isCompact) 22.dp else 24.dp
            val menuVerticalPad = if (isCompact) 14.dp else 16.dp
            val avatarSize = if (isCompact) 88.dp else 100.dp
            val nameSize = if (isCompact) 17.sp else 19.sp
            val logoutButtonHeight = if (isCompact) 46.dp else 50.dp

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppPageBg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Header
                    item {
                        ProfileHeader(
                            userName = state.userName,
                            userImage = state.userImage,
                            onImageClick = onProfileImageClick,
                            avatarSize = avatarSize,
                            nameSize = nameSize
                        )
                    }

                    // Profile Settings Section
                    item {
                        Text(
                            text = "PROFILE SETTINGS",
                            fontSize = sectionLabelSize,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF999999),
                            letterSpacing = 0.8.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPad, vertical = 14.dp)
                        )
                    }

                    item {
                        ProfileMenuCard(
                            items = listOf(
                                ProfileMenuItem("Edit Profile", Icons.Rounded.Edit, onEditProfile),
                                ProfileMenuItem("Account Activity", Icons.Rounded.ManageAccounts, onAccountActivity),
//                                ProfileMenuItem("Refer And Earn", Icons.Rounded.CardGiftcard, onReferAndEarn),
                                ProfileMenuItem("Change Password", Icons.Rounded.Password, onChangePassword),
                                ProfileMenuItem(
                                    title = "My Account",
                                    icon = Icons.Rounded.AccountBalance,
                                    onClick = onNavigateMyAccount
                                ),
                                ProfileMenuItem(
                                    title = "Community",
                                    icon = Icons.Rounded.Group,
                                    onClick = { onOpenDocumentsVault() }
                                )
                            ),
                            horizontalPad = horizontalPad,
                            cardCorner = cardCorner,
                            titleSize = menuTitleSize,
                            iconSize = menuIconSize,
                            verticalPad = menuVerticalPad
                        )
                    }

                    // Device Settings Section
                    item {
                        Text(
                            text = "DEVICE SETTINGS",
                            fontSize = sectionLabelSize,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF999999),
                            letterSpacing = 0.8.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPad, vertical = 14.dp)
                        )
                    }

                    item {
                        ProfileMenuCard(
                            items = listOf(
                                ProfileMenuItem("Manage Device", Icons.Rounded.Devices, onManageDevice),
                                ProfileMenuItem("GPS Device Data", Icons.Rounded.GpsFixed, onGPSData),
                                ProfileMenuItem("Manage Geo Fence", Icons.Rounded.LocationOn, onAddFence),
                                ProfileMenuItem("History Playback", Icons.Rounded.History, onHistoryPlayback)
                            ),
                            horizontalPad = horizontalPad,
                            cardCorner = cardCorner,
                            titleSize = menuTitleSize,
                            iconSize = menuIconSize,
                            verticalPad = menuVerticalPad
                        )
                    }

                    // About EVA2Z Section
                    item {
                        Text(
                            text = "ABOUT EVA2Z",
                            fontSize = sectionLabelSize,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF999999),
                            letterSpacing = 0.8.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPad, vertical = 14.dp)
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
                            onContactUs = onContactUs,
                            onAboutApp = onAboutApp,
                            onFacebook = onFacebook,
                            onInstagram = onInstagram,
                            onLinkedIn = onLinkedIn,
                            onYouTube = onYouTube,
                            horizontalPad = horizontalPad,
                            cardCorner = cardCorner,
                            isCompact = isCompact
                        )
                    }

                    // ============= NEW UI (COMMENTED OUT) =============
                    // Appearance Section — Theme toggle (dark/light mode)
                    /*
                    item {
                        Text(
                            text = "APPEARANCE",
                            fontSize = sectionLabelSize,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF999999),
                            letterSpacing = 0.8.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPad, vertical = 14.dp)
                        )
                    }

                    item {
                        ThemeToggleCard(
                            horizontalPad = horizontalPad,
                            cardCorner = cardCorner,
                            titleSize = menuTitleSize,
                            iconSize = menuIconSize,
                            verticalPad = menuVerticalPad
                        )
                    }
                    */

                    // Logout Button
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPad)
                                .height(logoutButtonHeight),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6A9F50)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            Text(
                                text = "Log Out",
                                fontSize = if (isCompact) 15.sp else 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }

                FloatingContactButton(
                    phoneNumber = "919755273822",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 20.dp)
                )

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF6A9F50)
                    )
                }
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
            title = {
                Text(
                    "Logout",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Yes", color = Color(0xFF6A9F50), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("No", color = Color(0xFF888888))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showVaultPasswordDialog) {
        VaultPasswordDialog(
            errorMessage = passwordError,
            onSubmit = { password ->
                passwordError = null
                onUnlockDocuments(password)
            },
            onDismiss = {
                onShowVaultPasswordDialog(false)
                passwordError = null
            }
        )
    }
}

@Composable
fun VaultPasswordDialog(
    errorMessage: String?,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(
                                top = 40.dp,
                                start = 24.dp,
                                end = 24.dp,
                                bottom = 16.dp
                            )
                    ) {
                        Text(
                            text = "Enter Your Login Password",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF222222)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Access Your E-Vault Documents",
                            fontSize = 13.sp,
                            color = Color(0xFF888888)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Password", color = Color(0xFFAAAAAA)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = errorMessage != null,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAF7),
                                unfocusedContainerColor = Color(0xFFF8FAF7),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedBorderColor = Color(0xFF6A9F50),
                                cursorColor = Color(0xFF6A9F50)
                            ),
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible }
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible)
                                            Icons.Default.VisibilityOff
                                        else
                                            Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color(0xFF999999)
                                    )
                                }
                            }
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(
                                    text = "Cancel",
                                    color = Color(0xFF888888),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            TextButton(
                                onClick = { onSubmit(password) },
                                enabled = password.isNotBlank()
                            ) {
                                Text(
                                    text = "Continue",
                                    color = if (password.isNotBlank())
                                        Color(0xFF6A9F50)
                                    else
                                        Color(0xFFCCCCCC),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-26).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6A9F50),
                        shadowElevation = 6.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(14.dp)
                                .size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
fun isVaultSessionValid(storageManager: StorageManager): Boolean {
    val expiry = storageManager.getString(StorageKeys.VAULT_EXPIRES_AT, "")
    if (expiry.isEmpty()) return false

    return try {
        val expiryInstant = kotlin.time.Instant.parse(expiry)
        val now = Clock.System.now()
        now < expiryInstant
    } catch (e: Exception) {
        false
    }
}

@Composable
fun ProfileHeader(
    userName: String,
    userImage: String?,
    onImageClick: () -> Unit = {},
    avatarSize: androidx.compose.ui.unit.Dp = 100.dp,
    nameSize: androidx.compose.ui.unit.TextUnit = 19.sp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar with green ring
        Box(
            modifier = Modifier
                .size(avatarSize + 6.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF6A9F50), Color(0xFF8BC34A))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F0))
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
                        fontSize = (avatarSize.value * 0.4f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6A9F50)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = userName,
            fontSize = nameSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222)
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
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )

                if (userName.isNotEmpty()) {
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
}

data class ProfileMenuItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun ProfileMenuCard(
    items: List<ProfileMenuItem>,
    horizontalPad: androidx.compose.ui.unit.Dp = 24.dp,
    cardCorner: androidx.compose.ui.unit.Dp = 18.dp,
    titleSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    verticalPad: androidx.compose.ui.unit.Dp = 16.dp
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPad),
        shape = RoundedCornerShape(cardCorner),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            items.forEachIndexed { index, item ->
                ProfileMenuItemRow(
                    title = item.title,
                    icon = item.icon,
                    onClick = item.onClick,
                    titleSize = titleSize,
                    iconSize = iconSize,
                    verticalPad = verticalPad
                )
                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color(0xFFF0F0F0),
                        thickness = 0.8.dp
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
    onClick: () -> Unit,
    titleSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    verticalPad: androidx.compose.ui.unit.Dp = 16.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = verticalPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with background
        Box(
            modifier = Modifier
                .size(iconSize + 12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF0F7EC)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF6A9F50),
                modifier = Modifier.size(iconSize - 4.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            fontSize = titleSize,
            color = Color(0xFF333333),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Arrow",
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(22.dp)
        )
    }
}

// ============= NEW UI ThemeToggleCard (COMMENTED OUT) =============
/*
@Composable
fun ThemeToggleCard(
    horizontalPad: androidx.compose.ui.unit.Dp = 24.dp,
    cardCorner: androidx.compose.ui.unit.Dp = 18.dp,
    titleSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    verticalPad: androidx.compose.ui.unit.Dp = 16.dp
) {
    val theme = LocalHomeTheme.current
    val isDark = theme.isDark
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPad),
        shape = RoundedCornerShape(cardCorner),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { theme.toggle() }
                .padding(horizontal = 16.dp, vertical = verticalPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize + 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isDark) Color(0xFFE8EEFF) else Color(0xFFFFF7E0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Default.DarkMode
                                  else Icons.Default.LightMode,
                    contentDescription = "Theme",
                    tint = if (isDark) Color(0xFF3B82F6) else Color(0xFFF59E0B),
                    modifier = Modifier.size(iconSize - 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dark Mode",
                    fontSize = titleSize,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isDark) "Dark theme is on" else "Light theme is on",
                    fontSize = 11.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Switch(
                checked = isDark,
                onCheckedChange = { theme.toggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF6A9F50),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFCCCCCC),
                    uncheckedBorderColor = Color(0xFFCCCCCC)
                )
            )
        }
    }
}
*/

@Composable
fun AboutSection(
    onRateApp: () -> Unit,
    onFeedback: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onTermsConditions: () -> Unit,
    onRefundPolicy: () -> Unit,
    onShippingPolicy: () -> Unit,
    onContactUs: () -> Unit,
    onAboutApp: () -> Unit,
    onFacebook: () -> Unit,
    onInstagram: () -> Unit,
    onLinkedIn: () -> Unit,
    onYouTube: () -> Unit,
    horizontalPad: androidx.compose.ui.unit.Dp = 24.dp,
    cardCorner: androidx.compose.ui.unit.Dp = 18.dp,
    isCompact: Boolean = false
) {
    val aboutTitleSize = if (isCompact) 13.sp else 14.sp
    val aboutIconSize = if (isCompact) 15.dp else 16.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPad),
        shape = RoundedCornerShape(cardCorner),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            AboutMenuItem(
                title = getPlatformRateAppText(),
                icon = Icons.Rounded.Star,
                onClick = onRateApp,
                titleSize = aboutTitleSize,
                iconSize = aboutIconSize
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFF0F0F0),
                thickness = 0.8.dp
            )

            AboutMenuItem(
                title = "About App",
                icon = Icons.Rounded.Info,
                onClick = onAboutApp,
                titleSize = aboutTitleSize,
                iconSize = aboutIconSize
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFF0F0F0),
                thickness = 0.8.dp
            )

            AboutMenuItem("Privacy Policy", Icons.Rounded.PrivacyTip, onPrivacyPolicy, aboutTitleSize, aboutIconSize)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFF0F0F0),
                thickness = 0.8.dp
            )
            AboutMenuItem("Terms & Conditions", Icons.Rounded.Description, onTermsConditions, aboutTitleSize, aboutIconSize)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFF0F0F0),
                thickness = 0.8.dp
            )
            AboutMenuItem("Refund & Cancellation Policy", Icons.Rounded.Receipt, onRefundPolicy, aboutTitleSize, aboutIconSize)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFF0F0F0),
                thickness = 0.8.dp
            )
            AboutMenuItem("Shipping & Installation Policy", Icons.Rounded.LocalShipping, onShippingPolicy, aboutTitleSize, aboutIconSize)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFF0F0F0),
                thickness = 0.8.dp
            )
            AboutMenuItem("Contact Us", Icons.Rounded.Support, onContactUs, aboutTitleSize, aboutIconSize)

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFFF0F0F0),
                thickness = 0.8.dp
            )

            SocialLinksRow(
                onFacebook = { openUrl("https://www.facebook.com/Eva2zofficial/") },
                onInstagram = { openUrl("https://www.instagram.com/eva2z_/") },
                onLinkedIn = { openUrl("https://www.linkedin.com/company/86388029") },
                onYouTube = { openUrl("https://youtube.com/@eva2z_connect?si=MDx3Obn2fAxwSyL7") },
                isCompact = isCompact
            )
        }
    }
}

expect fun getPlatformRateAppText(): String

@Composable
fun AboutMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    titleSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp
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
            tint = Color(0xFF6A9F50),
            modifier = Modifier.size(iconSize)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            fontSize = titleSize,
            color = Color(0xFF555555),
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Arrow",
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SocialLinksRow(
    onFacebook: () -> Unit,
    onInstagram: () -> Unit,
    onLinkedIn: () -> Unit,
    onYouTube: () -> Unit,
    isCompact: Boolean = false
) {
    val iconBgSize = if (isCompact) 42.dp else 46.dp
    val iconImgSize = if (isCompact) 20.dp else 22.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SocialPngIcon(
            icon = Res.drawable.ic_facebook,
            contentDescription = "Facebook",
            onClick = onFacebook,
            bgSize = iconBgSize,
            imgSize = iconImgSize
        )
        SocialPngIcon(
            icon = Res.drawable.ic_instagram,
            contentDescription = "Instagram",
            onClick = onInstagram,
            bgSize = iconBgSize,
            imgSize = iconImgSize
        )
        SocialPngIcon(
            icon = Res.drawable.ic_linkedin,
            contentDescription = "LinkedIn",
            onClick = onLinkedIn,
            bgSize = iconBgSize,
            imgSize = iconImgSize
        )
        SocialPngIcon(
            icon = Res.drawable.ic_youtube,
            contentDescription = "YouTube",
            onClick = onYouTube,
            bgSize = iconBgSize,
            imgSize = iconImgSize
        )
    }
}

@Composable
fun SocialPngIcon(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    bgSize: androidx.compose.ui.unit.Dp = 46.dp,
    imgSize: androidx.compose.ui.unit.Dp = 22.dp
) {
    Box(
        modifier = Modifier
            .size(bgSize)
            .clip(CircleShape)
            .background(Color(0xFFF0F7EC))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(imgSize)
        )
    }
}