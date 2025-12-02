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
import cafe.adriel.voyager.navigator.tab.*
import com.developer.ev2z.ui.screens.homeScreen.HomeScreen
import kotlinx.coroutines.launch

// ------------------ TABS -------------------

object HomeTab : Tab {
    @Composable
    override fun Content() = HomeScreen()

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

object OrdersTab : Tab {
    @Composable
    override fun Content() = OrdersScreen()

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 2u,
            title = "Shop",
            icon = rememberVectorPainter(Icons.Default.ShoppingCart)
        )
}

object ProfileTab : Tab {
    @Composable
    override fun Content() = ProfileScreen()

    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 3u,
            title = "Profile",
            icon = rememberVectorPainter(Icons.Default.Person)
        )
}

// ------------------ NAV GRAPH WITH SWIPE -------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeNavGraph(onLogout: () -> Unit = {}) {

    // Status bar setup (platform specific)
    SetupStatusBar()

    val tabs = listOf(HomeTab, WalletTab, OrdersTab, ProfileTab)
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
    val lightGreen = Color(0x1A4CAF50)  // Very light green background (10% opacity)

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
                    indicatorColor = lightGreen,  // Halka green background
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
// Platform-specific status bar setup
@Composable
expect fun SetupStatusBar()

@Composable
fun WalletScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Explore Screen", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun OrdersScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Shop Screen", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Profile Screen", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = {}) { Text("Logout") }
    }
}





//package com.developer.ev2z.navigation
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.unit.dp
//import cafe.adriel.voyager.core.screen.Screen
//import cafe.adriel.voyager.navigator.tab.CurrentTab
//import cafe.adriel.voyager.navigator.tab.Tab
//import cafe.adriel.voyager.navigator.tab.TabNavigator
//import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
//import cafe.adriel.voyager.navigator.tab.TabOptions
//import com.developer.ev2z.ui.screens.homeScreen.HomeScreen
//
//// Tab definitions
//object HomeTab : Tab {
//    override val options: TabOptions
//        @Composable
//        get() = TabOptions(
//            index = 0u,
//            title = "Home",
//            icon = rememberVectorPainter(Icons.Default.Home)
//        )
//
//    @Composable
//    override fun Content() {
//        HomeScreen()
//    }
//}
//
//object WalletTab : Tab {
//    override val options: TabOptions
//        @Composable
//        get() = TabOptions(
//            index = 1u,
//            title = "Wallet",
//            icon = rememberVectorPainter(Icons.Default.AccountBalanceWallet)
//        )
//
//    @Composable
//    override fun Content() {
//        WalletScreen()
//    }
//}
//
//object OrdersTab : Tab {
//    override val options: TabOptions
//        @Composable
//        get() = TabOptions(
//            index = 2u,
//            title = "Orders",
//            icon = rememberVectorPainter(Icons.Default.List)
//        )
//
//    @Composable
//    override fun Content() {
//        OrdersScreen()
//    }
//}
//
//object ProfileTab : Tab {
//    override val options: TabOptions
//        @Composable
//        get() = TabOptions(
//            index = 3u,
//            title = "Profile",
//            icon = rememberVectorPainter(Icons.Default.Person)
//        )
//
//    @Composable
//    override fun Content() {
//        ProfileScreen()
//    }
//}
//
//// Main Navigation
//@Composable
//fun HomeNavGraph(onLogout: () -> Unit = {}) {
//    TabNavigator(HomeTab) {
//        Scaffold(
//            bottomBar = {
//                NavigationBar {
//                    TabNavigationItem(HomeTab)
//                    TabNavigationItem(WalletTab)
//                    TabNavigationItem(OrdersTab)
//                    TabNavigationItem(ProfileTab)
//                }
//            }
//        ) { padding ->
//            Box(modifier = Modifier.padding(padding)) {
//                CurrentTab()
//            }
//        }
//    }
//}
//
//@Composable
//private fun RowScope.TabNavigationItem(tab: Tab) {
//    val tabNavigator = LocalTabNavigator.current
//
//    NavigationBarItem(
//        selected = tabNavigator.current == tab,
//        onClick = { tabNavigator.current = tab },
//        icon = { Icon(painter = tab.options.icon!!, contentDescription = tab.options.title) },
//        label = { Text(tab.options.title) }
//    )
//}
//
//
//
//@Composable
//fun WalletScreen() {
//    Box(
//        modifier = Modifier.fillMaxSize(),
//        contentAlignment = Alignment.Center
//    ) {
//        Text("Wallet Screen", style = MaterialTheme.typography.headlineMedium)
//    }
//}
//
//@Composable
//fun OrdersScreen() {
//    Box(
//        modifier = Modifier.fillMaxSize(),
//        contentAlignment = Alignment.Center
//    ) {
//        Text("Orders Screen", style = MaterialTheme.typography.headlineMedium)
//    }
//}
//
//@Composable
//fun ProfileScreen() {
//    Column(
//        modifier = Modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text("Profile Screen", style = MaterialTheme.typography.headlineMedium)
//        Spacer(modifier = Modifier.height(16.dp))
//        Button(onClick = { /* Logout */ }) {
//            Text("Logout")
//        }
//    }
//}
//
//@Composable
//fun rememberVectorPainter(imageVector: ImageVector) =
//    androidx.compose.ui.graphics.vector.rememberVectorPainter(imageVector)