@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.StorageKeys
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.response.Post
import com.developer.ev2z.response.StatusSession
import com.developer.ev2z.response.durationLabel
import com.developer.ev2z.response.durationSeconds
import com.developer.ev2z.response.VaultDocumentDto
import com.developer.ev2z.ui.screens.SOSParkingTag.ChatThread
import com.developer.ev2z.ui.screens.SOSParkingTag.ChatThreadListUiState
import com.developer.ev2z.ui.screens.SOSParkingTag.SosTagViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import com.developer.ev2z.theme.AppPageBg

// ─── Badge manager (unchanged) ────────────────────────────────────────────────

object NotificationBadgeManager {
    val unreadCount = MutableStateFlow(0)
    private val seenThreadIds = mutableSetOf<Int>()

    fun markAllSeen(threadIds: List<Int>) {
        seenThreadIds.addAll(threadIds)
        unreadCount.value = 0
    }

    fun markOneSeen(chatId: Int) {
        seenThreadIds.add(chatId)
        val current = unreadCount.value
        if (current > 0) unreadCount.value = current - 1
    }

    fun recalculate(state: ChatThreadListUiState): Int {
        val count = (state as? ChatThreadListUiState.Success)
            ?.threads?.count { it.isOpen && it.chat_id !in seenThreadIds } ?: 0
        unreadCount.value = count
        return count
    }
}

// ─── Model ────────────────────────────────────────────────────────────────────

internal sealed class NotifItem {
    data class QrItem(val thread: ChatThread) : NotifItem()
    data class GpsItem(
        val title: String,
        val deviceName: String,
        val status: String,
        val startTime: String,
        /** Already formatted — see `StatusSession.durationLabel`. */
        val duration: String,
    ) : NotifItem()
    data class VaultItem(val doc: VaultDocumentDto) : NotifItem()
    data class CommunityItem(val post: Post) : NotifItem()
}

// ─── Colors ───────────────────────────────────────────────────────────────────

private object NC {
    val ScreenBg    = AppPageBg
    val CardBg      = Color(0xFFFFFFFF)
    val ChipBg      = Color(0xFFE2E8F0)
    val ChipActive  = Color(0xFF00A844)
    val Green       = Color(0xFF16A34A)
    val TextDark    = Color(0xFF0F172A)
    val TextGray    = Color(0xFF94A3B8)
    val SectionGray = Color(0xFF94A3B8)
    val BackBtnBg   = Color(0xFFE2E8F0)
    val Blue        = Color(0xFF2563EB)
    val BlueBg      = Color(0xFFDBEAFE)
    val Amber       = Color(0xFFD97706)
    val AmberBg     = Color(0xFFFEF3C7)
    val Red         = Color(0xFFDC2626)
    val RedBg       = Color(0xFFFEE2E2)
    val Purple      = Color(0xFF8B5CF6)
    val PurpleBg    = Color(0xFFEDE9FE)
    val Muted       = Color(0xFF94A3B8)
    val MutedBg     = Color(0xFFF8FAFC)
}

