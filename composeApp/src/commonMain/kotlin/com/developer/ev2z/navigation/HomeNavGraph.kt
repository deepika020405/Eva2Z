package com.developer.ev2z.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.*
import com.developer.ev2z.LocalPlatformActivity
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.getAppContext
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.repository.ProductRepository
import com.developer.ev2z.ui.screens.EditProfile.EditProfileScreen
import com.developer.ev2z.ui.screens.GeoFencing.GeoFencingScreenWrapper
import com.developer.ev2z.ui.screens.HistoryPlayback.HistoryPlaybackScreen
import com.developer.ev2z.ui.screens.Profile.ProfileScreen
import com.developer.ev2z.ui.screens.homeScreen.HomeScreen
import com.developer.ev2z.ui.screens.homeScreen.HomeViewModel
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
import com.developer.ev2z.ui.screens.ChangePlan.ChangePlanScreen
import com.developer.ev2z.ui.screens.ChangePlan.ChangePlanScreenHost
//import com.developer.ev2z.ui.screens.ChangePlan.ChangePlanRoute
//import com.developer.ev2z.ui.screens.ChangePlan.ChangePlanScreen
import com.developer.ev2z.ui.screens.ChangePlan.ChangePlanViewModel
import com.developer.ev2z.ui.screens.ChangePlan.DevicePlanUi
import com.developer.ev2z.ui.screens.GPSData.GPSDataScreen
import com.developer.ev2z.ui.screens.MyAccount.MyAccountScreen
//import com.developer.ev2z.ui.screens.GPSData.GPSDataScreen
import com.developer.ev2z.ui.screens.Profile.AboutAppScreen
import com.developer.ev2z.ui.screens.Profile.ProfileViewModel
import com.developer.ev2z.ui.screens.Profile.VaultPasswordDialog
import com.developer.ev2z.ui.screens.ReferAndEarn.ReferAndEarnScreen
import com.developer.ev2z.ui.screens.SupportAndWarrenty.SupportWarrantyScreen
import com.developer.ev2z.ui.screens.SupportAndWarrenty.SupportWarrantyUi
import com.developer.ev2z.ui.screens.SupportAndWarrenty.WarrantyRoute
import com.developer.ev2z.ui.screens.SupportAndWarrenty.WarrantyStatus
import com.developer.ev2z.ui.screens.community.CommunityScreenRoot
import com.developer.ev2z.ui.screens.community.CreatePostScreen
import com.developer.ev2z.ui.screens.community.CreateStoryScreen
import com.developer.ev2z.ui.screens.community.StoryViewerScreen
import com.developer.ev2z.ui.screens.documents.DocumentViewerScreen
import com.developer.ev2z.ui.screens.documents.DocumentsScreen
import com.developer.ev2z.ui.screens.documents.UploadDocumentScreen
import com.developer.ev2z.ui.screens.social_media.openUrl
import com.developer.ev2z.util.BottomBarController
import com.developer.ev2z.util.platformDownloadFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ------------------ TABS -------------------

class HomeTab(
    private val viewModel: HomeViewModel,
    private val token: String,
    private val onNavigateToAddDevice: () -> Unit,
    private val onOpenDeviceStatus: () -> Unit,
    private val onNavigateToDeviceReport: () -> Unit,
    private val onOpenTrackTrace: () -> Unit,
    private val onOpenCommunity: () -> Unit
) : Tab {
    @Composable
    override fun Content() = HomeScreen(
        viewModel = viewModel,
        token = token,
        onNavigateToAddDevice = onNavigateToAddDevice,
        onNavigateToDeviceReport = onNavigateToDeviceReport,
        onOpenDeviceStatus = onOpenDeviceStatus,
        onOpenTrackTrace = onOpenTrackTrace,
        onOpenCommunity = onOpenCommunity
    )

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
        DocumentsGateState.LOCKED -> {
            LockedDocumentsPlaceholder(
                onUnlockClick = onUnlock
            )
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = "E-Vault",
            icon = rememberVectorPainter(Icons.Default.Folder)
        )
}

class OrdersTab : Tab {
    @Composable
    override fun Content() {
        Navigator(ShopScreenWrapper())
    }

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 2u,
            title = "Shop",
            icon = rememberVectorPainter(Icons.Default.ShoppingCart)
        )
}

