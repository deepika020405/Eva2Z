package com.developer.ev2z.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.*
import com.developer.ev2z.data.local.StorageManager
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
import com.developer.ev2z.ui.screens.community.CommunityScreenWrapper
import com.developer.ev2z.ui.screens.market_screen.ShopViewModel
import com.developer.ev2z.ui.screens.GeneralSettings.GeneralSettingsScreen
import com.developer.ev2z.ui.screens.AccountActivity.AccountActivityScreen
import com.developer.ev2z.ui.screens.Profile.AboutAppScreen
import com.developer.ev2z.ui.screens.ReferAndEarn.ReferAndEarnScreen
import kotlinx.coroutines.launch

// ------------------ TABS -------------------

class HomeTab(
    private val viewModel: HomeViewModel,
    private val token: String,
    private val onNavigateToAddDevice: () -> Unit,
    private val onNavigateToDeviceReport: () -> Unit
) : Tab {
    @Composable
    override fun Content() = HomeScreen(
        viewModel = viewModel,
        token = token,
        onNavigateToAddDevice = onNavigateToAddDevice,
        onNavigateToDeviceReport = onNavigateToDeviceReport
    )

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 0u,
            title = "Home",
            icon = rememberVectorPainter(Icons.Default.Home)
        )
}

object CommunityTab : Tab {
    @Composable
    override fun Content() {
        Navigator(CommunityScreenWrapper())
    }
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = "Community",
            icon = rememberVectorPainter(Icons.Default.GridView)
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
    private val refreshTrigger: Int, // ✅ Add refresh trigger
    private val onNavigateToEditProfile: () -> Unit,
    private val onNavigateToManageDevice: () -> Unit,
    private val onNavigateToPrivacyPolicy: () -> Unit,
    private val onNavigateToTermsConditions: () -> Unit,
    private val onNavigateToRefundPolicy: () -> Unit,
    private val onNavigateToShippingPolicy: () -> Unit,
    private val onNavigateToChangePassword: () -> Unit,
    private val onNavigateToGeoFencing: () -> Unit,
    private val onNavigateToHistoryPlayback: () -> Unit,
    private val onNavigateToGeneralSettings: () -> Unit,
    private val onNavigateToAccountActivity: () -> Unit,
    private val onNavigateToReferAndEarn: () -> Unit,
    private val onNavigateToAboutApp: () -> Unit = {},
    private val onLogout: () -> Unit
) : Tab {
    @Composable
    override fun Content() {
        ProfileScreen(
            apiService = apiService,
            storageManager = storageManager,
            refreshTrigger = refreshTrigger, // ✅ Pass refresh trigger
            onEditProfile = onNavigateToEditProfile,
            onManageDevice = onNavigateToManageDevice,
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
            onLogout = onLogout
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeNavGraph(
    viewModel: HomeViewModel,
    apiService: ApiService,
    storageManager: StorageManager,
    token: String,
    onNavigateToAddDevice: () -> Unit,
    onNavigateToDeviceReport: () -> Unit,
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToAboutApp: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    SetupStatusBar()

    // ✅ Screen visibility states
    var showEditProfile by remember { mutableStateOf(false) }
    var showManageDevice by remember { mutableStateOf(false) }
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

    // ✅ Edit device state
    var editDeviceId by remember { mutableStateOf("") }
    var editDeviceName by remember { mutableStateOf("") }
    var editSimNumber by remember { mutableStateOf("") }

    // ✅ Refresh triggers
    var manageDeviceRefresh by remember { mutableStateOf(0) }
    var profileRefresh by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeNavGraphContent(
            viewModel = viewModel,
            apiService = apiService,
            storageManager = storageManager,
            token = token,
            profileRefreshTrigger = profileRefresh, // ✅ Pass refresh trigger
            onNavigateToAddDevice = onNavigateToAddDevice,
            onNavigateToDeviceReport = onNavigateToDeviceReport,
            onNavigateToEditProfile = { showEditProfile = true },
            onNavigateToManageDevice = { showManageDevice = true },
            onNavigateToPrivacyPolicy = { showPrivacyPolicy = true },
            onNavigateToTermsConditions = { showTermsConditions = true },
            onNavigateToRefundPolicy = { showRefundPolicy = true },
            onNavigateToShippingPolicy = { showShippingPolicy = true },
            onNavigateToChangePassword = onNavigateToChangePassword,
            onNavigateToGeoFencing = { showGeoFencing = true },
            onNavigateToHistoryPlayback = { showHistoryPlayback = true },
            onNavigateToGeneralSettings = { showGeneralSettings = true },
            onNavigateToAccountActivity = { showAccountActivity = true },
            onNavigateToReferAndEarn = { showReferAndEarn = true },
            onNavigateToAboutApp = { showAboutApp = true },
            onLogout = onLogout
        )

        // ✅ Edit Profile Screen
        if (showEditProfile) {
            EditProfileScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = {
                    showEditProfile = false
                    profileRefresh++ // ✅ Trigger profile refresh
                }
            )
        }

        // ✅ Manage Device Screen
        if (showManageDevice) {
            ManageDeviceScreen(
                apiService = apiService,
                storageManager = storageManager,
                refreshTrigger = manageDeviceRefresh, // ✅ Pass refresh trigger
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

        // ✅ Edit Device Name Screen
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
                    manageDeviceRefresh++ // ✅ Trigger device list refresh
                }
            )
        }

        // ✅ Privacy Policy
        if (showPrivacyPolicy) {
            PrivacyPolicy_TandCScreen(
                type = WebViewType.PRIVACY_POLICY,
                onBackPressed = { showPrivacyPolicy = false }
            )
        }

        // ✅ Terms & Conditions
        if (showTermsConditions) {
            PrivacyPolicy_TandCScreen(
                type = WebViewType.TERMS_CONDITIONS,
                onBackPressed = { showTermsConditions = false }
            )
        }


        // ✅ Refund Policy
        if (showRefundPolicy) {
            PrivacyPolicy_TandCScreen(
                type = WebViewType.REFUND_POLICY,
                onBackPressed = { showRefundPolicy = false }
            )
        }

        // ✅ Shipping Policy
        if (showShippingPolicy) {
            PrivacyPolicy_TandCScreen(
                type = WebViewType.SHIPPING_POLICY,
                onBackPressed = { showShippingPolicy = false }
            )
        }

        // ✅ Geo Fencing
        if (showGeoFencing) {
            GeoFencingScreenWrapper(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = { showGeoFencing = false }
            )
        }

        // ✅ History Playback
        if (showHistoryPlayback) {
            HistoryPlaybackScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = { showHistoryPlayback = false }
            )
        }

        // ✅ General Settings
        if (showGeneralSettings) {
            GeneralSettingsScreen(
                onBackPressed = { showGeneralSettings = false }
            )
        }

        // ✅ Account Activity
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

        // ✅ Refer And Earn
        // ✅ Refer And Earn
        if (showReferAndEarn) {
            ReferAndEarnScreen(
                token = token,
                apiService = apiService,
                onBackPressed = { showReferAndEarn = false }
            )
        }

        if (showAboutApp) {
            AboutAppScreen(
                onBackClick = { showAboutApp = false }
            )
        }
    }
}