private val filters = listOf("All", "QR", "Vault", "GPS", "Community")

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun NotificationsScreen(
    viewModel: SosTagViewModel,
    onBackPressed: () -> Unit,
    onNotificationClick: (chatId: Int, visitorName: String, visitorPhone: String?) -> Unit,
    onCommunityClick: () -> Unit = {},
    apiService: ApiService? = null,
    token: String = "",
    deviceId: String? = null,
    deviceName: String = "My Vehicle",
    isGpsUser: Boolean = false,
    userPlan: String = "",
    storageManager: StorageManager? = null,
    initialFilter: String = "All",
) {
    PlatformBackHandler(enabled = true) { onBackPressed() }

    val threadListState by viewModel.threadListState.collectAsState()
    val showGpsEvents = (isGpsUser || userPlan == "both") && apiService != null && !deviceId.isNullOrBlank()

    var gpsEvents      by remember { mutableStateOf<List<StatusSession>>(emptyList()) }
    var vaultDocs      by remember { mutableStateOf<List<VaultDocumentDto>>(emptyList()) }
    var communityPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf(if (initialFilter in filters) initialFilter else "All") }

    LaunchedEffect(Unit) {
        viewModel.loadChatsWithMessages()

        // GPS sessions
        if (showGpsEvents && deviceId != null && apiService != null) {
            try {
                val resp = apiService.getDevicesStatus(token, deviceId)
                gpsEvents = resp.data?.sessions?.reversed()?.take(20) ?: emptyList()
            } catch (_: Exception) {}
        }

        // Vault documents (only if vault was previously unlocked and token is cached)
        if (storageManager != null && apiService != null) {
            val vaultToken = storageManager.getString(StorageKeys.VAULT_TOKEN, "")
            if (vaultToken.isNotEmpty()) {
                try {
                    vaultDocs = apiService.getVaultDocuments(vaultToken, token)
                } catch (_: Exception) {}
            }
        }

        // Community posts
        if (apiService != null) {
            try {
                communityPosts = apiService.getCommunityPosts().take(15)
            } catch (_: Exception) {}
        }

        // Periodic refresh for QR threads
        while (true) {
            delay(15_000)
            viewModel.refreshChatsWithMessages()
        }
    }

    LaunchedEffect(threadListState) {
        if (threadListState is ChatThreadListUiState.Success) {
            val ids = (threadListState as ChatThreadListUiState.Success).threads.map { it.chat_id }
            NotificationBadgeManager.markAllSeen(ids)
        }
    }

    val allItems: List<NotifItem> = remember(threadListState, gpsEvents, vaultDocs, communityPosts) {
        val qr = (threadListState as? ChatThreadListUiState.Success)
            ?.threads?.map { NotifItem.QrItem(it) } ?: emptyList()
        val gps = gpsEvents.map { s ->
            NotifItem.GpsItem(
                title = when (s.status) {
                    "moving"  -> "Vehicle Moving"
                    "idle"    -> "Engine Idle"
                    "parked"  -> "Vehicle Parked"
                    "offline" -> "Device Offline"
                    else      -> s.status.replaceFirstChar { it.uppercase() }
                },
                deviceName  = deviceName,
                status      = s.status,
                startTime   = s.start_time,
                duration    = if (s.durationSeconds() > 0) s.durationLabel() else "",
            )
        }
        val vault     = vaultDocs.map { NotifItem.VaultItem(it) }
        val community = communityPosts.map { NotifItem.CommunityItem(it) }
        gps + qr + community + vault
    }

    val filteredItems: List<NotifItem> = remember(allItems, selectedFilter) {
        when (selectedFilter) {
            "QR"        -> allItems.filterIsInstance<NotifItem.QrItem>()
            "GPS"       -> allItems.filterIsInstance<NotifItem.GpsItem>()
            "Vault"     -> allItems.filterIsInstance<NotifItem.VaultItem>()
            "Community" -> allItems.filterIsInstance<NotifItem.CommunityItem>()
            else        -> allItems
        }
    }

    val _ht      = LocalHomeTheme.current
    val bgColor  = if (_ht.isDark) _ht.palette.pageBg  else NC.ScreenBg
    val cardColor = if (_ht.isDark) _ht.palette.cardBg else NC.CardBg

    val isLoading = threadListState is ChatThreadListUiState.Loading
    val hasError  = threadListState is ChatThreadListUiState.Error && allItems.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        NotificationsHeader(onBackPressed = onBackPressed, isDark = _ht.isDark, cardBg = cardColor)
        FilterChipsRow(selected = selectedFilter, onSelect = { selectedFilter = it })

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading && allItems.isEmpty() -> NotificationsLoading()
                hasError -> NotificationsError(
                    message = (threadListState as ChatThreadListUiState.Error).message,
                    onRetry = { viewModel.loadChatsWithMessages() }
                )
                filteredItems.isEmpty() -> NotificationsEmpty()
                else -> NotificationsList(
                    items               = filteredItems,
                    onNotificationClick = onNotificationClick,
                    onCommunityClick    = onCommunityClick,
                    isDark              = _ht.isDark,
                    cardBg              = cardColor,
                )
            }
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun NotificationsHeader(onBackPressed: () -> Unit, isDark: Boolean, cardBg: Color) {
    val titleColor = if (isDark) LocalHomeTheme.current.palette.textPrimary else NC.TextDark
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(NC.BackBtnBg)
                .clickable { onBackPressed() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = NC.TextDark,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Notifications",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
        )
    }
}