class ShopScreenWrapper : Screen {
    @Composable
    override fun Content() {
        ShopScreenHost()
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun HomeNavGraph(
    viewModel: HomeViewModel,
    apiService: ApiService,
    storageManager: StorageManager,
    token: String,
    onNavigateToAddDevice: () -> Unit,
    onNavigateToDeviceReport: () -> Unit,
    onOpenDeviceStatus: () -> Unit,
    onOpenTrackTrace: () -> Unit,
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToAboutApp: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    SetupStatusBar()

    val dummySupportWarranty = SupportWarrantyUi(
        productName = "EVA2Z GPS Tracker",
        deviceId = "864839039292",
        warrantyStatus = WarrantyStatus.EXPIRED,
        warrantyExpiryDate = "15 Jan 2026"
    )

    // ✅ CREATE pagerState and coroutineScope HERE (at parent level)
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    var selectedWarrantyDeviceId by remember { mutableStateOf("") }
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
    var showUploadDocument by remember { mutableStateOf(false) } // 🔹 add state
    var showVaultPasswordDialog by remember { mutableStateOf(false) }
    var showCommunity by remember { mutableStateOf(false) }
    var showCreatePost by remember { mutableStateOf(false) }
    var showCreateStory by remember { mutableStateOf(false) }
    var showStoryViewer by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        HomeNavGraphContent(
            viewModel = viewModel,
            apiService = apiService,
            storageManager = storageManager,
            token = token,
            profileRefreshTrigger = profileRefresh,
            pagerState = pagerState, // ✅ PASS HERE
            coroutineScope = coroutineScope, // ✅ PASS HERE
            onNavigateToAddDevice = onNavigateToAddDevice,
            onNavigateToDeviceReport = onNavigateToDeviceReport,
            documentsGateState = documentsGateState,   // ✅ PASS HERE
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
            onNavigateToMyAccount = {
                showMyAccount = true     // 🔥 THIS IS THE KEY
            },
            onNavigateToDocuments = {
                showCommunity = true   // ✅ NOW CORRECT
            },
            onRequestDocumentsWithPassword = {
                showVaultPasswordDialog = true
            },

            onDocumentsTabSelected = {              // ✅ ADD THIS
                documentsGateState = DocumentsGateState.LOCKED
                showVaultPasswordDialog = true
            },

            onOpenCommunity = { showCommunity = true }, // ✅ HERE
            onLogout = onLogout
        )
        val addDeviceViewModel = remember {
            AddDeviceViewModel()
        }


        val isOnRootTabs =
            !showEditProfile &&
                    !showManageDevice &&
                    !showGPSData &&
                    !showEditDeviceName &&
                    !showPrivacyPolicy &&
                    !showTermsConditions &&
                    !showRefundPolicy &&
                    !showShippingPolicy &&
                    !showGeoFencing &&
                    !showHistoryPlayback &&
                    !showGeneralSettings &&
                    !showAccountActivity &&
                    !showReferAndEarn &&
                    !showAboutApp

        PlatformBackHandler(enabled = isOnRootTabs) {
            when (pagerState.currentPage) {
                0 -> showExitDialog = true
                1, 2, 3 -> {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                }
            }
        }


        // Edit Profile Screen
        if (showEditProfile) {
            EditProfileScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = {
                    showEditProfile = false
                    profileRefresh++
                }
            )
        }

//        if (showCommunity) {
//            Box(modifier = Modifier.fillMaxSize()) {
//
//                Navigator(
//                    CommunityScreenWrapper(
//                        onBack = {
//                            showCommunity = false   // 🔥 REAL BACK
//                        }
//                    )
//                )
//            }
//        }
        if (showCommunity) {
            CommunityScreenRoot(
                onBack = { showCommunity = false },

                onOpenCreatePost = {
                    showCreatePost = true
                },

                onOpenCreateStory = {
                    showCreateStory = true
                },

                onOpenStoryViewer = {
                    showStoryViewer = true
                }
            )
        }

        if (showCreatePost) {
           CreatePostScreen  (
               onBack = {
                   showCreatePost = false
               }
           )
        }

        if (showCreateStory) {
            CreateStoryScreen(
                onBack = {
                    showCreateStory = false
                }
            )
        }

        if (showStoryViewer) {
            StoryViewerScreen(
                onBack = {
                    showStoryViewer = false   // 🔥 THIS IS THE KEY
                }
            )
        }
        if (showDocuments) {
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
        }

        if (showUploadDocument) {
            UploadDocumentScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBack = {
                    showUploadDocument = false
                    showDocuments = true
                }
            )
        }

