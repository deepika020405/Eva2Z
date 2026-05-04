package com.developer.ev2z.ui.screens.SOSParkingTag


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.chat.ChatSocketListener
import com.developer.ev2z.chat.ChatSocketManager
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.network.KtorClient
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SosTagViewModel(
    private val apiService: ApiService,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _alertDetailState = MutableStateFlow<SosAlertUiState>(SosAlertUiState.Idle)
    val alertDetailState: StateFlow<SosAlertUiState> = _alertDetailState.asStateFlow()

    private val _alertListState = MutableStateFlow<SosAlertListUiState>(SosAlertListUiState.Idle)
    val alertListState: StateFlow<SosAlertListUiState> = _alertListState.asStateFlow()

    private val _threadListState = MutableStateFlow<ChatThreadListUiState>(ChatThreadListUiState.Idle)
    val threadListState: StateFlow<ChatThreadListUiState> = _threadListState.asStateFlow()

    private val _replyState = MutableStateFlow<SosReplyUiState>(SosReplyUiState.Idle)
    val replyState: StateFlow<SosReplyUiState> = _replyState.asStateFlow()

    // Thread selected from ChatInboxScreen — used to pre-seed QuickReplyScreen instantly
    private val _selectedThread = MutableStateFlow<ChatThread?>(null)
    val selectedThread: StateFlow<ChatThread?> = _selectedThread.asStateFlow()

    fun selectThread(thread: ChatThread) {
        _selectedThread.value = thread
    }

    fun clearSelectedThread() {
        _selectedThread.value = null
    }

    private val _selectedReply = MutableStateFlow<QuickReplyOption?>(null)
    val selectedReply: StateFlow<QuickReplyOption?> = _selectedReply.asStateFlow()

    private val _customMessage = MutableStateFlow("")
    val customMessage: StateFlow<String> = _customMessage.asStateFlow()

    private val _currentAlert = MutableStateFlow<SosAlert?>(null)
    val currentAlert: StateFlow<SosAlert?> = _currentAlert.asStateFlow()

    private var loadDetailJob: Job? = null
    private var loadListJob: Job? = null

    // ── Socket real-time chat ─────────────────────────────────────────
    // Messages pushed by the server via Socket.IO are emitted here so
    // QuickReplyScreen can apply them instantly without waiting for a poll.
    private val _socketMessageFlow = MutableSharedFlow<ChatMessageDto>(extraBufferCapacity = 32)
    val socketMessageFlow: SharedFlow<ChatMessageDto> = _socketMessageFlow.asSharedFlow()

    // Triple(messageId, isDelivered, isSeen) — real-time tick updates from socket
    private val _socketStatusFlow = MutableSharedFlow<Triple<Int, Boolean, Boolean>>(extraBufferCapacity = 32)
    val socketStatusFlow: SharedFlow<Triple<Int, Boolean, Boolean>> = _socketStatusFlow.asSharedFlow()

    private val _isSocketConnected = MutableStateFlow(false)
    val isSocketConnected: StateFlow<Boolean> = _isSocketConnected.asStateFlow()

    private val _isVisitorOnline = MutableStateFlow(false)
    val isVisitorOnline: StateFlow<Boolean> = _isVisitorOnline.asStateFlow()

    // chatId → visitor is currently connected via socket.
    // Updated by onVisitorOnline/Offline callbacks and cleared on disconnect.
    // Used by ChatInboxScreen to show the green presence dot accurately.
    private val _onlineChats = MutableStateFlow<Set<Int>>(emptySet())
    val onlineChats: StateFlow<Set<Int>> = _onlineChats.asStateFlow()

    private var chatSocketManager: ChatSocketManager? = null
    private var activeChatId: Int = 0
    private var activeUserId: Int = 0

    fun connectChatSocket(chatId: Int) {
        if (chatId <= 0) return
        val token = userPrefs.getToken() ?: return
        val userId = userPrefs.getUserId()

        // Gracefully leave previous chat before switching
        emitUserOffline()
        disconnectChatSocket()

        activeChatId = chatId
        activeUserId = userId

        chatSocketManager = ChatSocketManager(KtorClient.SOCKET_SERVER_URL)
        chatSocketManager?.connect(
            chatId = chatId,
            userId = userId,
            token = token,
            listener = object : ChatSocketListener {
                override fun onNewMessage(msg: ChatMessageDto) {
                    viewModelScope.launch {
                        _socketMessageFlow.emit(msg)
                        // Keep inbox thread list current so ChatInboxScreen shows live last-message.
                        // Run inside launch so the read-modify-write on _threadListState happens
                        // on the main thread and is not racing with other state updates.
                        updateThreadInbox(chatId, msg)
                    }
                    if (msg.type.equals("received", ignoreCase = true)) {
                        _isVisitorOnline.value = true
                    }
                }
                override fun onMessageStatusUpdate(messageId: Int, isDelivered: Boolean, isSeen: Boolean) {
                    viewModelScope.launch { _socketStatusFlow.emit(Triple(messageId, isDelivered, isSeen)) }
                }
                override fun onChatClosed() {
                    println("🔒 [SosTagVM] Server signalled chat_closed for chatId=$chatId")
                }
                override fun onVisitorOnline() {
                    _isVisitorOnline.value = true
                    _onlineChats.value = _onlineChats.value + chatId
                }
                override fun onVisitorOffline() {
                    _isVisitorOnline.value = false
                    _onlineChats.value = _onlineChats.value - chatId
                }
                override fun onConnected() {
                    _isSocketConnected.value = true
                    println("✅ [SosTagVM] Chat socket connected — chatId=$chatId userId=$userId")
                }
                override fun onDisconnected() {
                    _isSocketConnected.value = false
                }
            }
        )
    }

    // Called by the screen when a REST fetch reveals is_delivered=1 on a message that
    // was previously SENT — the visitor's socket received it, so they're reachable.
    fun setVisitorOnlineHint(online: Boolean) {
        if (online) {
            _isVisitorOnline.value = true
            if (activeChatId > 0) _onlineChats.value = _onlineChats.value + activeChatId
        }
    }

    // Emit user_offline for the currently active chat — call before disconnect or screen close.
    fun emitUserOffline() {
        if (activeChatId > 0 && activeUserId > 0) {
            chatSocketManager?.emitUserOffline(activeChatId, activeUserId)
        }
    }

    fun disconnectChatSocket() {
        if (activeChatId > 0) _onlineChats.value = _onlineChats.value - activeChatId
        chatSocketManager?.disconnect()
        chatSocketManager = null
        _isSocketConnected.value = false
        _isVisitorOnline.value = false
    }

    suspend fun markSeen(chatId: Int) {
        if (chatId <= 0) return
        println("🔵 [SosTagVM] markSeen triggered → chatId=$chatId")
        try { apiService.markSeen(chatId) } catch (_: Exception) {}
        chatSocketManager?.emitMessagesSeen(chatId)
    }

    suspend fun markDelivered(chatId: Int) {
        if (chatId <= 0) return
        try { apiService.markDelivered(chatId) } catch (_: Exception) {}
    }
    // ─────────────────────────────────────────────────────────────────

    // chatId → lastSeenMessageId (0 = never opened)
    private val readStateMap = mutableMapOf<Int, Int>()

    fun markChatRead(chatId: Int, lastMessageId: Int?) {
        readStateMap[chatId] = lastMessageId ?: 0
    }

    fun getUnreadCount(thread: ChatThread): Int {
        if (!thread.isOpen) return 0
        val lastMsgId = thread.last_message?.id ?: return 0
        val lastSeenId = readStateMap[thread.chat_id]
        return when {
            lastSeenId == null ->
                if (thread.last_message?.type.equals("received", ignoreCase = true)) thread.total_messages else 0
            lastMsgId > lastSeenId -> thread.total_messages
            else -> 0
        }
    }

    fun loadAlertDetail(alertId: Int) {
        if (alertId <= 0) {
            _alertDetailState.value = SosAlertUiState.Error("Invalid alert id")
            return
        }

        loadDetailJob?.cancel()
        loadDetailJob = viewModelScope.launch {
            _alertDetailState.value = SosAlertUiState.Loading

            repeat(5) { attempt ->
                try {
                    val response = apiService.getSosAlertDetail(alertId)

                    if (response.status && response.data != null) {
                        _currentAlert.value = response.data
                        _alertDetailState.value = SosAlertUiState.Success(response.data)
                        return@launch
                    } else {
                        println("⏳ Attempt ${attempt + 1}: data null, retrying...")
                        if (attempt < 4) {
                            kotlinx.coroutines.delay(3000)
                        }
                    }

                } catch (e: Exception) {
                    println("❌ Attempt ${attempt + 1} failed: ${e.message}")
                    if (attempt < 4) {
                        kotlinx.coroutines.delay(3000)
                    }
                }
            }

            _alertDetailState.value = SosAlertUiState.Error(
                "Alert data not available. Please try again."
            )
        }
    }

    suspend fun fetchAlertDetail(alertId: Int): SosAlert? {
        repeat(5) { attempt ->
            try {
                val response = apiService.getSosAlertDetail(alertId)
                if (response.status && response.data != null) {
                    return response.data
                } else {
                    println("⏳ fetchAlertDetail attempt ${attempt + 1}: data null")
                    if (attempt < 4) kotlinx.coroutines.delay(3000)
                }
            } catch (e: Exception) {
                println("❌ fetchAlertDetail attempt ${attempt + 1}: ${e.message}")
                if (attempt < 4) kotlinx.coroutines.delay(3000)
            }
        }
        return null
    }

    suspend fun fetchAllAlerts(): List<SosAlert>? {
        return try {
            val response = apiService.getSosAlerts()
            if (response.status) response.data else null
        } catch (e: Exception) {
            println("❌ fetchAllAlerts: ${e.message}")
            null
        }
    }

    suspend fun fetchChatMessages(chatId: Int): ChatMessagesResponse? {
        if (chatId <= 0) return null
        return try {
            val response = apiService.getChatMessages(chatId)
            if (response.status) response else null
        } catch (e: Exception) {
            println("❌ fetchChatMessages: ${e.message}")
            null
        }
    }

    // Update the last_message of one thread in the inbox without a full reload.
    // Called every time the socket delivers a new message so ChatInboxScreen stays live.
    private fun updateThreadInbox(chatId: Int, msg: ChatMessageDto) {
        val current = _threadListState.value as? ChatThreadListUiState.Success ?: return
        val newLastMsg = ChatMessageItem(
            id           = msg.id,
            text         = msg.message,
            sender_id    = msg.sender_id,
            type         = msg.type,
            time         = msg.time,
            timestamp    = msg.timestamp,
            is_delivered = msg.is_delivered,
            is_seen      = msg.is_seen
        )
        val updatedThreads = current.threads.map { thread ->
            if (thread.chat_id == chatId) {
                thread.copy(
                    last_message     = newLastMsg,
                    total_messages   = thread.total_messages + 1,
                    last_message_at  = msg.timestamp ?: thread.last_message_at
                )
            } else thread
        }
        _threadListState.value = ChatThreadListUiState.Success(updatedThreads)
    }

    // Silent refresh — updates inbox data without flipping to Loading state (no UI flicker).
    // Used for periodic background polling on ChatInboxScreen.
    fun refreshChatsWithMessages() {
        viewModelScope.launch {
            try {
                val response = apiService.getMyChatsWithMessages()
                if (response.status) {
                    _threadListState.value = ChatThreadListUiState.Success(response.data)
                }
            } catch (_: Exception) {}
        }
    }

    fun loadChatsWithMessages() {
        loadListJob?.cancel()
        loadListJob = viewModelScope.launch {
            _threadListState.value = ChatThreadListUiState.Loading
            try {
                val response = apiService.getMyChatsWithMessages()
                if (response.status) {
                    _threadListState.value = ChatThreadListUiState.Success(response.data)
                } else {
                    _threadListState.value = ChatThreadListUiState.Success(emptyList())
                }
            } catch (e: Exception) {
                _threadListState.value = ChatThreadListUiState.Error(
                    e.message ?: "Network error"
                )
            }
        }
    }

    fun loadAlertList() {
        loadListJob?.cancel()
        loadListJob = viewModelScope.launch {
            _alertListState.value = SosAlertListUiState.Loading
            try {
                val response = apiService.getSosAlerts()
                if (response.status) {
                    _alertListState.value = SosAlertListUiState.Success(
                        response.data ?: emptyList()
                    )
                } else {
                    _alertListState.value = SosAlertListUiState.Success(emptyList())
                }
            } catch (e: ResponseException) {
                if (e.response.status.value == 404) {
                    _alertListState.value = SosAlertListUiState.Success(emptyList())
                } else {
                    _alertListState.value = SosAlertListUiState.Error(
                        "Server error: ${e.response.status.value}"
                    )
                }
            } catch (e: Exception) {
                _alertListState.value = SosAlertListUiState.Error(
                    "Network error: ${e.message ?: "Unknown"}"
                )
            }
        }
    }

    fun selectReply(option: QuickReplyOption) {
        _selectedReply.value = option
        if (option != QuickReplyOption.CUSTOM) {
            _customMessage.value = ""
        }
        if (_replyState.value is SosReplyUiState.Error) {
            _replyState.value = SosReplyUiState.Idle
        }
    }

    fun onCustomMessageChange(value: String) {
        _customMessage.value = value
    }

    fun submitReply(alertId: Int) {
        val option = _selectedReply.value ?: return
        if (_replyState.value is SosReplyUiState.Loading) return

        if (option == QuickReplyOption.CUSTOM && _customMessage.value.isBlank()) return

        _replyState.value = SosReplyUiState.Loading  // set before launch to block rapid re-taps
        viewModelScope.launch {
            try {
                val message = when(option) {
                    QuickReplyOption.COMING_SOON -> "Coming in 2 minutes"
                    QuickReplyOption.THANKS -> "Thanks for Informing me"
                    QuickReplyOption.URGENT -> "Is it Urgent?"
                    QuickReplyOption.SORRY -> "Sorry for inconvenience"
                    QuickReplyOption.CUSTOM -> _customMessage.value.trim()
                }

                val response = apiService.sendOwnerReply(
                    OwnerReplyRequest(
                        chat_id = alertId,
                        message = message
                    )
                )

                if (response.status) {
                    _replyState.value = SosReplyUiState.Success(
                        response.message ?: "Response sent successfully!"
                    )
                    // Emit via socket so Node.js server forwards to visitor's connection.
                    // Backend must handle "send_message" event and emit "new_message" to room.
                    chatSocketManager?.emitOwnerMessage(alertId, message)
                } else {
                    _replyState.value = SosReplyUiState.Error(
                        response.message ?: "Failed to send response."
                    )
                }
            } catch (e: Exception) {
                _replyState.value = SosReplyUiState.Error(
                    "Network error: ${e.message ?: "Unknown"}"
                )
            }
        }
    }

    val isReplyValid: Boolean
        get() {
            val option = _selectedReply.value ?: return false
            return if (option == QuickReplyOption.CUSTOM) _customMessage.value.isNotBlank()
            else true
        }

    fun resetReplyState() {
        _replyState.value    = SosReplyUiState.Idle
        _selectedReply.value = null
        _customMessage.value = ""
    }

    fun closeChat(chatId: Int, onDone: () -> Unit = {}) {
        if (chatId <= 0) {
            onDone()
            return
        }
        viewModelScope.launch {
            try {
                val response = apiService.closeChat(chatId)
                println("✅ closeChat #$chatId result: status=${response.status} msg=${response.message}")
                if (response.status) {
                    // Emit before disconnect so Node.js can push chat_closed to visitor's socket.
                    chatSocketManager?.emitCloseChat(chatId)
                }
            } catch (e: Exception) {
                println("❌ closeChat #$chatId error: ${e.message}")
            } finally {
                onDone()
            }
        }
    }

    fun resetAll() {
        loadDetailJob?.cancel()
        loadListJob?.cancel()
        emitUserOffline()
        disconnectChatSocket()
        resetReplyState()
        _alertDetailState.value  = SosAlertUiState.Idle
        _alertListState.value    = SosAlertListUiState.Idle
        _threadListState.value   = ChatThreadListUiState.Idle
        _currentAlert.value      = null
        _selectedThread.value    = null
    }
}