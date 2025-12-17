// HomeNavGraph.kt - FINAL FIXED VERSION
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
import cafe.adriel.voyager.navigator.tab.*
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.repository.ProductRepository
import com.developer.ev2z.ui.screens.EditProfile.EditProfileScreen
import com.developer.ev2z.ui.screens.Profile.ProfileScreen
import com.developer.ev2z.ui.screens.homeScreen.HomeScreen
import com.developer.ev2z.ui.screens.homeScreen.HomeViewModel
import com.developer.ev2z.ui.screens.ManageDevices.ManageDeviceScreen
import com.developer.ev2z.ui.screens.market_screen.ShopScreenHost
import com.developer.ev2z.ui.screens.PrivacyPolicy_TandC.PrivacyPolicy_TandCScreen
import com.developer.ev2z.ui.screens.PrivacyPolicy_TandC.WebViewType
import com.developer.ev2z.ui.screens.market_screen.ShopViewModel
import kotlinx.coroutines.launch

// ------------------ TABS (same as before) -------------------

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

object WalletTab : Tab {
    @Composable
    override fun Content() = WalletScreen()

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = "Explore",
            icon = rememberVectorPainter(Icons.Default.GridView)
        )
}

class OrdersTab(
    private val shopViewModel: ShopViewModel,
    private val token: String
) : Tab {
    @Composable
    override fun Content() {
        Navigator(screen = ShopScreenWrapper(shopViewModel))
    }

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 2u,
            title = "Shop",
            icon = rememberVectorPainter(Icons.Default.ShoppingCart)
        )
}

class ShopScreenWrapper(
    private val viewModel: ShopViewModel
) : Screen {
    @Composable
    override fun Content() {
        ShopScreenHost(viewModel = viewModel)
    }
}

class ProfileTab(
    private val apiService: ApiService,
    private val storageManager: StorageManager,
    private val onNavigateToEditProfile: () -> Unit,
    private val onNavigateToManageDevice: () -> Unit,
    private val onNavigateToPrivacyPolicy: () -> Unit,
    private val onNavigateToTermsConditions: () -> Unit,
    private val onNavigateToChangePassword: () -> Unit,
    private val onLogout: () -> Unit
) : Tab {
    @Composable
    override fun Content() {
        ProfileScreen(
            apiService = apiService,
            storageManager = storageManager,
            onEditProfile = onNavigateToEditProfile,
            onManageDevice = onNavigateToManageDevice,
            onPrivacyPolicy = onNavigateToPrivacyPolicy,
            onTermsConditions = onNavigateToTermsConditions,
            onChangePassword = onNavigateToChangePassword,
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
    onNavigateToChangePassword: () -> Unit = {}, // ✅ RECEIVE FROM APP
    onLogout: () -> Unit = {}
) {
    SetupStatusBar()

    var showEditProfile by remember { mutableStateOf(false) }
    var showManageDevice by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTermsConditions by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeNavGraphContent(
            viewModel = viewModel,
            apiService = apiService,
            storageManager = storageManager,
            token = token,
            onNavigateToAddDevice = onNavigateToAddDevice,
            onNavigateToDeviceReport = onNavigateToDeviceReport,
            onNavigateToEditProfile = { showEditProfile = true },
            onNavigateToManageDevice = { showManageDevice = true },
            onNavigateToPrivacyPolicy = { showPrivacyPolicy = true },
            onNavigateToTermsConditions = { showTermsConditions = true },
            onNavigateToChangePassword = onNavigateToChangePassword, // ✅ PASS TO PROFILE
            onLogout = onLogout
        )

        // Edit Profile Screen
        if (showEditProfile) {
            EditProfileScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBackPressed = { showEditProfile = false }
            )
        }

        // Manage Device Screen
        if (showManageDevice) {
            ManageDeviceScreen(
                apiService = apiService,
                storageManager = storageManager,
                onBackClick = { showManageDevice = false },
                onProfileClick = { showManageDevice = false },
                onAddDeviceClick = {
                    showManageDevice = false
                    onNavigateToAddDevice()
                }
            )
        }

        // Privacy Policy Screen
        if (showPrivacyPolicy) {
            PrivacyPolicy_TandCScreen(
                type = WebViewType.PRIVACY_POLICY,
                onBackPressed = { showPrivacyPolicy = false }
            )
        }

        // Terms & Conditions Screen
        if (showTermsConditions) {
            PrivacyPolicy_TandCScreen(
                type = WebViewType.TERMS_CONDITIONS,
                onBackPressed = { showTermsConditions = false }
            )
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
    onNavigateToAddDevice: () -> Unit,
    onNavigateToDeviceReport: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToManageDevice: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToTermsConditions: () -> Unit,
    onNavigateToChangePassword: () -> Unit = {},
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
    val profileTab = remember {
        ProfileTab(
            apiService,
            storageManager,
            onNavigateToEditProfile,
            onNavigateToManageDevice,
            onNavigateToPrivacyPolicy,
            onNavigateToTermsConditions,
            onNavigateToChangePassword, // ✅ PASS CALLBACK
            onLogout
        )
    }
    val ordersTab = remember { OrdersTab(shopViewModel, token) }
    val tabs = listOf(homeTab, WalletTab, ordersTab, profileTab)
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