        if (showDocumentViewer && viewerBytes != null) {

            val androidContext = getAppContext()  // ✅ REAL Context

            DocumentViewerScreen(
                fileName = viewerFileName,
                bytes = viewerBytes!!,
                onBack = {
                    showDocumentViewer = false
                    showDocuments = true
                },
                onDownload = {
                    platformDownloadFile(
                        context = androidContext,   // ✅ FIXED
                        bytes = viewerBytes!!,
                        fileName = viewerFileName
                    )
                }
            )
        }

        // Manage Device Screen
        if (showManageDevice) {
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
                    showEditDeviceName = true
                }
            )
        }
//         GPS Data Screen
        if (showGPSData) {
            GPSDataScreen(
                apiService = apiService,
                storageManager = storageManager,
//                onBackClick = {showGPSData = false },
                onBackPressed = { showGPSData = false }
            )
        }

        if (showChangePassword) {
            ChangePasswordScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = {
                    showChangePassword = false

                    // ✅ FORCE Profile tab
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(3)
                    }
                }
            )
        }

        LaunchedEffect(showMyAccount) {
            if (showMyAccount && selectedWarrantyDeviceId.isEmpty()) {
                try {
                    val devices = apiService.getUserDevices()
                    val firstId = devices.data.firstOrNull()?.device_id?.toString() ?: ""
                    println("🔵 Setting default deviceId = $firstId")
                    selectedWarrantyDeviceId = firstId
                } catch (e: Exception) {
                    println("❌ Failed to set default device: ${e.message}")
                }
            }
        }

        if (showMyAccount) {
            MyAccountScreen(
                selectedDeviceId = selectedWarrantyDeviceId,                onBack = {
                    showMyAccount = false
                },
                onChangePlan = {
                    showMyAccount = false
                    showChangePlan = true
                },
                onBillDetails = {
                    showMyAccount = false
                    showBillDetails = true
                },
                onWarranty = { deviceId ->   // ✅ IMPORTANT
                    selectedWarrantyDeviceId = deviceId
                    showMyAccount = false
                    showWarranty = true
                }
            )
        }


        if (showChangePlan) {
            ChangePlanScreenHost(
                onBack = {
                    showChangePlan = false
                    showMyAccount = true
                }
            )
        }

        if (showBillDetails) {

            val vm = remember {
                BillDetailsViewModel(apiService, storageManager)
            }

            LaunchedEffect(Unit) {
                vm.loadBills()
            }

            BillDetailsScreen(
                bills = vm.bills,
                onBack = {
                    showBillDetails = false
                    showMyAccount = true
                }
            )
        }

        if (showWarranty && selectedWarrantyDeviceId != null) {
            WarrantyRoute(
                deviceId = selectedWarrantyDeviceId!!,
                onBack = {
                    showWarranty = false
                    showMyAccount = true
                },
                onBuyWarranty = {
                    println("Buy warranty clicked")
                }
            )
        }

        // Edit Device Name Screen
        if (showEditDeviceName) {
            EditDeviceNameScreen(
                apiService = apiService,
                storageManager = storageManager,
                deviceId = editDeviceId,
                deviceName = editDeviceName,
                simNumber = editSimNumber,
                onBackPressed = { showEditDeviceName = false },
                onUpdateSuccess = {
                    showEditDeviceName = false
                    manageDeviceRefresh++
                }
            )
        }

        // Privacy Policy
        if (showPrivacyPolicy) {
            PrivacyPolicy_TandCScreen(
                type = WebViewType.PRIVACY_POLICY,
                onBackPressed = { showPrivacyPolicy = false }
            )
        }

        // Terms & Conditions
        if (showTermsConditions) {
            PrivacyPolicy_TandCScreen(
                type = WebViewType.TERMS_CONDITIONS,
                onBackPressed = { showTermsConditions = false }
            )
        }

        // Refund Policy
        if (showRefundPolicy) {
            PrivacyPolicy_TandCScreen(
                type = WebViewType.REFUND_POLICY,
                onBackPressed = { showRefundPolicy = false }
            )
        }

        // Shipping Policy
        if (showShippingPolicy) {
            PrivacyPolicy_TandCScreen(
                type = WebViewType.SHIPPING_POLICY,
                onBackPressed = { showShippingPolicy = false }
            )
        }

        // Geo Fencing
        if (showGeoFencing) {
            GeoFencingScreenWrapper(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = { showGeoFencing = false }
            )
        }

        // History Playback
        if (showHistoryPlayback) {
            HistoryPlaybackScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = { showHistoryPlayback = false }
            )
        }

        // General Settings
        if (showGeneralSettings) {
            GeneralSettingsScreen(
                onBackPressed = { showGeneralSettings = false }
            )
        }

        // Account Activity
        if (showAccountActivity) {
            AccountActivityScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = { showAccountActivity = false },
                onAccountDeleted = {
                    showAccountActivity = false
                    onLogout()
                }
            )
        }

        // ✅ Refer And Earn - WITH SHOP NAVIGATION
        if (showReferAndEarn) {
            ReferAndEarnScreen(
                token = token,
                apiService = apiService,
                onBackPressed = { showReferAndEarn = false },
                onNavigateToShop = {
                    println("🛒 Navigating to Shop tab...")
                    showReferAndEarn = false
                    // ✅ Navigate to shop tab (index 2)
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(2)
                        println("✅ Shop tab navigation complete")
                    }
                },
                onNavigateToAddDevice = {
                    showReferAndEarn = false
                    showAddDevice = true
                }

            )
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


