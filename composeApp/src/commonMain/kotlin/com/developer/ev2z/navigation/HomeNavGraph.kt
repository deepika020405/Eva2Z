package com.developer.ev2z.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.launch
import cafe.adriel.voyager.navigator.tab.*
import com.developer.ev2z.LocalPlatformActivity
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.cancelCallNotification
import com.developer.ev2z.stopCallSound
import com.developer.ev2z.getAppContext
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.repository.ProductRepository
import com.developer.ev2z.ui.screens.EditProfile.EditProfileScreen
import com.developer.ev2z.ui.screens.GeoFencing.GeoFencingScreenWrapper
import com.developer.ev2z.ui.screens.HistoryPlayback.HistoryPlaybackScreen
import com.developer.ev2z.ui.screens.Profile.ProfileScreen
import com.developer.ev2z.ui.screens.homeScreen.HomeScreen
import com.developer.ev2z.ui.screens.homeScreen.HomeViewModel
import com.developer.ev2z.ui.screens.homeScreen.NotificationBadgeManager
import com.developer.ev2z.ui.screens.homeScreen.NotificationsScreen
import com.developer.ev2z.ui.screens.homeScreen.countUnreadNotifications
import com.developer.ev2z.ui.screens.ManageDevices.ManageDeviceScreen
import com.developer.ev2z.ui.screens.ManageDevices.EditDeviceNameScreen
import com.developer.ev2z.ui.screens.market_screen.ShopScreenHost
import com.developer.ev2z.ui.screens.PrivacyPolicy_TandC.PrivacyPolicy_TandCScreen
import com.developer.ev2z.ui.screens.PrivacyPolicy_TandC.WebViewType
import com.developer.ev2z.ui.screens.market_screen.ShopViewModel
import com.developer.ev2z.ui.screens.GeneralSettings.GeneralSettingsScreen
import com.developer.ev2z.ui.screens.AccountActivity.AccountActivityScreen
import com.developer.ev2z.ui.screens.AddDevice.AddDeviceDetailsCommon
import com.developer.ev2z.ui.screens.AddDevice.AddDeviceViewModel
import com.developer.ev2z.ui.screens.BillDetails.BillDetailsScreen
import com.developer.ev2z.ui.screens.BillDetails.BillDetailsViewModel
import com.developer.ev2z.ui.screens.ChangePassword.ChangePasswordScreen
import com.developer.ev2z.ui.screens.ChangePlan.ChangePlanScreenHost
//import com.developer.ev2z.ui.screens.ChangePlan.ChangePlanRoute
//import com.developer.ev2z.ui.screens.ChangePlan.ChangePlanScreen
import com.developer.ev2z.ui.screens.CriminalVerification.CriminalVerificationFlow
//import com.developer.ev2z.ui.screens.CriminalVerification.CriminalVerificationScreen
import com.developer.ev2z.ui.screens.DriverScore.DriverScoreFlowScreen
import com.developer.ev2z.ui.screens.GPSData.GPSDataScreen
import com.developer.ev2z.ui.screens.MyAccount.MyAccountScreen
//import com.developer.ev2z.ui.screens.GPSData.GPSDataScreen
import com.developer.ev2z.ui.screens.Profile.AboutAppScreen
import com.developer.ev2z.ui.screens.Profile.ProfileViewModel
import com.developer.ev2z.ui.screens.Profile.VaultPasswordDialog
import com.developer.ev2z.ui.screens.ReferAndEarn.ReferAndEarnScreen
import com.developer.ev2z.ui.screens.SupportAndWarrenty.WarrantyRoute
import com.developer.ev2z.ui.screens.VehicleParkingTag.ParkingScreen
import com.developer.ev2z.ui.screens.VehicleParkingTag.ParkingViewModel
import com.developer.ev2z.ui.screens.community.CommunityScreenRoot
import com.developer.ev2z.ui.screens.community.CreatePostScreen
import com.developer.ev2z.ui.screens.community.CreateStoryScreen
import com.developer.ev2z.ui.screens.community.StoryViewerScreen
import com.developer.ev2z.ui.screens.documents.DocumentsScreen
import com.developer.ev2z.ui.screens.documents.DocumentViewerScreen
import com.developer.ev2z.ui.screens.documents.UploadDocumentScreen
import com.developer.ev2z.ui.screens.homeScreen.HomeUiState
import com.developer.ev2z.ui.screens.homeScreen.LiveLocationScreen
import com.developer.ev2z.util.BottomBarController
import com.developer.ev2z.util.DeepLinkBus
import com.developer.ev2z.util.platformDownloadFile


// ✅ SOS Tag imports

import com.developer.ev2z.ui.screens.SOSParkingTag.CallScreen
import com.developer.ev2z.ui.screens.SOSParkingTag.ChatInboxScreen
import com.developer.ev2z.ui.screens.SOSParkingTag.ConfirmationScreen
import com.developer.ev2z.ui.screens.SOSParkingTag.IncomingCallScreen
import com.developer.ev2z.ui.screens.SOSParkingTag.QuickReplyScreen
import com.developer.ev2z.ui.screens.SOSParkingTag.SosTagViewModel
import com.developer.ev2z.calling.VoiceCallManager
import com.developer.ev2z.calling.startIncomingCallWatcher
import com.developer.ev2z.calling.stopIncomingCallWatcher
import com.developer.ev2z.util.IncomingCallData
import com.developer.ev2z.ui.screens.InstallationSupport.InstallationSupportScreen
import com.developer.ev2z.ui.screens.market_screen.ShopProduct
import com.developer.ev2z.util.SosNotificationHandler


// ------------------ TABS -------------------

