package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.developer.ev2z.response.Comment
import com.developer.ev2z.response.Post
import com.developer.ev2z.response.Story
import com.developer.ev2z.response.StoryViewer
import com.developer.ev2z.response.Users
import com.developer.ev2z.ui.screens.Profile.ProfileImageZoomDialog
import com.developer.ev2z.util.Analytics
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.time.ExperimentalTime

@Composable
fun CommunityScreen(
    posts: List<Post>,
    stories: List<Story>,
    authUserId: Int,
    onCreatePostClick: () -> Unit,
    onLikeClick: (Int) -> Unit,
    viewModel: CommunityViewModel, // ✅ ADD THIS
    onCommentClick: (Int) -> Unit,
    onAddStory: () -> Unit,
    onStoryClick: (Story) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFf2f5f0))
    ) {
         val BASE_IMAGE_URL = "https://gps.eva2z.in"

        item {
            StoriesRow(
                stories = stories,
                authUserId = authUserId,
                viewModel = viewModel ,
                onAddStory = onAddStory,
                onStoryClick = onStoryClick
            )

        }

        item {
            CreatePostCard(onClick = onCreatePostClick)
        }

        items(posts) { post ->
            PostItem(
                post = post,
                BASE_IMAGE_URL = BASE_IMAGE_URL,
                onLikeClick = onLikeClick,
                onCommentClick = onCommentClick
            )
        }
    }
}


@Composable
fun CreatePostCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
//            Box(
//                modifier = Modifier
//                    .size(40.dp)
//                    .clip(CircleShape)
//                    .background(Color.LightGray)
//            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = "Share something with community...",
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create",
                tint = Color(0xFF0066FF)
            )
        }
    }
}



@Composable
fun PostItem(
    post: Post,
    BASE_IMAGE_URL: String,
    onLikeClick: (Int) -> Unit,
    onCommentClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp)) // 🔥 MASTER CLIP
                .clipToBounds()
                .padding(14.dp)
        ) {

            PostHeader(post.user, post.created_at, BASE_IMAGE_URL)

            Spacer(Modifier.height(8.dp))

            post.title?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
            }

            post.body?.takeIf { it.isNotBlank() }?.let {
                Text(it)
                Spacer(Modifier.height(8.dp))
            }

            if (post.media_urls.isNotEmpty()) {
                FacebookPostImages(
                    images = post.media_urls.map { "$BASE_IMAGE_URL$it" }
                )
                Spacer(Modifier.height(8.dp))
            }

            PostFooter(
                post = post,
                onLikeClick = onLikeClick,
                onCommentClick = onCommentClick
            )
        }
    }
}

@Composable
fun FacebookPostImages(images: List<String>) {
    var previewImage by remember { mutableStateOf<String?>(null) }

    when (images.size) {
        1 -> FBOneImage(images[0]) { previewImage = it }
        2 -> FBTwoImages(images) { previewImage = it }
        3 -> FBThreeImages(images) { previewImage = it }
        else -> FBFourPlusImages(images) { previewImage = it }
    }

    previewImage?.let {
        ProfileImageZoomDialog(
            imageUrl = it,
            userName = "",
            onDismiss = { previewImage = null }
        )
    }
}