// ─── Filter chips ─────────────────────────────────────────────────────────────

@Composable
private fun FilterChipsRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        filters.forEach { label ->
            val active = label == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) NC.ChipActive else NC.ChipBg)
                    .clickable { onSelect(label) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    color = if (active) Color.White else NC.TextDark,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

// ─── List ─────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationsList(
    items: List<NotifItem>,
    onNotificationClick: (chatId: Int, visitorName: String, visitorPhone: String?) -> Unit,
    onCommunityClick: () -> Unit,
    isDark: Boolean,
    cardBg: Color,
) {
    val gpsItems       = items.filterIsInstance<NotifItem.GpsItem>()
    val qrItems        = items.filterIsInstance<NotifItem.QrItem>()
    val communityItems = items.filterIsInstance<NotifItem.CommunityItem>()
    val vaultItems     = items.filterIsInstance<NotifItem.VaultItem>()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // GPS
        if (gpsItems.isNotEmpty()) {
            item(key = "hdr_gps") { SectionLabel("GPS ACTIVITY") }
            items(gpsItems, key = { "gps_${it.startTime}_${it.status}" }) { item ->
                GpsNotificationCard(item = item, isDark = isDark, cardBg = cardBg)
            }
        }

        // QR — grouped by day
        if (qrItems.isNotEmpty()) {
            val grouped = groupThreadsByDay(qrItems.map { it.thread })
            grouped.forEach { (label, dayThreads) ->
                item(key = "hdr_qr_$label") { SectionLabel(label.uppercase()) }
                items(dayThreads, key = { it.chat_id }) { thread ->
                    QrNotificationCard(
                        thread  = thread,
                        isDark  = isDark,
                        cardBg  = cardBg,
                        onClick = {
                            // Visitor identity is hidden — always pass the generic label, no phone.
                            onNotificationClick(
                                thread.chat_id,
                                "Visitor",
                                null,
                            )
                        },
                    )
                }
            }
        }

        // Community
        if (communityItems.isNotEmpty()) {
            item(key = "hdr_community") { SectionLabel("COMMUNITY") }
            items(communityItems, key = { "community_${it.post.id}" }) { item ->
                CommunityNotificationCard(
                    item    = item,
                    isDark  = isDark,
                    cardBg  = cardBg,
                    onClick = onCommunityClick,
                )
            }
        }

        // Vault
        if (vaultItems.isNotEmpty()) {
            item(key = "hdr_vault") { SectionLabel("VAULT DOCUMENTS") }
            items(vaultItems, key = { "vault_${it.doc.id}" }) { item ->
                VaultNotificationCard(item = item, isDark = isDark, cardBg = cardBg)
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ─── Section label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = NC.SectionGray,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 2.dp, top = 6.dp, bottom = 2.dp),
    )
}

// ─── QR notification card ─────────────────────────────────────────────────────

@Composable
private fun QrNotificationCard(
    thread: ChatThread,
    isDark: Boolean,
    cardBg: Color,
    onClick: () -> Unit,
) {
    val tPrimary = if (isDark) LocalHomeTheme.current.palette.textPrimary else NC.TextDark
    val tGray    = if (isDark) LocalHomeTheme.current.palette.textSecondary else NC.TextGray

    // Visitor identity (name + phone) is intentionally hidden.
    val lastMsg = thread.last_message?.text ?: thread.messages.lastOrNull()?.text
        ?: "Your vehicle QR was scanned."
    val timeStr = relativeTime(thread.last_message_at)
    val body = buildString {
        append(lastMsg)
        if (timeStr.isNotEmpty()) append(" · $timeStr")
    }

    NotificationCard(
        icon       = Icons.Default.QrCode2,
        iconTint   = NC.Blue,
        iconBg     = NC.BlueBg,
        title      = "QR Scanned · Visitor",
        body       = body,
        action     = if (thread.isOpen) "View Details" else null,
        cardBg     = cardBg,
        titleColor = tPrimary,
        bodyColor  = tGray,
        onClick    = onClick,
    )
}

