package com.developer.ev2z.ui.screens.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.developer.ev2z.fetchPhoneLocation
import com.developer.ev2z.openLocationSettings
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.response.Comment
import com.developer.ev2z.response.Post
import com.developer.ev2z.response.Story
import com.developer.ev2z.response.StoryGroup
import com.developer.ev2z.response.Users
import com.developer.ev2z.ui.screens.Profile.ProfileImageZoomDialog
import com.developer.ev2z.ui.screens.homeScreen.LocalHomeTheme
import com.developer.ev2z.ui.screens.social_media.openUrl
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.text.firstOrNull
import kotlin.time.ExperimentalTime
import com.developer.ev2z.theme.AppPageBg

// ─────────────────────────────────────────────────────────────────
// Design tokens
// ─────────────────────────────────────────────────────────────────
private val CommScreenBg  = AppPageBg
private val CommCardBg    = Color(0xFFFFFFFF)
private val CommTopBarBg  = Color(0xFFFFFFFF)
private val CommGreen     = Color(0xFF00A844)
private val CommGreenSoft = Color(0xFFDCFCE7)
private val CommGreenText = Color(0xFF15803D)
private val CommTitleDark = Color(0xFF0F172A)
private val CommBodyGray  = Color(0xFF475569)
private val CommSubtle    = Color(0xFF94A3B8)
private val CommDivider   = Color(0xFFE2E8F0)
private val CommChipBg    = Color(0xFFF8FAFC)
private val CommStar      = Color(0xFFD97706)

// ─────────────────────────────────────────────────────────────────
// Category helper
// ─────────────────────────────────────────────────────────────────
private data class PostCategoryStyle(val label: String, val textColor: Color, val bgColor: Color)

private fun styleForLabel(label: String): PostCategoryStyle = when (label.uppercase()) {
    "FOUND"   -> PostCategoryStyle("FOUND",   Color(0xFF15803D), Color(0xFFDCFCE7))
    "STOLEN"  -> PostCategoryStyle("STOLEN",  Color(0xFFB91C1C), Color(0xFFFEE2E2))
    "LOST"    -> PostCategoryStyle("LOST",    Color(0xFFB45309), Color(0xFFFEF3C7))
    "POLICE"  -> PostCategoryStyle("POLICE",  Color(0xFF1D4ED8), Color(0xFFDBEAFE))
    "TIPS"    -> PostCategoryStyle("TIPS",    Color(0xFF92400E), Color(0xFFFEF3C7))
    "TRAFFIC" -> PostCategoryStyle("TRAFFIC", Color(0xFFC2410C), Color(0xFFFFEDD5))
    else      -> PostCategoryStyle("POST",    Color(0xFF475569), Color(0xFFF8FAFC))
}

private fun derivePostCategory(post: Post): PostCategoryStyle {
    // Prefer the explicit category from the API; fall back to keyword detection.
    post.category?.takeIf { it.isNotBlank() }?.let { return styleForLabel(it) }

    val text = "${post.title.orEmpty()} ${post.body.orEmpty()}".lowercase()
    return when {
        "found"   in text || "recover" in text             -> styleForLabel("FOUND")
        "stolen"  in text || "theft"   in text             -> styleForLabel("STOLEN")
        "lost"    in text || "missing" in text             -> styleForLabel("LOST")
        "police"  in text || "fir"     in text             -> styleForLabel("POLICE")
        "tips"    in text || "advice"  in text             -> styleForLabel("TIPS")
        "traffic" in text || "jam"     in text             -> styleForLabel("TRAFFIC")
        else                                               -> styleForLabel("POST")
    }
}

private fun filterKey(post: Post): String = derivePostCategory(post).label.lowercase()

private val avatarColors = listOf(
    Color(0xFF2563EB), Color(0xFFA855F7), Color(0xFF10B981),
    Color(0xFFDC2626), Color(0xFFD97706), Color(0xFF06B6D4),
)
private fun postAvatarColor(userId: Int) = avatarColors[kotlin.math.abs(userId) % avatarColors.size]

private data class CommTab(val id: String, val label: String, val emoji: String)


private val communityTabs = listOf(
    CommTab("all",     "All",     "📍"),
    CommTab("stolen",  "Stolen",  "🚗"),
    CommTab("lost",    "Lost",    "🔍"),
    CommTab("found",   "Found",   "✋"),
    CommTab("tips",    "Tips",    "💡"),
    CommTab("traffic", "Traffic", "🚦"),
    CommTab("police",  "Police",  "🚓"),
)

