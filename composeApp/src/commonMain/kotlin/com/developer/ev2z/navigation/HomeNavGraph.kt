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
import com.developer.ev2z.ui.screens.homeScreen.HomeViewModel
import kotlinx.coroutines.launch

// ------------------ TABS -------------------



// ------------------ TABS -------------------

class HomeTab(
    private val viewModel: HomeViewModel,
    private val token: String
) : Tab {
    @Composable
    override fun Content() = HomeScreen(
        viewModel = viewModel,
        token = token
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
fun HomeNavGraph(
    viewModel: HomeViewModel,
    token: String,
    onLogout: () -> Unit = {}
) {
    // Status bar setup (platform specific)
    SetupStatusBar()

    val homeTab = remember { HomeTab(viewModel, token) }
    val tabs = listOf(homeTab, WalletTab, OrdersTab, ProfileTab)
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

//object HomeTab : Tab {
//    @Composable
//    override fun Content() = HomeScreen()
//
//    override val options: TabOptions
//        @Composable get() = TabOptions(
//            index = 0u,
//            title = "Home",
//            icon = rememberVectorPainter(Icons.Default.Home)
//        )
//}
//
//object WalletTab : Tab {
//    @Composable
//    override fun Content() = WalletScreen()
//
//    override val options: TabOptions
//        @Composable get() = TabOptions(
//            index = 1u,
//            title = "Explore",
//            icon = rememberVectorPainter(Icons.Default.GridView)
//        )
//}
//
//object OrdersTab : Tab {
//    @Composable
//    override fun Content() = OrdersScreen()
//
//    override val options: TabOptions
//        @Composable get() = TabOptions(
//            index = 2u,
//            title = "Shop",
//            icon = rememberVectorPainter(Icons.Default.ShoppingCart)
//        )
//}
//
//object ProfileTab : Tab {
//    @Composable
//    override fun Content() = ProfileScreen()
//
//    override val options: TabOptions
//        @Composable get() = TabOptions(
//            index = 3u,
//            title = "Profile",
//            icon = rememberVectorPainter(Icons.Default.Person)
//        )
//}
//
//// ------------------ NAV GRAPH WITH SWIPE -------------------
//
//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun HomeNavGraph(onLogout: () -> Unit = {}) {
//
//    // Status bar setup (platform specific)
//    SetupStatusBar()
//
//    val tabs = listOf(HomeTab, WalletTab, OrdersTab, ProfileTab)
//    val pagerState = rememberPagerState(pageCount = { tabs.size })
//    val coroutineScope = rememberCoroutineScope()
//
//    Scaffold(
//        containerColor = Color.White,
//        bottomBar = {
//            CustomBottomNavBar(
//                tabs = tabs,
//                selectedIndex = pagerState.currentPage,
//                onTabSelected = { index ->
//                    coroutineScope.launch {
//                        pagerState.animateScrollToPage(index)
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        HorizontalPager(
//            state = pagerState,
//            modifier = Modifier
//                .padding(padding)
//                .fillMaxSize()
//        ) { page ->
//            tabs[page].Content()
//        }
//    }
//}
//
//@Composable
//fun CustomBottomNavBar(
//    tabs: List<Tab>,
//    selectedIndex: Int,
//    onTabSelected: (Int) -> Unit
//) {
//    val green = Color(0xFF4CAF50)
//    val lightGreen = Color(0x1A4CAF50)  // Very light green background (10% opacity)
//
//    NavigationBar(
//        containerColor = Color.White,
//        tonalElevation = 0.dp
//    ) {
//        tabs.forEachIndexed { index, tab ->
//            val selected = selectedIndex == index
//
//            NavigationBarItem(
//                selected = selected,
//                onClick = { onTabSelected(index) },
//
//                colors = NavigationBarItemDefaults.colors(
//                    selectedIconColor = green,
//                    selectedTextColor = green,
//                    indicatorColor = lightGreen,  // Halka green background
//                    unselectedIconColor = Color.Gray,
//                    unselectedTextColor = Color.Gray
//                ),
//
//                icon = {
//                    Icon(
//                        painter = tab.options.icon!!,
//                        contentDescription = tab.options.title
//                    )
//                },
//
//                label = {
//                    Text(
//                        text = tab.options.title,
//                        color = if (selected) green else Color.Gray
//                    )
//                }
//            )
//        }
//    }
//}
//// Platform-specific status bar setup
//@Composable
//expect fun SetupStatusBar()
//
//@Composable
//fun WalletScreen() {
//    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Text("Explore Screen", style = MaterialTheme.typography.headlineMedium)
//    }
//}
//
//@Composable
//fun OrdersScreen() {
//    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Text("Shop Screen", style = MaterialTheme.typography.headlineMedium)
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
//        Spacer(Modifier.height(16.dp))
//        Button(onClick = {}) { Text("Logout") }
//    }
//}





