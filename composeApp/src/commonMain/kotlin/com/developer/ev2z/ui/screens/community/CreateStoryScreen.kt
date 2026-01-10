package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.developer.ev2z.util.Analytics
import com.developer.ev2z.util.compressStoryImage
import com.developer.ev2z.util.fixAndCompressImage
import com.developer.ev2z.util.rememberImagePicker




class CreateStoryScreen : Screen {


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow
        val viewModel = CommunityViewModelProvider.viewModel

        var body by remember { mutableStateOf("") }
        var image by remember { mutableStateOf<ByteArray?>(null) }

        // ✅ SAME picker as EditProfile
//        val imagePickerLauncher = rememberImagePicker { imageBytes ->
//            if (imageBytes != null) {
//                image = imageBytes
//                println("✅ Story image selected: ${imageBytes.size} bytes")
//            }
//        }

        val imagePickerLauncher = rememberImagePicker { imageBytes ->
            if (imageBytes != null) {
                // 🔥 Fix rotation + reduce size (no zoom, no resize)
                val fixed = compressStoryImage(
                    imageBytes = imageBytes,
                    maxSizeKB = 800
                )


                image = fixed
                println("✅ Story image ready: ${fixed.size / 1024} KB")
            }
        }


        // Analytics
        LaunchedEffect(Unit) {
            Analytics.logEvent(name = "Create_story_screen_view")
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 🖼 DIRECT PICKER (NO BOTTOM SHEET)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE0E0E0))
                        .clickable {
                            // 🚀 DIRECT OPEN CAMERA / GALLERY
                            imagePickerLauncher.launch()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (image == null) {
                        Text("Tap to capture / select image")
                    } else {
                        Text("Image selected ✅")
                    }
                }

//                Spacer(Modifier.height(16.dp))

//                OutlinedTextField(
//                    value = body,
//                    onValueChange = { body = it },
//                    placeholder = { Text("Say something… (optional)") },
//                    modifier = Modifier.fillMaxWidth()
//                )
            }
        }
    }
}


