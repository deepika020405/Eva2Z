@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.ui.screens.SOSParkingTag.ChatThread
import com.developer.ev2z.ui.screens.SOSParkingTag.ChatThreadListUiState
import com.developer.ev2z.ui.screens.SOSParkingTag.SosTagViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

// Singleton for reactive badge count — updated by HomeNavGraph, collected by HomeTab
object NotificationBadgeManager {
    val unreadCount = MutableStateFlow(0)
}

// ─── Colors ───────────────────────────────────────────────────────────────────

private val NBg          = Color(0xFFF4F6F9)
private val NCard        = Color.White
private val NGreen       = Color(0xFF00A859)
private val NGreenLight  = Color(0xFFE6F7EF)
private val NTextPrimary = Color(0xFF111827)
private val NTextSecond  = Color(0xFF6B7280)
private val NTextHint    = Color(0xFFADB5BD)
private val NDivider     = Color(0xFFF0F2F5)
private val NUnreadDot   = Color(0xFF00A859)
private val NRedBadge    = Color(0xFFEF4444)

private val AvatarPalette = listOf(
    Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFFEF5350),
    Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFFFF7043),
    Color(0xFF66BB6A), Color(0xFFEC407A), Color(0xFF4A7D39),
    Color(0xFF00ACC1), Color(0xFFFF8F00), Color(0xFF6D4C41)
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun NotificationsScreen(
    viewModel: SosTagViewModel,
    onBackPressed: () -> Unit,
    onNotificationClick: (chatId: Int, visitorName: String, visitorPhone: String?) -> Unit
) {
    PlatformBackHandler(enabled = true) { onBackPressed() }

    val threadListState by viewModel.threadListState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadChatsWithMessages()
        while (true) {
            delay(15_000)
            viewModel.refreshChatsWithMessages()
        }
    }

    Scaffold(
        containerColor = NBg,
        topBar = { NotificationsTopBar(onBackPressed = onBackPressed) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = threadListState) {
                is ChatThreadListUiState.Loading -> NotificationsLoading()
                is ChatThreadListUiState.Success -> {
                    if (state.threads.isEmpty()) {
                        NotificationsEmpty()
                    } else {
                        NotificationsList(
                            threads = state.threads,
                            onNotificationClick = onNotificationClick
                        )
                    }
                }
                is ChatThreadListUiState.Error -> NotificationsError(
                    message = state.message,
                    onRetry = { viewModel.loadChatsWithMessages() }
                )
                else -> NotificationsLoading()
            }
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun NotificationsTopBar(onBackPressed: () -> Unit) {
    Surface(color = NCard, shadowElevation = 0.5.dp) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NGreenLight)
                    .clickable { onBackPressed() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = NGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(
                "Notifications",
                fontSize   = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = NTextPrimary
            )
        }
    }
}

// ─── List ─────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationsList(
    threads: List<ChatThread>,
    onNotificationClick: (chatId: Int, visitorName: String, visitorPhone: String?) -> Unit
) {
    // Group threads by day bucket
    val grouped = remember(threads) { groupThreadsByDay(threads) }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        grouped.forEach { (label, dayThreads) ->
            item(key = "header_$label") {
                DayHeader(label)
            }
            items(dayThreads, key = { it.chat_id }) { thread ->
                NotificationRow(
                    thread  = thread,
                    onClick = {
                        onNotificationClick(
                            thread.chat_id,
                            thread.visitor?.name ?: "Visitor",
                            thread.visitor?.phone
                        )
                    }
                )
                HorizontalDivider(color = NDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 74.dp))
            }
        }
    }
}

// ─── Row ──────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationRow(thread: ChatThread, onClick: () -> Unit) {
    val isUnread   = thread.isOpen
    val visitorName = thread.visitor?.name ?: "Visitor"
    val avatarColor = AvatarPalette[visitorName.hashCode().rem(AvatarPalette.size).let { if (it < 0) it + AvatarPalette.size else it }]
    val initials   = visitorName.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    val lastMsg    = thread.last_message?.text ?: thread.messages.lastOrNull()?.text ?: "New scan alert"
    val timeStr    = relativeTime(thread.last_message_at)

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(if (isUnread) NGreen.copy(alpha = 0.03f) else NCard)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier         = Modifier.size(50.dp).clip(CircleShape).background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(initials.ifBlank { "V" }, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.width(14.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    visitorName,
                    fontSize   = 15.sp,
                    fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color      = NTextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                Text(timeStr, fontSize = 11.sp, color = NTextHint)
            }

            Spacer(Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode2, null, tint = NGreen, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    thread.visitor?.phone?.let { "· $it" } ?: "",
                    fontSize = 12.sp, color = NTextSecond, maxLines = 1
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                lastMsg,
                fontSize   = 13.sp,
                color      = if (isUnread) NTextPrimary else NTextSecond,
                fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(10.dp))

        // Unread indicator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NUnreadDot)
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier         = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isUnread) NGreenLight else Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isUnread) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                    null,
                    tint     = if (isUnread) NGreen else NTextHint,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ─── Day header ───────────────────────────────────────────────────────────────

