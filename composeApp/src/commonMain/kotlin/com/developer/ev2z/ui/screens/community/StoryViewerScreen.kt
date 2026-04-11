package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.util.BottomBarController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerScreen(
    onBack: () -> Unit
) {
    val vm = CommunityViewModelProvider.viewModel
    val state = vm.state
    var showViewersSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        BottomBarController.visible = false
    }

    PlatformBackHandler(enabled = true) {
        onBack()
    }

    val story = vm.currentStory()

    DisposableEffect(Unit) {
        onDispose {
            vm.resetStoryViewer()
            BottomBarController.visible = true
        }
    }

    LaunchedEffect(story) {
        if (story == null) {
            onBack()
        }
    }

    if (story == null) return

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val isCompact = maxHeight < 700.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            vm.pauseStory()
                            tryAwaitRelease()
                            vm.resumeStory()
                        },
                        onTap = { offset ->
                            if (offset.x < size.width / 2)
                                vm.prevStoryInGroup()
                            else
                                vm.nextStoryInGroup()
                        }
                    )
                }
        ) {
            val group = vm.currentGroup!!

            StoryTopBar(
                userName = group.user.name,
                userProfile = "https://gps.eva2z.in/storage/${group.user.photo}",
                isMyStatus = group.user.id == state.authUserId,
                isCompact = isCompact,
                onBack = onBack
            )

            // Progress Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                group.stories.forEachIndexed { index, _ ->
                    LinearProgressIndicator(
                        progress = when {
                            index < vm.storyIndex -> 1f
                            index == vm.storyIndex -> vm.storyProgress
                            else -> 0f
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .height(3.dp),
                        color = Color(0xFF6A9F50),
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            // Story Image
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = "https://gps.eva2z.in${story.media_urls.first()}",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                if (group.isOwnStory) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                vm.loadStoryViewers(story.id)
                                showViewersSheet = true
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${story.view_count} views",
                            color = Color.White,
                            fontSize = if (isCompact) 13.sp else 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Viewers Bottom Sheet
    if (showViewersSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showViewersSheet = false
                vm.clearStoryViewers()
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Viewed by",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )

                Spacer(Modifier.height(12.dp))

                if (state.storyViewers.isEmpty()) {
                    Text(
                        text = "No views yet",
                        color = Color(0xFF888888),
                        fontSize = 14.sp
                    )
                } else {
                    state.storyViewers.forEach { viewer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF0F7EC)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!viewer.user.photo.isNullOrBlank()) {
                                    AsyncImage(
                                        model = "https://gps.eva2z.in/storage/${viewer.user.photo}",
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = viewer.user.name.first().uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6A9F50)
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = viewer.user.name,
                                fontSize = 15.sp,
                                color = Color(0xFF333333),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}


@Composable
private fun StoryTopBar(
    userName: String,
    userProfile: String?,
    isMyStatus: Boolean,
    isCompact: Boolean = false,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isCompact) 8.dp else 12.dp, start = 12.dp, end = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(26.dp)
                    .clickable { onBack() }
            )

            Spacer(Modifier.width(12.dp))

            AsyncImage(
                model = userProfile,
                contentDescription = null,
                modifier = Modifier
                    .size(if (isCompact) 34.dp else 38.dp)
                    .background(Color.DarkGray, CircleShape)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = if (isMyStatus) "My Status" else userName,
                color = Color.White,
                fontSize = if (isCompact) 14.sp else 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}