@Composable
fun CommunityScreen(
    posts: List<Post>,
    authUserId: Int,
    onCreatePostClick: () -> Unit,
    onLikeClick: (Int) -> Unit,
    viewModel: CommunityViewModel,
    onCommentClick: (Int) -> Unit,
) {
    val _ht           = LocalHomeTheme.current
    val CommScreenBg  = if (_ht.isDark) _ht.palette.pageBg        else CommScreenBg
    val CommCardBg    = if (_ht.isDark) _ht.palette.cardBg         else CommCardBg
    val CommTitleDark = if (_ht.isDark) _ht.palette.textPrimary    else CommTitleDark
    val CommBodyGray  = if (_ht.isDark) _ht.palette.textSecondary  else CommBodyGray
    val CommSubtle    = if (_ht.isDark) _ht.palette.textMuted      else CommSubtle
    val CommDivider   = if (_ht.isDark) _ht.palette.borderSoft     else CommDivider

    val BASE_IMAGE_URL   = "https://gps.eva2z.in"
    var selectedTab by remember { mutableStateOf("all") }

    val filteredPosts = remember(selectedTab, posts) {
        if (selectedTab == "all") posts
        else posts.filter { filterKey(it) == selectedTab }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = CommScreenBg) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Responsive horizontal padding: tighter on small phones, roomier on wide screens.
            val hPad = (maxWidth * 0.04f).coerceIn(12.dp, 22.dp)

            // ── Main list ──────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(
                    start = hPad, end = hPad,
                    top = 8.dp, bottom = 100.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Tab row
                item {
                    CommunityTabRow(
                        tabs       = communityTabs,
                        selectedId = selectedTab,
                        onSelect   = { selectedTab = it },
                    )
                }

                // Posts
                items(filteredPosts, key = { it.id }) { post ->
                    PostItem(
                        post           = post,
                        BASE_IMAGE_URL = BASE_IMAGE_URL,
                        onLikeClick    = onLikeClick,
                        onCommentClick = onCommentClick,
                    )
                }

                // Empty state
                if (filteredPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📭", fontSize = 35.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("No posts in this category", fontSize = 13.sp, color = CommSubtle)
                            }
                        }
                    }
                }
            }

            // ── FAB ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CommGreen)
                        .clickable(onClick = onCreatePostClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✏️", fontSize = 20.sp)
                }
            }
        }
    }
}