@Composable
private fun DayHeader(label: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(color = NDivider, modifier = Modifier.weight(1f))
        Text(
            label,
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color      = NTextHint,
            modifier   = Modifier.padding(horizontal = 10.dp)
        )
        HorizontalDivider(color = NDivider, modifier = Modifier.weight(1f))
    }
}

// ─── States ───────────────────────────────────────────────────────────────────

@Composable
private fun NotificationsLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NGreen, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
            Spacer(Modifier.height(14.dp))
            Text("Loading notifications…", fontSize = 14.sp, color = NTextSecond)
        }
    }
}

@Composable
private fun NotificationsEmpty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(NGreen.copy(0.15f), NGreen.copy(0.04f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.NotificationsNone, null, tint = NGreen, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("No Notifications Yet", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = NTextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "When someone scans your vehicle tag,\nyou'll see their message here.",
                fontSize  = 13.sp,
                color     = NTextSecond,
                lineHeight = 19.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun NotificationsError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CloudOff, null, tint = NTextHint, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Couldn't load notifications", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NTextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(message, fontSize = 12.sp, color = NTextSecond)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors  = ButtonDefaults.buttonColors(containerColor = NGreen),
                shape   = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry")
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun groupThreadsByDay(threads: List<ChatThread>): LinkedHashMap<String, List<ChatThread>> {
    val result = LinkedHashMap<String, List<ChatThread>>()
    val today     = mutableListOf<ChatThread>()
    val yesterday = mutableListOf<ChatThread>()
    val earlier   = mutableListOf<ChatThread>()

    val nowMs = kotlin.time.Clock.System.now().toEpochMilliseconds()
    val oneDayMs  = 86_400_000L
    val twoDayMs  = 172_800_000L

    threads.forEach { thread ->
        val ts = parseIsoTimestampMs(thread.last_message_at)
        val bucket = if (ts == null) earlier
        else when {
            nowMs - ts < oneDayMs  -> today
            nowMs - ts < twoDayMs  -> yesterday
            else                   -> earlier
        }
        bucket.add(thread)
    }

    if (today.isNotEmpty())     result["Today"]     = today
    if (yesterday.isNotEmpty()) result["Yesterday"] = yesterday
    if (earlier.isNotEmpty())   result["Earlier"]   = earlier
    return result
}

private fun parseIsoTimestampMs(timestamp: String?): Long? {
    if (timestamp.isNullOrBlank()) return null
    return try {
        Instant.parse(timestamp).toEpochMilliseconds()
    } catch (_: Exception) {
        try {
            val iso = timestamp.trim().replace(" ", "T")
            val ldt = kotlinx.datetime.LocalDateTime.parse(iso)
            ldt.toInstant(TimeZone.UTC).toEpochMilliseconds()
        } catch (_: Exception) { null }
    }
}

private fun relativeTime(timestamp: String?): String {
    val ms  = parseIsoTimestampMs(timestamp) ?: return ""
    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
    val diff = now - ms
    return when {
        diff < 60_000L         -> "Just now"
        diff < 3_600_000L      -> "${diff / 60_000}m ago"
        diff < 86_400_000L     -> {
            val h = diff / 3_600_000L
            "${h}h ago"
        }
        diff < 172_800_000L    -> "Yesterday"
        else                   -> {
            try {
                val ldt = Instant.fromEpochMilliseconds(ms)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                "${months[ldt.monthNumber - 1]} ${ldt.dayOfMonth}"
            } catch (_: Exception) { "" }
        }
    }
}

// ─── Badge count helper (used by HomeScreen) ──────────────────────────────────

fun countUnreadNotifications(state: ChatThreadListUiState): Int =
    (state as? ChatThreadListUiState.Success)?.threads?.count { it.isOpen } ?: 0