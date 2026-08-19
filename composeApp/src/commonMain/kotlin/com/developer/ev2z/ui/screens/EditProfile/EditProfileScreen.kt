package com.developer.ev2z.ui.screens.EditProfile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.util.rememberImagePicker
import kotlinx.coroutines.delay
import com.developer.ev2z.theme.AppPageBg
private const val BASE_IMAGE_URL = "https://gps.eva2z.in/storage/"

@Composable
fun EditProfileScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    onBackPressed: () -> Unit = {}
) {
    val viewModel = remember { EditProfileViewModel(apiService, storageManager) }
    val state by viewModel.state.collectAsState()

    var showImageZoom by remember { mutableStateOf(false) }

    var emailError by remember(state.userEmail) {
        mutableStateOf(
            state.userEmail.isNotBlank() && !isValidEmail(state.userEmail)
        )
    }

    val hasChanges = remember(state) {
        val nameChanged = state.userName.trim() != state.originalName
        val emailChanged = state.userEmail.trim() != state.originalEmail
        val imageChanged = state.userImageBytes != null
        nameChanged || emailChanged || imageChanged
    }

    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserData()
    }

    PlatformBackHandler(enabled = true) {
        onBackPressed()
    }

    val imagePickerLauncher = rememberImagePicker { imageBytes ->
        if (imageBytes != null) {
            println("✅ Image selected: ${imageBytes.size} bytes (waiting for Update button)")
            viewModel.updateProfileWithImage(imageBytes)
        }
    }

    LaunchedEffect(state.updateSuccess) {
        if (state.updateSuccess) {
            viewModel.resetUpdateSuccess()
            onBackPressed()
        }
    }

    EditProfileScreenContent(
        state = state,
        emailError = emailError,
        onBackPressed = onBackPressed,
        hasChanges = hasChanges,
        onNameChange = { viewModel.updateName(it) },
        onEmailChange = {
            viewModel.updateEmail(it)
            emailError = !isValidEmail(it)
        },
        onImageClick = { showImageZoom = true },
        onImageEditClick = { imagePickerLauncher.launch() },
        onUpdateProfile = { viewModel.updateProfile() },
        onDismissError = { viewModel.clearError() }
    )

    if (showImageZoom) {
        when {
            state.userImageBytes != null -> {
                ProfileImageZoomDialog(
                    imageBytes = state.userImageBytes!!,
                    onDismiss = { showImageZoom = false }
                )
            }

            !state.userImage.isNullOrEmpty() -> {
                val image = if (state.userImage!!.startsWith("https")) {
                    state.userImage
                } else {
                    BASE_IMAGE_URL + state.userImage
                }

                ProfileImageZoomDialog(
                    imageUrl = image,
                    onDismiss = { showImageZoom = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreenContent(
    state: EditProfileState,
    emailError: Boolean,
    hasChanges: Boolean,
    onBackPressed: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onImageClick: () -> Unit = {},
    onImageEditClick: () -> Unit = {},
    onUpdateProfile: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Edit Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF222222)
                        )
                        Text(
                            text = "Change your personal info.",
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF333333)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppPageBg
                )
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        val focusManager = LocalFocusManager.current

        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                }
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isCompact = screenHeight < 700.dp

            // Responsive sizes
            val horizontalPad = (screenWidth * 0.06f).coerceIn(16.dp, 32.dp)
            val avatarSize = if (isCompact) 100.dp else 120.dp
            val avatarInitialSize = (avatarSize.value * 0.4f).sp
            val editIconSize = if (isCompact) 32.dp else 36.dp
            val editInnerIconSize = if (isCompact) 18.dp else 20.dp
            val nameSize = if (isCompact) 18.sp else 20.sp
            val labelSize = if (isCompact) 13.sp else 14.sp
            val buttonHeight = if (isCompact) 48.dp else 52.dp
            val buttonTextSize = if (isCompact) 15.sp else 16.sp
            val fieldGap = if (isCompact) 16.dp else 20.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppPageBg)
                    .verticalScroll(scrollState)
                    .padding(horizontal = horizontalPad, vertical = if (isCompact) 16.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 32.dp))

                // Profile Image with Edit Button
                Box(
                    modifier = Modifier.size(avatarSize + 6.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // Green gradient ring
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
                            when {
                                state.userImageBytes != null -> {
                                    AsyncImage(
                                        model = state.userImageBytes,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                !state.userImage.isNullOrEmpty() -> {
                                    val image = if (state.userImage.startsWith("https")) {
                                        state.userImage
                                    } else {
                                        BASE_IMAGE_URL + state.userImage
                                    }
                                    AsyncImage(
                                        model = image,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                else -> {
                                    Text(
                                        text = state.userName.firstOrNull()?.uppercase() ?: "U",
                                        fontSize = avatarInitialSize,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6A9F50)
                                    )
                                }
                            }
                        }
                    }

                    // Edit Icon Button
                    Box(
                        modifier = Modifier
                            .size(editIconSize)
                            .clip(CircleShape)
                            .background(Color(0xFF6A9F50))
                            .clickable { onImageEditClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Photo",
                            tint = Color.White,
                            modifier = Modifier.size(editInnerIconSize)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = state.userName,
                    fontSize = nameSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )

                Spacer(modifier = Modifier.height(if (isCompact) 24.dp else 32.dp))

                // Full Name Field
                ProfileInputField(
                    label = "Full Name *",
                    value = state.userName,
                    onValueChange = onNameChange,
                    placeholder = "Enter your name (required)",
                    labelSize = labelSize
                )

                Spacer(modifier = Modifier.height(fieldGap))

                // Email Field
                ProfileInputField(
                    label = "Email *",
                    value = state.userEmail,
                    onValueChange = onEmailChange,
                    placeholder = "Enter your email",
                    keyboardType = KeyboardType.Email,
                    isError = emailError,
                    errorText = if (emailError) "Please enter a valid email address" else null,
                    labelSize = labelSize
                )

                Spacer(modifier = Modifier.height(fieldGap))

                // Phone Number Field (Disabled)
                ProfileInputField(
                    label = "Phone Number",
                    value = state.userPhone,
                    onValueChange = {},
                    placeholder = "Phone number",
                    enabled = false,
                    labelSize = labelSize
                )

                Spacer(modifier = Modifier.height(if (isCompact) 36.dp else 48.dp))

                // Update Button
                Button(
                    onClick = onUpdateProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A9F50),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFB5D4A5),
                        disabledContentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    ),
                    enabled = !state.isLoading &&
                            hasChanges &&
                            state.userName.isNotBlank() &&
                            !emailError
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (hasChanges) "Update Profile" else "No Changes",
                            fontSize = buttonTextSize,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            FloatingContactButton(
                phoneNumber = "919755273822",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
            )

            // Loading Overlay
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF6A9F50))
                }
            }
        }
    }

    // Error Toast
    state.error?.let { error ->
        LaunchedEffect(error) {
            println("❌ Error: $error")
            delay(3000)
            onDismissError()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ProfileImageZoomDialog(
    imageUrl: String? = null,
    imageBytes: ByteArray? = null,
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
            when {
                imageBytes != null -> {
                    AsyncImage(
                        model = imageBytes,
                        contentDescription = "Profile Picture Zoomed",
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                imageUrl != null -> {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Profile Picture Zoomed",
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    labelSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = labelSize,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF555555),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0xFFAAAAAA),
                    fontSize = labelSize
                )
            },
            enabled = enabled,
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6A9F50),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                disabledBorderColor = Color(0xFFE8E8E8),
                disabledTextColor = Color(0xFF999999),
                focusedContainerColor = Color(0xFFF8FAF7),
                unfocusedContainerColor = Color(0xFFF8FAF7),
                disabledContainerColor = Color(0xFFF5F5F5),
                cursorColor = Color(0xFF6A9F50)
            ),
            shape = RoundedCornerShape(14.dp),
            trailingIcon = if (enabled && label != "Password") {
                {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null
        )
        if (isError && !errorText.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

fun isValidEmail(email: String): Boolean {
    if (email.isBlank()) return true

    val emailRegex = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )
    return emailRegex.matches(email)
}