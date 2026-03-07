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
//    var showSuccessDialog by remember { mutableStateOf(false) }

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

    // ✅ Reload data when screen is opened
    LaunchedEffect(Unit) {
        viewModel.loadUserData()
    }
    // ✅ System back = same as top back
    PlatformBackHandler(enabled = true) {
        onBackPressed()
    }


    // ✅ Image Picker Launcher - NO auto upload
    val imagePickerLauncher = rememberImagePicker { imageBytes ->
        if (imageBytes != null) {
            println("✅ Image selected: ${imageBytes.size} bytes (waiting for Update button)")
            viewModel.updateProfileWithImage(imageBytes)
        }
    }

    // ✅ Handle update success with delay
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

    // ✅ Image Zoom Dialog
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
    Scaffold(    modifier = Modifier.statusBarsPadding(), // 👈🔥 ADD THIS

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
        },
        contentWindowInsets = WindowInsets(0) // 🔥 IMPORTANT

    ) { paddingValues ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
        val scrollState = rememberScrollState()
        val focusManager = LocalFocusManager.current

        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus() // 👈 KEYBOARD CLOSE
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Profile Image with Edit Button
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {


                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0))
                            .border(2.dp, Color(0xFF4CAF50), CircleShape)
                            .clickable { onImageClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            state.userImageBytes != null -> {
//                                val = state.userIma
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
                                println("User ki pehle se Image : " + state.userImage)
                                println("User ki recent se Image size: ${state.userImageBytes?.size} bytes")
                                println("User ki recent se Image :" +BASE_IMAGE_URL +state.userImageBytes)

                            }
                            else -> {
                                Text(
                                    text = state.userName.firstOrNull()?.uppercase() ?: "U",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Edit Icon Button
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

                Text(
                    text = state.userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Full Name Field (Required)
                ProfileInputField(
                    label = "Full Name *",
                    value = state.userName,
                    onValueChange = onNameChange,
                    placeholder = "Enter your name (required)"
                )

                Spacer(modifier = Modifier.height(20.dp))



                ProfileInputField(
                    label = "Email *",
                    value = state.userEmail,
                    onValueChange = onEmailChange,
                    placeholder = "Enter your email ",
                    keyboardType = KeyboardType.Email,
                    isError = emailError,
                    errorText = if (emailError) "Please enter a valid email address" else null
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
//                    enabled = !state.isLoading &&
//                            state.userName.isNotEmpty() &&
//                            !emailError
                    enabled = !state.isLoading &&
                            hasChanges &&
                            state.userName.isNotBlank() &&
                            !emailError

                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
//                        Text(
//                            text = "Update Profile",
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.SemiBold
//                        )
                        Text(
                            text = if (hasChanges) "Update Profile" else "No Changes",
                            fontSize = 16.sp,
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
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }

    // Error Snackbar
    state.error?.let { error ->
        LaunchedEffect(error) {
            println("❌ Error: $error")
            delay(3000)
            onDismissError()
        }

        // Show error at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF44336)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = error,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
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
                            .clip(RoundedCornerShape(16.dp)),
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
                            .clip(RoundedCornerShape(16.dp)),
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
    errorText: String? = null
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
            isError = isError,
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
        if (isError && !errorText.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                color = Color(0xFFD32F2F),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

fun isValidEmail(email: String): Boolean {
    if (email.isBlank()) return true // ✅ optional email allowed

    val emailRegex = Regex(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )
    return emailRegex.matches(email)
}