//        if (
//            documentsGateState == DocumentsGateState.LOCKED &&
//            pagerState.currentPage == 1 &&   // Documents tab
//            !showDocuments &&
//            !showVaultPasswordDialog
//        ) {
//            LockedDocumentsPlaceholder(
//                onUnlockClick = {
//                    showVaultPasswordDialog = true
//                }
//            )
//        }

        if (showAddDevice) {

            // 🔙 System back enable
            PlatformBackHandler(enabled = true) {
                showAddDevice = false
            }

            AddDeviceDetailsCommon(
                viewModel = addDeviceViewModel,
                onNavigate = { route ->
                    when (route) {
                        "back", "home" -> {
                            showAddDevice = false
                        }
                    }
                }
            )
        }


        LaunchedEffect(showVaultPasswordDialog) {
            if (showVaultPasswordDialog) {
                profileViewModel.clearError()
            }
        }

        // About App
        if (showAboutApp) {
            AboutAppScreen(
                onBackClick = { showAboutApp = false }
            )
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
                            containerColor = Color(0xFF7B61FF)
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
                    color = Color(0xFF7B61FF),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeNavGraphContent(
    viewModel: HomeViewModel,
    apiService: ApiService,
    storageManager: StorageManager,
    token: String,
    profileRefreshTrigger: Int = 0,
    pagerState: PagerState, // ✅ RECEIVE pagerState
    coroutineScope: CoroutineScope, // ✅ RECEIVE coroutineScope
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
    onOpenCommunity: () -> Unit,
    onLogout: () -> Unit
) {
    val productRepository = remember { ProductRepository(apiService) }
    val tokenProvider: () -> String = remember {
        { storageManager.getString("token","") }
    }
    val shopViewModel = remember {
        ShopViewModel(repo = productRepository, tokenProvider = tokenProvider)
    }

    val homeTab = remember { HomeTab(viewModel, token, onNavigateToAddDevice, onOpenDeviceStatus ,onNavigateToDeviceReport,onOpenTrackTrace,onOpenCommunity ) }

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

    val ordersTab = remember { OrdersTab() }
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

    // ✅ DON'T CREATE pagerState here - use the passed one

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            if (BottomBarController.visible) {
//                CustomBottomNavBar(
//                    tabs = tabs,
//                    selectedIndex = pagerState.currentPage,
//                    onTabSelected = { index ->
//                        coroutineScope.launch {
//                            pagerState.animateScrollToPage(index)
//                        }
//                    }
//                )
                CustomBottomNavBar(
                    tabs = tabs,
                    selectedIndex = pagerState.currentPage,
                    onTabSelected = { index ->
                        coroutineScope.launch {

                            // ✅ DOCUMENTS TAB CLICK
                            if (index == 1 && pagerState.currentPage != 1) {
                                onDocumentsTabSelected()   // 🔐 LOCK + dialog trigger
                            }

                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) { page ->
            tabs[page].Content()
        }
    }


}

@Composable
fun CustomBottomNavBar(
    tabs: List<Tab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val green = Color(0xFF4CAF50)
    val lightGreen = Color(0x1A4CAF50)

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedIndex == index

            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = green,
                    selectedTextColor = green,
                    indicatorColor = lightGreen,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                ),
                icon = {
                    Icon(
                        painter = tab.options.icon!!,
                        contentDescription = tab.options.title
                    )
                },
                label = {
                    Text(
                        text = tab.options.title,
                        color = if (selected) green else Color.Gray
                    )
                }
            )
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

