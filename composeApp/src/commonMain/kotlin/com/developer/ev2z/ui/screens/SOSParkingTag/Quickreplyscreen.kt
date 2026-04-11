package com.developer.ev2z.ui.screens.SOSParkingTag


import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.util.SosNotificationHandler
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// ─────────────────────────────────────────────────────────────────
// Design Tokens — Light green + white (matching app theme)
// (Previously lived in Sostagalertscreen.kt — now centralized here.)
// ─────────────────────────────────────────────────────────────────

internal val BrandGreen      = Color(0xFF4A7D39)
internal val BrandGreenLight = Color(0xFFF1F4ED)
internal val BrandGreenMid   = Color(0xFFCCD7C7)
internal val AccentGreen     = Color(0xFF85B070)
internal val SoftGreen       = Color(0xFF9DD680)
internal val BackgroundColor = Color(0xFFF8FAF0)
internal val CardColor       = Color.White
internal val TextPrimary     = Color(0xFF252A23)
internal val TextSecondary   = Color(0xFF5D6858)
internal val TextTertiary    = Color(0xFF9CA3AF)
internal val BorderColor     = Color(0xFFCCD7C7)
internal val ErrorRed        = Color(0xFFE5565B)
internal val SuccessGreen    = Color(0xFF4A7D39)
internal val SuccessLight    = Color(0xFFE8F5E0)

// ─────────────────────────────────────────────────────────────────
// Chat Color Tokens — light green + white (app theme)
// ─────────────────────────────────────────────────────────────────

private val ChatBg            = Color(0xFFF8FAF0)
private val OwnerBubble       = Color(0xFFE8F5E0)
private val VisitorBubble     = Color.White
private val ChatTopBarColor   = Color(0xFF4A7D39)
private val ChatTopBarLight   = Color(0xFF85B070)
private val SendBtnColor      = Color(0xFF4A7D39)
private val TimestampColor    = Color(0xFF5D6858)
private val ChipBg            = Color.White
private val ChipBorder        = Color(0xFF85B070)

// ─────────────────────────────────────────────────────────────────
// Local Chat Message Model
// ─────────────────────────────────────────────────────────────────

private data class ChatMessage(
    val text: String,
    val isOwner: Boolean,
    val timestamp: String,
    val sortKey: Long,                       // ms-since-epoch — drives WhatsApp-like time order
    val status: MessageStatus = MessageStatus.SENT,
    val isSystemMessage: Boolean = false,
    val localId: Long = 0L                   // tie-breaker for owner replies sent in same ms
)

private enum class MessageStatus { SENDING, SENT, DELIVERED, ERROR }

