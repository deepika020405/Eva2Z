package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.developer.ev2z.util.rememberImagePicker




class CreateStoryScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow
        val viewModel = CommunityViewModelProvider.viewModel

        var body by remember { mutableStateOf("") }
        var image by remember { mutableStateOf<ByteArray?>(null) }
        var showPicker by remember { mutableStateOf(false) }

        // ✅ same picker you already use
        val imagePickerLauncher = rememberImagePicker { imageBytes ->
            if (imageBytes != null) {
                image = imageBytes
            }
        }




        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add Story", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        TextButton(
                            enabled = image != null,
                            onClick = {
                                viewModel.createStory(
                                    body = body,
                                    media = listOfNotNull(image)
                                )
                                navigator.pop()
                            }
                        ) {
                            Text("Create")
                        }
                    }
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 🖼 Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE0E0E0))
                        .clickable { showPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (image == null) {
                        Text("Tap to select image")
                    } else {
                        Text("Image selected ✅")
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text("Say something… (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 🔥 Picker options
        if (showPicker) {
            SimpleImagePickerDialog(
                onDismiss = { showPicker = false },
                onCameraClick = {
                    showPicker = false
                    imagePickerLauncher.launch()
                },
                onGalleryClick = {
                    showPicker = false
                    imagePickerLauncher.launch()
                }
            )
        }


    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleImagePickerDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Text(
                text = "Add Story Image",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 📷 Camera
            Text(
                text = "📷 Camera",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCameraClick()
                        onDismiss()
                    }
                    .padding(vertical = 12.dp)
            )

            // 🖼 Gallery
            Text(
                text = "🖼 Gallery",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onGalleryClick()
                        onDismiss()
                    }
                    .padding(vertical = 12.dp)
            )
        }
    }
}
