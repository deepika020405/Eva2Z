package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.util.Analytics
import com.developer.ev2z.util.rememberImagePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onBack: () -> Unit
) {
    val viewModel = CommunityViewModelProvider.viewModel

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var images by remember { mutableStateOf<List<ByteArray>>(emptyList()) }

    var isPosting by remember { mutableStateOf(false) }
    var postSuccess by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        Analytics.logEvent("create_post_screen_view")
    }

    PlatformBackHandler(true) { onBack() }

    val imagePickerLauncher = rememberImagePicker { imageBytes ->
        if (imageBytes != null && images.size < 5) {
            images = images + imageBytes
        }
    }

    LaunchedEffect(postSuccess) {
        if (postSuccess) {
            snackbarHostState.showSnackbar("Post created successfully")
            kotlinx.coroutines.delay(800)
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create Post",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF222222)
                    )
                },
                navigationIcon = {
                    IconButton(
                        enabled = !isPosting,
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null,
                            tint = Color(0xFF333333)
                        )
                    }
                },
                actions = {
                    TextButton(
                        enabled = body.isNotBlank() && !isPosting,
                        onClick = {
                            isPosting = true
                            viewModel.createPost(
                                title = title,
                                body = body,
                                images = images
                            )
                            postSuccess = true
                        }
                    ) {
                        Text(
                            "Post",
                            color = if (body.isNotBlank() && !isPosting) Color(0xFF6A9F50)
                            else Color(0xFFCCCCCC),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F6F8))
        ) {
            val screenHeight = maxHeight
            val screenWidth = maxWidth
            val isCompact = screenHeight < 700.dp
            val horizontalPad = (screenWidth * 0.04f).coerceIn(12.dp, 20.dp)
            val bodyHeight = if (isCompact) 140.dp else 160.dp
            val labelSize = if (isCompact) 13.sp else 14.sp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPad, vertical = if (isCompact) 12.dp else 16.dp)
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            "Title (optional)",
                            color = Color(0xFFAAAAAA),
                            fontSize = labelSize
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = Color(0xFF6A9F50),
                        cursorColor = Color(0xFF6A9F50)
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(if (isCompact) 10.dp else 12.dp))

                // Body Field
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = {
                        Text(
                            "What's on your mind?",
                            color = Color(0xFFAAAAAA),
                            fontSize = labelSize
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bodyHeight),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = Color(0xFF6A9F50),
                        cursorColor = Color(0xFF6A9F50)
                    )
                )

                Spacer(Modifier.height(if (isCompact) 12.dp else 16.dp))

                // Selected Images
                if (images.isNotEmpty()) {
                    LazyRow {
                        itemsIndexed(images) { index, _ ->
                            Box(
                                modifier = Modifier
                                    .size(if (isCompact) 80.dp else 90.dp)
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF0F7EC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Image ${index + 1}",
                                    color = Color(0xFF6A9F50),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                // Close button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF6A9F50))
                                        .clickable {
                                            images = images.toMutableList().also {
                                                it.removeAt(index)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(if (isCompact) 12.dp else 16.dp))
                }

                // Attach Images Button
                OutlinedButton(
                    enabled = images.size < 5 && !isPosting,
                    onClick = { imagePickerLauncher.launch() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompact) 46.dp else 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF6A9F50)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (images.size < 5) Color(0xFF6A9F50) else Color(0xFFE0E0E0)
                    )
                ) {
                    Icon(
                        Icons.Default.Image,
                        null,
                        tint = Color(0xFF6A9F50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Attach images (${images.size}/5)",
                        fontWeight = FontWeight.Medium,
                        fontSize = if (isCompact) 14.sp else 15.sp
                    )
                }
            }

            // Loader Overlay
            if (isPosting) {
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
}