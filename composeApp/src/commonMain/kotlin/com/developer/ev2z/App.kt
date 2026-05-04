package com.developer.ev2z


import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.data.local.createStorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.repository.AuthRepository
import com.developer.ev2z.ui.screens.Auth.AuthPagerScreen
import com.developer.ev2z.ui.screens.ChangePassword.UpdatePasswordViewModel
import com.developer.ev2z.ui.screens.homeScreen.HomeViewModel
import com.developer.ev2z.viewmodel.AuthViewModel
import com.developer.ev2z.navigation.HomeNavGraph
import com.developer.ev2z.repository.DeviceReportRepository
import com.developer.ev2z.repository.HomeRepository
import com.developer.ev2z.ui.screens.AddDevice.AddDeviceViewModel
import com.developer.ev2z.ui.screens.AddDevice.AddDeviceResult
import com.developer.ev2z.ui.screens.AddDevice.BottomSheet
import com.developer.ev2z.ui.screens.AddDeviceSuccess.AddDeviceSuccessScreen
import com.developer.ev2z.ui.screens.ChangePassword.ChangePasswordScreen
import com.developer.ev2z.ui.screens.device_Report.DeviceReportScreen
import com.developer.ev2z.ui.screens.device_Report.DeviceReportViewModel
import com.developer.ev2z.ui.screens.device_Report.PlatformAddressProvider
import com.developer.ev2z.util.AppTokenProvider
import com.developer.ev2z.ui.screens.AuthFlows.ForgetPasswordFlow
import com.developer.ev2z.ui.screens.AuthFlows.RegistrationFlow
import com.developer.ev2z.util.UpdateAvailableDialog
import com.developer.ev2z.util.isUpdateAvailable
import com.developer.ev2z.ui.screens.Profile.getPlatformStoreUrl
import com.developer.ev2z.ui.screens.Profile.platformOpenUrl
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.developer.ev2z.data.local.DeepLinkManager
import com.developer.ev2z.ui.screens.SplashScreen
import com.developer.ev2z.ui.screens.deviceStatus.DeviceStatusScreen
import com.developer.ev2z.ui.screens.trackTrace.TrackTraceScreen
import com.developer.ev2z.util.FcmRegistrar


@Composable
fun App() {
    val apiService = remember { ApiService() }
    val authRepository = remember { AuthRepository(apiService) }
    val homeRepository = remember { HomeRepository(apiService) }
    val deviceReportRepository = remember { DeviceReportRepository(apiService) }
    val storage = remember { createStorageManager() }
    val userPrefs = remember { UserPrefs(storage) }
    // ✅ DeepLinkManager init karo
    remember { DeepLinkManager.init(storage) }
    val authViewModel = remember { AuthViewModel(authRepository, storage) }
    val homeViewModel = remember { HomeViewModel(homeRepository) }
    val addDeviceViewModel = remember { AddDeviceViewModel() }

    // ✅ NEW: UpdatePasswordViewModel
    val updatePasswordViewModel = remember { UpdatePasswordViewModel(apiService, storage) }

    val platformContext = getPlatformContext()
    val addressProvider = remember { PlatformAddressProvider(platformContext) }

    val deviceReportViewModel = remember {
        DeviceReportViewModel(
            repository = deviceReportRepository,
            platformAddressProvider = addressProvider
        )
    }

    var isLoading by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var authToken by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var showAddDevice by remember { mutableStateOf(false) }
    var showDeviceReport by remember { mutableStateOf(false) }
    var showAddDeviceSuccess by remember { mutableStateOf(false) }
    var showDeviceStatus by remember { mutableStateOf(false) }
    var showTrackTrace by remember { mutableStateOf(false) }
    var deviceStatusSource by remember { mutableStateOf(OpenSource.HOME) }
    var trackTraceSource by remember { mutableStateOf(OpenSource.HOME) }


    // Authentication flow states
    var showForgetPassword by remember { mutableStateOf(false) }
    var showRegistration by remember { mutableStateOf(false) }
    var registrationName by remember { mutableStateOf("") }
    var registrationPhone by remember { mutableStateOf("") }
    var registrationEmail by remember { mutableStateOf("") }

    // ✅ NEW: Change Password state
    var showChangePassword by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }


    // 🔥 My Account navigation states
    var showMyAccount by remember { mutableStateOf(false) }
    var showChangePlan by remember { mutableStateOf(false) }
    var showBillDetails by remember { mutableStateOf(false) }
    var showTransactions by remember { mutableStateOf(false) }
    var showAutoPay by remember { mutableStateOf(false) }
    var showWarranty by remember { mutableStateOf(false) }
    var showPaymentMethods by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }


    // Success screen data
    var successDeviceName by remember { mutableStateOf("") }
    var successDeviceId by remember { mutableStateOf("") }
    var successSimNumber by remember { mutableStateOf("") }
    var successDeviceType by remember { mutableStateOf("") }

    val loginSuccess by authViewModel.loginSuccess.collectAsState()
    val signupSuccess by authViewModel.signupSuccess.collectAsState()

    LaunchedEffect(signupSuccess) {
        if (signupSuccess) {
            showRegistration = true
            authViewModel.resetSignupSuccess()
        }
    }
    val addDeviceResult by addDeviceViewModel.addDeviceResult.collectAsState()
    var forgetPasswordPhone by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            if (isUpdateAvailable()) showUpdateDialog = true
        } catch (_: Exception) {}
    }

    // Login success handler
