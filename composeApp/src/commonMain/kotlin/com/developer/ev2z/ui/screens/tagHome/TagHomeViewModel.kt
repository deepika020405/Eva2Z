package com.developer.ev2z.ui.screens.tagHome

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ─── Shared Models (commonMain — used by Android & iOS) ───────────────────────

data class TagVehicleInfo(
    val name: String,
    val plate: String,
    val scanCount: Int,
    val isLive: Boolean
)

data class TagScanAlert(
    val location: String,
    val timeAgo: String
)

data class TagSafetyScore(
    val score: Int,
    val maxScore: Int,
    val hint: String
) {
    val percentage: Float get() = score.toFloat() / maxScore.toFloat()
}

enum class TagDocStatus { VALID, EXPIRED, EXPIRING, MISSING }

data class TagDocItem(
    val id: String,
    val emoji: String,
    val name: String,
    val subtitle: String,
    val status: TagDocStatus
)

data class TagFeatureItem(
    val id: String,
    val emoji: String,
    val name: String,
    val description: String,
    val colorHex: Long,
    val isLocked: Boolean = true
)

data class TagCommunityAlert(
    val id: String,
    val initial: String,
    val avatarColor: Long,
    val avatarTextColor: Long,
    val message: String,
    val timeAgo: String
)

data class TagHomeUiState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val vehicle: TagVehicleInfo? = null,
    val latestScanAlert: TagScanAlert? = null,
    val safetyScore: TagSafetyScore? = null,
    val documents: List<TagDocItem> = emptyList(),
    val features: List<TagFeatureItem> = emptyList(),
    val communityAlerts: List<TagCommunityAlert> = emptyList(),
    val error: String? = null
)

// ─── Shared ViewModel ─────────────────────────────────────────────────────────

class TagHomeViewModel(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(TagHomeUiState(isLoading = true))
    val uiState: StateFlow<TagHomeUiState> = _uiState.asStateFlow()

    init { loadInitialData() }

    fun loadInitialData(userName: String = "Rajan Sharma") {
        _uiState.value = TagHomeUiState(
            isLoading = false,
            userName = userName,
            vehicle = TagVehicleInfo(
                name = "Honda City \u2014 TAG Active",
                plate = "MH 02 BK 4421 \u00B7 3 scans this month",
                scanCount = 3,
                isLive = true
            ),
            latestScanAlert = TagScanAlert(
                location = "near Andheri Station, Mumbai",
                timeAgo = "2h ago"
            ),
            safetyScore = TagSafetyScore(
                score = 45,
                maxScore = 100,
                hint = "Add your Insurance doc to reach 65 and unlock more features"
            ),
            documents = listOf(
                TagDocItem("rc",  "\uD83D\uDCC4",       "RC Book",         "Expires 2031",   TagDocStatus.VALID),
                TagDocItem("ins", "\uD83D\uDEE1\uFE0F", "Insurance",       "Expired 12 Jan", TagDocStatus.EXPIRED),
                TagDocItem("puc", "\uD83D\uDCA8",       "PUC",             "33 days left",   TagDocStatus.EXPIRING),
                TagDocItem("dl",  "\uD83E\uDEAA",       "Driving Licence", "Tap to upload",  TagDocStatus.MISSING),
            ),
            features = listOf(
                TagFeatureItem("geo", "\uD83D\uDDFA\uFE0F", "Geo\nFencing",   "Set safe zones, get breach alerts instantly", 0xFF3B82F6),
                TagFeatureItem("eng", "\u26A1",             "Engine\nOn/Off", "Remotely cut or start your engine",           0xFFDC2626),
                TagFeatureItem("loc", "\uD83D\uDCCD",       "Live\nLocation", "Track your vehicle in real-time on map",      0xFF00C853),
                TagFeatureItem("spd", "\uD83D\uDEA8",       "Speed\nAlert",   "Alert when vehicle crosses speed limit",      0xFFF59E0B),
            ),
            communityAlerts = listOf(
                TagCommunityAlert("1", "R", 0xFFFEE2E2, 0xFFDC2626, "Bike stolen near Powai Lake \u2014 silver Activa, DL plate", "1h ago"),
                TagCommunityAlert("2", "A", 0xFFDCFCE7, 0xFF00A844, "Found keys near D-Mart Versova \u2014 contact if yours",     "3h ago"),
            )
        )
    }

    fun onGetGpsClicked() { /* hook navigation in caller */ }
    fun onViewVaultClicked() { /* hook navigation in caller */ }
    fun onDocumentTapped(docId: String) { /* hook navigation in caller */ }
}