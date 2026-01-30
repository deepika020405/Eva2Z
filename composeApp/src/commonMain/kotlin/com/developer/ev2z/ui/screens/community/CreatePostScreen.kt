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
import androidx.compose.material.icons.filled.ArrowBack
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
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

    PlatformBackHandler(true){
        onBack()
    }

    // ✅ Image picker
    val imagePickerLauncher = rememberImagePicker { imageBytes ->
        if (imageBytes != null && images.size < 5) {
            images = images + imageBytes
        }
    }

    // ✅ SUCCESS FLOW
    LaunchedEffect(postSuccess) {
        if (postSuccess) {
            snackbarHostState.showSnackbar("Post created successfully ✅")
            kotlinx.coroutines.delay(800)
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create Post", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(
                        enabled = !isPosting,
                        onClick = onBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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
                        Text("Post")
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFf2f5f0))
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text("What's on your mind?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(16.dp))

                if (images.isNotEmpty()) {
                    LazyRow {
                        itemsIndexed(images) { index, _ ->
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE0E0E0)),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Text("🖼", modifier = Modifier.align(Alignment.Center))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(18.dp)
                                        .clickable {
                                            images = images.toMutableList().also {
                                                it.removeAt(index)
                                            }
                                        }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    enabled = images.size < 5 && !isPosting,
                    onClick = { imagePickerLauncher.launch() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Attach images (${images.size}/5)")
                }
            }

            // ⭕ LOADER OVERLAY
            if (isPosting) {
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
}