@Composable
fun FBOneImage(
    url: String,
    onClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick(url) }
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
@Composable
fun FBTwoImages(
    urls: List<String>,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.height(200.dp)
    ) {
        urls.take(2).forEachIndexed { index, url ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(if (index == 0) 0.dp else 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClick(url) }
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun FBThreeImages(
    urls: List<String>,
    onClick: (String) -> Unit
) {
    Column(modifier = Modifier.height(260.dp)) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick(urls[0]) }
        ) {
            AsyncImage(
                model = urls[0],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.weight(1f)) {
            urls.drop(1).take(2).forEach { url ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onClick(url) }
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun FBFourPlusImages(
    urls: List<String>,
    onClick: (String) -> Unit
) {
    Column(modifier = Modifier.height(260.dp)) {

        Row(modifier = Modifier.weight(1f)) {
            urls.take(2).forEach { url ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onClick(url) }
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Row(modifier = Modifier.weight(1f)) {
            urls.drop(2).take(2).forEachIndexed { index, url ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onClick(url) }
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (index == 1 && urls.size > 4) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+${urls.size - 4}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
        }
    }
}









@Composable
fun PostHeader(
    user: Users?,
    createdAt: String,
    BASE_IMAGE_URL: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {


        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            if (!user?.photo.isNullOrEmpty()) {
//
                AsyncImage(
                    model = "$BASE_IMAGE_URL/storage/${user.photo}",
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = user?.name?.firstOrNull()?.uppercase() ?: "U",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }



        Spacer(Modifier.width(10.dp))

        Column {
            Text(text = user?.name ?: "N/A", fontWeight = FontWeight.SemiBold)
            Text(
                text = formatPostDateCmp(createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )

        }
    }
}


@OptIn(ExperimentalTime::class)
fun formatPostDateCmp(apiDate: String): String {
    return try {
        val postDate = LocalDate.parse(apiDate.take(10))

        val now = Instant.fromEpochMilliseconds(
            getTimeMillis()
        )


        val today = now
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

        val yesterday = today.minus(1, DateTimeUnit.DAY)

        when {
            postDate == today -> "Today"
            postDate == yesterday -> "Yesterday"
            else -> {
                val day = postDate.dayOfMonth.toString().padStart(2, '0')
                val month = postDate.monthNumber.toString().padStart(2, '0')
                val year = postDate.year
                "$day-$month-$year"
            }
        }
    } catch (e: Exception) {
        apiDate.take(10)
    }
}




@Composable
fun FullScreenImage(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // 🔹 Close on background tap
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
        )

        // 🔹 Image (center, well-fitted)
        NetworkImage(
            url = imageUrl,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(16.dp)
        )

        // 🔹 Cross button (top-right)
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(28.dp)
                .clickable { onDismiss() }
        )
    }
}
@Composable
fun PostFooter(
    post: Post,
    onLikeClick: (Int) -> Unit,
    onCommentClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                onLikeClick(post.id)
            }
        ) {
            Icon(
                imageVector =
                    if (post.liked) Icons.Filled.Favorite
                    else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like",
                tint = if (post.liked) Color.Red else Color.Gray
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = post.likes_count.toString(),
                color = Color.Gray
            )
        }

        Text(
            text = "💬 ${post.comments_count} Comments",
            color = Color.Gray,
            modifier = Modifier.clickable {
                onCommentClick(post.id)
            }
        )
    }
}


//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CommentBottomSheet(
//    postId: Int,
//    viewModel: CommunityViewModel,
//    onDismiss: () -> Unit
//) {
//
//    val state = viewModel.state
//    var input by remember { mutableStateOf("") }
//
//    LaunchedEffect(postId) {
//        viewModel.load(postId)
//    }
//
//
////    ModalBottomSheet(
////        onDismissRequest = onDismiss
////    ) {
////        Column(
////            modifier = Modifier
////                .fillMaxHeight(0.95f)
////                .imePadding()          // 👈 🔥 KEY LINE
////        ) {
////
////            // 🔹 TOP BAR
////            Text(
////                "Comments",
////                fontWeight = FontWeight.Bold,
////                modifier = Modifier
////                    .align(Alignment.CenterHorizontally)
////                    .padding(vertical = 12.dp)
////            )
////
////            Divider()
////
////            LazyColumn(
////                modifier = Modifier.weight(1f)
////            ) {
////                items(state.comments) { comment ->
////                    CommentItem(
////                        comment = comment,
////                        isReply = comment.parent_id != null,
////                        onLike = {
////                            viewModel.like(comment.id, postId)
////                        },
////                        onReply = {
////                            viewModel.setReply(comment)
////                        },
////                        onRemoveReplyTag = {
////                            viewModel.clearReply()
////                        }
////                    )
////                }
////            }
////
////            // Reply indicator
////            state.replyTo?.let {
////                Text(
////                    "Replying to ${it.user.name}",
////                    color = Color.Gray,
////                    modifier = Modifier.padding(8.dp)
////                )
////            }
////
////            // 👇 INPUT ALWAYS AT BOTTOM
////            Row(
////                Modifier
////                    .fillMaxWidth()
////                    .padding(8.dp),
////                verticalAlignment = Alignment.CenterVertically
////            ) {
////                OutlinedTextField(
////                    value = input,
////                    onValueChange = { input = it },
////                    modifier = Modifier.weight(1f),
////                    placeholder = { Text("Add a comment…") }
////                )
////
////                TextButton(
////                    enabled = input.isNotBlank(),
////                    onClick = {
////                        viewModel.sendComment(postId, input)
////                        input = ""
////                    }
////                ) {
////                    Text("Post")
////                }
////            }
////        }
//
//    ModalBottomSheet(
//        onDismissRequest = onDismiss
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxHeight(0.95f)
//                .imePadding() // 🔥 keyboard safe
//        ) {
//
//            // 🔹 TOP BAR
//            Text(
//                "Comments",
//                fontWeight = FontWeight.Bold,
//                modifier = Modifier
//                    .align(Alignment.CenterHorizontally)
//                    .padding(vertical = 12.dp)
//            )
//
//            Divider()
//
//            // 🔹 COMMENTS LIST
//            LazyColumn(
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxWidth()
//            ) {
//                items(state.comments) { comment ->
//                    CommentItem(
//                        comment = comment,
//                        isReply = comment.parent_id != null,
//                        onLike = {
//                            viewModel.like(comment.id, postId)
//                        },
//                        onReply = {
//                            viewModel.setReply(comment)
//                        },
//                        onRemoveReplyTag = {
//                            viewModel.clearReply()
//                        }
//                    )
//                }
//            }
//
//            // 🔹 INPUT FIXED AT BOTTOM
//            CommentInputBar(
//                replyTo = state.replyTo,
//                input = input,
//                onInputChange = { input = it },
//                onSend = {
//                    viewModel.sendComment(postId, input)
//                    input = ""
//                }
//            )
//        }
//    }
//
//
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    postId: Int,
    viewModel: CommunityViewModel,
    onDismiss: () -> Unit
) {
    val state = viewModel.state
    var input by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(postId) {
        viewModel.load(postId)
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
        ) {

            // 🔹 HEADER
            Text(
                text = "Comments",
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.dp)
            )

            Divider()

            // 🔹 COMMENTS LIST
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(state.comments) { comment ->
                    CommentItem(
                        comment = comment,
                        isReply = comment.parent_id != null,
                        onLike = {
                            viewModel.like(comment.id, postId)
                        },
                        onReply = {
                            viewModel.setReply(comment)
                        },
                        onRemoveReplyTag = {
                            viewModel.clearReply()
                        }
                    )
                }
            }

            // 🔥 INPUT BAR — handles keyboard safely
            CommentInputBar(
                replyTo = state.replyTo,
                input = input,
                onInputChange = { input = it },
                onSend = {
                    viewModel.sendComment(postId, input)
                    input = ""
                }
            )
        }
    }

}


