package com.developer.ev2z.ui.screens.SOSParkingTag


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SosTagViewModel(
    private val apiService: ApiService,
    private val userPrefs: UserPrefs
) : ViewModel() {

    // ── Alert Detail ──────────────────────────────────────────────

    private val _alertDetailState = MutableStateFlow<SosAlertUiState>(SosAlertUiState.Idle)
    val alertDetailState: StateFlow<SosAlertUiState> = _alertDetailState.asStateFlow()

    // ── Alerts List ───────────────────────────────────────────────

    private val _alertListState = MutableStateFlow<SosAlertListUiState>(SosAlertListUiState.Idle)
    val alertListState: StateFlow<SosAlertListUiState> = _alertListState.asStateFlow()

    // ── Quick Reply ───────────────────────────────────────────────

    private val _replyState = MutableStateFlow<SosReplyUiState>(SosReplyUiState.Idle)
    val replyState: StateFlow<SosReplyUiState> = _replyState.asStateFlow()

    // Selected quick reply option
    private val _selectedReply = MutableStateFlow<QuickReplyOption?>(null)
    val selectedReply: StateFlow<QuickReplyOption?> = _selectedReply.asStateFlow()

    // Custom message text (only used when CUSTOM is selected)
    private val _customMessage = MutableStateFlow("")
    val customMessage: StateFlow<String> = _customMessage.asStateFlow()

    // Current alert being viewed / replied to
    private val _currentAlert = MutableStateFlow<SosAlert?>(null)
    val currentAlert: StateFlow<SosAlert?> = _currentAlert.asStateFlow()

    // ── Load Alert Detail ─────────────────────────────────────────

    fun loadAlertDetail(alertId: Int) {
        if (alertId <= 0) {
            _alertDetailState.value = SosAlertUiState.Error("Invalid alert id")
            return
        }

        viewModelScope.launch {
            _alertDetailState.value = SosAlertUiState.Loading

            // ✅ Retry 5 baar, 3 second gap se
            repeat(5) { attempt ->
                try {
                    val response = apiService.getSosAlertDetail(alertId)

                    if (response.status && response.data != null) {
                        // ✅ Data mila — stop retry
                        _currentAlert.value = response.data
                        _alertDetailState.value = SosAlertUiState.Success(response.data)
                        return@launch  // ✅ EXIT
                    } else {
                        println("⏳ Attempt ${attempt + 1}: data null, retrying...")
                        // ✅ Data nahi mila — wait and retry
                        if (attempt < 4) {
                            kotlinx.coroutines.delay(3000)  // 3 second wait
                        }
                    }

                } catch (e: Exception) {
                    println("❌ Attempt ${attempt + 1} failed: ${e.message}")
                    if (attempt < 4) {
                        kotlinx.coroutines.delay(3000)
                    }
                }
            }

            // ✅ 5 attempts ke baad bhi data nahi mila
            _alertDetailState.value = SosAlertUiState.Error(
                "Alert data not available. Please try again."
            )
        }
    }

    // ── Load All Alerts ───────────────────────────────────────────

    fun loadAlertList() {
        viewModelScope.launch {
            _alertListState.value = SosAlertListUiState.Loading
            try {
                val response = apiService.getSosAlerts()
                if (response.status) {
                    _alertListState.value = SosAlertListUiState.Success(
                        response.data ?: emptyList()
                    )
                } else {
                    _alertListState.value = SosAlertListUiState.Error(
                        response.message ?: "Failed to load alerts."
                    )
                }
            } catch (e: Exception) {
                _alertListState.value = SosAlertListUiState.Error(
                    "Network error: ${e.message ?: "Unknown"}"
                )
            }
        }
    }

    // ── Select Quick Reply Option ─────────────────────────────────

    fun selectReply(option: QuickReplyOption) {
        _selectedReply.value = option
        if (option != QuickReplyOption.CUSTOM) {
            _customMessage.value = ""
        }
        // Clear any previous error
        if (_replyState.value is SosReplyUiState.Error) {
            _replyState.value = SosReplyUiState.Idle
        }
    }

    fun onCustomMessageChange(value: String) {
        _customMessage.value = value
    }

    // ── Submit Reply ──────────────────────────────────────────────

    fun submitReply(alertId: Int) {
        val option = _selectedReply.value ?: return
        if (_replyState.value is SosReplyUiState.Loading) return

        // Validate custom message if needed
        if (option == QuickReplyOption.CUSTOM && _customMessage.value.isBlank()) return

        viewModelScope.launch {
            _replyState.value = SosReplyUiState.Loading
            try {
                val message = when(option) {
                    QuickReplyOption.COMING_SOON -> "Coming in 2 minutes"
                    QuickReplyOption.MOVING_NOW -> "Moving vehicle now"
                    QuickReplyOption.SORRY -> "Sorry for inconvenience"
                    QuickReplyOption.CUSTOM -> _customMessage.value.trim()
                }

                val response = apiService.sendOwnerReply(
                    OwnerReplyRequest(
                        notification_id = alertId,
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

    // ── Helpers ───────────────────────────────────────────────────

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

    fun resetAll() {
        resetReplyState()
        _alertDetailState.value = SosAlertUiState.Idle
        _alertListState.value   = SosAlertListUiState.Idle  // ✅ yeh missing tha
        _currentAlert.value     = null
    }
}