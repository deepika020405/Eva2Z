package com.developer.ev2z.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.developer.ev2z.ui.screens.homeScreen.HomeScreen

// Tab definitions
object HomeTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = "Home",
            icon = rememberVectorPainter(Icons.Default.Home)
        )

    @Composable
    override fun Content() {
        HomeScreen()
    }
}

object WalletTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 1u,
            title = "Wallet",
            icon = rememberVectorPainter(Icons.Default.AccountBalanceWallet)
        )

    @Composable
    override fun Content() {
        WalletScreen()
    }
}

object OrdersTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 2u,
            title = "Orders",
            icon = rememberVectorPainter(Icons.Default.List)
        )

    @Composable
    override fun Content() {
        OrdersScreen()
    }
}

object ProfileTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 3u,
            title = "Profile",
            icon = rememberVectorPainter(Icons.Default.Person)
        )

    @Composable
    override fun Content() {
        ProfileScreen()
    }
}

// Main Navigation
@Composable
fun HomeNavGraph(onLogout: () -> Unit = {}) {
    TabNavigator(HomeTab) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    TabNavigationItem(HomeTab)
                    TabNavigationItem(WalletTab)
                    TabNavigationItem(OrdersTab)
                    TabNavigationItem(ProfileTab)
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                CurrentTab()
            }
        }
    }
}

@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationBarItem(
        selected = tabNavigator.current == tab,
        onClick = { tabNavigator.current = tab },
        icon = { Icon(painter = tab.options.icon!!, contentDescription = tab.options.title) },
        label = { Text(tab.options.title) }
    )
}

// Blank Screens
@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Home Screen", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun WalletScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Wallet Screen", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun OrdersScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Orders Screen", style = MaterialTheme.typography.headlineMedium)
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
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* Logout */ }) {
            Text("Logout")
        }
    }
}

@Composable
fun rememberVectorPainter(imageVector: ImageVector) =
    androidx.compose.ui.graphics.vector.rememberVectorPainter(imageVector)