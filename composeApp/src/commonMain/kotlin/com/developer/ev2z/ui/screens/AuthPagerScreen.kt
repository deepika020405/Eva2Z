package com.developer.ev2z.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import com.developer.ev2z.ui.screens.loginScreen.LoginScreen
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.developer.ev2z.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AuthPagerScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val pageCount = 2
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Tabs
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabButton(
                text = "Login",
                selected = pagerState.currentPage == 0
            ) {
                coroutineScope.launch { pagerState.animateScrollToPage(0) }
            }

            Spacer(modifier = Modifier.width(16.dp))

            TabButton(
                text = "Sign Up",
                selected = pagerState.currentPage == 1
            ) {
                coroutineScope.launch { pagerState.animateScrollToPage(1) }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Pager
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(16.dp)
            ) { page ->
                when (page) {
                    0 -> LoginScreen(viewModel, onLoginSuccess)
                    1 -> SignupScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun SignupScreen(viewModel: AuthViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Signup Screen Coming Soon")
    }
}