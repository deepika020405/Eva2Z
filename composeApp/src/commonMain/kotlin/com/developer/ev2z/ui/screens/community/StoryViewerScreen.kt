package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.util.BottomBarController


@Composable
fun StoryViewerScreen(
    onBack: () -> Unit
) {
    val vm = CommunityViewModelProvider.viewModel
    val state = vm.state
    var showViewersSheet by remember { mutableStateOf(false) }

    // 🔥 Hide bottom bar
    LaunchedEffect(Unit) {
        BottomBarController.visible = false
    }

    // 🔙 SYSTEM BACK
    PlatformBackHandler(enabled = true) {
        onBack()
    }

    val story = vm.currentStory()

    // 🧹 CLEANUP
    DisposableEffect(Unit) {
        onDispose {
            vm.resetStoryViewer()
            BottomBarController.visible = true
        }
    }

    // ✅ Story finished → auto close
    LaunchedEffect(story) {
        if (story == null) {
            onBack()
        }
    }

    if (story == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
            onBack = onBack          // 🔥 IMPORTANT
        )

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
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = "https://gps.eva2z.in${story.media_urls.first()}",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            if (group.isOwnStory) {
                Text(
                    text = "👁 ${story.view_count}",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clickable {
                            vm.loadStoryViewers(story.id)
                            showViewersSheet = true
                        }
                )
            }
        }
    }
}



@Composable
private fun StoryTopBar(
    userName: String,
    userProfile: String?,
    isMyStatus: Boolean,   // 👈 NEW
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 12.dp, end = 12.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Text(
                text = "←",
                color = Color.White,
                fontSize = 26.sp,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickable { onBack() }
            )

            AsyncImage(
                model = userProfile,
                contentDescription = null,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.DarkGray, CircleShape)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Text(
                text = if (isMyStatus) "My Status" else userName, // ✅ MAGIC LINE
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}