@Composable
fun CommentInputBar(
    replyTo: Comment?,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(replyTo) {
        if (replyTo != null) {
            delay(150) // 🔥 let sheet settle
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {

        replyTo?.let {
            Text(
                text = "Replying to ${it.user.name}",
                color = Color.Gray,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
            )
        }

        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("Add a comment…") }
            )

            TextButton(
                enabled = input.isNotBlank(),
                onClick = onSend
            ) {
                Text("Post")
            }
        }
    }
}



@Composable
fun CommentItem(
    comment: Comment,
    isReply: Boolean,
    replyToName: String? = null,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onRemoveReplyTag: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(
                start = if (isReply) 32.dp else 16.dp,
                end = 16.dp,
                top = 10.dp
            )
    ) {

        // 👤 USER PHOTO
        AsyncImage(
            model = comment.user.photo?.let {
                "https://gps.eva2z.in/storage/$it"
            },
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {

            // 👤 NAME
            Text(
                text = comment.user.name,
                fontWeight = FontWeight.SemiBold
            )

            // 🔁 REPLY TO TAG INSIDE COMMENT
            if (replyToName != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@$replyToName",
                        color = Color(0xFF0066FF),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = "✕",
                        color = Color.Gray,
                        modifier = Modifier.clickable {
                            onRemoveReplyTag()
                        }
                    )
                }
            }

            Text(comment.comment)

            Spacer(Modifier.height(4.dp))

            Row {
                Text(
                    text = if (comment.liked) "❤️ ${comment.likes_count}"
                    else "🤍 ${comment.likes_count}",
                    modifier = Modifier.clickable { onLike() }
                )

                Spacer(Modifier.width(16.dp))

                Text(
                    "Reply",
                    color = Color.Blue,
                    modifier = Modifier.clickable { onReply() }
                )
            }
        }
    }
}

//@Composable
//fun StoriesRow(
//    stories: List<Story>,
//    authUserId: Int,
//    onAddStory: () -> Unit,
//    onStoryClick: (Story) -> Unit
//) {
//    LazyRow(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 8.dp),
//        contentPadding = PaddingValues(horizontal = 12.dp)
//    ) {
//
//        // 🔹 YOUR STORY (FIRST)
//        item {
//            AddStoryItem(onClick = onAddStory)
//        }
//
//        // 🔹 OTHER STORIES
//        items(stories) { story ->
//            StoryItem(
//                story = story,
//                isMine = story.user.id == authUserId,
//                onClick = { onStoryClick(story) }
//            )
//        }
//    }
//}