// ─────────────────────────────────────────────────────────────────
// QuickReplyScreen — Direct Chat UI (opens from notification)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
@Composable
fun QuickReplyScreen(
    alertId: Int,
    viewModel: SosTagViewModel,
    onConfirmed: () -> Unit,
    onBackPressed: () -> Unit
) {
    // System back → close the chat overlay and return to home immediately.
    // Composed AFTER the home pager's Voyager handlers so it wins LIFO priority
    // and a single back press is enough.
    PlatformBackHandler(enabled = true) { onBackPressed() }

    val replyState by viewModel.replyState.collectAsState()
    val focusManager = LocalFocusManager.current

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var hasReplied by remember { mutableStateOf(false) }
    // Resolve which chat to load: explicit notification id, then last cached, then 0 (= empty state).
    val initialChatId = remember(alertId) {
        if (alertId > 0) alertId else (SosNotificationHandler.lastChatId ?: 0)
    }
    var activeAlertId by remember { mutableStateOf(initialChatId) }
    // Server message ids we've already rendered, so polling never duplicates a bubble.
    val seenServerMessageIds = remember { mutableSetOf<Int>() }
    // Track latest visitor info for top bar — seeded from the FCM payload stashed in
    // SosNotificationHandler, since /chat-messages/{chatId} doesn't return visitor info.
    var latestVisitorName by remember {
        mutableStateOf(SosNotificationHandler.lastVisitorName ?: "Visitor")
    }
    var latestVisitorPhone by remember {
        mutableStateOf(SosNotificationHandler.lastVisitorPhone)
    }
    var isLoading by remember { mutableStateOf(true) }
    // Server marks a chat as closed via `ChatMessagesResponse.closed`. Once that flips,
    // treat the screen like there's no active alert — visitor header + input hide,
    // and the "No Alert Found" empty state is shown.
    var isChatClosed by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Helper — apply a server message into the chat (idempotent on message id).
    // `type=="sent"` → owner bubble, anything else → visitor bubble.
    fun applyServerMessage(msg: ChatMessageDto) {
        val mid = msg.id ?: return
        if (mid in seenServerMessageIds) return
        val text = msg.message?.takeIf { it.isNotBlank() } ?: return
        seenServerMessageIds.add(mid)
        val isOwner = msg.type.equals("sent", ignoreCase = true)
        val sortMs = parseTimestampMs(msg.timestamp) ?: Clock.System.now().toEpochMilliseconds()
        val display = msg.time ?: formatMsTime(sortMs)
        messages = (messages + ChatMessage(
            text = text,
            isOwner = isOwner,
            timestamp = display,
            sortKey = sortMs,
            // Use the server id as the tie-breaker so message order is stable across polls.
            localId = mid.toLong(),
            status = if (isOwner) MessageStatus.DELIVERED else MessageStatus.SENT
        )).sortedWith(compareBy({ it.sortKey }, { it.localId }))
    }

    // Helper — render an entire chat response (initial seed and ongoing reconciliation).
    // Visitor name/phone are not in this endpoint's payload (see Sostagmodels.kt), so we
    // only reconcile messages here. The `closed` flag flips the screen into empty state.
    fun applyChatThread(thread: ChatMessagesResponse) {
        if (thread.closed) isChatClosed = true
        thread.data.forEach { applyServerMessage(it) }
    }

    // ── Load full chat history + live polling for the focused chat ──
    LaunchedEffect(Unit) {
        if (activeAlertId <= 0) {
            // No notification, no cached chat → empty state.
            isLoading = false
            return@LaunchedEffect
        }

        viewModel.fetchChatMessages(activeAlertId)?.let { applyChatThread(it) }
        isLoading = false

        // Polling — refetch the same chat every 2s and append any new messages by id.
        // Stop once the server tells us the chat is closed (empty state takes over).
        while (!isChatClosed) {
            kotlinx.coroutines.delay(2000)
            try {
                val thread = viewModel.fetchChatMessages(activeAlertId) ?: continue
                val freshIds = thread.data.mapNotNull { it.id }.filter { it !in seenServerMessageIds }
                if (freshIds.isNotEmpty()) {
                    println("📡 Live: chat #$activeAlertId +${freshIds.size} new message(s)")
                }
                applyChatThread(thread)
                // Pick up any newer visitor info that arrived via a fresh FCM push.
                SosNotificationHandler.lastVisitorName?.let { latestVisitorName = it }
                SosNotificationHandler.lastVisitorPhone?.let { latestVisitorPhone = it }
            } catch (_: Exception) { /* retry next cycle */ }
        }
    }

    // Handle reply success/error — refresh chat from server (single source of truth).
    LaunchedEffect(replyState) {
        when (replyState) {
            is SosReplyUiState.Success -> {
                hasReplied = true
                // Pull the freshly-stored owner message immediately instead of waiting 2s.
                viewModel.fetchChatMessages(activeAlertId)?.let { applyChatThread(it) }
                viewModel.resetReplyState()
            }
            is SosReplyUiState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to send. Try again.")
                }
                viewModel.resetReplyState()
            }
            else -> {}
        }
    }

    // Auto-scroll on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || activeAlertId <= 0) return
        val trimmed = text.trim()

        inputText = ""
        focusManager.clearFocus()

        // No optimistic local bubble — the server is the source of truth.
        // The Success branch of the replyState effect re-fetches the chat immediately so the
        // owner sees their own message within ~150ms.
        viewModel.selectReply(QuickReplyOption.CUSTOM)
        viewModel.onCustomMessageChange(trimmed)
        viewModel.submitReply(activeAlertId)
    }

    Scaffold(
        containerColor = ChatBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // When there's no active alert (or the chat has been closed on the
            // server), skip the visitor header entirely — only a minimal bar with
            // the back button sits above the empty state.
            if (activeAlertId > 0 && !isChatClosed) {
                ChatTopBar(
                    visitorName = latestVisitorName,
                    visitorPhone = latestVisitorPhone,
                    showClose = hasReplied,
                    onBackPressed = onBackPressed,
                    onCallPressed = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Coming soon")
                        }
                    },
                    onClosePressed = {
                        // Hit POST /api/owner/chat/{chatId}/close, then route to confirmation.
                        viewModel.closeChat(activeAlertId, onDone = onConfirmed)
                    }
                )
            } else {
                Surface(color = ChatBg) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                Icons.Default.ArrowBack, "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Hide the reply chips + input bar when there is no active alert or
            // the chat has been closed — nothing to reply to.
            if (!isLoading && activeAlertId > 0 && !isChatClosed) {
                Column {
                    // Quick reply chips — always visible so the owner can use them
                    // even after they've already typed/sent a custom message.
                    QuickReplyChips(
                        onChipSelected = { option ->
                            val text = when (option) {
                                QuickReplyOption.COMING_SOON -> option.title
                                QuickReplyOption.THANKS  -> option.title
                                QuickReplyOption.URGENT  -> option.title
                                QuickReplyOption.SORRY       -> option.title
                                QuickReplyOption.CUSTOM      -> return@QuickReplyChips
                            }
                            sendMessage(text)
                        },
                        onIgnore = onBackPressed
                    )

                    // Message input bar — bottom bar height is now stable across reply state,
                    // so messages no longer get cropped when "Close Chat" appears.
                    ChatInputBar(
                        value = inputText,
                        onValueChange = { inputText = it },
                        onSend = { sendMessage(inputText) },
                        isSending = replyState is SosReplyUiState.Loading
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Loading state
            if (isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = BrandGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        "Loading chat...",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
            // Empty state — no notification, no cached chat id, or the chat has
            // been closed on the server.
            else if (activeAlertId <= 0 || isChatClosed) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(BrandGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        "No Alert Found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        "You'll see parking alerts here when a visitor reports your vehicle.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
            // Chat messages
            else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Date header
                    item {
                        DateHeader(text = "Today")
                    }

                    // Alert banner
                    item {
                        AlertInfoBanner(alertId = activeAlertId)
                    }

                    // Chat bubbles
                    items(messages) { message ->
                        if (message.isSystemMessage) {
                            SystemMessage(text = message.text)
                        } else {
                            ChatBubble(message = message)
                        }
                    }

                    // Typing indicator
                    if (replyState is SosReplyUiState.Loading) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Chat Top Bar
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ChatTopBar(
    visitorName: String,
    visitorPhone: String?,
    showClose: Boolean,
    onBackPressed: () -> Unit,
    onCallPressed: () -> Unit,
    onClosePressed: () -> Unit
) {
    Surface(
        color = ChatTopBarColor,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    Icons.Default.ArrowBack, "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ChatTopBarLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = visitorName.firstOrNull()?.uppercase() ?: "V",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name & alert type
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = visitorName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = visitorPhone ?: "Online",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }

            // Call button
            IconButton(onClick = onCallPressed) {
                Icon(
                    Icons.Default.Call, "Call",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // "Mark resolved" / Close chat — appears after the owner sends their first reply.
            // Lives in the top bar so the bottom message area never gets pushed up or cropped.
            AnimatedVisibility(
                visible = showClose,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Surface(
                    onClick = onClosePressed,
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Close, "Mark chat closed",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Close chat",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Date Header
// ─────────────────────────────────────────────────────────────────

@Composable
private fun DateHeader(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF1F4ED))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF5D6858)
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// System Message — centered info pill (like WhatsApp encryption notice)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SystemMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF1F4ED))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = Color(0xFF5D6858),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Alert Info Banner
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AlertInfoBanner(alertId: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = BrandGreenLight
            ),
            elevation = CardDefaults.cardElevation(0.5.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("\uD83D\uDCAC", fontSize = 16.sp)
                Column {
                    Text(
                        "Parking Alert #$alertId",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreen
                    )
                    Text(
                        "Respond to resolve this alert",
                        fontSize = 11.sp,
                        color = AccentGreen
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Chat Bubble
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isOwner = message.isOwner

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isOwner) 60.dp else 4.dp,
                end = if (isOwner) 4.dp else 60.dp,
                top = 2.dp,
                bottom = 2.dp
            ),
        horizontalArrangement = if (isOwner) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = if (isOwner) 12.dp else 4.dp,
                topEnd = if (isOwner) 4.dp else 12.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isOwner) OwnerBubble else VisitorBubble
            ),
            elevation = CardDefaults.cardElevation(0.5.dp)
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 10.dp,
                    end = 10.dp,
                    top = 6.dp,
                    bottom = 4.dp
                )
            ) {
                if (!isOwner) {
                    Text(
                        text = "Visitor",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreen
                    )
                    Spacer(Modifier.height(2.dp))
                }

                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    color = Color(0xFF252A23),
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = message.timestamp,
                        fontSize = 11.sp,
                        color = TimestampColor
                    )
                    if (isOwner) {
                        Icon(
                            imageVector = when (message.status) {
                                MessageStatus.SENDING   -> Icons.Default.Schedule
                                MessageStatus.SENT      -> Icons.Default.Done
                                MessageStatus.DELIVERED  -> Icons.Default.DoneAll
                                MessageStatus.ERROR      -> Icons.Default.ErrorOutline
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = when (message.status) {
                                MessageStatus.DELIVERED -> Color(0xFF85B070)
                                MessageStatus.ERROR     -> ErrorRed
                                else -> TimestampColor
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Typing Indicator
// ─────────────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 60.dp, top = 2.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 12.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            ),
            colors = CardDefaults.cardColors(containerColor = VisitorBubble),
            elevation = CardDefaults.cardElevation(0.5.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(TimestampColor.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}



@Composable
private fun QuickReplyChips(
    onChipSelected: (QuickReplyOption) -> Unit,
    onIgnore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Quick Replies",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TimestampColor,
            modifier = Modifier.padding(start = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.width(4.dp))

            QuickReplyOption.values().filter { it != QuickReplyOption.CUSTOM }.forEach { option ->
                val icon = when (option) {
                    QuickReplyOption.COMING_SOON -> Icons.Outlined.AccessTime          // "Coming in 2 minutes"
                    QuickReplyOption.THANKS      -> Icons.Outlined.ThumbUp             // "Thanks for Informing me"
                    QuickReplyOption.URGENT      -> Icons.Outlined.PriorityHigh        // "Is it Urgent?"
                    QuickReplyOption.SORRY       -> Icons.Outlined.SentimentDissatisfied // "Sorry for inconvenience"
                    else -> Icons.Outlined.Send
                }

                OutlinedButton(
                    onClick = { onChipSelected(option) },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ChipBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ChipBg
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        icon, null,
                        modifier = Modifier.size(16.dp),
                        tint = BrandGreen
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        option.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF252A23)
                    )
                }
            }

            // Ignore Alert chip
            OutlinedButton(
                onClick = onIgnore,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderColor),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ChipBg
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Outlined.Close, null,
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Ignore Alert",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.width(4.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Chat Input Bar
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean
) {
    Surface(
        color = ChatBg,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        "Type a message...",
                        color = TimestampColor,
                        fontSize = 15.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 120.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = BrandGreen
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { if (value.isNotBlank() && !isSending) onSend() }
                ),
                maxLines = 4,
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
            )

            FloatingActionButton(
                onClick = { if (value.isNotBlank() && !isSending) onSend() },
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                containerColor = if (value.isNotBlank()) SendBtnColor else SendBtnColor.copy(alpha = 0.5f),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp
                )
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send, "Send",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────

// Parses an ISO-8601 timestamp like "2026-04-10T05:59:02.000000Z" into ms-since-epoch.
// Returns null if the string is missing or unparsable so the caller can fall back to clock time.
@OptIn(ExperimentalTime::class)
private fun parseTimestampMs(timestamp: String?): Long? {
    if (timestamp.isNullOrBlank()) return null
    return try {
        Instant.parse(timestamp).toEpochMilliseconds()
    } catch (_: Exception) {
        // Fallback for "yyyy-MM-dd HH:mm:ss" without timezone
        try {
            val iso = timestamp.trim().replace(" ", "T")
            LocalDateTime.parse(iso).toInstant(TimeZone.UTC).toEpochMilliseconds()
        } catch (_: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun formatMsTime(ms: Long): String {
    return try {
        val ldt = Instant.fromEpochMilliseconds(ms)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = ldt.hour
        val minute = ldt.minute.toString().padStart(2, '0')
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "$displayHour:$minute $amPm"
    } catch (_: Exception) {
        "now"
    }
}