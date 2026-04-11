package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.response.Comment
import com.developer.ev2z.response.Post
import com.developer.ev2z.response.Story
import com.developer.ev2z.response.StoryGroup
import com.developer.ev2z.response.Users
import com.developer.ev2z.ui.screens.Profile.ProfileImageZoomDialog
import com.developer.ev2z.ui.screens.social_media.openUrl
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.text.firstOrNull
import kotlin.time.ExperimentalTime


@Composable
fun CommunityScreen(
    posts: List<Post>,
    stories: List<Story>,
    authUserId: Int,
    onCreatePostClick: () -> Unit,
    onLikeClick: (Int) -> Unit,
    viewModel: CommunityViewModel,
    onCommentClick: (Int) -> Unit,
    onAddStory: () -> Unit,
    onStoryClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6F8)),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        val BASE_IMAGE_URL = "https://gps.eva2z.in"

        item {
            StoriesRow(
                groups = viewModel.storyGroups,
                viewModel = viewModel,
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
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))

            Text(
                text = "Share something with community...",
                color = Color(0xFF999999),
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF0F7EC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create",
                    tint = Color(0xFF6A9F50),
                    modifier = Modifier.size(18.dp)
                )
            }
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
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .clipToBounds()
                .padding(14.dp)
        ) {
            PostHeader(post.user, post.created_at, BASE_IMAGE_URL)

            Spacer(Modifier.height(8.dp))

            post.title?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )
                Spacer(Modifier.height(4.dp))
            }

            post.body?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    color = Color(0xFF444444),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
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

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val imageWidth = maxWidth
        // Responsive heights based on available width
        val singleImageHeight = (imageWidth * 0.65f).coerceIn(200.dp, 360.dp)
        val twoImageHeight = (imageWidth * 0.55f).coerceIn(180.dp, 300.dp)
        val multiImageHeight = (imageWidth * 0.72f).coerceIn(220.dp, 380.dp)

        when (images.size) {
            1 -> FBOneImage(images[0], singleImageHeight) { previewImage = it }
            2 -> FBTwoImages(images, twoImageHeight) { previewImage = it }
            3 -> FBThreeImages(images, multiImageHeight) { previewImage = it }
            else -> FBFourPlusImages(images, multiImageHeight) { previewImage = it }
        }
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
    imageHeight: androidx.compose.ui.unit.Dp,
    onClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(imageHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF5F5F5))
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
    imageHeight: androidx.compose.ui.unit.Dp,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(imageHeight),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        urls.take(2).forEach { url ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable { onClick(url) }
            ) {
                AsyncImage(
                    model = url, contentDescription = null,
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
    imageHeight: androidx.compose.ui.unit.Dp,
    onClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(imageHeight),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F5F5))
                .clickable { onClick(urls[0]) }
        ) {
            AsyncImage(
                model = urls[0], contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier.weight(0.8f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            urls.drop(1).take(2).forEach { url ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable { onClick(url) }
                ) {
                    AsyncImage(
                        model = url, contentDescription = null,
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
    imageHeight: androidx.compose.ui.unit.Dp,
    onClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(imageHeight),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            urls.take(2).forEach { url ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable { onClick(url) }
                ) {
                    AsyncImage(
                        model = url, contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            urls.drop(2).take(2).forEachIndexed { index, url ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable { onClick(url) }
                ) {
                    AsyncImage(
                        model = url, contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (index == 1 && urls.size > 4) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+${urls.size - 4}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
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
                .background(Color(0xFFF0F7EC)),
            contentAlignment = Alignment.Center
        ) {
            if (!user?.photo.isNullOrEmpty()) {
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
                    color = Color(0xFF6A9F50)
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column {
            Text(
                text = user?.name ?: "N/A",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF222222),
                fontSize = 15.sp
            )
            Text(
                text = formatPostDateCmp(createdAt),
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
        }
    }
}


@OptIn(ExperimentalTime::class)
fun formatPostDateCmp(apiDate: String): String {
    return try {
        val instant = Instant.parse(apiDate)
        val postDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val postDate = postDateTime.date
        val postTime = postDateTime.time
        val today = Instant.fromEpochMilliseconds(getTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        val yesterday = today.minus(1, DateTimeUnit.DAY)

        val hour24 = postTime.hour
        val minute = postTime.minute.toString().padStart(2, '0')
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val formattedTime = "$hour12:$minute $amPm"
        val day = postDate.dayOfMonth.toString().padStart(2, '0')
        val month = postDate.monthNumber.toString().padStart(2, '0')
        val year = postDate.year
        val formattedDate = "$day-$month-$year"

        when {
            postDate == today -> "Today, $formattedTime"
            postDate == yesterday -> "Yesterday, $formattedTime"
            else -> "$formattedDate, $formattedTime"
        }
    } catch (e: Exception) {
        apiDate.take(10)
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
            modifier = Modifier.clickable { onLikeClick(post.id) }
        ) {
            Icon(
                imageVector = if (post.liked) Icons.Filled.Favorite
                else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like",
                tint = if (post.liked) Color(0xFFE53935) else Color(0xFFCCCCCC),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = post.likes_count.toString(),
                color = Color(0xFF888888),
                fontSize = 14.sp
            )
        }

        Text(
            text = "${post.comments_count} Comments",
            color = Color(0xFF888888),
            fontSize = 14.sp,
            modifier = Modifier.clickable { onCommentClick(post.id) }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    postId: Int,
    viewModel: CommunityViewModel,
    onDismiss: () -> Unit
) {
    val state = viewModel.state
    var input by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(postId) {
        viewModel.load(postId)
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {

            Text(
                text = "Comments",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF222222),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.dp)
            )

            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.8.dp)

            val orderedComments = remember(state.comments) {
                val parents = state.comments.filter { it.parent_id == null }
                val repliesMap = state.comments
                    .filter { it.parent_id != null }
                    .groupBy { it.parent_id }
                buildList {
                    parents.forEach { parent ->
                        add(parent)
                        repliesMap[parent.id]?.let { replies -> addAll(replies) }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(orderedComments) { comment ->
                    val parentUserName = state.comments
                        .firstOrNull { it.id == comment.parent_id }
                        ?.user?.name

                    CommentItem(
                        comment = comment,
                        isReply = comment.parent_id != null,
                        replyToName = parentUserName,
                        onLike = { viewModel.like(comment.id, postId) },
                        onReply = { viewModel.setReply(comment) }
                    )
                }
            }

            CommentInputBar(
                replyTo = state.replyTo,
                input = input,
                onInputChange = { input = it },
                onSend = {
                    viewModel.sendComment(postId, input)
                    input = ""
                },
                onCancelReply = { viewModel.clearReply() }
            )
        }
    }
}


@Composable
fun CommentInputBar(
    replyTo: Comment?,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelReply: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(replyTo) {
        if (replyTo != null) {
            delay(150)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Replying to ${it.user.name}",
                    color = Color(0xFF888888),
                    fontSize = 13.sp
                )
                Text(
                    text = "✕",
                    fontSize = 16.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier
                        .clickable { onCancelReply() }
                        .padding(4.dp)
                )
            }
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
                placeholder = {
                    Text(
                        "Add a comment…",
                        color = Color(0xFFAAAAAA)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6A9F50),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    cursorColor = Color(0xFF6A9F50)
                )
            )

            TextButton(
                enabled = input.isNotBlank(),
                onClick = onSend
            ) {
                Text(
                    "Post",
                    color = if (input.isNotBlank()) Color(0xFF6A9F50)
                    else Color(0xFFCCCCCC),
                    fontWeight = FontWeight.SemiBold
                )
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
    onReply: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(
                start = if (isReply) 48.dp else 16.dp,
                end = 16.dp,
                top = 10.dp
            )
    ) {
        AsyncImage(
            model = comment.user.photo?.let {
                "https://gps.eva2z.in/storage/$it"
            },
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F7EC)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.user.name,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF222222),
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatPostDateCmp(comment.created_at),
                    fontSize = 11.sp,
                    color = Color(0xFF999999)
                )
            }

            if (replyToName != null) {
                Text(
                    text = "@$replyToName",
                    color = Color(0xFF6A9F50),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }

            LinkText(
                text = comment.comment,
                normalColor = Color(0xFF333333)
            )

            Spacer(Modifier.height(4.dp))

            Row {
                Text(
                    text = if (comment.liked) "❤️ ${comment.likes_count}"
                    else "🤍 ${comment.likes_count}",
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onLike() }
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "Reply",
                    color = Color(0xFF6A9F50),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onReply() }
                )
            }
        }
    }
}

@Composable
fun LinkText(
    text: String,
    normalColor: Color = Color(0xFF333333)
) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            var lastIndex = 0
            urlRegex.findAll(text).forEach { match ->
                val start = match.range.first
                val end = match.range.last + 1
                if (start > lastIndex) {
                    withStyle(style = SpanStyle(color = normalColor)) {
                        append(text.substring(lastIndex, start))
                    }
                }
                val url = match.value
                val finalUrl = if (url.startsWith("http")) url else "https://$url"
                pushStringAnnotation(tag = "URL", annotation = finalUrl)
                withStyle(
                    style = SpanStyle(
                        color = Color(0xFF6A9F50),
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append(url)
                }
                pop()
                lastIndex = end
            }
            if (lastIndex < text.length) {
                withStyle(style = SpanStyle(color = normalColor)) {
                    append(text.substring(lastIndex))
                }
            }
        }
    }

    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString
                .getStringAnnotations("URL", offset, offset)
                .firstOrNull()
                ?.let { openUrl(it.item) }
        }
    )
}