@Composable
fun StoriesRow(
    stories: List<Story>,
    authUserId: Int,
    viewModel: CommunityViewModel,
    onAddStory: () -> Unit,
    onStoryClick: (Story) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {

        item {
            AddStoryItem(onClick = onAddStory)
        }

        items(stories) { story ->
            StoryItem(
                story = story,
                isViewed = viewModel.isStoryViewed(story.id),
                onClick = { onStoryClick(story) }
            )
        }
    }
}


@Composable
fun AddStoryItem(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(end = 12.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF0066FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("Your story", style = MaterialTheme.typography.labelSmall)
    }
}


//@Composable
//fun StoryItem(
//    story: Story,
//    isMine: Boolean,
//    onClick: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .padding(end = 12.dp)
//            .clickable { onClick() },
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//
//        Box(
//            modifier = Modifier
//                .size(68.dp)
//                .clip(CircleShape)
//                .background(
//                    Brush.linearGradient(
//                        listOf(Color.Magenta, Color.Red, Color.Yellow)
//                    )
//                ),
//            contentAlignment = Alignment.Center
//        ) {
//
//            AsyncImage(
//                model = "https://gps.eva2z.in/storage/${story.user.photo}",
//                contentDescription = null,
//                modifier = Modifier
//                    .size(60.dp)
//                    .clip(CircleShape)
//                    .background(Color.LightGray),
//                contentScale = ContentScale.Crop
//            )
//        }
//
//        Spacer(Modifier.height(4.dp))
//        Text(
//            text = story.user.name,
//            maxLines = 1,
//            style = MaterialTheme.typography.labelSmall
//        )
//    }
//}



@Composable
fun StoryItem(
    story: Story,
    isViewed: Boolean,
    onClick: () -> Unit
) {
    val ringBrush =
        if (isViewed) {
            // ✅ Viewed → faded ring
            Brush.linearGradient(
                listOf(
                    Color.LightGray,
                    Color.Gray
                )
            )
        } else {
            // 🔥 Unviewed → Instagram gradient
            Brush.linearGradient(
                listOf(
                    Color(0xFFD62976), // pink
                    Color(0xFFFA7E1E), // orange
                    Color(0xFFFEDA75)  // yellow
                )
            )
        }

    Column(
        modifier = Modifier
            .padding(end = 12.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(ringBrush),
            contentAlignment = Alignment.Center
        ) {

            AsyncImage(
                model = "https://gps.eva2z.in/storage/${story.user.photo}",
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = story.user.name,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall,
            color = if (isViewed) Color.Gray else Color.Black
        )
    }
}

@Composable
fun StoryViewersBar(
    viewers: List<StoryViewer>
) {
    if (viewers.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {

        Text(
            text = "👁️ ${viewers.size} views",
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        viewers.take(5).forEach { viewer ->
            Text(
                text = viewer.user.name,
                color = Color.White,
                fontSize = 12.sp
            )
        }

        if (viewers.size > 5) {
            Text(
                text = "+${viewers.size - 5} more",
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }
    }
}



class CommunityScreenWrapper : Screen {

    @Composable
    override fun Content() {


        val viewModel = CommunityViewModelProvider.viewModel
        val state = viewModel.state
        val navigator = LocalNavigator.currentOrThrow
        var openCommentsPostId by remember { mutableStateOf<Int?>(null) }

        LaunchedEffect(Unit) {
            Analytics.logEvent(
                name = "community_screen_view"
            )
        }

        // 👇 refresh-aware API call
        val refreshKey = viewModel.observeRefreshKey()

        LaunchedEffect(refreshKey) {
            viewModel.loadPosts()
        }

        when {
            state.loading -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading...")
                }
            }

            state.error != null -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.error ?: "Error")
                }
            }

            else -> {
                CommunityScreen(
                    posts = state.posts,
                    stories = state.stories,
                    authUserId = state.authUserId,
                    onCreatePostClick = {
                        navigator.push(CreatePostScreen())
                    },
                    onLikeClick = { postId ->
                        viewModel.toggleLike(postId)
                    },
                    onCommentClick = { postId ->
                        openCommentsPostId = postId
                    },
                    onAddStory = {
                        navigator.push(CreateStoryScreen())
                    },
                    onStoryClick = { story ->
                        val index = state.stories.indexOfFirst { it.id == story.id }
                        viewModel.startStory(index)
                        navigator.push(StoryViewerScreen())
                    },
                    viewModel = viewModel

                )

            }

        }
        openCommentsPostId?.let { postId ->
            CommentBottomSheet(
                postId = postId,
                viewModel = viewModel,
                onDismiss = { openCommentsPostId = null }
            )
        }
    }
}


