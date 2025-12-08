package com.developer.ev2z.ui.screens.EditProfile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.util.rememberImagePicker

@Composable
fun EditProfileScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    onBackPressed: () -> Unit = {}
) {
    val viewModel = remember { EditProfileViewModel(apiService, storageManager) }
    val state by viewModel.state.collectAsState()

    var showImageZoom by remember { mutableStateOf(false) }

    // ✅ Image Picker Launcher
    val imagePickerLauncher = rememberImagePicker { imageBytes ->
        if (imageBytes != null) {
            viewModel.updateProfileWithImage(imageBytes)
        }
    }

    // Handle update success
    LaunchedEffect(state.updateSuccess) {
        if (state.updateSuccess) {
            viewModel.resetUpdateSuccess()
            onBackPressed()
        }
    }

    EditProfileScreenContent(
        state = state,
        onBackPressed = onBackPressed,
        onNameChange = { viewModel.updateName(it) },
        onEmailChange = { viewModel.updateEmail(it) },
        onImageClick = { showImageZoom = true },
        onImageEditClick = { imagePickerLauncher.launch() },  // ✅ Launch picker
        onUpdateProfile = { viewModel.updateProfile() },
        onDismissError = { viewModel.clearError() }
    )

    // ✅ Image Zoom Dialog
    if (showImageZoom && !state.userImage.isNullOrEmpty()) {
        ProfileImageZoomDialog(
            imageUrl = state.userImage!!,
            onDismiss = { showImageZoom = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreenContent(
    state: EditProfileState,
    onBackPressed: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onImageClick: () -> Unit = {},
    onImageEditClick: () -> Unit = {},
    onUpdateProfile: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Edit Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Change your personal info.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // ✅ Profile Image with Edit Button & Click to Zoom
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // Profile Image (Clickable to zoom)
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0))
                            .border(2.dp, Color(0xFF4CAF50), CircleShape)
                            .clickable { onImageClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!state.userImage.isNullOrEmpty()) {
                            AsyncImage(
                                model = state.userImage,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = state.userName.firstOrNull()?.uppercase() ?: "U",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    }

                    // ✅ Edit Icon Button (Opens Image Picker)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .clickable { onImageEditClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Photo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // User Name Display
                Text(
                    text = state.userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Full Name Field
                ProfileInputField(
                    label = "Full Name",
                    value = state.userName,
                    onValueChange = onNameChange,
                    placeholder = "Enter your name"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Email Field
                ProfileInputField(
                    label = "Email",
                    value = state.userEmail,
                    onValueChange = onEmailChange,
                    placeholder = "Enter your email",
                    keyboardType = KeyboardType.Email
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Phone Number Field (Disabled)
                ProfileInputField(
                    label = "Phone Number",
                    value = state.userPhone,
                    onValueChange = {},
                    placeholder = "Phone number",
                    enabled = false
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Password Field (Hidden)
                ProfileInputField(
                    label = "Password",
                    value = "••••••••••",
                    onValueChange = {},
                    placeholder = "Password",
                    enabled = false
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Update Button
                Button(
                    onClick = onUpdateProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Update Profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Loading Overlay
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }

    // Error Snackbar
    state.error?.let { error ->
        LaunchedEffect(error) {
            // Show error toast/snackbar
            onDismissError()
        }
    }
}

// ✅ Profile Image Zoom Dialog
@Composable
fun ProfileImageZoomDialog(
    imageUrl: String,
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

@Composable
fun ProfileInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color(0xFFBBBBBB)
                )
            },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                disabledBorderColor = Color(0xFFE0E0E0),
                disabledTextColor = Color(0xFF999999),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color(0xFFF5F5F5)
            ),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = if (enabled && label != "Password") {
                {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else null
        )
    }
}