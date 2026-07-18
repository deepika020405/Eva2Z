package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.LocationOn
import com.developer.ev2z.fetchPhoneLocation
import com.developer.ev2z.openLocationSettings
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.util.Analytics
import com.developer.ev2z.util.rememberImagePicker

private val postCategories = listOf("Tips", "Traffic", "Stolen", "Lost", "Found", "Police")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onBack: () -> Unit
) {
    val viewModel = CommunityViewModelProvider.viewModel

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var images by remember { mutableStateOf<List<ByteArray>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf(postCategories.first()) }

    // 📍 Auto-detected location → city (sent with the post so it lands in the right community)
    var lat by remember { mutableStateOf<Double?>(null) }
    var long by remember { mutableStateOf<Double?>(null) }
    var city by remember { mutableStateOf("") }
    var locating by remember { mutableStateOf(true) }
    var locationOff by remember { mutableStateOf(false) }

    var isPosting by remember { mutableStateOf(false) }
    var postSuccess by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        Analytics.logEvent("create_post_screen_view")
    }

    // Fetch device location, then reverse-geocode to a city name.
    fun fetchLocation() {
        locating = true
        locationOff = false
        fetchPhoneLocation { lt, ln ->
            if (lt != 0.0 || ln != 0.0) {
                lat = lt
                long = ln
                viewModel.resolveCity(lt, ln) { resolved ->
                    city = resolved
                    locating = false
                }
            } else {
                // Permission denied or location services off.
                locating = false
                locationOff = true
            }
        }
    }

    LaunchedEffect(Unit) { fetchLocation() }

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
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
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
                            tint = Color(0xFF0F172A)
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
                                images = images,
                                lat = lat,
                                long = long,
                                city = city.takeIf { it.isNotBlank() },
                                category = selectedCategory
                            )
                            postSuccess = true
                        }
                    ) {
                        Text(
                            "Post",
                            color = if (body.isNotBlank() && !isPosting) Color(0xFF00A844)
                            else Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
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
                .background(Color(0xFFF8FAFC))
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
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPad, vertical = if (isCompact) 12.dp else 16.dp)
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            "Title (optional)",
                            color = Color(0xFF94A3B8),
                            fontSize = labelSize
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = Color(0xFF00A844),
                        cursorColor = Color(0xFF00A844)
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
                            color = Color(0xFF94A3B8),
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
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = Color(0xFF00A844),
                        cursorColor = Color(0xFF00A844)
                    )
                )

                Spacer(Modifier.height(if (isCompact) 12.dp else 16.dp))

                // Category selector
                Text(
                    "Category",
                    fontSize = labelSize,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
                Spacer(Modifier.height(8.dp))
                LazyRow {
                    items(postCategories) { cat ->
                        val selected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (selected) Color(0xFF00A844) else Color.White)
                                .border(
                                    1.dp,
                                    if (selected) Color(0xFF00A844) else Color(0xFFE2E8F0),
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable(enabled = !isPosting) { selectedCategory = cat }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                cat,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(if (isCompact) 12.dp else 16.dp))

                // Detected location (auto from device, sent as the post's city)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !locating && !isPosting) {
                            if (locationOff) openLocationSettings()
                            fetchLocation()
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        tint = if (locationOff) Color(0xFFDC2626) else Color(0xFF00A844),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            locating -> "Detecting location…"
                            locationOff -> "Location is off — tap to enable"
                            city.isNotBlank() -> city
                            else -> "Location unavailable — tap to retry"
                        },
                        fontSize = 12.sp,
                        color = if (locationOff) Color(0xFFDC2626) else Color(0xFF94A3B8)
                    )
                }

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
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Image ${index + 1}",
                                    color = Color(0xFF00A844),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                // Close button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00A844))
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
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF00A844)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (images.size < 5) Color(0xFF00A844) else Color(0xFFE2E8F0)
                    )
                ) {
                    Icon(
                        Icons.Default.Image,
                        null,
                        tint = Color(0xFF00A844),
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
                    CircularProgressIndicator(color = Color(0xFF00A844))
                }
            }
        }
    }
}