// ─── GPS notification card ────────────────────────────────────────────────────

private val GpsStatusMeta = mapOf(
    "moving"  to Triple(Color(0xFF00A844), Color(0xFFDCFCE7), Icons.Default.DirectionsCar as ImageVector),
    "idle"    to Triple(Color(0xFFD97706), Color(0xFFFEF3C7), Icons.Default.EvStation     as ImageVector),
    "parked"  to Triple(Color(0xFF2563EB), Color(0xFFDBEAFE), Icons.Default.LocalParking  as ImageVector),
    "offline" to Triple(Color(0xFF94A3B8), Color(0xFFF8FAFC), Icons.Default.CloudOff      as ImageVector),
)

@Composable
private fun GpsNotificationCard(item: NotifItem.GpsItem, isDark: Boolean, cardBg: Color) {
    val tPrimary = if (isDark) LocalHomeTheme.current.palette.textPrimary else NC.TextDark
    val tGray    = if (isDark) LocalHomeTheme.current.palette.textSecondary else NC.TextGray

    val (dotColor, bgColor, icon) = GpsStatusMeta[item.status]
        ?: Triple(NC.Muted, NC.MutedBg, Icons.Default.DirectionsCar as ImageVector)
    val iconBg = if (isDark) dotColor.copy(alpha = 0.18f) else bgColor

    val bodyParts = mutableListOf(item.deviceName)
    item.duration.takeIf { it.isNotEmpty() }?.let { bodyParts.add(it) }
    relativeTime(item.startTime).takeIf { it.isNotEmpty() }?.let { bodyParts.add(it) }

    NotificationCard(
        icon       = icon,
        iconTint   = dotColor,
        iconBg     = iconBg,
        title      = item.title,
        body       = bodyParts.joinToString(" · "),
        action     = if (item.status == "moving") "View trip" else null,
        cardBg     = cardBg,
        titleColor = tPrimary,
        bodyColor  = tGray,
        onClick    = {},
    )
}

// ─── Community notification card ──────────────────────────────────────────────

@Composable
private fun CommunityNotificationCard(
    item: NotifItem.CommunityItem,
    isDark: Boolean,
    cardBg: Color,
    onClick: () -> Unit,
) {
    val tPrimary = if (isDark) LocalHomeTheme.current.palette.textPrimary else NC.TextDark
    val tGray    = if (isDark) LocalHomeTheme.current.palette.textSecondary else NC.TextGray
    val post = item.post

    val title = "${post.user?.name ?: "Community"} posted"
    val body  = buildString {
        post.title?.takeIf { it.isNotEmpty() }?.let { append(it) } ?: post.body?.let { append(it) }
        val timeStr = relativeTime(post.created_at)
        if (timeStr.isNotEmpty()) {
            if (isNotEmpty()) append(" · ")
            append(timeStr)
        }
    }.ifEmpty { "New post in community" }

    NotificationCard(
        icon       = Icons.Outlined.Groups,
        iconTint   = NC.Blue,
        iconBg     = NC.BlueBg,
        title      = title,
        body       = body,
        action     = "View Post",
        cardBg     = cardBg,
        titleColor = tPrimary,
        bodyColor  = tGray,
        onClick    = onClick,
    )
}

// ─── Vault notification card ──────────────────────────────────────────────────

