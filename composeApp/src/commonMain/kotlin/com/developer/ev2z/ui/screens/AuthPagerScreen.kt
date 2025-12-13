// ========== UPDATED AuthPagerScreen.kt ==========
package com.developer.ev2z.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.ui.screens.loginScreen.LoginScreen
import com.developer.ev2z.ui.screens.signupScreen.SignupScreen
import com.developer.ev2z.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AuthPagerScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onForgetPasswordClick: () -> Unit = {},
    onSignupOTPClick: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val pageCount = 2
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFE6EDE3)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val screenHeight = maxHeight

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = screenHeight * 0.20f)
                    .background(
                        color = Color(0xFFFFFFFF),
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Title
                    Text(
                        text = "Log in or Sign up today",
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(
                            fontSize = 24.sp,
                            lineHeight = 31.2.sp,
                            fontWeight = FontWeight(500),
                            color = Color(0xFF252A23),
                            textAlign = TextAlign.Center,
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Tab Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .background(
                                color = Color(0xFFE6EDE3),
                                shape = RoundedCornerShape(size = 40.dp)
                            )
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Login Tab
                        if (pagerState.currentPage == 0) {
                            Box(
                                modifier = Modifier
                                    .shadow(
                                        elevation = 4.dp,
                                        spotColor = Color(0x336A9F50),
                                        ambientColor = Color(0x336A9F50)
                                    )
                                    .weight(1f)
                                    .height(42.dp)
                                    .background(
                                        color = Color(0xFFFFFFFF),
                                        shape = RoundedCornerShape(size = 40.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Log In",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight(500),
                                        color = Color(0xFF494E47),
                                        textAlign = TextAlign.Center,
                                    )
                                )
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFF5D6858)
                                ),
                                shape = RoundedCornerShape(40.dp)
                            ) {
                                Text(
                                    text = "Log In",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight(400),
                                        color = Color(0xFF5D6858),
                                        textAlign = TextAlign.Center,
                                    )
                                )
                            }
                        }

                        // Signup Tab
                        if (pagerState.currentPage == 1) {
                            Box(
                                modifier = Modifier
                                    .shadow(
                                        elevation = 4.dp,
                                        spotColor = Color(0x336A9F50),
                                        ambientColor = Color(0x336A9F50)
                                    )
                                    .weight(1f)
                                    .height(42.dp)
                                    .background(
                                        color = Color(0xFFFFFFFF),
                                        shape = RoundedCornerShape(size = 40.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign Up",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight(500),
                                        color = Color(0xFF494E47),
                                        textAlign = TextAlign.Center,
                                    )
                                )
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFF5D6858)
                                ),
                                shape = RoundedCornerShape(40.dp)
                            ) {
                                Text(
                                    text = "Sign Up",
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight(400),
                                        color = Color(0xFF5D6858),
                                        textAlign = TextAlign.Center,
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pager Content
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = onLoginSuccess,
                                onForgetPasswordClick = onForgetPasswordClick
                            )
                            1 -> SignupScreen(
                                viewModel = viewModel,
                                onSignupSuccess = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                },
                                onSendOTPClick = onSignupOTPClick
                            )
                        }
                    }
                }
            }
        }
    }
}