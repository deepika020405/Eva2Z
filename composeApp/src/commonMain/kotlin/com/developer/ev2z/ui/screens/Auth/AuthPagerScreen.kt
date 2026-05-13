package com.developer.ev2z.ui.screens.Auth

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import com.developer.ev2z.viewmodel.AuthViewModel
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AuthPagerScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onForgetPasswordClick: (String) -> Unit = {},
    onSignupOTPClick: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val pageCount = 2
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
    val coroutineScope = rememberCoroutineScope()

    var preFilledPhoneLogin by remember { mutableStateOf("") }
    var preFilledPhoneSignup by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFE6EEE6)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val screenHeight = maxHeight
            val isCompact = screenHeight < 700.dp

            // Responsive header height
            val headerFraction = if (isCompact) 0.16f else 0.20f
            val logoSize = if (isCompact) 60.dp else 80.dp
            val appTitleSize = if (isCompact) 20.sp else 24.sp
            val tabTitleSize = if (isCompact) 20.sp else 24.sp
            val tabFontSize = if (isCompact) 16.sp else 18.sp
            val tabHeight = if (isCompact) 50.dp else 58.dp
            val tabInnerHeight = if (isCompact) 36.dp else 42.dp
            val contentHorizontalPad = if (isCompact) 24.dp else 32.dp

            // Header with logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * headerFraction),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(logoSize)
                    )

                    Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

                    Text(
                        text = "Eva2z Connect",
                        fontSize = appTitleSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                }
            }

            // White card content area
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = screenHeight * headerFraction)
                    .background(
                        color = Color(0xFFFFFFFF),
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = contentHorizontalPad, vertical = if (isCompact) 18.dp else 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 24.dp))

                    // Title
                    Text(
                        text = "Log in or Sign up",
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(
                            fontSize = tabTitleSize,
                            lineHeight = (tabTitleSize.value * 1.3f).sp,
                            fontWeight = FontWeight(500),
                            color = Color(0xFF252A23),
                            textAlign = TextAlign.Center,
                        )
                    )

                    Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 32.dp))

                    // Tab Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(tabHeight)
                            .background(
                                color = Color(0xFFE6EDE3),
                                shape = RoundedCornerShape(size = 40.dp)
                            )
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Log In Tab (Left) — page 0
                        if (pagerState.currentPage == 0) {
                            Box(
                                modifier = Modifier
                                    .shadow(
                                        elevation = 4.dp,
                                        spotColor = Color(0x336A9F50),
                                        ambientColor = Color(0x336A9F50)
                                    )
                                    .weight(1f)
                                    .height(tabInnerHeight)
                                    .background(
                                        color = Color(0xFFFFFFFF),
                                        shape = RoundedCornerShape(size = 40.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Log In",
                                    style = TextStyle(
                                        fontSize = tabFontSize,
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
                                    .height(tabInnerHeight),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFF5D6858)
                                ),
                                shape = RoundedCornerShape(40.dp)
                            ) {
                                Text(
                                    text = "Log In",
                                    style = TextStyle(
                                        fontSize = tabFontSize,
                                        fontWeight = FontWeight(400),
                                        color = Color(0xFF5D6858),
                                        textAlign = TextAlign.Center,
                                    )
                                )
                            }
                        }

                        // Sign Up Tab (Right) — page 1
                        if (pagerState.currentPage == 1) {
                            Box(
                                modifier = Modifier
                                    .shadow(
                                        elevation = 4.dp,
                                        spotColor = Color(0x336A9F50),
                                        ambientColor = Color(0x336A9F50)
                                    )
                                    .weight(1f)
                                    .height(tabInnerHeight)
                                    .background(
                                        color = Color(0xFFFFFFFF),
                                        shape = RoundedCornerShape(size = 40.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign Up",
                                    style = TextStyle(
                                        fontSize = tabFontSize,
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
                                    .height(tabInnerHeight),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFF5D6858)
                                ),
                                shape = RoundedCornerShape(40.dp)
                            ) {
                                Text(
                                    text = "Sign Up",
                                    style = TextStyle(
                                        fontSize = tabFontSize,
                                        fontWeight = FontWeight(400),
                                        color = Color(0xFF5D6858),
                                        textAlign = TextAlign.Center,
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 16.dp))

                    // Pager Content
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = onLoginSuccess,
                                onForgetPasswordClick = onForgetPasswordClick,
                                onNotRegistered = { phone ->
                                    println("📞 Login→Signup: $phone")
                                    preFilledPhoneSignup = phone
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                                preFilledPhone = preFilledPhoneLogin
                            )
                            1 -> SignupScreen(
                                viewModel = viewModel,
                                onSignupSuccess = onLoginSuccess,
                                onSendOTPClick = onSignupOTPClick,
                                onAlreadyRegistered = { phone ->
                                    println("📞 Signup→Login: $phone")
                                    preFilledPhoneLogin = phone
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                },
                                preFilledPhone = preFilledPhoneSignup
                            )
                        }
                    }
                }
            }
        }
    }
}