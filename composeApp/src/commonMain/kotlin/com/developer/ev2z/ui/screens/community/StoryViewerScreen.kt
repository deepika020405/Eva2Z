package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage


class StoryViewerScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow
        val vm = CommunityViewModelProvider.viewModel
        val state = vm.state

        val story = vm.currentStory()

        // ✅ Group khatam → auto back
        LaunchedEffect(story) {
            if (story == null) {
                navigator.pop()
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

            // 1️⃣ TOP BAR (FIXED)
            StoryTopBar(
                userName = story.user.name,
                userProfile = "https://gps.eva2z.in/storage/${story.user.photo}",
                onBack = { navigator.pop() }
            )

            // 2️⃣ LINEAR PROGRESS (TOP BAR KE NEECHHE)
            LinearProgressIndicator(
                progress = vm.storyProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                color = Color(0xFFB388FF),
                trackColor = Color.White.copy(alpha = 0.3f)
            )

            // 3️⃣ IMAGE (PROGRESS KE NEECHHE)
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                AsyncImage(
                    model = "https://gps.eva2z.in/storage/${story.media.first()}",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // 4️⃣ VIEW COUNT (IMAGE KE ANDAR, BOTTOM)
                if (story.user.id == state.authUserId) {
                    Text(
                        text = "👁 ${story.view_count}",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .clickable {
                                vm.loadStoryViewers(story.id)
                            }
                    )
                }
            }
        }


    }
}

@Composable
private fun StoryTopBar(
    userName: String,
    userProfile: String?,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 12.dp, end = 12.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back
            Text(
                text = "←",
                color = Color.White,
                fontSize = 26.sp,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickable { onBack() }
            )

            // Profile Image (Perfect Circle)
            AsyncImage(
                model = userProfile,
                contentDescription = null,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.DarkGray, CircleShape)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // Username
            Text(
                text = userName,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}