@Composable
fun HomeNavGraphContent(
    viewModel: HomeViewModel,
    apiService: ApiService,
    storageManager: StorageManager,
    token: String,
    profileRefreshTrigger: Int = 0, // ✅ Add refresh trigger
    onNavigateToAddDevice: () -> Unit,
    onNavigateToDeviceReport: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToManageDevice: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToTermsConditions: () -> Unit,
    onNavigateToRefundPolicy: () -> Unit,
    onNavigateToShippingPolicy: () -> Unit,
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToGeoFencing: () -> Unit = {},
    onNavigateToHistoryPlayback: () -> Unit = {},
    onNavigateToGeneralSettings: () -> Unit = {},
    onNavigateToAccountActivity: () -> Unit = {},
    onNavigateToReferAndEarn: () -> Unit = {},
    onNavigateToAboutApp: () -> Unit = {},
    onLogout: () -> Unit
) {
    val productRepository = remember { ProductRepository(apiService) }
    val tokenProvider: () -> String = remember {
        { storageManager.getString("token","") }
    }
    val shopViewModel = remember {
        ShopViewModel(repo = productRepository, tokenProvider = tokenProvider)
    }

    val homeTab = remember { HomeTab(viewModel, token, onNavigateToAddDevice, onNavigateToDeviceReport) }

    // ✅ Update ProfileTab with refresh trigger
    val profileTab = remember(profileRefreshTrigger) {
        ProfileTab(
            apiService,
            storageManager,
            profileRefreshTrigger, // ✅ Pass refresh trigger
            onNavigateToEditProfile,
            onNavigateToManageDevice,
            onNavigateToPrivacyPolicy,
            onNavigateToTermsConditions,
            onNavigateToRefundPolicy,
            onNavigateToShippingPolicy,
            onNavigateToChangePassword,
            onNavigateToGeoFencing,
            onNavigateToHistoryPlayback,
            onNavigateToGeneralSettings,
            onNavigateToAccountActivity,
            onNavigateToReferAndEarn,
            onNavigateToAboutApp,
            onLogout
        )
    }

    val ordersTab = remember { OrdersTab() }
    val tabs = listOf(homeTab, CommunityTab, ordersTab, profileTab)

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            CustomBottomNavBar(
                tabs = tabs,
                selectedIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
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

@Composable
expect fun SetupStatusBar()

@Composable
fun WalletScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Explore Screen", style = MaterialTheme.typography.headlineMedium)
    }
}