//    LaunchedEffect(loginSuccess) {
//        if (loginSuccess) {
//            authToken = userPrefs.getToken() ?: ""
//            isLoggedIn = authToken.isNotEmpty()
//            println("✅ Login successful! Token: $authToken")
//        }
//    }


    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            delay(100)

            val token = userPrefs.getToken() ?: ""
            val name = userPrefs.getName() ?: "User"

            if (token.isNotEmpty()) {
                authToken = token
                username = name
                isLoggedIn = true
                AppTokenProvider.getToken = { token }
                authViewModel.resetLoginSuccess()
            }
        }
    }

    /* ---------------------------------------
      2️⃣ POST-LOGIN SIDE EFFECT → FCM REGISTER
      Always send the token on every login so the server is guaranteed to have it.
      Token is saved only after successful API response to prevent silent cache bugs.
   ---------------------------------------- */
    LaunchedEffect(isLoggedIn, authToken) {
        if (isLoggedIn && authToken.isNotEmpty()) {
            FcmBridge.register { fcmToken ->
                FcmRegistrar.registerFcmToServer(
                    authTokenNullable = "Bearer $authToken",
                    fcmToken          = fcmToken,
                    deviceUuid        = userPrefs.getOrCreateDeviceUuid(),
                    platform          = platformName(),
                    onSuccess         = { userPrefs.saveLastFcmToken(fcmToken) }
                )
            }
        }
    }


    // AddDevice result handler
    LaunchedEffect(addDeviceResult) {
        when (val result = addDeviceResult) {
            is AddDeviceResult.Success -> {
                successDeviceName = result.deviceName
                successDeviceId = result.deviceId
                successSimNumber = result.simNumber
                successDeviceType = result.deviceType
                showAddDevice = false
                showAddDeviceSuccess = true
            }
            is AddDeviceResult.Error -> { }

            null -> { /* No result */ }
        }
    }

    MaterialTheme {
        when {
            // App.kt mein — isLoading block
            isLoading -> {
                SplashScreen(
                    storage = storage,
                    userPrefs = userPrefs,
                    onNavigateToHome = {
                        // ✅ Token bhi load karo
                        val token = userPrefs.getToken() ?: ""
                        authToken = token
                        username = userPrefs.getName() ?: "User"
                        AppTokenProvider.getToken = { token }
                        isLoggedIn = true
                        isLoading = false
                    },
                    onNavigateToLogin = {
                        isLoggedIn = false
                        authToken = ""
                        isLoading = false
                    },
                    onNavigateToShop = {
                        val token = userPrefs.getToken() ?: ""
                        authToken = token
                        username = userPrefs.getName() ?: "User"
                        AppTokenProvider.getToken = { token }
                        isLoggedIn = true
                        isLoading = false
                    },
                    onNavigateToProduct = { productId ->
                        val token = userPrefs.getToken() ?: ""
                        authToken = token
                        username = userPrefs.getName() ?: "User"
                        AppTokenProvider.getToken = { token }
                        isLoggedIn = true
                        isLoading = false
                    }
                )
            }

            // UPDATE showChangePassword rendering
            showChangePassword -> {
                ChangePasswordScreen(
                    apiService = apiService,
                    storageManager = storage,
                    onBackPressed = {
                        showChangePassword = false
                    }
                )
            }

            // Forget Password Flow
            showForgetPassword -> {
                ForgetPasswordFlow(
                    authViewModel = authViewModel,
                    preFilledPhone = forgetPasswordPhone,
                    onBack = {
                        showForgetPassword = false
                        forgetPasswordPhone = ""
                    },
                    onSuccess = {
                        showForgetPassword = false
                        forgetPasswordPhone = ""
                    }
                )
            }

            // Registration Flow
            showRegistration -> {
                RegistrationFlow(
                    authViewModel = authViewModel,
                    name = registrationName,
                    phone = registrationPhone,
                    email = registrationEmail,
                    onBack = {
                        showRegistration = false
                    },
                    onSuccess = {
                        showRegistration = false
                    }
                )
            }

            showAddDeviceSuccess -> {
                AddDeviceSuccessScreen(
                    onNavigate = { route ->
                        when (route) {
                            "home" -> {

                                showAddDeviceSuccess = false
                                addDeviceViewModel.clearData()
                                homeViewModel.loadDevices(authToken)
                            }
                        }
                    },
                    deviceName = successDeviceName,
                    deviceId = successDeviceId,
                    simNumber = successSimNumber,
                    deviceType = successDeviceType
                )
            }

            showAddDevice -> {
                BottomSheet(
                    onNavigate = { route ->
                        when (route) {
                            "home" -> {

                                showAddDevice = false
                                addDeviceViewModel.clearData()
                            }
                            "addDeviceDeteails" -> {

                            }
                        }
                    },
                    viewModel = addDeviceViewModel
                )
            }

            showDeviceReport -> {
                DeviceReportScreen(
                    viewModel = deviceReportViewModel,
                    token = authToken,
                    username = username,
                    onTrackTrace = {
                        trackTraceSource = OpenSource.DEVICE_REPORT   // ✅ FIX
                        showDeviceReport = false
                        showTrackTrace = true
                    },
                    onTodayReport = {
                        deviceStatusSource = OpenSource.DEVICE_REPORT
                        showDeviceReport = false
                        showDeviceStatus = true
                    },
                    onBack = {

                        showDeviceReport = false
                    }
                )
            }


            showDeviceStatus -> {
                DeviceStatusScreen(
                    onBack = {
                        showDeviceStatus = false

                        if (deviceStatusSource == OpenSource.DEVICE_REPORT) {
                            showDeviceReport = true
                        }
                    }
                )
            }
             showTrackTrace -> {
                TrackTraceScreen  (
                    onBack = {
                        showTrackTrace = false

                        if (trackTraceSource == OpenSource.DEVICE_REPORT) {
                            showDeviceReport = true
                        }
                    }
                )
            }



            isLoggedIn && authToken.isNotEmpty() -> {
                HomeNavGraph(
                    viewModel = homeViewModel,
                    apiService = apiService,
                    storageManager = storage,
                    userPrefs = userPrefs,        // ✅ add karo
                    token = authToken,
                    onNavigateToAddDevice = {
                        showAddDevice = true
                    },
                    onNavigateToDeviceReport = {
                        showDeviceReport = true
                    },
                    onNavigateToChangePassword = {
                        showChangePassword = true
                    },
                    onOpenDeviceStatus = {
                        deviceStatusSource = OpenSource.HOME
                        showDeviceStatus = true
                    },
                    onOpenTrackTrace = {
                        trackTraceSource = OpenSource.HOME   // ✅ CORRECT
                        showTrackTrace = true
                    },

                    onLogout = {
                        userPrefs.clear()
                        storage.clear()
                        isLoggedIn = false
                        authToken = ""
                        username = ""
                        authViewModel.resetLoginSuccess()
                        authViewModel.clearError()
                        AppTokenProvider.getToken = { "" }
                        FcmBridge.clear()
                    }
                )
            }

            else -> {
                AuthPagerScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        authViewModel.setLoginSuccess(true)
                    },
                    onForgetPasswordClick = { phone ->
                        forgetPasswordPhone = phone
                        showForgetPassword = true
                    },
                    onSignupOTPClick = { name, phone, email ->
                        registrationName = name
                        registrationPhone = phone
                        registrationEmail = email
                        // phone already contains the full international number (e.g. +929319837225)
                        authViewModel.signUp(name, phone, "", email)
                        // showRegistration is set by LaunchedEffect(signupSuccess)
                        // only after the OTP is confirmed sent successfully
                    }
                )
            }
        }

        if (showUpdateDialog) {
            UpdateAvailableDialog(
                onUpdate = {
                    platformOpenUrl(getPlatformStoreUrl())
                    showUpdateDialog = false
                },
                onDismiss = { showUpdateDialog = false }
            )
        }
    }
}