class HomeTab(
    private val viewModel: HomeViewModel,
    private val token: String,
    private val onNavigateToAddDevice: () -> Unit,
    private val onNavigateToManageDevice: () -> Unit,
    private val onNavigateToGPSData: () -> Unit,
    private val onNavigateToGeoFencing: () -> Unit,
    private val onNavigateToHistory: () -> Unit,
    private val onNavigateToRefer: () -> Unit,
    private val onOpenCommunity: () -> Unit,
    private val onOpenDeviceStatus: () -> Unit,
    private val onNavigateToDeviceReport: () -> Unit,
    private val onOpenTrackTrace: () -> Unit,
    private val onNavigateToCriminalVerification: () -> Unit,
    private val onDocumentsTabSelected: () -> Unit,
    private val onNavigateToDriverScore: () -> Unit,
    private val onNavigateToParking: () -> Unit,
    private val onNavigateToSosTag: (Int) -> Unit,
    private val onSosTagManual: () -> Unit,
    private val onSosChatInbox: () -> Unit,
    private val onNavigateToInstallationSupport: () -> Unit,
    private val onLogout: () -> Unit = {},
) : Tab {

    @Composable
    override fun Content() {

        var showLiveLocation by rememberSaveable { mutableStateOf(false) }
        val unreadNotifCount by NotificationBadgeManager.unreadCount.collectAsState()

        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.loadDevices(token)
        }

        val userName = remember(uiState) {
            val rawName = (uiState as? HomeUiState.Success)
                ?.deviceResponse
                ?.name

            rawName
                ?.substringBefore("@")
                ?.replaceFirstChar { it.uppercase() }
                ?.takeIf { it.isNotBlank() }
                ?: "User"
        }

        if (showLiveLocation) {

            LiveLocationScreen(
                viewModel = viewModel,
                token = token,
                onBack = { showLiveLocation = false },
                onNavigateToAddDevice = onNavigateToAddDevice,
                onNavigateToDeviceReport = onNavigateToDeviceReport,
                onOpenDeviceStatus = onOpenDeviceStatus,
                onOpenTrackTrace = onOpenTrackTrace,
                onOpenCommunity = onOpenCommunity,
                onSessionExpired = onLogout
            )

        } else {

            HomeScreen(
                userName = userName,
                onDriverScore = onNavigateToDriverScore,
                onParking = onNavigateToParking,         // ✅ CHANGED
                onSosTag = {
                    println("Visitor Chats opened manually")
                    onSosChatInbox()
                },
                onOpenLiveLocation = { showLiveLocation = true },

                onManageDevice = { onNavigateToManageDevice() },

                onGpsData = { onNavigateToGPSData() },

                onGeoFence = { onNavigateToGeoFencing() },

                onHistory = { onNavigateToHistory() },

                onRefer = { onNavigateToRefer() },

                onOpenAddDevice = { onNavigateToAddDevice() },

                onOpenDocuments = {
                    onDocumentsTabSelected()   // 🔐 Documents vault open flow
                },


                onOpenDeviceReport = {
                    onNavigateToDeviceReport()  // 📊 Today Device Report
                },

                onOpenCommunity = onOpenCommunity,

                onCriminalVerification = onNavigateToCriminalVerification,
                onInstallationSupport = onNavigateToInstallationSupport,
                onOpenNotifications = { onSosTagManual() },
                unreadNotificationCount = unreadNotifCount
            )
        }
    }

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 0u,
            title = "Home",
            icon = rememberVectorPainter(Icons.Default.Home)
        )
}

//class DocumentsTab(
//    private val onRequestDocuments: () -> Unit
//) : Tab {
//
//    @Composable
//    override fun Content() {
//        LaunchedEffect(Unit) {
//            onRequestDocuments()   // 🔐 password flow trigger
//        }
//        Box(modifier = Modifier.fillMaxSize())
//    }
//
//    override val options: TabOptions
//        @Composable get() = TabOptions(
//            index = 1u,
//            title = "E-Vault",
//            icon = rememberVectorPainter(Icons.Default.Folder)
//        )
//}

class DocumentsTab(
    private val documentsGateState: DocumentsGateState,
    private val onUnlock: () -> Unit
) : Tab {

    //    @Composable
//    override fun Content() {
//        when (documentsGateState) {
//            DocumentsGateState.LOCKED -> {
//                LockedDocumentsPlaceholder(
//                    onUnlockClick = onUnlock
//                )
//            }
//            DocumentsGateState.UNLOCKED -> {
//                // Actual documents screen trigger
//                LaunchedEffect(Unit) {
//                    onUnlock()
//                }
//            }
//            else -> Unit
//        }
//    }
    @Composable
    override fun Content() {
        when (documentsGateState) {
            DocumentsGateState.UNLOCKED -> Box(modifier = Modifier.fillMaxSize())
            else -> LockedDocumentsPlaceholder(onUnlockClick = onUnlock)
        }
    }

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = "E-Vault",
            icon = rememberVectorPainter(Icons.Default.Folder)
        )
}

class OrdersTab(
    private val category: String?
) : Tab {

    @Composable
    override fun Content() {
        Navigator(ShopScreenWrapper(category))
    }

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 2u,
            title = "Shop",
            icon = rememberVectorPainter(Icons.Default.ShoppingCart)
        )
}


class ShopScreenWrapper(
    private val category: String? = null
) : Screen {

    @Composable
    override fun Content() {
        ShopScreenHost(category)
    }
}

class ProfileTab(
    private val apiService: ApiService,
    private val storageManager: StorageManager,
    private val refreshTrigger: Int,
    private val onNavigateToEditProfile: () -> Unit,
    private val onNavigateToManageDevice: () -> Unit,
    private val onNavigateToGPSData: () -> Unit,
    private val onNavigateToPrivacyPolicy: () -> Unit,
    private val onNavigateToTermsConditions: () -> Unit,
    private val onNavigateToRefundPolicy: () -> Unit,
    private val onNavigateToShippingPolicy: () -> Unit,
    private val onNavigateToChangePassword: () -> Unit,
    private val onNavigateToMyAccount: () -> Unit,
    private val onNavigateToGeoFencing: () -> Unit,
    private val onNavigateToHistoryPlayback: () -> Unit,
    private val onNavigateToGeneralSettings: () -> Unit,
    private val onNavigateToAccountActivity: () -> Unit,
    private val onNavigateToReferAndEarn: () -> Unit,
    private val onNavigateToAboutApp: () -> Unit = {},
    private val onNavigateToDocuments: () -> Unit,
    private val onLogout: () -> Unit
) : Tab {
    @Composable
    override fun Content() {
        ProfileScreen(
            apiService = apiService,
            storageManager = storageManager,
            refreshTrigger = refreshTrigger,
            onEditProfile = onNavigateToEditProfile,
            onManageDevice = onNavigateToManageDevice,
            onGpsData = onNavigateToGPSData,
            onPrivacyPolicy = onNavigateToPrivacyPolicy,
            onTermsConditions = onNavigateToTermsConditions,
            onRefundPolicy = onNavigateToRefundPolicy,
            onShippingPolicy = onNavigateToShippingPolicy,
            onChangePassword = onNavigateToChangePassword,
            onGeoFencing = onNavigateToGeoFencing,
            onHistoryPlayback = onNavigateToHistoryPlayback,
            onGeneralSettings = onNavigateToGeneralSettings,
            onAccountActivity = onNavigateToAccountActivity,
            onReferAndEarn = onNavigateToReferAndEarn,
            onAboutApp = onNavigateToAboutApp,
            onLogout = onLogout,
            onOpenDocumentsVault = onNavigateToDocuments,
            onNavigateMyAccount = onNavigateToMyAccount, // ✅ OK NOW
        )
    }

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 3u,
            title = "Profile",
            icon = rememberVectorPainter(Icons.Default.Person)
        )
}

