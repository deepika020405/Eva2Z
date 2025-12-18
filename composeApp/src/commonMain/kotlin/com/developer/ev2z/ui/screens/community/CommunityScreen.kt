package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.developer.ev2z.response.Post
import com.developer.ev2z.response.Users

@Composable
fun CommunityScreen(
    posts: List<Post>,
    onCreatePostClick: () -> Unit,
    onLikeClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FA))
    ) {
         val BASE_IMAGE_URL = "https://gps.eva2z.in"

        item {
            CreatePostCard(onClick = onCreatePostClick)
        }

        items(posts) { post ->
            PostItem(
                post = post,
                BASE_IMAGE_URL = BASE_IMAGE_URL,
                onLikeClick = onLikeClick   // ✅ pass-through
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
        elevation = CardDefaults.cardElevation(4.dp)
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
fun PostItem(post: Post,BASE_IMAGE_URL: String,onLikeClick: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            PostHeader(post.user, post.created_at,BASE_IMAGE_URL)

            Spacer(Modifier.height(8.dp))

            // ✅ Text-only OR Text+Image
            post.title?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
            }
            post.body?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }

            // ✅ Image-only OR Text+Image
            if (post.media_urls.isNotEmpty()) {
                PostImages(post.media_urls,BASE_IMAGE_URL)
                Spacer(Modifier.height(8.dp))
            }

            PostFooter(
                post = post,
                onLikeClick = onLikeClick
            )
        }
    }
}


@Composable
fun PostHeader(
    user: Users,
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
            if (!user.photo.isNullOrEmpty()) {
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
                    text = user.name.firstOrNull()?.uppercase() ?: "U",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }



        Spacer(Modifier.width(10.dp))

        Column {
            Text(text = user.name, fontWeight = FontWeight.SemiBold)
            Text(
                text = createdAt.take(10),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}




@Composable
fun PostImages(
    images: List<String>?,
    BASE_IMAGE_URL: String
) {
    if (images.isNullOrEmpty()) return   // ✅ visibility hata di

    var previewImage by remember { mutableStateOf<String?>(null) }

    LazyRow {
        items(images) { image ->

            val fullUrl = "$BASE_IMAGE_URL$image"

            NetworkImage(
                url = fullUrl,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(160.dp)
                    .clip(RoundedCornerShape(12.dp)) // 👈 side radius
                    .clickable {
                        previewImage = fullUrl       // 👈 fullscreen open
                    }
            )
        }
    }

    // Fullscreen overlay
    previewImage?.let {
        FullScreenImage(
            imageUrl = it,
            onDismiss = { previewImage = null }
        )
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
    onLikeClick: (Int) -> Unit
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
            color = Color.Gray
        )
    }
}

class CommunityScreenWrapper : Screen {

    @Composable
    override fun Content() {


        val viewModel = CommunityViewModelProvider.viewModel
        val state = viewModel.state
        val navigator = LocalNavigator.currentOrThrow


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
                    onCreatePostClick = {
                        navigator.push(
                            CreatePostScreen()
                        )
                    },
                    onLikeClick = { postId ->
                        viewModel.toggleLike(postId)
                    }
                )
            }

        }
    }
}