@Composable
fun CreatePostCard(onClick: () -> Unit) {
    val _ht         = LocalHomeTheme.current
    val CommCardBg  = if (_ht.isDark) _ht.palette.cardBg     else CommCardBg
    val CommDivider = if (_ht.isDark) _ht.palette.borderSoft else CommDivider
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CommCardBg)
            .border(BorderStroke(1.dp, CommDivider), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CommGreenSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text("✏️", fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "Share something with community…",
            color    = CommSubtle,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Tab row
// ─────────────────────────────────────────────────────────────────

@Composable
private fun CommunityTabRow(
    tabs: List<CommTab>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    val _ht          = LocalHomeTheme.current
    val CommCardBg   = if (_ht.isDark) _ht.palette.cardBg        else CommCardBg
    val CommBodyGray = if (_ht.isDark) _ht.palette.textSecondary else CommBodyGray
    val CommDivider  = if (_ht.isDark) _ht.palette.borderSoft    else CommDivider
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEach { tab ->
            val selected = tab.id == selectedId
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) CommGreenSoft else CommCardBg)
                    .border(BorderStroke(1.dp, if (selected) CommGreen else CommDivider), RoundedCornerShape(18.dp))
                    .clickable { onSelect(tab.id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tab.emoji, fontSize = 12.sp)
                Spacer(Modifier.width(5.dp))
                Text(
                    tab.label,
                    fontSize   = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color      = if (selected) CommGreenText else CommBodyGray,
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
    onCommentClick: (Int) -> Unit,
) {
    val _ht           = LocalHomeTheme.current
    val CommCardBg    = if (_ht.isDark) _ht.palette.cardBg        else CommCardBg
    val CommTitleDark = if (_ht.isDark) _ht.palette.textPrimary   else CommTitleDark
    val CommBodyGray  = if (_ht.isDark) _ht.palette.textSecondary else CommBodyGray
    val CommDivider   = if (_ht.isDark) _ht.palette.borderSoft    else CommDivider
    val category = remember(post.id) { derivePostCategory(post) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CommCardBg)
            .padding(14.dp),
    ) {
        // ── Header ────────────────────────────────────────────────
        PostHeader(post.user, post.created_at, BASE_IMAGE_URL)

        Spacer(Modifier.height(10.dp))

        // ── Category badge + city ─────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(category.bgColor)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(category.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = category.textColor)
            }

            post.city?.takeIf { it.isNotBlank() }?.let { city ->
                Spacer(Modifier.width(8.dp))
                Text("📍", fontSize = 10.sp)
                Spacer(Modifier.width(2.dp))
                Text(city, fontSize = 11.sp, color = CommBodyGray)
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Title ─────────────────────────────────────────────────
        post.title?.takeIf { it.isNotBlank() }?.let {
            Text(it, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CommTitleDark)
            Spacer(Modifier.height(5.dp))
        }

        // ── Body ──────────────────────────────────────────────────
        post.body?.takeIf { it.isNotBlank() }?.let {
            LinkText(text = it, normalColor = CommBodyGray)
            Spacer(Modifier.height(8.dp))
        }

        // ── Images ────────────────────────────────────────────────
        if (post.media_urls.isNotEmpty()) {
            FacebookPostImages(images = post.media_urls.map { "$BASE_IMAGE_URL$it" })
            Spacer(Modifier.height(8.dp))
        }

        // ── Verified ──────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✓", color = CommGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text("Verified by Eva2z", fontSize = 11.sp, color = CommGreen)
        }

        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(CommDivider))
        Spacer(Modifier.height(10.dp))

        // ── Footer ────────────────────────────────────────────────
        PostFooter(post = post, onLikeClick = onLikeClick, onCommentClick = onCommentClick)
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
            .background(Color(0xFFF8FAFC))
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
                    .background(Color(0xFFF8FAFC))
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
                .background(Color(0xFFF8FAFC))
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
                        .background(Color(0xFFF8FAFC))
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
                        .background(Color(0xFFF8FAFC))
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
                        .background(Color(0xFFF8FAFC))
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
                                fontSize = 22.sp
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
    BASE_IMAGE_URL: String,
) {
    val _ht           = LocalHomeTheme.current
    val CommTitleDark = if (_ht.isDark) _ht.palette.textPrimary else CommTitleDark
    val CommSubtle    = if (_ht.isDark) _ht.palette.textMuted   else CommSubtle
    val avatarColor = postAvatarColor(user?.id ?: 0)
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center,
        ) {
            if (!user?.photo.isNullOrEmpty()) {
                AsyncImage(
                    model              = "$BASE_IMAGE_URL/storage/${user!!.photo}",
                    contentDescription = "Profile Picture",
                    modifier           = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale       = ContentScale.Crop,
                )
            } else {
                Text(
                    text       = user?.name?.firstOrNull()?.uppercase() ?: "U",
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    fontSize   = 14.sp,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    user?.name ?: "—",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = CommTitleDark,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Text("⭐", fontSize = 10.sp)
                Spacer(Modifier.width(2.dp))
                Text("Community", fontSize = 11.sp, color = CommStar)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                formatPostDateCmp(createdAt),
                fontSize = 11.sp,
                color    = CommSubtle,
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
    onCommentClick: (Int) -> Unit,
) {
    val _ht          = LocalHomeTheme.current
    val CommBodyGray = if (_ht.isDark) _ht.palette.textSecondary else CommBodyGray
    val CommSubtle   = if (_ht.isDark) _ht.palette.textMuted     else CommSubtle
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Like
        Row(
            modifier          = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onLikeClick(post.id) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = if (post.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like",
                tint               = if (post.liked) Color(0xFFE53935) else CommSubtle,
                modifier           = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(post.likes_count.toString(), fontSize = 12.sp, color = CommBodyGray)
        }

        Spacer(Modifier.width(20.dp))

        // Comment
        Row(
            modifier          = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onCommentClick(post.id) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("💬", fontSize = 13.sp)
            Spacer(Modifier.width(5.dp))
            Text(post.comments_count.toString(), fontSize = 12.sp, color = CommBodyGray)
        }

        Spacer(Modifier.weight(1f))

        // Report
        Text("🚩  Report", fontSize = 11.sp, color = CommSubtle)
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
                fontSize = 16.sp,
                color = Color(0xFF0F172A),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.dp)
            )

            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)

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
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Text(
                    text = "✕",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
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
                        color = Color(0xFF94A3B8)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00A844),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    cursorColor = Color(0xFF00A844)
                )
            )

            TextButton(
                enabled = input.isNotBlank(),
                onClick = onSend
            ) {
                Text(
                    "Post",
                    color = if (input.isNotBlank()) Color(0xFF00A844)
                    else Color(0xFF94A3B8),
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
                .background(Color(0xFFDCFCE7)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.user.name,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    fontSize = 13.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatPostDateCmp(comment.created_at),
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            if (replyToName != null) {
                Text(
                    text = "@$replyToName",
                    color = Color(0xFF00A844),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }

            LinkText(
                text = comment.comment,
                normalColor = Color(0xFF0F172A)
            )

            Spacer(Modifier.height(4.dp))

            Row {
                Text(
                    text = if (comment.liked) "❤️ ${comment.likes_count}"
                    else "🤍 ${comment.likes_count}",
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onLike() }
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "Reply",
                    color = Color(0xFF00A844),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onReply() }
                )
            }
        }
    }
}

