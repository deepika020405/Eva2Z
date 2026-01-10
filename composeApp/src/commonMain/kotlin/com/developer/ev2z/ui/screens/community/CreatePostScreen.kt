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
import com.developer.ev2z.util.Analytics
import com.developer.ev2z.util.rememberImagePicker

class CreatePostScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow

        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        var images by remember { mutableStateOf<List<ByteArray>>(emptyList()) }
//        var showImagePickerOptions by remember { mutableStateOf(false) }

        val viewModel = CommunityViewModelProvider.viewModel

        LaunchedEffect(Unit) {
            Analytics.logEvent(
                name = "create_post_screen_view"
            )
        }

        // ✅ EXISTING cross-platform picker
        val imagePickerLauncher = rememberImagePicker { imageBytes ->
            if (imageBytes != null && images.size < 5) {
                images = images + imageBytes
            }
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .windowInsetsPadding(WindowInsets.systemBars),
            topBar = {
                TopAppBar(
                    title = { Text("Create Post", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        TextButton(
                            enabled = body.isNotBlank(),
                            onClick = {
                                viewModel.createPost(title, body, images)
                                viewModel.refreshPosts()
                                navigator.pop()
                            }
                        ) {
                            Text("Post")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            contentWindowInsets = WindowInsets(0)
        ) { padding ->

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFf2f5f0))
                    .padding(16.dp)
            ) {

                // 🔹 Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                // 🔹 Body
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text("What's on your mind?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                /* ---------- UI SAME AS BEFORE ---------- */

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
                    enabled = images.size < 5,
                    onClick = {
                        imagePickerLauncher.launch()   // 🔥 DIRECT CAMERA / GALLERY
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Attach images (${images.size}/5)")
                }
            }
        }

        // ✅ Dialog ALWAYS outside Scaffold

    }
}

