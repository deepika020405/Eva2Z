package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.util.Analytics
import com.developer.ev2z.util.compressStoryImage
import com.developer.ev2z.util.rememberImagePicker
import com.developer.ev2z.theme.AppPageBg


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    onBack: () -> Unit
) {
    val viewModel = CommunityViewModelProvider.viewModel

    var body by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<ByteArray?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    var uploadSuccess by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberImagePicker { imageBytes ->
        if (imageBytes != null) {
            // Compression is a multi-second bitmap decode/encode on large camera
            // shots — keep it off the main thread or the picker return janks.
            scope.launch {
                val fixed = withContext(Dispatchers.Default) {
                    compressStoryImage(
                        imageBytes = imageBytes,
                        maxSizeKB = 800
                    )
                }
                image = fixed
            }
        }
    }

    PlatformBackHandler(true) { onBack() }

    LaunchedEffect(Unit) {
        Analytics.logEvent(name = "Create_story_screen_view")
    }

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            snackbarHostState.showSnackbar(
                message = "Story uploaded successfully"
            )
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
                        "Add Story",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(
                        enabled = !isUploading,
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null,
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                actions = {
                    TextButton(
                        enabled = image != null && !isUploading,
                        onClick = {
                            isUploading = true
                            viewModel.createStory(
                                body = body,
                                media = listOfNotNull(image)
                            )
                            uploadSuccess = true
                        }
                    ) {
                        Text(
                            "Create",
                            color = if (image != null && !isUploading) Color(0xFF00A844)
                            else Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppPageBg)
        ) {
            val screenHeight = maxHeight
            val screenWidth = maxWidth
            val isCompact = screenHeight < 700.dp
            val horizontalPad = (screenWidth * 0.04f).coerceIn(12.dp, 20.dp)
            val imageBoxHeight = if (isCompact) 260.dp else 320.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPad, vertical = if (isCompact) 12.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Image Picker Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageBoxHeight)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (image == null) Color(0xFFDCFCE7) else Color(0xFFDCFCE7))
                        .clickable(enabled = !isUploading) {
                            imagePickerLauncher.launch()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (image == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        Color(0xFF00A844).copy(alpha = 0.15f),
                                        RoundedCornerShape(18.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.CameraAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF00A844),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Tap to capture / select image",
                                color = Color(0xFF00A844),
                                fontWeight = FontWeight.Medium,
                                fontSize = if (isCompact) 14.sp else 15.sp
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                tint = Color(0xFF00A844),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Image selected",
                                color = Color(0xFF00A844),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = if (isCompact) 15.sp else 16.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tap to change",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Loading Overlay
            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00A844))
                }
            }
        }
    }
}