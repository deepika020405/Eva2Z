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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import com.developer.ev2z.theme.AppPageBg


internal val BrandGreen      = Color(0xFF4A7D39)
internal val BrandGreenLight = Color(0xFFF1F4ED)
internal val BrandGreenMid   = Color(0xFFCCD7C7)
internal val AccentGreen     = Color(0xFF85B070)
internal val SoftGreen       = Color(0xFF9DD680)
internal val BackgroundColor = AppPageBg
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

private val ChatBg            = AppPageBg
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
    val localId: Long = 0L,                  // tie-breaker for owner replies sent in same ms
    val isCall: Boolean = false,
    val callStatus: String? = null           // "initiated"|"answered"|"missed"|"ended"
)

private enum class MessageStatus { SENDING, SENT, DELIVERED, READ, ERROR }

// ─────────────────────────────────────────────────────────────────
// Chat list items — date dividers + messages interleaved
// ─────────────────────────────────────────────────────────────────

private sealed class ChatListItem {
    data class DateDivider(val label: String) : ChatListItem()
    data class Msg(val chatMessage: ChatMessage) : ChatListItem()
}

// ─────────────────────────────────────────────────────────────────
// QuickReplyScreen — Direct Chat UI (opens from notification)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalTime::class)
@Composable
fun QuickReplyScreen(
    alertId: Int,
    visitorName: String = "Visitor",
    visitorPhone: String? = null,
    viewModel: SosTagViewModel,
    onConfirmed: () -> Unit,
    onBackPressed: () -> Unit,
    onInitiateCall: (chatId: Int, onStarted: () -> Unit, onFailed: (String) -> Unit) -> Unit = { _, _, _ -> }
) {

    PlatformBackHandler(enabled = true) { onBackPressed() }

    val replyState by viewModel.replyState.collectAsState()
    val isVisitorOnline by viewModel.isVisitorOnline.collectAsState()
    val focusManager = LocalFocusManager.current

    // ── Seed from the thread already fetched by my-chats-with-messages ──
    val preloadedThread by viewModel.selectedThread.collectAsState()

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }

    // Group messages by calendar day → insert a DateDivider before each new day
    val chatItems = remember(messages) {
        buildList {
            var lastLabel = ""
            messages.forEach { msg ->
                val label = formatDateHeader(msg.sortKey)
                if (label != lastLabel) {
                    add(ChatListItem.DateDivider(label))
                    lastLabel = label
                }
                add(ChatListItem.Msg(msg))
            }
        }
    }

    var inputText by remember { mutableStateOf("") }
    var hasReplied by remember { mutableStateOf(false) }
    // Tracks the localId of the optimistic bubble we added before server confirms.
    var pendingLocalId by remember { mutableStateOf<Long?>(null) }
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
        mutableStateOf(
            visitorName.takeIf { it.isNotBlank() && it != "Visitor" }
                ?: SosNotificationHandler.lastVisitorName
                ?: "Visitor"
        )
    }
    var latestVisitorPhone by remember {
        mutableStateOf(visitorPhone ?: SosNotificationHandler.lastVisitorPhone)
    }
    // Seed isChatClosed from thread.isOpen immediately (no API wait)
    var isChatClosed by remember {
        mutableStateOf(preloadedThread?.let { !it.isOpen } ?: false)
    }
    // isLoading=false immediately if we have pre-loaded messages; true only for cold-open from FCM
    var isLoading by remember {
        mutableStateOf(
            preloadedThread == null || preloadedThread!!.messages.isEmpty()
        )
    }
    var isInitiatingCall by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun statusFromDto(msg: ChatMessageDto, isOwner: Boolean): MessageStatus {
        if (!isOwner) return MessageStatus.SENT
        return when {
            (msg.is_seen ?: 0) == 1      -> MessageStatus.READ
            (msg.is_delivered ?: 0) == 1 -> MessageStatus.DELIVERED
            else                         -> MessageStatus.SENT
        }
    }

    // Apply a server message. For new ids → add bubble. For known ids → update status only.
    fun applyServerMessage(msg: ChatMessageDto) {
        val mid = msg.id ?: return
        val isCallType   = msg.type.equals("call",   ignoreCase = true)
        val isSystemType = msg.type.equals("system", ignoreCase = true)
        val isOwner      = msg.type.equals("sent",   ignoreCase = true)
        val newStatus    = statusFromDto(msg, isOwner)

        if (mid in seenServerMessageIds) {
            if (isOwner) {
                messages = messages.map { m ->
                    if (m.localId == mid.toLong()) {
                        when {
                            newStatus == MessageStatus.READ -> m.copy(status = MessageStatus.READ)
                            newStatus == MessageStatus.DELIVERED && m.status !in listOf(MessageStatus.READ, MessageStatus.DELIVERED) ->
                                m.copy(status = MessageStatus.DELIVERED)
                            else -> m
                        }
                    } else m
                }
            }
            return
        }

        seenServerMessageIds.add(mid)
        val sortMs  = parseTimestampMs(msg.timestamp) ?: Clock.System.now().toEpochMilliseconds()
        val display = msg.time ?: formatMsTime(sortMs)

        // System message (e.g. "Visitor chat ended") — render as centered info pill
        if (isSystemType) {
            val text = msg.message?.takeIf { it.isNotBlank() } ?: "Visitor chat ended"
            messages = (messages + ChatMessage(
                text            = text,
                isOwner         = false,
                timestamp       = display,
                sortKey         = sortMs,
                localId         = mid.toLong(),
                isSystemMessage = true
            )).sortedWith(compareBy({ it.sortKey }, { it.localId }))
            return
        }

        if (isCallType) {
            messages = (messages + ChatMessage(
                text       = callStatusLabel(msg.call_status),
                isOwner    = false,
                timestamp  = display,
                sortKey    = sortMs,
                localId    = mid.toLong(),
                isCall     = true,
                callStatus = msg.call_status
            )).sortedWith(compareBy({ it.sortKey }, { it.localId }))
            return
        }

        val text = msg.message?.takeIf { it.isNotBlank() } ?: return
        messages = (messages + ChatMessage(
            text      = text,
            isOwner   = isOwner,
            timestamp = display,
            sortKey   = sortMs,
            localId   = mid.toLong(),
            status    = newStatus
        )).sortedWith(compareBy({ it.sortKey }, { it.localId }))
    }


    fun applyChatThread(thread: ChatMessagesResponse) {
        if (thread.closed) isChatClosed = true
        thread.data.forEach { applyServerMessage(it) }
    }

    // ── Connect Socket.IO for real-time messages ──
    LaunchedEffect(activeAlertId) {
        if (activeAlertId > 0 && !isChatClosed) {
            viewModel.connectChatSocket(activeAlertId)
            // Mark all messages as seen — owner is now viewing the chat
            viewModel.markSeen(activeAlertId)

            kotlinx.coroutines.delay(3000)
            if (!isChatClosed) {
                viewModel.fetchChatMessages(activeAlertId)?.let { response ->
                    applyChatThread(response)
                }
            }
        }
    }

    // Emit user_offline and disconnect when the screen leaves composition (point 2)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.emitUserOffline()
            viewModel.disconnectChatSocket()
        }
    }

    // Collect real-time messages pushed by the server via socket
    LaunchedEffect(Unit) {
        viewModel.socketMessageFlow.collect { dto ->
            applyServerMessage(dto)
            // Fetch REST for any message that is NOT explicitly an owner echo ("sent").
            // System messages (visitor ended chat) are NOT treated as "visitor is online".
            val isIncoming = dto.type?.equals("sent", ignoreCase = true) != true
            val isSystem   = dto.type?.equals("system", ignoreCase = true) == true
            if (isIncoming && !isSystem && activeAlertId > 0) {
                // Visitor just sent a message — they are online right now.
                viewModel.setVisitorOnlineHint(true)
                viewModel.markSeen(activeAlertId)

                val chatId = activeAlertId
                coroutineScope.launch {
                    viewModel.fetchChatMessages(chatId)?.let { response ->
                        applyChatThread(response)
                    }
                }
            }
        }
    }

    // Collect real-time tick status updates (delivered / seen) from socket
    LaunchedEffect(Unit) {
        viewModel.socketStatusFlow.collect { (messageId, isDelivered, isSeen) ->
            // Socket delivery/seen = visitor received the message right now → they are online
            if (isDelivered || isSeen) viewModel.setVisitorOnlineHint(true)
            val newStatus = when {
                isSeen      -> MessageStatus.READ
                isDelivered -> MessageStatus.DELIVERED
                else        -> MessageStatus.SENT
            }
            messages = messages.map { m ->
                if (m.localId == messageId.toLong() && m.isOwner) m.copy(status = newStatus) else m
            }
        }
    }

    // When the socket reconnects after a brief drop, fetch REST history to catch any
    // messages the socket missed while it was disconnected.
    val isSocketConnected by viewModel.isSocketConnected.collectAsState()
    LaunchedEffect(isSocketConnected) {
        if (isSocketConnected && activeAlertId > 0 && !isChatClosed) {
            viewModel.fetchChatMessages(activeAlertId)?.let { applyChatThread(it) }
        }
    }

    // Propagate "chat_closed" socket event from ViewModel to local UI state
    val isChatClosedFromSocket by viewModel.isChatClosed.collectAsState()
    LaunchedEffect(isChatClosedFromSocket) {
        if (isChatClosedFromSocket) {
            isChatClosed = true
            snackbarHostState.showSnackbar(
                message  = "Visitor ended the chat",
                duration = SnackbarDuration.Short
            )
        }
    }

    // FCM fallback: when a new chat notification arrives for the currently open chat,
    // fetch REST messages immediately — handles the case where socket is disconnected.
    LaunchedEffect(Unit) {
        SosNotificationHandler.alertFlow.collect { incomingChatId ->
            if (incomingChatId == activeAlertId && activeAlertId > 0 && !isChatClosed) {
                viewModel.fetchChatMessages(activeAlertId)?.let { response ->
                    applyChatThread(response)
                }
                viewModel.markSeen(activeAlertId)
            }
        }
    }

    // Visitor came online → upgrade all SENT owner messages to DELIVERED (like WhatsApp double-tick)
    LaunchedEffect(isVisitorOnline) {
        if (isVisitorOnline) {
            messages = messages.map { m ->
                if (m.isOwner && m.status == MessageStatus.SENT) m.copy(status = MessageStatus.DELIVERED) else m
            }
        }
    }

    // ── Load full chat history on open (one-time) ──
    LaunchedEffect(Unit) {
        if (activeAlertId <= 0) {
            isLoading = false
            return@LaunchedEffect
        }

        // Pre-seed from my-chats-with-messages data immediately (no network wait)
        preloadedThread?.let { thread ->
            isChatClosed = !thread.isOpen
            thread.messages.forEach { item -> applyServerMessage(item.toDto()) }
            isLoading = false
        }

        // Fetch full history once — new messages arrive in real-time via Socket.IO
        viewModel.fetchChatMessages(activeAlertId)?.let { applyChatThread(it) }
        isLoading = false
        SosNotificationHandler.lastVisitorName?.let { latestVisitorName = it }
        SosNotificationHandler.lastVisitorPhone?.let { latestVisitorPhone = it }
    }


    LaunchedEffect(replyState) {
        when (replyState) {
            is SosReplyUiState.Success -> {
                hasReplied = true
                // Drop the optimistic bubble — server response is the source of truth.
                pendingLocalId?.let { tempId ->
                    messages = messages.filter { it.localId != tempId }
                }
                pendingLocalId = null
                val capturedChatId = activeAlertId
                val capturedOnline = isVisitorOnline
                coroutineScope.launch {
                    viewModel.fetchChatMessages(capturedChatId)?.let { applyChatThread(it) }

                    if (capturedOnline) {
                        messages = messages.map { m ->
                            if (m.isOwner && m.status == MessageStatus.SENT) m.copy(status = MessageStatus.DELIVERED) else m
                        }
                    }
                }
                viewModel.resetReplyState()
            }
            is SosReplyUiState.Error -> {
                // Mark optimistic bubble as ERROR so the user can see it failed.
                pendingLocalId?.let { tempId ->
                    messages = messages.map { m ->
                        if (m.localId == tempId) m.copy(status = MessageStatus.ERROR) else m
                    }
                }
                pendingLocalId = null
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
        if (text.isBlank() || activeAlertId <= 0 || isChatClosed) return
        if (replyState is SosReplyUiState.Loading) return  // prevent double-send
        val trimmed = text.trim()

        inputText = ""
        focusManager.clearFocus()

        // Optimistic bubble — show instantly like WhatsApp, SENDING status until server confirms.
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val tempId = -nowMs  // negative so it never collides with server IDs
        pendingLocalId = tempId
        messages = (messages + ChatMessage(
            text = trimmed,
            isOwner = true,
            timestamp = formatMsTime(nowMs),
            sortKey = nowMs,
            localId = tempId,
            status = MessageStatus.SENDING
        )).sortedWith(compareBy({ it.sortKey }, { it.localId }))

        viewModel.selectReply(QuickReplyOption.CUSTOM)
        viewModel.onCustomMessageChange(trimmed)
        viewModel.submitReply(activeAlertId)
    }

    Scaffold(
        containerColor = ChatBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (activeAlertId > 0) {
                ChatTopBar(
                    visitorName = latestVisitorName,
                    visitorPhone = latestVisitorPhone,
                    // hide "Close Chat" button if chat already closed
                    showClose = hasReplied && !isChatClosed,
                    isClosed = isChatClosed,
                    isVisitorOnline = isVisitorOnline,
                    onBackPressed = onBackPressed,
                    onCallPressed = {
                        if (!isInitiatingCall && activeAlertId > 0) {
                            isInitiatingCall = true
                            onInitiateCall(
                                activeAlertId,
                                { isInitiatingCall = false },
                                { errMsg ->
                                    isInitiatingCall = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(errMsg)
                                    }
                                }
                            )
                        }
                    },
                    isCallLoading = isInitiatingCall,
                    onClosePressed = {
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

            if (activeAlertId > 0 && !isChatClosed) {
                Column {

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
            // Empty state — only when there's no chat to show at all
            else if (activeAlertId <= 0) {
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
                        "No Chat Found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        "This conversation is unavailable.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
            // Chat messages — shown for both open AND closed chats
            else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Date-grouped chat bubbles — each new calendar day gets its own header
                    items(chatItems) { item ->
                        when (item) {
                            is ChatListItem.DateDivider -> DateHeader(text = item.label)
                            is ChatListItem.Msg -> when {
                                item.chatMessage.isCall         -> CallHistoryCard(item.chatMessage)
                                item.chatMessage.isSystemMessage -> SystemMessage(text = item.chatMessage.text)
                                else                            -> ChatBubble(message = item.chatMessage)
                            }
                        }
                    }

                    // "Chat closed" end-of-conversation pill
                    if (isChatClosed) {
                        item {
                            ChatClosedBanner()
                        }
                    }

                    // Typing indicator (only for open chats)
                    if (!isChatClosed && replyState is SosReplyUiState.Loading) {
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
    isClosed: Boolean = false,
    isVisitorOnline: Boolean = false,
    isCallLoading: Boolean = false,
    onBackPressed: () -> Unit,
    onCallPressed: () -> Unit,
    onClosePressed: () -> Unit
) {
    Surface(
        color = if (isClosed) Color(0xFF6B7280) else ChatTopBarColor,
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
                    .background(if (isClosed) Color(0xFF9CA3AF) else ChatTopBarLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "V",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name & status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Visitor",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isClosed) {
                    Text(
                        text = "Chat closed",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isVisitorOnline) Color(0xFF4CAF50)
                                    else Color.White.copy(alpha = 0.4f)
                                )
                        )
                        Text(
                            text = if (isVisitorOnline) "Online" else "Offline",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Call button — only for open chats
            if (!isClosed) {
                IconButton(
                    onClick = onCallPressed,
                    enabled = !isCallLoading
                ) {
                    if (isCallLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Call, "Call",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Close chat button
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

//@Composable
//private fun AlertInfoBanner(alertId: Int) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 24.dp, vertical = 4.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Card(
//            shape = RoundedCornerShape(10.dp),
//            colors = CardDefaults.cardColors(
//                containerColor = BrandGreenLight
//            ),
//            elevation = CardDefaults.cardElevation(0.5.dp)
//        ) {
//            Row(
//                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                Text("\uD83D\uDCAC", fontSize = 16.sp)
//                Column {
//                    Text(
//                        "Parking Alert",
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        color = BrandGreen
//                    )
//                    Text(
//                        "Respond to resolve this alert",
//                        fontSize = 11.sp,
//                        color = AccentGreen
//                    )
//                }
//            }
//        }
//    }
//}

// ─────────────────────────────────────────────────────────────────
// Chat Closed Banner — shown at the bottom of message list
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ChatClosedBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF3F4F6))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(14.dp)
            )
            Text(
                "This chat has been closed",
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                fontWeight = FontWeight.Medium
            )
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
                                MessageStatus.SENT      -> Icons.Default.Done       // 1 grey tick
                                MessageStatus.DELIVERED -> Icons.Default.DoneAll    // 2 grey ticks
                                MessageStatus.READ      -> Icons.Default.DoneAll    // 2 blue ticks
                                MessageStatus.ERROR     -> Icons.Default.ErrorOutline
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = when (message.status) {
                                MessageStatus.READ      -> Color(0xFF2196F3)  // blue
                                MessageStatus.DELIVERED -> TimestampColor     // grey
                                MessageStatus.ERROR     -> ErrorRed
                                else                    -> TimestampColor
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
// Call History Card — WhatsApp-style centered call log entry
// ─────────────────────────────────────────────────────────────────

@Composable
private fun CallHistoryCard(message: ChatMessage) {
    val isMissed = message.callStatus.equals("missed", ignoreCase = true)
    val iconTint = if (isMissed) Color(0xFFE53935) else Color(0xFF4A7D39)
    val icon = if (isMissed) Icons.Default.PhoneMissed else Icons.Default.Call
    val bgColor = if (isMissed) Color(0xFFFFF0F0) else Color(0xFFF1F8EC)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = bgColor,
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                0.8.dp,
                if (isMissed) Color(0xFFFFCDD2) else Color(0xFFC8E6C9)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = iconTint
                )
                Text(
                    text = "· ${message.timestamp}",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}

private fun callStatusLabel(status: String?): String = when (status?.lowercase()) {
    "initiated" -> " Call started"
    "answered"  -> " Call answered"
    "missed"    -> " Missed call"
    "ended"     -> " Call ended"
    else        -> " Call"
}

// ─────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────

// Returns "Today", "Yesterday", or "23 May 2026" for a given epoch-millisecond value.
@OptIn(ExperimentalTime::class)
private fun formatDateHeader(epochMs: Long): String {
    return try {
        val tz      = TimeZone.currentSystemDefault()
        val msgDate = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(tz).date
        val today   = Clock.System.now().toLocalDateTime(tz).date
        when {
            msgDate == today                              -> "Today"
            today.toEpochDays() - msgDate.toEpochDays() == 1L -> "Yesterday"
            else -> {
                val month = msgDate.month.name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }
                    .take(3)
                "${msgDate.dayOfMonth} $month ${msgDate.year}"
            }
        }
    } catch (_: Exception) {
        "Today"
    }
}

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