//@Composable
//fun SplashScreen(
//    onNavigateToHome: () -> Unit,
//    onNavigateToLogin: () -> Unit,
//    onNavigateToShop: () -> Unit,
//    onNavigateToProduct: (String) -> Unit,
//    isUserLoggedIn: () -> Boolean  // pass from your auth manager
//) {
//    LaunchedEffect(Unit) {
//        // Android only — referrer fetch
//        ReferrerHelper(context).fetchIfFirstLaunch {
//            val token = storage.getString(StorageKeys.VAULT_TOKEN)
//
//            if (token.isNotEmpty()) {
//                // Logged in
//                val pendingLink = DeepLinkManager.consumePendingDeepLink()
//                if (pendingLink != null) {
//                    when {
//                        pendingLink.startsWith("/shop/product/") -> {
//                            val id = pendingLink.removePrefix("/shop/product/")
//                            onNavigateToProduct(id)
//                        }
//                        pendingLink.startsWith("/shop") -> onNavigateToShop()
//                        else -> onNavigateToHome()
//                    }
//                } else {
//                    onNavigateToHome()
//                }
//            } else {
//                onNavigateToLogin()
//            }
//        }
//    }
//
//    // ✅ Your existing UI — NO CHANGES NEEDED BELOW
//    val infiniteTransition = rememberInfiniteTransition()
//
//    val rotation by infiniteTransition.animateFloat(
//        initialValue = 0f,
//        targetValue = 360f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(durationMillis = 2000, easing = LinearEasing),
//            repeatMode = RepeatMode.Restart
//        )
//    )
//
//    val scale by infiniteTransition.animateFloat(
//        initialValue = 0.95f,
//        targetValue = 1.05f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
//            repeatMode = RepeatMode.Reverse
//        )
//    )
//
//    val alpha by infiniteTransition.animateFloat(
//        initialValue = 0.7f,
//        targetValue = 1f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
//            repeatMode = RepeatMode.Reverse
//        )
//    )
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color(0xFFFFFFFF)),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(160.dp)
//                    .scale(scale)
//                    .rotate(rotation),
//                contentAlignment = Alignment.Center
//            ) {
//                Image(
//                    painter = painterResource(Res.drawable.app_logo),
//                    contentDescription = "App Logo",
//                    modifier = Modifier.size(140.dp)
//                )
//            }
//
//            Spacer(modifier = Modifier.height(32.dp))
//            Text(
//                text = "EVA2Z",
//                fontSize = 48.sp,
//                fontWeight = FontWeight.Bold,
//                color = Color.Black.copy(alpha = alpha)
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(
//                text = "Connect",
//                fontSize = 24.sp,
//                fontWeight = FontWeight.Medium,
//                color = Color.Black.copy(alpha = alpha * 0.9f),
//                letterSpacing = 2.sp
//            )
//            Spacer(modifier = Modifier.height(48.dp))
//            CircularProgressIndicator(
//                color = Color.Black,
//                modifier = Modifier.size(48.dp),
//                strokeWidth = 4.dp
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                text = "Loading...",
//                fontSize = 14.sp,
//                color = Color.Black.copy(alpha = 0.8f),
//                letterSpacing = 1.sp
//            )
//        }
//
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .navigationBarsPadding()
//                .padding(bottom = 16.dp),
//            contentAlignment = Alignment.BottomCenter
//        ) {}
//    }
//}

enum class OpenSource {
    HOME,
    DEVICE_REPORT
}