private val urlRegex =
    "(https?:\\/\\/)?(www\\.)?(youtube\\.com|youtu\\.be|[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})(\\/\\S*)?"
        .toRegex(RegexOption.IGNORE_CASE)


@Composable
fun StoriesRow(
    groups: List<StoryGroup>,
    viewModel: CommunityViewModel,
    onAddStory: () -> Unit,
    onStoryClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        item {
            AddStoryItem(onClick = onAddStory)
        }

        items(
            items = groups,
            key = { it.user.id }
        ) { group ->
            StoryItem(
                user = group.user,
                isViewed = viewModel.isGroupViewed(group),
                onClick = {
                    viewModel.startStoryGroup(group)
                    onStoryClick()
                }
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
                    .background(Color(0xFFF0F7EC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF6A9F50),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Your story",
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
    }
}


@Composable
fun StoryItem(
    user: Users,
    isViewed: Boolean,
    onClick: () -> Unit
) {
    val ring = if (isViewed)
        Brush.linearGradient(listOf(Color(0xFFCCCCCC), Color(0xFFE0E0E0)))
    else
        Brush.linearGradient(
            listOf(Color(0xFF6A9F50), Color(0xFF8BC34A))
        )

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
                .background(ring),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "https://gps.eva2z.in/storage/${user.photo}",
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            user.name,
            maxLines = 1,
            fontSize = 12.sp,
            color = Color(0xFF555555)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreenRoot(
    onBack: () -> Unit,
    onOpenCreatePost: () -> Unit,
    onOpenCreateStory: () -> Unit,
    onOpenStoryViewer: () -> Unit
) {
    // System back → close the overlay and return to home immediately.
    // Composed AFTER the home pager's Voyager handlers so it wins LIFO priority
    // and a single back press is enough.
    PlatformBackHandler(enabled = true) { onBack() }

    val viewModel = CommunityViewModelProvider.viewModel
    val state = viewModel.state

    var openCommentsPostId by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) {
        onDispose { viewModel.cancelActiveJobs() }
    }

    LaunchedEffect(Unit) {
        if (state.posts.isEmpty()) {
            viewModel.loadPosts()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Community",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF222222)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancelActiveJobs()
                        onBack()
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = Color(0xFF333333)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CommunityScreen(
                posts = state.posts,
                stories = state.stories,
                authUserId = state.authUserId,
                viewModel = viewModel,
                onCreatePostClick = onOpenCreatePost,
                onAddStory = onOpenCreateStory,
                onStoryClick = onOpenStoryViewer,
                onLikeClick = viewModel::toggleLike,
                onCommentClick = { openCommentsPostId = it }
            )
        }
    }

    openCommentsPostId?.let {
        CommentBottomSheet(
            postId = it,
            viewModel = viewModel,
            onDismiss = { openCommentsPostId = null }
        )
    }
}