// ------------------ MAIN NAV GRAPH -------------------

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeNavGraph(
    viewModel: HomeViewModel,
    apiService: ApiService,
    storageManager: StorageManager,
    token: String,
    userPrefs: UserPrefs,         // ✅ add karo
    onNavigateToAddDevice: () -> Unit,
    onNavigateToDeviceReport: () -> Unit,
    onOpenDeviceStatus: () -> Unit,
    onOpenTrackTrace: () -> Unit,
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToAboutApp: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    SetupStatusBar()


    var selectedTabIndex by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    var shopCategory by remember { mutableStateOf<String?>(null) }
    var pendingProductId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        DeepLinkBus.navigateTo.collect { dest ->
            when {
                dest.startsWith("product_detail_10") -> {
                    pendingProductId = 10
                    shopCategory = null
                    selectedTabIndex = 2
                }
                dest.startsWith("product_detail_11") -> {
                    pendingProductId = 11
                    shopCategory = null
                    selectedTabIndex = 2
                }
                dest.startsWith("product_detail_26") -> {
                    pendingProductId = 26
                    shopCategory = null
                    selectedTabIndex = 2
                }
                dest.startsWith("shop_wired") -> {
                    shopCategory = "wired"
                    selectedTabIndex = 2
                }
                dest.startsWith("shop_wireless") -> {
                    shopCategory = "wireless"
                    selectedTabIndex = 2
                }
                dest.startsWith("shop") -> {
                    shopCategory = null
                    selectedTabIndex = 2
                }
            }
            DeepLinkBus.clear()
        }
    }

    var selectedWarrantyDeviceId by remember { mutableStateOf("") }
    var showCriminalVerification by remember { mutableStateOf(false) }
    var showInstallationSupport by remember { mutableStateOf(false) }
    // Screen visibility states
    var showEditProfile by remember { mutableStateOf(false) }
    var showManageDevice by remember { mutableStateOf(false) }
    var showGPSData by remember { mutableStateOf(false) }
    var showEditDeviceName by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTermsConditions by remember { mutableStateOf(false) }
    var showRefundPolicy by remember { mutableStateOf(false) }
    var showShippingPolicy by remember { mutableStateOf(false) }
    var showGeoFencing by remember { mutableStateOf(false) }
    var showHistoryPlayback by remember { mutableStateOf(false) }
    var showGeneralSettings by remember { mutableStateOf(false) }
    var showAccountActivity by remember { mutableStateOf(false) }
    var showReferAndEarn by remember { mutableStateOf(false) }
    var showAboutApp by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showAddDevice by remember { mutableStateOf(false) }
    var showDocuments by remember { mutableStateOf(false) }
    var showDocumentViewer by remember { mutableStateOf(false) }
    var viewerBytes by remember { mutableStateOf<ByteArray?>(null) }
    var viewerFileName by remember { mutableStateOf("") }
    var showUploadDocument by remember { mutableStateOf(false) }
    var showVaultPasswordDialog by remember { mutableStateOf(false) }
    var showCommunity by remember { mutableStateOf(false) }
    var showCreatePost by remember { mutableStateOf(false) }
    var showCreateStory by remember { mutableStateOf(false) }
    var showStoryViewer by remember { mutableStateOf(false) }


    var showDriverScore by remember { mutableStateOf(false) }
    var showParking by remember { mutableStateOf(false) }
    var showNotificationsScreen by remember { mutableStateOf(false) }

    // ── Incoming call flow (ringing screen → active call screen) ─────
    var showIncomingCallScreen by remember { mutableStateOf(false) }
    var showActiveCallScreen   by remember { mutableStateOf(false) }
    var callIsConnected        by remember { mutableStateOf(false) }
    var callIsRinging          by remember { mutableStateOf(false) }
    var incomingCallData       by remember { mutableStateOf<IncomingCallData?>(null) }
    // Holds the active WebRTC + Socket.IO manager for the current call.
    // Recreated on each new call; disposed on end or remote hang-up.
    var activeVoiceCallManager by remember { mutableStateOf<VoiceCallManager?>(null) }


    // ✅ SOS Tag — inbox list + chat (QuickReply) + confirmation
    var showSosChatInbox by remember { mutableStateOf(false) }
    var showSosQuickReply by remember { mutableStateOf(false) }
    var showSosConfirmation by remember { mutableStateOf(false) }
    var currentSosAlertId by remember { mutableStateOf<Int?>(null) }
    var currentVisitorName by remember { mutableStateOf("Visitor") }
    var currentVisitorPhone by remember { mutableStateOf<String?>(null) }
    var sosOpenedFromInbox by remember { mutableStateOf(false) }
    val sosTagViewModel = remember {
        SosTagViewModel(
            apiService = apiService,
            userPrefs = userPrefs
        )
    }

    // Keep badge count fresh — load chats once on entry, then every 30s silently
    LaunchedEffect(Unit) {
        sosTagViewModel.loadChatsWithMessages()
        while (true) {
            kotlinx.coroutines.delay(30_000)
            sosTagViewModel.refreshChatsWithMessages()
        }
    }
    val _threadStateForBadge by sosTagViewModel.threadListState.collectAsState()
    // Recalculate badge, skipping threads the user has already seen in the panel
    LaunchedEffect(_threadStateForBadge) {
        countUnreadNotifications(_threadStateForBadge)   // updates NotificationBadgeManager internally
    }

    val parkingViewModel = remember {
        ParkingViewModel(apiService = apiService, userPrefs = userPrefs)
    }

    // ✅ Notification automatic redirect — directly open chat
    LaunchedEffect(Unit) {
        SosNotificationHandler.alertFlow.collect { alertId ->
            println("📲 Auto redirect SOS Alert $alertId")

            if (showSosQuickReply && alertId == currentSosAlertId) {
                // Same chat is already open — socket + QuickReplyScreen's alertFlow collector
                // handle the new message in real-time. No navigation change needed.
                return@collect
            }

            if (showSosChatInbox) {
                // User is on the inbox list — refresh it so the new message appears,
                // then open the specific chat directly.
                sosTagViewModel.refreshChatsWithMessages()
            }

            // Close all unrelated overlays and open the chat for this alert
            showSosConfirmation = false
            showInstallationSupport = false
            showCriminalVerification = false
            showCommunity = false
            showParking = false
            showDriverScore = false
            showIncomingCallScreen = false
            showActiveCallScreen   = false

            sosTagViewModel.resetAll()
            currentSosAlertId = alertId
            currentVisitorName = SosNotificationHandler.lastVisitorName ?: "Visitor"
            currentVisitorPhone = SosNotificationHandler.lastVisitorPhone
            showSosQuickReply = true
            SosNotificationHandler.clear()
        }
    }

    // ── Incoming call notification → show ringing screen ────────────
    LaunchedEffect(Unit) {
        SosNotificationHandler.callFlow.collect { callData ->
            // Ignore if the user has already accepted and is on the active call screen.
            if (showActiveCallScreen) {
                println("⚠️ callFlow ignored — CallScreen already active (roomId=${callData.roomId})")
                SosNotificationHandler.clearCallFlow()
                return@collect
            }
            println("📞 Auto redirect Incoming Call name=${callData.visitorName}")
            incomingCallData       = callData
            showActiveCallScreen   = false
            showIncomingCallScreen = true
            // Connect to signaling room so we hear accept/decline from other devices immediately
            startIncomingCallWatcher(callData.roomId)
            SosNotificationHandler.clearCallFlow()
        }
    }

    // ── WebRTC connected → mark call as live ─────────────────────────
    LaunchedEffect(Unit) {
        SosNotificationHandler.callConnectedFlow.collect {
            println("🟢 Call connected — updating UI state")
            callIsConnected = true
        }
    }

    // ── Callee joined signaling room → stop ringing, show connecting ──
    LaunchedEffect(Unit) {
        SosNotificationHandler.calleeJoinedFlow.collect {
            println("📲 Callee joined — transitioning ringing → connecting")
            callIsRinging = false
        }
    }

    // ── Caller cancelled / call_ended → dismiss ringing or active call ─
    var callFailedMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        SosNotificationHandler.callEndedFlow.collect {
            println("📵 Caller cancelled — dismissing call screens")
            stopCallSound()
            stopIncomingCallWatcher()
            val chatId = incomingCallData?.chatId   // capture before clearing
            val wasOnActiveCall = showActiveCallScreen
            val wasConnected = callIsConnected

            activeVoiceCallManager?.dispose()
            activeVoiceCallManager  = null
            showIncomingCallScreen  = false
            showActiveCallScreen    = false
            callIsConnected         = false
            callIsRinging           = false
            incomingCallData        = null

            if (wasOnActiveCall && !wasConnected) {
                // Call ended before ICE connected → visitor unavailable / declined
                callFailedMsg = "Call disconnect by visitor. Please try again."
            }

            // Only redirect to chat if the call was actually connected (visitor hung up mid-call).
            // Do NOT redirect when the call failed before connecting (visitor unavailable).
            if (wasOnActiveCall && wasConnected && chatId != null && chatId > 0) {
                sosTagViewModel.resetAll()
                currentSosAlertId = chatId
                showSosQuickReply = true
            }
        }
    }

    // ── Another device accepted the same call → dismiss ringing screen ─
    // acceptingOnThisDevice is set in onAccepted before the watcher is stopped,
    // so if call-resolved echoes back to our still-open watcher socket we don't
    // show the toast on the device that just accepted the call.
    var acceptingOnThisDevice    by remember { mutableStateOf(false) }
    var callAcceptedElsewhereMsg by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        SosNotificationHandler.callAcceptedElsewhereFlow.collect {
            if (acceptingOnThisDevice) {
                // This device was the one that accepted — suppress the toast.
                acceptingOnThisDevice  = false
                stopCallSound()
                stopIncomingCallWatcher()
                showIncomingCallScreen = false
                incomingCallData       = null
                return@collect
            }
            println("📱 Another device accepted — dismissing IncomingCallScreen")
            stopCallSound()
            stopIncomingCallWatcher()
            showIncomingCallScreen   = false
            incomingCallData         = null
            callAcceptedElsewhereMsg = true
        }
    }

    // ── Owner accepted call from lock screen (IncomingCallActivity) ──────
    // Skip the ringing screen and go straight to CallScreen.
    LaunchedEffect(Unit) {
        SosNotificationHandler.callAcceptedByOwnerFlow.collect { callData ->
            SosNotificationHandler.clearAcceptedByOwnerFlow()
            cancelCallNotification()
            acceptingOnThisDevice = true   // suppress toast if call-resolved echoes back
            stopIncomingCallWatcher()
            println("✅ callAcceptedByOwnerFlow — starting CallScreen roomId=${callData.roomId}")
            incomingCallData = callData
            callIsConnected  = false
            val mgr = VoiceCallManager()
            mgr.startAsCallee(callData.roomId)
            activeVoiceCallManager = mgr
            showIncomingCallScreen = false
            showActiveCallScreen   = true
        }
    }


    // 🔥 MY ACCOUNT FLOW
    var showMyAccount by remember { mutableStateOf(false) }

