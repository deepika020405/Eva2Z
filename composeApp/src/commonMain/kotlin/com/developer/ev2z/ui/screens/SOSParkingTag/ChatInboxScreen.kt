package com.developer.ev2z.ui.screens.SOSParkingTag

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler

// ─── Design System ────────────────────────────────────────────────────────────

private val ColorBg            = Color(0xFFF4F4F8)
private val ColorCard          = Color(0xFFFFFFFF)
private val ColorPrimary       = Color(0xFF4A7D39)
private val ColorPrimaryDark   = Color(0xFF3A6029)
private val ColorPrimaryLight  = Color(0xFF6FA058)
private val ColorUnread        = Color(0xFF4CAF50)
private val ColorOnline        = Color(0xFF44B700)
private val ColorTextTitle     = Color(0xFF0D0D0D)
private val ColorTextBody      = Color(0xFF6B7280)
private val ColorTextMuted     = Color(0xFFADB5BD)
private val ColorTextUnread    = Color(0xFF0D0D0D)
private val ColorDivider       = Color(0xFFF0F2F5)
private val ColorClosedBadge   = Color(0xFFE9ECEF)
private val ColorClosedText    = Color(0xFF9CA3AF)
private val ColorTickBlue      = Color(0xFF42A5F5)

private val AvatarColors = listOf(
    Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFFEF5350),
    Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFFFF7043),
    Color(0xFF66BB6A), Color(0xFFEC407A), Color(0xFF4A7D39)
)

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun ChatInboxScreen(
    viewModel: SosTagViewModel,
    onChatSelected: (chatId: Int, visitorName: String, visitorPhone: String?) -> Unit,
    onBackPressed: () -> Unit
) {
    PlatformBackHandler(enabled = true) { onBackPressed() }

    val threadListState by viewModel.threadListState.collectAsState()
    val onlineChats    by viewModel.onlineChats.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadChatsWithMessages()
        // Periodic silent refresh — keeps inbox live when socket is not connected on this screen.
        // refreshChatsWithMessages() doesn't show a loading spinner so there's no UI flicker.
        while (true) {
            kotlinx.coroutines.delay(10_000)
            viewModel.refreshChatsWithMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ColorBg)) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar
            PremiumTopBar(
                onBackPressed = onBackPressed,
                onRefresh = { viewModel.loadChatsWithMessages() }
            )

            // ── Body
            AnimatedContent(
                targetState = threadListState,
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(120))
                },
                label = "inbox_state"
            ) { state ->
                when (state) {
                    is ChatThreadListUiState.Loading -> LoadingState()

                    is ChatThreadListUiState.Error -> EmptyState(
                        icon = Icons.Outlined.WifiOff,
                        title = "Couldn't load chats",
                        subtitle = state.message,
                        actionLabel = "Retry",
                        onAction = { viewModel.loadChatsWithMessages() }
                    )

                    is ChatThreadListUiState.Success -> {
                        if (state.threads.isEmpty()) {
                            EmptyState(
                                icon = Icons.Outlined.Forum,
                                title = "No conversations yet",
                                subtitle = "When visitors scan your parking tag and send a message, their chats will appear here.",
                                actionLabel = null,
                                onAction = {}
                            )
                        } else {
                            ThreadList(
                                threads = state.threads,
                                onlineChats = onlineChats,
                                viewModel = viewModel,
                                onChatSelected = onChatSelected
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun PremiumTopBar(
    onBackPressed: () -> Unit,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp)
            .background(
                Brush.verticalGradient(
                    listOf(ColorPrimaryDark, ColorPrimary)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    Icons.Default.ArrowBack, "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Inbox avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Forum, null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "QR Scan Chats",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 0.1.sp
                )
                Text(
                    "Parking tag messages",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Default.Refresh, "Refresh",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─── Thread list ─────────────────────────────────────────────────────────────

@Composable
private fun ThreadList(
    threads: List<ChatThread>,
    onlineChats: Set<Int>,
    viewModel: SosTagViewModel,
    onChatSelected: (chatId: Int, visitorName: String, visitorPhone: String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
    ) {
        // Section header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "All Chats",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorPrimary,
                    letterSpacing = 0.6.sp
                )
                Spacer(Modifier.width(8.dp))
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 0.5.dp,
                    color = ColorDivider
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${threads.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorPrimary
                )
            }
        }

        items(threads, key = { it.chat_id }) { thread ->
            val unreadCount = viewModel.getUnreadCount(thread)
            ThreadCard(
                thread = thread,
                unreadCount = unreadCount,
                isVisitorOnline = thread.chat_id in onlineChats,
                onClick = {
                    viewModel.markChatRead(thread.chat_id, thread.last_message?.id)
                    viewModel.selectThread(thread)
                    val name = thread.visitor?.name?.takeIf { it.isNotBlank() } ?: "Visitor"
                    onChatSelected(thread.chat_id, name, thread.visitor?.phone)
                }
            )
        }
    }
}

// ─── Thread card ─────────────────────────────────────────────────────────────

@Composable
private fun ThreadCard(
    thread: ChatThread,
    unreadCount: Int,
    isVisitorOnline: Boolean,
    onClick: () -> Unit
) {
    val visitorName   = thread.visitor?.name?.takeIf { it.isNotBlank() } ?: "Visitor"
    val displayName   = "Visitor #${thread.chat_id}"
    val lastMsg       = thread.last_message
    val lastMsgText   = lastMsg?.text?.takeIf { it.isNotBlank() } ?: "Parking alert"
    val lastMsgTime   = lastMsg?.time?.takeIf { it.isNotBlank() }
    val ownerSentLast = lastMsg?.type.equals("sent", ignoreCase = true)
    val isOpen        = thread.isOpen
    val hasUnread     = unreadCount > 0

    val avatarColor = AvatarColors[thread.chat_id % AvatarColors.size]
    val avatarLetter = "V"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = ColorCard,
            shadowElevation = if (hasUnread) 3.dp else 1.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Avatar with online dot
                Box(modifier = Modifier.size(52.dp)) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        avatarColor,
                                        avatarColor.copy(alpha = 0.75f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            avatarLetter,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                    // Green dot only when visitor is actually connected via socket right now
                    if (isVisitorOnline) {
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(ColorCard)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(ColorOnline)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // ── Text content
                Column(modifier = Modifier.weight(1f)) {

                    // Row 1: Name + time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayName,
                            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = if (isOpen) ColorTextTitle else ColorTextBody,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(6.dp))
                        if (lastMsgTime != null) {
                            Text(
                                text = lastMsgTime,
                                fontSize = 11.sp,
                                fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (hasUnread) ColorUnread else ColorTextMuted
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Row 2: Preview + badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tick icon driven by real is_delivered / is_seen from API.
                        // is_seen=1  → double blue tick  (visitor read it)
                        // is_delivered=1 only → double grey tick (visitor received it)
                        // neither       → single grey tick (sent, not yet delivered)
                        if (ownerSentLast) {
                            val isSeen      = lastMsg?.is_seen == 1
                            val isDelivered = lastMsg?.is_delivered == 1
                            Icon(
                                imageVector = if (isSeen || isDelivered) Icons.Default.DoneAll
                                              else Icons.Default.Done,
                                contentDescription = null,
                                tint = if (isSeen) ColorTickBlue else ColorTextMuted,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(end = 2.dp)
                            )
                        }

                        Text(
                            text = lastMsgText,
                            fontSize = 13.sp,
                            color = if (hasUnread) ColorTextUnread else ColorTextBody,
                            fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(6.dp))

                        // Right badges row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Closed badge
                            if (!isOpen) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ColorClosedBadge
                                ) {
                                    Text(
                                        "Closed",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ColorClosedText,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Unread count badge
                            AnimatedVisibility(
                                visible = hasUnread,
                                enter = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                exit = scaleOut() + fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                        .clip(CircleShape)
                                        .background(ColorUnread)
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Loading state ────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = ColorPrimary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Loading chats…",
            fontSize = 14.sp,
            color = ColorTextBody
        )
    }
}

// ─── Empty / Error state ─────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            ColorPrimary.copy(alpha = 0.15f),
                            ColorPrimary.copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint = ColorPrimary,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextTitle,
            letterSpacing = 0.2.sp
        )

        Spacer(Modifier.height(10.dp))

        Text(
            subtitle,
            fontSize = 14.sp,
            color = ColorTextBody,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null) {
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    Icons.Default.Refresh, null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    actionLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}