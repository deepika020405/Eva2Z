package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage

//class StoryViewerScreen : Screen {
//
//    @OptIn(ExperimentalMaterial3Api::class)
//    @Composable
//    override fun Content() {
//
//        val navigator = LocalNavigator.currentOrThrow
//        val vm = CommunityViewModelProvider.viewModel
//        val state = vm.state
//
//        val story = state.stories[state.storyIndex]
//        var lastStoryTimerStarted by remember { mutableStateOf(false) }
//
//        LaunchedEffect(state.storyIndex) {
//
//            val isLastStory =
//                state.storyIndex == state.stories.lastIndex
//
//            if (isLastStory && !lastStoryTimerStarted) {
//                lastStoryTimerStarted = true
//
//                // ⏳ last story poora dikhane ka time
//                kotlinx.coroutines.delay(15_000)
//
//                // Safety check
//                if (state.storyIndex == state.stories.lastIndex) {
//                    navigator.pop() // 🔥 AUTO BACK
//                }
//            }
//
//            // 🔄 Agar user prev story pe gaya
//            if (!isLastStory) {
//                lastStoryTimerStarted = false
//            }
//        }
//
//        Box(
//            Modifier
//                .fillMaxSize()
//                .background(Color.Black)
//                .pointerInput(Unit) {
//                    detectTapGestures(
//                        onPress = {
//                            vm.pauseStory()
//                            tryAwaitRelease()
//                            vm.resumeStory()
//                        },
//                        onTap = {
//                            if (it.x < size.width / 2) vm.prevStory()
//                            else vm.nextStory()
//                        }
//                    )
//                }
//        ) {
//
//            AsyncImage(
//                model = "https://gps.eva2z.in/storage/${story.media.first()}",
//                contentDescription = null,
//                modifier = Modifier.fillMaxSize(),
//                contentScale = ContentScale.Fit
//            )
//
//            LinearProgressIndicator(
//                progress = state.storyProgress,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(8.dp)
//            )
//
//            // 👁 OWNER ONLY
//            if (story.user.id == state.authUserId) {
//                Text(
//                    text = "👁 ${story.view_count}",
//                    color = Color.White,
//                    modifier = Modifier
//                        .align(Alignment.BottomCenter)
//                        .padding(16.dp)
//                        .clickable {
//                            vm.loadStoryViewers(story.id)
//                        }
//                )
//            }
//        }
//
//        if (state.storyViewers.isNotEmpty()) {
//            ModalBottomSheet(onDismissRequest = vm::clearStoryViewers) {
//                state.storyViewers.forEach {
//                    Text(it.user.name, Modifier.padding(12.dp))
//                }
//            }
//        }
//    }
//}

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

        Box(
            Modifier
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

            AsyncImage(
                model = "https://gps.eva2z.in/storage/${story.media.first()}",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            LinearProgressIndicator(
                progress = vm.storyProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            // 👁️ OWNER ONLY
            if (story.user.id == state.authUserId) {
                Text(
                    text = "👁 ${story.view_count}",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clickable {
                            vm.loadStoryViewers(story.id)
                        }
                )
            }
        }

        if (state.storyViewers.isNotEmpty()) {
            ModalBottomSheet(onDismissRequest = vm::clearStoryViewers) {
                state.storyViewers.forEach {
                    Text(it.user.name, Modifier.padding(12.dp))
                }
            }
        }
    }
}
