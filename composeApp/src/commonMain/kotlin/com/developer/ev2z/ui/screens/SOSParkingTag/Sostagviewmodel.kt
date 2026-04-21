package com.developer.ev2z.ui.screens.SOSParkingTag


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.network.ApiService
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

        viewModelScope.launch {
            _replyState.value = SosReplyUiState.Loading
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
        resetReplyState()
        _alertDetailState.value  = SosAlertUiState.Idle
        _alertListState.value    = SosAlertListUiState.Idle
        _threadListState.value   = ChatThreadListUiState.Idle
        _currentAlert.value      = null
        _selectedThread.value    = null
    }
}