@Composable
fun LinkText(
    text: String,
    normalColor: Color = Color(0xFF0F172A)
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
                        color = Color(0xFF00A844),
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
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF00A844),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Your story",
            fontSize = 11.sp,
            color = Color(0xFF475569)
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
        Brush.linearGradient(listOf(Color(0xFF94A3B8), Color(0xFFE2E8F0)))
    else
        Brush.linearGradient(
            listOf(Color(0xFF00A844), Color(0xFF00A844))
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
            fontSize = 11.sp,
            color = Color(0xFF475569)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreenRoot(
    onBack: () -> Unit,
    onOpenCreatePost: () -> Unit,
) {
    PlatformBackHandler(enabled = true) { onBack() }

    val viewModel = CommunityViewModelProvider.viewModel
    val state     = viewModel.state

    var openCommentsPostId by remember { mutableStateOf<Int?>(null) }

    // ── Current location for the "Nearby" pill ────────────────────
    var locationLabel by remember { mutableStateOf("Nearby") }
    var locating by remember { mutableStateOf(false) }
    var showEnableLocation by remember { mutableStateOf(false) }

    fun fetchCurrentLocation() {
        if (locating) return
        locating = true
        locationLabel = "Locating…"
        // Requests location permission if needed, then delivers lat/lng.
        fetchPhoneLocation { lat, lng ->
            if (lat != 0.0 || lng != 0.0) {
                viewModel.resolveCity(lat, lng) { city ->
                    locationLabel = city.takeIf { it.isNotBlank() } ?: "Nearby"
                    locating = false
                }
            } else {
                // Permission denied or location services are off → ask the user.
                locationLabel = "Enable location"
                locating = false
                showEnableLocation = true
            }
        }
    }

    DisposableEffect(Unit) { onDispose { viewModel.cancelActiveJobs() } }

    LaunchedEffect(Unit) {
        if (state.posts.isEmpty()) viewModel.loadPosts()
        // Proactively show the user's current location (will prompt for permission).
        fetchCurrentLocation()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = CommScreenBg) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .align(Alignment.TopCenter),
            ) {
                // ── Custom top bar ────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CommTopBarBg)
                        .windowInsetsPadding(WindowInsets.statusBars),
                ) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CommChipBg)
                                .clickable { viewModel.cancelActiveJobs(); onBack() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("‹", fontSize = 22.sp, color = CommTitleDark, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Community",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = CommTitleDark,
                            modifier   = Modifier.weight(1f),
                        )
                        // Location pill — tap to (re)fetch current location
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(CommChipBg)
                                .clickable { fetchCurrentLocation() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .widthIn(max = 160.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("📍", fontSize = 11.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                locationLabel,
                                fontSize = 12.sp,
                                color = CommBodyGray,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // ── Feed ─────────────────────────────────────────
                Box(modifier = Modifier.weight(1f)) {
                    CommunityScreen(
                        posts            = state.posts,
                        authUserId       = state.authUserId,
                        viewModel        = viewModel,
                        onCreatePostClick = onOpenCreatePost,
                        onLikeClick      = viewModel::toggleLike,
                        onCommentClick   = { openCommentsPostId = it },
                    )
                }
            }
        }
    }

    openCommentsPostId?.let {
        CommentBottomSheet(
            postId    = it,
            viewModel = viewModel,
            onDismiss = { openCommentsPostId = null },
        )
    }

    if (showEnableLocation) {
        EnableLocationDialog(
            onDismiss = { showEnableLocation = false },
            onOpenSettings = {
                showEnableLocation = false
                openLocationSettings()
            },
        )
    }
}

@Composable
private fun EnableLocationDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Turn on location", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Location is off. Enable location to show your current city and nearby community posts.",
                fontSize = 13.sp,
                color = Color(0xFF475569),
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open settings", color = CommGreen, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color.White,
    )
}