// child screens
    var showChangePlan by remember { mutableStateOf(false) }
    var showBillDetails by remember { mutableStateOf(false) }
    var showTransactions by remember { mutableStateOf(false) }
    var showAutoPay by remember { mutableStateOf(false) }
    var showWarranty by remember { mutableStateOf(false) }
    var showPaymentMethods by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var documentsGateState by remember {
        mutableStateOf(DocumentsGateState.NONE)
    }


    // Edit device state
    var editDeviceId by remember { mutableStateOf("") }
    var editDeviceName by remember { mutableStateOf("") }
    var editSimNumber by remember { mutableStateOf("") }

    // Refresh triggers
    var manageDeviceRefresh by remember { mutableStateOf(0) }
    var profileRefresh by remember { mutableStateOf(0) }

    var showExitDialog by remember { mutableStateOf(false) }

    val addDeviceViewModel = remember { AddDeviceViewModel() }

    Box(modifier = Modifier.fillMaxSize()) {

        if (showActiveCallScreen) {

            // ── Active call (after owner accepts) ─────────────────────
            CallScreen(
                visitorName       = incomingCallData?.visitorName  ?: SosNotificationHandler.lastVisitorName  ?: "Visitor",
                visitorPhone      = incomingCallData?.visitorPhone ?: SosNotificationHandler.lastVisitorPhone ?: "",
                voiceCallManager  = activeVoiceCallManager,
                isConnected       = callIsConnected,
                isRinging         = callIsRinging,
                onEnd             = {
                    activeVoiceCallManager?.endCall()
                    activeVoiceCallManager?.dispose()
                    activeVoiceCallManager  = null
                    showActiveCallScreen    = false
                    callIsConnected         = false
                    callIsRinging           = false
                    incomingCallData        = null
                }
            )

        } else if (showIncomingCallScreen) {

            // ── Ringing / accept-decline screen ───────────────────────
            val callData = incomingCallData
            if (callData != null) {
                IncomingCallScreen(
                    visitorName  = callData.visitorName,
                    visitorPhone = callData.visitorPhone,
                    roomId       = callData.roomId,
                    apiService   = apiService,
                    onAccepted   = {
                        acceptingOnThisDevice = true   // must be set before watcher is stopped
                        stopCallSound()
                        cancelCallNotification()
                        stopIncomingCallWatcher()
                        callIsConnected = false   // reset — will be set true when ICE connects
                        val mgr = VoiceCallManager()
                        mgr.startAsCallee(callData.roomId)
                        activeVoiceCallManager = mgr
                        showIncomingCallScreen = false
                        showActiveCallScreen   = true
                    },
                    onDeclined   = {
                        stopCallSound()
                        cancelCallNotification()
                        stopIncomingCallWatcher()
                        // Immediately notify the caller via socket so their screen closes
                        // without waiting for an FCM round-trip or the 60s ring timeout.
                        if (callData.roomId.isNotBlank()) {
                            VoiceCallManager().sendDeclineSignal(callData.roomId)
                        }
                        showIncomingCallScreen = false
                        showActiveCallScreen   = false
                        val chatId = callData.chatId
                        if (chatId != null && chatId > 0) {
                            sosTagViewModel.resetAll()
                            currentSosAlertId = chatId
                            showSosQuickReply = true
                        }
                        incomingCallData = null
                    }
                )
            }

        } else if (showParking) {

            ParkingScreen(
                viewModel = parkingViewModel,
                onBackPressed = { showParking = false }
            )

        } else if (showNotificationsScreen) {

            NotificationsScreen(
                viewModel = sosTagViewModel,
                onBackPressed = {
                    showNotificationsScreen = false
                    sosTagViewModel.resetAll()
                },
                onNotificationClick = { chatId, visitorName, visitorPhone ->
                    NotificationBadgeManager.markOneSeen(chatId)
                    currentSosAlertId = chatId
                    currentVisitorName = visitorName
                    currentVisitorPhone = visitorPhone
                    showNotificationsScreen = false
                    sosOpenedFromInbox = true
                    showSosQuickReply = true
                }
            )

        } else if (showSosChatInbox) {

            ChatInboxScreen(
                viewModel = sosTagViewModel,
                onChatSelected = { chatId, visitorName, visitorPhone ->
                    currentSosAlertId = chatId
                    currentVisitorName = visitorName
                    currentVisitorPhone = visitorPhone
                    showSosChatInbox = false
                    sosOpenedFromInbox = true
                    showSosQuickReply = true
                },
                onBackPressed = {
                    showSosChatInbox = false
                    sosTagViewModel.resetAll()
                }
            )

        } else if (showSosQuickReply) {

            key(currentSosAlertId) {
            QuickReplyScreen(
                alertId = currentSosAlertId ?: 0,
                visitorName = currentVisitorName,
                visitorPhone = currentVisitorPhone,
                viewModel = sosTagViewModel,
                onConfirmed = {
                    showSosQuickReply = false
                    showSosConfirmation = true
                },
                onBackPressed = {
                    showSosQuickReply = false
                    currentSosAlertId = null
                    sosTagViewModel.resetAll()
                    if (sosOpenedFromInbox) {
                        sosOpenedFromInbox = false
                        showSosChatInbox = true
                    }
                },
                onInitiateCall = { chatId, onStarted, onFailed ->
                    coroutineScope.launch {
                        println("📞 Owner initiating call — chatId=$chatId")
                        val resp = apiService.initiateCall(chatId)
                        val roomId = resp.room_id
                        if (resp.success && !roomId.isNullOrBlank()) {
                            println("📞 Call initiated — roomId=$roomId, starting as caller")
                            val mgr = VoiceCallManager()
                            mgr.startAsCaller(roomId)
                            incomingCallData = IncomingCallData(
                                visitorName  = currentVisitorName,
                                visitorPhone = currentVisitorPhone ?: "",
                                roomId       = roomId,
                                chatId       = chatId
                            )
                            activeVoiceCallManager = mgr
                            callIsConnected        = false
                            callIsRinging          = true
                            showActiveCallScreen   = true
                            onStarted()
                        } else {
                            println("❌ initiateCall failed: ${resp.message}")
                            onFailed(resp.message ?: "Could not start call. Please try again.")
                        }
                    }
                }
            )
            } // key(currentSosAlertId)

        } else if (showSosConfirmation) {

            ConfirmationScreen(
                alertId = currentSosAlertId ?: 0,
                viewModel = sosTagViewModel,
                onClose = {
                    showSosConfirmation = false
                    currentSosAlertId = null
                    sosTagViewModel.resetAll()
                }
            )

        } else if (showInstallationSupport) {

            InstallationSupportScreen(
                onBack = { showInstallationSupport = false }
            )

        } else if (showCriminalVerification) {

            CriminalVerificationFlow(
                apiService = apiService,
                onBack = { showCriminalVerification = false }
            )

        } else if (showEditProfile) {

            EditProfileScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = {
                    showEditProfile = false
                    profileRefresh++
                }
            )

        } else if (showCreatePost) {

            CreatePostScreen(onBack = { showCreatePost = false })

        } else if (showCreateStory) {

            CreateStoryScreen(onBack = { showCreateStory = false })

        } else if (showStoryViewer) {

            StoryViewerScreen(onBack = { showStoryViewer = false })

        } else if (showCommunity) {

            CommunityScreenRoot(
                onBack = { showCommunity = false },
                onOpenCreatePost = { showCreatePost = true },
                onOpenCreateStory = { showCreateStory = true },
                onOpenStoryViewer = { showStoryViewer = true }
            )

        } else if (showDocumentViewer && viewerBytes != null) {

            val androidContext = getAppContext()
            DocumentViewerScreen(
                fileName = viewerFileName,
                bytes = viewerBytes!!,
                onBack = {
                    showDocumentViewer = false
                    showDocuments = true
                },
                onDownload = {
                    platformDownloadFile(
                        context = androidContext,
                        bytes = viewerBytes!!,
                        fileName = viewerFileName
                    )
                }
            )

        } else if (showUploadDocument) {

            UploadDocumentScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBack = {
                    showUploadDocument = false
                    showDocuments = true
                }
            )

        } else if (showDocuments) {

            DocumentsScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBack = {
                    showDocuments = false
                    documentsGateState = DocumentsGateState.LOCKED
                },
                onOpenViewer = { bytes, fileName ->
                    viewerBytes = bytes
                    viewerFileName = fileName
                    showDocuments = false
                    showDocumentViewer = true
                },
                onOpenUpload = {
                    showDocuments = false
                    showUploadDocument = true
                }
            )

        } else if (showManageDevice) {

            ManageDeviceScreen(
                apiService = apiService,
                storageManager = storageManager,
                refreshTrigger = manageDeviceRefresh,
                onBackClick = { showManageDevice = false },
                onProfileClick = { showManageDevice = false },
                onAddDeviceClick = {
                    showManageDevice = false
                    onNavigateToAddDevice()
                },
                onEditDeviceClick = { deviceId, deviceName, simNumber ->
                    editDeviceId = deviceId
                    editDeviceName = deviceName
                    editSimNumber = simNumber
                    showManageDevice = false
                    showEditDeviceName = true
                }
            )

        } else if (showEditDeviceName) {

            EditDeviceNameScreen(
                apiService = apiService,
                storageManager = storageManager,
                deviceId = editDeviceId,
                deviceName = editDeviceName,
                simNumber = editSimNumber,
                onBackPressed = {
                    showEditDeviceName = false
                    showManageDevice = true
                },
                onUpdateSuccess = {
                    showEditDeviceName = false
                    showManageDevice = true
                    manageDeviceRefresh++
                }
            )

        } else if (showGPSData) {

            GPSDataScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = { showGPSData = false }
            )

        } else if (showGeoFencing) {

            GeoFencingScreenWrapper(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = { showGeoFencing = false }
            )

        } else if (showHistoryPlayback) {

            HistoryPlaybackScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = { showHistoryPlayback = false }
            )

        } else if (showDriverScore) {

            DriverScoreFlowScreen(
                onBack = { showDriverScore = false }
            )

        } else if (showChangePassword) {

            ChangePasswordScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = {
                    showChangePassword = false
                    selectedTabIndex = 3
                }
            )

        } else if (showWarranty) {

            WarrantyRoute(
                deviceId = selectedWarrantyDeviceId,
                onBack = {
                    showWarranty = false
                    showMyAccount = true
                },
                onBuyWarranty = { println("Buy warranty clicked") },
                onAddDevice = {
                    showWarranty = false
                    showAddDevice = true
                }
            )

        } else if (showBillDetails) {

            val vm = remember { BillDetailsViewModel(apiService, storageManager) }
            LaunchedEffect(Unit) { vm.loadBills() }
            BillDetailsScreen(
                bills = vm.bills,
                onBack = {
                    showBillDetails = false
                    showMyAccount = true
                }
            )

        } else if (showChangePlan) {

            ChangePlanScreenHost(
                onBack = {
                    showChangePlan = false
                    showMyAccount = true
                }
            )

        } else if (showMyAccount) {

            MyAccountScreen(
                selectedDeviceId = selectedWarrantyDeviceId,
                onBack = { showMyAccount = false },
                onChangePlan = {
                    showMyAccount = false
                    showChangePlan = true
                },
                onBillDetails = {
                    showMyAccount = false
                    showBillDetails = true
                },
                onWarranty = { deviceId ->
                    selectedWarrantyDeviceId = deviceId
                    showMyAccount = false
                    showWarranty = true
                }
            )

        } else if (showGeneralSettings) {

            GeneralSettingsScreen(onBackPressed = { showGeneralSettings = false })

        } else if (showAccountActivity) {

            AccountActivityScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = { showAccountActivity = false },
                onAccountDeleted = {
                    showAccountActivity = false
                    onLogout()
                }
            )

        } else if (showReferAndEarn) {

            ReferAndEarnScreen(
                token = token,
                apiService = apiService,
                onBackPressed = { showReferAndEarn = false },
                onNavigateToShop = {
                    showReferAndEarn = false
                    selectedTabIndex = 2
                },
                onNavigateToAddDevice = {
                    showReferAndEarn = false
                    showAddDevice = true
                }
            )

        } else if (showAddDevice) {

            AddDeviceDetailsCommon(
                viewModel = addDeviceViewModel,
                onNavigate = { route ->
                    when (route) {
                        "back", "home" -> showAddDevice = false
                    }
                }
            )

        } else if (showPrivacyPolicy) {

            PrivacyPolicy_TandCScreen(
                type = WebViewType.PRIVACY_POLICY,
                onBackPressed = { showPrivacyPolicy = false }
            )

        } else if (showTermsConditions) {

            PrivacyPolicy_TandCScreen(
                type = WebViewType.TERMS_CONDITIONS,
                onBackPressed = { showTermsConditions = false }
            )

        } else if (showRefundPolicy) {

            PrivacyPolicy_TandCScreen(
                type = WebViewType.REFUND_POLICY,
                onBackPressed = { showRefundPolicy = false }
            )

        } else if (showShippingPolicy) {

            PrivacyPolicy_TandCScreen(
                type = WebViewType.SHIPPING_POLICY,
                onBackPressed = { showShippingPolicy = false }
            )

        } else if (showAboutApp) {

            AboutAppScreen(onBackClick = { showAboutApp = false })

        } else {

            HomeNavGraphContent(
                viewModel = viewModel,
                apiService = apiService,
                storageManager = storageManager,
                token = token,
                pendingProductId = pendingProductId,
                onProductConsumed = { pendingProductId = null },
                profileRefreshTrigger = profileRefresh,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                shopCategory = shopCategory,
                onNavigateToAddDevice = onNavigateToAddDevice,
                onNavigateToDeviceReport = onNavigateToDeviceReport,
                documentsGateState = documentsGateState,
                onOpenDeviceStatus = onOpenDeviceStatus,
                onOpenTrackTrace = onOpenTrackTrace,
                onNavigateToEditProfile = { showEditProfile = true },
                onNavigateToManageDevice = { showManageDevice = true },
                onNavigateToGPSData = { showGPSData = true },
                onNavigateToPrivacyPolicy = { showPrivacyPolicy = true },
                onNavigateToTermsConditions = { showTermsConditions = true },
                onNavigateToRefundPolicy = { showRefundPolicy = true },
                onNavigateToShippingPolicy = { showShippingPolicy = true },
                onNavigateToChangePassword = { showChangePassword = true },
                onNavigateToGeoFencing = { showGeoFencing = true },
                onNavigateToHistoryPlayback = { showHistoryPlayback = true },
                onNavigateToGeneralSettings = { showGeneralSettings = true },
                onNavigateToAccountActivity = { showAccountActivity = true },
                onNavigateToReferAndEarn = { showReferAndEarn = true },
                onNavigateToAboutApp = { showAboutApp = true },
                onNavigateToCriminalVerification = { showCriminalVerification = true },
                onNavigateToInstallationSupport = { showInstallationSupport = true },
                onNavigateToMyAccount = { showMyAccount = true },
                onNavigateToDocuments = { showCommunity = true },
                onRequestDocumentsWithPassword = { showVaultPasswordDialog = true },
                onDocumentsTabSelected = {
                    documentsGateState = DocumentsGateState.LOCKED
                    showVaultPasswordDialog = true
                },
                onOpenCommunity = { showCommunity = true },
                onNavigateToDriverScore = { showDriverScore = true },
                onNavigateToParking = { showParking = true },
                onNavigateToSosTag = { alertId ->
                    sosTagViewModel.resetAll()
                    currentSosAlertId = alertId
                    showSosQuickReply = true
                },
                onSosTagManual = {
                    sosTagViewModel.resetAll()
                    currentSosAlertId = null
                    showNotificationsScreen = true
                },
                onSosChatInbox = {
                    sosTagViewModel.resetAll()
                    currentSosAlertId = null
                    showSosChatInbox = true
                },
                onLogout = onLogout
            )
        }

        // ── "Call accepted on another device" toast ───────────────────
        if (callAcceptedElsewhereMsg) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                callAcceptedElsewhereMsg = false
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
            ) {
                Snackbar(
                    containerColor = Color(0xFF323232),
                    contentColor   = Color.White,
                ) {
                    Text(
                        text = "Call accepted on another device",
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ── "Call failed" snackbar ────────────────────────────────────
        callFailedMsg?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(4000)
                callFailedMsg = null
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
            ) {
                Snackbar(
                    containerColor = Color(0xFF424242),
                    contentColor   = Color.White,
                ) {
                    Text(
                        text = msg,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // isAnyOverlayOpen — memoized to avoid recomputation on every recomposition
    val isAnyOverlayOpen by remember {
        derivedStateOf {
            showIncomingCallScreen || showActiveCallScreen ||
            showSosConfirmation || showSosQuickReply ||
            showCreatePost || showCreateStory || showStoryViewer || showCommunity ||
            showDocumentViewer || showUploadDocument || showDocuments ||
            showWarranty || showBillDetails || showChangePlan || showMyAccount ||
            showEditDeviceName || showManageDevice || showAddDevice || showGPSData ||
            showGeoFencing || showHistoryPlayback || showGeneralSettings ||
            showAccountActivity || showReferAndEarn || showChangePassword ||
            showAboutApp || showPrivacyPolicy || showTermsConditions ||
            showRefundPolicy || showShippingPolicy || showCriminalVerification ||
            showDriverScore || showParking || showEditProfile ||
            showInstallationSupport
        }
    }

    LaunchedEffect(showMyAccount) {
        if (showMyAccount && selectedWarrantyDeviceId.isEmpty()) {
            try {
                val devices = apiService.getUserDevices()
                val firstId = devices.data.firstOrNull()?.device_id?.toString() ?: ""
                selectedWarrantyDeviceId = firstId
            } catch (e: Exception) {
                println("❌ Failed to set default device: ${e.message}")
            }
        }
    }

    var vaultError by remember { mutableStateOf<String?>(null) }
    val profileViewModel = remember {
        ProfileViewModel(apiService, storageManager)
    }
    val profileState by profileViewModel.state.collectAsState()

    if (showVaultPasswordDialog) {
        VaultPasswordDialog(
            errorMessage = profileState.error,
            onSubmit = { password ->
                profileViewModel.unlockDocumentsVault(
                    password = password,
                    onSuccess = {
                        documentsGateState = DocumentsGateState.UNLOCKED
                        showVaultPasswordDialog = false
                        showDocuments = true
                    }
                )
            },
            onDismiss = {
                showVaultPasswordDialog = false
                documentsGateState = DocumentsGateState.LOCKED
            }
        )
    }

    LaunchedEffect(showVaultPasswordDialog) {
        if (showVaultPasswordDialog) {
            profileViewModel.clearError()
        }
    }


    if (showExitDialog) {
        val activity = LocalPlatformActivity.current
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App") },
            text = { Text("Are you sure you want to exit?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false

                        // ✅ KMP-safe exit
                        exitApp(activity)
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    // ── Overlay back handlers — one per screen so system back works instantly ──
    PlatformBackHandler(enabled = showEditProfile)       { showEditProfile = false; profileRefresh++ }
    PlatformBackHandler(enabled = showManageDevice)      { showManageDevice = false }
    PlatformBackHandler(enabled = showGPSData)           { showGPSData = false }
    PlatformBackHandler(enabled = showEditDeviceName)    { showEditDeviceName = false }
    PlatformBackHandler(enabled = showGeoFencing)        { showGeoFencing = false }
    PlatformBackHandler(enabled = showHistoryPlayback)   { showHistoryPlayback = false }
    PlatformBackHandler(enabled = showGeneralSettings)   { showGeneralSettings = false }
    PlatformBackHandler(enabled = showAccountActivity)   { showAccountActivity = false }
    PlatformBackHandler(enabled = showReferAndEarn)      { showReferAndEarn = false }
    PlatformBackHandler(enabled = showAboutApp)          { showAboutApp = false }
    PlatformBackHandler(enabled = showChangePassword)    { showChangePassword = false }
    PlatformBackHandler(enabled = showPrivacyPolicy)     { showPrivacyPolicy = false }
    PlatformBackHandler(enabled = showTermsConditions)   { showTermsConditions = false }
    PlatformBackHandler(enabled = showRefundPolicy)      { showRefundPolicy = false }
    PlatformBackHandler(enabled = showShippingPolicy)    { showShippingPolicy = false }
    PlatformBackHandler(enabled = showCriminalVerification) { showCriminalVerification = false }
    PlatformBackHandler(enabled = showInstallationSupport)  { showInstallationSupport = false }
    PlatformBackHandler(enabled = showDriverScore)       { showDriverScore = false }
    PlatformBackHandler(enabled = showParking)           { showParking = false }
    PlatformBackHandler(enabled = showMyAccount)         { showMyAccount = false }
    PlatformBackHandler(enabled = showChangePlan)        { showChangePlan = false; showMyAccount = true }
    PlatformBackHandler(enabled = showBillDetails)       { showBillDetails = false; showMyAccount = true }
    PlatformBackHandler(enabled = showWarranty)          { showWarranty = false; showMyAccount = true }
    // Document flow back handlers
    PlatformBackHandler(enabled = showDocuments)         { showDocuments = false }
    PlatformBackHandler(enabled = showUploadDocument)    { showUploadDocument = false; showDocuments = true }
    PlatformBackHandler(enabled = showDocumentViewer)    { showDocumentViewer = false; showDocuments = true }
    PlatformBackHandler(enabled = showAddDevice)         { showAddDevice = false }
    PlatformBackHandler(enabled = showVaultPasswordDialog) { showVaultPasswordDialog = false }
    PlatformBackHandler(enabled = showIncomingCallScreen) {
        stopCallSound()
        cancelCallNotification()
        incomingCallData?.roomId?.takeIf { it.isNotBlank() }?.let { VoiceCallManager().sendDeclineSignal(it) }
        showIncomingCallScreen = false
        incomingCallData = null
    }
    PlatformBackHandler(enabled = showActiveCallScreen)   { showActiveCallScreen = false; incomingCallData = null }
    PlatformBackHandler(enabled = showSosQuickReply)     { showSosQuickReply = false; currentSosAlertId = null; sosTagViewModel.resetAll() }
    PlatformBackHandler(enabled = showSosConfirmation)   { showSosConfirmation = false; currentSosAlertId = null; sosTagViewModel.resetAll() }
    PlatformBackHandler(enabled = showStoryViewer)       { showStoryViewer = false }
    PlatformBackHandler(enabled = showCreateStory)       { showCreateStory = false }
    PlatformBackHandler(enabled = showCreatePost)        { showCreatePost = false }
    PlatformBackHandler(enabled = showCommunity)         { showCommunity = false }

    // ── Root back handler — composed LAST so it has highest LIFO priority.
    // Disabled when any overlay is open (overlay-specific handlers above take over).
    PlatformBackHandler(enabled = !isAnyOverlayOpen) {
        if (selectedTabIndex == 0) showExitDialog = true else selectedTabIndex = 0
    }
}

@Composable
fun LockedDocumentsPlaceholder(
    onUnlockClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box {
            // 🔘 Card
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3F3F3)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(
                            top = 40.dp,
                            bottom = 24.dp,
                            start = 24.dp,
                            end = 24.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Documents are locked",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Enter Password to access your documents",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onUnlockClick,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6A9F50)
                        ),
                        modifier = Modifier
                            .height(44.dp)
                            .fillMaxWidth(0.8f)
                    ) {
                        Text(
                            text = "Unlock Documents",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 🔐 Floating lock icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-28).dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF6A9F50),
                    shadowElevation = 6.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeNavGraphContent(
    viewModel: HomeViewModel,
    apiService: ApiService,
    storageManager: StorageManager,
    token: String,
    pendingProductId: Int? = null,
    onProductConsumed: () -> Unit = {},
    profileRefreshTrigger: Int = 0,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    shopCategory: String?,
    onNavigateToAddDevice: () -> Unit,
    onNavigateToDeviceReport: () -> Unit,
    onOpenDeviceStatus: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToManageDevice: () -> Unit,
    onNavigateToGPSData: () -> Unit,
    onOpenTrackTrace: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToTermsConditions: () -> Unit,
    documentsGateState: DocumentsGateState,   // ✅ ADD THIS
    onNavigateToRefundPolicy: () -> Unit,
    onNavigateToShippingPolicy: () -> Unit,
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToGeoFencing: () -> Unit = {},
    onNavigateToHistoryPlayback: () -> Unit = {},
    onNavigateToGeneralSettings: () -> Unit = {},
    onNavigateToAccountActivity: () -> Unit = {},
    onNavigateToReferAndEarn: () -> Unit = {},
    onNavigateToAboutApp: () -> Unit = {},
    onNavigateToDocuments: () -> Unit,
    onNavigateToMyAccount: () -> Unit,   // ✅ ADD
    onRequestDocumentsWithPassword: () -> Unit,
    onDocumentsTabSelected: () -> Unit,   // ✅ ADD
    onNavigateToCriminalVerification: () -> Unit,   // ✅ ADD HERE
    onNavigateToInstallationSupport: () -> Unit,
    onOpenCommunity: () -> Unit,
    onNavigateToDriverScore: () -> Unit,  // ✅ ADD THIS LINE
    onNavigateToParking: () -> Unit,    // ✅ CHANGED — add after onNavigateToDriverScore
    onNavigateToSosTag: (Int) -> Unit,
    onSosTagManual: () -> Unit,
    onSosChatInbox: () -> Unit,
    onLogout: () -> Unit
) {
    val productRepository = remember { ProductRepository(apiService) }
    val tokenProvider: () -> String = remember {
        { storageManager.getString("token", "") }
    }
    val shopViewModel = remember {
        ShopViewModel(repo = productRepository, tokenProvider = tokenProvider)
    }
    val shopState by shopViewModel.state.collectAsState()

    // ✅ Deeplink product navigate
    LaunchedEffect(pendingProductId, shopState.products) {
        val pid = pendingProductId ?: return@LaunchedEffect
        if (shopState.products.isEmpty()) {
            shopViewModel.loadData()
            return@LaunchedEffect
        }

        val dto = shopState.products.firstOrNull { it.id == pid }
        if (dto != null) {
            val product = ShopProduct(
                id = dto.id,
                title = dto.name ?: "",
                subtitle = dto.device_type ?: "",
                price = dto.sale_price?.toDoubleOrNull() ?: 0.0,
                rechargePrice = dto.recharge_price?.toDoubleOrNull() ?: 0.0,
                promoAmount = dto.promo_amt?.toDoubleOrNull() ?: 0.0,
                promoCode = dto.promo_code ?: "",
                imageUrl = dto.image ?: "",
                description = dto.description ?: "",
                features = listOfNotNull(
                    dto.features1, dto.features2, dto.features3,
                    dto.features4, dto.features5, dto.features6, dto.features7
                ),
                deviceType = dto.device_type ?: "",
                rating = 4.8,
                ratingCount = 50,
                isFavorite = shopViewModel.isFavorite(dto.id)
            )
            // ✅ ShopScreenWrapper ke navigator mein push karo
            DeepLinkBus.navigateTo("open_product_${pid}")
            onProductConsumed()
        }
    }

    // rememberUpdatedState — stale lambda problem fix
    val latestOnSosTag by rememberUpdatedState(onNavigateToSosTag)
    val latestOnSosTagManual by rememberUpdatedState(onSosTagManual)
    val latestOnSosChatInbox by rememberUpdatedState(onSosChatInbox)

    val homeTab = remember {
        HomeTab(
            viewModel = viewModel,
            token = token,
            onNavigateToAddDevice = onNavigateToAddDevice,
            onNavigateToManageDevice = onNavigateToManageDevice,
            onNavigateToGPSData = onNavigateToGPSData,
            onNavigateToGeoFencing = onNavigateToGeoFencing,
            onNavigateToHistory = onNavigateToHistoryPlayback,
            onNavigateToRefer = onNavigateToReferAndEarn,
            onOpenCommunity = onOpenCommunity,
            onOpenDeviceStatus = onOpenDeviceStatus,
            onNavigateToDeviceReport = onNavigateToDeviceReport,
            onOpenTrackTrace = onOpenTrackTrace,
            onDocumentsTabSelected = onDocumentsTabSelected,
            onNavigateToCriminalVerification = onNavigateToCriminalVerification,
            onNavigateToDriverScore = onNavigateToDriverScore,
            onNavigateToParking = onNavigateToParking,
            onNavigateToSosTag = { alertId -> latestOnSosTag(alertId) },
            onSosTagManual = { latestOnSosTagManual() },
            onSosChatInbox = { latestOnSosChatInbox() },
            onNavigateToInstallationSupport = onNavigateToInstallationSupport,
            onLogout = onLogout,
        )
    }

    val profileTab = remember(profileRefreshTrigger) {
        ProfileTab(
            apiService,
            storageManager,
            profileRefreshTrigger,
            onNavigateToEditProfile,
            onNavigateToManageDevice,
            onNavigateToGPSData,
            onNavigateToPrivacyPolicy,
            onNavigateToTermsConditions,
            onNavigateToRefundPolicy,
            onNavigateToShippingPolicy,
            onNavigateToChangePassword,
            onNavigateToMyAccount,     // ✅ PASS IT
            onNavigateToGeoFencing,
            onNavigateToHistoryPlayback,
            onNavigateToGeneralSettings,
            onNavigateToAccountActivity,
            onNavigateToReferAndEarn,
            onNavigateToAboutApp,
            onNavigateToDocuments,
            onLogout,

            )
    }

    val ordersTab = remember(shopCategory) {
        OrdersTab(shopCategory)
    }
    val documentsTab = remember(documentsGateState) {
        DocumentsTab(
            documentsGateState = documentsGateState,
            onUnlock = {
                // 🔐 password dialog sirf yahin se trigger hoga
                onDocumentsTabSelected()
            }
        )
    }
    val tabs = listOf(homeTab, documentsTab, ordersTab, profileTab)

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            if (BottomBarController.visible) {
                CustomBottomNavBar(
                    tabs = tabs,
                    selectedIndex = selectedTabIndex,
                    onTabSelected = { index ->
                        if (index == 1 && selectedTabIndex != 1) {
                            onDocumentsTabSelected()
                        }
                        onTabSelected(index)
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            tabs[selectedTabIndex].Content()
        }
    }


}

@Composable
fun CustomBottomNavBar(
    tabs: List<Tab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val primary = Color(0xFF6A9F50)
    val unselected = Color(0xFF9CA3AF)
    val bgColor = Color(0xFFFFFFFF)
    val borderColor = Color(0xFFF0F2ED)

    Surface(
        color = bgColor,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Top divider line
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(borderColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = selectedIndex == index

                    val iconTint by animateColorAsState(
                        targetValue = if (selected) primary else unselected,
                        animationSpec = tween(250),
                        label = "navIconColor"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(index) }
                    ) {
                        // Active indicator pill
                        Box(
                            modifier = Modifier
                                .width(if (selected) 48.dp else 0.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(if (selected) primary else Color.Transparent)
                        )

                        Spacer(Modifier.height(6.dp))

                        // Icon with subtle background when selected
                        Box(
                            modifier = Modifier
                                .size(if (selected) 40.dp else 36.dp)
                                .background(
                                    if (selected) primary.copy(alpha = 0.1f) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = tab.options.icon!!,
                                contentDescription = tab.options.title,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = tab.options.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = iconTint,
                                letterSpacing = 0.1.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

enum class DocumentsGateState {
    NONE,
    LOCKED,
    UNLOCKED
}

@Composable
expect fun SetupStatusBar()

