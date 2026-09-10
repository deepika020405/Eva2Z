package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.data.local.createStorageManager
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.launch
import com.developer.ev2z.theme.AppPageBg

// ── Theme Colors ─────────────────────────────────────────────────
internal val CV_Primary = Color(0xFF6A9F50)
internal val CV_PrimaryDark = Color(0xFF4A7D39)
internal val CV_PrimaryBg = Color(0xFFF0F7EC)
internal val CV_Background = AppPageBg
internal val CV_Surface = Color(0xFFFFFFFF)
internal val CV_SectionBg = Color(0xFFF3F6F0)
internal val CV_Border = Color(0xFFE2E8DD)
internal val CV_TextPrimary = Color(0xFF1C2418)
internal val CV_TextSec = Color(0xFF5D6858)
internal val CV_TextHint = Color(0xFFA0A99B)
internal val CV_Success = Color(0xFF4A9F3A)
internal val CV_Error = Color(0xFFE5565B)
internal val CV_Warning = Color(0xFFF4C44E)
internal val CV_Info = Color(0xFF4A90D9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriminalVerificationFlow(
    apiService: ApiService,
    onBack: () -> Unit,
    onGoToDashboard: () -> Unit = onBack
) {
    val userPrefs = remember { UserPrefs(createStorageManager()) }
    val vm = remember { CriminalVerificationViewModel(apiService, userPrefs) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.loadPersistedState()
    }

    PlatformBackHandler(true) {
        when (vm.step) {
            VerificationStep.SUCCESS -> vm.reset()
            else -> onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CV_Background)
    ) {
        // Custom top bar
        Surface(
            color = CV_Surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = WindowInsets.statusBars
                            .asPaddingValues()
                            .calculateTopPadding() + 12.dp,
                        bottom = 12.dp,
                        start = 14.dp,
                        end = 14.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = CV_SectionBg,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            when (vm.step) {
                                VerificationStep.SUCCESS -> vm.reset()
                                else -> onBack()
                            }
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (vm.step == VerificationStep.SUCCESS)
                                Icons.Default.Close
                            else
                                Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp),
                            tint = CV_TextPrimary
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = CV_Primary,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    "Criminal Verification",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CV_TextPrimary
                )
            }
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (vm.step) {
                VerificationStep.FORM ->
                    CriminalVerificationEntryScreen(
                        isLoading = vm.loading,
                        history = vm.history,
                        onSubmitNew = { form -> scope.launch { vm.submitForm(form) } },
                        onSearchStatus = { candidateId, name ->
                            scope.launch { vm.searchByCandidate(candidateId, name) }
                        },
                        onDeleteHistory = { id -> vm.deleteHistoryEntry(id) }
                    )

                VerificationStep.CREATING_ORDER ->
                    ProcessingScreen("Creating payment order...")

                VerificationStep.PAYMENT ->
                    vm.paymentToken?.let { token ->
                        CriminalPaymentScreen(
                            paymentToken = token,
                            phonepeOrderId = vm.phonepeOrderId.orEmpty(),
                            amount = vm.amount,
                            onSuccess = { txnId ->
                                scope.launch { vm.onPaymentSuccess(txnId ?: "") }
                            },
                            onFailure = { error ->
                                vm.errorMessage = error ?: "Payment failed"
                                vm.step = VerificationStep.ERROR
                            },
                            onBack = { vm.reset() }
                        )
                    }

                VerificationStep.VERIFYING_PAYMENT ->
                    ProcessingScreen("Verifying payment...")

                VerificationStep.CREATING_CANDIDATE ->
                    ProcessingScreen("Creating candidate profile...")

                VerificationStep.SUBMITTING_BGV ->
                    ProcessingScreen("Submitting background verification...")

                VerificationStep.SUCCESS -> {
                    LaunchedEffect(vm.createdCandidateId) {
                        if (vm.createdCandidateId != null) {
                            vm.checkVerificationStatus()
                        }
                    }

                    CriminalSuccessScreen(
                        form = vm.cachedForm,
                        candidateId = vm.createdCandidateId,
                        status = vm.bgvStatus,
                        reportUrl = vm.reportUrl,
                        candidateName = vm.searchedCandidateName,
                        courtResult = vm.courtCheckResult,
                        paymentDone = vm.paymentDone,
                        amount = vm.amount,
                        merchantOrderId = vm.merchantOrderId,
                        onDone = onGoToDashboard,
                        onNewVerification = { vm.reset() }
                    )
                }

                VerificationStep.ERROR ->
                    CriminalErrorScreen(
                        message = vm.errorMessage,
                        onRetry = { vm.reset() },
                        onBack = onBack
                    )
            }
        }
    }
}