@Composable
private fun VaultNotificationCard(item: NotifItem.VaultItem, isDark: Boolean, cardBg: Color) {
    val tPrimary = if (isDark) LocalHomeTheme.current.palette.textPrimary else NC.TextDark
    val tGray    = if (isDark) LocalHomeTheme.current.palette.textSecondary else NC.TextGray
    val doc = item.doc

    val typeLabel = doc.fileType.uppercase().take(4)
    val body      = "$typeLabel · Added ${relativeTime(doc.createdAt).ifEmpty { doc.createdAt.take(10) }}"

    NotificationCard(
        icon       = Icons.Outlined.Description,
        iconTint   = NC.Purple,
        iconBg     = NC.PurpleBg,
        title      = doc.fileName,
        body       = body,
        action     = "Open in Vault",
        cardBg     = cardBg,
        titleColor = tPrimary,
        bodyColor  = tGray,
        onClick    = {},
    )
}

// ─── Base card ────────────────────────────────────────────────────────────────

@Composable
private fun NotificationCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    body: String,
    action: String?,
    cardBg: Color,
    titleColor: Color,
    bodyColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .clickable { onClick() }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    color = bodyColor,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (action != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "$action →",
                    color = NC.Green,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onClick() },
                )
            }
        }
    }
}

// ─── Empty / Loading / Error ──────────────────────────────────────────────────

@Composable
private fun NotificationsLoading() {
    val tGray = if (LocalHomeTheme.current.isDark) LocalHomeTheme.current.palette.textSecondary else NC.TextGray
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NC.ChipActive, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
            Spacer(Modifier.height(14.dp))
            Text("Loading notifications…", fontSize = 13.sp, color = tGray)
        }
    }
}

@Composable
private fun NotificationsEmpty() {
    val _ht      = LocalHomeTheme.current
    val tPrimary = if (_ht.isDark) _ht.palette.textPrimary   else NC.TextDark
    val tGray    = if (_ht.isDark) _ht.palette.textSecondary else NC.TextGray
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(NC.ChipActive.copy(0.15f), NC.ChipActive.copy(0.04f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Notifications, null, tint = NC.ChipActive, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("No Notifications Yet", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = tPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "When someone scans your vehicle tag,\nyou'll see their message here.",
                fontSize = 12.sp,
                color = tGray,
                lineHeight = 17.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NotificationsError(message: String, onRetry: () -> Unit) {
    val _ht      = LocalHomeTheme.current
    val tPrimary = if (_ht.isDark) _ht.palette.textPrimary   else NC.TextDark
    val tGray    = if (_ht.isDark) _ht.palette.textSecondary else NC.TextGray
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CloudOff, null, tint = NC.Muted, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Couldn't load notifications", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = tPrimary)
            Spacer(Modifier.height(6.dp))
            Text(message, fontSize = 11.sp, color = tGray)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors  = ButtonDefaults.buttonColors(containerColor = NC.ChipActive),
                shape   = RoundedCornerShape(12.dp),
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
    val result    = LinkedHashMap<String, List<ChatThread>>()
    val today     = mutableListOf<ChatThread>()
    val yesterday = mutableListOf<ChatThread>()
    val earlier   = mutableListOf<ChatThread>()

    val nowMs    = kotlin.time.Clock.System.now().toEpochMilliseconds()
    val oneDayMs = 86_400_000L
    val twoDayMs = 172_800_000L

    threads.forEach { thread ->
        val ts = parseIsoTimestampMs(thread.last_message_at)
        val bucket = if (ts == null) earlier
        else when {
            nowMs - ts < oneDayMs -> today
            nowMs - ts < twoDayMs -> yesterday
            else                  -> earlier
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
    val ms   = parseIsoTimestampMs(timestamp) ?: return ""
    val now  = kotlin.time.Clock.System.now().toEpochMilliseconds()
    val diff = now - ms
    return when {
        diff < 60_000L      -> "Just now"
        diff < 3_600_000L   -> "${diff / 60_000}m ago"
        diff < 86_400_000L  -> "${diff / 3_600_000L}h ago"
        diff < 172_800_000L -> "Yesterday"
        else -> try {
            val ldt = Instant.fromEpochMilliseconds(ms)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
            "${months[ldt.monthNumber - 1]} ${ldt.dayOfMonth}"
        } catch (_: Exception) { "" }
    }
}

fun countUnreadNotifications(state: ChatThreadListUiState): Int =
    NotificationBadgeManager.recalculate(state)